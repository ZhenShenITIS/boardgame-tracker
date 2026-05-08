package itis.boardgametracker.mapper

import itis.boardgametracker.api.dto.CreateBoardGameRequest
import itis.boardgametracker.api.dto.UpdateBoardGameRequest
import itis.boardgametracker.constant.BoardGameType
import itis.boardgametracker.model.BoardGame
import itis.boardgametracker.model.Tag
import itis.boardgametracker.properties.S3Properties
import org.springframework.stereotype.Component

object BoardGameMapper {
    fun map(boardGame: BoardGame, imageUrl: String?, previewUrl: String?): itis.boardgametracker.api.dto.BoardGame {
        return itis.boardgametracker.api.dto.BoardGame(
            id = boardGame.id ?: 0,
            type = boardGame.type.name,
            originalName = boardGame.originalName,
            displayName = boardGame.displayName,
            isCustom = boardGame.isCustom,
            tags = boardGame.tags.map { this.map(it) },
            bggId = boardGame.bggId,
            complexity = boardGame.complexity,
            minPlayers = boardGame.minPlayers,
            maxPlayers = boardGame.maxPlayers,
            playingTime = boardGame.playingTime,
            minPlayTime = boardGame.minPlayTime,
            maxPlayTime = boardGame.maxPlayTime,
            minAge = boardGame.minAge,
            yearPublished = boardGame.yearPublished,
            imageUrl = imageUrl,
            previewUrl = previewUrl

        )
    }

    fun mapWithoutId(updateBoardGameRequest: UpdateBoardGameRequest): BoardGame {
        return BoardGame(
            bggId = updateBoardGameRequest.bggId,
            type = BoardGameType.valueOf(updateBoardGameRequest.type),
            originalName =  updateBoardGameRequest.originalName,
            displayName = updateBoardGameRequest.displayName,
            complexity = updateBoardGameRequest.complexity,
            minPlayers = updateBoardGameRequest.minPlayers,
            maxPlayers = updateBoardGameRequest.maxPlayers,
            playingTime = updateBoardGameRequest.playingTime,
            minPlayTime = updateBoardGameRequest.minPlayTime,
            maxPlayTime =  updateBoardGameRequest.maxPlayTime,
            minAge = updateBoardGameRequest.minAge,
            yearPublished = updateBoardGameRequest.yearPublished,
            s3ImageKey = updateBoardGameRequest.s3ImageKey,
            s3PreviewKey = updateBoardGameRequest.s3PreviewKey,
            bggImageUrl = updateBoardGameRequest.bggImageUrl,
            bggPreviewUrl = updateBoardGameRequest.bggPreviewUrl,
            isCustom = updateBoardGameRequest.isCustom,
            createdById = updateBoardGameRequest.createdById,
            tags = updateBoardGameRequest.tags.map { this.map(it) }
        )

    }

    fun map (createBoardGameRequest: CreateBoardGameRequest): BoardGame {
        return BoardGame(
            bggId = createBoardGameRequest.bggId,
            type = BoardGameType.valueOf(createBoardGameRequest.type),
            originalName =  createBoardGameRequest.originalName,
            displayName = createBoardGameRequest.displayName,
            complexity = createBoardGameRequest.complexity,
            minPlayers = createBoardGameRequest.minPlayers,
            maxPlayers = createBoardGameRequest.maxPlayers,
            playingTime = createBoardGameRequest.playingTime,
            minPlayTime = createBoardGameRequest.minPlayTime,
            maxPlayTime =  createBoardGameRequest.maxPlayTime,
            minAge = createBoardGameRequest.minAge,
            yearPublished = createBoardGameRequest.yearPublished,
            s3ImageKey = createBoardGameRequest.s3ImageKey,
            s3PreviewKey = createBoardGameRequest.s3PreviewKey,
            bggImageUrl = createBoardGameRequest.bggImageUrl,
            bggPreviewUrl = createBoardGameRequest.bggPreviewUrl,
            isCustom = createBoardGameRequest.isCustom,
            createdById = createBoardGameRequest.createdById,
            tags = createBoardGameRequest.tags.map { this.map(it) }
        )
    }


    fun map(tag: Tag): itis.boardgametracker.api.dto.Tag {
        return itis.boardgametracker.api.dto.Tag(
            name = tag.name,
            description = tag.description
        )
    }

    fun map(tag: itis.boardgametracker.api.dto.Tag): Tag {
        return Tag (
            name = tag.name,
            description = tag.description
        )
    }
}