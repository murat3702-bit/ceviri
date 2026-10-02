package com.example.geminidict.data

import android.graphics.Bitmap
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.GenerateContentResponse
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

class AppError(message: String) : Exception(message)

class GeminiRepository(
    private val apiKey: String,
    modelName: String
) {
    private val model = GenerativeModel(
        modelName = modelName,
        apiKey = apiKey,
        generationConfig = generationConfig { temperature = 0.2f }
    )

    suspend fun translateText(input: String, dir: Direction = Direction.AUTO): Result<String> = call {
        model.generateContent(textPrompt(input.trim(), dir))
    }

    suspend fun translateImage(bitmap: Bitmap, dir: Direction = Direction.AUTO): Result<String> = call {
        model.generateContent(content {
            image(bitmap)
            text(imagePrompt(dir))
        })
    }

    private suspend fun call(block: suspend () -> GenerateContentResponse): Result<String> {
        if (apiKey.isBlank()) return Result.failure(AppError("API anahtarı tanımlı değil."))
        return try {
            val text = block().text?.trim()
            if (text.isNullOrEmpty()) Result.failure(AppError("Sonuç boş döndü."))
            else Result.success(text)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (e.isNetwork()) Result.failure(AppError("İnternet bağlantısı yok veya ağ hatası."))
            else Result.failure(AppError("API hatası: ${e.localizedMessage ?: "bilinmiyor"}"))
        }
    }

    private fun Throwable.isNetwork(): Boolean =
        generateSequence(this) { it.cause }.any { it is IOException }

    private fun textPrompt(t: String, dir: Direction): String {
        val isWord = t.split(Regex("\\s+")).size == 1
        return if (isWord) """
            "$t" kelimesi için sözlük girdisi hazırla. ${dir.instruction}
            Şu formatta, markdown kullanmadan yanıtla:
            Karşılık: ...
            Tür: (isim/fiil/sıfat...)
            Okunuş: ... (İngilizce olan kelimenin okunuşu)
            Örnek 1: ... (çevirisi)
            Örnek 2: ... (çevirisi)
        """.trimIndent() else """
            ${dir.instruction}
            Doğal ve akıcı çeviri ver. Sadece çeviriyi yaz, açıklama ekleme.

            $t
        """.trimIndent()
    }

    private fun imagePrompt(dir: Direction): String =
        "Görseldeki yazıları oku. ${dir.instruction} Doğal şekilde çevir. " +
        "Yazı yoksa sadece NO_TEXT yaz. Yalnızca çeviriyi ver."
}
