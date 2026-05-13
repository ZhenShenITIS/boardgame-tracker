package itis.boardgametracker.repository

import itis.boardgametracker.model.Role
import itis.boardgametracker.util.RoleRowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class RoleRepository(
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate
) {
    private val findByNameSql: String = """
        SELECT id, name
        FROM roles
        WHERE name = :name
    """.trimIndent()

    private val findByUserIdSql: String = """
        SELECT r.id, r.name
        FROM roles r
                 JOIN users_roles ur ON ur.role_id = r.id
        WHERE ur.user_id = :userId
        ORDER BY r.name
    """.trimIndent()

    private val assignRoleToUserSql: String = """
        INSERT INTO users_roles (user_id, role_id)
        VALUES (:userId, :roleId)
        ON CONFLICT (user_id, role_id) DO NOTHING
    """.trimIndent()

    fun findByName(name: String): Role {
        return namedParameterJdbcTemplate.queryForObject(
            findByNameSql,
            MapSqlParameterSource().addValue("name", name),
            RoleRowMapper
        )!!
    }

    fun findByUserId(userId: Long): List<Role> {
        return namedParameterJdbcTemplate.query(
            findByUserIdSql,
            MapSqlParameterSource().addValue("userId", userId),
            RoleRowMapper
        )
    }

    fun assignRoleToUser(userId: Long, roleId: Long) {
        namedParameterJdbcTemplate.update(
            assignRoleToUserSql,
            MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("roleId", roleId)
        )
    }
}
