package com.buildai.ultra.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.buildai.ultra.R
import com.buildai.ultra.build.ApiConfig
import com.buildai.ultra.data.SettingsManager
import com.buildai.ultra.databinding.FragmentSettingsBinding
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var settingsManager: SettingsManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        settingsManager = SettingsManager(requireContext())

        loadSettings()

        binding.saveServerUrlButton.setOnClickListener {
            val url = binding.serverUrlInput.text.toString().trim()
            if (url.isNotBlank()) {
                lifecycleScope.launch {
                    settingsManager.setServerUrl(url)
                    ApiConfig.serverUrl = url
                    Toast.makeText(requireContext(), R.string.settings_saved, Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.demoModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                settingsManager.setDemoMode(isChecked)
            }
        }
    }

    private fun loadSettings() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    settingsManager.serverUrl.collect { url ->
                        binding.serverUrlInput.setText(url)
                        ApiConfig.serverUrl = url
                    }
                }
                launch {
                    settingsManager.demoMode.collect { enabled ->
                        binding.demoModeSwitch.isChecked = enabled
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
