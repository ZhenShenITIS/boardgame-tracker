package itis.boardgametracker.model

import java.time.Instant

data class RefreshToken(
    val id: Long? = null,
    val userId: Long,
    val tokenHash: String,
    val expiresAt: Instant,
    val revoked: Boolean = false,
    val createdAt: Instant = Instant.now()
)