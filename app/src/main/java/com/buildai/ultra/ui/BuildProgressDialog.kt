package com.buildai.ultra.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.buildai.ultra.databinding.DialogBuildProgressBinding
import com.buildai.ultra.model.BuildPhase
import com.buildai.ultra.model.BuildState
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class BuildProgressDialog : BottomSheetDialogFragment() {

    private var _binding: DialogBuildProgressBinding? = null
    private val binding get() = _binding!!

    fun updateState(state: BuildState) {
        binding.apply {
            val progressPercent = (state.progress * 100).toInt().coerceIn(0, 100)
            progressLabel.text = "$progressPercent%"
            progressIndicator.progress = progressPercent
            phaseText.text = getPhaseLabel(state.phase)
            statusText.text = when {
                state.progress < 0.3f -> "Analyzing your idea…"
                state.progress < 0.5f -> "Generating app code…"
                state.progress < 0.8f -> "Creating resources…"
                state.progress < 1f -> "Compiling APK…"
                else -> "App ready!"
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = DialogBuildProgressBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isCancelable = false
        dialog?.setCanceledOnTouchOutside(false)
    }

    override fun getTheme(): Int = com.buildai.ultra.R.style.BottomSheetDialogTheme

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "BuildProgressDialog"

        fun newInstance(): BuildProgressDialog = BuildProgressDialog()

        private fun getPhaseLabel(phase: BuildPhase): String = when (phase) {
            BuildPhase.ANALYZING -> "Analyzing your idea"
            BuildPhase.PLANNING -> "Planning architecture"
            BuildPhase.UI_GENERATION -> "Generating UI"
            BuildPhase.LOGIC_GENERATION -> "Writing app logic"
            BuildPhase.DATABASE -> "Creating database"
            BuildPhase.API_CREATION -> "Creating APIs"
            BuildPhase.NAVIGATION -> "Setting up navigation"
            BuildPhase.SETTINGS -> "Configuring settings"
            BuildPhase.ASSETS -> "Generating assets"
            BuildPhase.COMPILING -> "Compiling APK"
            BuildPhase.COMPLETE -> "App Ready"
        }
    }
}