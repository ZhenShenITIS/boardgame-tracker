package itis.boardgametracker.util

import java.sql.ResultSet
import java.time.Instant
import java.time.OffsetDateTime


fun ResultSet.getNullableInt(columnLabel: String): Int? =
    this.getObject(columnLabel, Integer::class.java)?.toInt()

fun ResultSet.getNullableLong(columnLabel: String): Long? =
    this.getObject(columnLabel, java.lang.Long::class.java)?.toLong()

fun ResultSet.getNullableDouble(columnLabel: String): Double? =
    this.getObject(columnLabel, java.lang.Double::class.java)?.toDouble()

fun ResultSet.getInstant(column: String): Instant =
    this.getObject(column, OffsetDateTime::class.java).toInstant()