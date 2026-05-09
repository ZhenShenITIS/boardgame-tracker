package itis.boardgametracker.integration

import itis.boardgametracker.api.BoardGameController
import itis.boardgametracker.api.dto.CreateBoardGameRequest
import itis.boardgametracker.api.dto.UpdateBoardGameRequest
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.util.stream.Stream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BoardGameCrudIntegrationTest : IntegrationTest() {

    @Autowired
    lateinit var boardGameController: BoardGameController

    @Autowired
    lateinit var jdbcTemplate: NamedParameterJdbcTemplate


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
            return Stream.of(Arguments.of(createBoardGameRequest,updateBoardGameRequest ))
        }

    }

    private fun createBoardGameAndDirectCount(createBoardGameRequest: CreateBoardGameRequest) {
        val response = boardGameController.boardgamesPost(createBoardGameRequest)
        val countWithCorrectQuery = jdbcTemplate.queryForObject(
            """
        SELECT COUNT(*)
            FROM board_games
            WHERE display_name % :query AND (is_custom = FALSE OR created_by = :userId)
    """.trimIndent(),
            MapSqlParameterSource()
                .addValue("query", "мрчная гавнь")
                .addValue("userId", 1),
            Long::class.java
        )!!
        val countWithIncorrectQuery = jdbcTemplate.queryForObject(
            """
        SELECT COUNT(*)
            FROM board_games
            WHERE display_name % :query AND (is_custom = FALSE OR created_by = :userId)
    """.trimIndent(),
            MapSqlParameterSource()
                .addValue("query", "null")
                .addValue("userId", 0),
            Long::class.java
        )!!

        assertNotNull(response.body?.id)
        assertEquals(1, countWithCorrectQuery)
        assertEquals(0, countWithIncorrectQuery)
    }


    private fun createBoardGameAndFind(createBoardGameRequest: CreateBoardGameRequest) {
        val createResponse = boardGameController.boardgamesPost(createBoardGameRequest)
        val findWithCorrectQueryResponse = boardGameController.boardgamesGet("мрачна гвань", 1, 25)
        val findWithIncorrectQueryResponse = boardGameController.boardgamesGet("null", 1, 25)
        assertNotNull(findWithCorrectQueryResponse.body?.data?.isEmpty())
        assertFalse { findWithCorrectQueryResponse.body?.data?.isEmpty()!! }
        assertNotNull(findWithIncorrectQueryResponse.body?.data?.isEmpty())
        assertTrue { findWithIncorrectQueryResponse.body?.data?.isEmpty()!! }
        assertEquals(createResponse.body?.id, findWithCorrectQueryResponse.body?.data?.get(0)?.id)
    }

    private fun createBoardGameAndDelete(createBoardGameRequest: CreateBoardGameRequest) {
        val createResponse = boardGameController.boardgamesPost(createBoardGameRequest)
        boardGameController.boardgamesIdDelete(createResponse.body!!.id)
        val count = jdbcTemplate.queryForObject(
            """
        SELECT COUNT(*)
            FROM board_games
                """.trimIndent(),
            MapSqlParameterSource(),
            Long::class.java
        )!!
        assertEquals(0, count)
    }

    private fun createBoardGameAndUpdate(
        createBoardGameRequest: CreateBoardGameRequest,
        updateBoardGameRequest: UpdateBoardGameRequest
    ) {
        val createResponse = boardGameController.boardgamesPost(createBoardGameRequest)
        val id = createResponse.body!!.id
        boardGameController.boardgamesIdPut(id, updateBoardGameRequest)
        val displayName = jdbcTemplate.queryForObject(
            """
        SELECT display_name
            FROM board_games WHERE id = :id
                """.trimIndent(),
            MapSqlParameterSource()
                .addValue("id", id),
            String::class.java
        )!!
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

    @Test
    fun testNotFoundOnFindById () {

    }


}
