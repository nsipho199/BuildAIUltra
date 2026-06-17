package com.buildai.ultra.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buildai.ultra.build.BuildException
import com.buildai.ultra.build.BuildPipeline
import com.buildai.ultra.build.BuildProgress
import com.buildai.ultra.data.AppDatabase
import com.buildai.ultra.data.BuildHistoryEntity
import com.buildai.ultra.data.SettingsManager
import com.buildai.ultra.build.DemoBuildPipeline
import com.buildai.ultra.model.BuildPhase
import com.buildai.ultra.model.BuildState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

class BuildViewModel : ViewModel() {

    private val pipeline = BuildPipeline()
    private val demoPipeline = DemoBuildPipeline()

    private val _state = MutableStateFlow(BuildState())
    val state: StateFlow<BuildState> = _state.asStateFlow()

    private var settingsManager: SettingsManager? = null
    private var appDatabase: AppDatabase? = null

    fun init(settingsManager: SettingsManager, appDatabase: AppDatabase) {
        this.settingsManager = settingsManager
        this.appDatabase = appDatabase
    }

    fun startBuild(idea: String) {
        if (_state.value.isRunning) return
        if (idea.isBlank()) return

        _state.value = BuildState(isRunning = true, ideaDescription = idea)

        viewModelScope.launch {
            val isDemo = settingsManager?.demoMode?.first() ?: true
            val buildId = UUID.randomUUID().toString().substring(0, 8)

            try {
                val result = if (isDemo) {
                    demoPipeline.execute(idea = idea) { progress ->
                        updateState(progress)
                    }
                } else {
                    pipeline.execute(idea = idea) { progress ->
                        updateState(progress)
                    }
                }

                _state.value = _state.value.copy(
                    isRunning = false,
                    isComplete = true,
                    phase = BuildPhase.COMPLETE,
                    progress = 1f,
                    downloadUrl = result.downloadUrl,
                    apkSize = result.apkSize,
                    errorMessage = null
                )

                saveToHistory(buildId, idea, "COMPLETE", result.downloadUrl, result.apkSize, null)

            } catch (e: BuildException) {
                _state.value = _state.value.copy(
                    isRunning = false,
                    isComplete = false,
                    errorMessage = e.message
                )
                saveToHistory(buildId, idea, "FAILED", null, 0, e.message)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isRunning = false,
                    isComplete = false,
                    errorMessage = "Error: ${e.localizedMessage ?: "Unknown error"}"
                )
                saveToHistory(buildId, idea, "FAILED", null, 0, e.localizedMessage)
            }
        }
    }

    private fun updateState(progress: BuildProgress) {
        _state.value = _state.value.copy(
            isRunning = true,
            phase = progress.phase,
            progress = progress.progress,
            isComplete = progress.phase == BuildPhase.COMPLETE,
            downloadUrl = progress.downloadUrl,
            apkSize = progress.apkSize,
            errorMessage = null
        )
    }

    private suspend fun saveToHistory(
        id: String, idea: String, status: String,
        downloadUrl: String?, apkSize: Long, error: String?
    ) {
        try {
            appDatabase?.buildHistoryDao()?.insertBuild(
                BuildHistoryEntity(
                    id = id,
                    idea = idea.take(200),
                    status = status,
                    downloadUrl = downloadUrl,
                    apkSize = apkSize,
                    errorMessage = error?.take(500),
                    createdAt = System.currentTimeMillis()
                )
            )
        } catch (_: Exception) {}
    }

    fun reset() {
        _state.value = BuildState()
    }
}
