package itis.boardgametracker.mapper

import itis.boardgametracker.model.PlaySessionRecord
import java.time.OffsetDateTime
import java.time.ZoneOffset

object PlaySessionMapper {
    fun map(playSessionRecord: PlaySessionRecord): itis.boardgametracker.api.dto.PlaySession {
        return itis.boardgametracker.api.dto.PlaySession(
            id = playSessionRecord.id,
            collectionItemId = playSessionRecord.collectionItemId,
            dateStart = OffsetDateTime.ofInstant(playSessionRecord.dateStart, ZoneOffset.UTC),
            dateEnd = playSessionRecord.dateEnd?.let { OffsetDateTime.ofInstant(it, ZoneOffset.UTC) },
            comment = playSessionRecord.comment,
            createdAt = OffsetDateTime.ofInstant(playSessionRecord.createdAt, ZoneOffset.UTC),
            updatedAt = OffsetDateTime.ofInstant(playSessionRecord.updatedAt, ZoneOffset.UTC)
        )
    }
}
