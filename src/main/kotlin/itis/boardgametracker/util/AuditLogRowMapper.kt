package itis.boardgametracker.util

import com.fasterxml.jackson.databind.ObjectMapper
import itis.boardgametracker.model.AuditLog
import org.springframework.jdbc.core.RowMapper
import java.sql.ResultSet

class AuditLogRowMapper(
    private val objectMapper: ObjectMapper
) : RowMapper<AuditLog> {
    override fun mapRow(rs: ResultSet, rowNum: Int): AuditLog {
        return AuditLog(
            id = rs.getLong("id"),
            userId = rs.getNullableLong("user_id"),
            action = rs.getString("action"),
            entityType = rs.getNullableString("entity_type"),
            entityId = rs.getNullableLong("entity_id"),
            details = rs.getNullableString("details")?.let(objectMapper::readTree),
            ipAddress = rs.getInetAddress("ip_address"),
            userAgent = rs.getNullableString("user_agent"),
            createdAt = rs.getInstant("created_at")
        )
    }
}
