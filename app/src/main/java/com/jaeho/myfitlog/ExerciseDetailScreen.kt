package com.jaeho.myfitlog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun ExerciseDetailScreen(exercise: ExerciseDraft, modifier: Modifier) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(ScreenPadding, 8.dp, ScreenPadding, 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item {
            Text(exercise.equipment, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(10.dp))
            Text(exercise.name, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() })
        }
        item { TargetCard("헬스장 · PT 목표", exercise.kg, exercise.reps, exercise.sets) }
        if (exercise.home) item { TargetCard("집 목표", exercise.effectiveHomeKg, exercise.homeReps, exercise.homeSets) }
        item {
            Surface(shape = CardShape) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("자세 메모", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(exercise.note.ifBlank { "아직 메모가 없어요. PT에서 배운 자세와 주의사항을 남겨보세요." },
                        style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Text("목표와 메모 변경은 새로 구성하는 루틴부터 반영됩니다. 진행 중인 오늘 루틴과 수행 기록은 유지됩니다.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TargetCard(title: String, kg: String, reps: String, sets: String) {
    Surface(shape = CardShape) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Text(weightLabel(kg), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("${reps}회 × ${sets}세트", style = MaterialTheme.typography.titleMedium)
            Text("덤벨 무게는 한 개 기준", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
