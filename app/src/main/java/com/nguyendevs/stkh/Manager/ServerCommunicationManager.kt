package com.nguyendevs.stkh.manager

import android.content.Context
import android.net.Uri
import android.text.SpannableString
import android.text.style.StyleSpan
import android.util.Log
import android.widget.EditText
import android.widget.ProgressBar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nguyendevs.stkh.database.AppDatabase
import com.nguyendevs.stkh.database.HistoryEntity
import com.nguyendevs.stkh.util.gone
import com.nguyendevs.stkh.util.showToast
import com.nguyendevs.stkh.util.visible
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * ServerCommunicationManager - Giao tiếp với server Whisper qua HTTPS.
 * - Dùng Kotlin Coroutines (Dispatchers.IO) thay new Thread()
 * - Dùng Room DAO thay DatabaseHelper trực tiếp
 * - Dùng callback để tránh cast context → MainActivity
 */
class ServerCommunicationManager(
    private val context: Context,
    private val txtResult: EditText,
    private val progressBar: ProgressBar,
    private val db: AppDatabase,
    private val textToSpeechManager: TextToSpeechManager,
    private val lifecycleScope: CoroutineScope
) {

    companion object {
        private const val TAG = "ServerCommunicationManager"
    }

    /** Gọi khi transcription thành công: (text, detectedLanguage) */
    var onTranscriptionSuccess: ((text: String, language: String) -> Unit)? = null
    /** Gọi khi có lỗi (để enable lại mic button) */
    var onTranscriptionError: (() -> Unit)? = null
    /** Gọi để mở file picker (Activity Result API từ MainActivity) */
    var onShowFilePicker: (() -> Unit)? = null

    private val client: OkHttpClient by lazy { buildOkHttpClient() }

    private fun buildOkHttpClient(): OkHttpClient {
        return try {
            val trustManager = object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
            }
            val sslContext = SSLContext.getInstance("TLSv1.2").apply {
                init(null, arrayOf<TrustManager>(trustManager), SecureRandom())
            }
            OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .hostnameVerifier { _, _ -> true }
                .sslSocketFactory(sslContext.socketFactory, trustManager)
                .build()
        } catch (e: Exception) {
            Log.e(TAG, "SSL config error: ${e.message}")
            OkHttpClient.Builder().build()
        }
    }

    fun sendAudioToServer(file: File) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                val serverIP = prefs.getString("serverIP", "")?.trim()
                if (serverIP.isNullOrEmpty()) {
                    withContext(Dispatchers.Main) {
                        context.showToast("Địa chỉ server không hợp lệ!")
                        onTranscriptionError?.invoke()
                    }
                    return@launch
                }

                val language = prefs.getString("selectedLanguage", "Vietnamese") ?: "Vietnamese"
                val model = prefs.getString("selectedModel", "Medium") ?: "Medium"
                val serverUrl = "https://$serverIP:8443/transcribe"
                Log.d(TAG, "Sending to: $serverUrl")

                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("language", if (language == "Auto") "auto" else language)
                    .addFormDataPart("model_name", model)
                    .addFormDataPart("file", file.name, file.asRequestBody("audio/*".toMediaType()))
                    .build()

                val request = Request.Builder().url(serverUrl).post(requestBody).build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("Server error: ${response.code}")
                    val body = response.body?.string() ?: throw IOException("Empty response body")
                    val json = JSONObject(body)
                    val transcribedText = json.getString("text")
                    val detectedLanguage = json.getString("language")

                    // Lưu vào Room DB trên IO thread
                    db.historyDao().insert(
                        HistoryEntity(
                            content = transcribedText,
                            date = System.currentTimeMillis(),
                            lang = detectedLanguage
                        )
                    )

                    withContext(Dispatchers.Main) {
                        textToSpeechManager.setDetectedLanguage(detectedLanguage)
                        progressBar.gone()
                        txtResult.setText(transcribedText)
                        onTranscriptionSuccess?.invoke(transcribedText, detectedLanguage)
                        if (transcribedText.isNotEmpty()) {
                            textToSpeechManager.speakText(transcribedText)
                        }
                    }
                }

                if (file.exists()) file.delete()

            } catch (e: Exception) {
                Log.e(TAG, "Error: ${e.message}")
                withContext(Dispatchers.Main) {
                    context.showToast("Lỗi kết nối: ${e.message}", android.widget.Toast.LENGTH_LONG)
                    onTranscriptionError?.invoke()
                }
            }
        }
    }

    /** Gọi callback để MainActivity mở file picker (Activity Result API) */
    fun showAudioFilePicker() {
        onShowFilePicker?.invoke()
    }

    /** Xử lý URI audio được chọn từ file picker */
    fun handlePickedAudio(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            val fileName = getFileNameFromUri(uri)
            withContext(Dispatchers.Main) {
                confirmAndSendAudio(uri, fileName)
            }
        }
    }

    private fun getFileNameFromUri(uri: Uri): String {
        var fileName = "audio_${System.currentTimeMillis()}"
        runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(android.provider.MediaStore.MediaColumns.DISPLAY_NAME)
                    if (idx != -1) fileName = cursor.getString(idx)
                }
            }
        }
        return fileName
    }

    private fun confirmAndSendAudio(uri: Uri, fileName: String) {
        MaterialAlertDialogBuilder(context)
            .setTitle("Gửi file âm thanh?")
            .setMessage("File: $fileName")
            .setPositiveButton("Gửi") { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val tempFile = createTempFileFromUri(uri, fileName)
                        withContext(Dispatchers.Main) {
                            if (tempFile.exists()) {
                                progressBar.visible()
                                txtResult.setText("")
                                val hint = "Đang xử lý: $fileName"
                                txtResult.hint = SpannableString(hint).apply {
                                    setSpan(StyleSpan(android.graphics.Typeface.ITALIC), 0, hint.length, 0)
                                }
                                sendAudioToServer(tempFile)
                            } else {
                                context.showToast("File không tồn tại!")
                            }
                        }
                    } catch (e: IOException) {
                        withContext(Dispatchers.Main) {
                            context.showToast("Lỗi xử lý file: ${e.message}")
                        }
                    }
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    @Throws(IOException::class)
    private fun createTempFileFromUri(uri: Uri, fileName: String): File {
        val tempFile = File(
            context.getExternalFilesDir(android.os.Environment.DIRECTORY_MUSIC),
            fileName
        )
        val input: InputStream = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Cannot open URI")
        input.use { i ->
            FileOutputStream(tempFile).use { o ->
                i.copyTo(o, bufferSize = 8192)
            }
        }
        return tempFile
    }
}
