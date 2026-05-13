package itis.boardgametracker.service.auth

import com.fasterxml.jackson.databind.JsonNode
import java.net.InetAddress

data class AuthContext(
    val ipAddress: InetAddress? = null,
    val userAgent: String? = null,
    val details: JsonNode? = null
)
