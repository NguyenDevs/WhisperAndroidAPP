package com.nguyendevs.stkh.manager

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.widget.EditText
import android.widget.TextView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nguyendevs.stkh.util.showToast
import java.util.Locale

/**
 * TextToSpeechManager - Quản lý TTS với highlight từ khi đọc.
 * Migration: Java → Kotlin. Loại bỏ cast (MainActivity) context.
 * Dùng callback lambdas thay thế.
 */
class TextToSpeechManager(
    private val context: Context,
    private val txtResult: EditText
) : TextToSpeech.OnInitListener {

    private var textToSpeech: TextToSpeech? = null
    private var detectedLanguage = "Vietnamese"
    private var currentText = ""
    private var lastSpokenIndex = 0
    private var resumeIndex = 0
    private var isSpeaking = false
    private var isPaused = false
    private var isLanguageManuallySet = false

    private var speechSpeed: Int
    private var speechPitch: Int

    // Callbacks to notify MainActivity UI
    var onSpeakStart: (() -> Unit)? = null
    var onSpeakDone: (() -> Unit)? = null

    companion object {
        private val languageToLocaleMap: Map<String, Locale> = mapOf(
            "Vietnamese" to Locale("vi", "VN"),
            "English" to Locale.US,
            "Japanese" to Locale.JAPAN,
            "Russian" to Locale("ru", "RU"),
            "French" to Locale.FRANCE,
            "Spanish" to Locale("es", "ES"),
            "German" to Locale("de", "DE"),
            "Chinese" to Locale.SIMPLIFIED_CHINESE,
            "Korean" to Locale("ko", "KR"),
            "Italian" to Locale.ITALY
        )

        private val languageToCodeMap: Map<String, String> = mapOf(
            "Vietnamese" to "vi-VN",
            "English" to "en-US",
            "Japanese" to "ja-JP",
            "Russian" to "ru-RU",
            "French" to "fr-FR",
            "Spanish" to "es-ES",
            "German" to "de-DE",
            "Chinese" to "zh-CN",
            "Korean" to "ko-KR",
            "Italian" to "it-IT"
        )
    }

    init {
        val prefs: SharedPreferences = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        speechSpeed = prefs.getInt("speechSpeed", 50)
        speechPitch = prefs.getInt("speechPitch", 50)
        textToSpeech = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            setTextToSpeechLanguage()
            textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String) {
                    (context as? android.app.Activity)?.runOnUiThread {
                        onSpeakStart?.invoke()
                    }
                }

                override fun onDone(utteranceId: String) {
                    (context as? android.app.Activity)?.runOnUiThread {
                        txtResult.setText(currentText)
                        makeReadOnly()
                        isSpeaking = false
                        isPaused = false
                        lastSpokenIndex = 0
                        resumeIndex = 0
                        onSpeakDone?.invoke()
                    }
                }

                @Deprecated("Deprecated in API 21")
                override fun onError(utteranceId: String) {
                    (context as? android.app.Activity)?.runOnUiThread {
                        context.showToast("Lỗi khi đọc văn bản!")
                    }
                }

                override fun onRangeStart(utteranceId: String, start: Int, end: Int, frame: Int) {
                    val text = txtResult.text.toString()
                    if (text != currentText) {
                        lastSpokenIndex = 0
                        resumeIndex = 0
                        currentText = text
                    }
                    val adjustedStart = resumeIndex + start
                    val adjustedEnd = resumeIndex + end
                    if (adjustedStart >= 0 && adjustedEnd <= text.length) {
                        lastSpokenIndex = adjustedStart
                        (context as? android.app.Activity)?.runOnUiThread {
                            val editable: Editable = txtResult.text
                            val spannable = SpannableString(editable)
                            // Xóa spans cũ
                            spannable.getSpans(0, spannable.length, ForegroundColorSpan::class.java)
                                .forEach { spannable.removeSpan(it) }
                            // Tô toàn bộ màu on-surface
                            spannable.setSpan(
                                ForegroundColorSpan(Color.parseColor("#C8C8E8")),
                                0, spannable.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                            // Tô từ đang đọc màu accent (#00D4AA)
                            spannable.setSpan(
                                ForegroundColorSpan(Color.parseColor("#00D4AA")),
                                adjustedStart, adjustedEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                            txtResult.setText(spannable, TextView.BufferType.SPANNABLE)
                            txtResult.setSelection(adjustedEnd)
                        }
                    }
                }
            })
        } else {
            context.showToast("Khởi tạo TextToSpeech thất bại!")
        }
    }

    fun setLanguage(langCode: String) {
        isLanguageManuallySet = true
        val locale = languageToLocaleMap[langCode] ?: run {
            context.showToast("Ngôn ngữ không hỗ trợ: $langCode")
            return
        }
        applyLocale(locale)
    }

    fun setTextToSpeechLanguage(): Boolean {
        if (detectedLanguage == "Auto") return true
        val locale = languageToLocaleMap[detectedLanguage] ?: run {
            showLanguageNotSupportedDialog("Ngôn ngữ '$detectedLanguage' không được hỗ trợ.")
            return false
        }
        return applyLocale(locale)
    }

    private fun applyLocale(locale: Locale): Boolean {
        val result = textToSpeech?.setLanguage(locale) ?: return false
        return if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            showLanguageNotSupportedDialog("Ngôn ngữ '${locale.displayLanguage}' thiếu dữ liệu giọng nói.")
            false
        } else {
            val speed = speechSpeed / 50f
            val pitch = speechPitch / 50f
            textToSpeech?.setSpeechRate(speed)
            textToSpeech?.setPitch(pitch)
            true
        }
    }

    private fun showLanguageNotSupportedDialog(message: String) {
        (context as? android.app.Activity)?.runOnUiThread {
            MaterialAlertDialogBuilder(context)
                .setTitle("Ngôn ngữ không khả dụng")
                .setMessage("$message\nBạn có muốn tải dữ liệu giọng nói hoặc chuyển về tiếng Việt?")
                .setPositiveButton("Tải dữ liệu") { dialog, _ ->
                    try {
                        val intent = Intent("com.android.settings.TTS_SETTINGS")
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        context.showToast("Không thể mở cài đặt TTS.")
                    }
                    textToSpeech?.setLanguage(Locale("vi", "VN"))
                    detectedLanguage = "Vietnamese"
                    context.showToast("Đã chuyển tạm về tiếng Việt.")
                }
                .setNegativeButton("Chuyển về tiếng Việt") { dialog, _ ->
                    textToSpeech?.setLanguage(Locale("vi", "VN"))
                    detectedLanguage = "Vietnamese"
                    context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                        .edit().putString("selectedLanguage", "Vietnamese").apply()
                    context.showToast("Đã chuyển về tiếng Việt.")
                    dialog.dismiss()
                }
                .setCancelable(false)
                .show()
        }
    }

    fun speakText(text: String) {
        if (textToSpeech == null || text.isEmpty()) return
        makeReadOnly()
        currentText = text
        txtResult.setText(text)

        val textToSpeak: String
        if (isPaused && lastSpokenIndex > 0 && lastSpokenIndex < text.length) {
            textToSpeak = text.substring(lastSpokenIndex)
            resumeIndex = lastSpokenIndex
        } else {
            textToSpeak = text
            lastSpokenIndex = 0
            resumeIndex = 0
        }

        if (!isLanguageManuallySet) {
            if (!setTextToSpeechLanguage()) return
        }

        val utteranceId = System.currentTimeMillis().toString()
        textToSpeech?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        isSpeaking = true
        isPaused = false
    }

    fun pauseText() {
        if (textToSpeech != null && isSpeaking && !isPaused) {
            textToSpeech?.stop()
            isPaused = true
            isSpeaking = false
            (context as? android.app.Activity)?.runOnUiThread {
                val text = txtResult.text.toString()
                if (lastSpokenIndex < text.length) {
                    var wordEnd = lastSpokenIndex
                    while (wordEnd < text.length && !text[wordEnd].isWhitespace()) wordEnd++
                    val spannable = SpannableString(text).apply {
                        setSpan(
                            ForegroundColorSpan(Color.parseColor("#C8C8E8")),
                            0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        setSpan(
                            ForegroundColorSpan(Color.parseColor("#00D4AA")),
                            lastSpokenIndex, wordEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }
                    txtResult.setText(spannable)
                }
            }
        }
    }

    fun setDetectedLanguage(language: String) {
        detectedLanguage = language
        setTextToSpeechLanguage()
    }

    fun updateTtsSettings(speed: Int, pitch: Int) {
        speechSpeed = speed
        speechPitch = pitch
        setTextToSpeechLanguage()
    }

    private fun makeReadOnly() {
        txtResult.apply {
            isFocusable = false
            isCursorVisible = false
            isLongClickable = false
            keyListener = null
        }
    }

    fun destroy() {
        textToSpeech?.apply {
            stop()
            shutdown()
        }
        textToSpeech = null
    }
}
