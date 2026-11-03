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
        assertTrue(text.contains("무게 미기록 × 횟수 미기록 × 1세트"))
        assertTrue(text.contains("0 kg × 12회 × 1세트"))
        assertTrue(text.contains("    왼팔\n    어깨 확인"))
        assertFalse(text.contains("이전 기록"))
        val all = workoutConsultationText(data.history())
        assertTrue(all.indexOf("## 2026-10-20") < all.indexOf("## 2026-11-02"))
        assertEquals(before, data.json.toString())
    }

    @Test fun mergesRepeatedSetsAndMemosButPreservesWeightChangeOrder() {
        val date = LocalDate.of(2026, 11, 4)
        val records = listOf("3", "3", "4", "3").mapIndexed { i, kg ->
            HistoryRecord(HistoryKey(i, "", ""), "curl", "덤벨 컬", "집",
                i.toLong(), kg, "25", "자세 확인")
        }
        val text = workoutConsultationText(listOf(HistoryDay(date,
            listOf(HistoryGroup(date, "curl", "집", records)))))
        assertTrue(text.contains("3 kg × 25회 × 2세트"))
        assertTrue(text.contains("4 kg × 25회 × 1세트"))
        assertTrue(text.indexOf("4 kg") < text.lastIndexOf("3 kg × 25회 × 1세트"))
        assertEquals(1, Regex("자세 확인").findAll(text).count())
        assertFalse(text.contains("1세트:"))
        assertTrue(text.contains("총 4세트"))
    }

    @Test fun emptyHistoryDoesNotInventDatesOrCounts() {
        val text = workoutConsultationText(emptyList())
        assertTrue(text.contains("운동 기록이 없습니다."))
        assertFalse(text.contains("기록 범위:"))
    }
}
