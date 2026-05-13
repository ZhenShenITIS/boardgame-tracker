package itis.boardgametracker.service.auth

import itis.boardgametracker.exception.InvalidOrExpiredRefreshTokenException
import itis.boardgametracker.exception.RevokedRefreshTokenReuseException
import itis.boardgametracker.model.RefreshToken
import itis.boardgametracker.properties.JwtProperties
import itis.boardgametracker.repository.RefreshTokenRepository
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64

@Service
class RefreshTokenService(
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwtProperties: JwtProperties,
    private val authAuditService: AuthAuditService
) {
    data class RefreshTokenPair(
        val rawToken: String,
        val persistedToken: RefreshToken
    )

    fun issue(userId: Long): RefreshTokenPair {
        val rawToken = generateOpaqueToken()
        val tokenHash = hash(rawToken)
        val persisted = refreshTokenRepository.create(
            RefreshToken(
                userId = userId,
                tokenHash = tokenHash,
                expiresAt = Instant.now().plusSeconds(jwtProperties.refreshTokenTtlSeconds)
            )
        )
        return RefreshTokenPair(
            rawToken = rawToken,
            persistedToken = persisted
        )
    }

    fun refresh(rawToken: String, context: AuthContext?): RefreshTokenPair {
        val persisted = findByRawToken(rawToken)

        if (persisted.revoked) {
            authAuditService.write(
                action = "refresh_reuse_detected",
                userId = persisted.userId,
                context = context
            )
            refreshTokenRepository.revokeAllByUserId(persisted.userId)
            throw RevokedRefreshTokenReuseException()
        }

        if (persisted.expiresAt <= Instant.now()) {
            throw InvalidOrExpiredRefreshTokenException()
        }

        refreshTokenRepository.revokeByTokenHash(persisted.tokenHash)
        return issue(persisted.userId)
    }

    fun logout(rawToken: String, userId: Long) {
        val persisted = findByRawTokenAndUserId(rawToken, userId)
        refreshTokenRepository.revokeByTokenHash(persisted.tokenHash)
    }

    fun revokeAllByUserId(userId: Long) {
        refreshTokenRepository.revokeAllByUserId(userId)
    }

    private fun findByRawToken(rawToken: String): RefreshToken {
        val tokenHash = hash(rawToken)
        return try {
            refreshTokenRepository.findByTokenHash(tokenHash)
        } catch (_: EmptyResultDataAccessException) {
            throw InvalidOrExpiredRefreshTokenException()
        }
    }

    private fun findByRawTokenAndUserId(rawToken: String, userId: Long): RefreshToken {
        val tokenHash = hash(rawToken)
        return try {
            refreshTokenRepository.findByTokenHashAndUserId(tokenHash, userId)
        } catch (_: EmptyResultDataAccessException) {
            throw InvalidOrExpiredRefreshTokenException()
        }
    }

    private fun hash(rawToken: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(rawToken.toByteArray(StandardCharsets.UTF_8))
        return Base64.getEncoder().encodeToString(bytes)
    }

    private fun generateOpaqueToken(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}
