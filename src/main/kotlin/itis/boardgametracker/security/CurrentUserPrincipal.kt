package itis.boardgametracker.security

data class CurrentUserPrincipal(
    val userId: Long,
    val email: String,
    val roles: List<String>
)
