package com.nguyendevs.stkh

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.SpannableString
import android.text.style.StyleSpan
import android.view.animation.AnimationUtils
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.nguyendevs.stkh.adapter.HistoryAdapter
import com.nguyendevs.stkh.database.AppDatabase
import com.nguyendevs.stkh.databinding.ActivityMainBinding
import com.nguyendevs.stkh.manager.*
import com.nguyendevs.stkh.settings.SettingsActivity
import com.nguyendevs.stkh.util.*

/**
 * MainActivity - Activity chính NguyenDevs STT.
 * 
 * Cải tiến UI/UX:
 * - Activity Result API thay deprecated startActivityForResult/onActivityResult
 * - OnBackPressedCallback thay deprecated onBackPressed
 * - Snackbar thay Toast cho các action feedback
 * - Room DB (AppDatabase) thay SQLite thủ công
 * - Flow + observe tự động cập nhật danh sách lịch sử
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: SharedPreferences
    private lateinit var db: AppDatabase

    // Managers
    private lateinit var translateManager: TranslateManager
    private lateinit var textToSpeechManager: TextToSpeechManager
    private lateinit var audioRecorderManager: AudioRecorderManager
    private lateinit var serverCommunicationManager: ServerCommunicationManager
    private lateinit var historyManager: HistoryManager
    private lateinit var permissionManager: PermissionManager
    private lateinit var utilityManager: UtilityManager

    // Activity Result launcher (thay deprecated startActivityForResult)
    private val audioPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { serverCommunicationManager.handlePickedAudio(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        db = AppDatabase.getInstance(this)

        initManagers()
        setupRecyclerView()
        setupUiInitialState()
        setupListeners()
        setupBackPressedCallback()

        // Bắt đầu observe Room Flow → tự động update RecyclerView
        historyManager.observeHistory()

        permissionManager.checkAudioPermission()
        permissionManager.checkStoragePermission()
    }

    // ======================================================
    // INIT
    // ======================================================

    private fun initManagers() {
        textToSpeechManager = TextToSpeechManager(this, binding.txtResult).also {
            it.onSpeakStart = { /* placeholder */ }
            it.onSpeakDone = { /* placeholder */ }
        }

        translateManager = TranslateManager(this, binding.txtResult).also { mgr ->
            mgr.onTranslated = { text, lang ->
                textToSpeechManager.setDetectedLanguage(lang)
                textToSpeechManager.speakText(text)
            }
        }

        audioRecorderManager = AudioRecorderManager(this, binding.txtResult, binding.progressBar)

        serverCommunicationManager = ServerCommunicationManager(
            context = this,
            txtResult = binding.txtResult,
            progressBar = binding.progressBar,
            db = db,
            textToSpeechManager = textToSpeechManager,
            lifecycleScope = lifecycleScope
        ).also { mgr ->
            mgr.onTranscriptionSuccess = { _, _ ->
                binding.cardTtsControls.visible()
                binding.cardTtsControls.startAnimation(
                    AnimationUtils.loadAnimation(this, R.anim.fade_in)
                )
                binding.btnMic.isEnabled = true
            }
            mgr.onTranscriptionError = {
                binding.btnMic.isEnabled = true
                setResultAreaHint(getString(R.string.ask_me_anything))
            }
            // Activity Result API: MainActivity là owner của launcher
            mgr.onShowFilePicker = { audioPickerLauncher.launch("audio/*") }
        }

        val historyAdapter = HistoryAdapter(
            onItemClick = { item ->
                historyManager.onItemClick(item)
                binding.drawerLayout.closeDrawer(GravityCompat.START)
            },
            onMenuCopy = { item -> historyManager.onMenuCopy(item) },
            onMenuView = { item ->
                historyManager.onMenuView(item)
                binding.cardTtsControls.visible()
            },
            onMenuEdit = { item -> historyManager.onMenuEdit(item) },
            onMenuDelete = { item -> historyManager.onMenuDelete(item) },
            onMenuExport = { item -> historyManager.onMenuExport(item) }
        )

        permissionManager = PermissionManager(this)

        historyManager = HistoryManager(
            context = this,
            db = db,
            txtResult = binding.txtResult,
            drawerLayout = binding.drawerLayout,
            historyAdapter = historyAdapter,
            textToSpeechManager = textToSpeechManager,
            translateManager = translateManager,
            permissionManager = permissionManager,
            lifecycleScope = lifecycleScope
        ).also { mgr ->
            mgr.onHistoryCountChanged = { count ->
                if (count == 0) binding.tvHistoryEmpty.visible() else binding.tvHistoryEmpty.gone()
            }
            mgr.onSaveButtonShow = { visible ->
                if (visible) binding.layoutSave.visible() else binding.layoutSave.gone()
            }
            mgr.onTtsControlsShow = { visible ->
                if (visible) binding.cardTtsControls.visible() else binding.cardTtsControls.gone()
            }
            mgr.onCopyRequest = { text -> utilityManager.copyToClipboard(text) }
            mgr.onShowSnackbar = { message -> showSnackbar(message) }
        }

        utilityManager = UtilityManager(this, binding.txtResult)
    }

    private fun setupRecyclerView() {
        binding.historyList.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = historyManager.historyAdapter
            setHasFixedSize(false)
        }
    }

    private fun setupUiInitialState() {
        // txtResult readonly by default
        binding.txtResult.apply {
            isFocusable = false
            isCursorVisible = false
            isLongClickable = false
            keyListener = null
        }
        setResultAreaHint(getString(R.string.ask_me_anything))
        binding.cardTtsControls.gone()
        binding.layoutSave.gone()
        binding.progressBar.gone()
    }

    // ======================================================
    // LISTENERS
    // ======================================================

    private fun setupListeners() {
        // Mic FAB
        binding.btnMic.setOnClickListener {
            translateManager.resetOriginalTextSnapshot()
            if (!audioRecorderManager.isRecording) startRecording()
            else stopRecording()
        }

        // TTS Controls
        binding.btnRepeat.setOnClickListener {
            val text = binding.txtResult.text.toString()
            if (text.isNotEmpty()) textToSpeechManager.speakText(text)
        }
        binding.btnPause.setOnClickListener {
            val text = binding.txtResult.text.toString()
            if (text.isNotEmpty()) textToSpeechManager.pauseText()
        }

        // Action icons
        binding.btnCopy.setOnClickListener {
            val text = binding.txtResult.text.toString()
            if (text.isNotEmpty()) {
                utilityManager.copyToClipboard(text)
                showSnackbar("Đã sao chép!")
            } else {
                showSnackbar("Không có nội dung để sao chép")
            }
        }
        binding.btnShare.setOnClickListener { utilityManager.shareText() }
        binding.btnSearch.setOnClickListener { utilityManager.searchOnGoogle() }
        binding.btnTranslate.setOnClickListener { translateManager.showTargetLanguagePopup() }
        binding.btnImport.setOnClickListener {
            permissionManager.checkStoragePermission()
            serverCommunicationManager.showAudioFilePicker()
        }
        binding.btnSave.setOnClickListener { historyManager.saveEditedContent() }

        // Navigation
        binding.btnSettings.setOnClickListener {
            startActivity(
                Intent(this, SettingsActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
            )
        }
        binding.btnRecent.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    /** Dùng OnBackPressedCallback thay deprecated onBackPressed() */
    private fun setupBackPressedCallback() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    finishAffinity()
                }
            }
        })
    }

    // ======================================================
    // RECORDING STATE
    // ======================================================

    private fun startRecording() {
        // Make txtResult read-only during recording
        binding.txtResult.apply {
            isFocusable = false; isCursorVisible = false
            isLongClickable = false; keyListener = null
        }
        binding.layoutSave.gone()
        binding.cardTtsControls.gone()

        // Red FAB + pulse animation
        binding.btnMic.backgroundTintList = android.content.res.ColorStateList.valueOf(
            ContextCompat.getColor(this, R.color.colorRecording)
        )
        binding.btnMic.startAnimation(
            AnimationUtils.loadAnimation(this, R.anim.pulse)
        )

        audioRecorderManager.startRecording()
    }

    private fun stopRecording() {
        binding.btnMic.clearAnimation()
        binding.btnMic.backgroundTintList = android.content.res.ColorStateList.valueOf(
            ContextCompat.getColor(this, R.color.colorPrimary)
        )
        binding.btnMic.isEnabled = false

        audioRecorderManager.stopRecordingAndSendToServer(
            serverManager = serverCommunicationManager,
            onError = {
                binding.btnMic.isEnabled = true
                binding.btnMic.clearAnimation()
                binding.btnMic.backgroundTintList = android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.colorPrimary)
                )
            }
        )
    }

    // ======================================================
    // UI HELPERS
    // ======================================================

    fun getTextToSpeechManager(): TextToSpeechManager = textToSpeechManager

    private fun setResultAreaHint(hint: String) {
        binding.txtResult.hint = SpannableString(hint).apply {
            setSpan(StyleSpan(android.graphics.Typeface.ITALIC), 0, hint.length, 0)
        }
    }

    /** Snackbar thay Toast cho UI feedback tốt hơn */
    private fun showSnackbar(message: String, duration: Int = Snackbar.LENGTH_SHORT) {
        Snackbar.make(binding.root, message, duration)
            .setBackgroundTint(ContextCompat.getColor(this, R.color.colorSurface))
            .setTextColor(ContextCompat.getColor(this, R.color.colorOnSurface))
            .setActionTextColor(ContextCompat.getColor(this, R.color.colorAccent))
            .show()
    }

    // ======================================================
    // LIFECYCLE
    // ======================================================

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra("fromSettings", false)) {
            val language = intent.getStringExtra("selectedLanguage")
            val speed = intent.getIntExtra("speechSpeed", 50)
            val pitch = intent.getIntExtra("speechPitch", 50)

            language?.let { textToSpeechManager.setDetectedLanguage(it) }
            if (speed >= 0 && pitch >= 0) {
                textToSpeechManager.updateTtsSettings(speed, pitch)
            }
        }
    }

    override fun onDestroy() {
        textToSpeechManager.destroy()
        audioRecorderManager.destroy()
        translateManager.destroy()
        super.onDestroy()
    }
}
