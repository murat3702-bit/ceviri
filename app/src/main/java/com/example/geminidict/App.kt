package com.example.geminidict

import android.app.Application
import androidx.room.Room
import com.example.geminidict.data.GeminiRepository
import com.example.geminidict.data.OcrManager
import com.example.geminidict.data.db.AppDb

class App : Application() {
    val repo by lazy { GeminiRepository(BuildConfig.GEMINI_API_KEY, BuildConfig.GEMINI_MODEL) }
    val ocr by lazy { OcrManager(this) }
    val db by lazy { Room.databaseBuilder(this, AppDb::class.java, "dict.db").build() }
}
