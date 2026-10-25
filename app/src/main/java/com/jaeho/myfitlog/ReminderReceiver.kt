package com.jaeho.myfitlog

import android.Manifest
import android.os.Build
import android.content.pm.PackageManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            schedule(context)
            return
        }
        runCatching {
            val id = intent.getIntExtra("id", Int.MIN_VALUE)
            val appointment = FitData(FitRepository(context).read()).appointments().firstOrNull { it.id == id }
                ?: return
            if (intent.hasExtra("reminderAt") && intent.getLongExtra("reminderAt", Long.MIN_VALUE) != appointment.reminderAt) return
            val now = System.currentTimeMillis()
            // Ignore canceled, rescheduled-early or already-past appointments.
            if (appointment.time <= now || appointment.reminderAt > now) return
            ReminderScheduler.ensureChannel(context)
            if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
            if (!ReminderScheduler.notificationsEnabled(context)) return
            val open = PendingIntent.getActivity(context, id,
                Intent(context, MainActivity::class.java).putExtra("section", "예약")
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            context.getSystemService(NotificationManager::class.java).notify(id,
                Notification.Builder(context, ReminderScheduler.CHANNEL)
                    .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
                    .setContentTitle("PT 예약 알림")
                    .setContentText(SimpleDateFormat("MM월 dd일 HH:mm", Locale.KOREA).format(Date(appointment.time)) +
                        appointment.note.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty())
                    .setContentIntent(open).setAutoCancel(true).setOnlyAlertOnce(true).build())
        }.onFailure { Log.w("FitReminder", "PT 알림을 표시하지 못했습니다: ${it.javaClass.simpleName}") }
    }
    companion object {
        @JvmStatic fun schedule(context: Context) = ReminderScheduler.schedule(context)
    }
}
