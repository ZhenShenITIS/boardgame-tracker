package itis.boardgametracker.model

import java.time.Instant

data class Role(
    val id: Long? = null,
    val name: String,
    val createdAt: Instant = Instant.now()
)