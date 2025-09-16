package com.nguyendevs.stkh.Manager;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import com.nguyendevs.stkh.MainActivity;
import java.util.HashMap;
import java.util.Locale;
import java.util.Set;

public class TextToSpeechManager implements TextToSpeech.OnInitListener {
    private boolean isLanguageManuallySet = false;
    private static final String TAG = "TextToSpeechManager";
    private final Context context;
    private final EditText txtResult;
    private TextToSpeech textToSpeech;
    private String detectedLanguage = "Vietnamese";
    private String currentText = "";
    private int lastSpokenIndex = 0;
    private int resumeIndex = 0;
    private boolean isSpeaking = false;
    private boolean isPaused = false;
    private int speechSpeed = 50; // Tốc độ đọc (0-100)
    private int speechPitch = 50; // Cao độ giọng (0-100)
    private static final HashMap<String, String> languageToCodeMap = new HashMap<>();
    static {
        languageToCodeMap.put("Vietnamese", "vi-VN");
        languageToCodeMap.put("English", "en-US");
        languageToCodeMap.put("Japanese", "ja-JP");
        languageToCodeMap.put("Russian", "ru-RU");
        languageToCodeMap.put("French", "fr-FR");
        languageToCodeMap.put("Spanish", "es-ES");
        languageToCodeMap.put("German", "de-DE");
        languageToCodeMap.put("Chinese", "zh-CN");
        languageToCodeMap.put("Korean", "ko-KR");
        languageToCodeMap.put("Italian", "it-IT");
    }
    public TextToSpeechManager(Context context, EditText txtResult) {
        this.context = context;
        this.txtResult = txtResult;
        textToSpeech = new TextToSpeech(context, this);
        // Lấy giá trị từ SharedPreferences khi khởi tạo
        SharedPreferences prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        speechSpeed = prefs.getInt("speechSpeed", 50);
        speechPitch = prefs.getInt("speechPitch", 50);
    }
    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            setTextToSpeechLanguage();
            textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {}
                @Override
                public void onDone(String utteranceId) {
                    ((MainActivity) context).runOnUiThread(() -> {
                        txtResult.setText(currentText);
                        txtResult.setFocusable(false);
                        txtResult.setCursorVisible(false);
                        txtResult.setLongClickable(false);
                        txtResult.setKeyListener(null);
                        isSpeaking = false;
                        isPaused = false;
                        lastSpokenIndex = 0;
                        resumeIndex = 0;
                    });
                }
                @Override
                public void onError(String utteranceId) {
                    ((MainActivity) context).runOnUiThread(() -> {
                        Toast.makeText(context, "Lỗi khi đọc văn bản!", Toast.LENGTH_SHORT).show();
                    });
                }
                @Override
                public void onRangeStart(String utteranceId, int start, int end, int frame) {
                    String text = txtResult.getText().toString();
                    if (!text.equals(currentText)) {
                        lastSpokenIndex = 0;
                        resumeIndex = 0;
                        currentText = text;
                    }
                    int adjustedStart = resumeIndex + start;
                    int adjustedEnd = resumeIndex + end;
                    if (adjustedStart >= 0 && adjustedEnd <= text.length()) {
                        lastSpokenIndex = adjustedStart;
                        ((MainActivity) context).runOnUiThread(() -> {
                            Editable editable = txtResult.getText();
                            Spannable spannable = new SpannableString(editable);
                            // Xóa các ForegroundColorSpan cũ
                            ForegroundColorSpan[] spans = spannable.getSpans(0, spannable.length(), ForegroundColorSpan.class);
                            for (ForegroundColorSpan span : spans) {
                                spannable.removeSpan(span);
                            }
                            // Tô toàn bộ màu đen
                            spannable.setSpan(new ForegroundColorSpan(Color.BLACK), 0, spannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            // Tô màu đỏ cho từ đang nói
                            spannable.setSpan(new ForegroundColorSpan(Color.RED), adjustedStart, adjustedEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            // Cập nhật hiển thị
                            txtResult.setText(spannable, TextView.BufferType.SPANNABLE);

                            txtResult.setSelection(adjustedEnd);
                        });
                    }
                }
            });
        } else {
            Toast.makeText(context, "Khởi tạo TextToSpeech thất bại!", Toast.LENGTH_SHORT).show();
        }
    }
    public void setLanguage(String langCode) {
        isLanguageManuallySet = true;
        Locale locale;
        switch (langCode) {
            case "Vietnamese":
                locale = new Locale("vi", "VN");
                break;
            case "English":
                locale = Locale.US;
                break;
            case "Japanese":
                locale = Locale.JAPAN;
                break;
            case "Russian":
                locale = new Locale("ru", "RU");
                break;
            case "French":
                locale = Locale.FRANCE;
                break;
            case "Spanish":
                locale = new Locale("es", "ES");
                break;
            case "German":
                locale = new Locale("de", "DE");
                break;
            case "Chinese":
                locale = Locale.SIMPLIFIED_CHINESE;
                break;
            case "Korean":
                locale = new Locale("ko", "KR");
                break;
            case "Italian":
                locale = Locale.ITALY;
                break;
            default:
                Toast.makeText(context, "Ngôn ngữ không hỗ trợ: " + langCode, Toast.LENGTH_SHORT).show();
                return;
        }
        int result = textToSpeech.setLanguage(locale);
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Toast.makeText(context, "Ngôn ngữ không được hỗ trợ hoặc thiếu dữ liệu: " + langCode, Toast.LENGTH_SHORT).show();
            return;
        }
        float speed = (float) speechSpeed / 50;
        float pitch = (float) speechPitch / 50;
        textToSpeech.setSpeechRate(speed);
        textToSpeech.setPitch(pitch);
    }
    public boolean setTextToSpeechLanguage() {
        if ("Auto".equals(detectedLanguage)) {
         return true;
        }
        if (!languageToCodeMap.containsKey(detectedLanguage)) {
            showLanguageNotSupportedDialog("Ngôn ngữ '" + detectedLanguage + "' không được hỗ trợ.");
            return false;
        }
        String languageCode = languageToCodeMap.get(detectedLanguage);
        Locale locale;
        switch (languageCode) {
            case "en-US":
                locale = Locale.US;
                break;
            case "ja-JP":
                locale = Locale.JAPAN;
                break;
            case "ru-RU":
                locale = new Locale("ru", "RU");
                break;
            case "fr-FR":
                locale = Locale.FRANCE;
                break;
            case "vi-VN":
                locale = new Locale("vi", "VN");
                break;
            case "es-ES":
                locale = new Locale("es", "ES");
                break;
            case "de-DE":
                locale = new Locale("de", "DE");
                break;
            case "zh-CN":
                locale = Locale.SIMPLIFIED_CHINESE;
                break;
            case "ko-KR":
                locale = new Locale("ko", "KR");
                break;
            case "it-IT":
                locale = Locale.ITALY;
                break;
            default:
                showLanguageNotSupportedDialog("Mã ngôn ngữ '" + languageCode + "' không hợp lệ.");
                return false;
        }
        int result = textToSpeech.setLanguage(locale);
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            showLanguageNotSupportedDialog("Ngôn ngữ '" + detectedLanguage + "' không được hỗ trợ hoặc thiếu dữ liệu giọng nói.");
            return false;
        }

        float speed = (float) speechSpeed / 50; // Chuyển từ 0-100 sang 0.0-2.0 (mặc định là 1.0)
        float pitch = (float) speechPitch / 50; // Chuyển từ 0-100 sang 0.0-2.0 (mặc định là 1.0)
        textToSpeech.setSpeechRate(speed);
        textToSpeech.setPitch(pitch);
        return true;
    }

    private void showLanguageNotSupportedDialog(String message) {
        ((MainActivity) context).runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Ngôn ngữ không khả dụng");
            builder.setMessage(message + "\nBạn có muốn tải dữ liệu giọng nói hoặc chuyển về tiếng Việt?");
            builder.setPositiveButton("Tải dữ liệu", (dialog, which) -> {
                Intent intent = new Intent();
                intent.setAction("com.android.settings.TTS_SETTINGS");
                try {
                    context.startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(context, "Không thể mở cài đặt TTS. Vui lòng kiểm tra cài đặt hệ thống.", Toast.LENGTH_LONG).show();
                }
                // Chuyển tạm về tiếng Việt
                textToSpeech.setLanguage(new Locale("vi", "VN"));
                detectedLanguage = "Vietnamese";
                Toast.makeText(context, "Đã chuyển tạm về tiếng Việt.", Toast.LENGTH_SHORT).show();
            });
            builder.setNegativeButton("Chuyển về tiếng Việt", (dialog, which) -> {
                textToSpeech.setLanguage(new Locale("vi", "VN"));
                detectedLanguage = "Vietnamese";
                context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE).edit()
                        .putString("selectedLanguage", "Vietnamese").apply();
                Toast.makeText(context, "Đã chuyển về tiếng Việt.", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
            builder.setCancelable(false);
            builder.show();
        });
    }
    public void speakText(String text) {
        if (textToSpeech != null && !text.isEmpty()) {
            txtResult.setText(text);
            txtResult.setFocusable(false);
            txtResult.setCursorVisible(false);
            txtResult.setLongClickable(false);
            txtResult.setKeyListener(null);
            currentText = text;
            String utteranceId = String.valueOf(System.currentTimeMillis());
            String textToSpeak;
            if (isPaused && lastSpokenIndex > 0 && lastSpokenIndex < text.length()) {
                textToSpeak = text.substring(lastSpokenIndex);
                resumeIndex = lastSpokenIndex;
            } else {
                textToSpeak = text;
                lastSpokenIndex = 0;
                resumeIndex = 0;
            }
            if (!isLanguageManuallySet) {
                if (!setTextToSpeechLanguage()) {
                    return;
                }
            }
            textToSpeech.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
            isSpeaking = true;
            isPaused = false;
        }
    }
    public void pauseText() {
        if (textToSpeech != null && isSpeaking && !isPaused) {
            textToSpeech.stop();
            isPaused = true;
            isSpeaking = false;
            ((MainActivity) context).runOnUiThread(() -> {
                String text = txtResult.getText().toString();
                if (lastSpokenIndex < text.length()) {
                    int wordEnd = lastSpokenIndex;
                    while (wordEnd < text.length() && !Character.isWhitespace(text.charAt(wordEnd))) {
                        wordEnd++;
                    }
                    SpannableString spannable = new SpannableString(text);
                    spannable.setSpan(new ForegroundColorSpan(Color.BLACK), 0, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    spannable.setSpan(new ForegroundColorSpan(Color.RED), lastSpokenIndex, wordEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    txtResult.setText(spannable);
                }
            });
        }
    }
    public void destroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }
    public void setDetectedLanguage(String language) {
        this.detectedLanguage = language;
        setTextToSpeechLanguage();
    }
    public void updateTtsSettings(int speed, int pitch) {
        this.speechSpeed = speed;
        this.speechPitch = pitch;
        setTextToSpeechLanguage();
    }
}