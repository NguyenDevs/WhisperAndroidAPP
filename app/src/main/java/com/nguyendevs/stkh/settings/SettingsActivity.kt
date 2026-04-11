package com.nguyendevs.stkh.settings

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nguyendevs.stkh.MainActivity
import com.nguyendevs.stkh.R
import com.nguyendevs.stkh.database.AppDatabase
import com.nguyendevs.stkh.databinding.ActivitySettingsBinding
import com.nguyendevs.stkh.repository.IHistoryRepository
import com.nguyendevs.stkh.repository.RoomHistoryRepository
import com.nguyendevs.stkh.util.LanguageConstants
import com.nguyendevs.stkh.util.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: SharedPreferences
    private lateinit var historyRepository: IHistoryRepository

    private var selectedLanguage = "Vietnamese"
    private var selectedModel = "Medium"
    private var speechSpeed = 50
    private var speechPitch = 50

    companion object {
        private val MODELS = arrayOf("Small", "Medium", "Large")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        historyRepository = RoomHistoryRepository(AppDatabase.getInstance(this))

        restoreSettings()
        setupToolbar()
        setupLanguageSpinner()
        setupModelSpinner()
        setupSliders()
        setupServerIP()
        setupButtons()
        setupBackPressedCallback()
    }

    private fun restoreSettings() {
        selectedLanguage = prefs.getString("selectedLanguage", "Vietnamese") ?: "Vietnamese"
        selectedModel = prefs.getString("selectedModel", "Medium") ?: "Medium"
        speechSpeed = prefs.getInt("speechSpeed", 50)
        speechPitch = prefs.getInt("speechPitch", 50)
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { returnToMainActivity() }
        binding.btnAbout.setOnClickListener { showAboutDialog() }
    }

    private fun setupLanguageSpinner() {
        binding.spinnerLanguage.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, LanguageConstants.DISPLAY_LANGUAGES
        )
        binding.spinnerLanguage.setSelection(LanguageConstants.WHISPER_LANGUAGES.indexOf(selectedLanguage).coerceAtLeast(0))
        binding.spinnerLanguage.onItemSelectedListener = simpleItemSelectedListener { pos ->
            selectedLanguage = LanguageConstants.WHISPER_LANGUAGES[pos]
            prefs.edit().putString("selectedLanguage", selectedLanguage).apply()
        }
    }

    private fun setupModelSpinner() {
        binding.spinnerModel.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, MODELS
        )
        binding.spinnerModel.setSelection(MODELS.indexOf(selectedModel).coerceAtLeast(0))
        binding.spinnerModel.onItemSelectedListener = simpleItemSelectedListener { pos ->
            selectedModel = MODELS[pos]
            prefs.edit().putString("selectedModel", selectedModel).apply()
        }
    }

    private fun setupSliders() {
        binding.sliderSpeechSpeed.value = speechSpeed.toFloat()
        binding.tvSpeedValue.text = speechSpeed.toString()
        binding.sliderSpeechSpeed.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                speechSpeed = value.toInt()
                binding.tvSpeedValue.text = speechSpeed.toString()
                prefs.edit().putInt("speechSpeed", speechSpeed).apply()
            }
        }

        binding.sliderSpeechPitch.value = speechPitch.toFloat()
        binding.tvPitchValue.text = speechPitch.toString()
        binding.sliderSpeechPitch.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                speechPitch = value.toInt()
                binding.tvPitchValue.text = speechPitch.toString()
                prefs.edit().putInt("speechPitch", speechPitch).apply()
            }
        }
    }

    private fun setupServerIP() {
        val savedIp = prefs.getString("serverIP", "14.235.42.182") ?: "14.235.42.182"
        binding.etServerIP.setText(savedIp)
        binding.etServerIP.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                prefs.edit().putString("serverIP", s?.toString()?.trim()).apply()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun setupButtons() {
        binding.btnClearHistory.setOnClickListener { showClearHistoryDialog() }
        binding.btnReset.setOnClickListener { showResetDialog() }
    }

    private fun setupBackPressedCallback() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                returnToMainActivity()
            }
        })
    }

    private fun showAboutDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.dialog_about_title))
            .setMessage(getString(R.string.dialog_about_msg))
            .setPositiveButton(getString(R.string.action_ok)) { d, _ -> d.dismiss() }
            .show()
    }

    private fun showClearHistoryDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.dialog_clear_history_title))
            .setMessage(getString(R.string.dialog_clear_history_msg))
            .setPositiveButton(getString(R.string.action_delete)) { dialog, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    historyRepository.clearAll()
                    runOnUiThread {
                        showToast(getString(R.string.toast_history_cleared))
                        dialog.dismiss()
                        returnToMainActivity()
                    }
                }
            }
            .setNegativeButton(getString(R.string.action_cancel), null)
            .show()
    }

    private fun showResetDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.dialog_reset_title))
            .setMessage(getString(R.string.dialog_reset_msg))
            .setPositiveButton(getString(R.string.action_reset)) { dialog, _ ->
                selectedLanguage = "Vietnamese"
                selectedModel = "Medium"
                speechSpeed = 50
                speechPitch = 50
                val defaultIp = "14.235.42.182"

                prefs.edit()
                    .putString("selectedLanguage", selectedLanguage)
                    .putString("selectedModel", selectedModel)
                    .putInt("speechSpeed", speechSpeed)
                    .putInt("speechPitch", speechPitch)
                    .putString("serverIP", defaultIp)
                    .apply()

                setupLanguageSpinner()
                setupModelSpinner()
                binding.sliderSpeechSpeed.value = speechSpeed.toFloat()
                binding.tvSpeedValue.text = speechSpeed.toString()
                binding.sliderSpeechPitch.value = speechPitch.toFloat()
                binding.tvPitchValue.text = speechPitch.toString()
                binding.etServerIP.setText(defaultIp)

                showToast(getString(R.string.toast_reset_done))
                dialog.dismiss()
                returnToMainActivity()
            }
            .setNegativeButton(getString(R.string.action_cancel), null)
            .show()
    }

    private fun returnToMainActivity() {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("fromSettings", true)
            putExtra("selectedLanguage", selectedLanguage)
            putExtra("selectedModel", selectedModel)
            putExtra("speechSpeed", speechSpeed)
            putExtra("speechPitch", speechPitch)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        finish()
    }
    private fun simpleItemSelectedListener(
        onSelected: (position: Int) -> Unit
    ): android.widget.AdapterView.OnItemSelectedListener {
        return object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: android.widget.AdapterView<*>?,
                view: android.view.View?,
                position: Int,
                id: Long
            ) = onSelected(position)

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }
}
