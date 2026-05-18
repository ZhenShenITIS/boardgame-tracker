package itis.boardgametracker.model

import java.math.BigDecimal

data class UserCollectionStatsAggregate(
    val totalItems: Int,
    val playedItems: Int,
    val unplayedItems: Int,
    val shelfOfShameCost: BigDecimal
)
