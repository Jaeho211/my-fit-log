package com.jaeho.myfitlog

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FitApp(model: FitViewModel, onLegacy: (String) -> Unit) {
    val active = model.active
    var editPlan by rememberSaveable { mutableStateOf(false) }
    var removePlan by rememberSaveable { mutableStateOf(false) }
    var extraSetFor by rememberSaveable { mutableStateOf("") }
    var editorDate by rememberSaveable { mutableStateOf(model.summary.date.toString()) }
    val focus = LocalFocusManager.current
    val summary = model.summary
    val sets = model.sets
    val done = sets.size
    val progressKey = "${active?.key?.value}:$done"
    val isRecording = active != null && (done < active.sets || extraSetFor == progressKey)
    LaunchedEffect(summary.date) {
        if (editorDate != summary.date.toString()) {
            editPlan = false; removePlan = false; editorDate = summary.date.toString()
        }
    }
    BackHandler(active != null) { focus.clearFocus(); model.home() }

    Scaffold(
        modifier = Modifier.fillMaxSize().imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Row(Modifier.fillMaxWidth().statusBarsPadding().windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .padding(horizontal = ScreenPadding, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                if (active == null) {
                    Text("MY FIT LOG", style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    IconButton(onClick = { onLegacy("설정") }) { Icon(Icons.Default.Settings, "설정 열기") }
                } else {
                    TextButton(onClick = { focus.clearFocus(); model.home() }, contentPadding = PaddingValues(0.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp)); Text("오늘 루틴")
                    }
                    val index = summary.plans.indexOfFirst { it.key == active.key } + 1
                    Text("$index / ${summary.plans.size} 운동", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        bottomBar = {
            if (active == null) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    listOf(Triple("오늘", "오늘", Icons.Default.Home),
                        Triple("기록", "기록", Icons.Default.Refresh),
                        Triple("운동", "내 운동", Icons.AutoMirrored.Filled.List),
                        Triple("예약", "PT", Icons.Default.DateRange)).forEach { (route, label, icon) ->
                        NavigationBarItem(selected = route == "오늘", onClick = {
                            if (route != "오늘") onLegacy(route)
                        }, icon = { Icon(icon, null) }, label = { Text(label) })
                    }
                }
            } else {
                Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp) {
                    Column(Modifier.navigationBarsPadding().windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                        .padding(horizontal = ScreenPadding, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (model.undoToken.isNotEmpty()) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("세트를 기록했어요", style = MaterialTheme.typography.bodySmall)
                                TextButton(onClick = { model.undo() }) { Text("실행 취소") }
                            }
                        }
                        model.error?.let { Text(it, color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall) }
                        Button(onClick = {
                            focus.clearFocus()
                            if (isRecording) model.record(done) else {
                                val next = summary.plans.firstOrNull {
                                    it.key != active.key && (summary.counts[it.key.value] ?: 0) < it.sets
                                }
                                if (next == null) model.home() else model.open(next)
                            }
                        }, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp), shape = RoundedCornerShape(14.dp)) {
                            Text(if (isRecording) "${done + 1}세트 완료" else if (summary.plans.any {
                                it.key != active.key && (summary.counts[it.key.value] ?: 0) < it.sets
                            }) "다음 운동으로" else "오늘 루틴으로", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
    ) { padding ->
        val modifier = Modifier.padding(padding).consumeWindowInsets(padding)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
        if (active == null) {
            TodayScreen(summary, model.error, modifier, model::open,
                onEdit = { editPlan = true }, onAdd = { onLegacy("운동") }, onAppointments = { onLegacy("예약") })
        } else {
            WorkoutScreen(active, sets, model.previous, model.kg, model.reps, model.memo,
                isRecording, modifier, model::editKg, model::editReps, model::editMemo,
                onExtra = { extraSetFor = progressKey }, onRemove = { removePlan = true })
        }
    }
    if (editPlan) {
        PlanEditor(summary, model::choices, onDismiss = { editPlan = false }, onAdd = {
            editPlan = false; onLegacy("운동")
        }, onApply = { place, ids ->
            model.applyPlan(place, ids, summary.date)
            if (model.error == null) editPlan = false
        }, error = model.error)
    }
    if (removePlan && active != null) {
        AlertDialog(onDismissRequest = { removePlan = false }, title = { Text("오늘 루틴에서 제외할까요?") },
            text = { Text("${active.name}을 오늘 루틴에서 제외합니다. 이미 수행한 기록은 유지됩니다.") },
            confirmButton = { TextButton(onClick = { model.removeActive(); removePlan = false }) { Text("제외") } },
            dismissButton = { TextButton(onClick = { removePlan = false }) { Text("취소") } })
    }
}
