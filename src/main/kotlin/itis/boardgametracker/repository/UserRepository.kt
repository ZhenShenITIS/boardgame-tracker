package itis.boardgametracker.repository

import itis.boardgametracker.model.Role
import itis.boardgametracker.model.User
import itis.boardgametracker.util.RoleRowMapper
import itis.boardgametracker.util.UserRowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.Types
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Repository
class UserRepository(
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate
) {
    private val insertSql: String = """
        INSERT INTO users (name, email, password, created_at)
        VALUES (:name, :email, :password, :createdAt)
        RETURNING id, name, email, password, created_at
    """.trimIndent()

    private val findByIdSql: String = """
        SELECT id, name, email, password, created_at
        FROM users
        WHERE id = :id
    """.trimIndent()

    private val findByEmailSql: String = """
        SELECT id, name, email, password, created_at
        FROM users
        WHERE email = :email
    """.trimIndent()

    private val existsByEmailSql: String = """
        SELECT EXISTS(SELECT 1 FROM users WHERE email = :email)
    """.trimIndent()

    private val updateNameSql: String = """
        UPDATE users
        SET name = :name
        WHERE id = :userId
        RETURNING id, name, email, password, created_at
    """.trimIndent()

    private val updatePasswordSql: String = """
        UPDATE users
        SET password = :password
        WHERE id = :userId
        RETURNING id, name, email, password, created_at
    """.trimIndent()

    private val findRolesByUserIdSql: String = """
        SELECT r.id, r.name
        FROM roles r
                 JOIN users_roles ur ON ur.role_id = r.id
        WHERE ur.user_id = :userId
        ORDER BY r.name
    """.trimIndent()

    fun create(user: User): User {
        val persistedUser = namedParameterJdbcTemplate.queryForObject(
            insertSql,
            mapForCreate(user),
            UserRowMapper
        )!!
        return persistedUser.copy(roles = findRolesByUserId(persistedUser.id!!))
    }

    fun findById(id: Long): User {
        val user = namedParameterJdbcTemplate.queryForObject(
            findByIdSql,
            MapSqlParameterSource().addValue("id", id),
            UserRowMapper
        )!!
        return user.copy(roles = findRolesByUserId(user.id!!))
    }

    fun findByEmail(email: String): User {
        val user = namedParameterJdbcTemplate.queryForObject(
            findByEmailSql,
            MapSqlParameterSource().addValue("email", email),
            UserRowMapper
        )!!
        return user.copy(roles = findRolesByUserId(user.id!!))
    }

    fun existsByEmail(email: String): Boolean {
        return namedParameterJdbcTemplate.queryForObject(
            existsByEmailSql,
            MapSqlParameterSource().addValue("email", email),
            Boolean::class.java
        ) ?: false
    }

    fun updateName(userId: Long, name: String): User {
        val user = namedParameterJdbcTemplate.queryForObject(
            updateNameSql,
            MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("name", name),
            UserRowMapper
        )!!
        return user.copy(roles = findRolesByUserId(user.id!!))
    }

    fun updatePassword(userId: Long, passwordHash: String): User {
        val user = namedParameterJdbcTemplate.queryForObject(
            updatePasswordSql,
            MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("password", passwordHash),
            UserRowMapper
        )!!
        return user.copy(roles = findRolesByUserId(user.id!!))
    }

    private fun findRolesByUserId(userId: Long): List<Role> {
        return namedParameterJdbcTemplate.query(
            findRolesByUserIdSql,
            MapSqlParameterSource().addValue("userId", userId),
            RoleRowMapper
        )
    }

    private fun mapForCreate(user: User): MapSqlParameterSource {
        return MapSqlParameterSource()
            .addValue("name", user.name, Types.VARCHAR)
            .addValue("email", user.email, Types.VARCHAR)
            .addValue("password", user.password, Types.VARCHAR)
            .addValue(
                "createdAt",
                OffsetDateTime.ofInstant(user.createdAt, ZoneOffset.UTC),
                Types.TIMESTAMP_WITH_TIMEZONE
            )
    }
}
