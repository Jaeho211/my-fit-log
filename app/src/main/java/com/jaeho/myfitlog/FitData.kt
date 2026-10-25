package com.jaeho.myfitlog

import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.UUID

data class PlanKey(val date: String, val id: String, val place: String) {
    val value: String get() = JSONArray(listOf(date, id, place)).toString()
}

data class ExercisePlan(
    val key: PlanKey, val name: String, val kg: String, val reps: String,
    val sets: Int, val note: String,
)
data class ExerciseChoice(val id: String, val name: String, val equipment: String)
data class SetEntry(val token: String, val time: Long, val kg: String, val reps: String, val memo: String)
data class AppointmentSummary(val time: Long, val note: String)
data class TodaySummary(
    val date: LocalDate,
    val plans: List<ExercisePlan>,
    val counts: Map<String, Int>,
    val exerciseCount: Int,
    val weeklyGoal: Int,
    val week: List<LocalDate>,
    val trainedDays: Set<LocalDate>,
    val nextAppointment: AppointmentSummary?,
)

/** Works on the original version-1 JSON without dropping unrecognized fields. */
class FitData(val json: JSONObject, private val zone: ZoneId = ZoneId.systemDefault()) {
    private fun array(key: String): JSONArray = json.getJSONArray(key)
    private fun date(time: Long): LocalDate = Instant.ofEpochMilli(time).atZone(zone).toLocalDate()
    private fun objects(key: String): List<JSONObject> = array(key).let { a ->
        (0 until a.length()).map { a.getJSONObject(it) }
    }
    private fun plan(p: JSONObject) = ExercisePlan(
        PlanKey(p.getString("date"), p.getString("id"), p.getString("place")),
        p.optString("name"), p.optString("kg", "0"), p.optString("reps", "12"),
        p.optInt("sets", 3).coerceIn(1, 1000), p.optString("note"),
    )
    fun plans(today: LocalDate): List<ExercisePlan> = objects("plan")
        .filter { it.optString("date") == today.toString() }.map(::plan)

    private fun matches(log: JSONObject, p: ExercisePlan): Boolean =
        log.optString("id") == p.key.id && log.optString("place") == p.key.place

    private fun entry(log: JSONObject) = SetEntry(
        log.optString("entryId"), log.optLong("time"), log.optString("kg"),
        log.optString("reps"), log.optString("memo"),
    )
    fun sets(p: ExercisePlan): List<SetEntry> = objects("logs")
        .filter { matches(it, p) && date(it.optLong("time")).toString() == p.key.date }
        .sortedBy { it.optLong("time") }.map(::entry)

    fun previous(p: ExercisePlan): SetEntry? = objects("logs")
        .filter { matches(it, p) && date(it.optLong("time")).toString() < p.key.date }
        .maxByOrNull { it.optLong("time") }?.let(::entry)

    fun summary(now: Long): TodaySummary {
        val today = date(now)
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val week = (0L..6L).map(monday::plusDays)
        val trained = objects("logs").filter { it.optLong("time") <= now }
            .map { date(it.optLong("time")) }.filter { it in week }.toSet()
        val plans = plans(today)
        val next = objects("appointments").filter { it.optLong("time") > now }
            .minByOrNull { it.optLong("time") }
        return TodaySummary(today, plans, plans.associate { it.key.value to sets(it).size },
            array("exercises").length(), json.optInt("weekly", 3).coerceIn(1, 7), week, trained,
            next?.let { AppointmentSummary(it.optLong("time"), it.optString("note")) })
    }

    fun choices(place: String): List<ExerciseChoice> = objects("exercises")
        .filter { place != "집" || it.optBoolean("home") }
        .sortedBy { exercise -> objects("logs").filter { it.optString("id") == exercise.optString("id") }
            .maxOfOrNull { it.optLong("time") } ?: 0L }
        .map { ExerciseChoice(it.getString("id"), it.optString("name"), it.optString("equipment")) }

    fun applyPlan(today: LocalDate, place: String, ids: Set<String>) {
        require(place in listOf("집", "헬스장", "PT"))
        val eligible = choices(place).map { it.id }.toSet()
        require(ids.isNotEmpty() && eligible.containsAll(ids)) { "운동을 하나 이상 선택해 주세요" }
        val replacement = JSONArray()
        objects("plan").filter { it.optString("date") != today.toString() }.forEach(replacement::put)
        objects("exercises").filter { it.optString("id") in ids }.forEach { e ->
            val home = place == "집"
            replacement.put(JSONObject().apply {
                put("date", today.toString()); put("id", e.getString("id")); put("name", e.optString("name"))
                put("place", place); put("note", e.optString("note"))
                put("kg", if (home) {
                    if (e.optString("equipment").contains("덤벨")) "3" else e.optString("homeKg", "0")
                } else e.optString("kg", "0"))
                put("reps", if (home) e.optString("homeReps", "25") else e.optString("reps", "25"))
                put("sets", if (home) e.optString("homeSets", "3") else e.optString("sets", "3"))
            })
        }
        json.put("plan", replacement)
    }

    /** Reject a second tap that still refers to the same displayed set count. */
    fun record(p: ExercisePlan, kg: String, reps: String, memo: String, expectedCount: Int, now: Long): String {
        require(p.key.date == date(now).toString()) { "날짜가 바뀌었어요. 오늘 루틴을 다시 확인해 주세요" }
        require(plans(date(now)).any { it.key == p.key }) { "루틴이 변경되었어요" }
        require(sets(p).size == expectedCount) { "이미 기록했어요. 세트 상태를 확인해 주세요" }
        require(validWeight(kg) && validReps(reps)) { "무게는 0~1,000 kg, 횟수는 1~1,000의 정수로 입력해 주세요" }
        val token = UUID.randomUUID().toString()
        array("logs").put(JSONObject().apply {
            put("time", now); put("id", p.key.id); put("name", p.name); put("place", p.key.place)
            put("kg", kg.trim()); put("reps", reps.trim()); put("memo", memo.trim()); put("entryId", token)
        })
        return token
    }

    fun hasEntry(token: String): Boolean = token.isNotBlank() && objects("logs").any { it.optString("entryId") == token }

    fun undo(token: String): Boolean {
        val logs = array("logs")
        for (i in logs.length() - 1 downTo 0) {
            if (token.isNotBlank() && logs.getJSONObject(i).optString("entryId") == token) {
                logs.remove(i)
                return true
            }
        }
        return false
    }
    fun removePlan(key: PlanKey) {
        val plans = array("plan")
        for (i in plans.length() - 1 downTo 0) {
            if (plan(plans.getJSONObject(i)).key == key) plans.remove(i)
        }
    }
    companion object {
        const val EMPTY = """{"version":1,"exercises":[],"logs":[],"plan":[],"appointments":[],"weekly":3}"""
        fun validWeight(value: String): Boolean = value.trim().toDoubleOrNull()
            ?.let { it.isFinite() && it in 0.0..1000.0 } == true
        fun validReps(value: String): Boolean = value.trim().toIntOrNull()?.let { it in 1..1000 } == true
    }
}
