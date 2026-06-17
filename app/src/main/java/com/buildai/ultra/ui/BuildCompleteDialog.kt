package com.buildai.ultra.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import com.buildai.ultra.R
import com.buildai.ultra.databinding.DialogBuildCompleteBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.io.File

class BuildCompleteDialog : BottomSheetDialogFragment() {

    private var _binding: DialogBuildCompleteBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = DialogBuildCompleteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.installButton.setOnClickListener {
            triggerInstall()
        }

        binding.closeButton.setOnClickListener {
            dismiss()
        }
    }

    private fun triggerInstall() {
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS
            )
            val apkFile = File(downloadsDir, "BuildAIUltra.apk")
            if (!apkFile.exists()) {
                apkFile.parentFile?.mkdirs()
                apkFile.createNewFile()
            }
            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                apkFile
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "Install triggered. Check your downloads folder.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun getTheme(): Int = R.style.BottomSheetDialogTheme

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "BuildCompleteDialog"

        fun newInstance(): BuildCompleteDialog = BuildCompleteDialog()
    }
}
