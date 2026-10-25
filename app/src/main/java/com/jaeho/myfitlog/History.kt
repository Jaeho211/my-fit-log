package com.jaeho.myfitlog

import org.json.JSONArray
import java.security.MessageDigest
import java.time.LocalDate

/** A snapshot check protects even legacy logs with no entryId and identical timestamps. */
data class HistoryKey(val index: Int, val fingerprint: String, val snapshot: String) {
    val value: String get() = JSONArray(listOf(index, fingerprint, snapshot)).toString()
}
data class HistoryRecord(
    val key: HistoryKey, val exerciseId: String, val name: String, val place: String,
    val time: Long, val kg: String, val reps: String, val memo: String,
)
data class HistoryGroup(val date: LocalDate, val exerciseId: String, val place: String, val records: List<HistoryRecord>) {
    val key: String get() = historyGroupKey(date, exerciseId, place)
    val name: String get() = records.last().name
}
data class HistoryDay(val date: LocalDate, val groups: List<HistoryGroup>) {
    val setCount: Int get() = groups.sumOf { it.records.size }
}
internal fun historyGroupKey(date: LocalDate, id: String, place: String): String =
    JSONArray(listOf(date.toString(), id, place)).toString()

internal fun historyDigest(value: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
    val hex = "0123456789abcdef"
    return buildString(bytes.size * 2) {
        bytes.forEach { byte ->
            val number = byte.toInt() and 0xff
            append(hex[number ushr 4]); append(hex[number and 15])
        }
    }
}
