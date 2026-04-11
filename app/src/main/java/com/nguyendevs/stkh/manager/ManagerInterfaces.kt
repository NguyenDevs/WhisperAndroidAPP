package com.nguyendevs.stkh.manager

/** Interface cho chức năng ghi âm. */
interface IAudioRecorder {
    val isRecording: Boolean
    fun startRecording()
    fun stopRecordingAndSendToServer(serverManager: IServerClient, onError: () -> Unit)
    fun destroy()
}

/** Interface cho giao tiếp server transcription. */
interface IServerClient {
    var onShowFilePicker: (() -> Unit)?
    fun sendAudioToServer(file: java.io.File)
    fun handlePickedAudio(uri: android.net.Uri)
    fun showAudioFilePicker()
}

/** Interface cho Text-to-Speech. */
interface ITextToSpeech {
    fun speakText(text: String)
    fun pauseText()
    fun setLanguage(langCode: String)
    fun setDetectedLanguage(language: String)
    fun updateTtsSettings(speed: Int, pitch: Int)
    fun destroy()
}

/** Interface cho dịch thuật. */
interface ITranslator {
    fun showTargetLanguagePopup()
    fun resetOriginalTextSnapshot()
    fun destroy()
}
