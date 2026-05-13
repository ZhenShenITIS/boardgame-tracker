package itis.boardgametracker.controller

import itis.boardgametracker.api.AuthController
import itis.boardgametracker.api.dto.AuthResponse
import itis.boardgametracker.api.dto.ChangePasswordRequest
import itis.boardgametracker.api.dto.LogoutRequest
import itis.boardgametracker.api.dto.RegisterRequest
import itis.boardgametracker.api.dto.User
import itis.boardgametracker.security.CurrentUserProvider
import itis.boardgametracker.service.auth.AuthService
import itis.boardgametracker.service.auth.AuthContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest
import java.time.OffsetDateTime

class AuthControllerTest {
    private val authService = mock(AuthService::class.java)
    private val currentUserProvider = mock(CurrentUserProvider::class.java)

    @Test
    fun registerReturnsCreatedAndPassesRequestMetadata() {
        val request = MockHttpServletRequest()
        request.remoteAddr = "127.0.0.1"
        request.addHeader("User-Agent", "JUnit")
        val controller = AuthController(authService, currentUserProvider, request)
        val registerRequest = RegisterRequest("John Doe", "john@itis.com", "Password123")
        val authResponse = authResponse()
        doReturn(authResponse)
            .`when`(authService)
            .register(eqValue(registerRequest), anyAuthContext())

        val response = controller.authRegisterPost(registerRequest)

        val contextCaptor = ArgumentCaptor.forClass(AuthContext::class.java)
        verify(authService).register(eqValue(registerRequest), contextCaptor.capture())
        assertEquals(HttpStatus.CREATED, response.statusCode)
        assertEquals(authResponse, response.body)
        assertEquals("127.0.0.1", contextCaptor.value.ipAddress?.hostAddress)
        assertEquals("JUnit", contextCaptor.value.userAgent)
    }

    @Test
    fun changePasswordUsesCurrentUserId() {
        val request = MockHttpServletRequest()
        val controller = AuthController(authService, currentUserProvider, request)
        val changePasswordRequest = ChangePasswordRequest("Password123", "Password456")
        val authResponse = authResponse()
        `when`(currentUserProvider.currentUserId()).thenReturn(42L)
        doReturn(authResponse)
            .`when`(authService)
            .changePassword(eqValue(42L), eqValue(changePasswordRequest), anyAuthContext())

        val response = controller.authChangePasswordPost(changePasswordRequest)

        verify(authService).changePassword(eqValue(42L), eqValue(changePasswordRequest), anyAuthContext())
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(authResponse, response.body)
    }

    @Test
    fun logoutReturnsOkAndUsesCurrentUserId() {
        val request = MockHttpServletRequest()
        val controller = AuthController(authService, currentUserProvider, request)
        val logoutRequest = LogoutRequest("refresh-token")
        `when`(currentUserProvider.currentUserId()).thenReturn(42L)

        val response = controller.authLogoutPost(logoutRequest)

        verify(authService).logout(eqValue(logoutRequest), eqValue(42L), anyAuthContext())
        assertEquals(HttpStatus.OK, response.statusCode)
    }

    private fun anyAuthContext(): AuthContext {
        any(AuthContext::class.java)
        return AuthContext()
    }

    private fun <T> eqValue(value: T): T {
        eq(value)
        return value
    }

    private fun authResponse(): AuthResponse {
        return AuthResponse(
            accessToken = "access-token",
            refreshToken = "refresh-token",
            user = User(
                id = 42,
                email = "john@itis.com",
                name = "John Doe",
                roles = listOf("USER"),
                createdAt = OffsetDateTime.parse("2024-01-15T10:30:00Z")
            ),
            expiresIn = 3600
        )
    }
}
