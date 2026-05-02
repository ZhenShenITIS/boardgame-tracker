package itis.boardgametracker.service

import io.micrometer.core.annotation.Timed
import itis.boardgametracker.api.dto.*
import itis.boardgametracker.constant.MetricsCatalog
import itis.boardgametracker.mapper.BoardGameMapper
import itis.boardgametracker.repository.BoardGameRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BoardGameService(
    private val boardGameRepository: BoardGameRepository,
    private val boardGameMapper: BoardGameMapper
) {

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

        val boardGameListDto = boardGameList.map { bg -> boardGameMapper.map(bg) }

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
        return boardGameMapper.map(boardGameRepository.findById(id))
    }

    @Transactional
    @Timed(value = MetricsCatalog.BOARDGAMES_UPDATE_TIME, description = "Время обновления игры")
    fun updateBoardGameById(id: Long, updateBoardGameRequest: UpdateBoardGameRequest): BoardGame {
        var boardGame: itis.boardgametracker.model.BoardGame =
            boardGameMapper.mapWithoutId(updateBoardGameRequest)
        boardGame = boardGame.copy(
            id = id
        )
        return boardGameMapper.map(boardGameRepository.update(boardGame))

    }


    @Transactional
    @Timed(value = MetricsCatalog.BOARDGAME_CREATE_TIME, description = "Время создания игры")
    fun createBoardGame(createBoardGameRequest: CreateBoardGameRequest): BoardGame {
        val boardGame: itis.boardgametracker.model.BoardGame = boardGameMapper.map(createBoardGameRequest)
        return boardGameMapper.map(boardGameRepository.create(boardGame))
    }
}