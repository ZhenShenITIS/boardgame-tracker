package itis.boardgametracker.api

import itis.boardgametracker.api.dto.CreatePlaySessionRequest
import itis.boardgametracker.api.dto.PlaySession
import itis.boardgametracker.api.dto.PlaySessionList
import itis.boardgametracker.api.dto.QuickPlaySessionRequest
import itis.boardgametracker.api.dto.UpdatePlaySessionRequest
import itis.boardgametracker.security.CurrentUserProvider
import itis.boardgametracker.service.PlaySessionService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class PlaySessionController(
    private val playSessionService: PlaySessionService,
    private val currentUserProvider: CurrentUserProvider
) : PlaySessionsApi {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun collectionItemsCollectionItemIdPlaySessionsGet(
        collectionItemId: Long,
        page: Int,
        limit: Int
    ): ResponseEntity<PlaySessionList> {
        val currentUser = currentUserProvider.currentUser()

        log.atInfo()
            .addKeyValue("userId", currentUser.userId)
            .addKeyValue("collectionItemId", collectionItemId)
            .addKeyValue("page", page)
            .addKeyValue("limit", limit)
            .log("Received request to get play sessions")

        val playSessionList = playSessionService.getPlaySessions(
            collectionItemId = collectionItemId,
            page = page,
            limit = limit,
            currentUser = currentUser
        )
        return ResponseEntity.ok(playSessionList)
    }

    override fun collectionItemsCollectionItemIdPlaySessionsPost(
        collectionItemId: Long,
        createPlaySessionRequest: CreatePlaySessionRequest
    ): ResponseEntity<PlaySession> {
        val currentUser = currentUserProvider.currentUser()

        log.atInfo()
            .addKeyValue("userId", currentUser.userId)
            .addKeyValue("collectionItemId", collectionItemId)
            .log("Received request to create play session")

        val createdPlaySession = playSessionService.createPlaySession(
            collectionItemId = collectionItemId,
            createPlaySessionRequest = createPlaySessionRequest,
            currentUser = currentUser
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(createdPlaySession)
    }

    override fun collectionItemsCollectionItemIdPlaySessionsQuickPost(
        collectionItemId: Long,
        quickPlaySessionRequest: QuickPlaySessionRequest?
    ): ResponseEntity<PlaySession> {
        val currentUser = currentUserProvider.currentUser()

        log.atInfo()
            .addKeyValue("userId", currentUser.userId)
            .addKeyValue("collectionItemId", collectionItemId)
            .log("Received request to quick create play session")

        val createdPlaySession = playSessionService.quickCreatePlaySession(
            collectionItemId = collectionItemId,
            quickPlaySessionRequest = quickPlaySessionRequest,
            currentUser = currentUser
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(createdPlaySession)
    }

    override fun playSessionsIdDelete(id: Long): ResponseEntity<Unit> {
        val currentUser = currentUserProvider.currentUser()

        log.atInfo()
            .addKeyValue("userId", currentUser.userId)
            .addKeyValue("playSessionId", id)
            .log("Received request to delete play session")

        playSessionService.deletePlaySessionById(id, currentUser)
        return ResponseEntity.noContent().build()
    }

    override fun playSessionsIdGet(id: Long): ResponseEntity<PlaySession> {
        val currentUser = currentUserProvider.currentUser()

        log.atInfo()
            .addKeyValue("userId", currentUser.userId)
            .addKeyValue("playSessionId", id)
            .log("Received request to get play session")

        val playSession = playSessionService.getPlaySessionById(id, currentUser)
        return ResponseEntity.ok(playSession)
    }

    override fun playSessionsIdPut(
        id: Long,
        updatePlaySessionRequest: UpdatePlaySessionRequest
    ): ResponseEntity<PlaySession> {
        val currentUser = currentUserProvider.currentUser()

        log.atInfo()
            .addKeyValue("userId", currentUser.userId)
            .addKeyValue("playSessionId", id)
            .log("Received request to update play session")

        val updatedPlaySession = playSessionService.updatePlaySessionById(
            id = id,
            updatePlaySessionRequest = updatePlaySessionRequest,
            currentUser = currentUser
        )
        return ResponseEntity.ok(updatedPlaySession)
    }
}
