package com.jaeho.myfitlog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.format.DateTimeFormatter

@Composable
internal fun TodayScreen(summary: TodaySummary, error: String?, modifier: Modifier,
                        onOpen: (ExercisePlan) -> Unit, onEdit: () -> Unit,
                        onAdd: () -> Unit, onAppointments: () -> Unit, onPtRecord: () -> Unit) {
    val plans = summary.plans
    val finished = plans.count { (summary.counts[it.key.value] ?: 0) >= it.sets }
    val allDone = plans.isNotEmpty() && finished == plans.size
    val started = plans.any { (summary.counts[it.key.value] ?: 0) > 0 }
    val ptOnly = plans.isNotEmpty() && plans.all { it.key.place == "PT" }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(ScreenPadding, 8.dp, ScreenPadding, 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item {
            Text(summary.date.format(DateTimeFormatter.ofPattern("M월 d일 EEEE", Korean)),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            Text("오늘도, 한 세트씩.", style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
        }
        error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        item {
            Surface(color = Color(0xFF173B3A), contentColor = Color.White, shape = CardShape) {
                Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(if (plans.isEmpty()) "나의 운동 노트" else plans.map { it.key.place }.distinct().joinToString(" · ") + " · ${plans.size}개 운동",
                        color = Color(0xFFD2EBE5), style = MaterialTheme.typography.labelMedium)
                    Text(when { ptOnly -> "PT 운동 기록"; allDone -> "오늘 운동을 마쳤어요"; plans.isNotEmpty() -> "오늘의 운동 루틴"
                        summary.exerciseCount == 0 -> "배운 운동부터 담아볼까요?"; else -> "오늘은 어디서 운동할까요?" },
                        style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(when { ptOnly -> "PT는 운동을 시작하지 않고 수행한 내용을 기록합니다."
                        allDone -> "${plans.size}개 운동 완료 · 오늘도 수고했어요"
                        plans.isNotEmpty() -> "목표 ${plans.sumOf { it.sets }}세트 · $finished / ${plans.size} 운동 완료"
                        summary.exerciseCount == 0 -> "PT에서 배운 운동과 자세 메모를 저장해 두세요."
                        else -> "장소와 운동을 고르고 나만의 루틴을 시작하세요." },
                        color = Color(0xFFC3D8D4), style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(2.dp))
                    Button(onClick = {
                        if (ptOnly) onPtRecord()
                        else if (plans.isEmpty()) { if (summary.exerciseCount == 0) onAdd() else onEdit() }
                        else onOpen(plans.firstOrNull { (summary.counts[it.key.value] ?: 0) < it.sets } ?: plans.first())
                    }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB8E8D5), contentColor = Color(0xFF153C35))) {
                        Text(when { ptOnly -> "PT 기록하기"; allDone -> "오늘 루틴 살펴보기"; started -> "운동 이어하기"; plans.isNotEmpty() -> "운동 시작"
                            summary.exerciseCount == 0 -> "운동 골라 담기"; else -> "오늘 루틴 구성" },
                            modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(20.dp))
                    }
                }
            }
        }
        if (!ptOnly) item {
            OutlinedButton(onClick = onPtRecord, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp)) {
                Icon(Icons.Default.Edit, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("PT 운동 기록하기")
            }
        }
        if (plans.isNotEmpty()) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("오늘 할 운동", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    TextButton(onClick = onEdit) { Text("루틴 편집") }
                }
                Surface(shape = CardShape, color = MaterialTheme.colorScheme.surface) {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        plans.forEachIndexed { i, p ->
                            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Row(Modifier.fillMaxWidth().clickable { if (p.key.place == "PT") onPtRecord() else onOpen(p) }.padding(vertical = 18.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Surface(shape = RoundedCornerShape(9.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                                    Text("%02d".format(i + 1), Modifier.padding(8.dp), style = MaterialTheme.typography.labelMedium)
                                }
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(p.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text("${weightLabel(p.kg)} · ${p.reps}회 × ${p.sets}세트",
                                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                val done = summary.counts[p.key.value] ?: 0
                                Text(if (done >= p.sets) "완료 ✓" else "$done / ${p.sets}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (done >= p.sets) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
        item { WeekCard(summary) }
        summary.nextAppointment?.let { next ->
            item {
                TextButton(onClick = onAppointments, contentPadding = PaddingValues(0.dp)) {
                    Icon(Icons.Default.DateRange, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                    Text("다음 PT · ${formatTime(next.time, "M월 d일 (E) a h:mm")}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun WeekCard(summary: TodaySummary) {
    Surface(shape = CardShape, color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("이번 주의 꾸준함", style = MaterialTheme.typography.titleSmall)
                Text("${summary.trainedDays.size} / ${summary.weeklyGoal}일", color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                summary.week.forEach { date ->
                    val trained = date in summary.trainedDays
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(date.format(DateTimeFormatter.ofPattern("E", Korean)),
                            color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                        Spacer(Modifier.height(7.dp))
                        Surface(shape = CircleShape,
                            color = if (trained || date == summary.date) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.background) {
                            Text(if (trained) "✓" else date.dayOfMonth.toString(), Modifier.padding(7.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (trained) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
    }
}
