package com.keyo

import android.app.Application
import com.keyo.tools.ToolRegistry

class KeyoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ToolRegistry.init()
        // Prefer a user-supplied API key (from settings); fall back to the build-time default.
        val savedKey = KeyboardPrefs.getApiKey(this)
        if (savedKey.isNotBlank()) GroqApi.apiKey = savedKey
        // Here and not only in the keyboard service: Settings can run in a process where the
        // keyboard never started, and its "Test" button would then use the default model.
        GroqApi.model = KeyboardPrefs.getTextModel(this)
        GroqApi.aiModel = KeyboardPrefs.getAiModel(this)
    }
}
