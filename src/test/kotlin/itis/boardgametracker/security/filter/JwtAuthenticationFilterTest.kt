package itis.boardgametracker.security.filter

import com.fasterxml.jackson.databind.ObjectMapper
import itis.boardgametracker.model.Role
import itis.boardgametracker.model.User
import itis.boardgametracker.properties.JwtProperties
import itis.boardgametracker.security.CurrentUserPrincipal
import itis.boardgametracker.service.auth.AccessTokenService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.AuthenticationEntryPoint

class JwtAuthenticationFilterTest {
    private lateinit var accessTokenService: AccessTokenService
    private lateinit var authenticationEntryPoint: AuthenticationEntryPoint
    private lateinit var filter: JwtAuthenticationFilter

    @BeforeEach
    fun beforeEach() {
        SecurityContextHolder.clearContext()
        accessTokenService = AccessTokenService(
            ObjectMapper(),
            JwtProperties(
                issuer = "test-issuer",
                accessTokenTtlSeconds = 3600,
                refreshTokenTtlSeconds = 2592000,
                secret = "test-secret-test-secret-test-secret"
            )
        )
        authenticationEntryPoint = mock(AuthenticationEntryPoint::class.java)
        filter = JwtAuthenticationFilter(accessTokenService, authenticationEntryPoint)
    }

    @AfterEach
    fun afterEach() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun validBearerTokenAuthenticatesCurrentUser() {
        val token = accessTokenService.generate(
            User(
                id = 42L,
                name = "John Doe",
                email = "john@itis.com",
                password = "hashed",
                roles = listOf(Role(name = "USER"))
            )
        )
        val request = MockHttpServletRequest()
        request.addHeader("Authorization", "Bearer $token")
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, MockFilterChain())

        val authentication = SecurityContextHolder.getContext().authentication
        assertNotNull(authentication)
        val principal = authentication.principal as CurrentUserPrincipal
        assertEquals(42L, principal.userId)
        assertEquals("john@itis.com", principal.email)
        assertEquals(listOf("ROLE_USER"), principal.roles)
        assertTrue(authentication.authorities.any { it.authority == "ROLE_USER" })
    }

    @Test
    fun missingAuthorizationHeaderDoesNotAuthenticate() {
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, MockFilterChain())

        assertNull(SecurityContextHolder.getContext().authentication)
    }

    @Test
    fun invalidBearerTokenClearsContextAndUsesEntryPoint() {
        val request = MockHttpServletRequest()
        request.addHeader("Authorization", "Bearer invalid-token")
        val response = MockHttpServletResponse()
        SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken("existing", null)

        filter.doFilter(request, response, MockFilterChain())

        assertNull(SecurityContextHolder.getContext().authentication)
        verify(authenticationEntryPoint).commence(
            eq(request),
            eq(response),
            any(AuthenticationException::class.java)
        )
    }

    @Test
    fun invalidBearerTokenOnPublicAuthPathIsIgnored() {
        val request = MockHttpServletRequest("POST", "/auth/login")
        request.addHeader("Authorization", "Bearer invalid-token")
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, MockFilterChain())

        assertNull(SecurityContextHolder.getContext().authentication)
        verifyNoInteractions(authenticationEntryPoint)
    }
}
