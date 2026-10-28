package com.jaeho.myfitlog

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import java.time.LocalDate

class FitViewModel(application: Application, private val saved: SavedStateHandle) : AndroidViewModel(application) {
    private val repository = FitRepository(application)
    private var data = FitData(repository.read())
    var summary by mutableStateOf(data.summary(System.currentTimeMillis()))
        private set
    var section by mutableStateOf(saved.get<String>("section") ?: "오늘")
        private set
    var exercises by mutableStateOf(data.exercises())
        private set
    var history by mutableStateOf(data.history())
        private set
    var appointments by mutableStateOf(data.appointments())
        private set
    var active by mutableStateOf<ExercisePlan?>(null)
        private set
    var sets by mutableStateOf(emptyList<SetEntry>())
        private set
    var previous by mutableStateOf<SetEntry?>(null)
        private set
    var kg by mutableStateOf(saved.get<String>("kg") ?: "")
        private set
    var reps by mutableStateOf(saved.get<String>("reps") ?: "")
        private set
    var memo by mutableStateOf(saved.get<String>("memo") ?: "")
        private set
    var undoToken by mutableStateOf(saved.get<String>("undo") ?: "")
        private set
    var error by mutableStateOf<String?>(null)
        private set

    init { refresh() }

    fun refresh() {
        runCatching {
            data = FitData(repository.read())
            summary = data.summary(System.currentTimeMillis())
            exercises = data.exercises()
            history = data.history()
            appointments = data.appointments()
            active = summary.plans.firstOrNull { it.key.value == saved.get<String>("active") }
            if (active == null) saved["active"] = ""
            if (!data.hasEntry(undoToken)) { undoToken = ""; saved["undo"] = "" }
            updateWorkout()
        }.onFailure { error = "저장 데이터를 읽을 수 없습니다. ${it.message.orEmpty()}" }
    }
    fun refreshDate() { if (LocalDate.now() != summary.date) refresh() }
    private fun updateWorkout() {
        sets = active?.let(data::sets).orEmpty()
        previous = active?.let(data::previous)
    }
    fun open(p: ExercisePlan) {
        active = p
        saved["active"] = p.key.value
        if (saved.get<String>("draft") != p.key.value) {
            saved["draft"] = p.key.value
            editKg(p.kg); editReps(p.reps); editMemo("")
        }
        error = null
        updateWorkout()
    }
    fun home() { section = "오늘"; saved["section"] = "오늘"; active = null; saved["active"] = ""; error = null }
    fun navigate(destination: String) {
        home()
        section = if (destination in listOf("운동", "기록", "예약", "PT기록")) destination else "오늘"
        saved["section"] = section
    }
    fun saveExercise(draft: ExerciseDraft): String? {
        var id: String? = null
        val success = mutate { id = data.saveExercise(draft) }
        return if (success) id else null
    }
    fun deleteExercise(id: String): Boolean = mutate { data.deleteExercise(id, LocalDate.now()) }
    fun deleteHistoryRecord(key: HistoryKey): Boolean = mutate { data.deleteHistoryRecord(key) }
    fun deletePtHistoryGroup(group: HistoryGroup): Boolean = mutate { data.deletePtHistoryGroup(group) }
    fun deletePtHistoryGroup(group: HistoryGroup): Boolean = mutate { data.deletePtHistoryGroup(group) }

    fun saveAppointment(draft: AppointmentDraft): Int? {
        var id: Int? = null
        val success = mutate { id = data.saveAppointment(draft, System.currentTimeMillis()) }
        return if (success) id else null
    }
    fun deleteAppointment(id: Int): Boolean = mutate { data.deleteAppointment(id) }
    fun clearError() { error = null }
    fun editKg(value: String) { kg = value; saved["kg"] = value; error = null }
    fun editReps(value: String) { reps = value; saved["reps"] = value; error = null }
    fun editMemo(value: String) { memo = value; saved["memo"] = value }
    fun choices(place: String) = data.choices(place)
    fun applyPlan(place: String, ids: Set<String>, expectedDate: LocalDate) {
        mutate {
            require(expectedDate == LocalDate.now()) { "날짜가 바뀌었어요. 루틴을 다시 구성해 주세요" }
            data.applyPlan(expectedDate, place, ids)
            home()
        }
    }
    private var lastPtRecordAt = 0L
    fun recordPt(exerciseId: String, kg: String, reps: String, setCount: String, memo: String): Boolean {
        val elapsed = android.os.SystemClock.elapsedRealtime()
        if (elapsed - lastPtRecordAt < 650L) return false
        return mutate {
            data.recordPt(exerciseId, kg, reps, setCount, memo, System.currentTimeMillis())
            lastPtRecordAt = elapsed
        }
    }

    private var lastRecordAt = 0L
    fun record(expectedCount: Int) {
        val elapsed = android.os.SystemClock.elapsedRealtime()
        if (elapsed - lastRecordAt < 650L) return
        val p = active ?: return
        mutate {
            val token = data.record(p, kg, reps, memo, expectedCount, System.currentTimeMillis())
            lastRecordAt = elapsed
            undoToken = token; saved["undo"] = token
            editMemo("")
        }
    }
    fun undo() {
        mutate { data.undo(undoToken); undoToken = ""; saved["undo"] = "" }
    }
    fun removeActive() {
        val p = active ?: return
        mutate { data.removePlan(p.key); home() }
    }
    private fun mutate(action: () -> Unit): Boolean {
        error = null
        return runCatching {
            action()
            repository.write(data.json)
            refresh()
            true
        }.getOrElse { error = it.message ?: "변경 사항을 저장할 수 없습니다"; false }
    }
}
