package com.nguyendevs.stkh.manager

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Environment
import android.text.SpannableString
import android.text.style.StyleSpan
import android.widget.EditText
import android.widget.ProgressBar
import com.nguyendevs.stkh.R
import com.nguyendevs.stkh.util.gone
import com.nguyendevs.stkh.util.showToast
import com.nguyendevs.stkh.util.visible
import java.io.File
import java.io.IOException

/**
 * AudioRecorderManager implements IAudioRecorder.
 *
 * SOLID:
 * - SRP: Chỉ lo ghi âm và phát âm thanh, không biết gì về server/UI
 * - LSP: Có thể thay thế bằng MockAudioRecorder trong test mà không ảnh hưởng MainActivity
 * - DIP: MainActivity phụ thuộc vào IAudioRecorder, không phụ thuộc class cụ thể
 */
class AudioRecorderManager(
    private val context: Context,
    private val txtResult: EditText,
    private val progressBar: ProgressBar
) : IAudioRecorder {

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var _audioFile: File? = null
    val audioFile: File? get() = _audioFile

    override var isRecording: Boolean = false
        private set

    override fun startRecording() {
        try {
            txtResult.setText("")
            _audioFile = File(
                context.getExternalFilesDir(Environment.DIRECTORY_MUSIC),
                "recording_${System.currentTimeMillis()}.amr"
            )
            @Suppress("DEPRECATION")
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.DEFAULT)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(_audioFile!!.absolutePath)
                prepare()
                start()
            }
            isRecording = true
            playSound(R.raw.on)
            progressBar.visible()

            val hint = "Đang ghi âm…"
            txtResult.hint = SpannableString(hint).apply {
                setSpan(StyleSpan(android.graphics.Typeface.ITALIC), 0, hint.length, 0)
            }
        } catch (e: IOException) {
            context.showToast("Lỗi khi ghi âm: ${e.message}")
        }
    }

    override fun stopRecordingAndSendToServer(
        serverManager: IServerClient,
        onError: () -> Unit
    ) {
        if (mediaRecorder != null && isRecording) {
            try {
                mediaRecorder?.apply { stop(); release() }
                mediaRecorder = null
                isRecording = false
                playSound(R.raw.off)
                progressBar.gone()

                val hint = "Đang xử lý…"
                txtResult.hint = SpannableString(hint).apply {
                    setSpan(StyleSpan(android.graphics.Typeface.ITALIC), 0, hint.length, 0)
                }
                _audioFile?.let { serverManager.sendAudioToServer(it) }
            } catch (e: Exception) {
                context.showToast("Lỗi khi dừng ghi âm: ${e.message}")
                onError()
            }
        }
    }

    fun playSound(resId: Int) {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer.create(context, resId)?.apply { start() }
    }

    override fun destroy() {
        mediaRecorder?.release()
        mediaRecorder = null
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
