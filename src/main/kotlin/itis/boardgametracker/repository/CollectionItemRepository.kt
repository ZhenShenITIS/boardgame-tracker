package itis.boardgametracker.repository

import itis.boardgametracker.constant.CollectionItemStatus
import itis.boardgametracker.model.CollectionItemRecord
import itis.boardgametracker.model.UserCollectionStatsAggregate
import itis.boardgametracker.util.CollectionItemRowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.sql.Types
import java.time.LocalDate

@Repository
class CollectionItemRepository(
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate
) {
    private val findByIdAndUserIdSql: String = """
        SELECT id, user_id, board_game_id, date_purchased, sum_in_rubles, status, play_count, comment, created_at, updated_at
        FROM collection_items
        WHERE id = :id AND user_id = :userId
    """.trimIndent()

    private val findByUserIdWithFiltersSql: String = """
        SELECT id, user_id, board_game_id, date_purchased, sum_in_rubles, status, play_count, comment, created_at, updated_at
        FROM collection_items
        WHERE user_id = :userId
          AND (:status IS NULL OR status = :status)
          AND (:shelfOfShameOnly = FALSE OR (status = 'IN_COLLECTION' AND play_count = 0))
        ORDER BY id
        LIMIT :limit
        OFFSET :offset
    """.trimIndent()

    private val countByUserIdWithFiltersSql: String = """
        SELECT COUNT(*)
        FROM collection_items
        WHERE user_id = :userId
          AND (:status IS NULL OR status = :status)
          AND (:shelfOfShameOnly = FALSE OR (status = 'IN_COLLECTION' AND play_count = 0))
    """.trimIndent()

    private val userCollectionStatsSql: String = """
        SELECT COUNT(*) FILTER (WHERE status = 'IN_COLLECTION') AS total_items,
               COUNT(*) FILTER (WHERE status = 'IN_COLLECTION' AND play_count > 0) AS played_items,
               COUNT(*) FILTER (WHERE status = 'IN_COLLECTION' AND play_count = 0) AS unplayed_items,
               COALESCE(SUM(sum_in_rubles) FILTER (
                   WHERE status = 'IN_COLLECTION' AND play_count = 0 AND sum_in_rubles IS NOT NULL
               ), 0) AS shelf_of_shame_cost
        FROM collection_items
        WHERE user_id = :userId
    """.trimIndent()

    private val countGlobalShelfOfShameItemsSql: String = """
        SELECT COUNT(*)
        FROM collection_items
        WHERE status = 'IN_COLLECTION'
          AND play_count = 0
    """.trimIndent()

    private val insertSql: String = """
        INSERT INTO collection_items (user_id, board_game_id, date_purchased, sum_in_rubles, status, comment)
        VALUES (:userId, :boardGameId, :datePurchased, :sumInRubles, :status, :comment)
        RETURNING id, user_id, board_game_id, date_purchased, sum_in_rubles, status, play_count, comment, created_at, updated_at
    """.trimIndent()

    private val updateSql: String = """
        UPDATE collection_items
        SET board_game_id = :boardGameId,
            date_purchased = :datePurchased,
            sum_in_rubles = :sumInRubles,
            status = :status,
            comment = :comment
        WHERE id = :id AND user_id = :userId
        RETURNING id, user_id, board_game_id, date_purchased, sum_in_rubles, status, play_count, comment, created_at, updated_at
    """.trimIndent()

    private val deleteByIdAndUserIdSql: String = """
        DELETE FROM collection_items
        WHERE id = :id AND user_id = :userId
    """.trimIndent()

    fun findByIdAndUserId(id: Long, userId: Long): CollectionItemRecord? {
        return namedParameterJdbcTemplate.query(
            findByIdAndUserIdSql,
            MapSqlParameterSource()
                .addValue("id", id, Types.BIGINT)
                .addValue("userId", userId, Types.BIGINT),
            CollectionItemRowMapper
        ).firstOrNull()
    }

    fun findByUserIdWithFilters(
        userId: Long,
        status: CollectionItemStatus?,
        shelfOfShameOnly: Boolean,
        limit: Int,
        offset: Int
    ): List<CollectionItemRecord> {
        return namedParameterJdbcTemplate.query(
            findByUserIdWithFiltersSql,
            mapFilters(userId, status, shelfOfShameOnly)
                .addValue("limit", limit, Types.INTEGER)
                .addValue("offset", offset, Types.INTEGER),
            CollectionItemRowMapper
        )
    }

    fun countByUserIdWithFilters(
        userId: Long,
        status: CollectionItemStatus?,
        shelfOfShameOnly: Boolean
    ): Int {
        return namedParameterJdbcTemplate.queryForObject(
            countByUserIdWithFiltersSql,
            mapFilters(userId, status, shelfOfShameOnly),
            Int::class.java
        ) ?: 0
    }

    fun create(
        userId: Long,
        boardGameId: Long,
        datePurchased: LocalDate?,
        sumInRubles: BigDecimal?,
        status: CollectionItemStatus,
        comment: String?
    ): CollectionItemRecord {
        return namedParameterJdbcTemplate.queryForObject(
            insertSql,
            mapWrite(userId, boardGameId, datePurchased, sumInRubles, status, comment),
            CollectionItemRowMapper
        ) ?: throw IllegalStateException("Failed to create collection item")
    }

    fun update(
        id: Long,
        userId: Long,
        boardGameId: Long,
        datePurchased: LocalDate?,
        sumInRubles: BigDecimal?,
        status: CollectionItemStatus,
        comment: String?
    ): CollectionItemRecord? {
        return namedParameterJdbcTemplate.query(
            updateSql,
            mapWrite(userId, boardGameId, datePurchased, sumInRubles, status, comment)
                .addValue("id", id, Types.BIGINT),
            CollectionItemRowMapper
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

    fun getUserCollectionStats(userId: Long): UserCollectionStatsAggregate {
        return namedParameterJdbcTemplate.queryForObject(
            userCollectionStatsSql,
            MapSqlParameterSource().addValue("userId", userId, Types.BIGINT)
        ) { rs, _ ->
            UserCollectionStatsAggregate(
                totalItems = rs.getInt("total_items"),
                playedItems = rs.getInt("played_items"),
                unplayedItems = rs.getInt("unplayed_items"),
                shelfOfShameCost = rs.getBigDecimal("shelf_of_shame_cost")
            )
        } ?: UserCollectionStatsAggregate(
            totalItems = 0,
            playedItems = 0,
            unplayedItems = 0,
            shelfOfShameCost = BigDecimal.ZERO
        )
    }

    fun countGlobalShelfOfShameItems(): Int {
        return namedParameterJdbcTemplate.queryForObject(
            countGlobalShelfOfShameItemsSql,
            emptyMap<String, Any>(),
            Int::class.java
        ) ?: 0
    }

    private fun mapFilters(
        userId: Long,
        status: CollectionItemStatus?,
        shelfOfShameOnly: Boolean
    ): MapSqlParameterSource {
        return MapSqlParameterSource()
            .addValue("userId", userId, Types.BIGINT)
            .addValue("status", status?.name, Types.VARCHAR)
            .addValue("shelfOfShameOnly", shelfOfShameOnly, Types.BOOLEAN)
    }

    private fun mapWrite(
        userId: Long,
        boardGameId: Long,
        datePurchased: LocalDate?,
        sumInRubles: BigDecimal?,
        status: CollectionItemStatus,
        comment: String?
    ): MapSqlParameterSource {
        return MapSqlParameterSource()
            .addValue("userId", userId, Types.BIGINT)
            .addValue("boardGameId", boardGameId, Types.BIGINT)
            .addValue("datePurchased", datePurchased, Types.DATE)
            .addValue("sumInRubles", sumInRubles, Types.NUMERIC)
            .addValue("status", status.name, Types.VARCHAR)
            .addValue("comment", comment, Types.VARCHAR)
    }
}
