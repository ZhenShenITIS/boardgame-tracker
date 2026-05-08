package itis.boardgametracker.service

import io.micrometer.core.annotation.Timed
import itis.boardgametracker.api.dto.*
import itis.boardgametracker.exception.NotFoundException
import itis.boardgametracker.mapper.BoardGameMapper
import itis.boardgametracker.mapper.BoardGameMapper.map
import itis.boardgametracker.mapper.BoardGameMapper.mapWithoutId
import itis.boardgametracker.properties.S3Properties
import itis.boardgametracker.repository.BoardGameRepository
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BoardGameService(
    private val boardGameRepository: BoardGameRepository,
    private val s3Properties: S3Properties
) {

    object MetricsCatalog {
        const val BOARDGAMES_UPDATE_TIME =  "boardgames.update.time"
        const val BOARDGAME_GET_TIME = "boardgames.get.time"
        const val BOARDGAME_GET_LIST_TIME = "boardgames.get_list.time"
        const val BOARDGAME_CREATE_TIME = "boardgames.create.time"
    }

    @Timed(value = MetricsCatalog.BOARDGAME_GET_LIST_TIME, description = "Время получения списка игр")
    fun getBoardGameListByQueryWithPagination(query: String?, page: Int, limit: Int, userId: Long): BoardGameList {
        val boardGameList = boardGameRepository.findByQueryAndUserIdWithLimitOffset(
            userId = userId,
            query = query,
            limit = limit,
            offset = (page - 1) * limit
        )
        val totalCount = boardGameRepository.countFindByQueryAndUserId(
            userId = userId,
            query = query
        )

        val boardGameListDto = boardGameList.map { bg -> map(bg) }

        return BoardGameList(
            data = boardGameListDto,
            pagination = Pagination(
                page = page,
                limit = limit,
                total = totalCount,
                totalPages = (totalCount + limit - 1) / limit
            )
        )

    }

    fun deleteBoardGameById(id: Long) {
        boardGameRepository.deleteById(id)
    }
    @Timed(value = MetricsCatalog.BOARDGAME_GET_TIME, description = "Время получения одной игры")
    fun getBoardGameById(id: Long): BoardGame {
        try {
            return map(boardGameRepository.findById(id))
        } catch (_: EmptyResultDataAccessException) {
            throw NotFoundException()
        }
    }

    @Transactional
    @Timed(value = MetricsCatalog.BOARDGAMES_UPDATE_TIME, description = "Время обновления игры")
    fun updateBoardGameById(id: Long, updateBoardGameRequest: UpdateBoardGameRequest): BoardGame {
        var boardGame: itis.boardgametracker.model.BoardGame =
            mapWithoutId(updateBoardGameRequest)
        boardGame = boardGame.copy(
            id = id
        )
        try {
            return map(boardGameRepository.update(boardGame))
        } catch (_: EmptyResultDataAccessException) {
            throw NotFoundException()
        }

    }


    @Transactional
    @Timed(value = MetricsCatalog.BOARDGAME_CREATE_TIME, description = "Время создания игры")
    fun createBoardGame(createBoardGameRequest: CreateBoardGameRequest): BoardGame {
        val boardGame: itis.boardgametracker.model.BoardGame = map(createBoardGameRequest)
        return map(boardGameRepository.create(boardGame))
    }


    fun map(boardGame: itis.boardgametracker.model.BoardGame): itis.boardgametracker.api.dto.BoardGame {
        val imageUrl: String?
        if (boardGame.s3ImageKey != null) {
            imageUrl = s3Properties.baseS3Url + boardGame.s3ImageKey
        } else {
            imageUrl = boardGame.bggImageUrl
        }

        val previewUrl: String?
        if (boardGame.s3PreviewKey != null) {
            previewUrl = s3Properties.baseS3Url + boardGame.s3PreviewKey
        } else {
            previewUrl = boardGame.bggPreviewUrl
        }

        return BoardGameMapper.map(
            boardGame = boardGame,
            imageUrl = imageUrl,
            previewUrl = previewUrl)
    }
}