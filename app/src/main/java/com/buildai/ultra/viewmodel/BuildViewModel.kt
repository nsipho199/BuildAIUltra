package com.buildai.ultra.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.buildai.ultra.build.BuildPipeline
import com.buildai.ultra.model.BuildPhase
import com.buildai.ultra.model.BuildState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BuildViewModel(application: Application) : AndroidViewModel(application) {

    private val pipeline = BuildPipeline()

    private val _state = MutableStateFlow(BuildState())
    val state: StateFlow<BuildState> = _state.asStateFlow()

    fun startBuild(idea: String) {
        if (_state.value.isRunning) return
        if (idea.isBlank()) return

        _state.value = BuildState(isRunning = true, ideaDescription = idea)

        viewModelScope.launch {
            pipeline.execute(idea).collect { progress ->
                _state.value = _state.value.copy(
                    isRunning = true,
                    progress = progress.progress,
                    phase = progress.phase,
                    phaseProgress = progress.phaseProgress,
                    isComplete = progress.progress >= 100
                )
            }
        }
    }

    fun reset() {
        _state.value = BuildState()
    }

    fun dismissComplete() {
        val current = _state.value
        _state.value = current.copy(isComplete = false)
    }
}
