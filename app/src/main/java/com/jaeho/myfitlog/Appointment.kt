package com.jaeho.myfitlog

import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

data class PtAppointment(val id: Int, val time: Long, val note: String, val minutes: Int) {
    val reminderAt: Long get() = time - minutes * 60_000L
    fun draft(zone: ZoneId = ZoneId.systemDefault()): AppointmentDraft {
        val local = Instant.ofEpochMilli(time).atZone(zone)
        return AppointmentDraft(id, local.toLocalDate().toString(), local.hour, local.minute, note, minutes.toString())
    }
}

data class AppointmentDraft(
    val id: Int? = null, val date: String = LocalDate.now().plusDays(1).toString(),
    val hour: Int = 19, val minute: Int = 0, val note: String = "", val minutes: String = "60",
) {
    fun time(zone: ZoneId = ZoneId.systemDefault()): Long {
        val local = LocalDateTime.of(LocalDate.parse(date), LocalTime.of(hour, minute))
        val zoned = local.atZone(zone)
        require(zoned.toLocalDateTime() == local) { "선택한 지역에서 사용할 수 없는 시간입니다" }
        return zoned.toInstant().toEpochMilli()
    }
    fun errors(now: Long, zone: ZoneId = ZoneId.systemDefault()): Map<String, String> = buildMap {
        val time = runCatching { time(zone) }.getOrNull()
        if (time == null) put("time", "날짜와 시간을 확인해 주세요")
        else if (time <= now) put("time", "미래 날짜와 시간을 선택해 주세요")
        if (!FitData.validReps(minutes)) put("minutes", "1~1,000분의 정수로 입력해 주세요")
    }
    fun toJson(): JSONObject = JSONObject().apply {
        if (id != null) put("id", id)
        put("date", date); put("hour", hour); put("minute", minute); put("note", note); put("minutes", minutes)
    }
    companion object {
        fun fromJson(value: JSONObject) = AppointmentDraft(
            if (value.has("id")) value.getInt("id") else null, value.getString("date"),
            value.getInt("hour"), value.getInt("minute"), value.optString("note"), value.optString("minutes", "60"),
        )
    }
}

data class PtReminder(val id: Int, val triggerAt: Long)
internal fun futurePtReminders(appointments: List<PtAppointment>, now: Long): List<PtReminder> = appointments
    .filter { it.time > now && it.reminderAt > now }.map { PtReminder(it.id, it.reminderAt) }
