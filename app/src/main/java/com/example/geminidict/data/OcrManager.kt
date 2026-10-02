package com.example.geminidict.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.core.content.FileProvider
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import java.io.File

class OcrManager(private val context: Context) {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    /** A yöntemi: cihaz üstünde, çevrimdışı OCR. */
    suspend fun readText(bitmap: Bitmap): String =
        recognizer.process(InputImage.fromBitmap(bitmap, 0)).await().text

    fun newPhotoUri(): Uri {
        val file = File(context.cacheDir, "photo_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    /** EXIF yönünü düzeltir ve büyük görseli küçültür. */
    fun loadBitmap(uri: Uri, maxSide: Int = 1600): Bitmap =
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { d, info, _ ->
            d.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val s = maxOf(info.size.width, info.size.height)
            if (s > maxSide) {
                val r = maxSide.toFloat() / s
                d.setTargetSize((info.size.width * r).toInt(), (info.size.height * r).toInt())
            }
        }
}
