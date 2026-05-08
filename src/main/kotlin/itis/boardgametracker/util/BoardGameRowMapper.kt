package itis.boardgametracker.util

import itis.boardgametracker.constant.BoardGameType
import itis.boardgametracker.model.BoardGame
import org.springframework.jdbc.core.RowMapper
import java.sql.ResultSet

object BoardGameRowMapper : RowMapper<BoardGame> {
    override fun mapRow(rs: ResultSet, rowNum: Int): BoardGame? {
        return BoardGame(
            id = rs.getLong("id"),
            bggId = rs.getNullableLong("bgg_id"),
            type = BoardGameType.valueOf(rs.getString("type")),
            originalName = rs.getString("original_name"),
            displayName = rs.getString("display_name"),
            complexity = rs.getNullableDouble("complexity"),
            minPlayers = rs.getNullableInt("min_players"),
            maxPlayers = rs.getNullableInt("max_players"),
            playingTime = rs.getNullableInt("playing_time"),
            minPlayTime = rs.getNullableInt("min_play_time"),
            maxPlayTime = rs.getNullableInt("max_play_time"),
            minAge = rs.getNullableInt("min_age"),
            yearPublished = rs.getNullableInt("year_published"),
            s3ImageKey = rs.getString("s3_image_key"),
            s3PreviewKey = rs.getString("s3_preview_key"),
            bggImageUrl = rs.getString("bgg_image_url"),
            bggPreviewUrl = rs.getString("bgg_preview_url"),
            isCustom = rs.getBoolean("is_custom"),
            createdById = rs.getNullableLong("created_by"),
            createdAt = rs.getInstant("created_at"),
            updatedAt = rs.getInstant("updated_at")
        )
    }
}