package com.jaeho.myfitlog

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val model: FitViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ReminderScheduler.schedule(this)
        if (savedInstanceState == null && intent.hasExtra("section")) model.navigate(intent.getStringExtra("section") ?: "오늘")
        setContent {
            FitTheme {
                FitApp(model) { destination ->
                    startActivity(Intent(this, LegacyActivity::class.java).putExtra("tab", destination))
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                model.refresh()
                while (true) { delay(30_000); model.refreshDate() }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        model.refresh()
        model.navigate(intent.getStringExtra("section") ?: "오늘")
    }
}
