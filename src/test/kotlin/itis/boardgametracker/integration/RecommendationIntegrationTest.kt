package itis.boardgametracker.integration

import com.fasterxml.jackson.databind.JsonNode
import itis.boardgametracker.security.CurrentUserPrincipal
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RecommendationIntegrationTest : IntegrationTest() {

    @Autowired
    lateinit var mockMvc: MockMvc

    private var userId: Long = 0
    private var otherUserId: Long = 0

    @BeforeEach
    fun setCurrentUser() {
        userId = testJdbcBoardGameRepository.createUser(uniqueEmail("recommendation-user"))
        otherUserId = testJdbcBoardGameRepository.createUser(uniqueEmail("recommendation-other"))
        authenticate(userId, "ROLE_USER")
    }

    @Test
    fun recommendationsIncludeOnlyCurrentUsersCollection() {
        val ownBoardGameId = testJdbcBoardGameRepository.createBoardGame(
            displayName = "Cascadia",
            minPlayers = 1,
            maxPlayers = 4,
            playingTime = 45
        )
        val foreignBoardGameId = testJdbcBoardGameRepository.createBoardGame(
            displayName = "Ark Nova",
            minPlayers = 1,
            maxPlayers = 4,
            playingTime = 90
        )

        val ownItemId = testJdbcBoardGameRepository.createCollectionItem(userId = userId, boardGameId = ownBoardGameId)
        testJdbcBoardGameRepository.createCollectionItem(userId = otherUserId, boardGameId = foreignBoardGameId)

        val response = getRecommendations(playerCount = 3, maxPlayTimeMinutes = 120)

        val ids = response["data"].map { it["collectionItemId"].asLong() }.toSet()
        assertEquals(setOf(ownItemId), ids)
    }

    @Test
    fun playerCountFilterWorks() {
        val suitableBoardGameId = testJdbcBoardGameRepository.createBoardGame(
            displayName = "Root",
            minPlayers = 2,
            maxPlayers = 4,
            playingTime = 90
        )
        val unsuitableBoardGameId = testJdbcBoardGameRepository.createBoardGame(
            displayName = "War of the Ring",
            minPlayers = 5,
            maxPlayers = 6,
            playingTime = 120
        )

        val suitableItemId = testJdbcBoardGameRepository.createCollectionItem(userId = userId, boardGameId = suitableBoardGameId)
        testJdbcBoardGameRepository.createCollectionItem(userId = userId, boardGameId = unsuitableBoardGameId)

        val response = getRecommendations(playerCount = 4)
        val ids = response["data"].map { it["collectionItemId"].asLong() }.toSet()

        assertEquals(setOf(suitableItemId), ids)
    }

    @Test
    fun maxPlayTimeFilterWorks() {
        val shortBoardGameId = testJdbcBoardGameRepository.createBoardGame(
            displayName = "Azul",
            minPlayers = 2,
            maxPlayers = 4,
            playingTime = 45
        )
        val longBoardGameId = testJdbcBoardGameRepository.createBoardGame(
            displayName = "Twilight Imperium",
            minPlayers = 3,
            maxPlayers = 6,
            playingTime = 240
        )

        val shortItemId = testJdbcBoardGameRepository.createCollectionItem(userId = userId, boardGameId = shortBoardGameId)
        testJdbcBoardGameRepository.createCollectionItem(userId = userId, boardGameId = longBoardGameId)

        val response = getRecommendations(playerCount = 4, maxPlayTimeMinutes = 90)
        val ids = response["data"].map { it["collectionItemId"].asLong() }.toSet()

        assertEquals(setOf(shortItemId), ids)
    }

    @Test
    fun shelfOfShameOnlyReturnsOnlyUnplayedItems() {
        val boardGameId = testJdbcBoardGameRepository.createBoardGame(
            displayName = "Brass: Birmingham",
            minPlayers = 2,
            maxPlayers = 4,
            playingTime = 120
        )

        val unplayedItemId = testJdbcBoardGameRepository.createCollectionItem(userId = userId, boardGameId = boardGameId)
        val playedItemId = testJdbcBoardGameRepository.createCollectionItem(userId = userId, boardGameId = boardGameId)
        testJdbcBoardGameRepository.createPlaySession(collectionItemId = playedItemId)

        val response = getRecommendations(playerCount = 4, shelfOfShameOnly = true)

        val ids = response["data"].map { it["collectionItemId"].asLong() }.toSet()
        assertEquals(setOf(unplayedItemId), ids)
        assertTrue(response["data"].all { it["playCount"].asInt() == 0 })
    }

    @Test
    fun unplayedItemRanksAbovePlayedItem() {
        val boardGameId = testJdbcBoardGameRepository.createBoardGame(
            displayName = "Dune: Imperium",
            minPlayers = 1,
            maxPlayers = 4,
            playingTime = 90
        )

        val unplayedItemId = testJdbcBoardGameRepository.createCollectionItem(userId = userId, boardGameId = boardGameId)
        val playedItemId = testJdbcBoardGameRepository.createCollectionItem(userId = userId, boardGameId = boardGameId)
        testJdbcBoardGameRepository.createPlaySession(collectionItemId = playedItemId)

        val response = getRecommendations(playerCount = 4, shelfOfShameOnly = false)
        val data = response["data"]

        assertFalse(data.isEmpty)
        assertEquals(unplayedItemId, data.first()["collectionItemId"].asLong())
        assertTrue(data.map { it["collectionItemId"].asLong() }.contains(playedItemId))
    }

    @Test
    fun soldAndWishlistItemsAreExcluded() {
        val boardGameId = testJdbcBoardGameRepository.createBoardGame(
            displayName = "The Crew",
            minPlayers = 2,
            maxPlayers = 5,
            playingTime = 30
        )

        val inCollectionItemId = testJdbcBoardGameRepository.createCollectionItem(
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

        val response = getRecommendations(playerCount = 3, maxPlayTimeMinutes = 60)
        val ids = response["data"].map { it["collectionItemId"].asLong() }.toSet()

        assertEquals(setOf(inCollectionItemId), ids)
    }

    @Test
    fun invalidPlayerCountReturnsValidationError() {
        mockMvc.perform(
            get("/recommendations/tonight")
                .param("playerCount", "0")
        ).andExpect(status().isBadRequest)
    }

    private fun getRecommendations(
        playerCount: Int,
        maxPlayTimeMinutes: Int? = null,
        shelfOfShameOnly: Boolean = false,
        limit: Int = 10
    ): JsonNode {
        val request = get("/recommendations/tonight")
            .param("playerCount", playerCount.toString())
            .param("shelfOfShameOnly", shelfOfShameOnly.toString())
            .param("limit", limit.toString())

        if (maxPlayTimeMinutes != null) {
            request.param("maxPlayTimeMinutes", maxPlayTimeMinutes.toString())
        }

        return mockMvc.perform(request)
            .andExpect(status().isOk)
            .andReturn()
            .let { objectMapper.readTree(it.response.contentAsString) }
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
