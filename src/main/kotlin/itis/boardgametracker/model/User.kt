package itis.boardgametracker.model

import java.time.Instant

data class User(
    val id: Long? = null,
    val name: String,
    val email: String,
    val password: String,
    val roles: List<Role> = emptyList(),
    val createdAt: Instant = Instant.now()
)