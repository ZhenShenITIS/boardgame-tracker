package itis.boardgametracker.mapper

import itis.boardgametracker.api.dto.BoardGame
import itis.boardgametracker.model.CollectionItemRecord
import java.time.OffsetDateTime
import java.time.ZoneOffset

object CollectionItemMapper {

    fun map(
        collectionItemRecord: CollectionItemRecord,
        boardGame: BoardGame
    ): itis.boardgametracker.api.dto.CollectionItem {
        return itis.boardgametracker.api.dto.CollectionItem(
            id = collectionItemRecord.id,
            userId = collectionItemRecord.userId,
            boardGame = boardGame,
            datePurchased = collectionItemRecord.datePurchased,
            sumInRubles = collectionItemRecord.sumInRubles,
            status = map(collectionItemRecord.status),
            playCount = collectionItemRecord.playCount,
            comment = collectionItemRecord.comment,
            createdAt = OffsetDateTime.ofInstant(collectionItemRecord.createdAt, ZoneOffset.UTC),
            updatedAt = OffsetDateTime.ofInstant(collectionItemRecord.updatedAt, ZoneOffset.UTC)
        )
    }

    fun map(status: itis.boardgametracker.api.dto.CollectionItemStatus?): itis.boardgametracker.constant.CollectionItemStatus {
        return status
            ?.let { itis.boardgametracker.constant.CollectionItemStatus.valueOf(it.name) }
            ?: itis.boardgametracker.constant.CollectionItemStatus.IN_COLLECTION
    }

    fun map(status: itis.boardgametracker.constant.CollectionItemStatus): itis.boardgametracker.api.dto.CollectionItemStatus {
        return itis.boardgametracker.api.dto.CollectionItemStatus.valueOf(status.name)
    }
}
