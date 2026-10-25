package com.jaeho.myfitlog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
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
internal fun ExerciseListScreen(exercises: List<ExerciseDraft>, modifier: Modifier,
                                onOpen: (ExerciseDraft) -> Unit, onAdd: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var homeOnly by rememberSaveable { mutableStateOf(false) }
    val key = ExerciseDraft.searchKey(query)
    val filtered = exercises.filter { (!homeOnly || it.home) &&
        ExerciseDraft.searchKey(it.name + " " + it.equipment + " " + it.note).contains(key) }
    LazyColumn(modifier.fillMaxSize().testTag("exercise_list"), contentPadding = PaddingValues(ScreenPadding, 8.dp, ScreenPadding, 104.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("내 운동", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() })
            Spacer(Modifier.height(8.dp))
            Text("내가 배운 운동 ${exercises.size}개 · 목표와 자세를 한곳에", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            ExerciseSearch(query, { query = it }, "운동 이름 · 장비 · 메모 검색")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !homeOnly, onClick = { homeOnly = false }, label = { Text("전체") })
                FilterChip(selected = homeOnly, onClick = { homeOnly = true }, label = { Text("집에서도 가능") })
            }
        }
        if (filtered.isEmpty()) item {
            Surface(shape = CardShape) {
                Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(if (exercises.isEmpty()) "배운 운동을 모아보세요" else "일치하는 운동이 없어요",
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(if (exercises.isEmpty()) "PT에서 배운 이름과 자세 메모를 담아두면 오늘 루틴에서 바로 사용할 수 있어요."
                        else "다른 검색어를 입력하거나 전체 운동을 확인해 보세요.",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = { if (exercises.isEmpty()) onAdd() else { query = ""; homeOnly = false } }) {
                        Text(if (exercises.isEmpty()) "첫 운동 등록하기" else "검색·필터 초기화")
                    }
                }
            }
        }
        items(filtered, key = { it.id }) { exercise ->
            Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface) {
                Row(Modifier.fillMaxWidth().clickable { onOpen(exercise) }.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text(exercise.equipment + if (exercise.home) " · 집에서도 가능" else " · 헬스장·PT",
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Text(exercise.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${weightLabel(exercise.kg)} · ${exercise.reps}회 × ${exercise.sets}세트",
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (exercise.note.isNotBlank()) Text(exercise.note, maxLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
internal fun ExerciseSearch(query: String, onChange: (String) -> Unit, label: String) {
    OutlinedTextField(value = query, onValueChange = onChange, modifier = Modifier.fillMaxWidth().testTag("exercise_search"),
        singleLine = true, shape = RoundedCornerShape(16.dp), label = { Text(label) },
        leadingIcon = { Icon(Icons.Default.Search, null) }, trailingIcon = {
            if (query.isNotEmpty()) IconButton(onClick = { onChange("") }) { Icon(Icons.Default.Close, "검색어 지우기") }
        })
}
