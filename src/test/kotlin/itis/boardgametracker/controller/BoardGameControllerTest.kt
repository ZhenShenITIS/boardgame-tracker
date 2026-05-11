package itis.boardgametracker.controller

import itis.boardgametracker.ApplicationTestConfiguration
import itis.boardgametracker.api.BoardGameController
import itis.boardgametracker.exception.NotFoundException
import itis.boardgametracker.service.BoardGameService
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import kotlin.test.Test

@WebMvcTest(
    controllers = [BoardGameController::class],
    excludeAutoConfiguration = [
        SecurityAutoConfiguration::class,
        SecurityFilterAutoConfiguration::class
    ]
)
@ContextConfiguration(classes = [ApplicationTestConfiguration::class])
class BoardGameControllerTest {

    @MockitoBean
    lateinit var boardGameService: BoardGameService

    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun test404onFindById() {
        `when`(boardGameService.getBoardGameById(9999)).thenThrow(NotFoundException())

        mockMvc.perform(
            get("/boardgames/9999")
        ).andExpect(status().isNotFound)
            .andReturn()
    }
}