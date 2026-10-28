package com.jaeho.myfitlog

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ExerciseDataTest {
    private val today = LocalDate.of(2026, 10, 25)
    private fun data() = FitData(JSONObject(FitData.EMPTY), ZoneId.of("Asia/Seoul"))

    @Test fun legacyDefaultsAndCatalogDoNotEnrollExercises() {
        val data = data()
        val legacy = ExerciseDraft.fromJson(JSONObject("""{"id":"old","name":"덤벨 컬","equipment":"덤벨","kg":"8","home":true}"""))
        assertEquals("3", legacy.effectiveHomeKg)
        assertEquals("25", legacy.homeReps)
        assertEquals("3", legacy.homeSets)
        assertEquals(22, ExerciseCatalog.size)
        val legRaise = ExerciseCatalog.single { it.name == "라잉 레그 레이즈" }
        assertEquals("복부", legRaise.category)
        assertEquals("맨몸", legRaise.equipment)
        assertEquals("0", legRaise.draft().kg)
        assertEquals("25", legRaise.draft().reps)
        assertEquals("3", legRaise.draft().sets)
        ExerciseCatalog.first().draft()
        assertTrue(data.exercises().isEmpty())
    }

    @Test fun editPreservesIdUnknownFieldsCurrentPlanAndHistoricLogs() {
        val data = data()
        val id = data.saveExercise(ExerciseDraft(name = "덤벨 컬", home = true))
        data.json.getJSONArray("exercises").getJSONObject(0).put("custom", "keep")
        data.applyPlan(today, "집", setOf(id))
        val plan = data.plans(today).single()
        data.record(plan, "3", "25", "수행 메모", 0,
            today.atTime(12, 0).atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli())
        val plansBefore = data.json.getJSONArray("plan").toString()
        val logsBefore = data.json.getJSONArray("logs").toString()
        val changed = data.exercises().single().copy(name = "해머 컬", kg = "10", reps = "12", note = "새 자세")
        assertEquals(id, data.saveExercise(changed))
        assertEquals("keep", data.json.getJSONArray("exercises").getJSONObject(0).getString("custom"))
        assertEquals(plansBefore, data.json.getJSONArray("plan").toString())
        assertEquals(logsBefore, data.json.getJSONArray("logs").toString())
        val restored = FitData(JSONObject(data.json.toString()))
        assertEquals(changed.name, restored.exercises().single().name)
        assertEquals(1, restored.json.getInt("version"))
    }

    @Test fun duplicateOrInvalidEditCannotPartiallyMutateData() {
        val data = data()
        data.saveExercise(ExerciseDraft(name = "덤벨 컬"))
        val before = data.json.toString()
        listOf(ExerciseDraft(name = "  덤벨   컬  "), ExerciseDraft(name = ""),
            ExerciseDraft(name = "해머 컬", reps = "2.5"), ExerciseDraft(name = "해머 컬", kg = "NaN"),
            ExerciseDraft(name = "해머 컬", sets = "0"), ExerciseDraft(name = "해머 컬", equipment = ""))
            .forEach { draft ->
                assertThrows(IllegalArgumentException::class.java) { data.saveExercise(draft) }
                assertEquals(before, data.json.toString())
            }
    }

    @Test fun homeValidationAndFixedDumbbellWeightApplyOnlyWhenUsed() {
        val data = data()
        val id = data.saveExercise(ExerciseDraft(name = "푸시업", equipment = "맨몸", home = false,
            homeKg = "bad", homeReps = "bad", homeSets = "bad"))
        val draft = data.exercises().single()
        assertEquals("0", draft.homeKg); assertEquals("25", draft.homeReps)
        assertThrows(IllegalArgumentException::class.java) {
            data.saveExercise(draft.copy(home = true, homeReps = "0"))
        }
        data.saveExercise(draft.copy(equipment = "덤벨", home = true, homeKg = "99", homeReps = "18", homeSets = "4"))
        data.applyPlan(today, "집", setOf(id))
        assertEquals("3", data.plans(today).single().kg)
        assertEquals("18", data.plans(today).single().reps)
        assertEquals(4, data.plans(today).single().sets)
    }

    @Test fun deleteKeepsLogsAndPastPlansAndRemovesOnlyTodayAndFutureReferences() {
        val data = data()
        val id = data.saveExercise(ExerciseDraft(name = "덤벨 컬"))
        val other = data.saveExercise(ExerciseDraft(name = "해머 컬"))
        listOf(today.minusDays(1), today, today.plusDays(1)).forEach {
            data.applyPlan(it, "헬스장", setOf(id, other))
        }
        data.json.getJSONArray("logs").put(JSONObject().put("id", id).put("name", "덤벨 컬").put("kg", "3"))
        val logsBefore = data.json.getJSONArray("logs").toString()
        data.deleteExercise(id, today)
        assertEquals(listOf(other), data.exercises().map { it.id })
        assertEquals(2, data.plans(today.minusDays(1)).size)
        assertEquals(listOf(other), data.plans(today).map { it.key.id })
        assertEquals(listOf(other), data.plans(today.plusDays(1)).map { it.key.id })
        assertEquals(logsBefore, data.json.getJSONArray("logs").toString())
        assertThrows(IllegalArgumentException::class.java) { data.saveExercise(ExerciseDraft(id = id, name = "덤벨 컬")) }
    }

    @Test fun legacyDuplicateNamesCanStillBeEditedWithoutCreatingAnotherDuplicate() {
        val data = data()
        val id = data.saveExercise(ExerciseDraft(name = "덤벨 컬"))
        data.json.getJSONArray("exercises").put(data.exercises().single().copy(id = "legacy-duplicate").toJson())
        data.saveExercise(data.exercises().first().copy(note = "메모만 수정"))
        assertEquals(id, data.exercises().first().id)
        assertEquals("메모만 수정", data.exercises().first().note)
        assertEquals(2, data.exercises().size)
    }
}
