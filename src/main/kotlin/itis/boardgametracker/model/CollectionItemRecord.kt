package itis.boardgametracker.model

import itis.boardgametracker.constant.CollectionItemStatus
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

data class CollectionItemRecord(
    val id: Long,
    val userId: Long,
    val boardGameId: Long,
    val datePurchased: LocalDate?,
    val sumInRubles: BigDecimal?,
    val status: CollectionItemStatus,
    val playCount: Int,
    val comment: String?,
    val createdAt: Instant,
    val updatedAt: Instant
)
