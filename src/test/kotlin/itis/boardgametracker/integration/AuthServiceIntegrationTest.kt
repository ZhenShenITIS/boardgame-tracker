package itis.boardgametracker.integration

import itis.boardgametracker.api.dto.ChangePasswordRequest
import itis.boardgametracker.api.dto.LoginRequest
import itis.boardgametracker.api.dto.LogoutRequest
import itis.boardgametracker.api.dto.RefreshRequest
import itis.boardgametracker.api.dto.RegisterRequest
import itis.boardgametracker.exception.DuplicateEmailException
import itis.boardgametracker.exception.InvalidCredentialsException
import itis.boardgametracker.exception.InvalidOrExpiredRefreshTokenException
import itis.boardgametracker.exception.RevokedRefreshTokenReuseException
import itis.boardgametracker.service.auth.AuthService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64

class AuthServiceIntegrationTest : IntegrationTest() {
    @Autowired
    lateinit var authService: AuthService

    @Autowired
    lateinit var jdbcTemplate: NamedParameterJdbcTemplate

    @BeforeEach
    fun cleanupAuthData() {
        jdbcTemplate.jdbcTemplate.execute("DELETE FROM audit_logs")
        jdbcTemplate.jdbcTemplate.execute("DELETE FROM refresh_tokens")
        jdbcTemplate.jdbcTemplate.execute("DELETE FROM users_roles")
        jdbcTemplate.jdbcTemplate.execute("DELETE FROM users")
    }

    @Test
    fun registerSuccessCreatesUserRoleAndHashedPassword() {
        val response = authService.register(RegisterRequest("John Doe", "john@itis.com", "Password123"))

        val userRow = jdbcTemplate.queryForMap(
            "SELECT id, password FROM users WHERE email = :email",
            MapSqlParameterSource().addValue("email", "john@itis.com")
        )
        val userId = (userRow["id"] as Number).toLong()
        val storedPassword = userRow["password"] as String

        val roleCount = jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*) FROM users_roles ur
                JOIN roles r ON r.id = ur.role_id
                WHERE ur.user_id = :userId AND r.name = 'USER'
            """.trimIndent(),
            MapSqlParameterSource().addValue("userId", userId),
            Int::class.java
        ) ?: 0

        assertTrue(storedPassword != "Password123")
        assertEquals(1, roleCount)
        assertNotNull(response.accessToken)
        assertNotNull(response.refreshToken)
        assertEquals("USER", response.user.roles?.first())
    }

    @Test
    fun registerDuplicateEmailThrowsConflict() {
        authService.register(RegisterRequest("John Doe", "john@itis.com", "Password123"))
        assertThrows(DuplicateEmailException::class.java) {
            authService.register(RegisterRequest("Jane Doe", "john@itis.com", "Password123"))
        }
    }

    @Test
    fun registerDuplicateEmailViaDatabaseConstraintThrowsConflict() {
        authService.register(RegisterRequest("John Doe", "john@itis.com", "Password123"))
        jdbcTemplate.jdbcTemplate.execute("DELETE FROM users")

        jdbcTemplate.update(
            """
                INSERT INTO users (name, email, password)
                VALUES (:name, :email, :password)
            """.trimIndent(),
            MapSqlParameterSource()
                .addValue("name", "Existing User")
                .addValue("email", "john@itis.com")
                .addValue("password", "bcrypt")
        )

        assertThrows(DuplicateEmailException::class.java) {
            authService.register(RegisterRequest("Jane Doe", "john@itis.com", "Password123"))
        }
    }

    @Test
    fun loginSuccess() {
        authService.register(RegisterRequest("John Doe", "john@itis.com", "Password123"))
        val response = authService.login(LoginRequest("john@itis.com", "Password123"))
        assertNotNull(response.accessToken)
        assertNotNull(response.refreshToken)
    }

    @Test
    fun loginWithInvalidPasswordThrowsException() {
        authService.register(RegisterRequest("John Doe", "john@itis.com", "Password123"))
        assertThrows(InvalidCredentialsException::class.java) {
            authService.login(LoginRequest("john@itis.com", "WrongPassword123"))
        }
    }

    @Test
    fun refreshSuccessRevokesOldAndCreatesNew() {
        authService.register(RegisterRequest("John Doe", "john@itis.com", "Password123"))
        val loginResponse = authService.login(LoginRequest("john@itis.com", "Password123"))
        val oldRefresh = loginResponse.refreshToken

        val refreshResponse = authService.refresh(RefreshRequest(oldRefresh))
        val oldRevoked = jdbcTemplate.queryForObject(
            "SELECT revoked FROM refresh_tokens WHERE token_hash = :hash",
            MapSqlParameterSource().addValue("hash", hash(oldRefresh)),
            Boolean::class.java
        ) ?: false

        assertTrue(oldRevoked)
        assertTrue(refreshResponse.oldRefreshTokenRevoked == true)
        assertNotEquals(oldRefresh, refreshResponse.refreshToken)
    }

    @Test
    fun revokedRefreshReuseRevokesAllAndThrowsException() {
        authService.register(RegisterRequest("John Doe", "john@itis.com", "Password123"))
        val loginResponse = authService.login(LoginRequest("john@itis.com", "Password123"))
        val oldRefresh = loginResponse.refreshToken
        authService.refresh(RefreshRequest(oldRefresh))

        assertThrows(RevokedRefreshTokenReuseException::class.java) {
            authService.refresh(RefreshRequest(oldRefresh))
        }

        val activeTokens = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM refresh_tokens WHERE revoked = FALSE",
            MapSqlParameterSource(),
            Int::class.java
        ) ?: 0
        assertEquals(0, activeTokens)
    }

    @Test
    fun changePasswordValidatesOldPasswordAndRevokesUserTokens() {
        authService.register(RegisterRequest("John Doe", "john@itis.com", "Password123"))
        val loginResponse = authService.login(LoginRequest("john@itis.com", "Password123"))
        val oldRefresh = loginResponse.refreshToken
        val userId = (jdbcTemplate.queryForMap(
            "SELECT id FROM users WHERE email = :email",
            MapSqlParameterSource().addValue("email", "john@itis.com")
        )["id"] as Number).toLong()

        authService.changePassword(userId, ChangePasswordRequest("Password123", "Password456"))

        val oldRevoked = jdbcTemplate.queryForObject(
            "SELECT revoked FROM refresh_tokens WHERE token_hash = :hash",
            MapSqlParameterSource().addValue("hash", hash(oldRefresh)),
            Boolean::class.java
        ) ?: false
        val activeTokens = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM refresh_tokens WHERE user_id = :userId AND revoked = FALSE",
            MapSqlParameterSource().addValue("userId", userId),
            Int::class.java
        ) ?: 0

        assertTrue(oldRevoked)
        assertEquals(1, activeTokens)
        assertThrows(InvalidCredentialsException::class.java) {
            authService.login(LoginRequest("john@itis.com", "Password123"))
        }
    }

    @Test
    fun logoutWithForeignRefreshTokenReturnsUnauthorizedAndDoesNotRevokeToken() {
        authService.register(RegisterRequest("John Doe", "john1@itis.com", "Password123"))
        authService.register(RegisterRequest("Jane Doe", "john2@itis.com", "Password123"))

        val johnLogin = authService.login(LoginRequest("john1@itis.com", "Password123"))
        val janeUserId = (jdbcTemplate.queryForMap(
            "SELECT id FROM users WHERE email = :email",
            MapSqlParameterSource().addValue("email", "john2@itis.com")
        )["id"] as Number).toLong()

        assertThrows(InvalidOrExpiredRefreshTokenException::class.java) {
            authService.logout(LogoutRequest(johnLogin.refreshToken), janeUserId)
        }

        val johnTokenRevoked = jdbcTemplate.queryForObject(
            "SELECT revoked FROM refresh_tokens WHERE token_hash = :hash",
            MapSqlParameterSource().addValue("hash", hash(johnLogin.refreshToken)),
            Boolean::class.java
        ) ?: true
        assertEquals(false, johnTokenRevoked)
    }

    private fun hash(rawToken: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(rawToken.toByteArray(StandardCharsets.UTF_8))
        return Base64.getEncoder().encodeToString(bytes)
    }
}
