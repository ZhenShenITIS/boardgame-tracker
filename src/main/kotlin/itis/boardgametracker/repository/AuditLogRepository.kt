package itis.boardgametracker.repository

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import itis.boardgametracker.model.AuditLog
import itis.boardgametracker.util.AuditLogRowMapper
import org.postgresql.util.PGobject
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.Types
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Repository
class AuditLogRepository(
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
    private val objectMapper: ObjectMapper
) {
    private val insertSql: String = """
        INSERT INTO audit_logs (user_id, action, entity_type, entity_id, details, ip_address, user_agent, created_at)
        VALUES (:userId, :action, :entityType, :entityId, :details, :ipAddress, :userAgent, :createdAt)
        RETURNING id, user_id, action, entity_type, entity_id, details, ip_address, user_agent, created_at
    """.trimIndent()

    fun create(auditLog: AuditLog): AuditLog {
        return namedParameterJdbcTemplate.queryForObject(
            insertSql,
            map(auditLog),
            AuditLogRowMapper(objectMapper)
        )!!
    }

    private fun map(auditLog: AuditLog): MapSqlParameterSource {
        return MapSqlParameterSource()
            .addValue("userId", auditLog.userId, Types.BIGINT)
            .addValue("action", auditLog.action, Types.VARCHAR)
            .addValue("entityType", auditLog.entityType, Types.VARCHAR)
            .addValue("entityId", auditLog.entityId, Types.BIGINT)
            .addValue("details", toJsonb(auditLog.details), Types.OTHER)
            .addValue("ipAddress", auditLog.ipAddress?.hostAddress, Types.OTHER)
            .addValue("userAgent", auditLog.userAgent, Types.VARCHAR)
            .addValue(
                "createdAt",
                OffsetDateTime.ofInstant(auditLog.createdAt, ZoneOffset.UTC),
                Types.TIMESTAMP_WITH_TIMEZONE
            )
    }

    private fun toJsonb(details: JsonNode?): PGobject? {
        if (details == null) {
            return null
        }
        return PGobject().apply {
            type = "jsonb"
            value = objectMapper.writeValueAsString(details)
        }
    }
}
