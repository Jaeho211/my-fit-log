package com.jaeho.myfitlog

import android.app.Notification
import android.content.res.Configuration
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class AppointmentFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private fun shell(command: String) {
        ParcelFileDescriptor.AutoCloseInputStream(InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)).use { it.readBytes() }
    }
    @Before fun seedReservations() {
        if (Build.VERSION.SDK_INT >= 33) shell("pm grant com.jaeho.myfitlog android.permission.POST_NOTIFICATIONS")
        rule.activityRule.scenario.onActivity { activity ->
            val d = FitData(JSONObject(FitData.EMPTY))
            fun add(id: Int, days: Long, note: String) {
                val time = LocalDate.now().plusDays(days).atTime(19, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                d.json.getJSONArray("appointments").put(JSONObject().put("id", id).put("time", time).put("minutes", 60).put("note", note))
            }
            add(70, 1, "첫 예약 메모"); add(71, 3, "다음 예약 메모"); add(72, -1, "지난 예약 메모")
            FitRepository(activity).write(d.json)
        }
        rule.activityRule.scenario.recreate()
        rule.onNodeWithText("PT").performClick()
    }
    @After fun restoreNotifications() {
        InstrumentationRegistry.getInstrumentation().targetContext.getSystemService(NotificationManager::class.java).cancelAll()
    }
    private fun open(id: Int) {
        rule.onNodeWithTag("appointment_list").performScrollToNode(hasTestTag("appointment_$id"))
        rule.onNodeWithTag("appointment_$id").performClick()
    }
    private fun data(check: (FitData) -> Unit) {
        rule.activityRule.scenario.onActivity { check(FitData(FitRepository(it).read())) }
    }

    @Test fun compactDateAndTimeInputsValidateAndApplyNumericValues() {
        val year = LocalDate.now().year + 1
        var result: AppointmentDraft? = null
        rule.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                val config = Configuration(LocalConfiguration.current).apply { screenWidthDp = 320; screenHeightDp = 640 }
                val density = LocalDensity.current.density
                CompositionLocalProvider(LocalConfiguration provides config, LocalDensity provides Density(density, 1.5f)) {
                    var value by remember { mutableStateOf(AppointmentDraft()) }
                    FitTheme {
                        AppointmentEditor(value, emptyMap(), System.currentTimeMillis(), NotificationAccess(true) {},
                            Modifier.width(320.dp)) { value = it; result = it }
                    }
                }
            }
        }
        rule.onNodeWithTag("appointment_date").performScrollTo().performClick()
        rule.onNodeWithTag("appointment_year").performTextReplacement(year.toString())
        rule.onNodeWithTag("appointment_month").performTextReplacement("13")
        rule.onNodeWithText("날짜 적용").assertIsNotEnabled()
        rule.onNodeWithTag("appointment_month").performTextReplacement("11")
        rule.onNodeWithTag("appointment_day").performTextReplacement("26")
        rule.onNodeWithText("날짜 적용").performClick()
        rule.runOnIdle { assertEquals("$year-11-26", result!!.date) }
        rule.onNodeWithTag("appointment_time").performScrollTo().performClick()
        rule.onNodeWithTag("appointment_hour").performTextReplacement("21")
        rule.onNodeWithTag("appointment_minute").performTextReplacement("15")
        rule.onNodeWithText("시간 적용").performClick()
        rule.runOnIdle { assertEquals(21, result!!.hour); assertEquals(15, result!!.minute) }
    }

    @Test fun registrationUsesChosenTimeAndCanceledDatePickerDoesNotChangeDraft() {
        rule.onNodeWithTag("add_appointment").performClick()
        rule.onNodeWithTag("appointment_date").performClick()
        rule.onNodeWithText("취소").performClick()
        rule.onNodeWithTag("appointment_time").performClick()
        rule.onNodeWithTag("appointment_hour").performTextReplacement("20")
        rule.onNodeWithTag("appointment_minute").performTextReplacement("30")
        rule.onNodeWithText("시간 적용").performClick()
        rule.onNodeWithTag("appointment_lead").performScrollTo().performTextReplacement("15")
        rule.onNodeWithTag("appointment_note").performScrollTo().performTextReplacement("새 예약 메모")
        rule.onNodeWithText("예약 저장").performClick()
        rule.onNodeWithText("새 예약 메모").assertIsDisplayed()
        data {
            val new = it.appointments().single { a -> a.note == "새 예약 메모" }
            assertEquals(4, it.appointments().size); assertEquals(15, new.minutes)
            val local = new.draft()
            assertEquals(LocalDate.now().plusDays(1).toString(), local.date); assertEquals(20, local.hour); assertEquals(30, local.minute)
            assertTrue(new.id !in setOf(70, 71, 72))
        }
    }

    @Test fun editingSurvivesRecreationAndCanBeDiscardedOrSavedWithSameId() {
        open(70); rule.onNodeWithText("예약 수정").performClick()
        rule.onNodeWithTag("appointment_note").performScrollTo().performTextReplacement("작성 중인 메모")
        rule.activityRule.scenario.recreate()
        rule.onNodeWithTag("appointment_note").performScrollTo().assertTextContains("작성 중인 메모")
        rule.onNodeWithText("PT 예약 수정").performClick()
        rule.onNodeWithText("변경 취소").performClick()
        rule.onNodeWithText("첫 예약 메모").assertIsDisplayed()
        rule.onNodeWithText("예약 수정").performClick()
        rule.onNodeWithTag("appointment_lead").performScrollTo().performTextReplacement("30")
        rule.onNodeWithText("예약 저장").performClick()
        data { assertEquals(30, it.appointments().single { a -> a.id == 70 }.minutes); assertEquals(3, it.appointments().size) }
    }

    @Test fun deleteCanBeCanceledAndConfirmedDeletionRemovesItsAlarm() {
        data { assertNotNull(ReminderScheduler.existingAlarm(rule.activity, 70)) }
        open(70); rule.onNodeWithText("삭제").performClick(); rule.onNodeWithText("취소").performClick()
        data { assertEquals(3, it.appointments().size) }
        rule.onNodeWithText("삭제").performClick(); rule.onNodeWithText("예약 삭제").performClick()
        rule.onNodeWithTag("appointment_71").assertIsDisplayed()
        data {
            assertEquals(2, it.appointments().size); assertNull(ReminderScheduler.existingAlarm(rule.activity, 70))
            assertNotNull(ReminderScheduler.existingAlarm(rule.activity, 71))
        }
    }

    @Test fun pastAppointmentsStayCollapsedAndRebookingPreservesTheOriginal() {
        rule.onNodeWithTag("appointment_72").assertDoesNotExist()
        rule.onNodeWithTag("appointment_list").performScrollToNode(hasText("지난 일정 보기 (1)"))
        rule.onNodeWithText("지난 일정 보기 (1)").performClick()
        open(72); rule.onNodeWithText("다시 예약하기").performClick()
        rule.onNodeWithText("예약 저장").performClick()
        data {
            assertEquals(4, it.appointments().size)
            assertTrue(it.appointments().single { a -> a.id == 72 }.time < System.currentTimeMillis())
            assertEquals(2, it.appointments().count { a -> a.note == "지난 예약 메모" })
        }
    }

    @Test fun invalidLeadIsRejectedAndAValidReservationCanBeSaved() {
        rule.onNodeWithTag("add_appointment").performClick()
        rule.onNodeWithTag("appointment_lead").performScrollTo().performTextReplacement("0")
        rule.onNodeWithText("예약 저장").performClick()
        data { assertEquals(3, it.appointments().size) }
        rule.onNodeWithTag("appointment_lead").performScrollTo().performTextReplacement("10")
        rule.onNodeWithText("예약 저장").performClick()
        data { assertEquals(4, it.appointments().size) }
    }

    @Test fun dueNotificationPostsAndItsPendingIntentOpensThePtTab() {
        rule.activityRule.scenario.onActivity {
            val d = FitData(FitRepository(it).read())
            d.json.getJSONArray("appointments").put(JSONObject().put("id", 73).put("time", System.currentTimeMillis() + 600000)
                .put("minutes", 11).put("note", "알림 내용 확인"))
            FitRepository(it).write(d.json)
        }
        rule.onNodeWithText("오늘").performClick()
        rule.activityRule.scenario.onActivity {
            ReminderReceiver().onReceive(it, Intent(it, ReminderReceiver::class.java).putExtra("id", 73))
        }
        rule.waitUntil(5_000) {
            InstrumentationRegistry.getInstrumentation().targetContext.getSystemService(NotificationManager::class.java)
                .activeNotifications.any { it.id == 73 }
        }
        rule.activityRule.scenario.onActivity {
            val notification = it.getSystemService(NotificationManager::class.java).activeNotifications.single { n -> n.id == 73 }.notification
            assertEquals("PT 예약 알림", notification.extras.getString(Notification.EXTRA_TITLE))
            assertTrue(notification.extras.getString(Notification.EXTRA_TEXT)!!.contains("알림 내용 확인"))
            notification.contentIntent.send()
        }
        rule.onNodeWithText("PT 일정").assertIsDisplayed()
        rule.onNodeWithTag("appointment_73").assertIsDisplayed()
    }
}
