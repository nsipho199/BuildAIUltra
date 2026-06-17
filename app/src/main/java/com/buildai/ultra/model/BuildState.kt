package com.buildai.ultra.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class BuildState(
    val isRunning: Boolean = false,
    val phase: BuildPhase = BuildPhase.ANALYZING,
    val progress: Float = 0f,
    val isComplete: Boolean = false,
    val errorMessage: String? = null,
    val ideaDescription: String = "",
    val downloadUrl: String? = null,
    val apkSize: Long = 0
) : Parcelable
