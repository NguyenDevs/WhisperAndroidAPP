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
import com.nguyendevs.stkh.repository.RoomHistoryRepository
import com.nguyendevs.stkh.settings.SettingsActivity
import com.nguyendevs.stkh.util.LanguageConstants
import com.nguyendevs.stkh.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: SharedPreferences

    private lateinit var ttsManager: ITextToSpeech
    private lateinit var translator: ITranslator
    private lateinit var audioRecorder: IAudioRecorder
    private lateinit var serverClient: IServerClient
    private lateinit var historyManager: HistoryManager
    private lateinit var permissionManager: PermissionManager
    private lateinit var utilityManager: UtilityManager

    private val audioPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { serverClient.handlePickedAudio(it) } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        wireUpDependencies()
        setupRecyclerView()
        setupUiInitialState()
        setupListeners()
        setupBackPressedCallback()
        historyManager.observeHistory()
        permissionManager.checkAudioPermission()
        permissionManager.checkStoragePermission()
        updateLanguageChip()
    }

    /** Khởi tạo và inject toàn bộ dependencies (Composition Root). */
    private fun wireUpDependencies() {
        val db = AppDatabase.getInstance(this)
        val historyRepository = RoomHistoryRepository(db)

        ttsManager = TextToSpeechManager(this, binding.txtResult).also {
            it.onSpeakStart = {}
            it.onSpeakDone = {}
        }

        translator = TranslateManager(this, binding.txtResult).also { mgr ->
            (mgr as TranslateManager).onTranslated = { text, lang ->
                ttsManager.setDetectedLanguage(lang)
                ttsManager.speakText(text)
            }
        }

        audioRecorder = AudioRecorderManager(this, binding.txtResult, binding.progressBar)
        permissionManager = PermissionManager(this)

        serverClient = ServerCommunicationManager(
            context = this,
            txtResult = binding.txtResult,
            progressBar = binding.progressBar,
            historyRepository = historyRepository,
            ttsManager = ttsManager,
            lifecycleScope = lifecycleScope
        ).also { mgr ->
            mgr as ServerCommunicationManager
            mgr.onTranscriptionSuccess = { _, lang ->
                binding.cardTtsControls.visible()
                binding.cardTtsControls.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in))
                binding.btnMic.isEnabled = true
                updateLanguageChip(lang)
            }
            mgr.onTranscriptionError = {
                binding.btnMic.isEnabled = true
                setResultHint(getString(R.string.ask_me_anything))
            }
            mgr.onShowFilePicker = { audioPickerLauncher.launch("audio/*") }
        }

        val historyAdapter = HistoryAdapter(
            onItemClick = { item -> historyManager.onItemClick(item) },
            onMenuCopy  = { item -> historyManager.onMenuCopy(item) },
            onMenuView  = { item -> historyManager.onMenuView(item).also { binding.cardTtsControls.visible() } },
            onMenuEdit  = { item -> historyManager.onMenuEdit(item) },
            onMenuDelete = { item -> historyManager.onMenuDelete(item) },
            onMenuExport = { item -> historyManager.onMenuExport(item) }
        )

        historyManager = HistoryManager(
            context = this,
            repository = historyRepository,
            txtResult = binding.txtResult,
            drawerLayout = binding.drawerLayout,
            historyAdapter = historyAdapter,
            ttsManager = ttsManager,
            translator = translator,
            permissionManager = permissionManager,
            lifecycleScope = lifecycleScope
        ).also { mgr ->
            mgr.onHistoryCountChanged = { count ->
                if (count == 0) binding.tvHistoryEmpty.visible() else binding.tvHistoryEmpty.gone()
            }
            mgr.onSaveButtonShow = { v -> if (v) binding.layoutSave.visible() else binding.layoutSave.gone() }
            mgr.onTtsControlsShow = { v -> if (v) binding.cardTtsControls.visible() else binding.cardTtsControls.gone() }
            mgr.onCopyRequest = { text -> utilityManager.copyToClipboard(text) }
            mgr.onShowSnackbar = { msg -> showPremiumMessage(msg) }
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
        binding.txtResult.apply {
            setText("")
            isFocusable = false; isCursorVisible = false; isLongClickable = false; keyListener = null
        }
        setResultHint(getString(R.string.ask_me_anything))
        updateLanguageChip() 
        binding.cardTtsControls.gone()
        binding.layoutSave.gone()
        binding.progressBar.gone()
    }

    private fun resetToInitialState() {
        ttsManager.pauseText()
        translator.resetOriginalTextSnapshot()
        setupUiInitialState()
        setResultHint(getString(R.string.ask_me_anything))
    }

    private fun setupListeners() {
        binding.btnMic.setOnClickListener {
            translator.resetOriginalTextSnapshot()
            if (!audioRecorder.isRecording) startRecording() else stopRecording()
        }

        binding.btnRepeat.setOnClickListener {
            binding.txtResult.text.toString().takeIf { it.isNotEmpty() }?.let { ttsManager.speakText(it) }
        }
        binding.btnPause.setOnClickListener {
            binding.txtResult.text.toString().takeIf { it.isNotEmpty() }?.let { ttsManager.pauseText() }
        }
        binding.btnCopy.setOnClickListener {
            val text = binding.txtResult.text.toString()
            if (text.isNotEmpty()) { utilityManager.copyToClipboard(text); showPremiumMessage("Đã sao chép!") }
            else showPremiumMessage("Không có nội dung để sao chép")
        }
        binding.btnShare.setOnClickListener { utilityManager.shareText() }
        binding.btnSearch.setOnClickListener { utilityManager.searchOnGoogle() }
        binding.btnTranslate.setOnClickListener { translator.showTargetLanguagePopup() }
        binding.btnImport.setOnClickListener {
            permissionManager.checkStoragePermission()
            serverClient.showAudioFilePicker()
        }
        binding.btnSave.setOnClickListener { historyManager.saveEditedContent() }
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            })
        }
        binding.btnRecent.setOnClickListener { binding.drawerLayout.openDrawer(GravityCompat.START) }
    }

    private fun setupBackPressedCallback() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                } else if (!binding.txtResult.text.isNullOrEmpty()) {
                    resetToInitialState()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    finishAffinity()
                }
            }
        })
    }

    private fun startRecording() {
        binding.txtResult.apply {
            isFocusable = false; isCursorVisible = false; isLongClickable = false; keyListener = null
        }
        binding.layoutSave.gone()
        binding.cardTtsControls.gone()
        binding.btnMic.apply {
            backgroundTintList = android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(this@MainActivity, R.color.colorRecording)
            )
            startAnimation(AnimationUtils.loadAnimation(this@MainActivity, R.anim.pulse))
        }
        audioRecorder.startRecording()
    }

    private fun stopRecording() {
        binding.btnMic.apply {
            clearAnimation()
            backgroundTintList = android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(this@MainActivity, R.color.colorPrimary)
            )
            isEnabled = false
        }
        audioRecorder.stopRecordingAndSendToServer(
            serverManager = serverClient,
            onError = {
                binding.btnMic.apply {
                    isEnabled = true; clearAnimation()
                    backgroundTintList = android.content.res.ColorStateList.valueOf(
                        ContextCompat.getColor(this@MainActivity, R.color.colorPrimary)
                    )
                }
            }
        )
    }

    private fun setResultHint(hint: String) {
        binding.txtResult.hint = SpannableString(hint).apply {
            setSpan(StyleSpan(android.graphics.Typeface.ITALIC), 0, hint.length, 0)
        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra("fromSettings", false)) {
            val selectedLang = intent.getStringExtra("selectedLanguage")
            selectedLang?.let { ttsManager.setDetectedLanguage(it) }
            updateLanguageChip()
            
            val speed = intent.getIntExtra("speechSpeed", -1)
            val pitch = intent.getIntExtra("speechPitch", -1)
            if (speed >= 0 && pitch >= 0) ttsManager.updateTtsSettings(speed, pitch)
        }
    }

    private fun updateLanguageChip(detectedLang: String? = null) {
        val selectedLang = detectedLang ?: prefs.getString("selectedLanguage", "Vietnamese")
        binding.chipLanguage.text = LanguageConstants.getDisplayName(selectedLang)
    }

    override fun onDestroy() {
        ttsManager.destroy()
        audioRecorder.destroy()
        translator.destroy()
        super.onDestroy()
    }
}
