package com.jaeho.myfitlog

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import org.json.JSONObject

@Composable
internal fun AppointmentScreen(appointments: List<PtAppointment>, error: String?, onSave: (AppointmentDraft) -> Int?,
                               onDelete: (Int) -> Boolean, clearError: () -> Unit, onNavigate: (String) -> Unit) {
    var page by rememberSaveable { mutableStateOf("list") }
    var selectedId by rememberSaveable { mutableIntStateOf(Int.MIN_VALUE) }
    var editorReturn by rememberSaveable { mutableStateOf("list") }
    var draftJson by rememberSaveable { mutableStateOf(AppointmentDraft().toJson().toString()) }
    var initialJson by rememberSaveable { mutableStateOf(draftJson) }
    var discard by rememberSaveable { mutableStateOf(false) }
    var deleting by rememberSaveable { mutableStateOf(false) }
    var attempted by rememberSaveable { mutableStateOf(false) }
    var showPast by rememberSaveable { mutableStateOf(false) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val owner = LocalLifecycleOwner.current
    val access = rememberNotificationAccess()
    val focus = LocalFocusManager.current
    val draft = remember(draftJson) { AppointmentDraft.fromJson(JSONObject(draftJson)) }
    val selected = appointments.firstOrNull { it.id == selectedId }
    val future = appointments.filter { it.time > now }.sortedBy { it.time }
    val past = appointments.filter { it.time <= now }.sortedByDescending { it.time }
    LaunchedEffect(owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) { now = System.currentTimeMillis(); delay(30_000) }
        }
    }
    LaunchedEffect(appointments, selectedId, page) { if (page == "detail" && selected == null) { page = "list"; deleting = false } }
    fun edit(value: AppointmentDraft, origin: String) {
        draftJson = value.toJson().toString(); initialJson = draftJson; editorReturn = origin
        attempted = false; clearError(); page = "edit"
    }
    val back: () -> Unit = {
        focus.clearFocus(); clearError()
        when (page) {
            "edit" -> if (draftJson != initialJson) discard = true else page = editorReturn
            "detail" -> page = "list"
            else -> onNavigate("오늘")
        }
    }
    BackHandler { back() }
    Scaffold(modifier = Modifier.fillMaxSize().imePadding(), containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0), topBar = {
            Row(Modifier.fillMaxWidth().statusBarsPadding().windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)).padding(horizontal = ScreenPadding, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                if (page == "list") {
                    Text("MY FIT LOG", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    IconButton(onClick = { onNavigate("설정") }) { Icon(Icons.Default.Settings, "설정 열기") }
                } else {
                    TextButton(onClick = back, contentPadding = PaddingValues(0.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp))
                        Text(if (page == "edit") if (draft.id == null) "PT 예약 등록" else "PT 예약 수정" else "PT 일정")
                    }
                    if (page == "detail") TextButton(onClick = { deleting = true }) { Text("삭제", color = MaterialTheme.colorScheme.error) }
                }
            }
        }, bottomBar = {
            if (page == "list") FitNavigation("예약", onNavigate)
            else Surface(color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.navigationBarsPadding().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                    .padding(horizontal = ScreenPadding, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    if (page == "edit" && attempted) draft.errors(now).values.firstOrNull()?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    Button(onClick = {
                        focus.clearFocus()
                        if (page == "edit") {
                            now = System.currentTimeMillis(); attempted = true
                            if (draft.errors(now).isEmpty()) onSave(draft)?.let { id -> selectedId = id; page = "detail"; attempted = false }
                        } else selected?.let {
                            edit(if (it.time > now) it.draft() else AppointmentDraft(note = it.note, minutes = it.minutes.toString()), "detail")
                        }
                    }, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp), shape = RoundedCornerShape(14.dp)) {
                        Text(if (page == "edit") "예약 저장" else if (selected != null && selected.time > now) "예약 수정" else "다시 예약하기",
                            fontWeight = FontWeight.Bold)
                    }
                }
            }
        }, floatingActionButton = {
            if (page == "list") ExtendedFloatingActionButton(onClick = { edit(AppointmentDraft(), "list") },
                modifier = Modifier.testTag("add_appointment").semantics { contentDescription = "PT 예약 추가" },
                icon = { Icon(Icons.Default.Add, null) }, text = { Text("PT 예약") },
                containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
        }) { padding ->
        val modifier = Modifier.padding(padding).consumeWindowInsets(padding)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
        when (page) {
            "list" -> LazyColumn(modifier.fillMaxSize().testTag("appointment_list"),
                contentPadding = PaddingValues(ScreenPadding, 8.dp, ScreenPadding, 100.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)) {
                item {
                    Text("PT 일정", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                        modifier = Modifier.semantics { heading() })
                    Spacer(Modifier.height(8.dp))
                    Text("예약을 확인하고, 다음 PT를 준비하세요.", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                item { NotificationAccessCard(access) }
                error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
                if (future.isEmpty()) item {
                    Surface(shape = CardShape) {
                        Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("다음 PT를 준비하세요", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("예약 시간과 트레이너에게 물어볼 내용을 남겨보세요.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            TextButton(onClick = { edit(AppointmentDraft(), "list") }) { Text("첫 PT 예약하기") }
                        }
                    }
                }
                future.firstOrNull()?.let { next ->
                    item {
                        Surface(shape = CardShape, color = Color(0xFF173B3A), contentColor = Color.White) {
                            Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("다음 PT", style = MaterialTheme.typography.labelLarge, color = Color(0xFFD2EBE5))
                                Text(formatTime(next.time, "M월 d일 EEEE"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                Text(formatTime(next.time, "a h:mm"), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                                Text(next.note.ifBlank { "PT에서 확인할 내용을 메모해 보세요." }, maxLines = 2,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, color = Color(0xFFC3D8D4))
                                Text(if (next.reminderAt > now) "${next.minutes}분 전 알림" else "알림 시각이 이미 지났어요",
                                    style = MaterialTheme.typography.labelMedium, color = Color(0xFFD2EBE5))
                                Button(onClick = { clearError(); selectedId = next.id; page = "detail" },
                                    modifier = Modifier.fillMaxWidth().testTag("appointment_${next.id}").heightIn(min = 50.dp),
                                    shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFB8E8D5), contentColor = Color(0xFF153C35))) { Text("예약 상세 보기") }
                            }
                        }
                    }
                }
                if (future.size > 1) item { Text("예정된 일정 ${future.size - 1}건", fontWeight = FontWeight.Bold) }
                items(future.drop(1), key = { "future:${it.id}" }) { appointment ->
                    AppointmentRow(appointment, onOpen = { clearError(); selectedId = appointment.id; page = "detail" })
                }
                if (past.isNotEmpty()) {
                    item { TextButton(onClick = { showPast = !showPast }) { Text(if (showPast) "지난 일정 접기 (${past.size})" else "지난 일정 보기 (${past.size})") } }
                    if (showPast) items(past, key = { "past:${it.id}" }) { appointment ->
                        AppointmentRow(appointment, onOpen = { clearError(); selectedId = appointment.id; page = "detail" })
                    }
                }
            }
            "edit" -> AppointmentEditor(draft, if (attempted) draft.errors(now) else emptyMap(), now, access, modifier) {
                draftJson = it.toJson().toString(); clearError()
            }
            "detail" -> selected?.let { AppointmentDetail(it, now, access, modifier) }
        }
    }
    if (discard) AlertDialog(onDismissRequest = { discard = false }, title = { Text("변경 내용을 취소할까요?") },
        text = { Text("저장하지 않은 예약 입력은 사라집니다.") },
        confirmButton = { TextButton(onClick = { discard = false; draftJson = initialJson; page = editorReturn; clearError() }) { Text("변경 취소") } },
        dismissButton = { TextButton(onClick = { discard = false }) { Text("계속 작성") } })
    if (deleting && selected != null) AlertDialog(onDismissRequest = { deleting = false; clearError() }, title = { Text("PT 예약을 삭제할까요?") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("${formatTime(selected.time, "yyyy년 M월 d일 (E) a h:mm")}\n예약과 해당 알림을 삭제합니다. 운동 기록은 유지됩니다.")
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }, confirmButton = { TextButton(onClick = { if (onDelete(selected.id)) { deleting = false; page = "list" } }) {
            Text("예약 삭제", color = MaterialTheme.colorScheme.error)
        } }, dismissButton = { TextButton(onClick = { deleting = false; clearError() }) { Text("취소") } })
}

@Composable
private fun AppointmentRow(appointment: PtAppointment, onOpen: () -> Unit) {
    Surface(shape = CardShape) {
        Column(Modifier.fillMaxWidth().testTag("appointment_${appointment.id}").clickable(onClick = onOpen).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(formatTime(appointment.time, "yyyy년 M월 d일 (E) a h:mm"), fontWeight = FontWeight.SemiBold)
            Text("${appointment.minutes}분 전 알림", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            if (appointment.note.isNotBlank()) Text(appointment.note, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AppointmentDetail(appointment: PtAppointment, now: Long, access: NotificationAccess, modifier: Modifier) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text(if (appointment.time > now) "예정된 PT" else "지난 PT", color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge)
        Text(formatTime(appointment.time, "yyyy년 M월 d일 EEEE"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(formatTime(appointment.time, "a h:mm"), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Surface(shape = CardShape) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("예약 메모", fontWeight = FontWeight.Bold)
                Text(appointment.note.ifBlank { "아직 메모가 없어요" })
            }
        }
        Text("${appointment.minutes}분 전 알림 · ${formatTime(appointment.reminderAt, "M월 d일 (E) a h:mm")}",
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (appointment.time > now && appointment.reminderAt <= now) Text("알림 시각이 지나 이번 예약의 알림은 울리지 않습니다. 알림 시점을 수정할 수 있어요.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        NotificationAccessCard(access)
    }
}
