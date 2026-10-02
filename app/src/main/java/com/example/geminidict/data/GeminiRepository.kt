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
        generationConfig = generationConfig { 
            temperature = 0.0f // Modelin yaratıcılığını sıfırlayarak en hızlı yanıt moduna alır
        },
        // Modelin gereksiz akıl yürütmesini ve hantallığını önleyen ana hız talimatı:
        systemInstruction = content {
            text("Sen ultra hızlı çalışan, doğrudan ve net bir sözlük ve çeviri motorusun. " +
                 "Sana verilen komutları yerine getirirken ekstra hiçbir açıklama, yorum, " +
                 "selamlaşma veya dipnot ekleme. Sadece senden istenen formattaki çeviri çıktısını en hızlı şekilde döndür.")
        }
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

    // YENİ EKLENEN VE OTOMATİK TEKRAR DENEYEN HIZLI DÖNGÜ FONKSİYONU:
    private suspend fun call(block: suspend () -> GenerateContentResponse): Result<String> {
        if (apiKey.isBlank()) return Result.failure(AppError("API anahtarı tanımlı değil."))
        
        var attempts = 0
        val maxAttempts = 3 // Yoğunluk veya hata anında arka planda en fazla 3 kez şansını deneyecek
        
        while (attempts < maxAttempts) {
            try {
                val text = block().text?.trim()
                if (text.isNullOrEmpty()) return Result.failure(AppError("Sonuç boş döndü."))
                return Result.success(text)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                attempts++
                val msg = e.localizedMessage ?: ""
                val isTransientError = msg.contains("MissingFieldException") || msg.contains("503") || msg.contains("UNAVAILABLE")
                
                // Eğer geçici bir 503/yoğunluk hatasıysa ve deneme sınırına ulaşmadıysak milisaniyeler içinde hemen tekrar dene
                if (isTransientError && attempts < maxAttempts) {
                    kotlinx.coroutines.delay(400L * attempts) // Çok kısa bekleyip döngüyü tekrar çalıştırır
                    continue
                }
                
                // Tüm denemeler tükendiyse veya internet tamamen yoksa hata arayüze paslanır
                return if (e.isNetwork()) {
                    Result.failure(AppError("İnternet bağlantısı yok veya ağ hatası."))
                } else if (isTransientError) {
                    Result.failure(AppError("Sunucu şu an çok yoğun. Lütfen birkaç dakika sonra tekrar deneyin."))
                } else {
                    Result.failure(AppError("API hatası: ${e.localizedMessage ?: "bilinmiyor"}"))
                }
            }
        }
        return Result.failure(AppError("Sunucu yanıt vermedi."))
    }

    // ALT KISIMDA KALAN VE KORUNAN ORİJİNAL KODLARINIZ:
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
