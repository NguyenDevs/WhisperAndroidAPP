package com.nguyendevs.stkh.manager

/**
 * IAudioRecorder - Interface cho chức năng ghi âm.
 *
 * SOLID - ISP: Tách biệt recording contract khỏi playback hay server upload.
 * Client (MainActivity) chỉ cần biết interface này, không phụ thuộc toàn bộ AudioRecorderManager.
 */
interface IAudioRecorder {
    val isRecording: Boolean
    fun startRecording()
    fun stopRecordingAndSendToServer(
        serverManager: IServerClient,
        onError: () -> Unit
    )
    fun destroy()
}

/**
 * IServerClient - Interface cho giao tiếp server.
 *
 * SOLID - ISP: Tách biệt server communication khỏi file picking logic.
 * DIP: MainActivity + AudioRecorderManager phụ thuộc vào interface, không phụ thuộc OkHttp cụ thể.
 */
interface IServerClient {
    /** Gọi callback để owner mở file picker */
    var onShowFilePicker: (() -> Unit)?
    /** Gửi file âm thanh lên server để transcribe */
    fun sendAudioToServer(file: java.io.File)
    /** Xử lý URI file đã chọn */
    fun handlePickedAudio(uri: android.net.Uri)
    /** Yêu cầu owner mở file picker */
    fun showAudioFilePicker()
}

/**
 * ITextToSpeech - Interface cho TTS.
 *
 * SOLID - ISP: TTS có nhiều chức năng (speak, pause, language), interface chia nhỏ
 * các consumer theo nhu cầu (HistoryManager chỉ cần setLanguage + speakText).
 */
interface ITextToSpeech {
    fun speakText(text: String)
    fun pauseText()
    fun setLanguage(langCode: String)
    fun setDetectedLanguage(language: String)
    fun updateTtsSettings(speed: Int, pitch: Int)
    fun destroy()
}

/**
 * ITranslator - Interface cho dịch thuật.
 */
interface ITranslator {
    fun showTargetLanguagePopup()
    fun resetOriginalTextSnapshot()
    fun destroy()
}
