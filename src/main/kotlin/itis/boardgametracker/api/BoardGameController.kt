package itis.boardgametracker.api

import itis.boardgametracker.api.dto.BoardGame
import itis.boardgametracker.api.dto.BoardGameList
import itis.boardgametracker.api.dto.CreateBoardGameRequest
import itis.boardgametracker.api.dto.UpdateBoardGameRequest
import itis.boardgametracker.security.CurrentUserProvider
import itis.boardgametracker.service.BoardGameService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class BoardGameController(
    private val boardGameService: BoardGameService,
    private val currentUserProvider: CurrentUserProvider
) : BoardgamesApi {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun boardgamesGet(
        query: String?,
        page: Int,
        limit: Int
    ): ResponseEntity<BoardGameList> {
        val userId = currentUserProvider.currentUserId()

        log.atInfo()
            .addKeyValue("userId", userId)
            .addKeyValue("query", query)
            .addKeyValue("page", page)
            .addKeyValue("limit", limit)
            .log("Пришёл новый запрос на получение списка настольных игр")

        val boardGameList = boardGameService.getBoardGameListByQueryWithPagination(
            query = query,
            page = page,
            limit = limit,
            userId = userId
        )

        log.atInfo()
            .addKeyValue("userId", userId)
            .addKeyValue("fetchedCount", boardGameList.data.size)
            .addKeyValue("totalPages", boardGameList.pagination.totalPages)
            .log("Получен ответ на запрос получения списка настольных игр")
        return ResponseEntity.ok(boardGameList)
    }

    override fun boardgamesIdDelete(id: Long): ResponseEntity<Unit> {
        log.atInfo()
            .addKeyValue("boardGameId", id)
            .log("Пришёл запрос на удаление настольной игры")

        boardGameService.deleteBoardGameById(id)

        log.atInfo()
            .addKeyValue("boardGameId", id)
            .log("Настольная игра удалена")
        return ResponseEntity.noContent().build()
    }

    override fun boardgamesIdGet(id: Long): ResponseEntity<BoardGame> {
        log.atInfo()
            .addKeyValue("boardGameId", id)
            .log("Пришёл запрос на получение настольной игры по id")

        val boardGame = boardGameService.getBoardGameById(id)

        log.atInfo()
            .addKeyValue("boardGameId", id)
            .log("Получен ответ на запрос настольной игры по id")
        return ResponseEntity.ok(boardGame)
    }

    override fun boardgamesIdPut(
        id: Long,
        updateBoardGameRequest: UpdateBoardGameRequest
    ): ResponseEntity<BoardGame> {
        log.atInfo()
            .addKeyValue("boardGameId", id)
            .log("Пришёл запрос на обновление настольной игры")

        val updatedBoardGame = boardGameService.updateBoardGameById(id, updateBoardGameRequest)

        log.atInfo()
            .addKeyValue("boardGameId", id)
            .log("Настольная игра обновлена")
        return ResponseEntity.ok(updatedBoardGame)
    }

    override fun boardgamesPost(createBoardGameRequest: CreateBoardGameRequest): ResponseEntity<BoardGame> {
        log.atInfo()
            .log("Пришёл запрос на создание настольной игры")

        val createdBoardGame = boardGameService.createBoardGame(createBoardGameRequest)

        log.atInfo()
            .addKeyValue("boardGameId", createdBoardGame.id)
            .log("Настольная игра создана")
        return ResponseEntity.status(HttpStatus.CREATED).body(createdBoardGame)
    }
}
