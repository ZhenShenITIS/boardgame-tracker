package itis.boardgametracker.util

import itis.boardgametracker.model.User
import org.springframework.jdbc.core.RowMapper
import java.sql.ResultSet

object UserRowMapper : RowMapper<User> {
    override fun mapRow(rs: ResultSet, rowNum: Int): User {
        return User(
            id = rs.getLong("id"),
            name = rs.getString("name"),
            email = rs.getString("email"),
            password = rs.getString("password"),
            createdAt = rs.getInstant("created_at")
        )
    }
}
