package itis.boardgametracker.model

import com.fasterxml.jackson.databind.JsonNode
import java.net.InetAddress
import java.time.Instant

data class AuditLog(
    val id: Long? = null,
    val userId: Long? = null,
    val action: String,
    val entityType: String? = null,
    val entityId: Long? = null,
    val details: JsonNode? = null,
    val ipAddress: InetAddress? = null,
    val userAgent: String? = null,
    val createdAt: Instant = Instant.now()
)