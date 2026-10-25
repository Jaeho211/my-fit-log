package com.jaeho.myfitlog

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

internal data class NotificationAccess(val enabled: Boolean, val request: () -> Unit)

@Composable
internal fun rememberNotificationAccess(): NotificationAccess {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val preferences = remember(context) { context.getSharedPreferences("fit", Context.MODE_PRIVATE) }
    var enabled by remember { mutableStateOf(ReminderScheduler.notificationsEnabled(context)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        enabled = ReminderScheduler.notificationsEnabled(context)
        if (enabled) ReminderScheduler.schedule(context)
    }
    DisposableEffect(owner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) enabled = ReminderScheduler.notificationsEnabled(context)
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    return NotificationAccess(enabled) {
        val missingPermission = Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        val mayAskAgain = !preferences.getBoolean("notifications_requested", false) ||
            (Build.VERSION.SDK_INT >= 33 && (context as? Activity)?.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) == true)
        if (missingPermission && mayAskAgain) {
            preferences.edit().putBoolean("notifications_requested", true).apply()
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            val manager = context.getSystemService(NotificationManager::class.java)
            context.startActivity(Intent(if (manager.areNotificationsEnabled()) Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS
                else Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                .putExtra(Settings.EXTRA_CHANNEL_ID, ReminderScheduler.CHANNEL))
        }
    }
}

@Composable
internal fun NotificationAccessCard(access: NotificationAccess) {
    if (access.enabled) {
        Text("알림 사용 중 · 기기 정책에 따라 지연될 수 있어요.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else {
        Surface(shape = CardShape, color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("PT 알림이 꺼져 있어요", style = MaterialTheme.typography.titleSmall)
                Text("예약은 저장되지만 알림이 표시되지 않습니다.", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = access.request) { Text("알림 켜기") }
            }
        }
    }
}
