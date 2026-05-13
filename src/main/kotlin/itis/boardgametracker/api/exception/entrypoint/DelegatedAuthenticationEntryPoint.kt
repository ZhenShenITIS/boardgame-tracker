package itis.boardgametracker.api.exception.entrypoint

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerExceptionResolver
import kotlin.math.log

@Component
class DelegatedAuthenticationEntryPoint(
    private val handlerExceptionResolver: HandlerExceptionResolver
): AuthenticationEntryPoint {
    private val log = LoggerFactory.getLogger(javaClass)
    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException
    ) {
        log.atInfo().log("Перешли в EntryPoint")
        handlerExceptionResolver.resolveException(request, response, null, authException)
    }
}