package itis.boardgametracker.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.security.jwt")
data class JwtProperties(
    val issuer: String = "boardgame-tracker",
    val accessTokenTtlSeconds: Long = 3600,
    val refreshTokenTtlSeconds: Long = 2_592_000,
    val secret: String = "change-me-please-change-me-please"
)
