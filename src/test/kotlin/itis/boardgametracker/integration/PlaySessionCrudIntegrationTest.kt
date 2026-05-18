package itis.boardgametracker.integration

import itis.boardgametracker.api.dto.CreatePlaySessionRequest
import itis.boardgametracker.api.dto.PlaySession
import itis.boardgametracker.api.dto.QuickPlaySessionRequest
import itis.boardgametracker.api.dto.UpdatePlaySessionRequest
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
import java.time.OffsetDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlaySessionCrudIntegrationTest : IntegrationTest() {

    @Autowired
    lateinit var mockMvc: MockMvc

    private var userId: Long = 0
    private var otherUserId: Long = 0
    private var boardGameId: Long = 0
    private var collectionItemId: Long = 0
    private var secondOwnCollectionItemId: Long = 0
    private var foreignCollectionItemId: Long = 0

    @BeforeEach
    fun setCurrentUser() {
        userId = testJdbcBoardGameRepository.createUser(uniqueEmail("playsession-user"))
        otherUserId = testJdbcBoardGameRepository.createUser(uniqueEmail("playsession-other"))
        boardGameId = testJdbcBoardGameRepository.createBoardGame(displayName = "Brass: Birmingham")
        collectionItemId = testJdbcBoardGameRepository.createCollectionItem(userId = userId, boardGameId = boardGameId)
        secondOwnCollectionItemId = testJdbcBoardGameRepository.createCollectionItem(userId = userId, boardGameId = boardGameId)
        foreignCollectionItemId = testJdbcBoardGameRepository.createCollectionItem(userId = otherUserId, boardGameId = boardGameId)
        authenticate(userId, "ROLE_USER")
    }

    @Test
    fun createSessionForOwnCollectionItem() {
        val createdPlaySession = createPlaySession(
            collectionItemId = collectionItemId,
            request = CreatePlaySessionRequest(
                dateStart = OffsetDateTime.parse("2026-05-16T09:00:00Z"),
                dateEnd = OffsetDateTime.parse("2026-05-16T10:20:00Z"),
                comment = "Удачная партия"
            )
        )

        assertEquals(collectionItemId, createdPlaySession.collectionItemId)
        assertEquals("Удачная партия", createdPlaySession.comment)
        assertEquals(1, testJdbcBoardGameRepository.findCollectionItemPlayCount(collectionItemId))
    }

    @Test
    fun createSessionForForeignCollectionItemReturns404() {
        mockMvc.perform(
            post("/collection-items/$foreignCollectionItemId/play-sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        CreatePlaySessionRequest(
                            dateStart = OffsetDateTime.parse("2026-05-16T09:00:00Z")
                        )
                    )
                )
        ).andExpect(status().isNotFound)
    }

    @Test
    fun listReturnsOnlySessionsForOwnItem() {
        val ownSession = createPlaySession(
            collectionItemId = collectionItemId,
            request = CreatePlaySessionRequest(
                dateStart = OffsetDateTime.parse("2026-05-16T09:00:00Z")
            )
        )
        testJdbcBoardGameRepository.createPlaySession(
            collectionItemId = secondOwnCollectionItemId,
            dateStart = OffsetDateTime.parse("2026-05-17T09:00:00Z")
        )
        testJdbcBoardGameRepository.createPlaySession(
            collectionItemId = foreignCollectionItemId,
            dateStart = OffsetDateTime.parse("2026-05-18T09:00:00Z")
        )

        val response = mockMvc.perform(
            get("/collection-items/$collectionItemId/play-sessions")
                .param("page", "1")
                .param("limit", "20")
        )
            .andExpect(status().isOk)
            .andReturn()
            .let { objectMapper.readTree(it.response.contentAsString) }

        val ids = response["data"].map { it["id"].asLong() }.toSet()
        assertEquals(setOf(ownSession.id), ids)
        assertEquals(1, response["pagination"]["total"].asInt())
    }

    @Test
    fun getForeignSessionReturns404() {
        val foreignSessionId = testJdbcBoardGameRepository.createPlaySession(
            collectionItemId = foreignCollectionItemId,
            dateStart = OffsetDateTime.parse("2026-05-16T09:00:00Z")
        )

        mockMvc.perform(get("/play-sessions/$foreignSessionId"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun updateOwnSession() {
        val ownSession = createPlaySession(
            collectionItemId = collectionItemId,
            request = CreatePlaySessionRequest(
                dateStart = OffsetDateTime.parse("2026-05-16T09:00:00Z")
            )
        )

        val updatedPlaySession = mockMvc.perform(
            put("/play-sessions/${ownSession.id}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        UpdatePlaySessionRequest(
                            dateStart = OffsetDateTime.parse("2026-05-16T09:10:00Z"),
                            dateEnd = OffsetDateTime.parse("2026-05-16T10:30:00Z"),
                            comment = "Обновленный комментарий"
                        )
                    )
                )
        )
            .andExpect(status().isOk)
            .andReturn()
            .let { objectMapper.readValue(it.response.contentAsString, PlaySession::class.java) }

        assertEquals(ownSession.id, updatedPlaySession.id)
        assertEquals("Обновленный комментарий", updatedPlaySession.comment)
        assertEquals(OffsetDateTime.parse("2026-05-16T10:30:00Z"), updatedPlaySession.dateEnd)
    }

    @Test
    fun deleteOwnSession() {
        val ownSession = createPlaySession(
            collectionItemId = collectionItemId,
            request = CreatePlaySessionRequest(
                dateStart = OffsetDateTime.parse("2026-05-16T09:00:00Z")
            )
        )

        mockMvc.perform(delete("/play-sessions/${ownSession.id}"))
            .andExpect(status().isNoContent)

        assertFalse(testJdbcBoardGameRepository.playSessionExists(ownSession.id))
    }

    @Test
    fun quickSessionIncrementsPlayCount() {
        val beforePlayCount = testJdbcBoardGameRepository.findCollectionItemPlayCount(collectionItemId)

        mockMvc.perform(
            post("/collection-items/$collectionItemId/play-sessions/quick")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(QuickPlaySessionRequest(comment = "Быстрый +1")))
        ).andExpect(status().isCreated)

        val afterPlayCount = testJdbcBoardGameRepository.findCollectionItemPlayCount(collectionItemId)
        assertEquals(beforePlayCount + 1, afterPlayCount)
        assertEquals(1, testJdbcBoardGameRepository.countPlaySessionsByCollectionItem(collectionItemId))
    }

    @Test
    fun deleteSessionDecrementsPlayCount() {
        val ownSession = createPlaySession(
            collectionItemId = collectionItemId,
            request = CreatePlaySessionRequest(
                dateStart = OffsetDateTime.parse("2026-05-16T09:00:00Z")
            )
        )
        assertEquals(1, testJdbcBoardGameRepository.findCollectionItemPlayCount(collectionItemId))

        mockMvc.perform(delete("/play-sessions/${ownSession.id}"))
            .andExpect(status().isNoContent)

        assertEquals(0, testJdbcBoardGameRepository.findCollectionItemPlayCount(collectionItemId))
    }

    @Test
    fun dateEndBeforeDateStartReturns400() {
        mockMvc.perform(
            post("/collection-items/$collectionItemId/play-sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        CreatePlaySessionRequest(
                            dateStart = OffsetDateTime.parse("2026-05-16T10:00:00Z"),
                            dateEnd = OffsetDateTime.parse("2026-05-16T09:00:00Z"),
                            comment = "Некорректная дата"
                        )
                    )
                )
        ).andExpect(status().isBadRequest)
    }

    private fun createPlaySession(collectionItemId: Long, request: CreatePlaySessionRequest): PlaySession {
        return mockMvc.perform(
            post("/collection-items/$collectionItemId/play-sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andReturn()
            .let { objectMapper.readValue(it.response.contentAsString, PlaySession::class.java) }
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
