package com.jaeho.myfitlog

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class FitDataTest {
    private val zone = ZoneId.of("Asia/Seoul")
    private val today = LocalDate.of(2026, 10, 25)
    private fun time(date: LocalDate, hour: Int = 12) = date.atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()
    private fun fixture(): FitData = FitData(JSONObject("""{
      "version":1,"weekly":3,"custom":"preserve me",
      "exercises":[
        {"id":"pull","name":"랫풀다운","equipment":"머신","kg":"20","reps":"12","sets":"3","home":false},
        {"id":"curl","name":"덤벨 컬","equipment":"덤벨","kg":"8","reps":"12","sets":"3","home":true,"homeReps":"25","homeSets":"4"}],
      "logs":[],"appointments":[{"id":1,"time":${time(today.plusDays(1))},"note":"PT","minutes":60}],
      "plan":[{"date":"2026-10-25","id":"pull","name":"랫풀다운","place":"헬스장","kg":"20","reps":"12","sets":"3","note":"어깨 내리기"},
              {"date":"2026-10-26","id":"curl","name":"덤벨 컬","place":"집","kg":"3","reps":"25","sets":"4"}]
    }"""), zone)

    @Test fun versionOneAndUnknownFieldsSurviveRecording() {
        val data = fixture(); val p = data.plans(today).single()
        val token = data.record(p, "21.5", "10", "괜찮음", 0, time(today))
        val restored = FitData(JSONObject(data.json.toString()), zone)
        assertEquals("preserve me", restored.json.getString("custom"))
        assertEquals(1, restored.json.getInt("version"))
        assertEquals(1, restored.json.getJSONArray("appointments").length())
        assertEquals("21.5", restored.sets(p).single().kg)
        assertEquals(token, restored.sets(p).single().token)
    }

    @Test fun duplicateAndMidnightRecordsAreRejectedWithoutMutation() {
        val data = fixture(); val p = data.plans(today).single()
        data.record(p, "20", "12", "", 0, time(today))
        assertThrows(IllegalArgumentException::class.java) { data.record(p, "20", "12", "", 0, time(today)) }
        assertThrows(IllegalArgumentException::class.java) { data.record(p, "20", "12", "", 1, time(today.plusDays(1))) }
        assertEquals(1, data.sets(p).size)
    }

    @Test fun undoRemovesOnlyTheChosenSetEvenAtTheSameTimestamp() {
        val data = fixture(); val p = data.plans(today).single()
        val first = data.record(p, "20", "12", "", 0, time(today))
        val second = data.record(p, "18", "10", "", 1, time(today))
        assertTrue(data.undo(second))
        assertFalse(data.undo(second))
        assertEquals(first, data.sets(p).single().token)
    }

    @Test fun previousRecordAndProgressAreScopedToDateAndPlace() {
        val data = fixture(); val p = data.plans(today).single()
        val logs = data.json.getJSONArray("logs")
        fun log(day: LocalDate, place: String, kg: String) = JSONObject().apply {
            put("time", time(day)); put("id", "pull"); put("name", "랫풀다운")
            put("place", place); put("kg", kg); put("reps", "12")
        }
        logs.put(log(today.minusDays(3), "헬스장", "20")) // original records have no entryId
        logs.put(log(today.minusDays(1), "PT", "30"))
        logs.put(log(today, "헬스장", "22"))
        assertEquals("20", data.previous(p)!!.kg)
        assertEquals("22", data.sets(p).single().kg)
        assertEquals("", data.sets(p).single().token)
    }

    @Test fun homePlanUsesHomeTargetsAndPreservesFuturePlansAndLogs() {
        val data = fixture(); val p = data.plans(today).single()
        data.record(p, "20", "12", "", 0, time(today))
        data.applyPlan(today, "집", setOf("curl"))
        val home = data.plans(today).single()
        assertEquals("3", home.kg); assertEquals("25", home.reps); assertEquals(4, home.sets)
        assertEquals(1, data.plans(today.plusDays(1)).size)
        assertEquals(1, data.json.getJSONArray("logs").length())
        data.removePlan(home.key)
        assertTrue(data.plans(today).isEmpty())
        assertEquals(1, data.json.getJSONArray("logs").length())
        assertThrows(IllegalArgumentException::class.java) { data.applyPlan(today, "집", setOf("pull")) }
    }

    @Test fun weeklyCountUsesDistinctDaysAndMondayBoundary() {
        val data = fixture(); val logs = data.json.getJSONArray("logs")
        listOf(today.minusDays(7), today.minusDays(6), today.minusDays(6), today, today.plusDays(1)).forEach {
            logs.put(JSONObject().put("time", time(it)))
        }
        val summary = data.summary(time(today))
        assertEquals(today.minusDays(6), summary.week.first())
        assertEquals(setOf(today.minusDays(6), today), summary.trainedDays)
        assertEquals(time(today.plusDays(1)), summary.nextAppointment!!.time)
        assertEquals(setOf(today.plusDays(1)), data.summary(time(today.plusDays(1))).trainedDays)
    }

    @Test fun ptCanBeRecordedAsMultipleSetsWithoutAWorkoutPlan() {
        val data = fixture()
        val plansBefore = data.json.getJSONArray("plan").toString()
        data.recordPt("curl", "4", "25", "3", "트레이너 지도", time(today))
        data.recordPt("pull", "", "", "3", "무게와 횟수 미확인", time(today))
        val records = data.history().single { it.date == today }.groups.filter { it.place == "PT" }
        assertEquals(2, records.size)
        assertEquals(6, records.sumOf { it.records.size })
        assertEquals(3, records.single { it.name == "덤벨 컬" }.records.size)
        val unknown = records.single { it.name == "랫풀다운" }.records
        assertTrue(unknown.all { it.kg.isBlank() && it.reps.isBlank() })
        assertEquals(plansBefore, data.json.getJSONArray("plan").toString())
        assertEquals(setOf(today), data.summary(time(today)).trainedDays)
        assertEquals(1, data.json.getInt("version"))
    }

    @Test fun ptRemovalPreservesOtherExercise() {
        val data = fixture()
        data.recordPt("curl", "4", "25", "3", "", time(today))
        data.recordPt("pull", "15", "12", "2", "", time(today))
        val chosen = data.history().single { it.date == today }.groups.single { it.exerciseId == "curl" }
        data.deletePtHistoryGroup(chosen)
        assertEquals(2, data.json.getJSONArray("logs").length())
        assertEquals("pull", data.history().single { it.date == today }.groups.single().exerciseId)
    }

    @Test fun invalidPtRecordDoesNotWritePartialSets() {
        val data = fixture()
        listOf("0", "101", "-1", "abc").forEach { count ->
            assertThrows(IllegalArgumentException::class.java) {
                data.recordPt("curl", "4", "25", count, "", time(today))
            }
        }
        assertThrows(IllegalArgumentException::class.java) { data.recordPt("missing", "4", "25", "3", "", time(today)) }
        assertThrows(IllegalArgumentException::class.java) { data.recordPt("curl", "NaN", "25", "3", "", time(today)) }
        assertThrows(IllegalArgumentException::class.java) { data.recordPt("curl", "4", "1.5", "3", "", time(today)) }
        assertThrows(IllegalArgumentException::class.java) { data.applyPlan(today, "PT", setOf("curl")) }
        assertEquals(0, data.json.getJSONArray("logs").length())
    }

    @Test fun invalidInputNeverCreatesARecord() {
        val data = fixture(); val p = data.plans(today).single()
        listOf("NaN", "Infinity", "-1", "1001", "").forEach {
            assertThrows(IllegalArgumentException::class.java) { data.record(p, it, "12", "", 0, time(today)) }
        }
        listOf("0", "1.5", "1001", "").forEach {
            assertThrows(IllegalArgumentException::class.java) { data.record(p, "20", it, "", 0, time(today)) }
        }
        assertTrue(data.sets(p).isEmpty())
        assertTrue(FitData.validWeight("0")); assertTrue(FitData.validReps("1000"))
    }
}
