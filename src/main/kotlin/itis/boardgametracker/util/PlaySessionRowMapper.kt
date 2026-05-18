package itis.boardgametracker.util

import itis.boardgametracker.model.PlaySessionRecord
import org.springframework.jdbc.core.RowMapper
import java.sql.ResultSet

object PlaySessionRowMapper : RowMapper<PlaySessionRecord> {
    override fun mapRow(rs: ResultSet, rowNum: Int): PlaySessionRecord {
        return PlaySessionRecord(
            id = rs.getLong("id"),
            collectionItemId = rs.getLong("collection_item_id"),
            dateStart = rs.getInstant("date_start"),
            dateEnd = rs.getObject("date_end", java.time.OffsetDateTime::class.java)?.toInstant(),
            comment = rs.getString("comment"),
            createdAt = rs.getInstant("created_at"),
            updatedAt = rs.getInstant("updated_at")
        )
    }
}
