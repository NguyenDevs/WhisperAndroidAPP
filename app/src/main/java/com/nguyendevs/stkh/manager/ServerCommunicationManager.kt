package com.nguyendevs.stkh.manager

import android.content.Context
import android.net.Uri
import android.text.SpannableString
import android.text.style.StyleSpan
import android.util.Log
import android.widget.EditText
import android.widget.ProgressBar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nguyendevs.stkh.repository.IHistoryRepository
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

class ServerCommunicationManager(
    private val context: Context,
    private val txtResult: EditText,
    private val progressBar: ProgressBar,
    private val historyRepository: IHistoryRepository,
    private val ttsManager: ITextToSpeech,
    private val lifecycleScope: CoroutineScope
) : IServerClient {

    companion object {
        private const val TAG = "ServerCommunicationManager"
    }

    var onTranscriptionSuccess: ((text: String, language: String) -> Unit)? = null
    var onTranscriptionError: (() -> Unit)? = null
    override var onShowFilePicker: (() -> Unit)? = null

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

    override fun sendAudioToServer(file: File) {
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
                val url = "https://$serverIP:8443/transcribe"

                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("language", if (language == "Auto") "auto" else language)
                    .addFormDataPart("model_name", model)
                    .addFormDataPart("file", file.name, file.asRequestBody("audio/*".toMediaType()))
                    .build()

                client.newCall(Request.Builder().url(url).post(requestBody).build())
                    .execute().use { response ->
                        if (!response.isSuccessful) throw IOException("Server error: ${response.code}")
                        val json = JSONObject(response.body?.string() ?: throw IOException("Empty response"))
                        val text = json.getString("text")
                        val lang = json.getString("language")

                        historyRepository.insert(text, lang)

                        withContext(Dispatchers.Main) {
                            ttsManager.setDetectedLanguage(lang)
                            progressBar.gone()
                            txtResult.setText(text)
                            onTranscriptionSuccess?.invoke(text, lang)
                            if (text.isNotEmpty()) ttsManager.speakText(text)
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

    override fun showAudioFilePicker() = onShowFilePicker?.invoke() ?: Unit

    override fun handlePickedAudio(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            val fileName = getFileNameFromUri(uri)
            withContext(Dispatchers.Main) { confirmAndSendAudio(uri, fileName) }
        }
    }

    private fun getFileNameFromUri(uri: Uri): String {
        var name = "audio_${System.currentTimeMillis()}"
        runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(android.provider.MediaStore.MediaColumns.DISPLAY_NAME)
                    if (idx != -1) name = cursor.getString(idx)
                }
            }
        }
        return name
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
                                txtResult.hint = SpannableString("Đang xử lý: $fileName").apply {
                                    setSpan(StyleSpan(android.graphics.Typeface.ITALIC), 0, length, 0)
                                }
                                sendAudioToServer(tempFile)
                            } else {
                                context.showToast("File không tồn tại!")
                            }
                        }
                    } catch (e: IOException) {
                        withContext(Dispatchers.Main) { context.showToast("Lỗi xử lý file: ${e.message}") }
                    }
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    @Throws(IOException::class)
    private fun createTempFileFromUri(uri: Uri, fileName: String): File {
        val file = File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_MUSIC), fileName)
        val input: InputStream = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Cannot open URI")
        input.use { i -> FileOutputStream(file).use { o -> i.copyTo(o, 8192) } }
        return file
    }
}
