package itis.boardgametracker.integration

import itis.boardgametracker.api.dto.CollectionItem
import itis.boardgametracker.api.dto.CollectionItemStatus
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
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CustomGameCollectionFlowIntegrationTest : IntegrationTest() {

    @Autowired
    lateinit var mockMvc: MockMvc

    private var userId: Long = 0
    private var otherUserId: Long = 0

    @BeforeEach
    fun setUp() {
        userId = testJdbcBoardGameRepository.createUser(uniqueEmail("custom-game-user"))
        otherUserId = testJdbcBoardGameRepository.createUser(uniqueEmail("custom-game-other"))
        authenticate(userId, "ROLE_USER")
    }

    @Test
    fun customGameEndpointCreatesBoardGameAndCollectionItem() {
        val createdItem = createCustomGame(
            mapOf(
                "originalName" to "Prototype X",
                "displayName" to "Прототип X",
                "minPlayers" to 1,
                "maxPlayers" to 4,
                "playingTime" to 90,
                "comment" to "Собственный прототип",
                "tags" to listOf(mapOf("name" to "Prototype"))
            )
        )

        assertEquals(userId, createdItem.userId)
        assertEquals(CollectionItemStatus.IN_COLLECTION, createdItem.status)
        assertTrue(createdItem.boardGame.isCustom)
        assertEquals(1L, testJdbcBoardGameRepository.countCollectionItemsByUser(userId))
        assertEquals(true, testJdbcBoardGameRepository.findIsCustomById(createdItem.boardGame.id))
        assertEquals(userId, testJdbcBoardGameRepository.findCreatedByIdById(createdItem.boardGame.id))
    }

    @Test
    fun foreignUserCannotSeeCreatedCustomGame() {
        val createdItem = createCustomGame(
            mapOf(
                "originalName" to "Secret Prototype",
                "displayName" to "Secret Prototype"
            )
        )

        authenticate(otherUserId, "ROLE_USER")
        mockMvc.perform(get("/boardgames/${createdItem.boardGame.id}"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun requestCannotSetBggId() {
        val createdItem = createCustomGame(
            mapOf(
                "originalName" to "No BGG",
                "displayName" to "No BGG",
                "bggId" to 174430,
                "isCustom" to false,
                "createdById" to otherUserId
            )
        )

        assertNull(createdItem.boardGame.bggId)
        assertTrue(createdItem.boardGame.isCustom)
        assertEquals(userId, testJdbcBoardGameRepository.findCreatedByIdById(createdItem.boardGame.id))
        assertEquals(0L, testJdbcBoardGameRepository.countBoardGamesByBggId(174430))
    }

    @Test
    fun rollbackWorksIfCollectionItemCreationFails() {
        assertFailsWith<jakarta.servlet.ServletException> {
            mockMvc.perform(
                post("/collection-items/custom-game")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            mapOf(
                                "originalName" to "Rollback Game",
                                "displayName" to "Rollback Game",
                                "sumInRubles" to BigDecimal("1000000000.00")
                            )
                        )
                    )
            )
                .andReturn()
        }

        assertEquals(0L, testJdbcBoardGameRepository.countAll())
        assertEquals(0L, testJdbcBoardGameRepository.countCollectionItemsByUser(userId))
    }

    private fun createCustomGame(payload: Map<String, Any?>): CollectionItem {
        return mockMvc.perform(
            post("/collection-items/custom-game")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload))
        )
            .andExpect(status().isCreated)
            .andReturn()
            .let { objectMapper.readValue(it.response.contentAsString, CollectionItem::class.java) }
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
