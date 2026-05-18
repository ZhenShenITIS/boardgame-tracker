package itis.boardgametracker.integration

import itis.boardgametracker.api.dto.BoardGame
import itis.boardgametracker.api.dto.CreateBoardGameRequest
import itis.boardgametracker.api.dto.UpdateBoardGameRequest
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
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BoardGameCrudIntegrationTest : IntegrationTest() {

    @Autowired
    lateinit var mockMvc: MockMvc

    private var userId: Long = 0
    private var otherUserId: Long = 0
    private var adminUserId: Long = 0

    @BeforeEach
    fun setCurrentUser() {
        userId = testJdbcBoardGameRepository.createUser(uniqueEmail("boardgame-user"))
        otherUserId = testJdbcBoardGameRepository.createUser(uniqueEmail("boardgame-other"))
        adminUserId = testJdbcBoardGameRepository.createUser(uniqueEmail("boardgame-admin"))
        authenticate(userId, "ROLE_USER")
    }

    @Test
    fun listDoesNotReturnForeignCustomGameForUser() {
        val globalGame = createBoardGameAsAdmin(createRequest(displayName = "Мрачная гавань", isCustom = false))
        val foreignCustomGame = createBoardGameAsAdmin(
            createRequest(displayName = "Мрачная гавань чужая", isCustom = true, createdById = otherUserId)
        )

        authenticate(userId, "ROLE_USER")
        val ownCustomGame = createBoardGame(
            createRequest(displayName = "Мрачная гавань авторская", isCustom = true, createdById = otherUserId)
        )

        val response = mockMvc.perform(
            get("/boardgames")
                .param("query", "Мрачная гавань")
                .param("page", "1")
                .param("limit", "10")
        )
            .andExpect(status().isOk)
            .andReturn()
            .let { objectMapper.readTree(it.response.contentAsString) }

        val ids = response["data"].map { it["id"].asLong() }.toSet()
        assertTrue(ids.contains(globalGame.id))
        assertTrue(ids.contains(ownCustomGame.id))
        assertFalse(ids.contains(foreignCustomGame.id))
        assertEquals(2, response["pagination"]["total"].asInt())
    }

    @Test
    fun getForeignCustomGameReturns404() {
        val foreignCustomGame = createBoardGameAsAdmin(
            createRequest(displayName = "Foreign custom", isCustom = true, createdById = otherUserId)
        )

        authenticate(userId, "ROLE_USER")
        mockMvc.perform(get("/boardgames/${foreignCustomGame.id}"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun userCreateGlobalGameReturns403() {
        mockMvc.perform(
            post("/boardgames")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest(displayName = "Global", isCustom = false)))
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun userCreateCustomGameStoresCurrentUserAsCreatedBy() {
        val createdGame = createBoardGame(
            createRequest(displayName = "Own custom", isCustom = true, createdById = otherUserId)
        )

        assertNotNull(createdGame.id)
        assertEquals(userId, testJdbcBoardGameRepository.findCreatedByIdById(createdGame.id))
        assertEquals(true, testJdbcBoardGameRepository.findIsCustomById(createdGame.id))
    }

    @Test
    fun userUpdateGlobalGameReturns403() {
        val globalGame = createBoardGameAsAdmin(createRequest(displayName = "Global", isCustom = false))

        authenticate(userId, "ROLE_USER")
        mockMvc.perform(
            put("/boardgames/${globalGame.id}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest(displayName = "Updated", isCustom = true)))
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun userUpdateOwnCustomGameReturns200AndKeepsOwnership() {
        val ownCustomGame = createBoardGame(
            createRequest(displayName = "Own custom", isCustom = true, createdById = otherUserId)
        )

        mockMvc.perform(
            put("/boardgames/${ownCustomGame.id}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        updateRequest(displayName = "Updated custom", isCustom = false, createdById = otherUserId)
                    )
                )
        )
            .andExpect(status().isOk)

        assertEquals("Updated custom", testJdbcBoardGameRepository.findDisplayNameById(ownCustomGame.id))
        assertEquals(userId, testJdbcBoardGameRepository.findCreatedByIdById(ownCustomGame.id))
        assertEquals(true, testJdbcBoardGameRepository.findIsCustomById(ownCustomGame.id))
    }

    @Test
    fun deleteNonexistentReturns404() {
        mockMvc.perform(delete("/boardgames/999999"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun userDeleteOwnCustomGameReturns204() {
        val ownCustomGame = createBoardGame(createRequest(displayName = "Own custom", isCustom = true))

        mockMvc.perform(delete("/boardgames/${ownCustomGame.id}"))
            .andExpect(status().isNoContent)

        assertEquals(0, testJdbcBoardGameRepository.countAll())
    }

    @Test
    fun userDeleteGlobalGameReturns403() {
        val globalGame = createBoardGameAsAdmin(createRequest(displayName = "Global", isCustom = false))

        authenticate(userId, "ROLE_USER")
        mockMvc.perform(delete("/boardgames/${globalGame.id}"))
            .andExpect(status().isForbidden)

        assertEquals(1, testJdbcBoardGameRepository.countAll())
    }

    @Test
    fun adminCanManageGlobalGame() {
        authenticate(adminUserId, "ROLE_ADMIN")

        val globalGame = createBoardGame(createRequest(displayName = "Global", isCustom = false))
        assertEquals(false, testJdbcBoardGameRepository.findIsCustomById(globalGame.id))

        mockMvc.perform(
            put("/boardgames/${globalGame.id}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest(displayName = "Updated global", isCustom = false)))
        )
            .andExpect(status().isOk)

        assertEquals("Updated global", testJdbcBoardGameRepository.findDisplayNameById(globalGame.id))

        mockMvc.perform(delete("/boardgames/${globalGame.id}"))
            .andExpect(status().isNoContent)

        assertEquals(0, testJdbcBoardGameRepository.countAll())
    }

    @Test
    fun deleteBoardGameReferencedByCollectionReturns409() {
        val globalGame = createBoardGameAsAdmin(createRequest(displayName = "Global", isCustom = false))
        testJdbcBoardGameRepository.createCollectionItem(userId = userId, boardGameId = globalGame.id)

        authenticate(adminUserId, "ROLE_ADMIN")
        mockMvc.perform(delete("/boardgames/${globalGame.id}"))
            .andExpect(status().isConflict)
    }

    private fun createBoardGameAsAdmin(createBoardGameRequest: CreateBoardGameRequest): BoardGame {
        authenticate(adminUserId, "ROLE_ADMIN")
        val createdGame = createBoardGame(createBoardGameRequest)
        authenticate(userId, "ROLE_USER")
        return createdGame
    }

    private fun createBoardGame(createBoardGameRequest: CreateBoardGameRequest): BoardGame {
        return mockMvc.perform(
            post("/boardgames")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createBoardGameRequest))
        )
            .andExpect(status().isCreated)
            .andReturn()
            .let {
                objectMapper.readValue(
                    it.response.contentAsString,
                    BoardGame::class.java
                )
            }
    }

    private fun createRequest(
        displayName: String,
        isCustom: Boolean,
        createdById: Long? = null
    ): CreateBoardGameRequest {
        return CreateBoardGameRequest(
            type = "BOARDGAME",
            originalName = displayName,
            displayName = displayName,
            isCustom = isCustom,
            tags = emptyList(),
            createdById = createdById
        )
    }

    private fun updateRequest(
        displayName: String,
        isCustom: Boolean,
        createdById: Long? = null
    ): UpdateBoardGameRequest {
        return UpdateBoardGameRequest(
            type = "BOARDGAME",
            originalName = displayName,
            displayName = displayName,
            isCustom = isCustom,
            tags = emptyList(),
            createdById = createdById
        )
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
