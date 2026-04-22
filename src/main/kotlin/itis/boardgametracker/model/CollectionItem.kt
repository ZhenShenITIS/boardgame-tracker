package itis.boardgametracker.model

import itis.boardgametracker.constant.CollectionItemStatus
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

data class CollectionItem(
    val id: Long? = null,
    val userId: Long,
    val boardGame: BoardGame,
    val datePurchased: LocalDate? = null,
    val sumInRubles: BigDecimal? = null,
    val status: CollectionItemStatus = CollectionItemStatus.IN_COLLECTION,
    val playCount: Int = 0,
    val playSessions: List<PlaySession> = emptyList(),
    val comment: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)