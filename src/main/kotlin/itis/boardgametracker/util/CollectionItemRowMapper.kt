package itis.boardgametracker.util

import itis.boardgametracker.constant.CollectionItemStatus
import itis.boardgametracker.model.CollectionItemRecord
import org.springframework.jdbc.core.RowMapper
import java.sql.ResultSet
import java.time.LocalDate

object CollectionItemRowMapper : RowMapper<CollectionItemRecord> {
    override fun mapRow(rs: ResultSet, rowNum: Int): CollectionItemRecord {
        return CollectionItemRecord(
            id = rs.getLong("id"),
            userId = rs.getLong("user_id"),
            boardGameId = rs.getLong("board_game_id"),
            datePurchased = rs.getObject("date_purchased", LocalDate::class.java),
            sumInRubles = rs.getBigDecimal("sum_in_rubles"),
            status = CollectionItemStatus.valueOf(rs.getString("status")),
            playCount = rs.getInt("play_count"),
            comment = rs.getString("comment"),
            createdAt = rs.getInstant("created_at"),
            updatedAt = rs.getInstant("updated_at")
        )
    }
}
