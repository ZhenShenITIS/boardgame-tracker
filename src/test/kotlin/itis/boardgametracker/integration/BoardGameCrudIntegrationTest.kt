package itis.boardgametracker.integration

import itis.boardgametracker.api.dto.BoardGame
import itis.boardgametracker.api.dto.CreateBoardGameRequest
import itis.boardgametracker.api.dto.UpdateBoardGameRequest
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.stream.Stream
import kotlin.test.*


class BoardGameCrudIntegrationTest : IntegrationTest() {

    @Autowired
    lateinit var mockMvc: MockMvc


    private companion object {
        val createBoardGameRequest = CreateBoardGameRequest(
            type = "BOARDGAME",
            originalName = "Gloomhaven",
            displayName = "Мрачная гавань",
            isCustom = false,
            tags = emptyList(),
            bggId = 174430,
            complexity = 3.91,
            minPlayers = 1,
            maxPlayers = 4,
            playingTime = 120,
            minPlayTime = 60,
            maxPlayTime = 120,
            minAge = 14,
            yearPublished = 2017,
            s3ImageKey = "boardgames/1/image.jpg",
            s3PreviewKey = "boardgames/1/preview.jpg",
            bggImageUrl = "https://cf.geekdo-images.com/image.jpg",
            bggPreviewUrl = "https://cf.geekdo-images.com/preview.jpg"
        )

        val updateBoardGameRequest = UpdateBoardGameRequest(
            type = "BOARDGAME",
            originalName = "Gloomhaven",
            displayName = "Весёлая гавань",
            isCustom = false,
            tags = emptyList(),
            bggId = 174430,
            complexity = 3.91,
            minPlayers = 1,
            maxPlayers = 4,
            playingTime = 120,
            minPlayTime = 60,
            maxPlayTime = 120,
            minAge = 14,
            yearPublished = 2017,
            s3ImageKey = "boardgames/1/image.jpg",
            s3PreviewKey = "boardgames/1/preview.jpg",
            bggImageUrl = "https://cf.geekdo-images.com/image.jpg",
            bggPreviewUrl = "https://cf.geekdo-images.com/preview.jpg"
        )

        @JvmStatic
        fun getCreatBoardGameRequest(): Stream<Arguments> {
            return Stream.of(Arguments.of(createBoardGameRequest))
        }

        @JvmStatic
        fun getCreatBoardGameRequestAndUpdateBoardGameRequest(): Stream<Arguments> {
            return Stream.of(Arguments.of(createBoardGameRequest, updateBoardGameRequest))
        }

    }

    private fun createBoardGame(createBoardGameRequest: CreateBoardGameRequest) =
        mockMvc.perform(
            post("/boardgames")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createBoardGameRequest))
        )
            .andExpect(status().isCreated)
            .andReturn()
            .let {
                objectMapper
                    .readValue(
                        it.response.contentAsString,
                        BoardGame::class.java
                    )
            }

    private fun createBoardGameAndDirectCount(createBoardGameRequest: CreateBoardGameRequest) {
        val response = createBoardGame(createBoardGameRequest)

        val countWithCorrectQuery = testJdbcBoardGameRepository.countByFuzzyDisplayNameAndUser("мрчная гавнь", 1L)
        val countWithIncorrectQuery = testJdbcBoardGameRepository.countByFuzzyDisplayNameAndUser("null", 0L)

        assertNotNull(response?.id)
        assertEquals(1, countWithCorrectQuery)
        assertEquals(0, countWithIncorrectQuery)
    }


    private fun createBoardGameAndFind(createBoardGameRequest: CreateBoardGameRequest) {
        val createResponse = createBoardGame(createBoardGameRequest)

        val findWithCorrectQueryResponse = mockMvc.perform(
            get("/boardgames")
                .param("query", "мрачна гвань")
                .param("offset", "1")
                .param("limit", "25")
        )
            .andExpect(status().isOk)
            .andReturn()
            .let { objectMapper.readTree(it.response.contentAsString) }

        val findWithIncorrectQueryResponse = mockMvc.perform(
            get("/boardgames")
                .param("query", "null")
                .param("offset", "1")
                .param("limit", "25")
        )
            .andExpect(status().isOk)
            .andReturn()
            .let { objectMapper.readTree(it.response.contentAsString) }

        assertFalse(findWithCorrectQueryResponse["data"].isEmpty)
        assertTrue(findWithIncorrectQueryResponse["data"].isEmpty)
        assertEquals(createResponse.id, findWithCorrectQueryResponse["data"][0]["id"].asLong())
    }

    private fun createBoardGameAndDelete(createBoardGameRequest: CreateBoardGameRequest) {
        val createResponse = createBoardGame(createBoardGameRequest)

        mockMvc.perform(delete("/boardgames/${createResponse.id}"))
            .andExpect(status().isNoContent)

        val count = testJdbcBoardGameRepository.countAll()
        assertEquals(0, count)
    }

    private fun createBoardGameAndUpdate(
        createBoardGameRequest: CreateBoardGameRequest,
        updateBoardGameRequest: UpdateBoardGameRequest
    ) {
        val createResponse = createBoardGame(createBoardGameRequest)

        val id = createResponse.id
        mockMvc.perform(
            put("/boardgames/$id")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateBoardGameRequest))
        )
            .andExpect(status().isOk)

        val displayName = testJdbcBoardGameRepository.findDisplayNameById(id)
        assertEquals(updateBoardGameRequest.displayName, displayName)
    }


    @ParameterizedTest
    @MethodSource("getCreatBoardGameRequest")
    fun testCreateBoardGame(createBoardGameRequest: CreateBoardGameRequest) {
        createBoardGameAndDirectCount(createBoardGameRequest)
    }

    @ParameterizedTest
    @MethodSource("getCreatBoardGameRequest")
    fun testCreateBoardGameAndFind(createBoardGameRequest: CreateBoardGameRequest) {
        createBoardGameAndFind(createBoardGameRequest)
    }

    @ParameterizedTest
    @MethodSource("getCreatBoardGameRequest")
    fun testCreateBoardGameAndDelete(createBoardGameRequest: CreateBoardGameRequest) {
        createBoardGameAndDelete(createBoardGameRequest)
    }

    @ParameterizedTest
    @MethodSource("getCreatBoardGameRequestAndUpdateBoardGameRequest")
    fun testCreateBoardGameAndUpdate(
        createBoardGameRequest: CreateBoardGameRequest,
        updateBoardGameRequest: UpdateBoardGameRequest
    ) {
        createBoardGameAndUpdate(createBoardGameRequest, updateBoardGameRequest)
    }


}
