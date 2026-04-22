package itis.boardgametracker.model

import itis.boardgametracker.constant.BoardGameType
import java.time.Instant

data class BoardGame(
    val id: Long? = null,
    val bggId: Long? = null,
    val type: BoardGameType = BoardGameType.BOARDGAME,
    val originalName: String,
    val displayName: String,
    val complexity: Double? = null,
    val minPlayers: Int? = null,
    val maxPlayers: Int? = null,
    val playingTime: Int? = null,
    val minPlayTime: Int? = null,
    val maxPlayTime: Int? = null,
    val minAge: Int? = null,
    val yearPublished: Int? = null,
    val s3ImageKey: String? = null,
    val s3PreviewKey: String? = null,
    val bggImageUrl: String? = null,
    val bggPreviewUrl: String? = null,
    val isCustom: Boolean = false,
    val createdById: Long? = null,
    val tags: List<Tag> = emptyList(),
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)