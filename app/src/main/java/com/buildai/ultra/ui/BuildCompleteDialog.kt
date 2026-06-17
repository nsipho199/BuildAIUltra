package com.buildai.ultra.ui

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.buildai.ultra.R
import com.buildai.ultra.databinding.DialogBuildCompleteBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

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

        val downloadUrl = arguments?.getString(ARG_DOWNLOAD_URL) ?: ""
        val apkSize = arguments?.getLong(ARG_APK_SIZE, 0) ?: 0

        val sizeText = when {
            apkSize < 1024 -> "$apkSize B"
            apkSize < 1024 * 1024 -> "${apkSize / 1024} KB"
            else -> "${apkSize / (1024 * 1024)} MB"
        }
        binding.sizeText.text = "APK Size: $sizeText"

        binding.installButton.setOnClickListener {
            downloadApk(downloadUrl)
        }

        binding.shareButton.setOnClickListener {
            shareApk(downloadUrl)
        }

        binding.closeButton.setOnClickListener {
            dismiss()
        }
    }

    private fun downloadApk(url: String) {
        try {
            if (url.startsWith("http")) {
                val request = DownloadManager.Request(Uri.parse(url)).apply {
                    setTitle("BuildAI Ultra APK")
                    setDescription("Downloading your generated app")
                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "BuildAIUltra.apk")
                }
                val manager = requireContext().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                manager.enqueue(request)
                Toast.makeText(requireContext(), R.string.download_started, Toast.LENGTH_SHORT).show()
            } else {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(Uri.parse(url), "application/vnd.android.package-archive")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                startActivity(intent)
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Download URL: $url", Toast.LENGTH_LONG).show()
        }
        dismiss()
    }

    private fun shareApk(url: String) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "Check out my app built with BuildAI Ultra!\nDownload: $url")
            }
            startActivity(Intent.createChooser(intent, "Share APK link"))
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Could not open share sheet", Toast.LENGTH_SHORT).show()
        }
    }

    override fun getTheme(): Int = R.style.BottomSheetDialogTheme

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "BuildCompleteDialog"
        private const val ARG_DOWNLOAD_URL = "download_url"
        private const val ARG_APK_SIZE = "apk_size"

        fun newInstance(downloadUrl: String, apkSize: Long): BuildCompleteDialog {
            val args = Bundle().apply {
                putString(ARG_DOWNLOAD_URL, downloadUrl)
                putLong(ARG_APK_SIZE, apkSize)
            }
            return BuildCompleteDialog().apply { arguments = args }
        }
    }
}
