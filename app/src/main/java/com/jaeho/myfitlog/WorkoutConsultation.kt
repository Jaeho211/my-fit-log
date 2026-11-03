package com.jaeho.myfitlog

/** Uses recorded snapshots, including exercises that have since been deleted. */
internal fun workoutConsultationText(days: List<HistoryDay>): String = buildString {
    appendLine("# 운동 기록 상담")
    appendLine("아래 실제 운동 기록을 바탕으로 운동량과 무게·횟수의 변화, 회복과 다음 운동에서 고려할 점을 상담해 주세요. 기록에 없는 정보는 추측하지 말고 필요한 내용을 질문해 주세요.")
    appendLine()
    val ordered = days.filter { it.setCount > 0 }.sortedBy { it.date }
    if (ordered.isEmpty()) {
        append("운동 기록이 없습니다.")
        return@buildString
    }
    appendLine("기록 범위: ${ordered.first().date} ~ ${ordered.last().date}")
    appendLine("운동한 날: ${ordered.size}일 / 총 ${ordered.sumOf { it.setCount }}세트")
    appendLine("무게는 kg 단위이며 덤벨은 한 개 기준입니다. 0 kg은 기록값이며, 미기록과 구분합니다. 세트 사이 휴식 시간은 이 기록에 포함되어 있지 않습니다.")
    ordered.forEach { day ->
        appendLine()
        appendLine("## ${day.date} · ${day.setCount}세트")
        day.groups.sortedBy { it.records.first().time }.forEach { group ->
            appendLine("### ${group.name} / ${group.place} / ${group.records.size}세트")
            // Merge consecutive equal sets while preserving changes in workout order.
            val runs = mutableListOf<MutableList<HistoryRecord>>()
            group.records.forEach { record ->
                val previous = runs.lastOrNull()?.last()
                if (previous != null && previous.kg.trim() == record.kg.trim() &&
                    previous.reps.trim() == record.reps.trim()) runs.last().add(record)
                else runs.add(mutableListOf(record))
            }
            runs.forEach { run ->
                val record = run.first()
                val weight = record.kg.trim().takeIf { it.isNotBlank() }?.let { "$it kg" } ?: "무게 미기록"
                val reps = record.reps.trim().takeIf { it.isNotBlank() }?.let { "${it}회" } ?: "횟수 미기록"
                appendLine("- $weight × $reps × ${run.size}세트")
            }
            val memos = group.records.map { it.memo.trim() }.filter { it.isNotBlank() }.distinct()
            memos.forEach { memo ->
                appendLine("  메모:")
                memo.lineSequence().forEach { appendLine("    $it") }
            }
        }
    }
}.trimEnd()
