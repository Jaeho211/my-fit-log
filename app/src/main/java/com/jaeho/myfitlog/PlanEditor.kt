package com.jaeho.myfitlog

import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
internal fun PlanEditor(summary: TodaySummary, choices: (String) -> List<ExerciseChoice>,
                       onDismiss: () -> Unit, onAdd: () -> Unit, onApply: (String, Set<String>) -> Unit, error: String?) {
    var place by rememberSaveable {
        mutableStateOf(summary.plans.firstOrNull()?.key?.place?.takeIf { it == "집" || it == "헬스장" } ?: "집")
    }
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    val eligible = choices(place)
    var selected by rememberSaveable(place) {
        val existing = summary.plans.filter { it.key.place == place }.map { it.key.id }
        mutableStateOf(if (existing.isNotEmpty()) existing else eligible.take(3).map { it.id })
    }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("오늘 루틴 구성") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("장소와 운동을 선택한 뒤 적용하세요. 수행 기록은 유지됩니다.")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("집", "헬스장").forEach { name ->
                    FilterChip(selected = place == name, onClick = { place = name }, label = { Text(name) })
                }
            }
            if (eligible.isEmpty()) {
                Text("이 장소에서 할 수 있는 운동이 없어요. 내 운동에서 먼저 등록해 주세요.")
                TextButton(onClick = onAdd) { Text("내 운동 등록하기") }
            } else {
                Text("최근에 덜 한 운동부터 표시해요. 최대 3개를 기본 선택합니다.", style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { selected = emptyList() }, contentPadding = PaddingValues(0.dp)) {
                    Text("모두 해제")
                }
                if (selected.isEmpty() && summary.plans.isNotEmpty()) {
                    Text("적용하면 오늘 할 운동을 모두 비웁니다. 이미 완료한 운동 기록은 유지됩니다.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                eligible.forEach { exercise ->
                    Row(Modifier.fillMaxWidth().toggleable(value = exercise.id in selected, role = Role.Checkbox, onValueChange = { checked ->
                        selected = if (checked) selected + exercise.id else selected - exercise.id
                    }), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = exercise.id in selected, onCheckedChange = null)
                        Column(Modifier.padding(start = 8.dp)) {
                            Text(exercise.name, style = MaterialTheme.typography.bodyMedium)
                            Text(exercise.equipment, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }, confirmButton = {
        TextButton(
            onClick = {
                if (selected.isEmpty() && summary.plans.isNotEmpty()) confirmClear = true
                else onApply(place, selected.toSet())
            },
            enabled = selected.isNotEmpty() || summary.plans.isNotEmpty(),
        ) { Text(if (selected.isEmpty()) "오늘 루틴 비우기" else "루틴 적용") }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } })

    if (confirmClear) AlertDialog(
        onDismissRequest = { confirmClear = false },
        title = { Text("오늘 루틴을 모두 비울까요?") },
        text = { Text("오늘 할 운동 ${summary.plans.size}개를 계획에서 제외합니다. 이미 완료한 운동 기록과 내 운동 목록은 그대로 유지됩니다.") },
        confirmButton = {
            TextButton(onClick = {
                confirmClear = false
                onApply(place, emptySet())
            }) { Text("모두 비우기", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = { confirmClear = false }, modifier = Modifier.testTag("cancel_clear_plan")) { Text("취소") } },
    )
}
