package com.example.geminidict.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.geminidict.App
import com.example.geminidict.data.AppError
import com.example.geminidict.data.Direction
import com.example.geminidict.data.GeminiRepository
import com.example.geminidict.data.OcrManager
import com.example.geminidict.data.db.HistoryDao
import com.example.geminidict.data.db.HistoryItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TranslateViewModel(
    private val repo: GeminiRepository,
    private val ocr: OcrManager,
    private val dao: HistoryDao
) : ViewModel() {

    var state by mutableStateOf<UiState>(UiState.Idle)
        private set
    var selected by mutableStateOf<HistoryItem?>(null)
        private set
    var useOfflineOcr by mutableStateOf(false)
    var direction by mutableStateOf(Direction.AUTO)

    val history: StateFlow<List<HistoryItem>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun translateText(input: String) {
        if (input.isBlank()) {
            state = UiState.Error("Lütfen bir metin gir.")
            return
        }
        launchCall(input.trim()) { repo.translateText(input, direction) }
    }

    fun translateImage(uri: Uri?) {
        if (uri == null) {
            state = UiState.Error("Görsel seçilmedi.")
            return
        }
        launchCall("📷 Görsel") {
            try {
                val bmp = withContext(Dispatchers.IO) { ocr.loadBitmap(uri) }
                if (useOfflineOcr) {
                    val text = ocr.readText(bmp)
                    if (text.isBlank()) Result.failure<String>(AppError("Görselde yazı bulunamadı."))
                    else repo.translateText(text, direction)
                } else {
                    repo.translateImage(bmp, direction).mapCatching {
                        if (it.contains("NO_TEXT")) throw AppError("Görselde yazı bulunamadı.") else it
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure<String>(AppError("Görsel okunamadı."))
            }
        }
    }

    private fun launchCall(label: String, block: suspend () -> Result<String>) {
        state = UiState.Loading
        viewModelScope.launch {
            block().fold(
                onSuccess = {
                    state = UiState.Success(it)
                    dao.insert(HistoryItem(query = label, result = it))
                },
                onFailure = { state = UiState.Error(it.message ?: "Bir hata oluştu.") }
            )
        }
    }

    fun select(item: HistoryItem?) { selected = item }
    fun clearHistory() { viewModelScope.launch { dao.clear() } }
    fun consumeError() { if (state is UiState.Error) state = UiState.Idle }
}

@Composable
fun rememberTranslateVm(): TranslateViewModel {
    val app = LocalContext.current.applicationContext as App
    return viewModel(factory = viewModelFactory {
        initializer { TranslateViewModel(app.repo, app.ocr, app.db.historyDao()) }
    })
}
