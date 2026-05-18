package itis.boardgametracker.api

import itis.boardgametracker.api.dto.GameRecommendationList
import itis.boardgametracker.security.CurrentUserProvider
import itis.boardgametracker.service.RecommendationService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class RecommendationController(
    private val recommendationService: RecommendationService,
    private val currentUserProvider: CurrentUserProvider
) : RecommendationsApi {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun recommendationsTonightGet(
        playerCount: Int,
        maxPlayTimeMinutes: Int?,
        shelfOfShameOnly: Boolean,
        limit: Int
    ): ResponseEntity<GameRecommendationList> {
        val currentUser = currentUserProvider.currentUser()

        log.atInfo()
            .addKeyValue("userId", currentUser.userId)
            .addKeyValue("playerCount", playerCount)
            .addKeyValue("maxPlayTimeMinutes", maxPlayTimeMinutes)
            .addKeyValue("shelfOfShameOnly", shelfOfShameOnly)
            .addKeyValue("limit", limit)
            .log("Received request to get tonight recommendations")

        val recommendations = recommendationService.getTonightRecommendations(
            playerCount = playerCount,
            maxPlayTimeMinutes = maxPlayTimeMinutes,
            shelfOfShameOnly = shelfOfShameOnly,
            limit = limit,
            currentUser = currentUser
        )
        return ResponseEntity.ok(recommendations)
    }
}
