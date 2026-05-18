package itis.boardgametracker.integration

import com.fasterxml.jackson.databind.JsonNode
import itis.boardgametracker.api.dto.CreatePlaySessionRequest
import itis.boardgametracker.api.dto.QuickPlaySessionRequest
import itis.boardgametracker.security.CurrentUserPrincipal
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.OffsetDateTime
import kotlin.test.assertEquals

class ShelfOfShameStatsIntegrationTest : IntegrationTest() {

    @Autowired
    lateinit var mockMvc: MockMvc

    private var userId: Long = 0
    private var otherUserId: Long = 0
    private var boardGameId: Long = 0

    @BeforeEach
    fun setCurrentUser() {
        userId = testJdbcBoardGameRepository.createUser(uniqueEmail("shelf-user"))
        otherUserId = testJdbcBoardGameRepository.createUser(uniqueEmail("shelf-other"))
        boardGameId = testJdbcBoardGameRepository.createBoardGame(displayName = "Ark Nova")
        authenticate(userId, "ROLE_USER")
    }

    @Test
    fun emptyCollectionStatsReturnsZeros() {
        val stats = getStats()

        assertEquals(0, stats["totalItems"].asInt())
        assertEquals(0, stats["playedItems"].asInt())
        assertEquals(0, stats["unplayedItems"].asInt())
        assertDecimalEquals("0", stats["shelfOfShameCost"])
        assertDecimalEquals("0", stats["playedPercent"])
    }

    @Test
    fun statsWithPlayedUnplayedSoldAndWishlistAreCalculatedCorrectly() {
        testJdbcBoardGameRepository.createCollectionItem(
            userId = userId,
            boardGameId = boardGameId,
            status = "IN_COLLECTION",
            sumInRubles = BigDecimal("1000.00")
        )
        val playedInCollectionId = testJdbcBoardGameRepository.createCollectionItem(
            userId = userId,
            boardGameId = boardGameId,
            status = "IN_COLLECTION",
            sumInRubles = BigDecimal("2000.00")
        )
        testJdbcBoardGameRepository.createCollectionItem(
            userId = userId,
            boardGameId = boardGameId,
            status = "SOLD",
            sumInRubles = BigDecimal("3000.00")
        )
        testJdbcBoardGameRepository.createCollectionItem(
            userId = userId,
            boardGameId = boardGameId,
            status = "WISH_LIST",
            sumInRubles = BigDecimal("4000.00")
        )
        createPlaySession(playedInCollectionId)

        val stats = getStats()

        assertEquals(2, stats["totalItems"].asInt())
        assertEquals(1, stats["playedItems"].asInt())
        assertEquals(1, stats["unplayedItems"].asInt())
        assertDecimalEquals("1000.00", stats["shelfOfShameCost"])
        assertDecimalEquals("50.00", stats["playedPercent"])
    }

    @Test
    fun shelfEndpointReturnsOnlyCurrentUsersUnplayedInCollectionItems() {
        val ownUnplayedInCollection = testJdbcBoardGameRepository.createCollectionItem(
            userId = userId,
            boardGameId = boardGameId,
            status = "IN_COLLECTION"
        )
        val ownPlayedInCollection = testJdbcBoardGameRepository.createCollectionItem(
            userId = userId,
            boardGameId = boardGameId,
            status = "IN_COLLECTION"
        )
        testJdbcBoardGameRepository.createCollectionItem(
            userId = userId,
            boardGameId = boardGameId,
            status = "SOLD"
        )
        testJdbcBoardGameRepository.createCollectionItem(
            userId = userId,
            boardGameId = boardGameId,
            status = "WISH_LIST"
        )
        testJdbcBoardGameRepository.createCollectionItem(
            userId = otherUserId,
            boardGameId = boardGameId,
            status = "IN_COLLECTION"
        )
        createPlaySession(ownPlayedInCollection)

        val response = mockMvc.perform(
            get("/collection-items/shelf-of-shame")
                .param("page", "1")
                .param("limit", "20")
        )
            .andExpect(status().isOk)
            .andReturn()
            .let { objectMapper.readTree(it.response.contentAsString) }

        val ids = response["data"].map { it["id"].asLong() }.toSet()
        assertEquals(setOf(ownUnplayedInCollection), ids)
        assertEquals(1, response["pagination"]["total"].asInt())
    }

    @Test
    fun foreignUserItemsDoNotAffectStats() {
        testJdbcBoardGameRepository.createCollectionItem(
            userId = userId,
            boardGameId = boardGameId,
            status = "IN_COLLECTION",
            sumInRubles = BigDecimal("500.00")
        )
        val ownPlayed = testJdbcBoardGameRepository.createCollectionItem(
            userId = userId,
            boardGameId = boardGameId,
            status = "IN_COLLECTION",
            sumInRubles = BigDecimal("700.00")
        )
        testJdbcBoardGameRepository.createCollectionItem(
            userId = otherUserId,
            boardGameId = boardGameId,
            status = "IN_COLLECTION",
            sumInRubles = BigDecimal("9999.99")
        )
        val foreignPlayed = testJdbcBoardGameRepository.createCollectionItem(
            userId = otherUserId,
            boardGameId = boardGameId,
            status = "IN_COLLECTION",
            sumInRubles = BigDecimal("1500.00")
        )
        createPlaySession(ownPlayed)
        testJdbcBoardGameRepository.createPlaySession(collectionItemId = foreignPlayed)

        val stats = getStats()

        assertEquals(2, stats["totalItems"].asInt())
        assertEquals(1, stats["playedItems"].asInt())
        assertEquals(1, stats["unplayedItems"].asInt())
        assertDecimalEquals("500.00", stats["shelfOfShameCost"])
        assertDecimalEquals("50.00", stats["playedPercent"])
    }

    @Test
    fun statsChangeAfterCreatingPlaySession() {
        val collectionItemId = testJdbcBoardGameRepository.createCollectionItem(
            userId = userId,
            boardGameId = boardGameId,
            status = "IN_COLLECTION",
            sumInRubles = BigDecimal("900.00")
        )

        val before = getStats()
        assertEquals(1, before["totalItems"].asInt())
        assertEquals(0, before["playedItems"].asInt())
        assertEquals(1, before["unplayedItems"].asInt())
        assertDecimalEquals("900.00", before["shelfOfShameCost"])
        assertDecimalEquals("0", before["playedPercent"])

        mockMvc.perform(
            post("/collection-items/$collectionItemId/play-sessions/quick")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(QuickPlaySessionRequest(comment = "Первая партия")))
        ).andExpect(status().isCreated)

        val after = getStats()
        assertEquals(1, after["totalItems"].asInt())
        assertEquals(1, after["playedItems"].asInt())
        assertEquals(0, after["unplayedItems"].asInt())
        assertDecimalEquals("0", after["shelfOfShameCost"])
        assertDecimalEquals("100.00", after["playedPercent"])
    }

    private fun getStats(): JsonNode {
        return mockMvc.perform(get("/me/stats"))
            .andExpect(status().isOk)
            .andReturn()
            .let { objectMapper.readTree(it.response.contentAsString) }
    }

    private fun createPlaySession(collectionItemId: Long) {
        mockMvc.perform(
            post("/collection-items/$collectionItemId/play-sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        CreatePlaySessionRequest(
                            dateStart = OffsetDateTime.parse("2026-05-16T09:00:00Z"),
                            comment = "test play session"
                        )
                    )
                )
        ).andExpect(status().isCreated)
    }

    private fun assertDecimalEquals(expected: String, actualNode: JsonNode) {
        assertEquals(0, actualNode.decimalValue().compareTo(BigDecimal(expected)))
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
