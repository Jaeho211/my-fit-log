package com.jaeho.myfitlog

import android.content.Context
import org.json.JSONObject

/** Shared by Compose screens and the remaining Java screens. Storage and backup stay version 1. */
class FitRepository(context: Context) {
    private val app = context.applicationContext
    private val preferences = app.getSharedPreferences("fit", Context.MODE_PRIVATE)

    fun read(): JSONObject = JSONObject(preferences.getString("db", FitData.EMPTY) ?: FitData.EMPTY)

    fun write(json: JSONObject) {
        preferences.edit().putString("db", json.toString()).apply()
        ReminderReceiver.schedule(app)
    }
}
