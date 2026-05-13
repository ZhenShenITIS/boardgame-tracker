package itis.boardgametracker.util

import itis.boardgametracker.model.Role
import org.springframework.jdbc.core.RowMapper
import java.sql.ResultSet
import java.time.Instant

object RoleRowMapper : RowMapper<Role> {
    override fun mapRow(rs: ResultSet, rowNum: Int): Role {
        return Role(
            id = rs.getLong("id"),
            name = rs.getString("name"),
            createdAt = Instant.now()
        )
    }
}
