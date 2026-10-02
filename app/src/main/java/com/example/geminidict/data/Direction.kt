package com.example.geminidict.data

enum class Direction(val label: String, val instruction: String) {
    AUTO("Otomatik", "Metin İngilizceyse Türkçeye, Türkçeyse İngilizceye çevrilsin."),
    EN_TR("EN → TR", "Metin İngilizce; Türkçeye çevir."),
    TR_EN("TR → EN", "Metin Türkçe; İngilizceye çevir.")
}
