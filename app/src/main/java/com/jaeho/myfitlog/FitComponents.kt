package com.jaeho.myfitlog

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

internal val ScreenPadding = 22.dp
internal val CardShape = RoundedCornerShape(22.dp)
internal val Korean = Locale.KOREA
internal fun formatTime(time: Long, pattern: String): String = Instant.ofEpochMilli(time)
    .atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern(pattern, Korean))
internal fun weightLabel(value: String) = when {
    value.isBlank() -> "무게 미기록"
    value.toDoubleOrNull() == 0.0 -> "맨몸"
    else -> "$value kg"
}
internal fun repsLabel(value: String) = if (value.isBlank()) "횟수 미기록" else "$value회"
