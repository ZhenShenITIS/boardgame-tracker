package itis.boardgametracker.exception

open class AuthException(message: String) : RuntimeException(message)

class DuplicateEmailException : AuthException("Email already exists")

class InvalidCredentialsException : AuthException("Invalid credentials")

class InvalidOrExpiredRefreshTokenException : AuthException("Invalid or expired refresh token")

class RevokedRefreshTokenReuseException : AuthException("Revoked refresh token reuse detected")

class OldPasswordMismatchException : AuthException("Old password mismatch")

class UserNotFoundException : AuthException("User not found")
