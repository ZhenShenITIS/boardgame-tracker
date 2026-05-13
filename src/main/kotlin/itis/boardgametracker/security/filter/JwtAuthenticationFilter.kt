package itis.boardgametracker.security.filter

import itis.boardgametracker.security.CurrentUserPrincipal
import itis.boardgametracker.service.auth.AccessTokenService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val accessTokenService: AccessTokenService,
    private val authenticationEntryPoint: AuthenticationEntryPoint
) : OncePerRequestFilter() {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val accessToken = extractBearerToken(request)
        if (accessToken == null) {
            filterChain.doFilter(request, response)
            return
        }

        val authentication = try {
            buildAuthentication(accessToken, request)
        } catch (exception: Exception) {
            SecurityContextHolder.clearContext()
            log.atWarn().log("Invalid access token rejected")
            authenticationEntryPoint.commence(
                request,
                response,
                BadCredentialsException("Invalid access token", exception)
            )
            return
        }

        val securityContext = SecurityContextHolder.createEmptyContext()
        securityContext.authentication = authentication
        SecurityContextHolder.setContext(securityContext)

        filterChain.doFilter(request, response)
    }

    private fun buildAuthentication(
        accessToken: String,
        request: HttpServletRequest
    ): UsernamePasswordAuthenticationToken {
        val claims = accessTokenService.extract(accessToken)
        val principal = CurrentUserPrincipal(
            userId = claims.userId,
            email = claims.email,
            roles = claims.roles
        )
        val authentication = UsernamePasswordAuthenticationToken(
            principal,
            null,
            claims.roles.map { SimpleGrantedAuthority(toAuthorityName(it)) }
        )
        authentication.details = WebAuthenticationDetailsSource().buildDetails(request)
        return authentication
    }

    private fun extractBearerToken(request: HttpServletRequest): String? {
        val authorizationHeader = request.getHeader(AUTHORIZATION_HEADER) ?: return null
        if (!authorizationHeader.startsWith(BEARER_PREFIX, ignoreCase = true)) {
            return null
        }
        return authorizationHeader.substring(BEARER_PREFIX.length).trim()
    }

    private fun toAuthorityName(role: String): String {
        return if (role.startsWith(ROLE_PREFIX)) role else "$ROLE_PREFIX$role"
    }

    private companion object {
        const val AUTHORIZATION_HEADER = "Authorization"
        const val BEARER_PREFIX = "Bearer "
        const val ROLE_PREFIX = "ROLE_"
    }
}
