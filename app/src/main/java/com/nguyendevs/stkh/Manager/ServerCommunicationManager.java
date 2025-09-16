package com.nguyendevs.stkh.Manager;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import com.nguyendevs.stkh.Database.DatabaseHelper;
import com.nguyendevs.stkh.MainActivity;
import com.nguyendevs.stkh.R;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONObject;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class ServerCommunicationManager {
    private static final String TAG = "ServerCommunicationManager";
    private final Context context;
    private final EditText txtResult;
    private final ProgressBar progressBar;
    private final DatabaseHelper dbHelper;
    private final TextToSpeechManager textToSpeechManager;
    private static final int REQUEST_CODE_PICK_AUDIO = 3;
    private final OkHttpClient client;
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

    public ServerCommunicationManager(Context context, EditText txtResult, ProgressBar progressBar, DatabaseHelper dbHelper, TextToSpeechManager textToSpeechManager) {
        this.context = context;
        this.txtResult = txtResult;
        this.progressBar = progressBar;
        this.dbHelper = dbHelper;
        this.textToSpeechManager = textToSpeechManager;

        // Khởi tạo OkHttpClient với cấu hình chấp nhận chứng chỉ tự ký
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS);

        // Định nghĩa TrustManager và SSLContext
        try {
            // Tạo TrustManager tùy chỉnh
            X509TrustManager trustManager = new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {
                    Log.d(TAG, "Client trusted for authType: " + authType);
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {
                    Log.d(TAG, "Server trusted for authType: " + authType);
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            };

            // Khởi tạo SSLContext với TLSv1.2 (hỗ trợ tốt trên Android)
            SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
            sslContext.init(null, new TrustManager[]{trustManager}, new SecureRandom());

            // Lấy SSLSocketFactory
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            // Cấu hình hostnameVerifier để bỏ qua kiểm tra hostname
            builder.hostnameVerifier((hostname, session) -> {
                Log.d(TAG, "Hostname verified: " + hostname);
                return true;
            });
            builder.sslSocketFactory(sslSocketFactory, trustManager);

            Log.d(TAG, "SSL/TLS configuration successful");

        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi cấu hình SSL/TLS: " + e.getMessage());
            // Fallback to unsecure client (for debugging only)
            builder = new OkHttpClient.Builder();
            Log.e(TAG, "Falling back to unsecure client due to SSL error");
        }

        this.client = builder.build();
    }

    public void sendAudioToServer(final File file) {
        new Thread(() -> {
            try {
                String serverIP = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                        .getString("serverIP", "localhost");
                String language = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                        .getString("selectedLanguage", "Vietnamese");
                String model = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                        .getString("selectedModel", "Medium");

                if (serverIP == null || serverIP.trim().isEmpty()) {
                    ((MainActivity) context).runOnUiThread(() -> {
                        Toast.makeText(context, "Địa chỉ server không hợp lệ!", Toast.LENGTH_LONG).show();
                        ((MainActivity) context).findViewById(R.id.btnMic).setEnabled(true);
                    });
                    return;
                }

                String serverUrl = "https://" + serverIP + ":8443/transcribe";
                Log.d(TAG, "Attempting to connect to: " + serverUrl);

                RequestBody requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("language", language.equals("Auto") ? "auto" : language)
                        .addFormDataPart("model_name", model)
                        .addFormDataPart("file", file.getName(),
                                RequestBody.create(MediaType.parse("audio/*"), file))
                        .build();

                Request request = new Request.Builder()
                        .url(serverUrl)
                        .post(requestBody)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("Unexpected code " + response);
                    }

                    String responseBody = response.body().string();
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    String transcribedText = jsonResponse.getString("text");
                    String detectedLanguage = jsonResponse.getString("language");

                    ((MainActivity) context).runOnUiThread(() -> {
                        textToSpeechManager.setDetectedLanguage(detectedLanguage);
                        progressBar.setVisibility(View.GONE);
                        ((MainActivity) context).findViewById(R.id.btnMic).setEnabled(true);
                        ((MainActivity) context).findViewById(R.id.btnRepeat).setVisibility(View.VISIBLE);
                        ((MainActivity) context).findViewById(R.id.btnPause).setVisibility(View.VISIBLE);
                        txtResult.setText(transcribedText);
                        dbHelper.addHistory(transcribedText, detectedLanguage);
                        ((MainActivity) context).loadHistory();
                        if (!transcribedText.isEmpty()) {
                            textToSpeechManager.speakText(transcribedText);
                        }
                    });

                    if (file.exists()) {
                        file.delete();
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Lỗi kết nối server: " + e.getMessage());
                ((MainActivity) context).runOnUiThread(() -> {
                    Toast.makeText(context, "Lỗi kết nối server: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    ((MainActivity) context).findViewById(R.id.btnMic).setEnabled(true);
                });
            }
        }).start();
    }

    public void showAudioFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            ((MainActivity) context).startActivityForResult(Intent.createChooser(intent, "Chọn file âm thanh"), REQUEST_CODE_PICK_AUDIO);
        } catch (Exception e) {
            Toast.makeText(context, "Không thể mở trình chọn file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_PICK_AUDIO && resultCode == MainActivity.RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                String fileName = getFileNameFromUri(uri);
                confirmAndSendAudio(uri.toString() + "|" + fileName);
            } else {
                Toast.makeText(context, "Không thể lấy file âm thanh!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String getFileNameFromUri(Uri uri) {
        String fileName = "unknown";
        Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    fileName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        return fileName;
    }

    private void confirmAndSendAudio(String fileInfo) {
        String[] parts = fileInfo.split("\\|");
        String fileUri = parts[0];
        String fileName = parts[1];

        AlertDialog.Builder confirmBuilder = new AlertDialog.Builder(context);
        confirmBuilder.setTitle("Xác nhận");
        confirmBuilder.setMessage("Bạn muốn gửi file: " + fileName + "?");
        confirmBuilder.setPositiveButton("OK", (dialog, which) -> {
            Uri audioUri = Uri.parse(fileUri);
            try {
                File tempFile = createTempFileFromUri(audioUri, fileName);
                if (tempFile.exists()) {
                    progressBar.setVisibility(View.VISIBLE);
                    txtResult.setText("");
                    String hintText = "Đang xử lý file: " + fileName;
                    SpannableString spannableHint = new SpannableString(hintText);
                    spannableHint.setSpan(new StyleSpan(android.graphics.Typeface.ITALIC), 0, hintText.length(), 0);
                    txtResult.setHint(spannableHint);
                    sendAudioToServer(tempFile);
                } else {
                    Toast.makeText(context, "File không tồn tại!", Toast.LENGTH_SHORT).show();
                }
            } catch (IOException e) {
                Toast.makeText(context, "Lỗi khi xử lý file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        confirmBuilder.setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());
        confirmBuilder.show();
    }

    private File createTempFileFromUri(Uri uri, String fileName) throws IOException {
        File tempFile = new File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), fileName);
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
             FileOutputStream outputStream = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
        return tempFile;
    }
}