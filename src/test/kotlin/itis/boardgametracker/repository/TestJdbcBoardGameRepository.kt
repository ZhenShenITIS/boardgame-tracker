package itis.boardgametracker.repository

import org.springframework.context.annotation.Primary
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.OffsetDateTime

@Primary
@Repository
class TestJdbcBoardGameRepository(
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate
) : BoardGameRepository(
    namedParameterJdbcTemplate = namedParameterJdbcTemplate
) {
    fun createUser(email: String): Long {
        return namedParameterJdbcTemplate.queryForObject(
            """
            INSERT INTO users (name, email, password)
            VALUES (:name, :email, :password)
            RETURNING id
            """.trimIndent(),
            MapSqlParameterSource()
                .addValue("name", "Test User")
                .addValue("email", email)
                .addValue("password", "password"),
            Long::class.java
        ) ?: throw IllegalStateException()
    }

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

    fun findCreatedByIdById(id: Long): Long? {
        return namedParameterJdbcTemplate.queryForObject(
            "SELECT created_by FROM board_games WHERE id = :id",
            MapSqlParameterSource().addValue("id", id),
            Long::class.java
        )
    }

    fun findIsCustomById(id: Long): Boolean? {
        return namedParameterJdbcTemplate.queryForObject(
            "SELECT is_custom FROM board_games WHERE id = :id",
            MapSqlParameterSource().addValue("id", id),
            Boolean::class.java
        )
    }

    fun createCollectionItem(
        userId: Long,
        boardGameId: Long,
        status: String = "IN_COLLECTION",
        sumInRubles: BigDecimal? = null
    ): Long {
        return namedParameterJdbcTemplate.queryForObject(
            """
            INSERT INTO collection_items (user_id, board_game_id, status, sum_in_rubles)
            VALUES (:userId, :boardGameId, :status, :sumInRubles)
            RETURNING id
            """.trimIndent(),
            MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("boardGameId", boardGameId)
                .addValue("status", status)
                .addValue("sumInRubles", sumInRubles),
            Long::class.java
        ) ?: throw IllegalStateException()
    }

    fun createBoardGame(
        displayName: String,
        bggId: Long? = null,
        isCustom: Boolean = false,
        createdBy: Long? = null,
        minPlayers: Int? = null,
        maxPlayers: Int? = null,
        playingTime: Int? = null
    ): Long {
        return namedParameterJdbcTemplate.queryForObject(
            """
            INSERT INTO board_games (bgg_id, type, original_name, display_name, is_custom, created_by, min_players, max_players, playing_time)
            VALUES (:bggId, 'BOARDGAME', :originalName, :displayName, :isCustom, :createdBy, :minPlayers, :maxPlayers, :playingTime)
            RETURNING id
            """.trimIndent(),
            MapSqlParameterSource()
                .addValue("bggId", bggId)
                .addValue("originalName", displayName)
                .addValue("displayName", displayName)
                .addValue("isCustom", isCustom)
                .addValue("createdBy", createdBy)
                .addValue("minPlayers", minPlayers)
                .addValue("maxPlayers", maxPlayers)
                .addValue("playingTime", playingTime),
            Long::class.java
        ) ?: throw IllegalStateException()
    }

    fun countBoardGamesByBggId(bggId: Long): Long {
        return namedParameterJdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM board_games WHERE bgg_id = :bggId",
            MapSqlParameterSource().addValue("bggId", bggId),
            Long::class.java
        ) ?: 0L
    }

    fun countCollectionItemsByUser(userId: Long): Long {
        return namedParameterJdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM collection_items WHERE user_id = :userId",
            MapSqlParameterSource().addValue("userId", userId),
            Long::class.java
        ) ?: 0L
    }

    fun findCollectionItemStatusById(id: Long): String? {
        return namedParameterJdbcTemplate.queryForObject(
            "SELECT status FROM collection_items WHERE id = :id",
            MapSqlParameterSource().addValue("id", id),
            String::class.java
        )
    }

    fun findCollectionItemBoardGameIdById(id: Long): Long? {
        return namedParameterJdbcTemplate.queryForObject(
            "SELECT board_game_id FROM collection_items WHERE id = :id",
            MapSqlParameterSource().addValue("id", id),
            Long::class.java
        )
    }

    fun findLatestCollectionItemIdByUser(userId: Long): Long {
        return namedParameterJdbcTemplate.queryForObject(
            """
            SELECT id
            FROM collection_items
            WHERE user_id = :userId
            ORDER BY id DESC
            LIMIT 1
            """.trimIndent(),
            MapSqlParameterSource().addValue("userId", userId),
            Long::class.java
        ) ?: throw IllegalStateException()
    }

    fun collectionItemExists(id: Long): Boolean {
        return (namedParameterJdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM collection_items WHERE id = :id",
            MapSqlParameterSource().addValue("id", id),
            Long::class.java
        ) ?: 0L) > 0L
    }

    fun createPlaySession(
        collectionItemId: Long,
        dateStart: OffsetDateTime = OffsetDateTime.parse("2026-05-16T09:00:00Z"),
        dateEnd: OffsetDateTime? = null,
        comment: String? = null
    ): Long {
        return namedParameterJdbcTemplate.queryForObject(
            """
            INSERT INTO play_sessions (collection_item_id, date_start, date_end, comment)
            VALUES (:collectionItemId, :dateStart, :dateEnd, :comment)
            RETURNING id
            """.trimIndent(),
            MapSqlParameterSource()
                .addValue("collectionItemId", collectionItemId)
                .addValue("dateStart", dateStart)
                .addValue("dateEnd", dateEnd)
                .addValue("comment", comment),
            Long::class.java
        ) ?: throw IllegalStateException()
    }

    fun playSessionExists(id: Long): Boolean {
        return (namedParameterJdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM play_sessions WHERE id = :id",
            MapSqlParameterSource().addValue("id", id),
            Long::class.java
        ) ?: 0L) > 0L
    }

    fun countPlaySessionsByCollectionItem(collectionItemId: Long): Long {
        return namedParameterJdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM play_sessions WHERE collection_item_id = :collectionItemId",
            MapSqlParameterSource().addValue("collectionItemId", collectionItemId),
            Long::class.java
        ) ?: 0L
    }

    fun findCollectionItemPlayCount(collectionItemId: Long): Int {
        return namedParameterJdbcTemplate.queryForObject(
            "SELECT play_count FROM collection_items WHERE id = :collectionItemId",
            MapSqlParameterSource().addValue("collectionItemId", collectionItemId),
            Int::class.java
        ) ?: 0
    }

    fun deleteAll() {
        namedParameterJdbcTemplate.update("DELETE FROM play_sessions", emptyMap<String, Any>())
        namedParameterJdbcTemplate.update("DELETE FROM collection_items", emptyMap<String, Any>())
        namedParameterJdbcTemplate.update("DELETE FROM board_games", emptyMap<String, Any>())
    }
}
