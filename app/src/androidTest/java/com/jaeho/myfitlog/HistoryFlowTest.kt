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
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class HistoryFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private lateinit var firstKey: String
    private lateinit var secondKey: String
    private val groupTag get() = "history_group_${historyGroupKey(LocalDate.now(), "pull", "헬스장")}"

    @Before fun seedLegacyHistory() {
        rule.activityRule.scenario.onActivity { activity ->
            val db = JSONObject(FitData.EMPTY)
            db.getJSONArray("exercises").put(ExerciseDraft(id = "pull", name = "랫풀다운", equipment = "머신", kg = "20", reps = "12").toJson())
            val data = FitData(db)
            data.applyPlan(LocalDate.now(), "헬스장", setOf("pull"))
            val now = System.currentTimeMillis() - 60_000
            fun log(id: String, name: String, place: String, time: Long) = JSONObject()
                .put("id", id).put("name", name).put("place", place).put("time", time)
                .put("kg", "20").put("reps", "12").put("memo", "첫줄\n\n자세 메모")
            val logs = db.getJSONArray("logs")
            logs.put(log("pull", "랫풀다운", "헬스장", now))
            logs.put(log("pull", "랫풀다운", "헬스장", now))
            logs.put(log("pull", "랫풀다운 PT", "PT", now - 60_000))
            val old = LocalDate.now().minusDays(20).atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            logs.put(log("curl", "덤벨 컬", "집", old))
            val records = data.history().first().groups.first { it.place == "헬스장" }.records
            firstKey = records[0].key.value; secondKey = records[1].key.value
            FitRepository(activity).write(db)
        }
        rule.activityRule.scenario.recreate()
        rule.onNodeWithText("기록").performClick()
    }

    @Test fun datedSummaryExpandsLegacySetsWithoutChangingStorage() {
        rule.onNodeWithText("총 2일 · 4세트의 꾸준함").assertIsDisplayed()
        rule.onNodeWithTag("history_set_$firstKey").assertDoesNotExist()
        rule.onNodeWithTag(groupTag).performClick()
        rule.onNodeWithTag("history_set_$firstKey").assertIsDisplayed()
        rule.onNodeWithTag("history_set_$secondKey").assertIsDisplayed()
        rule.activityRule.scenario.onActivity {
            val logs = FitRepository(it).read().getJSONArray("logs")
            assertEquals(4, logs.length()); assertFalse(logs.getJSONObject(0).has("entryId"))
        }
    }

    @Test fun detailAndExpansionSurviveRecreationAndDeleteCanBeCanceled() {
        rule.onNodeWithTag(groupTag).performClick()
        rule.onNodeWithTag("history_set_$firstKey").performClick()
        rule.activityRule.scenario.recreate()
        rule.onNodeWithText("랫풀다운 · 1세트").assertIsDisplayed()
        rule.onNode(hasText("첫줄\n\n자세 메모") and hasAnyAncestor(isDialog())).assertIsDisplayed()
        rule.onNodeWithText("세트 삭제").performClick()
        rule.onNodeWithText("취소").performClick()
        rule.onNodeWithText("닫기").performClick()
        rule.onNodeWithTag("history_set_$firstKey").assertIsDisplayed()
        rule.activityRule.scenario.onActivity { assertEquals(4, FitRepository(it).read().getJSONArray("logs").length()) }
    }

    @Test fun deletingOneIdenticalLegacySetUpdatesTheTodayRoutine() {
        rule.onNodeWithTag(groupTag).performClick()
        rule.onNodeWithTag("history_set_$secondKey").performClick()
        rule.onNodeWithText("세트 삭제").performClick()
        rule.onNodeWithText("기록 삭제").performClick()
        rule.onNodeWithText("세트 기록을 삭제했어요").assertIsDisplayed()
        rule.activityRule.scenario.onActivity {
            val d = FitData(FitRepository(it).read())
            assertEquals(3, d.json.getJSONArray("logs").length())
            assertEquals(1, d.sets(d.plans(LocalDate.now()).single()).size)
            assertEquals(1, d.exercises().size)
        }
        rule.onNodeWithText("오늘").performClick()
        rule.onNodeWithText("1 / 3").assertIsDisplayed()
    }

    @Test fun weekFilterSurvivesRecreationAndAllHistoryRemainsAvailable() {
        rule.onNodeWithText("이번 주").performClick()
        rule.activityRule.scenario.recreate()
        rule.onNodeWithText("이번 주").assertIsSelected()
        rule.onNodeWithTag(groupTag).assertIsDisplayed()
        rule.onNodeWithText("전체").performClick()
        rule.onNodeWithTag("history_list").performScrollToNode(hasText("덤벨 컬"))
        rule.onNodeWithText("덤벨 컬").assertIsDisplayed()
    }

    @Test fun emptyHistoryOffersAnActionToStartTodaysWorkout() {
        rule.activityRule.scenario.onActivity {
            val db = FitRepository(it).read().put("logs", org.json.JSONArray())
            FitRepository(it).write(db)
        }
        rule.activityRule.scenario.recreate()
        rule.onNodeWithText("첫 기록을 기다리고 있어요").assertIsDisplayed()
        rule.onNodeWithText("오늘 운동하러 가기").performClick()
        rule.onNodeWithText("운동 시작").assertIsDisplayed()
    }
}
