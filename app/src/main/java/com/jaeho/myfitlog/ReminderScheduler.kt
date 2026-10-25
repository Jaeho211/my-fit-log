package com.jaeho.myfitlog

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import org.json.JSONObject

/** Keeps the original receiver component, request codes and channel for installed apps. */
internal object ReminderScheduler {
    const val CHANNEL = "pt"
    fun ensureChannel(context: Context) {
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "PT 예약", NotificationManager.IMPORTANCE_HIGH))
    }
    fun notificationsEnabled(context: Context): Boolean {
        val manager = context.getSystemService(NotificationManager::class.java)
        return manager.areNotificationsEnabled() && manager.getNotificationChannel(CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE
    }
    fun existingAlarm(context: Context, id: Int): PendingIntent? = PendingIntent.getBroadcast(context, id,
        Intent(context, ReminderReceiver::class.java), PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)

    fun cancelAlarm(context: Context, id: Int) {
        existingAlarm(context, id)?.let {
            context.getSystemService(AlarmManager::class.java).cancel(it)
            it.cancel()
        }
    }
    private fun rawAppointments(json: JSONObject?): Map<Int, String> {
        val appointments = json?.optJSONArray("appointments") ?: return emptyMap()
        return (0 until appointments.length()).associate { i ->
            val a = appointments.getJSONObject(i)
            a.getInt("id") to a.toString()
        }
    }
    fun reconcile(context: Context, previous: JSONObject?, current: JSONObject) {
        runCatching {
            ensureChannel(context)
            val before = rawAppointments(previous)
            val after = rawAppointments(current)
            (before.keys + after.keys).forEach { cancelAlarm(context, it) }
            before.forEach { (id, value) ->
                if (after[id] != value) context.getSystemService(NotificationManager::class.java).cancel(id)
            }
            val manager = context.getSystemService(AlarmManager::class.java)
            futurePtReminders(FitData(current).appointments(), System.currentTimeMillis()).forEach { reminder ->
                val pending = PendingIntent.getBroadcast(context, reminder.id,
                    Intent(context, ReminderReceiver::class.java).putExtra("id", reminder.id).putExtra("reminderAt", reminder.triggerAt),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.triggerAt, pending)
            }
        }.onFailure { Log.w("FitReminder", "PT 알림을 재예약하지 못했습니다: ${it.javaClass.simpleName}") }
    }
    fun schedule(context: Context) {
        runCatching { reconcile(context, null, FitRepository(context).read()) }
            .onFailure { Log.w("FitReminder", "저장된 PT 예약을 읽지 못했습니다") }
    }
}
