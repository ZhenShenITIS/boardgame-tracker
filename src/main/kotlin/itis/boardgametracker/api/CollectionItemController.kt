package itis.boardgametracker.api

import itis.boardgametracker.api.dto.CollectionItem
import itis.boardgametracker.api.dto.CollectionItemList
import itis.boardgametracker.api.dto.CollectionItemStatus
import itis.boardgametracker.api.dto.CreateCustomGameCollectionItemRequest
import itis.boardgametracker.api.dto.CreateCollectionItemRequest
import itis.boardgametracker.api.dto.ShelfOfShameList
import itis.boardgametracker.api.dto.UpdateCollectionItemRequest
import itis.boardgametracker.security.CurrentUserProvider
import itis.boardgametracker.service.CollectionItemService
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class CollectionItemController(
    private val collectionItemService: CollectionItemService,
    private val currentUserProvider: CurrentUserProvider
) : CollectionItemsApi {

    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping("/collection-items")
    override fun collectionItemsGet(
        @RequestParam(value = "status", required = false) status: CollectionItemStatus?,
        @RequestParam(value = "shelfOfShame", required = false) shelfOfShame: Boolean?,
        @RequestParam(value = "page", defaultValue = "1") page: Int,
        @RequestParam(value = "limit", defaultValue = "25") limit: Int
    ): ResponseEntity<CollectionItemList> {
        val currentUser = currentUserProvider.currentUser()
        log.atInfo()
            .addKeyValue("userId", currentUser.userId)
            .addKeyValue("status", status?.name)
            .addKeyValue("shelfOfShame", shelfOfShame)
            .addKeyValue("page", page)
            .addKeyValue("limit", limit)
            .log("Received request to get collection items")

        val collectionItemList = collectionItemService.getCollectionItems(
            status = status,
            shelfOfShame = shelfOfShame,
            page = page,
            limit = limit,
            currentUser = currentUser
        )
        return ResponseEntity.ok(collectionItemList)
    }

    @GetMapping("/collection-items/shelf-of-shame")
    override fun collectionItemsShelfOfShameGet(
        @RequestParam(value = "page", defaultValue = "1") page: Int,
        @RequestParam(value = "limit", defaultValue = "25") limit: Int
    ): ResponseEntity<ShelfOfShameList> {
        val currentUser = currentUserProvider.currentUser()
        log.atInfo()
            .addKeyValue("userId", currentUser.userId)
            .addKeyValue("page", page)
            .addKeyValue("limit", limit)
            .log("Received request to get shelf of shame")

        val shelfOfShameList = collectionItemService.getShelfOfShame(
            page = page,
            limit = limit,
            currentUser = currentUser
        )
        return ResponseEntity.ok(shelfOfShameList)
    }

    @DeleteMapping("/collection-items/{id}")
    override fun collectionItemsIdDelete(@PathVariable("id") id: Long): ResponseEntity<Unit> {
        val currentUser = currentUserProvider.currentUser()
        log.atInfo()
            .addKeyValue("userId", currentUser.userId)
            .addKeyValue("collectionItemId", id)
            .log("Received request to delete collection item")

        collectionItemService.deleteCollectionItemById(id, currentUser)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/collection-items/{id}")
    override fun collectionItemsIdGet(@PathVariable("id") id: Long): ResponseEntity<CollectionItem> {
        val currentUser = currentUserProvider.currentUser()
        log.atInfo()
            .addKeyValue("userId", currentUser.userId)
            .addKeyValue("collectionItemId", id)
            .log("Received request to get collection item by id")

        val collectionItem = collectionItemService.getCollectionItemById(id, currentUser)
        return ResponseEntity.ok(collectionItem)
    }

    @PutMapping("/collection-items/{id}")
    override fun collectionItemsIdPut(
        @PathVariable("id") id: Long,
        @Valid @RequestBody updateCollectionItemRequest: UpdateCollectionItemRequest
    ): ResponseEntity<CollectionItem> {
        val currentUser = currentUserProvider.currentUser()
        log.atInfo()
            .addKeyValue("userId", currentUser.userId)
            .addKeyValue("collectionItemId", id)
            .log("Received request to update collection item")

        val updatedCollectionItem = collectionItemService.updateCollectionItemById(
            id = id,
            updateCollectionItemRequest = updateCollectionItemRequest,
            currentUser = currentUser
        )
        return ResponseEntity.ok(updatedCollectionItem)
    }

    @PostMapping("/collection-items")
    override fun collectionItemsPost(@Valid @RequestBody createCollectionItemRequest: CreateCollectionItemRequest): ResponseEntity<CollectionItem> {
        val currentUser = currentUserProvider.currentUser()
        log.atInfo()
            .addKeyValue("userId", currentUser.userId)
            .log("Received request to create collection item")

        val createdCollectionItem = collectionItemService.createCollectionItem(createCollectionItemRequest, currentUser)
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCollectionItem)
    }

    @PostMapping("/collection-items/custom-game")
    override fun collectionItemsCustomGamePost(
        @Valid @RequestBody createCustomGameCollectionItemRequest: CreateCustomGameCollectionItemRequest
    ): ResponseEntity<CollectionItem> {
        val currentUser = currentUserProvider.currentUser()
        log.atInfo()
            .addKeyValue("userId", currentUser.userId)
            .addKeyValue("displayName", createCustomGameCollectionItemRequest.displayName)
            .log("Received request to create collection item with custom game")

        val createdCollectionItem = collectionItemService.createCollectionItemWithCustomGame(
            createCustomGameCollectionItemRequest = createCustomGameCollectionItemRequest,
            currentUser = currentUser
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCollectionItem)
    }
}
