package com.nguyendevs.stkh;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.nguyendevs.stkh.Database.DatabaseHelper;
import com.nguyendevs.stkh.Manager.*;
import com.nguyendevs.stkh.Settings.SettingsActivity;

public class MainActivity extends AppCompatActivity {
    private EditText txtResult;
    private ImageView btnMic, btnRepeat, btnPause;
    private ImageButton searchIcon, btnSettings;
    private ProgressBar progressBar;
    private DrawerLayout drawerLayout;
    private ListView historyListView;
    private Handler handler;
    private SharedPreferences prefs;
    // Managers
    private TranslateManager translateManager;
    private TextToSpeechManager textToSpeechManager;
    private AudioRecorderManager audioRecorderManager;
    private ServerCommunicationManager serverCommunicationManager;
    private HistoryManager historyManager;
    private PermissionManager permissionManager;
    private UtilityManager utilityManager;
    private static final int SETTINGS_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.speechlayout);
        // Khởi tạo hoạt ảnh nền
        AnimationDrawable animDrawable = (AnimationDrawable) findViewById(R.id.root_layout).getBackground();
        animDrawable.setEnterFadeDuration(10);
        animDrawable.setExitFadeDuration(5000);
        animDrawable.start();
        // Khởi tạo các thành phần UI
        txtResult = findViewById(R.id.txtResult);
        btnMic = findViewById(R.id.btnMic);
        btnRepeat = findViewById(R.id.btnRepeat);
        btnPause = findViewById(R.id.btnPause);
        searchIcon = findViewById(R.id.search_icon);
        progressBar = findViewById(R.id.progressBar);
        btnSettings = findViewById(R.id.settings);
        drawerLayout = findViewById(R.id.drawer_layout);
        historyListView = findViewById(R.id.history_list);
        ImageView shareButton = findViewById(R.id.imageView);
        ImageView copyButton = findViewById(R.id.imageView2);
        ImageView saveButton = findViewById(R.id.imageViewSave);
        ImageView audioPicker = findViewById(R.id.imageView4);
        Button recentButton = findViewById(R.id.recent);
        ImageView translateButton = findViewById(R.id.imageViewTrans);
        handler = new Handler();
        prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        // Khởi tạo các thành phần managers
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        translateManager = new TranslateManager(this, txtResult);
        textToSpeechManager = new TextToSpeechManager(this, txtResult);
        audioRecorderManager = new AudioRecorderManager(this, txtResult, progressBar);
        serverCommunicationManager = new ServerCommunicationManager(this, txtResult, progressBar, dbHelper, textToSpeechManager);
        historyManager = new HistoryManager(textToSpeechManager, translateManager, this, dbHelper, historyListView, txtResult, saveButton);
        permissionManager = new PermissionManager(this);
        utilityManager = new UtilityManager(this, txtResult);
        // Đặt gợi ý cho txtResult
        String hintText = getString(R.string.ask_me_anything);
        SpannableString spannableHint = new SpannableString(hintText);
        spannableHint.setSpan(new StyleSpan(android.graphics.Typeface.ITALIC), 0, hintText.length(), 0);
        txtResult.setHint(spannableHint);
        // Kiểm tra quyền
        permissionManager.checkAudioPermission();
        permissionManager.checkStoragePermission();
        // Đặt chế độ hiển thị mặc định
        btnPause.setVisibility(View.GONE);
        btnRepeat.setVisibility(View.GONE);
        // Thiết lập listeners
        btnMic.setOnClickListener(v -> {
            translateManager.resetOriginalTextSnapshot();
                if (!audioRecorderManager.isRecording()) {
                    txtResult.setFocusable(false);
                    txtResult.setCursorVisible(false);
                    txtResult.setLongClickable(false);
                    txtResult.setKeyListener(null);
                    saveButton.setVisibility(View.GONE);
                    btnRepeat.setVisibility(View.GONE);
                    btnPause.setVisibility(View.GONE);
                    audioRecorderManager.startRecording();
                } else {
                    btnRepeat.setVisibility(View.GONE);
                    btnPause.setVisibility(View.GONE);
                    btnMic.setEnabled(false);
                    audioRecorderManager.stopRecordingAndSendToServer(serverCommunicationManager);
                }
        }
        );

        btnRepeat.setOnClickListener(v -> {
            String text = txtResult.getText().toString();
            if (!text.isEmpty()) {
                textToSpeechManager.speakText(text); } });

        btnPause.setOnClickListener(v -> {
            String text = txtResult.getText().toString();
            if (!text.isEmpty()) {
                textToSpeechManager.pauseText(); } });

        copyButton.setOnClickListener(v -> utilityManager.copyToClipboard(txtResult.getText().toString()));
        shareButton.setOnClickListener(v -> utilityManager.shareText());
        searchIcon.setOnClickListener(v -> utilityManager.searchOnGoogle());
        saveButton.setOnClickListener(v -> historyManager.saveEditedContent());
        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent); });
        audioPicker.setOnClickListener(v -> {
            permissionManager.checkStoragePermission();
            serverCommunicationManager.showAudioFilePicker(); });
        recentButton.setOnClickListener(v -> {
            historyManager.loadHistory();
            drawerLayout.openDrawer(GravityCompat.START); });
        translateButton.setOnClickListener(v -> translateManager.showTargetLanguagePopup());
        txtResult.setFocusable(false);
        txtResult.setCursorVisible(false);
        txtResult.setLongClickable(false);
        txtResult.setKeyListener(null); }
    public TextToSpeechManager getTextToSpeechManager() {
        return textToSpeechManager; }
    // Ủy quyền các phương thức cho managers
    public void copyToClipboard(String content) {
        utilityManager.copyToClipboard(content); }
    public void loadHistory() {
        historyManager.loadHistory(); }
    public void playSound(int resId) {
        audioRecorderManager.playSound(resId); }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionManager.onRequestPermissionsResult(requestCode, permissions, grantResults); }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        serverCommunicationManager.onActivityResult(requestCode, resultCode, data); }
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent != null && intent.getBooleanExtra("fromSettings", false)) {
            String language = intent.getStringExtra("selectedLanguage");
            int speed = intent.getIntExtra("speechSpeed", 50);
            int pitch = intent.getIntExtra("speechPitch", 50);
            String mode = intent.getStringExtra("selectedMode");
            if (language != null) {
                textToSpeechManager.setDetectedLanguage(language); }
            if (speed >= 0 && pitch >= 0) {
                textToSpeechManager.updateTtsSettings(speed, pitch); }
            if (mode != null) {
                prefs.edit().putString("selectedMode", mode).apply();
                ImageView audioPicker = findViewById(R.id.imageView4);
                audioPicker.setVisibility(mode.equals("Online") ? View.VISIBLE : View.GONE);
            }
        }
    }
    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
            finishAffinity();
        }
    }

    @Override
    protected void onDestroy() {
        textToSpeechManager.destroy();
        audioRecorderManager.destroy();
        historyManager.destroy();
        translateManager.destroy();
        handler.removeCallbacksAndMessages(null);
        super.onDestroy(); } }