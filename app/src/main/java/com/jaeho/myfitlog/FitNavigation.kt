package com.jaeho.myfitlog

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

@Composable
internal fun FitNavigation(selected: String, onNavigate: (String) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        listOf(Triple("오늘", "오늘", Icons.Default.Home), Triple("기록", "기록", Icons.Default.Refresh),
            Triple("운동", "내 운동", Icons.AutoMirrored.Filled.List), Triple("예약", "PT", Icons.Default.DateRange))
            .forEach { (route, label, icon) ->
                NavigationBarItem(selected = route == selected, onClick = { onNavigate(route) },
                    icon = { Icon(icon, null) }, label = { Text(label) })
            }
    }
}
