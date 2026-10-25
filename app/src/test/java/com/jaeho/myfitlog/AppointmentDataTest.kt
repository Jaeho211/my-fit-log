package com.jaeho.myfitlog

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class AppointmentDataTest {
    private val zone = ZoneId.of("Asia/Seoul")
    private val now = Instant.parse("2026-10-25T03:00:00Z").toEpochMilli()
    private fun data() = FitData(JSONObject(FitData.EMPTY), zone)
    private fun draft() = AppointmentDraft(date = "2026-10-27", hour = 19, minute = 30, note = "자세 질문", minutes = "60")
    private fun raw(id: Int, time: Long) = JSONObject().put("id", id).put("time", time).put("note", "기존 예약")

    @Test fun readsVersionOneDefaultsAndSortsUpcomingAndPastWithoutMutatingStorage() {
        val d = data(); val a = d.json.getJSONArray("appointments")
        a.put(raw(1, now + 7200000)); a.put(raw(0, now - 7200000)); a.put(raw(2, now + 3600000))
        val before = d.json.toString()
        assertEquals(listOf(0, 2, 1), d.appointments().map { it.id })
        assertEquals(60, d.appointments().first().minutes)
        assertEquals(now + 3600000, d.summary(now).nextAppointment!!.time)
        assertEquals(before, d.json.toString())
    }

    @Test fun newIdsDoNotCollideWithLegacyPendingIntentIds() {
        val d = data(); val a = d.json.getJSONArray("appointments")
        listOf(0, 1, 2, Int.MAX_VALUE).forEach { a.put(raw(it, now + 3600000)) }
        val id = d.saveAppointment(draft(), now)
        assertEquals(3, id); assertEquals(5, d.appointments().map { it.id }.toSet().size)
        assertEquals(1, d.json.getInt("version"))
    }

    @Test fun editPreservesIdUnknownFieldsAndUnrelatedWorkoutData() {
        val d = data(); val id = d.saveAppointment(draft(), now)
        d.json.getJSONArray("appointments").getJSONObject(0).put("custom", "keep")
        d.json.getJSONArray("logs").put(JSONObject().put("name", "과거 기록"))
        val logs = d.json.getJSONArray("logs").toString()
        val plans = d.json.getJSONArray("plan").toString()
        assertEquals(id, d.saveAppointment(d.appointments().single().draft(zone).copy(note = "수정 메모", minutes = "30"), now))
        assertEquals(1, d.appointments().size)
        assertEquals("keep", d.json.getJSONArray("appointments").getJSONObject(0).getString("custom"))
        assertEquals(logs, d.json.getJSONArray("logs").toString()); assertEquals(plans, d.json.getJSONArray("plan").toString())
        assertEquals("수정 메모", FitData(JSONObject(d.json.toString()), zone).appointments().single().note)
    }

    @Test fun invalidDateTimeAndReminderLeadCannotPartiallySave() {
        val d = data(); d.saveAppointment(draft(), now)
        val before = d.json.toString()
        listOf(draft().copy(date = "2026-02-30"), draft().copy(date = "2026-10-24"),
            draft().copy(hour = 24), draft().copy(minute = 60), draft().copy(minutes = "0"),
            draft().copy(minutes = "1001"), draft().copy(minutes = "1.5"), draft().copy(minutes = "NaN"))
            .forEach { value ->
                assertThrows(IllegalArgumentException::class.java) { d.saveAppointment(value, now) }
                assertEquals(before, d.json.toString())
            }
    }

    @Test fun deleteOnlyChangesTheSelectedAppointmentAndDoesNotResurrectMissingIds() {
        val d = data(); val id = d.saveAppointment(draft(), now)
        val other = d.saveAppointment(draft().copy(date = "2026-10-28"), now)
        d.json.getJSONArray("logs").put(JSONObject().put("memo", "보존"))
        val logs = d.json.getJSONArray("logs").toString()
        d.deleteAppointment(id)
        assertEquals(listOf(other), d.appointments().map { it.id })
        assertEquals(logs, d.json.getJSONArray("logs").toString())
        assertThrows(IllegalArgumentException::class.java) { d.saveAppointment(draft().copy(id = id), now) }
    }

    @Test fun draftRoundTripUsesLocalTimeAndKeepsLegacyZeroId() {
        val d = data(); val value = draft().copy(id = 0)
        d.json.getJSONArray("appointments").put(raw(0, value.time(zone)))
        val local = d.appointments().single().draft(zone)
        assertEquals("2026-10-27", local.date); assertEquals(19, local.hour); assertEquals(30, local.minute)
        assertEquals(local, AppointmentDraft.fromJson(local.toJson()))
        assertEquals(null, AppointmentDraft.fromJson(draft().toJson()).id)
        assertEquals(Instant.parse("2026-10-27T10:30:00Z").toEpochMilli(), value.time(zone))
    }

    @Test fun nonexistentDaylightSavingTimeIsRejectedInsteadOfSilentlyShifted() {
        val ny = ZoneId.of("America/New_York")
        val d = FitData(JSONObject(FitData.EMPTY), ny)
        val value = draft().copy(date = "2027-03-14", hour = 2, minute = 30)
        assertTrue(value.errors(now, ny).containsKey("time"))
        assertThrows(IllegalArgumentException::class.java) { d.saveAppointment(value, now) }
        assertTrue(d.appointments().isEmpty())
    }

    @Test fun remindersOnlyScheduleFutureTriggersAndElapsedLeadStillAllowsSavingTheReservation() {
        val values = listOf(PtAppointment(1, now + 3600000, "", 60), PtAppointment(2, now + 3600000, "", 30),
            PtAppointment(3, now - 1, "", 30), PtAppointment(4, now + 60000, "", 60))
        assertEquals(listOf(PtReminder(2, now + 1800000)), futurePtReminders(values, now))
        val d = data()
        val soon = AppointmentDraft(date = LocalDate.of(2026, 10, 25).toString(), hour = 12, minute = 1, minutes = "60")
        d.saveAppointment(soon, now)
        assertEquals(1, d.appointments().size); assertTrue(futurePtReminders(d.appointments(), now).isEmpty())
    }
}
