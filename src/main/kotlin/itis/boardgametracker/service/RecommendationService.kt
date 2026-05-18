package itis.boardgametracker.service

import io.micrometer.core.annotation.Timed
import itis.boardgametracker.api.dto.GameRecommendation
import itis.boardgametracker.api.dto.GameRecommendationList
import itis.boardgametracker.mapper.RecommendationMapper
import itis.boardgametracker.model.RecommendationCandidateRecord
import itis.boardgametracker.repository.RecommendationRepository
import itis.boardgametracker.security.CurrentUserPrincipal
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import kotlin.math.max

@Service
class RecommendationService(
    private val recommendationRepository: RecommendationRepository,
    private val boardGameService: BoardGameService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    object MetricsCatalog {
        const val RECOMMENDATIONS_TONIGHT_GET_TIME = "recommendations.tonight.get.time"
    }

    @Timed(value = MetricsCatalog.RECOMMENDATIONS_TONIGHT_GET_TIME, description = "Время подбора рекомендаций на вечер")
    fun getTonightRecommendations(
        playerCount: Int,
        maxPlayTimeMinutes: Int?,
        shelfOfShameOnly: Boolean,
        limit: Int,
        currentUser: CurrentUserPrincipal
    ): GameRecommendationList {
        val candidates = recommendationRepository.findTonightCandidates(
            userId = currentUser.userId,
            playerCount = playerCount,
            maxPlayTimeMinutes = maxPlayTimeMinutes,
            shelfOfShameOnly = shelfOfShameOnly
        )

        val recommendations = candidates
            .map { candidate ->
                val scoredCandidate = scoreCandidate(candidate, maxPlayTimeMinutes)
                RecommendationMapper.map(
                    candidate = candidate,
                    boardGame = boardGameService.map(candidate.boardGame),
                    score = scoredCandidate.score,
                    reasons = scoredCandidate.reasons
                )
            }
            .sortedWith(
                compareByDescending<GameRecommendation> { it.score }
                    .thenBy { it.playCount }
                    .thenBy { it.collectionItemId }
            )
            .take(limit)

        log.atInfo()
            .addKeyValue("userId", currentUser.userId)
            .addKeyValue("playerCount", playerCount)
            .addKeyValue("maxPlayTimeMinutes", maxPlayTimeMinutes)
            .addKeyValue("shelfOfShameOnly", shelfOfShameOnly)
            .addKeyValue("requestedLimit", limit)
            .addKeyValue("candidateCount", candidates.size)
            .addKeyValue("resultCount", recommendations.size)
            .log("Tonight recommendations prepared")

        return GameRecommendationList(data = recommendations)
    }

    private fun scoreCandidate(
        candidate: RecommendationCandidateRecord,
        maxPlayTimeMinutes: Int?
    ): ScoredCandidate {
        val reasons = mutableListOf<String>()
        var score = 0.0

        if (candidate.playCount == 0) {
            score += 1000.0
            reasons.add("shelf_of_shame_bonus")
        } else {
            reasons.add("already_played_lower_priority")
        }

        val minPlayers = candidate.boardGame.minPlayers
        val maxPlayers = candidate.boardGame.maxPlayers
        if (minPlayers == null && maxPlayers == null) {
            reasons.add("unknown_player_range_included")
        } else {
            reasons.add("matches_player_count")
        }

        if (maxPlayTimeMinutes != null) {
            val playingTime = candidate.boardGame.playingTime
            if (playingTime == null) {
                reasons.add("unknown_play_time_included")
            } else {
                val remainingMinutes = max(maxPlayTimeMinutes - playingTime, 0)
                val timeBonus = remainingMinutes.toDouble() / maxPlayTimeMinutes.toDouble() * 50.0
                score += timeBonus
                reasons.add(if (timeBonus > 0.0) "short_play_time_bonus" else "matches_play_time_limit")
            }
        }

        return ScoredCandidate(score = score, reasons = reasons)
    }

    private data class ScoredCandidate(
        val score: Double,
        val reasons: List<String>
    )
}
