package com.example.geminidict.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.example.geminidict.util.rememberTts
import java.util.Locale

@Composable
fun StateContent(state: UiState) {
    when (state) {
        UiState.Loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
        is UiState.Success -> ResultCard(state.text)
        is UiState.Error -> Text(state.message, color = MaterialTheme.colorScheme.error)
        UiState.Idle -> Unit
    }
}

@Composable
fun ResultCard(text: String) {
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val tts = rememberTts()

    fun toast(msg: String) = Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            SelectionContainer { Text(text) }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = {
                    clipboard.setText(AnnotatedString(text))
                    toast("Kopyalandı")
                }) { Text("Kopyala") }
                TextButton(onClick = {
                    if (!tts.speak(text, Locale("tr", "TR"))) toast("Türkçe ses verisi yok")
                }) { Text("Dinle TR") }
                TextButton(onClick = {
                    if (!tts.speak(text, Locale.ENGLISH)) toast("İngilizce ses verisi yok")
                }) { Text("Dinle EN") }
            }
        }
    }
}
