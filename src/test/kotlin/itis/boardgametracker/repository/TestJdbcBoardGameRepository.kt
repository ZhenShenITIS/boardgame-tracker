package itis.boardgametracker.repository

import org.springframework.context.annotation.Primary
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository

@Primary
@Repository
class TestJdbcBoardGameRepository(
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate
) : BoardGameRepository(
    namedParameterJdbcTemplate = namedParameterJdbcTemplate
) {
    fun countByFuzzyDisplayNameAndUser(query: String, userId: Long): Long {
        return namedParameterJdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM board_games
            WHERE display_name % :query AND (is_custom = FALSE OR created_by = :userId)
            """.trimIndent(),
            MapSqlParameterSource()
                .addValue("query", query)
                .addValue("userId", userId),
            Long::class.java
        ) ?: 0L
    }

    fun countAll(): Long {
        return namedParameterJdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM board_games",
            emptyMap<String, Any>(),
            Long::class.java
        ) ?: 0L
    }

    fun findDisplayNameById(id: Long): String? {
        return namedParameterJdbcTemplate.queryForObject(
            "SELECT display_name FROM board_games WHERE id = :id",
            MapSqlParameterSource().addValue("id", id),
            String::class.java
        )
    }

    fun deleteAll() {
        namedParameterJdbcTemplate.update("DELETE FROM board_games", emptyMap<String, Any>())
    }
}
