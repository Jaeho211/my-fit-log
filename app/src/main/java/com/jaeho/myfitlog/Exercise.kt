package com.jaeho.myfitlog

import org.json.JSONObject
import java.util.Locale

/** Editable values keep the version-1 backup keys and string-valued targets. */
data class ExerciseDraft(
    val id: String = "", val name: String = "", val equipment: String = "덤벨",
    val note: String = "", val kg: String = "3", val reps: String = "25", val sets: String = "3",
    val home: Boolean = false, val homeKg: String = "0", val homeReps: String = "25", val homeSets: String = "3",
) {
    val dumbbell: Boolean get() = equipment.contains("덤벨")
    val effectiveHomeKg: String get() = if (dumbbell) "3" else homeKg

    fun errors(): Map<String, String> = buildMap {
        if (name.isBlank()) put("name", "운동 이름을 입력해 주세요")
        if (equipment.isBlank()) put("equipment", "장비 또는 맨몸 여부를 입력해 주세요")
        if (!FitData.validWeight(kg)) put("kg", "0~1,000 kg")
        if (!FitData.validReps(reps)) put("reps", "1~1,000의 정수")
        if (!FitData.validReps(sets)) put("sets", "1~1,000의 정수")
        if (home) {
            if (!FitData.validWeight(effectiveHomeKg)) put("homeKg", "0~1,000 kg")
            if (!FitData.validReps(homeReps)) put("homeReps", "1~1,000의 정수")
            if (!FitData.validReps(homeSets)) put("homeSets", "1~1,000의 정수")
        }
    }

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id); put("name", name); put("equipment", equipment); put("note", note)
        put("kg", kg); put("reps", reps); put("sets", sets); put("home", home)
        put("homeKg", homeKg); put("homeReps", homeReps); put("homeSets", homeSets)
    }
    companion object {
        fun fromJson(e: JSONObject) = ExerciseDraft(
            e.optString("id"), e.optString("name"), e.optString("equipment", "덤벨"), e.optString("note"),
            e.optString("kg", "3"), e.optString("reps", "25"), e.optString("sets", "3"),
            e.optBoolean("home"), e.optString("homeKg", "0"), e.optString("homeReps", "25"), e.optString("homeSets", "3"),
        )
        fun normalizedName(value: String): String = value.trim().replace(Regex("\\s+"), " ").lowercase(Locale.ROOT)
        fun searchKey(value: String): String = value.lowercase(Locale.ROOT).replace(Regex("\\s+"), "")
    }
}

data class CatalogExercise(val name: String, val category: String, val equipment: String) {
    fun draft() = ExerciseDraft(name = name, equipment = equipment, kg = if (equipment == "덤벨") "3" else "0")
}

/** Suggestions only; selecting one opens a form and never enrolls it until Save. */
val ExerciseCatalog = listOf(
    CatalogExercise("덤벨 컬", "팔", "덤벨"), CatalogExercise("해머 컬", "팔", "덤벨"),
    CatalogExercise("덤벨 킥백", "팔", "덤벨"), CatalogExercise("케이블 푸시다운", "팔", "케이블"),
    CatalogExercise("덤벨 숄더 프레스", "어깨", "덤벨"), CatalogExercise("아놀드 프레스", "어깨", "덤벨"),
    CatalogExercise("사이드 레터럴 레이즈", "어깨", "덤벨"), CatalogExercise("리버스 펙덱 플라이", "어깨", "머신"),
    CatalogExercise("랫풀다운", "등", "머신"), CatalogExercise("시티드 케이블 로우", "등", "케이블"),
    CatalogExercise("원암 덤벨 로우", "등", "덤벨"), CatalogExercise("체스트 서포티드 로우", "등", "머신"),
    CatalogExercise("페이스 풀", "등", "케이블"), CatalogExercise("체스트 프레스", "가슴", "머신"),
    CatalogExercise("펙덱 플라이", "가슴", "머신"), CatalogExercise("덤벨 벤치 프레스", "가슴", "덤벨 + 벤치"),
    CatalogExercise("덤벨 플로어 프레스", "가슴", "덤벨"), CatalogExercise("푸시업", "가슴", "맨몸"),
    CatalogExercise("풀업", "철봉", "철봉"), CatalogExercise("친업", "철봉", "철봉"),
    CatalogExercise("스캐풀라 풀업", "철봉", "철봉"),
)
