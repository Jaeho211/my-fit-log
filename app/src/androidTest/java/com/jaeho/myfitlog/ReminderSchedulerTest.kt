package com.jaeho.myfitlog

import android.app.Notification
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReminderSchedulerTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val repository get() = FitRepository(context)
    private val notifications get() = context.getSystemService(NotificationManager::class.java)
    private fun shell(command: String) {
        ParcelFileDescriptor.AutoCloseInputStream(InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)).use { it.readBytes() }
    }
    private fun add(db: JSONObject, id: Int, delay: Long, minutes: Int = 60) {
        db.getJSONArray("appointments").put(JSONObject().put("id", id).put("time", System.currentTimeMillis() + delay)
            .put("minutes", minutes).put("note", "예약 $id"))
    }
    private fun eventually(condition: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + 5_000
        while (!condition() && SystemClock.elapsedRealtime() < deadline) SystemClock.sleep(25)
        assertTrue("Notification manager did not reach the expected state", condition())
    }
    @Before fun reset() {
        if (Build.VERSION.SDK_INT >= 33) shell("pm grant com.jaeho.myfitlog android.permission.POST_NOTIFICATIONS")
        notifications.cancelAll(); repository.write(JSONObject(FitData.EMPTY))
        eventually { notifications.activeNotifications.isEmpty() }
    }
    @After fun cleanup() {
        notifications.cancelAll(); repository.write(JSONObject(FitData.EMPTY))
        eventually { notifications.activeNotifications.isEmpty() }
    }

    @Test fun editsAndDeletionReconcileAlarmTokensAndAlreadyVisibleNotifications() {
        val db = JSONObject(FitData.EMPTY); add(db, 11, 7200000); add(db, 12, 10800000)
        repository.write(db)
        assertNotNull(ReminderScheduler.existingAlarm(context, 11)); assertNotNull(ReminderScheduler.existingAlarm(context, 12))
        notifications.notify(11, Notification.Builder(context, ReminderScheduler.CHANNEL).setSmallIcon(android.R.drawable.ic_menu_my_calendar)
            .setContentTitle("이전 예약 알림").build())
        eventually { notifications.activeNotifications.any { it.id == 11 } }
        db.getJSONArray("appointments").getJSONObject(0).put("time", System.currentTimeMillis() + 14400000)
        repository.write(db)
        eventually { notifications.activeNotifications.none { it.id == 11 } }
        val data = FitData(db); data.deleteAppointment(11); repository.write(data.json)
        assertNull(ReminderScheduler.existingAlarm(context, 11)); assertNotNull(ReminderScheduler.existingAlarm(context, 12))
    }

    @Test fun bootReceiverRecreatesFutureAlarmsAndSkipsElapsedLeadTimes() {
        val db = JSONObject(FitData.EMPTY); add(db, 11, 7200000); add(db, 12, -60000); add(db, 13, 60000)
        repository.write(db); ReminderScheduler.cancelAlarm(context, 11)
        assertNull(ReminderScheduler.existingAlarm(context, 11))
        ReminderReceiver().onReceive(context, Intent(Intent.ACTION_BOOT_COMPLETED))
        assertNotNull(ReminderScheduler.existingAlarm(context, 11))
        assertNull(ReminderScheduler.existingAlarm(context, 12)); assertNull(ReminderScheduler.existingAlarm(context, 13))
    }

    @Test fun receiverRejectsEarlyPastMissingAndStaleReservations() {
        val db = JSONObject(FitData.EMPTY); add(db, 11, 7200000); add(db, 12, -60000); add(db, 14, 600000, 11)
        repository.write(db)
        fun receive(id: Int) = ReminderReceiver().onReceive(context, Intent(context, ReminderReceiver::class.java).putExtra("id", id))
        listOf(11, 12, 99).forEach(::receive)
        assertTrue(notifications.activeNotifications.isEmpty())
        val due = FitData(db).appointments().single { it.id == 14 }
        ReminderReceiver().onReceive(context, Intent(context, ReminderReceiver::class.java).putExtra("id", 14)
            .putExtra("reminderAt", due.reminderAt + 60000))
        assertTrue(notifications.activeNotifications.isEmpty())
        receive(14); eventually { notifications.activeNotifications.any { it.id == 14 } }
        val data = FitData(db); data.deleteAppointment(14); repository.write(data.json)
        eventually { notifications.activeNotifications.isEmpty() }
    }
}
