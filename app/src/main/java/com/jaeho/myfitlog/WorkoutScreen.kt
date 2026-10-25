package com.jaeho.myfitlog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
internal fun WorkoutScreen(p: ExercisePlan, sets: List<SetEntry>, previous: SetEntry?, kg: String, reps: String,
                          memo: String, recording: Boolean, modifier: Modifier,
                          onKg: (String) -> Unit, onReps: (String) -> Unit, onMemo: (String) -> Unit,
                          onExtra: () -> Unit, onRemove: () -> Unit) {
    var showNote by rememberSaveable(p.key.value) { mutableStateOf(false) }
    var showMemo by rememberSaveable(p.key.value) { mutableStateOf(false) }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(ScreenPadding, 8.dp, ScreenPadding, 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(8.dp)) {
                Text(p.key.place, Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(Modifier.height(12.dp))
            Text(p.name, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() })
        }
        previous?.let { last ->
            item {
                Surface(shape = RoundedCornerShape(14.dp)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("지난 기록 · ${formatTime(last.time, "M월 d일")}", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${weightLabel(last.kg)} × ${last.reps}회", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("세트 기록", fontWeight = FontWeight.Bold)
                Text("${sets.size} / ${p.sets} 완료", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        // Input precedes long histories so even routines with many sets remain easy to record.
        if (recording) {
            item {
                Text("${sets.size + 1}세트 입력", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = kg, onValueChange = onKg, label = { Text("무게 kg") },
                        singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f))
                    OutlinedTextField(value = reps, onValueChange = onReps, label = { Text("횟수") },
                        singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f))
                }
                Text("덤벨 무게는 한 개 기준", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
            }
        } else {
            item {
                Text("목표 세트를 마쳤어요!", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                TextButton(onClick = onExtra, contentPadding = PaddingValues(0.dp)) { Text("추가 세트 기록") }
            }
        }
        item { SetRow("세트", "무게", "횟수", "완료", header = true) }
        items(maxOf(p.sets, sets.size), key = { it }) { index ->
            val entry = sets.getOrNull(index)
            val current = index == sets.size && recording
            Column(Modifier.background(if (current) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                RoundedCornerShape(10.dp))) {
                SetRow("${index + 1}", weightLabel(entry?.kg ?: if (current) kg else p.kg), "${entry?.reps ?: if (current) reps else p.reps}회",
                    if (entry != null) "✓" else if (current) "입력 중" else "—", completed = entry != null)
                if (!entry?.memo.isNullOrBlank()) Text(entry!!.memo, Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            HorizontalDivider()
            TextButton(onClick = { showNote = !showNote }, contentPadding = PaddingValues(0.dp)) {
                Text(if (showNote) "자세 메모 접기" else "자세 메모 보기")
            }
            if (showNote) Text(p.note.ifBlank { "아직 자세 메모가 없어요. 내 운동에서 추가할 수 있어요." },
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (recording) {
                TextButton(onClick = { showMemo = !showMemo }, contentPadding = PaddingValues(0.dp)) {
                    Text(if (showMemo) "컨디션 메모 접기" else "컨디션 메모 남기기")
                }
                if (showMemo) OutlinedTextField(value = memo, onValueChange = onMemo,
                    modifier = Modifier.fillMaxWidth(), minLines = 2, label = { Text("힘든 정도 · 불편한 부위") })
            }
            TextButton(onClick = onRemove) { Text("루틴에서 제외", color = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun SetRow(set: String, kg: String, reps: String, status: String, header: Boolean = false, completed: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = if (header) 6.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        listOf(set, kg, reps, status).forEachIndexed { i, value ->
            Box(Modifier.weight(if (i == 1) 1.3f else 1f), contentAlignment = Alignment.Center) {
                Text(value, style = if (header) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodyMedium,
                    color = when { header -> MaterialTheme.colorScheme.onSurfaceVariant
                        completed -> MaterialTheme.colorScheme.primary; else -> MaterialTheme.colorScheme.onSurface })
            }
        }
    }
}
