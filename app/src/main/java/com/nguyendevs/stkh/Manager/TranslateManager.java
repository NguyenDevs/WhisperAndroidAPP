package com.nguyendevs.stkh.Manager;

import android.app.AlertDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.EditText;
import android.widget.Toast;
import com.google.mlkit.nl.languageid.LanguageIdentification;
import com.google.mlkit.nl.languageid.LanguageIdentifier;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;
import com.nguyendevs.stkh.MainActivity;

public class TranslateManager {
    private MainActivity activity;
    private EditText txtResult;
    private Translator translator;
    private String originalTextSnapshot = null;
    public TranslateManager(MainActivity activity, EditText txtResult) {
        this.activity = activity;
        this.txtResult = txtResult; }
    public void showTargetLanguagePopup() {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Dịch sang ngôn ngữ nào?");
        String[] languages = {
                "Tiếng Việt",
                "Tiếng Anh",
                "Tiếng Nhật",
                "Tiếng Nga",
                "Tiếng Pháp",
                "Tiếng Tây Ban Nha",
                "Tiếng Đức",
                "Tiếng Trung",
                "Tiếng Hàn",
                "Tiếng Ý" };
        String[] languageCodes = {
                "Vietnamese",
                "English",
                "Japanese",
                "Russian",
                "French",
                "Spanish",
                "German",
                "Chinese",
                "Korean",
                "Italian" };
        builder.setItems(languages, (dialog, which) -> {
            String currentText = txtResult.getText().toString().trim();
            if (currentText.isEmpty()) {
                Toast.makeText(activity, "Không có nội dung để dịch.", Toast.LENGTH_SHORT).show();
                return; }
            if (originalTextSnapshot == null) {
                originalTextSnapshot = currentText; }
            String targetLang = languageCodes[which];
            detectAndTranslate(originalTextSnapshot, targetLang); });
        builder.show();  }
    public void resetOriginalTextSnapshot() {
        originalTextSnapshot = null; }
    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnected(); }
    private String convertToMLKitCode(String lang) {
        switch (lang) {
            case "Vietnamese": return com.google.mlkit.nl.translate.TranslateLanguage.VIETNAMESE;
            case "Japanese": return com.google.mlkit.nl.translate.TranslateLanguage.JAPANESE;
            case "Korean": return com.google.mlkit.nl.translate.TranslateLanguage.KOREAN;
            case "English": return com.google.mlkit.nl.translate.TranslateLanguage.ENGLISH;
            case "Russian": return com.google.mlkit.nl.translate.TranslateLanguage.RUSSIAN;
            case "Italian": return com.google.mlkit.nl.translate.TranslateLanguage.ITALIAN;
            case "Spanish": return com.google.mlkit.nl.translate.TranslateLanguage.SPANISH;
            case "French": return com.google.mlkit.nl.translate.TranslateLanguage.FRENCH;
            case "German": return com.google.mlkit.nl.translate.TranslateLanguage.GERMAN;
            case "Chinese": return com.google.mlkit.nl.translate.TranslateLanguage.CHINESE;
            default: return com.google.mlkit.nl.translate.TranslateLanguage.VIETNAMESE; } }
    private String getDisplayLanguageName(String lang) {
        switch (lang) {
            case "Vietnamese": return "Tiếng Việt";
            case "Japanese": return "Tiếng Nhật";
            case "Korean": return "Tiếng Hàn";
            case "English": return "Tiếng Anh";
            case "French": return "Tiếng Pháp";
            case "German": return "Tiếng Đức";
            case "Chinese": return "Tiếng Trung";
            case "Russian": return "Tiếng Nga";
            case "Spanish": return "Tiếng Tây Ban Nha";
            case "Italian": return "Tiếng Ý";
            default: return "Tiếng Việt"; } }
    private void detectAndTranslate(String text, String targetLang) {
        LanguageIdentifier identifier = LanguageIdentification.getClient();
        identifier.identifyLanguage(text)
                .addOnSuccessListener(languageCode -> {
                    if (languageCode.equals("unknown")) {
                        Toast.makeText(activity, "Không thể xác định ngôn ngữ.", Toast.LENGTH_SHORT).show();
                        return; }
                    translateText(text, languageCode, convertToMLKitCode(targetLang), targetLang);  })
                .addOnFailureListener(e -> {
                    Toast.makeText(activity, "Lỗi xác định ngôn ngữ: " + e.getMessage(), Toast.LENGTH_SHORT).show(); }); }
    private void translateText(String text, String sourceLang, String targetLangCode, String targetLangName) {
        TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(sourceLang)
                .setTargetLanguage(targetLangCode)
                .build();
        translator = Translation.getClient(options);
        if (!isNetworkAvailable()) {
            Toast.makeText(activity, "Không có kết nối mạng. Đang thử dịch ngoại tuyến.", Toast.LENGTH_SHORT).show(); }
        translator.downloadModelIfNeeded()
                .addOnSuccessListener(unused -> {
                    translator.translate(text)
                            .addOnSuccessListener(translatedText -> {
                                txtResult.setText(translatedText);
                                TextToSpeechManager tts = activity.getTextToSpeechManager();
                                tts.setDetectedLanguage(targetLangName);
                                tts.speakText(translatedText); })
                            .addOnFailureListener(e -> {
                                Toast.makeText(activity, "Dịch thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show(); }); })
                .addOnFailureListener(e -> {
                    Toast.makeText(activity, "Không thể tải mô hình dịch: " + e.getMessage(), Toast.LENGTH_LONG).show(); }); }
    public void destroy() {
        if (translator != null) {
            translator.close(); } } }