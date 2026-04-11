package com.nguyendevs.stkh.manager

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.widget.EditText
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.nguyendevs.stkh.util.showToast

class TranslateManager(
    private val context: Context,
    private val txtResult: EditText
) : ITranslator {

    private var translator: Translator? = null
    private var originalTextSnapshot: String? = null

    var onTranslated: ((text: String, targetLang: String) -> Unit)? = null

    companion object {
        private val languageDisplayMap: Map<String, String> = mapOf(
            "Vietnamese" to "Tiếng Việt",
            "English" to "Tiếng Anh",
            "Japanese" to "Tiếng Nhật",
            "Russian" to "Tiếng Nga",
            "French" to "Tiếng Pháp",
            "Spanish" to "Tiếng Tây Ban Nha",
            "German" to "Tiếng Đức",
            "Chinese" to "Tiếng Trung",
            "Korean" to "Tiếng Hàn",
            "Italian" to "Tiếng Ý"
        )

        private val languageToMLKitMap: Map<String, String> = mapOf(
            "Vietnamese" to TranslateLanguage.VIETNAMESE,
            "English" to TranslateLanguage.ENGLISH,
            "Japanese" to TranslateLanguage.JAPANESE,
            "Russian" to TranslateLanguage.RUSSIAN,
            "French" to TranslateLanguage.FRENCH,
            "Spanish" to TranslateLanguage.SPANISH,
            "German" to TranslateLanguage.GERMAN,
            "Chinese" to TranslateLanguage.CHINESE,
            "Korean" to TranslateLanguage.KOREAN,
            "Italian" to TranslateLanguage.ITALIAN
        )
    }

    override fun showTargetLanguagePopup() {
        val languages = languageDisplayMap.keys.toTypedArray()
        val displayNames = languageDisplayMap.values.toTypedArray()

        MaterialAlertDialogBuilder(context)
            .setTitle("Dịch sang ngôn ngữ nào?")
            .setItems(displayNames) { _, which ->
                val currentText = txtResult.text.toString().trim()
                if (currentText.isEmpty()) {
                    context.showToast("Không có nội dung để dịch.")
                    return@setItems
                }
                if (originalTextSnapshot == null) {
                    originalTextSnapshot = currentText
                }
                detectAndTranslate(originalTextSnapshot!!, languages[which])
            }
            .show()
    }

    override fun resetOriginalTextSnapshot() {
        originalTextSnapshot = null
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            cm.activeNetworkInfo?.isConnected == true
        }
    }

    private fun detectAndTranslate(text: String, targetLang: String) {
        val identifier = LanguageIdentification.getClient()
        identifier.identifyLanguage(text)
            .addOnSuccessListener { languageCode ->
                if (languageCode == "und") {
                    context.showToast("Không thể xác định ngôn ngữ.")
                    return@addOnSuccessListener
                }
                val targetMLKitCode = languageToMLKitMap[targetLang]
                    ?: TranslateLanguage.VIETNAMESE
                translateText(text, languageCode, targetMLKitCode, targetLang)
            }
            .addOnFailureListener { e ->
                context.showToast("Lỗi xác định ngôn ngữ: ${e.message}")
            }
    }

    private fun translateText(
        text: String,
        sourceLang: String,
        targetLangCode: String,
        targetLangName: String
    ) {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(sourceLang)
            .setTargetLanguage(targetLangCode)
            .build()

        translator = Translation.getClient(options)

        if (!isNetworkAvailable()) {
            context.showToast("Không có kết nối mạng. Đang thử dịch ngoại tuyến.")
        }

        translator!!.downloadModelIfNeeded()
            .addOnSuccessListener {
                translator!!.translate(text)
                    .addOnSuccessListener { translatedText ->
                        txtResult.setText(translatedText)
                        onTranslated?.invoke(translatedText, targetLangName)
                    }
                    .addOnFailureListener { e ->
                        context.showToast("Dịch thất bại: ${e.message}", android.widget.Toast.LENGTH_LONG)
                    }
            }
            .addOnFailureListener { e ->
                context.showToast("Không thể tải mô hình dịch: ${e.message}", android.widget.Toast.LENGTH_LONG)
            }
    }

    override fun destroy() {
        translator?.close()
        translator = null
    }
}
