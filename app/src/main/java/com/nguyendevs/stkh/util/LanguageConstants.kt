package com.nguyendevs.stkh.util

object LanguageConstants {
    val DISPLAY_LANGUAGES = arrayOf(
        "Tự động", "Tiếng Việt", "English", "Japanese",
        "Russian", "French", "Spanish", "German", "Chinese", "Korean", "Italian"
    )
    val WHISPER_LANGUAGES = arrayOf(
        "Auto", "Vietnamese", "English", "Japanese",
        "Russian", "French", "Spanish", "German", "Chinese", "Korean", "Italian"
    )

    /**
     * Chuyển đổi từ mã Whisper (e.g. "Vietnamese") sang tên hiển thị (e.g. "Tiếng Việt").
     */
    fun getDisplayName(whisperLang: String?): String {
        val index = WHISPER_LANGUAGES.indexOf(whisperLang ?: "Vietnamese")
        return if (index != -1) DISPLAY_LANGUAGES[index] else "Tiếng Việt"
    }
}
