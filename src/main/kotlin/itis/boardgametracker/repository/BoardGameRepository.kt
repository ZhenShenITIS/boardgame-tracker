package itis.boardgametracker.repository

import itis.boardgametracker.model.BoardGame
import itis.boardgametracker.model.Tag
import itis.boardgametracker.util.BoardGameRowMapper
import itis.boardgametracker.util.getInstant
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Repository
class BoardGameRepository(
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate
) {
    private val insertSql: String = """
        INSERT INTO board_games (
            bgg_id, type, original_name, display_name, complexity, min_players, max_players, playing_time,
            min_play_time, max_play_time, min_age, year_published, s3_image_key, s3_preview_key,
            bgg_image_url, bgg_preview_url, is_custom, created_by, created_at, updated_at
        ) VALUES (
            :bggId, :type, :originalName, :displayName, :complexity, :minPlayers, :maxPlayers,
            :playingTime, :minPlayTime, :maxPlayTime, :minAge, :yearPublished,
            :s3ImageKey, :s3PreviewKey, :bggImageUrl, :bggPreviewUrl,
            :isCustom, :createdById, :createdAt, :updatedAt
        ) RETURNING 
            id, bgg_id, type, original_name, display_name, complexity, min_players, max_players, playing_time,
            min_play_time, max_play_time, min_age, year_published, s3_image_key, s3_preview_key,
            bgg_image_url, bgg_preview_url, is_custom, created_by, created_at, updated_at
    """.trimIndent()
    private val insertImportedIgnoringDuplicatesSql: String = """
        INSERT INTO board_games (
            bgg_id, type, original_name, display_name, complexity, min_players, max_players, playing_time,
            min_play_time, max_play_time, min_age, year_published, s3_image_key, s3_preview_key,
            bgg_image_url, bgg_preview_url, is_custom, created_by, created_at, updated_at
        ) VALUES (
            :bggId, :type, :originalName, :displayName, :complexity, :minPlayers, :maxPlayers,
            :playingTime, :minPlayTime, :maxPlayTime, :minAge, :yearPublished,
            :s3ImageKey, :s3PreviewKey, :bggImageUrl, :bggPreviewUrl,
            :isCustom, :createdById, :createdAt, :updatedAt
        )
        ON CONFLICT (bgg_id) DO NOTHING
    """.trimIndent()
    private val findBoardGameIdByBggIdSql: String = """
        SELECT id
        FROM board_games
        WHERE bgg_id = :bggId
    """.trimIndent()
    private val findBoardGamesByQueryAndUserIdWithLimitOffsetSql: String = """
        SELECT id, bgg_id, type, original_name, display_name, complexity, min_players, max_players, playing_time,
            min_play_time, max_play_time, min_age, year_published, s3_image_key, s3_preview_key,
            bgg_image_url, bgg_preview_url, is_custom, created_by, created_at, updated_at
            FROM board_games
            WHERE display_name % :query
                AND (:includeAllCustomGames = TRUE OR is_custom = FALSE OR created_by = :userId)
            ORDER BY similarity(display_name, :query) DESC, display_name ASC
            LIMIT :limit
            OFFSET :offset
    """.trimIndent()


    private val findById: String = """
        SELECT id, bgg_id, type, original_name, display_name, complexity, min_players, max_players, playing_time,
            min_play_time, max_play_time, min_age, year_published, s3_image_key, s3_preview_key,
            bgg_image_url, bgg_preview_url, is_custom, created_by, created_at, updated_at
            FROM board_games
            WHERE id = :id
    """.trimIndent()

    private val deleteById: String = """
        DELETE FROM board_games WHERE id = :id
    """.trimIndent()

    private val updateById: String = """
        UPDATE board_games
        SET
            bgg_id = :bggId,
            type = :type,
            original_name = :originalName,
            display_name = :displayName,
            complexity = :complexity,
            min_players = :minPlayers,
            max_players = :maxPlayers,
            playing_time = :playingTime,
            min_play_time = :minPlayTime,
            max_play_time = :maxPlayTime,
            min_age = :minAge,
            year_published = :yearPublished,
            s3_image_key = :s3ImageKey,
            s3_preview_key = :s3PreviewKey,
            bgg_image_url = :bggImageUrl,
            bgg_preview_url = :bggPreviewUrl,
            is_custom = :isCustom,
            created_by = :createdById,
            created_at = :createdAt,
            updated_at = :updatedAt
        WHERE id = :id
        RETURNING
            id, bgg_id, type, original_name, display_name, complexity, min_players, max_players, playing_time,
            min_play_time, max_play_time, min_age, year_published, s3_image_key, s3_preview_key,
            bgg_image_url, bgg_preview_url, is_custom, created_by, created_at, updated_at
    """.trimIndent()

    private val countFindBoardGamesByQueryAndUserIdWithLimitOffsetSql: String = """
        SELECT COUNT(*)
            FROM board_games
            WHERE display_name % :query
                AND (:includeAllCustomGames = TRUE OR is_custom = FALSE OR created_by = :userId)
    """.trimIndent()

    private val findBoardGamesByUserIdWithLimitOffsetSql: String = """
        SELECT id, bgg_id, type, original_name, display_name, complexity, min_players, max_players, playing_time,
            min_play_time, max_play_time, min_age, year_published, s3_image_key, s3_preview_key,
            bgg_image_url, bgg_preview_url, is_custom, created_by, created_at, updated_at
            FROM board_games
            WHERE :includeAllCustomGames = TRUE OR is_custom = FALSE OR (is_custom = TRUE AND created_by = :userId)
            LIMIT :limit
            OFFSET :offset
    """.trimIndent()

    private val countFindBoardGamesByUserIdWithLimitOffsetSql: String = """
        SELECT COUNT (*)
        FROM board_games
        WHERE :includeAllCustomGames = TRUE OR is_custom = FALSE OR (is_custom = TRUE AND created_by = :userId)
    """.trimIndent()

    private val upsertTag: String = """
        INSERT INTO tags (name, description) VALUES (:name, :description) 
        ON CONFLICT (name) 
        DO UPDATE SET
        description = EXCLUDED.description
        RETURNING id, name, description
    """.trimIndent()

    private val insertBoardGameTagRelation: String = """
        INSERT INTO board_games_tags (board_game_id, tag_id) VALUES (:boardGameId, :tagId) ON CONFLICT DO NOTHING
    """.trimIndent()

    private val deleteTagsRelationByBoardGameId: String = """
        DELETE FROM board_games_tags
        WHERE board_game_id = :boardGameId
    """.trimIndent()

    private val findTagsByBoardGameIds: String = """
        SELECT
            bgt.board_game_id,
            t.id,
            t.name,
            t.description,
            t.created_at
        FROM board_games_tags bgt
                 JOIN tags t ON t.id = bgt.tag_id
        WHERE bgt.board_game_id IN (:boardGameIds)
        ORDER BY bgt.board_game_id, t.name
    """.trimIndent()

    fun create(boardGame: BoardGame): BoardGame {
        val boardGame = namedParameterJdbcTemplate
            .queryForObject(
                insertSql,
                map(boardGame),
                BoardGameRowMapper
            )
            ?: throw IllegalStateException()

        val tags = upsertTags(boardGame.tags, boardGame.id!!)
        return boardGame.copy(tags = tags)
    }


    fun upsertTags(tags: List<Tag>, boardGameId: Long): List<Tag> {
        if (tags.isEmpty()) {
            return tags
        }

        val keyHolder = GeneratedKeyHolder()

        namedParameterJdbcTemplate.batchUpdate(
            upsertTag,
            tags.map {
                MapSqlParameterSource()
                    .addValue("name", it.name)
                    .addValue("description", it.description)
            }.toTypedArray(),
            keyHolder,
            arrayOf("id", "name", "description")
        )

        namedParameterJdbcTemplate.batchUpdate(
            insertBoardGameTagRelation,
            keyHolder.keyList.map { keys ->
                (keys["id"] as Number).toLong()
            }.map {
                MapSqlParameterSource()
                    .addValue("boardGameId", boardGameId)
                    .addValue("tagId", it)
            }.toTypedArray()
        )

        return keyHolder.keyList.map { keys ->
            Tag(
                id = (keys["id"] as Number).toLong(),
                name = keys["name"] as String,
                description = keys["description"] as String?,
            )
        }
    }

    fun findByQueryAndUserIdWithLimitOffset(
        userId: Long,
        query: String?,
        limit: Int,
        offset: Int,
        includeAllCustomGames: Boolean
    ): List<BoardGame> {
        if (query != null) {
            val boardGames = namedParameterJdbcTemplate.query(
                findBoardGamesByQueryAndUserIdWithLimitOffsetSql,
                MapSqlParameterSource()
                    .addValue("userId", userId)
                    .addValue("limit", limit)
                    .addValue("offset", offset)
                    .addValue("includeAllCustomGames", includeAllCustomGames)
                    .addValue("query", query),
                BoardGameRowMapper
            )
            return enrichWithTags(boardGames)
        }

        val boardGames = namedParameterJdbcTemplate.query(
            findBoardGamesByUserIdWithLimitOffsetSql,
            MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("limit", limit)
                .addValue("includeAllCustomGames", includeAllCustomGames)
                .addValue("offset", offset),
            BoardGameRowMapper
        )
        return enrichWithTags(boardGames)

    }


    fun countFindByQueryAndUserId(userId: Long, query: String?, includeAllCustomGames: Boolean): Int {
        if (query != null) {
            return namedParameterJdbcTemplate.queryForObject(
                countFindBoardGamesByQueryAndUserIdWithLimitOffsetSql,

                MapSqlParameterSource()
                    .addValue("userId", userId)
                    .addValue("includeAllCustomGames", includeAllCustomGames)
                    .addValue("query", query),

                Int::class.java
            ) ?: 0
        }

        return namedParameterJdbcTemplate.queryForObject(
            countFindBoardGamesByUserIdWithLimitOffsetSql,

            MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("includeAllCustomGames", includeAllCustomGames),


            Int::class.java
        ) ?: 0
    }


    fun deleteById(id: Long): Int {
        return namedParameterJdbcTemplate.update(
            deleteById,
            MapSqlParameterSource()
                .addValue("id", id)
        )
    }

    fun findById(id: Long): BoardGame {
        val boardGame = namedParameterJdbcTemplate.queryForObject(
            findById,
            MapSqlParameterSource()
                .addValue("id", id),
            BoardGameRowMapper
        )!!
        return enrichWithTags(listOf(boardGame)).first()
    }

    fun update(boardGame: BoardGame): BoardGame {
        val boardGame =
            namedParameterJdbcTemplate.queryForObject(
                updateById,
                map(boardGame),
                BoardGameRowMapper
            )!!


        val boardGameId = boardGame.id ?: throw IllegalStateException()
        deleteTagsRelationsByBoardGameId(boardGameId)
        val tags = upsertTags(boardGame.tags, boardGameId)
        return boardGame.copy(tags = tags)
    }

    fun createImportedIgnoringDuplicates(boardGame: BoardGame): Boolean {
        val bggId = boardGame.bggId ?: return false
        val updatedRows = namedParameterJdbcTemplate.update(
            insertImportedIgnoringDuplicatesSql,
            map(boardGame)
        )

        val boardGameId = namedParameterJdbcTemplate.queryForObject(
            findBoardGameIdByBggIdSql,
            MapSqlParameterSource().addValue("bggId", bggId),
            Long::class.java
        ) ?: throw IllegalStateException("Board game with bggId=$bggId was not found after import upsert")

        upsertTags(boardGame.tags, boardGameId)
        return updatedRows > 0
    }


    private fun deleteTagsRelationsByBoardGameId(boardGameId: Long) {
        namedParameterJdbcTemplate.update(
            deleteTagsRelationByBoardGameId,
            MapSqlParameterSource()
                .addValue("boardGameId", boardGameId)
        )
    }

    private fun enrichWithTags(boardGames: List<BoardGame>): List<BoardGame> {
        if (boardGames.isEmpty()) {
            return boardGames
        }

        val boardGameIds = boardGames.mapNotNull { it.id }
        if (boardGameIds.isEmpty()) {
            return boardGames
        }

        val tagsByBoardGameId = namedParameterJdbcTemplate.query(
            findTagsByBoardGameIds,
            MapSqlParameterSource()
                .addValue("boardGameIds", boardGameIds)
        ) { rs, _ ->
            Pair(
                rs.getLong("board_game_id"),
                Tag(
                    id = rs.getLong("id"),
                    name = rs.getString("name"),
                    description = rs.getString("description"),
                    createdAt = rs.getInstant("created_at")
                )
            )
        }.groupBy(
            keySelector = { it.first },
            valueTransform = { it.second }
        )

        return boardGames.map { boardGame ->
            boardGame.copy(tags = tagsByBoardGameId[boardGame.id].orEmpty())
        }
    }


    fun map(boardGame: BoardGame): MapSqlParameterSource {
        val map = MapSqlParameterSource()
        map.addValue("id", boardGame.id, java.sql.Types.BIGINT)
        map.addValue("bggId", boardGame.bggId, java.sql.Types.BIGINT)
        map.addValue("type", boardGame.type.name, java.sql.Types.VARCHAR)
        map.addValue("originalName", boardGame.originalName, java.sql.Types.VARCHAR)
        map.addValue("displayName", boardGame.displayName, java.sql.Types.VARCHAR)
        map.addValue("complexity", boardGame.complexity, java.sql.Types.DOUBLE)
        map.addValue("minPlayers", boardGame.minPlayers, java.sql.Types.INTEGER)
        map.addValue("maxPlayers", boardGame.maxPlayers, java.sql.Types.INTEGER)
        map.addValue("playingTime", boardGame.playingTime, java.sql.Types.INTEGER)
        map.addValue("minPlayTime", boardGame.minPlayTime, java.sql.Types.INTEGER)
        map.addValue("maxPlayTime", boardGame.maxPlayTime, java.sql.Types.INTEGER)
        map.addValue("minAge", boardGame.minAge, java.sql.Types.INTEGER)
        map.addValue("yearPublished", boardGame.yearPublished, java.sql.Types.INTEGER)
        map.addValue("s3ImageKey", boardGame.s3ImageKey, java.sql.Types.VARCHAR)
        map.addValue("s3PreviewKey", boardGame.s3PreviewKey, java.sql.Types.VARCHAR)
        map.addValue("bggImageUrl", boardGame.bggImageUrl, java.sql.Types.VARCHAR)
        map.addValue("bggPreviewUrl", boardGame.bggPreviewUrl, java.sql.Types.VARCHAR)
        map.addValue("isCustom", boardGame.isCustom, java.sql.Types.BOOLEAN)
        map.addValue("createdById", boardGame.createdById, java.sql.Types.BIGINT)
        map.addValue(
            "createdAt",
            OffsetDateTime.ofInstant(boardGame.createdAt, ZoneOffset.UTC),
            java.sql.Types.TIMESTAMP_WITH_TIMEZONE
        )
        map.addValue(
            "updatedAt",
            OffsetDateTime.ofInstant(boardGame.updatedAt, ZoneOffset.UTC),
            java.sql.Types.TIMESTAMP_WITH_TIMEZONE
        )
        return map
    }


}
