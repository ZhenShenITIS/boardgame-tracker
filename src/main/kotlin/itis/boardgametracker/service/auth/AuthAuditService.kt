package itis.boardgametracker.service.auth

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import itis.boardgametracker.model.AuditLog
import itis.boardgametracker.repository.AuditLogRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class AuthAuditService(
    private val auditLogRepository: AuditLogRepository,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun write(
        action: String,
        userId: Long? = null,
        context: AuthContext? = null,
        details: Map<String, Any?> = emptyMap()
    ) {
        runCatching {
            auditLogRepository.create(
                AuditLog(
                    userId = userId,
                    action = action,
                    entityType = "auth",
                    details = toJson(details, context?.details),
                    ipAddress = context?.ipAddress,
                    userAgent = context?.userAgent
                )
            )
        }.onFailure { throwable ->
            log.atError()
                .setCause(throwable)
                .addKeyValue("action", action)
                .addKeyValue("userId", userId)
                .log("Failed to write auth audit record")
        }
    }

    private fun toJson(details: Map<String, Any?>, fallback: JsonNode?): JsonNode? {
        if (details.isNotEmpty()) {
            return objectMapper.valueToTree(details)
        }
        return fallback
    }
}
