package com.buildai.ultra.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buildai.ultra.build.BuildException
import com.buildai.ultra.build.BuildPipeline
import com.buildai.ultra.build.BuildProgress
import com.buildai.ultra.model.BuildPhase
import com.buildai.ultra.model.BuildState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BuildViewModel : ViewModel() {

    private val pipeline = BuildPipeline()

    private val _state = MutableStateFlow(BuildState())
    val state: StateFlow<BuildState> = _state.asStateFlow()

    fun startBuild(idea: String) {
        if (_state.value.isRunning) return
        if (idea.isBlank()) return

        _state.value = BuildState(isRunning = true, ideaDescription = idea)

        viewModelScope.launch {
            try {
                pipeline.execute(
                    idea = idea,
                    onProgress = { progress ->
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
                )
            } catch (e: BuildException) {
                _state.value = _state.value.copy(
                    isRunning = false,
                    isComplete = false,
                    errorMessage = e.message
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isRunning = false,
                    isComplete = false,
                    errorMessage = "Error: ${e.localizedMessage ?: "Unknown error"}"
                )
            }
        }
    }

    fun reset() {
        _state.value = BuildState()
    }
}