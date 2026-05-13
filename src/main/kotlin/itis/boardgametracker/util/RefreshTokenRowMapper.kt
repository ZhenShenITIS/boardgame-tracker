package itis.boardgametracker.util

import itis.boardgametracker.model.RefreshToken
import org.springframework.jdbc.core.RowMapper
import java.sql.ResultSet

object RefreshTokenRowMapper : RowMapper<RefreshToken> {
    override fun mapRow(rs: ResultSet, rowNum: Int): RefreshToken {
        return RefreshToken(
            id = rs.getLong("id"),
            userId = rs.getLong("user_id"),
            tokenHash = rs.getString("token_hash"),
            expiresAt = rs.getInstant("expires_at"),
            revoked = rs.getBoolean("revoked"),
            createdAt = rs.getInstant("created_at")
        )
    }
}
