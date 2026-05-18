package itis.boardgametracker.service

import io.micrometer.core.annotation.Timed
import itis.boardgametracker.api.dto.*
import itis.boardgametracker.exception.ConflictException
import itis.boardgametracker.exception.NotFoundException
import itis.boardgametracker.mapper.BoardGameMapper
import itis.boardgametracker.mapper.BoardGameMapper.map
import itis.boardgametracker.mapper.BoardGameMapper.mapWithoutId
import itis.boardgametracker.properties.S3Properties
import itis.boardgametracker.repository.BoardGameRepository
import itis.boardgametracker.security.CurrentUserPrincipal
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.security.access.AccessDeniedException
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
        const val BOARDGAME_DELETE_TIME = "boardgames.delete.time"
    }

    @Timed(value = MetricsCatalog.BOARDGAME_GET_LIST_TIME, description = "Время получения списка игр")
    fun getBoardGameListByQueryWithPagination(
        query: String?,
        page: Int,
        limit: Int,
        currentUser: CurrentUserPrincipal
    ): BoardGameList {
        val boardGameList = boardGameRepository.findByQueryAndUserIdWithLimitOffset(
            userId = currentUser.userId,
            query = query,
            limit = limit,
            offset = (page - 1) * limit,
            includeAllCustomGames = currentUser.isAdmin()
        )
        val totalCount = boardGameRepository.countFindByQueryAndUserId(
            userId = currentUser.userId,
            query = query,
            includeAllCustomGames = currentUser.isAdmin()
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

    @Transactional
    @Timed(value = MetricsCatalog.BOARDGAME_DELETE_TIME, description = "Время удаления игры")
    fun deleteBoardGameById(id: Long, currentUser: CurrentUserPrincipal) {
        val existingBoardGame = findBoardGameByIdOrThrow(id)
        authorizeWrite(existingBoardGame, currentUser)

        try {
            val deletedRows = boardGameRepository.deleteById(id)
            if (deletedRows == 0) {
                throw NotFoundException()
            }
        } catch (_: DataIntegrityViolationException) {
            throw ConflictException("Нельзя удалить настольную игру, которая используется в коллекциях")
        }
    }

    @Timed(value = MetricsCatalog.BOARDGAME_GET_TIME, description = "Время получения одной игры")
    fun getBoardGameById(id: Long, currentUser: CurrentUserPrincipal): BoardGame {
        val boardGame = findBoardGameByIdOrThrow(id)
        if (!boardGame.isVisibleTo(currentUser)) {
            throw NotFoundException()
        }

        return map(boardGame)
    }

    @Transactional
    @Timed(value = MetricsCatalog.BOARDGAMES_UPDATE_TIME, description = "Время обновления игры")
    fun updateBoardGameById(
        id: Long,
        updateBoardGameRequest: UpdateBoardGameRequest,
        currentUser: CurrentUserPrincipal
    ): BoardGame {
        val existingBoardGame = findBoardGameByIdOrThrow(id)
        authorizeWrite(existingBoardGame, currentUser)

        val boardGameFromRequest = mapWithoutId(updateBoardGameRequest)
        val boardGame = if (currentUser.isAdmin()) {
            boardGameFromRequest.copy(
                id = id,
                createdAt = existingBoardGame.createdAt
            )
        } else {
            boardGameFromRequest.copy(
                id = id,
                isCustom = true,
                createdById = currentUser.userId,
                createdAt = existingBoardGame.createdAt
            )
        }

        try {
            return map(boardGameRepository.update(boardGame))
        } catch (_: EmptyResultDataAccessException) {
            throw NotFoundException()
        }

    }


    @Transactional
    @Timed(value = MetricsCatalog.BOARDGAME_CREATE_TIME, description = "Время создания игры")
    fun createBoardGame(createBoardGameRequest: CreateBoardGameRequest, currentUser: CurrentUserPrincipal): BoardGame {
        val boardGameFromRequest: itis.boardgametracker.model.BoardGame = map(createBoardGameRequest)
        val boardGame = if (currentUser.isAdmin()) {
            boardGameFromRequest
        } else {
            if (!createBoardGameRequest.isCustom) {
                throw AccessDeniedException("User cannot create global board game")
            }
            boardGameFromRequest.copy(
                isCustom = true,
                createdById = currentUser.userId
            )
        }

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

    private fun findBoardGameByIdOrThrow(id: Long): itis.boardgametracker.model.BoardGame {
        try {
            return boardGameRepository.findById(id)
        } catch (_: EmptyResultDataAccessException) {
            throw NotFoundException()
        }
    }

    private fun authorizeWrite(
        boardGame: itis.boardgametracker.model.BoardGame,
        currentUser: CurrentUserPrincipal
    ) {
        if (currentUser.isAdmin()) {
            return
        }
        if (!boardGame.isCustom) {
            throw AccessDeniedException("User cannot manage global board game")
        }
        if (boardGame.createdById != currentUser.userId) {
            throw NotFoundException()
        }
    }

    private fun itis.boardgametracker.model.BoardGame.isVisibleTo(currentUser: CurrentUserPrincipal): Boolean {
        return currentUser.isAdmin() || !isCustom || createdById == currentUser.userId
    }

    private fun CurrentUserPrincipal.isAdmin(): Boolean {
        return roles.any { it == "ADMIN" || it == "ROLE_ADMIN" }
    }
}
