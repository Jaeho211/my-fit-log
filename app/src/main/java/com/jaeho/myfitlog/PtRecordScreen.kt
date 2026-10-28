package com.jaeho.myfitlog

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** PT sessions are recorded after exercise; saving writes all sets at once. */
@Composable
internal fun PtRecordScreen(
    date: LocalDate, exercises: List<ExerciseDraft>, today: List<HistoryGroup>, error: String?,
    onSave: (String, String, String, String, String) -> Boolean, onNavigate: (String) -> Unit,
    onDelete: (HistoryGroup) -> Boolean,
) {
    var selectedId by rememberSaveable { mutableStateOf("") }
    var kg by rememberSaveable { mutableStateOf("") }
    var reps by rememberSaveable { mutableStateOf("") }
    var count by rememberSaveable { mutableStateOf("3") }
    var memo by rememberSaveable { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var lastSaved by rememberSaveable { mutableStateOf("") }
    var deletingGroupKey by rememberSaveable { mutableStateOf("") }
    val deletingGroup = today.firstOrNull { it.key == deletingGroupKey }
    val selected = exercises.firstOrNull { it.id == selectedId }
    val focus = LocalFocusManager.current
    val back = { focus.clearFocus(); onNavigate("오늘") }
    BackHandler { back() }

    Scaffold(
        modifier = Modifier.fillMaxSize().imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Row(Modifier.fillMaxWidth().statusBarsPadding().windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .padding(horizontal = ScreenPadding, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = back, contentPadding = PaddingValues(0.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("오늘")
                }
                Spacer(Modifier.weight(1f))
                Text("PT 기록", style = MaterialTheme.typography.labelLarge)
            }
        },
        bottomBar = {
            if (exercises.isNotEmpty()) Surface(color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.navigationBarsPadding().windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                    .padding(horizontal = ScreenPadding, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (lastSaved.isNotBlank()) {
                        Text(lastSaved, color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall)
                    }
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall) }
                    Button(
                        onClick = {
                            focus.clearFocus()
                            val exercise = selected ?: return@Button
                            if (onSave(exercise.id, kg, reps, count, memo)) {
                                lastSaved = "${exercise.name} · ${count.trim()}세트 저장했어요"
                                selectedId = ""; kg = ""; reps = ""; count = "3"; memo = ""
                            }
                        },
                        enabled = selected != null,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) { Text("PT 기록 저장", fontWeight = FontWeight.Bold) }
                }
            }
        },
    ) { padding ->
        LazyColumn(Modifier.padding(padding).consumeWindowInsets(padding)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .fillMaxSize(), contentPadding = PaddingValues(ScreenPadding, 10.dp, ScreenPadding, 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)) {
            item {
                Text("PT 운동 기록", style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
                Spacer(Modifier.height(8.dp))
                Text("${date.format(DateTimeFormatter.ofPattern("M월 d일 EEEE", Korean))} · 수행한 운동만 기록하세요.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("운동 시작이나 세트별 완료 없이, 세트 수만큼 한 번에 저장합니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (exercises.isEmpty()) item {
                Surface(shape = CardShape) {
                    Column(Modifier.fillMaxWidth().padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("등록된 운동이 없어요", fontWeight = FontWeight.Bold)
                        Text("내 운동에 먼저 운동을 추가하세요.")
                        Button(onClick = { onNavigate("운동") }) { Text("내 운동 등록하기") }
                    }
                }
            }
            if (exercises.isNotEmpty()) item {
                Surface(shape = CardShape) {
                    Column(Modifier.fillMaxWidth().padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("운동 선택", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Box {
                            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                                Text(selected?.name ?: "기록할 운동을 선택하세요", modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ArrowDropDown, null)
                            }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                exercises.forEach { exercise ->
                                    DropdownMenuItem(text = { Text(exercise.name) }, onClick = {
                                        selectedId = exercise.id
                                        kg = if (exercise.equipment == "맨몸") "0"
                                            else exercise.kg.takeUnless { it == "0" }.orEmpty()
                                        reps = ""
                                        count = exercise.sets
                                        memo = ""
                                        lastSaved = ""
                                        expanded = false
                                    })
                                }
                            }
                        }
                        if (selected != null) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(value = kg, onValueChange = { kg = it },
                                    label = { Text("무게 kg") }, singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f))
                                OutlinedTextField(value = reps, onValueChange = { reps = it },
                                    label = { Text("횟수") }, singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f))
                            }
                            Text("무게나 횟수를 모르면 비워 두세요. 덤벨 무게는 한 개 기준입니다.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            OutlinedTextField(value = count, onValueChange = { count = it },
                                label = { Text("수행한 세트 수") }, singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = memo, onValueChange = { memo = it },
                                label = { Text("메모 (선택)") }, minLines = 2,
                                modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
            if (today.isNotEmpty()) item {
                Surface(shape = CardShape) {
                    Column(Modifier.fillMaxWidth().padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("오늘 기록한 PT 운동", style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold)
                        today.forEachIndexed { index, group ->
                            if (index > 0) HorizontalDivider()
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("${group.name} · ${group.records.size}세트", fontWeight = FontWeight.SemiBold)
                                    val last = group.records.last()
                                    Text("${weightLabel(last.kg)} · ${repsLabel(last.reps)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                TextButton(onClick = { deletingGroupKey = group.key }) {
                                    Text("전체 삭제", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                        TextButton(onClick = { onNavigate("기록") }) { Text("전체 기록 보기") }
                    }
                }
            }
        }
    }
    if (deletingGroup != null) AlertDialog(
        onDismissRequest = { deletingGroupKey = "" },
        title = { Text("${deletingGroup.name} 기록 삭제") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${deletingGroup.date}에 기록한 ${deletingGroup.name}의 PT ${deletingGroup.records.size}세트를 모두 삭제합니다. 같은 날 여러 번 기록했다면 모두 삭제되며, 다른 운동 기록은 유지됩니다.")
                Text("삭제한 기록은 되돌릴 수 없습니다.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val deletedName = deletingGroup.name
                if (onDelete(deletingGroup)) {
                    deletingGroupKey = ""
                    lastSaved = "${deletedName} PT 기록을 삭제했어요"
                }
            }) { Text("기록 전체 삭제", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = {
            TextButton(onClick = { deletingGroupKey = "" }) { Text("취소") }
        },
    )
}
