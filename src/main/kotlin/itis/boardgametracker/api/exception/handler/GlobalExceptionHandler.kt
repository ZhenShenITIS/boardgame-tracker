package itis.boardgametracker.api.exception.handler

import itis.boardgametracker.api.dto.Error
import itis.boardgametracker.api.dto.ErrorError
import itis.boardgametracker.exception.BadRequestException
import itis.boardgametracker.exception.ConflictException
import itis.boardgametracker.exception.DuplicateEmailException
import itis.boardgametracker.exception.InvalidCredentialsException
import itis.boardgametracker.exception.InvalidOrExpiredRefreshTokenException
import itis.boardgametracker.exception.ImportJobAlreadyRunningException
import itis.boardgametracker.exception.NotFoundException
import itis.boardgametracker.exception.OldPasswordMismatchException
import itis.boardgametracker.exception.RevokedRefreshTokenReuseException
import itis.boardgametracker.exception.UserNotFoundException
import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.http.converter.HttpMessageNotReadableException

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

    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthenticationException(exception: AuthenticationException): ResponseEntity<Error> {
        log.atWarn().log("Не авторизован")
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(
                Error(
                    ErrorError(
                        code = "UNAUTHORIZED",
                        message = "Требуется аутентификация"
                    )
                )
            )
    }

    @ExceptionHandler(DuplicateEmailException::class)
    fun handleDuplicateEmailException(exception: DuplicateEmailException): ResponseEntity<Error> {
        log.atWarn().log("Email conflict")
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(
                Error(
                    ErrorError(
                        code = "CONFLICT",
                        message = "Пользователь с таким email уже существует"
                    )
                )
            )
    }

    @ExceptionHandler(
        InvalidCredentialsException::class,
        InvalidOrExpiredRefreshTokenException::class,
        RevokedRefreshTokenReuseException::class,
        OldPasswordMismatchException::class
    )
    fun handleAuthInvalidException(exception: RuntimeException): ResponseEntity<Error> {
        log.atWarn().log("Unauthorized auth request")
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(
                Error(
                    ErrorError(
                        code = "UNAUTHORIZED",
                        message = "Неверные учетные данные или токен"
                    )
                )
            )
    }

    @ExceptionHandler(UserNotFoundException::class)
    fun handleUserNotFoundException(exception: UserNotFoundException): ResponseEntity<Error> {
        log.atWarn().log("User not found")
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(
                Error(
                    ErrorError(
                        code = "NOT_FOUND",
                        message = "Пользователь не найден"
                    )
                )
            )
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDeniedException(exception: AccessDeniedException): ResponseEntity<Error> {
        log.atWarn().log("Нет прав доступа к ресурсу")
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(
                Error(
                    ErrorError(
                        code = "FORBIDDEN",
                        message = "Доступ запрещён"
                    )
                )
            )
    }

    @ExceptionHandler(ConflictException::class)
    fun handleConflictException(exception: ConflictException): ResponseEntity<Error> {
        log.atWarn().log("Конфликт данных")
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(
                Error(
                    ErrorError(
                        code = "CONFLICT",
                        message = exception.message ?: "Конфликт данных"
                    )
                )
            )
    }

    @ExceptionHandler(ImportJobAlreadyRunningException::class)
    fun handleImportJobAlreadyRunningException(exception: ImportJobAlreadyRunningException): ResponseEntity<Error> {
        log.atWarn().log("Импорт уже выполняется")
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(
                Error(
                    ErrorError(
                        code = "CONFLICT",
                        message = "Импорт уже выполняется"
                    )
                )
            )
    }

    @ExceptionHandler(
        BadRequestException::class,
        MethodArgumentNotValidException::class,
        ConstraintViolationException::class,
        HttpMessageNotReadableException::class,
        MethodArgumentTypeMismatchException::class
    )
    fun handleBadRequestException(exception: Exception): ResponseEntity<Error> {
        log.atWarn().log("Невалидный запрос")
        val message = when (exception) {
            is BadRequestException -> exception.message ?: "Невалидный запрос"
            else -> "Невалидный запрос"
        }
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                Error(
                    ErrorError(
                        code = "BAD_REQUEST",
                        message = message
                    )
                )
            )
    }

}
