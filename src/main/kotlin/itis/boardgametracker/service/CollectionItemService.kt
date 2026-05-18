package itis.boardgametracker.service

import io.micrometer.core.annotation.Timed
import itis.boardgametracker.api.dto.CollectionItemList
import itis.boardgametracker.api.dto.CollectionItemStatus
import itis.boardgametracker.api.dto.CreateCustomGameCollectionItemRequest
import itis.boardgametracker.api.dto.CreateCollectionItemRequest
import itis.boardgametracker.api.dto.Pagination
import itis.boardgametracker.api.dto.ShelfOfShameList
import itis.boardgametracker.api.dto.UpdateCollectionItemRequest
import itis.boardgametracker.api.dto.UserCollectionStats
import itis.boardgametracker.constant.BoardGameType
import itis.boardgametracker.exception.NotFoundException
import itis.boardgametracker.mapper.BoardGameMapper
import itis.boardgametracker.mapper.CollectionItemMapper
import itis.boardgametracker.model.BoardGame
import itis.boardgametracker.repository.BoardGameRepository
import itis.boardgametracker.repository.CollectionItemRepository
import itis.boardgametracker.security.CurrentUserPrincipal
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode

@Service
class CollectionItemService(
    private val collectionItemRepository: CollectionItemRepository,
    private val boardGameRepository: BoardGameRepository,
    private val boardGameService: BoardGameService,
    private val shelfOfShameMetricsService: ShelfOfShameMetricsService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    object MetricsCatalog {
        const val COLLECTION_ITEM_GET_LIST_TIME = "collection_items.get_list.time"
        const val COLLECTION_ITEM_GET_TIME = "collection_items.get.time"
        const val COLLECTION_ITEM_CREATE_TIME = "collection_items.create.time"
        const val COLLECTION_ITEM_CREATE_WITH_CUSTOM_GAME_TIME = "collection_items.create_with_custom_game.time"
        const val COLLECTION_ITEM_UPDATE_TIME = "collection_items.update.time"
        const val COLLECTION_ITEM_DELETE_TIME = "collection_items.delete.time"
        const val COLLECTION_ITEM_SHELF_OF_SHAME_GET_LIST_TIME = "collection_items.shelf_of_shame.get_list.time"
        const val USER_COLLECTION_STATS_GET_TIME = "user_collection_stats.get.time"
    }

    @Timed(value = MetricsCatalog.COLLECTION_ITEM_GET_LIST_TIME, description = "Время получения списка элементов коллекции")
    fun getCollectionItems(
        status: CollectionItemStatus?,
        shelfOfShame: Boolean?,
        page: Int,
        limit: Int,
        currentUser: CurrentUserPrincipal
    ): CollectionItemList {
        val statusFilter = status?.let { CollectionItemMapper.map(it) }
        val shelfOfShameOnly = shelfOfShame == true

        val collectionItemRecords = collectionItemRepository.findByUserIdWithFilters(
            userId = currentUser.userId,
            status = statusFilter,
            shelfOfShameOnly = shelfOfShameOnly,
            limit = limit,
            offset = (page - 1) * limit
        )
        val totalCount = collectionItemRepository.countByUserIdWithFilters(
            userId = currentUser.userId,
            status = statusFilter,
            shelfOfShameOnly = shelfOfShameOnly
        )

        val data = collectionItemRecords.map { collectionItemRecord ->
            val boardGame = boardGameService.getBoardGameById(collectionItemRecord.boardGameId, currentUser)
            CollectionItemMapper.map(collectionItemRecord, boardGame)
        }

        log.atInfo()
            .addKeyValue("userId", currentUser.userId)
            .addKeyValue("status", statusFilter?.name)
            .addKeyValue("shelfOfShameOnly", shelfOfShameOnly)
            .addKeyValue("resultCount", data.size)
            .log("Collection items fetched")

        return CollectionItemList(
            data = data,
            pagination = Pagination(
                page = page,
                limit = limit,
                total = totalCount,
                totalPages = (totalCount + limit - 1) / limit
            )
        )
    }

    @Timed(
        value = MetricsCatalog.COLLECTION_ITEM_SHELF_OF_SHAME_GET_LIST_TIME,
        description = "Время получения полки позора пользователя"
    )
    fun getShelfOfShame(
        page: Int,
        limit: Int,
        currentUser: CurrentUserPrincipal
    ): ShelfOfShameList {
        val shelfItems = collectionItemRepository.findByUserIdWithFilters(
            userId = currentUser.userId,
            status = null,
            shelfOfShameOnly = true,
            limit = limit,
            offset = (page - 1) * limit
        )
        val totalCount = collectionItemRepository.countByUserIdWithFilters(
            userId = currentUser.userId,
            status = null,
            shelfOfShameOnly = true
        )
        val data = shelfItems.map { collectionItemRecord ->
            val boardGame = boardGameService.getBoardGameById(collectionItemRecord.boardGameId, currentUser)
            CollectionItemMapper.map(collectionItemRecord, boardGame)
        }

        log.atInfo()
            .addKeyValue("userId", currentUser.userId)
            .addKeyValue("resultCount", data.size)
            .log("Shelf of shame fetched")

        return ShelfOfShameList(
            data = data,
            pagination = Pagination(
                page = page,
                limit = limit,
                total = totalCount,
                totalPages = (totalCount + limit - 1) / limit
            )
        )
    }

    @Timed(value = MetricsCatalog.USER_COLLECTION_STATS_GET_TIME, description = "Время получения статистики коллекции пользователя")
    fun getUserCollectionStats(currentUser: CurrentUserPrincipal): UserCollectionStats {
        val stats = collectionItemRepository.getUserCollectionStats(currentUser.userId)
        val playedPercent = if (stats.totalItems == 0) {
            BigDecimal.ZERO
        } else {
            BigDecimal(stats.playedItems)
                .multiply(BigDecimal(100))
                .divide(BigDecimal(stats.totalItems), 2, RoundingMode.HALF_UP)
        }

        log.atInfo()
            .addKeyValue("userId", currentUser.userId)
            .addKeyValue("totalItems", stats.totalItems)
            .addKeyValue("playedItems", stats.playedItems)
            .addKeyValue("unplayedItems", stats.unplayedItems)
            .log("User collection stats fetched")

        return UserCollectionStats(
            totalItems = stats.totalItems,
            playedItems = stats.playedItems,
            unplayedItems = stats.unplayedItems,
            shelfOfShameCost = stats.shelfOfShameCost,
            playedPercent = playedPercent
        )
    }

    @Timed(value = MetricsCatalog.COLLECTION_ITEM_GET_TIME, description = "Время получения элемента коллекции")
    fun getCollectionItemById(
        id: Long,
        currentUser: CurrentUserPrincipal
    ): itis.boardgametracker.api.dto.CollectionItem {
        val collectionItemRecord = collectionItemRepository.findByIdAndUserId(id, currentUser.userId)
            ?: throw NotFoundException()
        val boardGame = boardGameService.getBoardGameById(collectionItemRecord.boardGameId, currentUser)
        return CollectionItemMapper.map(collectionItemRecord, boardGame)
    }

    @Transactional
    @Timed(value = MetricsCatalog.COLLECTION_ITEM_CREATE_TIME, description = "Время создания элемента коллекции")
    fun createCollectionItem(
        createCollectionItemRequest: CreateCollectionItemRequest,
        currentUser: CurrentUserPrincipal
    ): itis.boardgametracker.api.dto.CollectionItem {
        val boardGame = boardGameService.getBoardGameById(createCollectionItemRequest.boardGameId, currentUser)
        val status = CollectionItemMapper.map(createCollectionItemRequest.status)

        val createdCollectionItem = collectionItemRepository.create(
            userId = currentUser.userId,
            boardGameId = createCollectionItemRequest.boardGameId,
            datePurchased = createCollectionItemRequest.datePurchased,
            sumInRubles = createCollectionItemRequest.sumInRubles,
            status = status,
            comment = createCollectionItemRequest.comment
        )

        log.atInfo()
            .addKeyValue("userId", currentUser.userId)
            .addKeyValue("collectionItemId", createdCollectionItem.id)
            .addKeyValue("boardGameId", createdCollectionItem.boardGameId)
            .log("Collection item created")

        shelfOfShameMetricsService.refreshShelfOfShameSize()

        return CollectionItemMapper.map(createdCollectionItem, boardGame)
    }

    @Transactional
    @Timed(
        value = MetricsCatalog.COLLECTION_ITEM_CREATE_WITH_CUSTOM_GAME_TIME,
        description = "Время создания элемента коллекции с кастомной игрой"
    )
    fun createCollectionItemWithCustomGame(
        createCustomGameCollectionItemRequest: CreateCustomGameCollectionItemRequest,
        currentUser: CurrentUserPrincipal
    ): itis.boardgametracker.api.dto.CollectionItem {
        val customBoardGame = boardGameRepository.create(
            BoardGame(
                type = BoardGameType.BOARDGAME,
                originalName = createCustomGameCollectionItemRequest.originalName,
                displayName = createCustomGameCollectionItemRequest.displayName,
                minPlayers = createCustomGameCollectionItemRequest.minPlayers,
                maxPlayers = createCustomGameCollectionItemRequest.maxPlayers,
                playingTime = createCustomGameCollectionItemRequest.playingTime,
                minPlayTime = createCustomGameCollectionItemRequest.minPlayTime,
                maxPlayTime = createCustomGameCollectionItemRequest.maxPlayTime,
                minAge = createCustomGameCollectionItemRequest.minAge,
                yearPublished = createCustomGameCollectionItemRequest.yearPublished,
                isCustom = true,
                createdById = currentUser.userId,
                tags = createCustomGameCollectionItemRequest.tags
                    ?.map { tag -> BoardGameMapper.map(tag) }
                    ?: emptyList()
            )
        )

        val boardGameId = customBoardGame.id ?: throw IllegalStateException("Created custom board game id is required")
        val createdCollectionItem = collectionItemRepository.create(
            userId = currentUser.userId,
            boardGameId = boardGameId,
            datePurchased = createCustomGameCollectionItemRequest.datePurchased,
            sumInRubles = createCustomGameCollectionItemRequest.sumInRubles,
            status = CollectionItemMapper.map(createCustomGameCollectionItemRequest.status),
            comment = createCustomGameCollectionItemRequest.comment
        )

        log.atInfo()
            .addKeyValue("userId", currentUser.userId)
            .addKeyValue("collectionItemId", createdCollectionItem.id)
            .addKeyValue("boardGameId", boardGameId)
            .log("Collection item created with custom game")

        shelfOfShameMetricsService.refreshShelfOfShameSize()
        return CollectionItemMapper.map(createdCollectionItem, boardGameService.map(customBoardGame))
    }

    @Transactional
    @Timed(value = MetricsCatalog.COLLECTION_ITEM_UPDATE_TIME, description = "Время обновления элемента коллекции")
    fun updateCollectionItemById(
        id: Long,
        updateCollectionItemRequest: UpdateCollectionItemRequest,
        currentUser: CurrentUserPrincipal
    ): itis.boardgametracker.api.dto.CollectionItem {
        val existingCollectionItem = collectionItemRepository.findByIdAndUserId(id, currentUser.userId)
            ?: throw NotFoundException()
        val boardGame = boardGameService.getBoardGameById(updateCollectionItemRequest.boardGameId, currentUser)
        val status = CollectionItemMapper.map(updateCollectionItemRequest.status)

        val updatedCollectionItem = collectionItemRepository.update(
            id = existingCollectionItem.id,
            userId = currentUser.userId,
            boardGameId = updateCollectionItemRequest.boardGameId,
            datePurchased = updateCollectionItemRequest.datePurchased,
            sumInRubles = updateCollectionItemRequest.sumInRubles,
            status = status,
            comment = updateCollectionItemRequest.comment
        ) ?: throw NotFoundException()

        log.atInfo()
            .addKeyValue("userId", currentUser.userId)
            .addKeyValue("collectionItemId", updatedCollectionItem.id)
            .log("Collection item updated")

        shelfOfShameMetricsService.refreshShelfOfShameSize()

        return CollectionItemMapper.map(updatedCollectionItem, boardGame)
    }

    @Transactional
    @Timed(value = MetricsCatalog.COLLECTION_ITEM_DELETE_TIME, description = "Время удаления элемента коллекции")
    fun deleteCollectionItemById(
        id: Long,
        currentUser: CurrentUserPrincipal
    ) {
        val deletedRows = collectionItemRepository.deleteByIdAndUserId(id, currentUser.userId)
        if (deletedRows == 0) {
            throw NotFoundException()
        }

        log.atInfo()
            .addKeyValue("userId", currentUser.userId)
            .addKeyValue("collectionItemId", id)
            .log("Collection item deleted")

        shelfOfShameMetricsService.refreshShelfOfShameSize()
    }
}
