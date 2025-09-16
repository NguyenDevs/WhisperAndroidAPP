package com.nguyendevs.stkh.Settings;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.nguyendevs.stkh.Database.DatabaseHelper;
import com.nguyendevs.stkh.MainActivity;
import com.nguyendevs.stkh.R;

public class SettingsActivity extends AppCompatActivity {
    private String selectedLanguage = "Vietnamese";
    private String selectedModel = "Medium";
    private int speechSpeed = 50;
    private int speechPitch = 50;
    private DatabaseHelper dbHelper;
    private Spinner languageSpinner, modelSpinner;
    private TextView modelLabel;
    private SharedPreferences prefs;
    private EditText serverIP;
    private SeekBar speechSpeedSeekBar, speechPitchSeekBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settinglayout);
        serverIP = findViewById(R.id.serverip);
        dbHelper = new DatabaseHelper(this);
        prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        selectedLanguage = prefs.getString("selectedLanguage", "Vietnamese");
        selectedModel = prefs.getString("selectedModel", "Medium");
        speechSpeed = prefs.getInt("speechSpeed", 50);
        speechPitch = prefs.getInt("speechPitch", 50);
        serverIP.setText(prefs.getString("serverIP", "14.235.42.182"));
        serverIP.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                prefs.edit().putString("serverIP", s.toString().trim()).apply();
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });
        languageSpinner = findViewById(R.id.language);
        updateLanguageSpinner();
        modelSpinner = findViewById(R.id.model);
        modelLabel = findViewById(R.id.modeler);
        String[] models = {"Small", "Medium", "Large"};
        ArrayAdapter<String> modelAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, models);
        modelSpinner.setAdapter(modelAdapter);
        modelSpinner.setSelection(java.util.Arrays.asList(models).indexOf(selectedModel));
        modelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedModel = models[position];
                prefs.edit().putString("selectedModel", selectedModel).apply();
            }
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        modelSpinner.setVisibility(View.VISIBLE);
        modelLabel.setVisibility(View.VISIBLE);
        speechSpeedSeekBar = findViewById(R.id.speech_speed);
        speechSpeedSeekBar.setProgress(speechSpeed);
        speechSpeedSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                speechSpeed = progress;
                prefs.edit().putInt("speechSpeed", speechSpeed).apply();
            }
            public void onStartTrackingTouch(SeekBar seekBar) {}
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        speechPitchSeekBar = findViewById(R.id.speech_pitch);
        speechPitchSeekBar.setProgress(speechPitch);
        speechPitchSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                speechPitch = progress;
                prefs.edit().putInt("speechPitch", speechPitch).apply();
            }
            public void onStartTrackingTouch(SeekBar seekBar) {}
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        findViewById(R.id.clear_history_button).setOnClickListener(v -> showClearHistoryDialog());
        findViewById(R.id.reset_button).setOnClickListener(v -> showResetDialog());
        findViewById(R.id.back).setOnClickListener(v -> returnToMainActivity());
        findViewById(R.id.about).setOnClickListener(v -> showAboutDialog());
    }
    private void showAboutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Về chúng tôi")
                .setMessage("Ứng dụng được phát triển bởi NguyenDevs. Có chức năng nhận diện giọng nói thành văn bản. Tích hợp mô hình AI Whisper của OpenAI")
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .setCancelable(true)
                .show();
    }
    private void updateLanguageSpinner() {
        String[] displayLanguages = {"Tự động", "Tiếng Việt", "English", "Japanese", "Russian", "French", "Spanish", "German", "Chinese", "Korean", "Italian"};
        String[] whisperLanguages = {"Auto", "Vietnamese", "English", "Japanese", "Russian", "French", "Spanish", "German", "Chinese", "Korean", "Italian"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, displayLanguages);
        languageSpinner.setAdapter(adapter);
        int selectedIndex = java.util.Arrays.asList(whisperLanguages).indexOf(selectedLanguage);
        if (selectedIndex == -1) selectedIndex = 0;
        languageSpinner.setSelection(selectedIndex);
        languageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedLanguage = whisperLanguages[position];
                prefs.edit().putString("selectedLanguage", selectedLanguage).apply();
            }
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }
    private void showClearHistoryDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa lịch sử")
                .setMessage("Bạn có chắc chắn muốn xóa toàn bộ lịch sử không? Hành động này không thể hoàn tác.")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    dbHelper.clearHistory();
                    Toast.makeText(this, "Đã xóa toàn bộ lịch sử", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    returnToMainActivity();
                })
                .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss())
                .setCancelable(true)
                .show();
    }
    private void showResetDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận reset cài đặt")
                .setMessage("Bạn có chắc chắn muốn reset về cài đặt mặc định không?")
                .setPositiveButton("Reset", (dialog, which) -> {
                    selectedLanguage = "Vietnamese";
                    selectedModel = "Medium";
                    speechSpeed = 50;
                    speechPitch = 50;
                    serverIP.setText("14.235.42.182");

                    prefs.edit()
                            .putString("selectedLanguage", selectedLanguage)
                            .putString("selectedModel", selectedModel)
                            .putInt("speechSpeed", speechSpeed)
                            .putInt("speechPitch", speechPitch)
                            .putString("serverIP", "14.235.42.182")
                            .apply();
                    updateLanguageSpinner();
                    modelSpinner.setSelection(1);
                    speechSpeedSeekBar.setProgress(speechSpeed);
                    speechPitchSeekBar.setProgress(speechPitch);
                    Toast.makeText(this, "Đã reset về cài đặt mặc định", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    returnToMainActivity();
                })
                .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss())
                .setCancelable(true)
                .show();
    }
    private void returnToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("fromSettings", true);
        intent.putExtra("selectedLanguage", selectedLanguage);
        intent.putExtra("selectedModel", selectedModel);
        intent.putExtra("speechSpeed", speechSpeed);
        intent.putExtra("speechPitch", speechPitch);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        returnToMainActivity();
    }
    @Override
    protected void onDestroy() {
        if (dbHelper != null) dbHelper.close();
        super.onDestroy();
    }
}