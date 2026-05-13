package itis.boardgametracker.security

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Component
class CurrentUserProvider {
    fun currentUser(): CurrentUserPrincipal {
        val principal = SecurityContextHolder.getContext().authentication?.principal
        if (principal is CurrentUserPrincipal) {
            return principal
        }
        throw AuthenticationCredentialsNotFoundException("Current user principal is not available")
    }

    fun currentUserId(): Long {
        return currentUser().userId
    }
}
