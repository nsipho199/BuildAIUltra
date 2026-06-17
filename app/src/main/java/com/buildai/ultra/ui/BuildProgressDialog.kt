package com.buildai.ultra.ui

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.buildai.ultra.R
import com.buildai.ultra.databinding.DialogBuildProgressBinding
import com.buildai.ultra.model.BuildPhase
import com.buildai.ultra.model.BuildState
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class BuildProgressDialog : BottomSheetDialogFragment() {

    private var _binding: DialogBuildProgressBinding? = null
    private val binding get() = _binding!!

    fun updateState(state: BuildState) {
        binding?.apply {
            val progressText = "${state.progress}%"
            progressLabel.text = progressText
            progressIndicator.progress = state.progress
            phaseText.text = getPhaseLabel(state.phase)
            statusText.text = when {
                state.progress < 100 -> "Building your app…"
                else -> "Complete!"
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

    override fun getTheme(): Int = R.style.BottomSheetDialogTheme

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "BuildProgressDialog"

        fun newInstance(): BuildProgressDialog = BuildProgressDialog()

        private fun getPhaseLabel(phase: BuildPhase): String = when (phase) {
            BuildPhase.ANALYZING -> "Analyzing your idea…"
            BuildPhase.PLANNING -> "Planning architecture…"
            BuildPhase.DESIGNING -> "Designing user interface…"
            BuildPhase.CODING -> "Generating application logic…"
            BuildPhase.DATABASE -> "Creating database schema…"
            BuildPhase.API -> "Building APIs…"
            BuildPhase.NAVIGATION -> "Setting up navigation…"
            BuildPhase.ASSETS -> "Generating assets…"
            BuildPhase.COMPILING -> "Compiling APK…"
        }
    }
}
