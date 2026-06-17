package com.buildai.ultra.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class BuildState(
    val isRunning: Boolean = false,
    val progress: Int = 0,
    val phase: BuildPhase = BuildPhase.ANALYZING,
    val phaseProgress: Float = 0f,
    val isComplete: Boolean = false,
    val errorMessage: String? = null,
    val ideaDescription: String = ""
) : Parcelable
