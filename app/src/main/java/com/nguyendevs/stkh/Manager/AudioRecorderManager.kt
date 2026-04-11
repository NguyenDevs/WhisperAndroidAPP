package com.nguyendevs.stkh.manager

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Environment
import android.text.SpannableString
import android.text.style.StyleSpan
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import com.nguyendevs.stkh.R
import com.nguyendevs.stkh.util.showToast
import java.io.File
import java.io.IOException

/**
 * AudioRecorderManager - Quản lý ghi âm và phát lại âm thanh.
 * Migration: Java → Kotlin với state management rõ ràng hơn.
 */
class AudioRecorderManager(
    private val context: Context,
    private val txtResult: EditText,
    private val progressBar: ProgressBar
) {

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var _audioFile: File? = null
    val audioFile: File? get() = _audioFile

    var isRecording = false
        private set

    fun startRecording() {
        try {
            txtResult.setText("")
            _audioFile = File(
                context.getExternalFilesDir(Environment.DIRECTORY_MUSIC),
                "recording.amr"
            )
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
            progressBar.visibility = View.VISIBLE

            val hint = "Đang ghi âm…"
            val spannableHint = SpannableString(hint).apply {
                setSpan(StyleSpan(android.graphics.Typeface.ITALIC), 0, hint.length, 0)
            }
            txtResult.hint = spannableHint

        } catch (e: IOException) {
            context.showToast("Lỗi khi ghi âm: ${e.message}")
        }
    }

    fun stopRecordingAndSendToServer(
        serverManager: ServerCommunicationManager,
        onError: () -> Unit
    ) {
        if (mediaRecorder != null && isRecording) {
            try {
                mediaRecorder?.apply {
                    stop()
                    release()
                }
                mediaRecorder = null
                isRecording = false
                playSound(R.raw.off)
                progressBar.visibility = View.GONE

                val hint = "Đang chuyển đổi nội dung…"
                val spannableHint = SpannableString(hint).apply {
                    setSpan(StyleSpan(android.graphics.Typeface.ITALIC), 0, hint.length, 0)
                }
                txtResult.hint = spannableHint

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

    fun destroy() {
        mediaRecorder?.release()
        mediaRecorder = null
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
