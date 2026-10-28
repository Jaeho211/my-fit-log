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

    fun appointments(): List<PtAppointment> = objects("appointments").map {
        PtAppointment(it.getInt("id"), it.getLong("time"), it.optString("note"), it.optInt("minutes", 60))
    }.sortedBy { it.time }

    fun saveAppointment(draft: AppointmentDraft, now: Long): Int {
        require(draft.errors(now, zone).isEmpty()) { draft.errors(now, zone).values.first() }
        val appointments = array("appointments")
        val existingIndex = (0 until appointments.length()).firstOrNull {
            appointments.getJSONObject(it).getInt("id") == draft.id
        }
        require(draft.id == null || existingIndex != null) { "삭제된 예약입니다. 목록을 다시 확인해 주세요" }
        val used = this.appointments().map { it.id }.toSet()
        val id = draft.id ?: (1..Int.MAX_VALUE).first { it !in used }
        val updated = if (existingIndex == null) JSONObject() else JSONObject(appointments.getJSONObject(existingIndex).toString())
        updated.put("id", id); updated.put("time", draft.time(zone)); updated.put("note", draft.note.trim())
        updated.put("minutes", draft.minutes.trim().toInt())
        if (existingIndex == null) appointments.put(updated) else appointments.put(existingIndex, updated)
        return id
    }

    fun deleteAppointment(id: Int) {
        val appointments = array("appointments")
        val index = (0 until appointments.length()).firstOrNull { appointments.getJSONObject(it).getInt("id") == id }
        require(index != null) { "이미 삭제된 예약입니다" }
        appointments.remove(index)
    }

    fun history(): List<HistoryDay> {
        val snapshot = historyDigest(array("logs").toString())
        val records = objects("logs").mapIndexed { index, log ->
            HistoryRecord(HistoryKey(index, historyDigest(log.toString()), snapshot),
                log.optString("id"), log.optString("name").ifBlank { "이름 없는 운동" },
                log.optString("place").ifBlank { "장소 미지정" }, log.optLong("time"),
                log.optString("kg", "0"), log.optString("reps", "0"), log.optString("memo"))
        }
        return records.groupBy { date(it.time) }.toSortedMap(compareByDescending { it })
            .map { (day, entries) ->
                val groups = entries.groupBy { it.exerciseId to it.place }.map { (key, sets) ->
                    HistoryGroup(day, key.first, key.second, sets.sortedWith(compareBy({ it.time }, { it.key.index })))
                }.sortedByDescending { it.records.last().time }
                HistoryDay(day, groups)
            }
    }

    /**
     * Delete all sets in a PT day/exercise group atomically.
     * A full-history snapshot check prevents stale UI selection deleting unrelated sets.
     */
    fun deletePtHistoryGroup(group: HistoryGroup) {
        require(group.place == "PT" && group.records.isNotEmpty()) { "PT 운동 기록만 전체 삭제할 수 있어요" }
        val current = history().flatMap { it.groups }.firstOrNull { it.key == group.key }
        require(current != null && current.records.map { it.key } == group.records.map { it.key }) {
            "운동 기록이 변경됐어요. 목록을 다시 확인해 주세요"
        }
        val logs = array("logs")
        group.records.map { it.key.index }.sortedDescending().forEach(logs::remove)
    }

    /** Delete one exercise's PT sets for the selected day, with stale-record protection. */
    fun deletePtHistoryGroup(group: HistoryGroup) {
        require(group.place == "PT" && group.records.isNotEmpty()) { "PT 운동 기록만 전체 삭제할 수 있어요" }
        val current = history().flatMap { it.groups }.firstOrNull { it.key == group.key }
        require(current != null && current.records.map { it.key } == group.records.map { it.key }) {
            "운동 기록이 변경됐어요. 목록을 다시 확인해 주세요"
        }
        val logs = array("logs")
        group.records.map { it.key.index }.sortedDescending().forEach(logs::remove)
    }

    fun deleteHistoryRecord(key: HistoryKey) {
        val logs = array("logs")
        require(historyDigest(logs.toString()) == key.snapshot && key.index in 0 until logs.length() &&
            historyDigest(logs.getJSONObject(key.index).toString()) == key.fingerprint) {
            "기록이 변경되었어요. 목록에서 다시 선택해 주세요"
        }
        logs.remove(key.index)
    }

    fun exercises(): List<ExerciseDraft> = objects("exercises").map(ExerciseDraft::fromJson)

    fun saveExercise(draft: ExerciseDraft): String {
        require(draft.errors().isEmpty()) { "입력값을 확인해 주세요" }
        val existing = objects("exercises").firstOrNull { it.optString("id") == draft.id }
        require(draft.id.isBlank() || existing != null) { "삭제된 운동입니다. 내 운동 목록을 다시 확인해 주세요" }
        val normalized = ExerciseDraft.normalizedName(draft.name)
        val unchangedName = existing != null && normalized == ExerciseDraft.normalizedName(existing.optString("name"))
        require(unchangedName || objects("exercises").none {
            it.optString("id") != draft.id && ExerciseDraft.normalizedName(it.optString("name")) == normalized
        }) { "이미 등록한 운동 이름입니다. 다른 이름으로 구분해 주세요" }
        val id = existing?.getString("id") ?: UUID.randomUUID().toString()
        // Clone before editing so validation failures cannot partially mutate a saved exercise.
        val updated = if (existing == null) JSONObject() else JSONObject(existing.toString())
        val values = draft.copy(id = id, name = draft.name.trim(), equipment = draft.equipment.trim(),
            kg = draft.kg.trim(), reps = draft.reps.trim(), sets = draft.sets.trim(),
            homeKg = if (FitData.validWeight(draft.effectiveHomeKg)) draft.effectiveHomeKg.trim() else "0",
            homeReps = if (FitData.validReps(draft.homeReps)) draft.homeReps.trim() else "25",
            homeSets = if (FitData.validReps(draft.homeSets)) draft.homeSets.trim() else "3").toJson()
        values.keys().forEach { key -> updated.put(key, values.get(key)) }
        val exercises = array("exercises")
        val index = (0 until exercises.length()).firstOrNull { exercises.getJSONObject(it).optString("id") == id }
        if (index == null) exercises.put(updated) else exercises.put(index, updated)
        // Plans/logs are snapshots: editing a target does not rewrite an ongoing workout or history.
        return id
    }

    fun deleteExercise(id: String, today: LocalDate) {
        val exercises = array("exercises")
        for (i in exercises.length() - 1 downTo 0) {
            if (exercises.getJSONObject(i).optString("id") == id) exercises.remove(i)
        }
        val plans = array("plan")
        for (i in plans.length() - 1 downTo 0) {
            val p = plans.getJSONObject(i)
            if (p.optString("id") == id && p.optString("date") >= today.toString()) plans.remove(i)
        }
    }

    fun choices(place: String): List<ExerciseChoice> = objects("exercises")
        .filter { place != "집" || it.optBoolean("home") }
        .sortedBy { exercise -> objects("logs").filter { it.optString("id") == exercise.optString("id") }
            .maxOfOrNull { it.optLong("time") } ?: 0L }
        .map { ExerciseChoice(it.getString("id"), it.optString("name"), it.optString("equipment")) }

    fun applyPlan(today: LocalDate, place: String, ids: Set<String>) {
        require(place in listOf("집", "헬스장"))
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

    /** PT sessions are recorded after exercise, without an active routine. */
    fun recordPt(exerciseId: String, kg: String, reps: String, setCount: String, memo: String, now: Long) {
        val exercise = objects("exercises").firstOrNull { it.optString("id") == exerciseId }
        require(exercise != null) { "먼저 내 운동에서 운동을 등록해 주세요" }
        require(kg.isBlank() || validWeight(kg)) { "무게는 비워 두거나 0~1,000 kg으로 입력해 주세요" }
        require(reps.isBlank() || validReps(reps)) { "횟수는 비워 두거나 1~1,000의 정수로 입력해 주세요" }
        val count = setCount.trim().toIntOrNull()
        require(count != null && count in 1..100) { "세트 수는 1~100의 정수로 입력해 주세요" }
        val logs = array("logs")
        repeat(count) {
            logs.put(JSONObject().apply {
                put("time", now); put("id", exerciseId); put("name", exercise.optString("name"))
                put("place", "PT"); put("kg", kg.trim()); put("reps", reps.trim())
                put("memo", memo.trim()); put("entryId", UUID.randomUUID().toString())
            })
        }
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
