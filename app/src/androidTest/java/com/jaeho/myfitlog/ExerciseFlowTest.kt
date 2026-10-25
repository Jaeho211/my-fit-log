package com.jaeho.myfitlog

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class ExerciseFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Before fun seedLibrary() {
        rule.activityRule.scenario.onActivity { activity ->
            val data = FitData(JSONObject(FitData.EMPTY))
            val id = data.saveExercise(ExerciseDraft(name = "랫풀다운", equipment = "머신", kg = "20", reps = "12"))
            data.applyPlan(LocalDate.now(), "헬스장", setOf(id))
            val plan = data.plans(LocalDate.now()).single()
            data.record(plan, "20", "12", "기존 기록", 0, System.currentTimeMillis())
            FitRepository(activity).write(data.json)
        }
        rule.activityRule.scenario.recreate()
        rule.onNodeWithText("내 운동").performClick()
    }

    private fun field(tag: String, value: String) {
        rule.onNodeWithTag("exercise_editor_list").performScrollToNode(hasTestTag(tag))
        rule.onNodeWithTag(tag).performTextReplacement(value)
    }
    private fun data(check: (FitData) -> Unit) {
        rule.activityRule.scenario.onActivity { check(FitData(FitRepository(it).read())) }
    }

    @Test fun catalogRegistrationFeedsTheHomeRoutineWithSavedTargets() {
        rule.onNodeWithTag("add_exercise").performClick()
        rule.onNodeWithText("덤벨 컬").performClick()
        field("gym_kg", "7.5")
        field("gym_reps", "10")
        rule.onNodeWithTag("exercise_editor_list").performScrollToNode(hasTestTag("exercise_home"))
        rule.onNodeWithTag("exercise_home").performClick()
        field("home_reps", "18")
        field("exercise_note", "팔꿈치를 몸 옆에 고정")
        rule.onNodeWithText("운동 저장").performClick()
        rule.onNodeWithText("덤벨 컬").assertIsDisplayed()
        data { d ->
            val saved = d.exercises().single { it.name == "덤벨 컬" }
            assertEquals("7.5", saved.kg); assertTrue(saved.home); assertEquals("3", saved.effectiveHomeKg)
            assertEquals("18", saved.homeReps); assertEquals("팔꿈치를 몸 옆에 고정", saved.note)
        }
        rule.onNodeWithText("내 운동").performClick()
        rule.onNodeWithText("오늘").performClick()
        rule.onNodeWithText("루틴 편집").performClick()
        rule.onNodeWithText("집", useUnmergedTree = true).performClick()
        rule.onNodeWithText("루틴 적용").performClick()
        data {
            val plan = it.plans(LocalDate.now()).single()
            assertEquals("덤벨 컬", plan.name); assertEquals("3", plan.kg); assertEquals("18", plan.reps)
        }
    }

    @Test fun editingSurvivesRecreationAndDiscardPreservesSavedData() {
        rule.onNodeWithText("랫풀다운").performClick()
        rule.onNodeWithText("목표·메모 수정").performClick()
        field("exercise_name", "랫풀다운 수정 중")
        rule.activityRule.scenario.recreate()
        rule.onNodeWithTag("exercise_editor_list").performScrollToNode(hasTestTag("exercise_name"))
        rule.onNodeWithTag("exercise_name").assertTextContains("랫풀다운 수정 중")
        rule.onNodeWithText("운동 수정").performClick()
        rule.onNodeWithText("변경 취소").performClick()
        rule.onNodeWithText("랫풀다운").assertIsDisplayed()
        data { assertEquals("랫풀다운", it.exercises().single().name) }
    }

    @Test fun searchAndHomeFilterPersistWhenReturningFromDetails() {
        rule.activityRule.scenario.onActivity {
            val d = FitData(FitRepository(it).read())
            d.saveExercise(ExerciseDraft(name = "덤벨 컬", equipment = "덤벨", home = true))
            FitRepository(it).write(d.json)
        }
        rule.activityRule.scenario.recreate()
        rule.onNodeWithTag("exercise_search").performTextReplacement("덤 벨")
        rule.onNodeWithText("랫풀다운").assertDoesNotExist()
        rule.onNodeWithText("덤벨 컬").performClick()
        rule.onNodeWithText("내 운동").performClick()
        rule.onNodeWithTag("exercise_search").assertTextContains("덤 벨")
        rule.onNodeWithTag("exercise_search").performTextClearance()
        rule.onNodeWithText("집에서도 가능").performClick()
        rule.onNodeWithText("랫풀다운").assertDoesNotExist()
        rule.onNodeWithText("덤벨 컬").assertIsDisplayed()
    }

    @Test fun invalidAndDuplicateRegistrationDoNotCreateAnExercise() {
        rule.onNodeWithTag("add_exercise").performClick()
        rule.onNodeWithText("직접 운동 등록").performClick()
        rule.onNodeWithText("운동 저장").performClick()
        rule.onNodeWithText("운동 이름을 입력해 주세요").assertIsDisplayed()
        field("exercise_name", "랫풀다운")
        rule.onNodeWithText("운동 저장").performClick()
        rule.onNodeWithText("이미 등록한 운동 이름입니다. 다른 이름으로 구분해 주세요").assertIsDisplayed()
        data { assertEquals(1, it.exercises().size) }
    }

    @Test fun deletingExerciseRemovesItsPlanButPreservesPerformedSets() {
        rule.onNodeWithText("랫풀다운").performClick()
        rule.onNodeWithText("삭제").performClick()
        rule.onNodeWithText("취소").performClick()
        data { assertEquals(1, it.exercises().size) }
        rule.onNodeWithText("삭제").performClick()
        rule.onNodeWithText("운동 삭제").performClick()
        rule.onNodeWithText("배운 운동을 모아보세요").assertIsDisplayed()
        data {
            assertTrue(it.exercises().isEmpty()); assertTrue(it.plans(LocalDate.now()).isEmpty())
            assertEquals(1, it.json.getJSONArray("logs").length())
            assertEquals("기존 기록", it.json.getJSONArray("logs").getJSONObject(0).getString("memo"))
        }
    }
}
