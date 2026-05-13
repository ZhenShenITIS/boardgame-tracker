package itis.boardgametracker.repository

import itis.boardgametracker.model.RefreshToken
import itis.boardgametracker.util.RefreshTokenRowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.Types
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Repository
class RefreshTokenRepository(
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate
) {
    private val insertSql: String = """
        INSERT INTO refresh_tokens (user_id, token_hash, expires_at, revoked, created_at)
        VALUES (:userId, :tokenHash, :expiresAt, :revoked, :createdAt)
        RETURNING id, user_id, token_hash, expires_at, revoked, created_at
    """.trimIndent()

    private val findByTokenHashSql: String = """
        SELECT id, user_id, token_hash, expires_at, revoked, created_at
        FROM refresh_tokens
        WHERE token_hash = :tokenHash
    """.trimIndent()

    private val findByTokenHashAndUserIdSql: String = """
        SELECT id, user_id, token_hash, expires_at, revoked, created_at
        FROM refresh_tokens
        WHERE token_hash = :tokenHash AND user_id = :userId
    """.trimIndent()

    private val revokeByTokenHashSql: String = """
        UPDATE refresh_tokens
        SET revoked = TRUE
        WHERE token_hash = :tokenHash
    """.trimIndent()

    private val revokeAllByUserIdSql: String = """
        UPDATE refresh_tokens
        SET revoked = TRUE
        WHERE user_id = :userId AND revoked = FALSE
    """.trimIndent()

    private val deleteExpiredSql: String = """
        DELETE FROM refresh_tokens
        WHERE expires_at <= :now
    """.trimIndent()

    fun create(refreshToken: RefreshToken): RefreshToken {
        return namedParameterJdbcTemplate.queryForObject(
            insertSql,
            map(refreshToken),
            RefreshTokenRowMapper
        )!!
    }

    fun findByTokenHash(tokenHash: String): RefreshToken {
        return namedParameterJdbcTemplate.queryForObject(
            findByTokenHashSql,
            MapSqlParameterSource().addValue("tokenHash", tokenHash),
            RefreshTokenRowMapper
        )!!
    }

    fun findByTokenHashAndUserId(tokenHash: String, userId: Long): RefreshToken {
        return namedParameterJdbcTemplate.queryForObject(
            findByTokenHashAndUserIdSql,
            MapSqlParameterSource()
                .addValue("tokenHash", tokenHash)
                .addValue("userId", userId),
            RefreshTokenRowMapper
        )!!
    }

    fun revokeByTokenHash(tokenHash: String): Int {
        return namedParameterJdbcTemplate.update(
            revokeByTokenHashSql,
            MapSqlParameterSource().addValue("tokenHash", tokenHash)
        )
    }

    fun revokeAllByUserId(userId: Long): Int {
        return namedParameterJdbcTemplate.update(
            revokeAllByUserIdSql,
            MapSqlParameterSource().addValue("userId", userId)
        )
    }

    fun deleteExpired(now: Instant): Int {
        return namedParameterJdbcTemplate.update(
            deleteExpiredSql,
            MapSqlParameterSource()
                .addValue("now", OffsetDateTime.ofInstant(now, ZoneOffset.UTC), Types.TIMESTAMP_WITH_TIMEZONE)
        )
    }

    private fun map(refreshToken: RefreshToken): MapSqlParameterSource {
        return MapSqlParameterSource()
            .addValue("userId", refreshToken.userId, Types.BIGINT)
            .addValue("tokenHash", refreshToken.tokenHash, Types.VARCHAR)
            .addValue(
                "expiresAt",
                OffsetDateTime.ofInstant(refreshToken.expiresAt, ZoneOffset.UTC),
                Types.TIMESTAMP_WITH_TIMEZONE
            )
            .addValue("revoked", refreshToken.revoked, Types.BOOLEAN)
            .addValue(
                "createdAt",
                OffsetDateTime.ofInstant(refreshToken.createdAt, ZoneOffset.UTC),
                Types.TIMESTAMP_WITH_TIMEZONE
            )
    }
}
