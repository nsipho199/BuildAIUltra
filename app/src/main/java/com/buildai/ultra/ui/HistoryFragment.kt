package com.buildai.ultra.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.buildai.ultra.data.AppDatabase
import com.buildai.ultra.data.BuildHistoryEntity
import com.buildai.ultra.databinding.FragmentHistoryBinding
import com.buildai.ultra.databinding.ItemBuildHistoryBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: HistoryAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = HistoryAdapter { build -> shareBuild(build) }
        binding.historyList.adapter = adapter

        val db = AppDatabase.getInstance(requireContext())
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                db.buildHistoryDao().getAllBuilds().collect { builds ->
                    adapter.submitList(builds)
                    binding.emptyText.visibility = if (builds.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun shareBuild(build: BuildHistoryEntity) {
        val text = build.downloadUrl?.let {
            "Check out my app built with BuildAI Ultra!\nIdea: ${build.idea}\nDownload: $it"
        } ?: "I built \"${build.idea}\" with BuildAI Ultra!"
        try {
            startActivity(Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            })
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Could not open share sheet", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class HistoryAdapter(
    private val onShare: (BuildHistoryEntity) -> Unit
) : ListAdapter<BuildHistoryEntity, HistoryAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBuildHistoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding, onShare)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemBuildHistoryBinding,
        private val onShare: (BuildHistoryEntity) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(build: BuildHistoryEntity) {
            binding.ideaText.text = build.idea
            binding.statusBadge.text = when (build.status) {
                "COMPLETE" -> "Complete"
                "FAILED" -> "Failed"
                else -> build.status
            }
            val context = binding.root.context
            binding.statusBadge.setTextColor(
                if (build.status == "COMPLETE")
                    androidx.core.content.ContextCompat.getColor(context, com.buildai.ultra.R.color.status_complete)
                else
                    androidx.core.content.ContextCompat.getColor(context, com.buildai.ultra.R.color.status_failed)
            )
            val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            binding.dateText.text = dateFormat.format(Date(build.createdAt))
            binding.shareButton.setOnClickListener { onShare(build) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<BuildHistoryEntity>() {
        override fun areItemsTheSame(old: BuildHistoryEntity, new: BuildHistoryEntity): Boolean = old.id == new.id
        override fun areContentsTheSame(old: BuildHistoryEntity, new: BuildHistoryEntity): Boolean = old == new
    }
}
