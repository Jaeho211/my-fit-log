package com.jaeho.myfitlog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppointmentEditor(draft: AppointmentDraft, errors: Map<String, String>, now: Long,
                               access: NotificationAccess, modifier: Modifier, onEdit: (AppointmentDraft) -> Unit) {
    var chooseDate by rememberSaveable { mutableStateOf(false) }
    var chooseTime by rememberSaveable { mutableStateOf(false) }
    val compact = LocalConfiguration.current.screenWidthDp < 360 || LocalConfiguration.current.screenHeightDp < 700 ||
        LocalDensity.current.fontScale > 1.2f
    Column(modifier.fillMaxSize().testTag("appointment_editor").verticalScroll(rememberScrollState()).padding(ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text(if (draft.id == null) "다음 PT 준비하기" else "예약 일정 바꾸기", style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold)
        Text("날짜와 시간, 알림 시점을 정하고 PT에서 확인할 내용을 남겨보세요.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(onClick = { chooseDate = true }, modifier = Modifier.fillMaxWidth().testTag("appointment_date"),
            shape = RoundedCornerShape(14.dp), contentPadding = PaddingValues(16.dp)) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("날짜", style = MaterialTheme.typography.labelMedium)
                Text(LocalDate.parse(draft.date).format(DateTimeFormatter.ofPattern("yyyy년 M월 d일 (E)", Korean)),
                    style = MaterialTheme.typography.titleMedium)
            }
        }
        OutlinedButton(onClick = { chooseTime = true }, modifier = Modifier.fillMaxWidth().testTag("appointment_time"),
            shape = RoundedCornerShape(14.dp), contentPadding = PaddingValues(16.dp)) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("시간", style = MaterialTheme.typography.labelMedium)
                Text("%02d:%02d".format(draft.hour, draft.minute), style = MaterialTheme.typography.titleLarge)
            }
        }
        errors["time"]?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        Text("알림 시점", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("30", "60", "120").forEach { minutes ->
                FilterChip(selected = draft.minutes == minutes, onClick = { onEdit(draft.copy(minutes = minutes)) },
                    label = { Text("${minutes}분 전") })
            }
        }
        OutlinedTextField(value = draft.minutes, onValueChange = { onEdit(draft.copy(minutes = it)) },
            label = { Text("몇 분 전 알림") }, modifier = Modifier.fillMaxWidth().testTag("appointment_lead"),
            singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(14.dp), isError = errors["minutes"] != null,
            supportingText = { Text(errors["minutes"] ?: "1~1,000분 사이로 직접 입력할 수 있어요") })
        if (draft.errors(now).isEmpty() && draft.time() - draft.minutes.trim().toInt() * 60_000L <= now) {
            Text("선택한 알림 시각이 이미 지나 예약만 저장됩니다. 알림 시점을 바꿀 수 있어요.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        OutlinedTextField(value = draft.note, onValueChange = { onEdit(draft.copy(note = it)) },
            label = { Text("예약 메모 · 물어볼 내용") }, minLines = 3,
            modifier = Modifier.fillMaxWidth().testTag("appointment_note"), shape = RoundedCornerShape(14.dp))
        NotificationAccessCard(access)
    }
    if (chooseDate && compact) AppointmentDateInput(LocalDate.parse(draft.date),
        onDismiss = { chooseDate = false }, onApply = { date -> onEdit(draft.copy(date = date.toString())); chooseDate = false })
    if (chooseDate && !compact) {
        val initial = LocalDate.parse(draft.date)
        val state = rememberDatePickerState(initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            initialDisplayMode = if (compact) DisplayMode.Input else DisplayMode.Picker,
            yearRange = minOf(1900, initial.year)..maxOf(2100, initial.year))
        DatePickerDialog(onDismissRequest = { chooseDate = false }, confirmButton = {
            TextButton(enabled = state.selectedDateMillis != null, onClick = {
                state.selectedDateMillis?.let { value ->
                    onEdit(draft.copy(date = Instant.ofEpochMilli(value).atZone(ZoneOffset.UTC).toLocalDate().toString()))
                }
                chooseDate = false
            }) { Text("날짜 적용") }
        }, dismissButton = { TextButton(onClick = { chooseDate = false }) { Text("취소") } }) {
            DatePicker(state = state, modifier = Modifier.verticalScroll(rememberScrollState()))
        }
    }
    if (chooseTime) AppointmentTimeDialog(draft.hour, draft.minute,
        onDismiss = { chooseTime = false }, onApply = { hour, minute ->
            onEdit(draft.copy(hour = hour, minute = minute)); chooseTime = false
        })
}

@Composable
private fun AppointmentTimeDialog(initialHour: Int, initialMinute: Int, onDismiss: () -> Unit, onApply: (Int, Int) -> Unit) {
    var hour by rememberSaveable { mutableStateOf(initialHour.toString().padStart(2, '0')) }
    var minute by rememberSaveable { mutableStateOf(initialMinute.toString().padStart(2, '0')) }
    val valid = hour.toIntOrNull() in 0..23 && minute.toIntOrNull() in 0..59
    AlertDialog(onDismissRequest = onDismiss, title = { Text("PT 시간") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("24시간 기준으로 입력하세요.")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = hour, onValueChange = { if (it.length <= 2) hour = it },
                    label = { Text("시 (0~23)") }, singleLine = true,
                    modifier = Modifier.weight(1f).testTag("appointment_hour"), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = minute, onValueChange = { if (it.length <= 2) minute = it },
                    label = { Text("분 (0~59)") }, singleLine = true,
                    modifier = Modifier.weight(1f).testTag("appointment_minute"), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
        }
    }, confirmButton = { TextButton(enabled = valid, onClick = { onApply(hour.toInt(), minute.toInt()) }) { Text("시간 적용") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } })
}

@Composable
private fun AppointmentDateInput(initial: LocalDate, onDismiss: () -> Unit, onApply: (LocalDate) -> Unit) {
    var year by rememberSaveable { mutableStateOf(initial.year.toString()) }
    var month by rememberSaveable { mutableStateOf(initial.monthValue.toString()) }
    var day by rememberSaveable { mutableStateOf(initial.dayOfMonth.toString()) }
    val date = runCatching { LocalDate.of(year.toInt(), month.toInt(), day.toInt()) }.getOrNull()
    AlertDialog(onDismissRequest = onDismiss, title = { Text("PT 날짜") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(value = year, onValueChange = { if (it.length <= 4) year = it },
                label = { Text("연도") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("appointment_year"),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            OutlinedTextField(value = month, onValueChange = { if (it.length <= 2) month = it },
                label = { Text("월 (1~12)") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("appointment_month"),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            OutlinedTextField(value = day, onValueChange = { if (it.length <= 2) day = it },
                label = { Text("일 (1~31)") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("appointment_day"),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            if (date == null) Text("입력한 날짜를 확인해 주세요", color = MaterialTheme.colorScheme.error)
        }
    }, confirmButton = { TextButton(enabled = date != null, onClick = { date?.let(onApply) }) { Text("날짜 적용") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } })
}
