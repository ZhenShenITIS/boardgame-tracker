package itis.boardgametracker.integration

import itis.boardgametracker.api.dto.CollectionItem
import itis.boardgametracker.api.dto.CollectionItemStatus
import itis.boardgametracker.api.dto.CreateCollectionItemRequest
import itis.boardgametracker.api.dto.UpdateCollectionItemRequest
import itis.boardgametracker.security.CurrentUserPrincipal
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CollectionItemCrudIntegrationTest : IntegrationTest() {

    @Autowired
    lateinit var mockMvc: MockMvc

    private var userId: Long = 0
    private var otherUserId: Long = 0
    private var boardGameId: Long = 0
    private var secondBoardGameId: Long = 0

    @BeforeEach
    fun setCurrentUser() {
        userId = testJdbcBoardGameRepository.createUser(uniqueEmail("collection-user"))
        otherUserId = testJdbcBoardGameRepository.createUser(uniqueEmail("collection-other"))
        boardGameId = testJdbcBoardGameRepository.createBoardGame(displayName = "Terraforming Mars")
        secondBoardGameId = testJdbcBoardGameRepository.createBoardGame(displayName = "Dune: Imperium")
        authenticate(userId, "ROLE_USER")
    }

    @Test
    fun createItem() {
        val createdItem = createCollectionItem(
            CreateCollectionItemRequest(
                boardGameId = boardGameId,
                datePurchased = LocalDate.parse("2026-05-16"),
                sumInRubles = BigDecimal("4990.00"),
                comment = "Купил на распродаже"
            )
        )

        assertNotNull(createdItem.id)
        assertEquals(userId, createdItem.userId)
        assertEquals(boardGameId, createdItem.boardGame.id)
        assertEquals(CollectionItemStatus.IN_COLLECTION, createdItem.status)
        assertEquals(0, createdItem.playCount)
        assertEquals(1L, testJdbcBoardGameRepository.countCollectionItemsByUser(userId))
    }

    @Test
    fun listOnlyCurrentUserItems() {
        val ownItem = createCollectionItem(
            CreateCollectionItemRequest(
                boardGameId = boardGameId,
                status = CollectionItemStatus.WISH_LIST
            )
        )
        testJdbcBoardGameRepository.createCollectionItem(userId = otherUserId, boardGameId = boardGameId)

        val response = mockMvc.perform(
            get("/collection-items")
                .param("page", "1")
                .param("limit", "10")
        )
            .andExpect(status().isOk)
            .andReturn()
            .let { objectMapper.readTree(it.response.contentAsString) }

        val ids = response["data"].map { it["id"].asLong() }.toSet()
        assertTrue(ids.contains(ownItem.id))
        assertEquals(1, response["data"].size())
        assertEquals(1, response["pagination"]["total"].asInt())
    }

    @Test
    fun getOwnItem() {
        val ownItem = createCollectionItem(
            CreateCollectionItemRequest(
                boardGameId = boardGameId,
                status = CollectionItemStatus.IN_COLLECTION
            )
        )

        val fetchedItem = mockMvc.perform(get("/collection-items/${ownItem.id}"))
            .andExpect(status().isOk)
            .andReturn()
            .let {
                objectMapper.readValue(it.response.contentAsString, CollectionItem::class.java)
            }

        assertEquals(ownItem.id, fetchedItem.id)
        assertEquals(userId, fetchedItem.userId)
    }

    @Test
    fun foreignItemReturns404() {
        val foreignItemId = testJdbcBoardGameRepository.createCollectionItem(userId = otherUserId, boardGameId = boardGameId)

        mockMvc.perform(get("/collection-items/$foreignItemId"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun updateOwnItem() {
        val ownItem = createCollectionItem(
            CreateCollectionItemRequest(
                boardGameId = boardGameId,
                status = CollectionItemStatus.WISH_LIST
            )
        )

        val updatedItem = mockMvc.perform(
            put("/collection-items/${ownItem.id}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        UpdateCollectionItemRequest(
                            boardGameId = secondBoardGameId,
                            status = CollectionItemStatus.SOLD,
                            sumInRubles = BigDecimal("4500.00"),
                            comment = "Продал другу"
                        )
                    )
                )
        )
            .andExpect(status().isOk)
            .andReturn()
            .let {
                objectMapper.readValue(it.response.contentAsString, CollectionItem::class.java)
            }

        assertEquals(CollectionItemStatus.SOLD, updatedItem.status)
        assertEquals(secondBoardGameId, updatedItem.boardGame.id)
        assertEquals("SOLD", testJdbcBoardGameRepository.findCollectionItemStatusById(ownItem.id))
        assertEquals(secondBoardGameId, testJdbcBoardGameRepository.findCollectionItemBoardGameIdById(ownItem.id))
    }

    @Test
    fun deleteOwnItem() {
        val ownItem = createCollectionItem(
            CreateCollectionItemRequest(
                boardGameId = boardGameId
            )
        )

        mockMvc.perform(delete("/collection-items/${ownItem.id}"))
            .andExpect(status().isNoContent)

        assertFalse(testJdbcBoardGameRepository.collectionItemExists(ownItem.id))
    }

    @Test
    fun deleteForeignItemReturns404() {
        val foreignItemId = testJdbcBoardGameRepository.createCollectionItem(userId = otherUserId, boardGameId = boardGameId)

        mockMvc.perform(delete("/collection-items/$foreignItemId"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun invalidBoardGameIdReturns404() {
        mockMvc.perform(
            post("/collection-items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        CreateCollectionItemRequest(
                            boardGameId = 999999L,
                            status = CollectionItemStatus.IN_COLLECTION
                        )
                    )
                )
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun negativeSumReturns400() {
        mockMvc.perform(
            post("/collection-items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        CreateCollectionItemRequest(
                            boardGameId = boardGameId,
                            sumInRubles = BigDecimal("-1")
                        )
                    )
                )
        )
            .andExpect(status().isBadRequest)
    }

    private fun createCollectionItem(request: CreateCollectionItemRequest): CollectionItem {
        return mockMvc.perform(
            post("/collection-items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andReturn()
            .let {
                objectMapper.readValue(it.response.contentAsString, CollectionItem::class.java)
            }
    }

    private fun authenticate(userId: Long, role: String) {
        SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken(
            CurrentUserPrincipal(
                userId = userId,
                email = "user-$userId@itis.com",
                roles = listOf(role)
            ),
            null,
            listOf(SimpleGrantedAuthority(role))
        )
    }

    private fun uniqueEmail(prefix: String): String {
        return "$prefix-${System.nanoTime()}@itis.com"
    }
}
