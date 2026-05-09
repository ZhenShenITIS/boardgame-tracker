package itis.boardgametracker.util

import java.sql.ResultSet
import java.time.Instant
import java.time.OffsetDateTime

fun ResultSet.getNullableInt(columnLabel: String): Int? {
    val value = this.getInt(columnLabel)
    return if (this.wasNull()) null else value
}

fun ResultSet.getNullableLong(columnLabel: String): Long? {
    val value = this.getLong(columnLabel)
    return if (this.wasNull()) null else value
}

fun ResultSet.getNullableDouble(columnLabel: String): Double? {
    val value = this.getDouble(columnLabel)
    return if (this.wasNull()) null else value
}

fun ResultSet.getInstant(columnLabel: String): Instant {
    val offsetDateTime = this.getObject(columnLabel, OffsetDateTime::class.java)
    return offsetDateTime.toInstant()
}