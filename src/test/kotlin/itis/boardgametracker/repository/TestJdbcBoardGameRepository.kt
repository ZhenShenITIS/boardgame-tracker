package itis.boardgametracker.repository

import org.springframework.context.annotation.Primary
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository

@Primary
@Repository
class TestJdbcBoardGameRepository(
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate
) : BoardGameRepository(
    namedParameterJdbcTemplate = namedParameterJdbcTemplate
) {
    fun deleteAll() {
        namedParameterJdbcTemplate.update("DELETE FROM board_games", emptyMap<String, Any>())
    }
}