package itis.boardgametracker.repository

import itis.boardgametracker.model.PlaySessionRecord
import itis.boardgametracker.util.PlaySessionRowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.Types
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Repository
class PlaySessionRepository(
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate
) {
    private val findByCollectionItemIdAndUserIdSql: String = """
        SELECT ps.id, ps.collection_item_id, ps.date_start, ps.date_end, ps.comment, ps.created_at, ps.updated_at
        FROM play_sessions ps
        JOIN collection_items ci ON ci.id = ps.collection_item_id
        WHERE ps.collection_item_id = :collectionItemId
          AND ci.user_id = :userId
        ORDER BY ps.date_start DESC, ps.id DESC
        LIMIT :limit
        OFFSET :offset
    """.trimIndent()

    private val countByCollectionItemIdAndUserIdSql: String = """
        SELECT COUNT(*)
        FROM play_sessions ps
        JOIN collection_items ci ON ci.id = ps.collection_item_id
        WHERE ps.collection_item_id = :collectionItemId
          AND ci.user_id = :userId
    """.trimIndent()

    private val findByIdAndUserIdSql: String = """
        SELECT ps.id, ps.collection_item_id, ps.date_start, ps.date_end, ps.comment, ps.created_at, ps.updated_at
        FROM play_sessions ps
        JOIN collection_items ci ON ci.id = ps.collection_item_id
        WHERE ps.id = :id
          AND ci.user_id = :userId
    """.trimIndent()

    private val insertSql: String = """
        INSERT INTO play_sessions (collection_item_id, date_start, date_end, comment)
        VALUES (:collectionItemId, :dateStart, :dateEnd, :comment)
        RETURNING id, collection_item_id, date_start, date_end, comment, created_at, updated_at
    """.trimIndent()

    private val updateByIdAndUserIdSql: String = """
        UPDATE play_sessions ps
        SET date_start = :dateStart,
            date_end = :dateEnd,
            comment = :comment
        FROM collection_items ci
        WHERE ps.id = :id
          AND ps.collection_item_id = ci.id
          AND ci.user_id = :userId
        RETURNING ps.id, ps.collection_item_id, ps.date_start, ps.date_end, ps.comment, ps.created_at, ps.updated_at
    """.trimIndent()

    private val deleteByIdAndUserIdSql: String = """
        DELETE FROM play_sessions ps
        USING collection_items ci
        WHERE ps.id = :id
          AND ps.collection_item_id = ci.id
          AND ci.user_id = :userId
    """.trimIndent()

    fun findByCollectionItemIdAndUserId(
        collectionItemId: Long,
        userId: Long,
        limit: Int,
        offset: Int
    ): List<PlaySessionRecord> {
        return namedParameterJdbcTemplate.query(
            findByCollectionItemIdAndUserIdSql,
            mapAccess(collectionItemId, userId)
                .addValue("limit", limit, Types.INTEGER)
                .addValue("offset", offset, Types.INTEGER),
            PlaySessionRowMapper
        )
    }

    fun countByCollectionItemIdAndUserId(collectionItemId: Long, userId: Long): Int {
        return namedParameterJdbcTemplate.queryForObject(
            countByCollectionItemIdAndUserIdSql,
            mapAccess(collectionItemId, userId),
            Int::class.java
        ) ?: 0
    }

    fun findByIdAndUserId(id: Long, userId: Long): PlaySessionRecord? {
        return namedParameterJdbcTemplate.query(
            findByIdAndUserIdSql,
            MapSqlParameterSource()
                .addValue("id", id, Types.BIGINT)
                .addValue("userId", userId, Types.BIGINT),
            PlaySessionRowMapper
        ).firstOrNull()
    }

    fun create(
        collectionItemId: Long,
        dateStart: Instant,
        dateEnd: Instant?,
        comment: String?
    ): PlaySessionRecord {
        return namedParameterJdbcTemplate.queryForObject(
            insertSql,
            mapWrite(collectionItemId, dateStart, dateEnd, comment),
            PlaySessionRowMapper
        ) ?: throw IllegalStateException("Failed to create play session")
    }

    fun updateByIdAndUserId(
        id: Long,
        userId: Long,
        dateStart: Instant,
        dateEnd: Instant?,
        comment: String?
    ): PlaySessionRecord? {
        return namedParameterJdbcTemplate.query(
            updateByIdAndUserIdSql,
            mapWrite(collectionItemId = null, dateStart = dateStart, dateEnd = dateEnd, comment = comment)
                .addValue("id", id, Types.BIGINT)
                .addValue("userId", userId, Types.BIGINT),
            PlaySessionRowMapper
        ).firstOrNull()
    }

    fun deleteByIdAndUserId(id: Long, userId: Long): Int {
        return namedParameterJdbcTemplate.update(
            deleteByIdAndUserIdSql,
            MapSqlParameterSource()
                .addValue("id", id, Types.BIGINT)
                .addValue("userId", userId, Types.BIGINT)
        )
    }

    private fun mapAccess(collectionItemId: Long, userId: Long): MapSqlParameterSource {
        return MapSqlParameterSource()
            .addValue("collectionItemId", collectionItemId, Types.BIGINT)
            .addValue("userId", userId, Types.BIGINT)
    }

    private fun mapWrite(
        collectionItemId: Long?,
        dateStart: Instant,
        dateEnd: Instant?,
        comment: String?
    ): MapSqlParameterSource {
        val map = MapSqlParameterSource()
            .addValue(
                "dateStart",
                OffsetDateTime.ofInstant(dateStart, ZoneOffset.UTC),
                Types.TIMESTAMP_WITH_TIMEZONE
            )
            .addValue(
                "dateEnd",
                dateEnd?.let { OffsetDateTime.ofInstant(it, ZoneOffset.UTC) },
                Types.TIMESTAMP_WITH_TIMEZONE
            )
            .addValue("comment", comment, Types.VARCHAR)

        if (collectionItemId != null) {
            map.addValue("collectionItemId", collectionItemId, Types.BIGINT)
        }

        return map
    }
}
