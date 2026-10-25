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

    @Test fun legacyExerciseManagementReturnsToComposeHome() {
        rule.onNodeWithText("내 운동").performClick()
        onView(withText("내가 배운 운동 1개")).check(matches(isDisplayed()))
        onView(withContentDescription("오늘 탭")).perform(click())
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

    @Test fun inputAndActiveWorkoutSurviveRecreation() {
        rule.onNodeWithText("운동 시작").performClick()
        rule.onNode(hasSetTextAction() and hasText("무게 kg")).performTextReplacement("18.5")
        rule.activityRule.scenario.recreate()
        rule.onNode(hasSetTextAction() and hasText("무게 kg")).assertTextContains("18.5")
        rule.onNodeWithText("1세트 완료").assertIsDisplayed()
    }
}
