package itis.boardgametracker.api.exception.entrypoint

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerExceptionResolver

@Component
class DelegatedAccessDeniedHandler(
    private val handlerExceptionResolver: HandlerExceptionResolver
): AccessDeniedHandler {
    private val log = LoggerFactory.getLogger(javaClass)
    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: AccessDeniedException
    ) {
        log.atInfo().log("Перешли в EntryPoint")
        handlerExceptionResolver.resolveException(request, response, null, accessDeniedException)
    }
}