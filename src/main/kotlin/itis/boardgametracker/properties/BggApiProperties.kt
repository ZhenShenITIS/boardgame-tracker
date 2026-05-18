package itis.boardgametracker.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "api.bgg")
data class BggApiProperties(
    val token: String,
    val baseUrl: String = "https://boardgamegeek.com/xmlapi2",
    val requestTimeoutSeconds: Long = 30,
    val minDelayMillis: Long = 5000,
    val retryMaxAttempts: Int = 5
)
