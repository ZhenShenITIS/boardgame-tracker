package itis.boardgametracker.api

import itis.boardgametracker.api.dto.AuthResponse
import itis.boardgametracker.api.dto.ChangeNameRequest
import itis.boardgametracker.api.dto.ChangePasswordRequest
import itis.boardgametracker.api.dto.LoginRequest
import itis.boardgametracker.api.dto.LogoutRequest
import itis.boardgametracker.api.dto.RefreshRequest
import itis.boardgametracker.api.dto.RefreshResponse
import itis.boardgametracker.api.dto.RegisterRequest
import itis.boardgametracker.api.dto.User
import itis.boardgametracker.security.CurrentUserProvider
import itis.boardgametracker.service.auth.AuthService
import itis.boardgametracker.service.auth.AuthContext
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import java.net.InetAddress

@RestController
class AuthController(
    private val authService: AuthService,
    private val currentUserProvider: CurrentUserProvider,
    private val httpServletRequest: HttpServletRequest
) : AuthApi {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun authChangePasswordPost(changePasswordRequest: ChangePasswordRequest): ResponseEntity<AuthResponse> {
        val userId = currentUserProvider.currentUserId()
        log.atInfo()
            .addKeyValue("userId", userId)
            .log("Received password change request")

        val response = authService.changePassword(userId, changePasswordRequest, authContext())

        log.atInfo()
            .addKeyValue("userId", userId)
            .log("Password changed")
        return ResponseEntity.ok(response)
    }

    override fun authLoginPost(loginRequest: LoginRequest): ResponseEntity<AuthResponse> {
        log.atInfo().log("Received login request")

        val response = authService.login(loginRequest, authContext())

        log.atInfo()
            .addKeyValue("userId", response.user.id)
            .log("Login completed")
        return ResponseEntity.ok(response)
    }

    override fun authLogoutPost(logoutRequest: LogoutRequest): ResponseEntity<Unit> {
        val userId = currentUserProvider.currentUserId()
        log.atInfo()
            .addKeyValue("userId", userId)
            .log("Received logout request")

        authService.logout(logoutRequest, userId, authContext())

        log.atInfo()
            .addKeyValue("userId", userId)
            .log("Logout completed")
        return ResponseEntity.ok().build()
    }

    override fun authProfileGet(): ResponseEntity<User> {
        val userId = currentUserProvider.currentUserId()
        log.atInfo()
            .addKeyValue("userId", userId)
            .log("Received profile get request")

        val response = authService.getProfile(userId)

        log.atInfo()
            .addKeyValue("userId", userId)
            .log("Profile get completed")
        return ResponseEntity.ok(response)
    }

    override fun authProfilePatch(changeNameRequest: ChangeNameRequest): ResponseEntity<User> {
        val userId = currentUserProvider.currentUserId()
        log.atInfo()
            .addKeyValue("userId", userId)
            .log("Received profile update request")

        val response = authService.changeName(userId, changeNameRequest)

        log.atInfo()
            .addKeyValue("userId", userId)
            .log("Profile update completed")
        return ResponseEntity.ok(response)
    }

    override fun authRefreshPost(refreshRequest: RefreshRequest): ResponseEntity<RefreshResponse> {
        log.atInfo().log("Received token refresh request")

        val response = authService.refresh(refreshRequest, authContext())

        log.atInfo().log("Token refresh completed")
        return ResponseEntity.ok(response)
    }

    override fun authRegisterPost(registerRequest: RegisterRequest): ResponseEntity<AuthResponse> {
        log.atInfo().log("Received register request")

        val response = authService.register(registerRequest, authContext())

        log.atInfo()
            .addKeyValue("userId", response.user.id)
            .log("Register completed")
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    private fun authContext(): AuthContext {
        return AuthContext(
            ipAddress = clientIpAddress(),
            userAgent = httpServletRequest.getHeader(USER_AGENT_HEADER),
            details = null
        )
    }

    private fun clientIpAddress(): InetAddress? {
        val forwardedFor = httpServletRequest.getHeader(FORWARDED_FOR_HEADER)
            ?.split(",")
            ?.firstOrNull()
            ?.trim()
            ?.takeIf { it.isNotBlank() }
        val remoteAddress = httpServletRequest.remoteAddr?.takeIf { it.isNotBlank() }
        val ipAddress = forwardedFor ?: remoteAddress ?: return null
        return runCatching { InetAddress.getByName(ipAddress) }.getOrNull()
    }

    private companion object {
        const val USER_AGENT_HEADER = "User-Agent"
        const val FORWARDED_FOR_HEADER = "X-Forwarded-For"
    }
}
