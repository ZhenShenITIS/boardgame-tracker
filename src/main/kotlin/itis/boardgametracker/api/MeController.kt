package itis.boardgametracker.api

import itis.boardgametracker.api.dto.UserCollectionStats
import itis.boardgametracker.security.CurrentUserProvider
import itis.boardgametracker.service.CollectionItemService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class MeController(
    private val collectionItemService: CollectionItemService,
    private val currentUserProvider: CurrentUserProvider
) : MeApi {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun meStatsGet(): ResponseEntity<UserCollectionStats> {
        val currentUser = currentUserProvider.currentUser()

        log.atInfo()
            .addKeyValue("userId", currentUser.userId)
            .log("Received request to get current user collection stats")

        val stats = collectionItemService.getUserCollectionStats(currentUser)
        return ResponseEntity.ok(stats)
    }
}
