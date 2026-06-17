package com.buildai.ultra.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.buildai.ultra.R
import com.buildai.ultra.databinding.FragmentBuildBinding
import com.buildai.ultra.viewmodel.BuildViewModel
import kotlinx.coroutines.launch

class BuildFragment : Fragment() {

    private var _binding: FragmentBuildBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: BuildViewModel
    private var progressDialog: BuildProgressDialog? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBuildBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[BuildViewModel::class.java]

        binding.buildButton.setOnClickListener {
            val idea = binding.ideaInput.text.toString().trim()
            if (idea.isEmpty()) {
                val shake = AnimationUtils.loadAnimation(requireContext(), R.anim.shake)
                binding.ideaInput.startAnimation(shake)
                Toast.makeText(requireContext(), R.string.empty_idea_error, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.startBuild(idea)
            showProgressDialog()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    progressDialog?.updateState(state)
                    if (state.isComplete && state.downloadUrl != null) {
                        showCompleteDialog(state.downloadUrl, state.apkSize)
                    }
                    if (!state.isRunning && !state.isComplete && state.errorMessage != null) {
                        showError(state.errorMessage)
                    }
                }
            }
        }

        viewModel.reset()
        binding.ideaInput.setText("")
    }

    private fun showProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = BuildProgressDialog.newInstance()
        progressDialog?.show(parentFragmentManager, BuildProgressDialog.TAG)
    }

    private fun dismissProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }

    private fun showCompleteDialog(downloadUrl: String, apkSize: Long) {
        dismissProgressDialog()
        BuildCompleteDialog.newInstance(downloadUrl, apkSize)
            .show(parentFragmentManager, BuildCompleteDialog.TAG)
    }

    private fun showError(message: String) {
        dismissProgressDialog()
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
