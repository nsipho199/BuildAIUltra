package com.buildai.ultra

import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.buildai.ultra.databinding.ActivityMainBinding
import com.buildai.ultra.ui.BuildCompleteDialog
import com.buildai.ultra.ui.BuildProgressDialog
import com.buildai.ultra.viewmodel.BuildViewModel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: BuildViewModel
    private var progressDialog: BuildProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[BuildViewModel::class.java]

        setupWindow()
        setupBuildButton()
        observeState()
    }

    private fun setupWindow() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, binding.root).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    private fun setupBuildButton() {
        binding.buildButton.setOnClickListener {
            val idea = binding.ideaInput.text.toString().trim()

            if (idea.isEmpty()) {
                val shake = AnimationUtils.loadAnimation(this, R.anim.shake)
                binding.ideaInput.startAnimation(shake)
                Toast.makeText(this, R.string.empty_idea_error, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.startBuild(idea)
            showProgressDialog()
        }
    }

    private fun showProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = BuildProgressDialog.newInstance()
        progressDialog?.show(supportFragmentManager, BuildProgressDialog.TAG)
    }

    private fun dismissProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }

    private fun showCompleteDialog(downloadUrl: String, apkSize: Long) {
        dismissProgressDialog()
        val dialog = BuildCompleteDialog.newInstance(downloadUrl, apkSize)
        dialog.show(supportFragmentManager, BuildCompleteDialog.TAG)
    }

    private fun showError(message: String) {
        dismissProgressDialog()
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun observeState() {
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
    }
}