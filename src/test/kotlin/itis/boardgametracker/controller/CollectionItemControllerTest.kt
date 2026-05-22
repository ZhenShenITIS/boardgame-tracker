package itis.boardgametracker.controller

import com.fasterxml.jackson.databind.ObjectMapper
import itis.boardgametracker.api.CollectionItemController
import itis.boardgametracker.api.dto.CreateCollectionItemRequest
import itis.boardgametracker.exception.NotFoundException
import itis.boardgametracker.security.CurrentUserPrincipal
import itis.boardgametracker.security.CurrentUserProvider
import itis.boardgametracker.service.CollectionItemService
import itis.boardgametracker.service.auth.AccessTokenService
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import kotlin.test.Test

@WebMvcTest(
    controllers = [CollectionItemController::class],
    excludeAutoConfiguration = [
        SecurityAutoConfiguration::class,
        SecurityFilterAutoConfiguration::class
    ]
)
class CollectionItemControllerTest {

    @MockitoBean
    lateinit var collectionItemService: CollectionItemService

    @MockitoBean
    lateinit var currentUserProvider: CurrentUserProvider

    @MockitoBean
    lateinit var accessTokenService: AccessTokenService

    @MockitoBean
    lateinit var authenticationEntryPoint: AuthenticationEntryPoint

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Test
    fun test404onFindById() {
        val currentUser = CurrentUserPrincipal(
            userId = 1L,
            email = "test@itis.com",
            roles = listOf("ROLE_USER")
        )
        `when`(currentUserProvider.currentUser()).thenReturn(currentUser)
        `when`(collectionItemService.getCollectionItemById(9999L, currentUser)).thenThrow(NotFoundException())

        mockMvc.perform(
            get("/collection-items/9999")
        ).andExpect(status().isNotFound)
    }

    @Test
    fun pageOverflowReturns400() {
        mockMvc.perform(
            get("/collection-items")
                .param("page", "2147483648")
        ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"))
    }

    @Test
    fun postWithNegativeSumReturns400() {
        val payload = CreateCollectionItemRequest(
            boardGameId = 1L,
            sumInRubles = BigDecimal("-1")
        )

        mockMvc.perform(
            post("/collection-items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload))
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun postWithOversizedSumReturns400() {
        val payload = CreateCollectionItemRequest(
            boardGameId = 1L,
            sumInRubles = BigDecimal("100000000")
        )

        mockMvc.perform(
            post("/collection-items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload))
        ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"))
    }

    @Test
    fun customGameIntegerOverflowReturns400() {
        mockMvc.perform(
            post("/collection-items/custom-game")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "originalName": "Oversized players",
                      "displayName": "Oversized players",
                      "minPlayers": 2147483648
                    }
                    """.trimIndent()
                )
        ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"))
    }
}
