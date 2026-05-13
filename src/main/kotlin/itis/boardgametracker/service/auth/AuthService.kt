package itis.boardgametracker.service.auth

import io.micrometer.core.annotation.Timed
import itis.boardgametracker.api.dto.AuthResponse
import itis.boardgametracker.api.dto.ChangeNameRequest
import itis.boardgametracker.api.dto.ChangePasswordRequest
import itis.boardgametracker.api.dto.LoginRequest
import itis.boardgametracker.api.dto.LogoutRequest
import itis.boardgametracker.api.dto.RefreshRequest
import itis.boardgametracker.api.dto.RefreshResponse
import itis.boardgametracker.api.dto.RegisterRequest
import itis.boardgametracker.exception.DuplicateEmailException
import itis.boardgametracker.exception.InvalidCredentialsException
import itis.boardgametracker.exception.OldPasswordMismatchException
import itis.boardgametracker.exception.UserNotFoundException
import itis.boardgametracker.model.User
import itis.boardgametracker.repository.RoleRepository
import itis.boardgametracker.repository.UserRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val passwordEncoder: PasswordEncoder,
    private val accessTokenService: AccessTokenService,
    private val refreshTokenService: RefreshTokenService,
    private val authAuditService: AuthAuditService
) {
    object MetricsCatalog {
        const val REGISTER_TIME = "auth.register.time"
        const val LOGIN_TIME = "auth.login.time"
        const val REFRESH_TIME = "auth.refresh.time"
        const val LOGOUT_TIME = "auth.logout.time"
        const val PROFILE_GET_TIME = "auth.profile.get.time"
        const val PROFILE_NAME_CHANGE_TIME = "auth.profile.change_name.time"
        const val PASSWORD_CHANGE_TIME = "auth.password.change.time"
    }

    @Transactional
    @Timed(value = MetricsCatalog.REGISTER_TIME)
    fun register(request: RegisterRequest, context: AuthContext? = null): AuthResponse {
        if (userRepository.existsByEmail(request.email)) {
            throw DuplicateEmailException()
        }

        var createdUser = try {
            userRepository.create(
                User(
                    name = request.name,
                    email = request.email,
                    password = passwordEncoder.encode(request.password)
                )
            )
        } catch (_: DataIntegrityViolationException) {
            throw DuplicateEmailException()
        }
        val createdUserId = createdUser.id ?: throw UserNotFoundException()
        val userRole = roleRepository.findByName("USER")
        roleRepository.assignRoleToUser(createdUserId, userRole.id!!)
        createdUser = findUserById(createdUserId)

        val accessToken = accessTokenService.generate(createdUser)
        val refreshTokenPair = refreshTokenService.issue(createdUserId)
        authAuditService.write(action = "register", userId = createdUserId, context = context)

        return AuthResponse(
            accessToken = accessToken,
            refreshToken = refreshTokenPair.rawToken,
            user = mapUser(createdUser),
            expiresIn = accessTokenService.expiresInSeconds()
        )
    }

    @Timed(value = MetricsCatalog.LOGIN_TIME)
    fun login(request: LoginRequest, context: AuthContext? = null): AuthResponse {
        val user = try {
            userRepository.findByEmail(request.email)
        } catch (_: EmptyResultDataAccessException) {
            authAuditService.write(action = "login_failed", context = context)
            throw InvalidCredentialsException()
        }

        if (!passwordEncoder.matches(request.password, user.password)) {
            authAuditService.write(action = "login_failed", userId = user.id, context = context)
            throw InvalidCredentialsException()
        }

        val accessToken = accessTokenService.generate(user)
        val refreshTokenPair = refreshTokenService.issue(user.id!!)
        authAuditService.write(action = "login_success", userId = user.id, context = context)

        return AuthResponse(
            accessToken = accessToken,
            refreshToken = refreshTokenPair.rawToken,
            user = mapUser(user),
            expiresIn = accessTokenService.expiresInSeconds()
        )
    }

    @Timed(value = MetricsCatalog.REFRESH_TIME)
    fun refresh(request: RefreshRequest, context: AuthContext? = null): RefreshResponse {
        val refreshPair = refreshTokenService.refresh(request.refreshToken, context)
        val user = findUserById(refreshPair.persistedToken.userId)
        val accessToken = accessTokenService.generate(user)
        authAuditService.write(action = "refresh_success", userId = user.id, context = context)
        return RefreshResponse(
            accessToken = accessToken,
            refreshToken = refreshPair.rawToken,
            oldRefreshTokenRevoked = true,
            expiresIn = accessTokenService.expiresInSeconds()
        )
    }

    @Timed(value = MetricsCatalog.LOGOUT_TIME)
    fun logout(request: LogoutRequest, userId: Long?, context: AuthContext? = null) {
        val currentUserId = userId ?: throw InvalidCredentialsException()
        refreshTokenService.logout(request.refreshToken, currentUserId)
        authAuditService.write(action = "logout", userId = userId, context = context)
    }

    @Timed(value = MetricsCatalog.PROFILE_GET_TIME)
    fun getProfile(userId: Long): itis.boardgametracker.api.dto.User {
        return mapUser(findUserById(userId))
    }

    @Transactional
    @Timed(value = MetricsCatalog.PROFILE_NAME_CHANGE_TIME)
    fun changeName(userId: Long, request: ChangeNameRequest): itis.boardgametracker.api.dto.User {
        val user = try {
            userRepository.updateName(userId, request.name)
        } catch (_: EmptyResultDataAccessException) {
            throw UserNotFoundException()
        }
        return mapUser(user)
    }

    @Transactional
    @Timed(value = MetricsCatalog.PASSWORD_CHANGE_TIME)
    fun changePassword(userId: Long, request: ChangePasswordRequest, context: AuthContext? = null): AuthResponse {
        val user = findUserById(userId)
        if (!passwordEncoder.matches(request.oldPassword, user.password)) {
            throw OldPasswordMismatchException()
        }

        val updatedUser = userRepository.updatePassword(userId, passwordEncoder.encode(request.newPassword))
        refreshTokenService.revokeAllByUserId(userId)
        val accessToken = accessTokenService.generate(updatedUser)
        val refreshTokenPair = refreshTokenService.issue(updatedUser.id!!)
        authAuditService.write(action = "password_changed", userId = userId, context = context)

        return AuthResponse(
            accessToken = accessToken,
            refreshToken = refreshTokenPair.rawToken,
            user = mapUser(updatedUser),
            expiresIn = accessTokenService.expiresInSeconds()
        )
    }

    private fun findUserById(userId: Long): User {
        return try {
            userRepository.findById(userId)
        } catch (_: EmptyResultDataAccessException) {
            throw UserNotFoundException()
        }
    }

    private fun mapUser(user: User): itis.boardgametracker.api.dto.User {
        return itis.boardgametracker.api.dto.User(
            id = user.id!!.toInt(),
            email = user.email,
            name = user.name,
            roles = user.roles.map { it.name },
            createdAt = OffsetDateTime.ofInstant(user.createdAt, ZoneOffset.UTC)
        )
    }
}