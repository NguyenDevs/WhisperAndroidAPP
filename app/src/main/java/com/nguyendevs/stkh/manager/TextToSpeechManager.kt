package com.nguyendevs.stkh.manager

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.widget.EditText
import android.widget.TextView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nguyendevs.stkh.util.showToast
import java.util.Locale

class TextToSpeechManager(
    private val context: Context,
    private val txtResult: EditText
) : TextToSpeech.OnInitListener, ITextToSpeech {

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

    var onSpeakStart: (() -> Unit)? = null
    var onSpeakDone: (() -> Unit)? = null

    companion object {
        private val LOCALE_MAP: Map<String, Locale> = mapOf(
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
    }

    init {
        val prefs: SharedPreferences = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        speechSpeed = prefs.getInt("speechSpeed", 50)
        speechPitch = prefs.getInt("speechPitch", 50)
        textToSpeech = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            setTtsLanguage()
            textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String) =
                    runOnUi { onSpeakStart?.invoke() }

                override fun onDone(utteranceId: String) = runOnUi {
                    txtResult.setText(currentText)
                    makeReadOnly()
                    isSpeaking = false; isPaused = false
                    lastSpokenIndex = 0; resumeIndex = 0
                    onSpeakDone?.invoke()
                }

                @Deprecated("Deprecated in API 21")
                override fun onError(utteranceId: String) =
                    runOnUi { context.showToast("Lỗi khi đọc văn bản!") }

                override fun onRangeStart(utteranceId: String, start: Int, end: Int, frame: Int) {
                    val text = txtResult.text.toString()
                    if (text != currentText) { lastSpokenIndex = 0; resumeIndex = 0; currentText = text }
                    val aStart = resumeIndex + start
                    val aEnd = resumeIndex + end
                    if (aStart >= 0 && aEnd <= text.length) {
                        lastSpokenIndex = aStart
                        runOnUi { highlightWord(aStart, aEnd) }
                    }
                }
            })
        } else {
            context.showToast("Khởi tạo TextToSpeech thất bại!")
        }
    }

    /** Highlight từ đang đọc trong EditText. */
    private fun highlightWord(start: Int, end: Int) {
        val spannable = SpannableString(txtResult.text)
        spannable.getSpans(0, spannable.length, ForegroundColorSpan::class.java)
            .forEach { spannable.removeSpan(it) }
        spannable.setSpan(ForegroundColorSpan(Color.parseColor("#C8C8E8")),
            0, spannable.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(ForegroundColorSpan(Color.parseColor("#00D4AA")),
            start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        txtResult.setText(spannable, TextView.BufferType.SPANNABLE)
        txtResult.setSelection(end)
    }

    override fun setLanguage(langCode: String) {
        isLanguageManuallySet = true
        val locale = LOCALE_MAP[langCode] ?: run { context.showToast("Ngôn ngữ không hỗ trợ: $langCode"); return }
        applyLocale(locale)
    }

    fun setTtsLanguage(): Boolean {
        if (detectedLanguage == "Auto") return true
        val locale = LOCALE_MAP[detectedLanguage] ?: run {
            showLangNotSupportedDialog("Ngôn ngữ '$detectedLanguage' không được hỗ trợ.")
            return false
        }
        return applyLocale(locale)
    }

    private fun applyLocale(locale: Locale): Boolean {
        val result = textToSpeech?.setLanguage(locale) ?: return false
        return if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            showLangNotSupportedDialog("Ngôn ngữ '${locale.displayLanguage}' thiếu dữ liệu giọng nói.")
            false
        } else {
            textToSpeech?.setSpeechRate(speechSpeed / 50f)
            textToSpeech?.setPitch(speechPitch / 50f)
            true
        }
    }

    private fun showLangNotSupportedDialog(message: String) {
        runOnUi {
            MaterialAlertDialogBuilder(context)
                .setTitle("Ngôn ngữ không khả dụng")
                .setMessage("$message\nBạn có muốn tải dữ liệu giọng nói hoặc chuyển về tiếng Việt?")
                .setPositiveButton("Tải dữ liệu") { _, _ ->
                    try { context.startActivity(Intent("com.android.settings.TTS_SETTINGS")) }
                    catch (e: Exception) { context.showToast("Không thể mở cài đặt TTS.") }
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

    override fun speakText(text: String) {
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
            lastSpokenIndex = 0; resumeIndex = 0
        }
        if (!isLanguageManuallySet && !setTtsLanguage()) return
        textToSpeech?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, System.currentTimeMillis().toString())
        isSpeaking = true; isPaused = false
    }

    override fun pauseText() {
        if (textToSpeech != null && isSpeaking && !isPaused) {
            textToSpeech?.stop()
            isPaused = true; isSpeaking = false
            runOnUi {
                val text = txtResult.text.toString()
                if (lastSpokenIndex < text.length) {
                    var wordEnd = lastSpokenIndex
                    while (wordEnd < text.length && !text[wordEnd].isWhitespace()) wordEnd++
                    highlightWord(lastSpokenIndex, wordEnd)
                }
            }
        }
    }

    override fun setDetectedLanguage(language: String) {
        detectedLanguage = language
        setTtsLanguage()
    }

    override fun updateTtsSettings(speed: Int, pitch: Int) {
        speechSpeed = speed; speechPitch = pitch
        setTtsLanguage()
    }

    override fun destroy() {
        textToSpeech?.apply { stop(); shutdown() }
        textToSpeech = null
    }

    private fun makeReadOnly() {
        txtResult.apply { isFocusable = false; isCursorVisible = false; isLongClickable = false; keyListener = null }
    }

    private fun runOnUi(block: () -> Unit) =
        (context as? android.app.Activity)?.runOnUiThread(block) ?: Unit
}
