package itis.boardgametracker.model

import java.time.Instant

data class PlaySession(
    val id: Long? = null,
    val collectionItemId: Long,
    val dateStart: Instant = Instant.now(),
    val dateEnd: Instant? = null,
    val comment: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)