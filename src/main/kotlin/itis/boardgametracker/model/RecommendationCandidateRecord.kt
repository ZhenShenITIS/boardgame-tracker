package itis.boardgametracker.model

import itis.boardgametracker.constant.CollectionItemStatus

data class RecommendationCandidateRecord(
    val collectionItemId: Long,
    val playCount: Int,
    val status: CollectionItemStatus,
    val boardGame: BoardGame
)
