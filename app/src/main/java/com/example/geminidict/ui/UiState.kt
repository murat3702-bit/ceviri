package com.example.geminidict.ui

sealed interface UiState {
    data object Idle : UiState
    data object Loading : UiState
    data class Success(val text: String) : UiState
    data class Error(val message: String) : UiState
}
