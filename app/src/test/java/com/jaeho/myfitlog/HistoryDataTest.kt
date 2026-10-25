package com.jaeho.myfitlog

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class HistoryDataTest {
    private val zone = ZoneId.of("Asia/Seoul")
    private val today = LocalDate.of(2026, 10, 25)
    private fun data() = FitData(JSONObject(FitData.EMPTY), zone)
    private fun time(date: LocalDate, hour: Int = 12) = date.atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()
    private fun log(date: LocalDate, id: String = "pull", place: String = "헬스장", name: String = "랫풀다운", hour: Int = 12) =
        JSONObject().put("id", id).put("name", name).put("place", place).put("time", time(date, hour))
            .put("kg", "20").put("reps", "12").put("memo", "어깨 확인")

    @Test fun historyGroupsByLocalDayIdAndPlaceAndSortsSetsChronologically() {
        val data = data(); val logs = data.json.getJSONArray("logs")
        logs.put(log(today, hour = 18)); logs.put(log(today.minusDays(1)))
        logs.put(log(today, hour = 10)); logs.put(log(today, place = "PT"))
        logs.put(log(today, id = "other", name = "랫풀다운"))
        val days = data.history()
        assertEquals(listOf(today, today.minusDays(1)), days.map { it.date })
        assertEquals(3, days.first().groups.size); assertEquals(4, days.first().setCount)
        val group = days.first().groups.first { it.exerciseId == "pull" && it.place == "헬스장" }
        assertEquals(listOf(time(today, 10), time(today, 18)), group.records.map { it.time })
    }

    @Test fun deletedAndRenamedExercisesKeepTheirRecordedNamesAndMemoWithoutRewritingData() {
        val data = data(); val logs = data.json.getJSONArray("logs")
        logs.put(log(today.minusDays(1), name = "배웠던 이름"))
        logs.put(log(today, name = "바꾼 이름").put("custom", "keep"))
        val before = data.json.toString()
        val history = data.history()
        assertEquals("바꾼 이름", history.first().groups.single().name)
        assertEquals("배웠던 이름", history.last().groups.single().name)
        assertEquals("어깨 확인", history.first().groups.single().records.single().memo)
        assertEquals(before, data.json.toString())
        assertTrue(data.exercises().isEmpty())
    }

    @Test fun identicalLegacySetsCanBeDeletedIndividuallyAndStaleSelectionIsRejected() {
        val data = data(); val logs = data.json.getJSONArray("logs")
        logs.put(log(today)); logs.put(log(today)); logs.put(log(today))
        val records = data.history().single().groups.single().records
        assertEquals(3, records.map { it.key.value }.toSet().size)
        data.deleteHistoryRecord(records[1].key)
        assertEquals(2, logs.length())
        assertThrows(IllegalArgumentException::class.java) { data.deleteHistoryRecord(records[1].key) }
        assertEquals(2, logs.length())
    }

    @Test fun changedBackupOrAppendedLogCannotMakeOldSelectionDeleteAnotherEntry() {
        val data = data(); val logs = data.json.getJSONArray("logs")
        logs.put(log(today)); val original = data.history().single().groups.single().records.single().key
        logs.getJSONObject(0).put("memo", "백업의 다른 메모")
        assertThrows(IllegalArgumentException::class.java) { data.deleteHistoryRecord(original) }
        val updated = data.history().single().groups.single().records.single().key
        logs.put(log(today, id = "other"))
        assertThrows(IllegalArgumentException::class.java) { data.deleteHistoryRecord(updated) }
        assertEquals(2, logs.length())
        assertEquals("백업의 다른 메모", logs.getJSONObject(0).getString("memo"))
    }

    @Test fun deletingLastSetUpdatesTodayProgressAndWeeklyDayWithoutRemovingPlanOrExercise() {
        val data = data()
        val id = data.saveExercise(ExerciseDraft(name = "랫풀다운", equipment = "머신"))
        data.applyPlan(today, "헬스장", setOf(id))
        val plan = data.plans(today).single()
        data.record(plan, "20", "12", "", 0, time(today))
        assertEquals(setOf(today), data.summary(time(today)).trainedDays)
        data.deleteHistoryRecord(data.history().single().groups.single().records.single().key)
        assertTrue(data.history().isEmpty()); assertEquals(0, data.sets(plan).size)
        assertTrue(data.summary(time(today)).trainedDays.isEmpty())
        assertEquals(1, data.exercises().size); assertEquals(1, data.plans(today).size)
    }

    @Test fun timeZoneBoundaryAndStructuredGroupKeysDoNotMergeSeparateRecords() {
        val data = data(); val logs = data.json.getJSONArray("logs")
        val midnight = today.atStartOfDay(zone).toInstant().toEpochMilli()
        logs.put(log(today).put("time", midnight - 1))
        logs.put(log(today, id = "a|b", place = "c").put("time", midnight))
        logs.put(log(today, id = "a", place = "b|c").put("time", midnight))
        val days = data.history()
        assertEquals(listOf(today, today.minusDays(1)), days.map { it.date })
        assertEquals(2, days.first().groups.map { it.key }.toSet().size)
    }
}
