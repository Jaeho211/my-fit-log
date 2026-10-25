package com.jaeho.myfitlog

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject

@Composable
internal fun ExerciseLibrary(
    exercises: List<ExerciseDraft>, error: String?, onSave: (ExerciseDraft) -> String?,
    onDelete: (String) -> Boolean, clearError: () -> Unit, onNavigate: (String) -> Unit,
) {
    var page by rememberSaveable { mutableStateOf("list") }
    var selectedId by rememberSaveable { mutableStateOf("") }
    var editorReturn by rememberSaveable { mutableStateOf("catalog") }
    var draftJson by rememberSaveable { mutableStateOf(ExerciseDraft().toJson().toString()) }
    var initialJson by rememberSaveable { mutableStateOf(draftJson) }
    var discard by rememberSaveable { mutableStateOf(false) }
    var delete by rememberSaveable { mutableStateOf(false) }
    val stateHolder = rememberSaveableStateHolder()
    var validationAttempt by rememberSaveable { mutableIntStateOf(0) }
    var submitted by rememberSaveable { mutableStateOf(false) }
    val draft = remember(draftJson) { ExerciseDraft.fromJson(JSONObject(draftJson)) }
    val selected = exercises.firstOrNull { it.id == selectedId }
    val focus = LocalFocusManager.current
    val errors = if (submitted) draft.errors() else emptyMap()
    val back: () -> Unit = {
        focus.clearFocus()
        clearError()
        when (page) {
            "list" -> onNavigate("오늘")
            "edit" -> if (draftJson != initialJson) discard = true else page = editorReturn
            "detail" -> page = "list"
            else -> page = "list"
        }
    }
    fun edit(value: ExerciseDraft, origin: String) {
        clearError(); submitted = false; validationAttempt = 0; stateHolder.removeState("edit"); editorReturn = origin
        draftJson = value.toJson().toString(); initialJson = draftJson; page = "edit"
    }
    BackHandler { back() }
    LaunchedEffect(exercises, page, selectedId) {
        if (page == "detail" && selected == null) page = "list"
    }
    Scaffold(
        modifier = Modifier.fillMaxSize().imePadding(), containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Row(Modifier.fillMaxWidth().statusBarsPadding().windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .padding(horizontal = ScreenPadding, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                if (page == "list") {
                    Text("MY FIT LOG", style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    IconButton(onClick = { onNavigate("설정") }) { Icon(Icons.Default.Settings, "설정 열기") }
                } else {
                    TextButton(onClick = back, contentPadding = PaddingValues(0.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(when (page) { "catalog" -> "내 운동"; "edit" -> if (draft.id.isBlank()) "운동 등록" else "운동 수정"; else -> "내 운동" })
                    }
                    if (page == "detail") TextButton(onClick = { delete = true }) {
                        Text("삭제", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        bottomBar = {
            when (page) {
                "list" -> FitNavigation("운동", onNavigate)
                "detail", "edit" -> Surface(color = MaterialTheme.colorScheme.surface) {
                    Column(Modifier.navigationBarsPadding().windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                        .padding(horizontal = ScreenPadding, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                        if (submitted && errors.isNotEmpty()) Text("입력값을 확인해 주세요. 오류가 있는 항목 아래에 표시됩니다.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        Button(onClick = {
                            focus.clearFocus()
                            if (page == "detail") selected?.let { edit(it, "detail") }
                            else {
                                submitted = true; validationAttempt++
                                if (draft.errors().isEmpty()) onSave(draft)?.let { id ->
                                    selectedId = id; page = "detail"; submitted = false
                                }
                            }
                        }, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp), shape = RoundedCornerShape(14.dp)) {
                            Text(if (page == "detail") "목표·메모 수정" else "운동 저장", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (page == "list") ExtendedFloatingActionButton(modifier = Modifier.testTag("add_exercise").semantics { contentDescription = "운동 추가" }, onClick = { clearError(); page = "catalog" },
                icon = { Icon(Icons.Default.Add, null) }, text = { Text("운동 추가") },
                containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
        },
    ) { padding ->
        val modifier = Modifier.padding(padding).consumeWindowInsets(padding)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
        stateHolder.SaveableStateProvider(page) {
            when (page) {
                "list" -> ExerciseListScreen(exercises, modifier,
                    onOpen = { selectedId = it.id; clearError(); page = "detail" }, onAdd = { page = "catalog" })
                "catalog" -> ExerciseCatalogScreen(exercises, modifier.navigationBarsPadding(),
                    onCustom = { edit(ExerciseDraft(), "catalog") }, onPick = { edit(it.draft(), "catalog") })
                "edit" -> ExerciseEditorScreen(draft, errors, modifier, validationAttempt, onEdit = {
                    draftJson = it.toJson().toString(); clearError()
                })
                "detail" -> selected?.let { ExerciseDetailScreen(it, modifier) }
            }
        }
    }

    if (discard) AlertDialog(onDismissRequest = { discard = false }, title = { Text("변경 내용을 취소할까요?") },
        text = { Text("저장하지 않은 입력 내용은 사라집니다.") },
        confirmButton = { TextButton(onClick = { discard = false; page = editorReturn; draftJson = initialJson; clearError() }) { Text("변경 취소") } },
        dismissButton = { TextButton(onClick = { discard = false }) { Text("계속 작성") } })
    if (delete && selected != null) AlertDialog(onDismissRequest = { delete = false }, title = { Text("${selected.name} 삭제") },
        text = { Text("내 운동과 오늘 이후의 루틴에서 제외합니다. 이미 수행한 기록은 유지됩니다.") },
        confirmButton = { TextButton(onClick = {
            if (onDelete(selected.id)) { delete = false; page = "list"; selectedId = "" }
        }) { Text("운동 삭제", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = { delete = false; clearError() }) { Text("취소") } })
}
