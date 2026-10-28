package com.jaeho.myfitlog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun ExerciseCatalogScreen(exercises: List<ExerciseDraft>, modifier: Modifier,
                                   onCustom: () -> Unit, onPick: (CatalogExercise) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("전체") }
    val registered = exercises.map { ExerciseDraft.normalizedName(it.name) }.toSet()
    val results = ExerciseCatalog.filter { (category == "전체" || it.category == category) &&
        ExerciseDraft.searchKey(it.name + it.category + it.equipment).contains(ExerciseDraft.searchKey(query)) }
    LazyColumn(modifier.fillMaxSize().testTag("exercise_catalog_list"), contentPadding = PaddingValues(ScreenPadding, 8.dp, ScreenPadding, 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("운동 추가", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() })
            Spacer(Modifier.height(8.dp))
            Text("배운 운동을 골라 목표와 메모를 설정하세요. 저장한 운동만 내 운동에 추가됩니다.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            FilledTonalButton(onClick = onCustom, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("직접 운동 등록") }
        }
        item {
            ExerciseSearch(query, { query = it }, "운동 이름 · 부위 · 장비 검색")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("전체", "팔", "어깨", "등", "가슴", "복부", "철봉").forEach { name ->
                    FilterChip(selected = category == name, onClick = { category = name }, label = { Text(name) })
                }
            }
        }
        if (results.isEmpty()) item {
            Text("검색 결과가 없어요. 직접 운동을 등록할 수 있어요.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(results, key = { it.name }) { exercise ->
            val exists = ExerciseDraft.normalizedName(exercise.name) in registered
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
                Row(Modifier.fillMaxWidth().clickable(enabled = !exists) { onPick(exercise) }.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(exercise.name, fontWeight = FontWeight.SemiBold,
                            color = if (exists) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                        Text("${exercise.category} · ${exercise.equipment}", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(if (exists) "등록됨" else "선택", style = MaterialTheme.typography.labelMedium,
                        color = if (exists) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
