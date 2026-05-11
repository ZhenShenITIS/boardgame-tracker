package itis.boardgametracker.api.exception.handler

import itis.boardgametracker.api.dto.Error
import itis.boardgametracker.api.dto.ErrorError
import itis.boardgametracker.exception.NotFoundException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(NotFoundException::class)
    fun handleNotFoundException(exception: NotFoundException): ResponseEntity<Error> {
        log.atWarn().log("Запрашиваемый ресурс не найден")
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(
                Error(
                    ErrorError(
                        code = "NOT_FOUND",
                        message = "Запрашиваемый ресурс не найден"
                    )
                )
            )
    }
}