package com.jaeho.myfitlog

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

@Composable
internal fun HistoryScreen(history: List<HistoryDay>, today: LocalDate, error: String?,
                           onDelete: (HistoryKey) -> Boolean, clearError: () -> Unit, onNavigate: (String) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var selectedKey by rememberSaveable { mutableStateOf("") }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var weekOnly by rememberSaveable { mutableStateOf(false) }
    var deleted by rememberSaveable { mutableStateOf(false) }
    val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val days = if (weekOnly) history.filter { it.date in monday..today } else history
    val groups = history.flatMap { it.groups }
    val selectedGroup = if (selectedKey.isBlank()) null else groups.firstOrNull { g -> g.records.any { it.key.value == selectedKey } }
    val selected = selectedGroup?.records?.firstOrNull { it.key.value == selectedKey }
    val number = selectedGroup?.records?.indexOfFirst { it.key.value == selectedKey }?.plus(1) ?: 0
    LaunchedEffect(history) {
        expanded = expanded.filter { key -> groups.any { it.key == key } }
        if (selected == null) { selectedKey = ""; confirmDelete = false }
    }
    BackHandler { onNavigate("오늘") }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background, contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Row(Modifier.fillMaxWidth().statusBarsPadding().windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .padding(horizontal = ScreenPadding, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("MY FIT LOG", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                IconButton(onClick = { onNavigate("설정") }) { Icon(Icons.Default.Settings, "설정 열기") }
            }
        },
        bottomBar = { FitNavigation("기록", onNavigate) },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)).testTag("history_list"),
            contentPadding = PaddingValues(ScreenPadding, 8.dp, ScreenPadding, 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text("운동 기록", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics { heading() })
                Spacer(Modifier.height(8.dp))
                Text("총 ${history.size}일 · ${history.sumOf { it.setCount }}세트의 꾸준함",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !weekOnly, onClick = { weekOnly = false }, label = { Text("전체") })
                    FilterChip(selected = weekOnly, onClick = { weekOnly = true }, label = { Text("이번 주") })
                }
            }
            error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
            if (deleted) item {
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(12.dp)) {
                    Text("세트 기록을 삭제했어요", Modifier.padding(14.dp), style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (days.isEmpty()) item {
                Surface(shape = CardShape) {
                    Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(if (history.isEmpty()) "첫 기록을 기다리고 있어요" else "이번 주 기록이 아직 없어요",
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(if (history.isEmpty()) "오늘 루틴에서 완료한 세트가 날짜별로 모입니다."
                            else "전체 기록에서 이전 운동을 확인할 수 있어요.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = { if (history.isEmpty()) onNavigate("오늘") else weekOnly = false }) {
                            Text(if (history.isEmpty()) "오늘 운동하러 가기" else "전체 기록 보기")
                        }
                    }
                }
            }
            days.forEach { day ->
                item(key = "day:${day.date}") {
                    Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(historyDateLabel(day.date, today), style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
                        Text("운동 ${day.groups.size}건 · ${day.setCount}세트", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                day.groups.forEach { group ->
                    val open = group.key in expanded
                    item(key = "group:${group.key}") {
                        HistoryGroupCard(group, open) {
                            expanded = if (open) expanded - group.key else expanded + group.key
                        }
                    }
                    if (open) itemsIndexed(group.records, key = { _, record -> "set:${record.key.value}" }) { i, record ->
                        Surface(modifier = Modifier.padding(start = 12.dp), shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface) {
                            Column(Modifier.fillMaxWidth().testTag("history_set_${record.key.value}")
                                .clickable(onClickLabel = "세트 상세 보기") {
                                    clearError(); selectedKey = record.key.value; confirmDelete = false
                                }.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("${i + 1}세트", style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary)
                                    Text(formatTime(record.time, "a h:mm"), style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text("${weightLabel(record.kg)} × ${record.reps}회", style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold)
                                if (record.memo.isNotBlank()) Text(record.memo, maxLines = 2,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
    if (selected != null && !confirmDelete) AlertDialog(
        onDismissRequest = { selectedKey = ""; clearError() },
        title = { Text("${selected.name} · ${number}세트") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(formatTime(selected.time, "yyyy년 M월 d일 (E) a h:mm:ss"), style = MaterialTheme.typography.bodySmall)
                Text(selected.place, color = MaterialTheme.colorScheme.primary)
                Text("${weightLabel(selected.kg)} × ${selected.reps}회", style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold)
                HorizontalDivider()
                Text("컨디션 메모", fontWeight = FontWeight.Bold)
                Text(selected.memo.ifBlank { "남긴 메모가 없어요" })
            }
        },
        confirmButton = { TextButton(onClick = { selectedKey = ""; clearError() }) { Text("닫기") } },
        dismissButton = { TextButton(onClick = { confirmDelete = true }) {
            Text("세트 삭제", color = MaterialTheme.colorScheme.error)
        } },
    )
    if (selected != null && confirmDelete) AlertDialog(onDismissRequest = { confirmDelete = false; clearError() },
        title = { Text("이 세트 기록을 삭제할까요?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("${selected.name} · ${number}세트\n${weightLabel(selected.kg)} × ${selected.reps}회\n\n선택한 세트만 삭제합니다. 오늘 루틴의 진행과 주간 달성 일수도 다시 계산됩니다.")
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton(onClick = {
            if (onDelete(selected.key)) { selectedKey = ""; confirmDelete = false; deleted = true }
        }) { Text("기록 삭제", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = { confirmDelete = false; clearError() }) { Text("취소") } })
}

@Composable
private fun HistoryGroupCard(group: HistoryGroup, open: Boolean, onToggle: () -> Unit) {
    val latest = group.records.last()
    Surface(shape = CardShape, color = MaterialTheme.colorScheme.surface) {
        Row(Modifier.fillMaxWidth().testTag("history_group_${group.key}")
            .semantics { stateDescription = if (open) "펼침" else "접힘" }
            .clickable(onClickLabel = if (open) "세트 기록 접기" else "세트 기록 펼치기", onClick = onToggle)
            .padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("${group.place} · ${group.records.size}세트", color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium)
                Text(group.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("마지막 세트 ${weightLabel(latest.kg)} × ${latest.reps}회", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(if (open) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

internal fun historyDateLabel(date: LocalDate, today: LocalDate): String {
    val label = date.format(DateTimeFormatter.ofPattern(if (date.year == today.year) "M월 d일 EEEE" else "yyyy년 M월 d일 EEEE", Korean))
    return when (date) { today -> "오늘 · $label"; today.minusDays(1) -> "어제 · $label"; else -> label }
}
