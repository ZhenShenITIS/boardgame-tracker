package itis.boardgametracker.model

import java.time.Instant

data class PlaySessionRecord(
    val id: Long,
    val collectionItemId: Long,
    val dateStart: Instant,
    val dateEnd: Instant?,
    val comment: String?,
    val createdAt: Instant,
    val updatedAt: Instant
)
