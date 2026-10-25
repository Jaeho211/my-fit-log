package com.jaeho.myfitlog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
internal fun ExerciseEditorScreen(draft: ExerciseDraft, errors: Map<String, String>, modifier: Modifier,
                                  validationAttempt: Int, onEdit: (ExerciseDraft) -> Unit) {
    val scroll = rememberLazyListState()
    LaunchedEffect(validationAttempt) {
        if (errors.isNotEmpty()) scroll.animateScrollToItem(when (errors.keys.first()) {
            "name" -> 1; "equipment" -> 2; "kg", "reps", "sets" -> 4; else -> 6
        })
    }
    LazyColumn(modifier.fillMaxSize().testTag("exercise_editor_list"), state = scroll,
        contentPadding = PaddingValues(ScreenPadding, 8.dp, ScreenPadding, 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item {
            Text(if (draft.id.isBlank()) "배운 운동 담기" else "나에게 맞는 목표", style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("정확한 운동 이름과 PT에서 배운 자세를 기록하세요.", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item { EditField(draft.name, { onEdit(draft.copy(name = it)) }, "운동 이름", "exercise_name", errors["name"]) }
        item { EditField(draft.equipment, { onEdit(draft.copy(equipment = it)) }, "장비 · 맨몸", "exercise_equipment", errors["equipment"]) }
        item {
            Text("헬스장 · PT 목표", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("덤벨은 한 개 무게, 맨몸은 0 kg으로 입력하세요.", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            TargetFields(draft.kg, draft.reps, draft.sets, "gym", errors["kg"], errors["reps"], errors["sets"],
                { onEdit(draft.copy(kg = it)) }, { onEdit(draft.copy(reps = it)) }, { onEdit(draft.copy(sets = it)) })
        }
        item {
            HorizontalDivider()
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("집에서도 가능한 운동", style = MaterialTheme.typography.titleSmall)
                    Text("집 장비로 혼자 수행할 수 있어요", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = draft.home, onCheckedChange = { onEdit(draft.copy(home = it)) },
                    modifier = Modifier.testTag("exercise_home"))
            }
        }
        item {
            if (draft.home) {
                Text("집 목표", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                TargetFields(draft.effectiveHomeKg, draft.homeReps, draft.homeSets, "home",
                    errors["homeKg"], errors["homeReps"], errors["homeSets"],
                    { onEdit(draft.copy(homeKg = it)) }, { onEdit(draft.copy(homeReps = it)) }, { onEdit(draft.copy(homeSets = it)) },
                    fixedWeight = draft.dumbbell)
                if (draft.dumbbell) Text("집 덤벨은 한 개당 3 kg으로 고정됩니다.", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
            }
        }
        item {
            OutlinedTextField(value = draft.note, onValueChange = { onEdit(draft.copy(note = it)) },
                label = { Text("자세 메모 · 주의사항") }, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().testTag("exercise_note"), minLines = 3)
        }
        item {
            Text("저장한 목표는 새로 구성하는 루틴부터 적용됩니다. 오늘 루틴과 과거 기록은 바뀌지 않습니다.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TargetFields(kg: String, reps: String, sets: String, prefix: String,
                         kgError: String?, repsError: String?, setsError: String?,
                         onKg: (String) -> Unit, onReps: (String) -> Unit, onSets: (String) -> Unit, fixedWeight: Boolean = false) {
    val largeText = LocalDensity.current.fontScale > 1.2f
    BoxWithConstraints {
        if (maxWidth < 300.dp || largeText) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                EditField(kg, onKg, "무게 kg", "${prefix}_kg", kgError, KeyboardType.Decimal, fixedWeight)
                EditField(reps, onReps, "횟수", "${prefix}_reps", repsError, KeyboardType.Number)
                EditField(sets, onSets, "세트", "${prefix}_sets", setsError, KeyboardType.Number)
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.weight(1f)) { EditField(kg, onKg, "무게 kg", "${prefix}_kg", kgError, KeyboardType.Decimal, fixedWeight) }
                Box(Modifier.weight(1f)) { EditField(reps, onReps, "횟수", "${prefix}_reps", repsError, KeyboardType.Number) }
                Box(Modifier.weight(1f)) { EditField(sets, onSets, "세트", "${prefix}_sets", setsError, KeyboardType.Number) }
            }
        }
    }
}

@Composable
private fun EditField(value: String, onValue: (String) -> Unit, label: String, tag: String,
                      error: String?, type: KeyboardType = KeyboardType.Text, readOnly: Boolean = false) {
    OutlinedTextField(value = value, onValueChange = onValue, label = { Text(label) },
        modifier = Modifier.fillMaxWidth().testTag(tag), singleLine = true, readOnly = readOnly, shape = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(keyboardType = type), isError = error != null,
        supportingText = if (error == null) null else { { Text(error) } })
}
