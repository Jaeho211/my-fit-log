package com.jaeho.myfitlog

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class WorkoutFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Before fun seedVersionOneData() {
        rule.activityRule.scenario.onActivity { activity ->
            val json = JSONObject(FitData.EMPTY)
            json.getJSONArray("exercises").put(JSONObject().put("id", "pull").put("name", "랫풀다운")
                .put("equipment", "머신").put("kg", "20").put("reps", "12").put("sets", "3"))
            json.put("plan", JSONArray().put(JSONObject().put("date", LocalDate.now().toString())
                .put("id", "pull").put("name", "랫풀다운").put("place", "헬스장")
                .put("kg", "20").put("reps", "12").put("sets", "3")))
            activity.getSharedPreferences("fit", Context.MODE_PRIVATE).edit().putString("db", json.toString()).commit()
        }
        rule.activityRule.scenario.recreate()
        rule.waitForIdle()
    }

    @Test fun editedSetPersistsAndUndoRemovesIt() {
        rule.onNodeWithText("운동 시작").performClick()
        rule.onNode(hasSetTextAction() and hasText("무게 kg")).performTextReplacement("22.5")
        rule.onNode(hasSetTextAction() and hasText("횟수")).performTextReplacement("10")
        rule.onNodeWithText("1세트 완료").performClick()
        rule.onNodeWithText("1 / 3 완료").assertIsDisplayed()
        rule.activityRule.scenario.onActivity {
            val logs = FitRepository(it).read().getJSONArray("logs")
            assertEquals("22.5", logs.getJSONObject(0).getString("kg"))
            assertEquals("10", logs.getJSONObject(0).getString("reps"))
        }
        rule.onNodeWithText("실행 취소").performClick()
        rule.onNodeWithText("0 / 3 완료").assertIsDisplayed()
        rule.activityRule.scenario.onActivity { assertEquals(0, FitRepository(it).read().getJSONArray("logs").length()) }
    }

    @Test fun exerciseLibraryHistoryAndLegacySettingsReturnToHome() {
        rule.onNodeWithText("내 운동").performClick()
        rule.onNodeWithText("내가 배운 운동 1개 · 목표와 자세를 한곳에").assertIsDisplayed()
        rule.onNodeWithText("기록").performClick()
        rule.onNodeWithText("첫 기록을 기다리고 있어요").assertIsDisplayed()
        rule.onNodeWithContentDescription("설정 열기").performClick()
        onView(withText("재호 · 1986년생")).check(matches(isDisplayed()))
        onView(withContentDescription("기록 탭")).perform(click())
        rule.onNodeWithText("첫 기록을 기다리고 있어요").assertIsDisplayed()
        rule.onNodeWithText("오늘").performClick()
        rule.onNodeWithText("운동 시작").assertIsDisplayed()
    }

    @Test fun cancelingPlanChangesDoesNotReplaceTheRoutine() {
        rule.onNodeWithText("루틴 편집").performClick()
        rule.onNodeWithText("집", useUnmergedTree = true).performClick()
        rule.onNodeWithText("취소").performClick()
        rule.onNodeWithText("랫풀다운").assertIsDisplayed()
        rule.activityRule.scenario.onActivity {
            assertEquals("헬스장", FitRepository(it).read().getJSONArray("plan").getJSONObject(0).getString("place"))
        }
    }

    @Test fun clearingAllCheckedExercisesRemovesTodaysPlan() {
        rule.onNodeWithText("루틴 편집").performClick()
        rule.onNodeWithText("모두 해제").performClick()
        rule.onNodeWithText("오늘 루틴 비우기").assertIsDisplayed().performClick()
        rule.onNodeWithText("오늘 루틴을 모두 비울까요?").assertIsDisplayed()
        rule.onNodeWithText("모두 비우기").performClick()
        rule.activityRule.scenario.onActivity {
            val data = FitData(FitRepository(it).read())
            assertEquals(0, data.plans(LocalDate.now()).size)
            assertEquals(1, data.exercises().size)
        }
        rule.onNodeWithText("오늘 할 운동").assertDoesNotExist()
        rule.onNodeWithText("오늘 루틴 구성").assertIsDisplayed()
    }

    @Test fun cancellingClearAllKeepsTodaysPlan() {
        rule.onNodeWithText("루틴 편집").performClick()
        rule.onNodeWithText("모두 해제").performClick()
        rule.onNodeWithText("오늘 루틴 비우기").performClick()
        rule.onNodeWithText("취소").performClick()
        rule.activityRule.scenario.onActivity {
            assertEquals(1, FitData(FitRepository(it).read()).plans(LocalDate.now()).size)
        }
    }

    @Test fun ptRecordsAllSetsWithoutStartingAWorkout() {
        rule.onNodeWithText("PT 운동 기록하기").performClick()
        rule.onNodeWithText("기록할 운동을 선택하세요").performClick()
        rule.onNodeWithText("랫풀다운").performClick()
        rule.onNode(hasSetTextAction() and hasText("무게 kg")).performTextReplacement("24")
        rule.onNode(hasSetTextAction() and hasText("횟수")).performTextReplacement("15")
        rule.onNodeWithText("PT 기록 저장").performClick()
        rule.activityRule.scenario.onActivity {
            val data = FitData(FitRepository(it).read())
            val ptLogs = data.history().first { day -> day.date == LocalDate.now() }.groups
                .flatMap { group -> group.records }.filter { record -> record.place == "PT" }
            assertEquals(3, ptLogs.size)
            assertEquals("24", ptLogs.first().kg)
            assertEquals("15", ptLogs.first().reps)
            assertEquals("헬스장", data.plans(LocalDate.now()).single().key.place)
        }
    }

    @Test fun ptGroupCanBeDeletedWithConfirmation() {
        rule.onNodeWithText("PT 운동 기록하기").performClick()
        rule.onNodeWithText("기록할 운동을 선택하세요").performClick()
        rule.onNodeWithText("랫풀다운").performClick()
        rule.onNodeWithText("PT 기록 저장").performClick()
        rule.onNodeWithText("전체 삭제").performClick()
        rule.onNodeWithText("기록 전체 삭제").performClick()
        rule.activityRule.scenario.onActivity {
            val data = FitData(FitRepository(it).read())
            assertEquals(0, data.json.getJSONArray("logs").length())
            assertEquals("헬스장", data.plans(LocalDate.now()).single().key.place)
        }
    }

    @Test fun inputAndActiveWorkoutSurviveRecreation() {
        rule.onNodeWithText("운동 시작").performClick()
        rule.onNode(hasSetTextAction() and hasText("무게 kg")).performTextReplacement("18.5")
        rule.activityRule.scenario.recreate()
        rule.onNode(hasSetTextAction() and hasText("무게 kg")).assertTextContains("18.5")
        rule.onNodeWithText("1세트 완료").assertIsDisplayed()
    }
}
