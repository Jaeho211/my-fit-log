package com.jaeho.myfitlog

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class WorkoutConsultationTest {
    @Test fun exportsSelectedHistoryWithMissingValuesZeroWeightAndMultilineMemos() {
        val data = FitData(JSONObject(FitData.EMPTY), ZoneId.of("Asia/Seoul"))
        fun log(date: String, kg: String, reps: String, memo: String) = JSONObject()
            .put("id", "deleted-exercise").put("name", "덤벨 킥백").put("place", "PT")
            .put("time", LocalDate.parse(date).atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli())
            .put("kg", kg).put("reps", reps).put("memo", memo)
        data.json.getJSONArray("logs")
            .put(log("2026-11-02", "", "", "왼팔\n어깨 확인"))
            .put(log("2026-11-02", "0", "12", ""))
            .put(log("2026-10-20", "3", "25", "이전 기록"))
        val before = data.json.toString()
        val text = workoutConsultationText(data.history().take(1))
        assertTrue(text.contains("운동한 날: 1일 / 총 2세트"))
        assertTrue(text.contains("덤벨 킥백 / PT / 2세트"))
        assertTrue(text.contains("1세트: 무게 미기록 × 횟수 미기록"))
        assertTrue(text.contains("2세트: 0 kg × 12 회"))
        assertTrue(text.contains("    왼팔\n    어깨 확인"))
        assertFalse(text.contains("이전 기록"))
        val all = workoutConsultationText(data.history())
        assertTrue(all.indexOf("## 2026-10-20") < all.indexOf("## 2026-11-02"))
        assertEquals(before, data.json.toString())
    }

    @Test fun emptyHistoryDoesNotInventDatesOrCounts() {
        val text = workoutConsultationText(emptyList())
        assertTrue(text.contains("운동 기록이 없습니다."))
        assertFalse(text.contains("기록 범위:"))
    }
}
