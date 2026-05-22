package itis.boardgametracker.service

import io.micrometer.core.annotation.Timed
import itis.boardgametracker.api.dto.CreatePlaySessionRequest
import itis.boardgametracker.api.dto.Pagination
import itis.boardgametracker.api.dto.PlaySessionList
import itis.boardgametracker.api.dto.QuickPlaySessionRequest
import itis.boardgametracker.api.dto.UpdatePlaySessionRequest
import itis.boardgametracker.config.CacheCatalog
import itis.boardgametracker.exception.BadRequestException
import itis.boardgametracker.exception.NotFoundException
import itis.boardgametracker.mapper.PlaySessionMapper
import itis.boardgametracker.repository.CollectionItemRepository
import itis.boardgametracker.repository.PlaySessionRepository
import itis.boardgametracker.security.CurrentUserPrincipal
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.CacheEvict
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class PlaySessionService(
    private val playSessionRepository: PlaySessionRepository,
    private val collectionItemRepository: CollectionItemRepository,
    private val shelfOfShameMetricsService: ShelfOfShameMetricsService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    object MetricsCatalog {
        const val PLAY_SESSION_GET_LIST_TIME = "play_sessions.get_list.time"
        const val PLAY_SESSION_GET_TIME = "play_sessions.get.time"
        const val PLAY_SESSION_CREATE_TIME = "play_sessions.create.time"
        const val PLAY_SESSION_QUICK_CREATE_TIME = "play_sessions.quick_create.time"
        const val PLAY_SESSION_UPDATE_TIME = "play_sessions.update.time"
        const val PLAY_SESSION_DELETE_TIME = "play_sessions.delete.time"
    }

    @Timed(value = MetricsCatalog.PLAY_SESSION_GET_LIST_TIME, description = "Время получения списка игровых сессий")
    fun getPlaySessions(
        collectionItemId: Long,
        page: Int,
        limit: Int,
        currentUser: CurrentUserPrincipal
    ): PlaySessionList {
        ensureCollectionItemOwnership(collectionItemId, currentUser.userId)

        val playSessionRecords = playSessionRepository.findByCollectionItemIdAndUserId(
            collectionItemId = collectionItemId,
            userId = currentUser.userId,
            limit = limit,
            offset = (page - 1) * limit
        )
        val totalCount = playSessionRepository.countByCollectionItemIdAndUserId(
            collectionItemId = collectionItemId,
            userId = currentUser.userId
        )

        log.atInfo()
            .addKeyValue("userId", currentUser.userId)
            .addKeyValue("collectionItemId", collectionItemId)
            .addKeyValue("resultCount", playSessionRecords.size)
            .log("Play sessions fetched")

        return PlaySessionList(
            data = playSessionRecords.map { PlaySessionMapper.map(it) },
            pagination = Pagination(
                page = page,
                limit = limit,
                total = totalCount,
                totalPages = (totalCount + limit - 1) / limit
            )
        )
    }

    @Timed(value = MetricsCatalog.PLAY_SESSION_GET_TIME, description = "Время получения игровой сессии")
    fun getPlaySessionById(id: Long, currentUser: CurrentUserPrincipal): itis.boardgametracker.api.dto.PlaySession {
        val playSessionRecord = playSessionRepository.findByIdAndUserId(id, currentUser.userId)
            ?: throw NotFoundException()

        log.atInfo()
            .addKeyValue("userId", currentUser.userId)
            .addKeyValue("playSessionId", id)
            .log("Play session fetched")

        return PlaySessionMapper.map(playSessionRecord)
    }

    @Transactional
    @Timed(value = MetricsCatalog.PLAY_SESSION_CREATE_TIME, description = "Время создания игровой сессии")
    @CacheEvict(cacheNames = [CacheCatalog.USER_COLLECTION_STATS], key = "#currentUser.userId")
    fun createPlaySession(
        collectionItemId: Long,
        createPlaySessionRequest: CreatePlaySessionRequest,
        currentUser: CurrentUserPrincipal
    ): itis.boardgametracker.api.dto.PlaySession {
        ensureCollectionItemOwnership(collectionItemId, currentUser.userId)

        val dateStart = createPlaySessionRequest.dateStart.toInstant()
        val dateEnd = createPlaySessionRequest.dateEnd?.toInstant()
        validateDateRange(dateStart, dateEnd)

        val createdPlaySession = playSessionRepository.create(
            collectionItemId = collectionItemId,
            dateStart = dateStart,
            dateEnd = dateEnd,
            comment = createPlaySessionRequest.comment
        )

        log.atInfo()
            .addKeyValue("userId", currentUser.userId)
            .addKeyValue("collectionItemId", collectionItemId)
            .addKeyValue("playSessionId", createdPlaySession.id)
            .log("Play session created")

        shelfOfShameMetricsService.refreshShelfOfShameSize()

        return PlaySessionMapper.map(createdPlaySession)
    }

    @Transactional
    @Timed(value = MetricsCatalog.PLAY_SESSION_QUICK_CREATE_TIME, description = "Время быстрого создания игровой сессии")
    @CacheEvict(cacheNames = [CacheCatalog.USER_COLLECTION_STATS], key = "#currentUser.userId")
    fun quickCreatePlaySession(
        collectionItemId: Long,
        quickPlaySessionRequest: QuickPlaySessionRequest?,
        currentUser: CurrentUserPrincipal
    ): itis.boardgametracker.api.dto.PlaySession {
        ensureCollectionItemOwnership(collectionItemId, currentUser.userId)

        val createdPlaySession = playSessionRepository.create(
            collectionItemId = collectionItemId,
            dateStart = Instant.now(),
            dateEnd = null,
            comment = quickPlaySessionRequest?.comment
        )

        log.atInfo()
            .addKeyValue("userId", currentUser.userId)
            .addKeyValue("collectionItemId", collectionItemId)
            .addKeyValue("playSessionId", createdPlaySession.id)
            .log("Quick play session created")

        shelfOfShameMetricsService.refreshShelfOfShameSize()

        return PlaySessionMapper.map(createdPlaySession)
    }

    @Transactional
    @Timed(value = MetricsCatalog.PLAY_SESSION_UPDATE_TIME, description = "Время обновления игровой сессии")
    fun updatePlaySessionById(
        id: Long,
        updatePlaySessionRequest: UpdatePlaySessionRequest,
        currentUser: CurrentUserPrincipal
    ): itis.boardgametracker.api.dto.PlaySession {
        val dateStart = updatePlaySessionRequest.dateStart.toInstant()
        val dateEnd = updatePlaySessionRequest.dateEnd?.toInstant()
        validateDateRange(dateStart, dateEnd)

        val updatedPlaySession = playSessionRepository.updateByIdAndUserId(
            id = id,
            userId = currentUser.userId,
            dateStart = dateStart,
            dateEnd = dateEnd,
            comment = updatePlaySessionRequest.comment
        ) ?: throw NotFoundException()

        log.atInfo()
            .addKeyValue("userId", currentUser.userId)
            .addKeyValue("playSessionId", id)
            .log("Play session updated")

        return PlaySessionMapper.map(updatedPlaySession)
    }

    @Transactional
    @Timed(value = MetricsCatalog.PLAY_SESSION_DELETE_TIME, description = "Время удаления игровой сессии")
    @CacheEvict(cacheNames = [CacheCatalog.USER_COLLECTION_STATS], key = "#currentUser.userId")
    fun deletePlaySessionById(id: Long, currentUser: CurrentUserPrincipal) {
        val deletedRows = playSessionRepository.deleteByIdAndUserId(id, currentUser.userId)
        if (deletedRows == 0) {
            throw NotFoundException()
        }

        log.atInfo()
            .addKeyValue("userId", currentUser.userId)
            .addKeyValue("playSessionId", id)
            .log("Play session deleted")

        shelfOfShameMetricsService.refreshShelfOfShameSize()
    }

    private fun ensureCollectionItemOwnership(collectionItemId: Long, userId: Long) {
        collectionItemRepository.findByIdAndUserId(collectionItemId, userId) ?: throw NotFoundException()
    }

    private fun validateDateRange(dateStart: Instant, dateEnd: Instant?) {
        if (dateEnd != null && dateEnd.isBefore(dateStart)) {
            throw BadRequestException("dateEnd must be greater than or equal to dateStart")
        }
    }
}
