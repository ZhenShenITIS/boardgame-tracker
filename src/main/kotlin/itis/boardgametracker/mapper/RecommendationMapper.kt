package itis.boardgametracker.mapper

import itis.boardgametracker.api.dto.BoardGame
import itis.boardgametracker.api.dto.GameRecommendation
import itis.boardgametracker.model.RecommendationCandidateRecord

object RecommendationMapper {
    fun map(
        candidate: RecommendationCandidateRecord,
        boardGame: BoardGame,
        score: Double,
        reasons: List<String>
    ): GameRecommendation {
        return GameRecommendation(
            collectionItemId = candidate.collectionItemId,
            boardGame = boardGame,
            playCount = candidate.playCount,
            status = CollectionItemMapper.map(candidate.status),
            score = score,
            reasons = reasons
        )
    }
}
