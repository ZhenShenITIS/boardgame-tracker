package itis.boardgametracker.repository

import itis.boardgametracker.constant.CollectionItemStatus
import itis.boardgametracker.model.RecommendationCandidateRecord
import itis.boardgametracker.util.BoardGameRowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.Types

@Repository
class RecommendationRepository(
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate
) {
    private val findTonightCandidatesSql: String = """
        SELECT ci.id AS collection_item_id,
               ci.play_count,
               ci.status AS collection_item_status,
               bg.id, bg.bgg_id, bg.type, bg.original_name, bg.display_name, bg.complexity, bg.min_players, bg.max_players,
               bg.playing_time, bg.min_play_time, bg.max_play_time, bg.min_age, bg.year_published, bg.s3_image_key,
               bg.s3_preview_key, bg.bgg_image_url, bg.bgg_preview_url, bg.is_custom, bg.created_by, bg.created_at, bg.updated_at
        FROM collection_items ci
                 JOIN board_games bg ON bg.id = ci.board_game_id
        WHERE ci.user_id = :userId
          AND ci.status = 'IN_COLLECTION'
          AND (:shelfOfShameOnly = FALSE OR ci.play_count = 0)
          AND (:playerCount >= COALESCE(bg.min_players, :playerCount))
          AND (:playerCount <= COALESCE(bg.max_players, :playerCount))
          AND (:maxPlayTimeMinutes IS NULL OR bg.playing_time IS NULL OR bg.playing_time <= :maxPlayTimeMinutes)
        ORDER BY ci.play_count ASC, bg.playing_time ASC NULLS LAST, ci.id ASC
    """.trimIndent()

    fun findTonightCandidates(
        userId: Long,
        playerCount: Int,
        maxPlayTimeMinutes: Int?,
        shelfOfShameOnly: Boolean
    ): List<RecommendationCandidateRecord> {
        return namedParameterJdbcTemplate.query(
            findTonightCandidatesSql,
            MapSqlParameterSource()
                .addValue("userId", userId, Types.BIGINT)
                .addValue("playerCount", playerCount, Types.INTEGER)
                .addValue("maxPlayTimeMinutes", maxPlayTimeMinutes, Types.INTEGER)
                .addValue("shelfOfShameOnly", shelfOfShameOnly, Types.BOOLEAN)
        ) { rs, rowNum ->
            RecommendationCandidateRecord(
                collectionItemId = rs.getLong("collection_item_id"),
                playCount = rs.getInt("play_count"),
                status = CollectionItemStatus.valueOf(rs.getString("collection_item_status")),
                boardGame = BoardGameRowMapper.mapRow(rs, rowNum)
                    ?: throw IllegalStateException("Failed to map board game for recommendation")
            )
        }
    }
}
