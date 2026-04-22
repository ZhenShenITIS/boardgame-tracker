package itis.boardgametracker.model

import java.time.Instant

data class Tag(
    val id: Long? = null,
    val name: String,
    val description: String? = null,
    val createdAt: Instant = Instant.now()
)