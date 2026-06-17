package com.buildai.ultra.build

import com.buildai.ultra.model.BuildPhase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

data class BuildProgress(
    val phase: BuildPhase,
    val progress: Float,
    val downloadUrl: String? = null,
    val apkSize: Long = 0,
    val error: String? = null
)

class BuildPipeline(
    private val api: BuildApiService = BuildApiService()
) {

    suspend fun execute(
        idea: String,
        onProgress: (BuildProgress) -> Unit
    ): BuildProgress {
        onProgress(BuildProgress(BuildPhase.ANALYZING, 0f))

        val initialStatus = withContext(Dispatchers.IO) {
            api.startBuild(idea)
        }

        val buildId = initialStatus.buildId

        onProgress(BuildProgress(BuildPhase.ANALYZING, initialStatus.progress / 100f))

        while (true) {
            delay(ApiConfig.POLL_INTERVAL_MS)

            val status = withContext(Dispatchers.IO) {
                api.pollStatus(buildId)
            }

            val progress = status.progress / 100f
            val phase = mapPhase(status.phase)
            onProgress(BuildProgress(phase, progress))

            when (status.status) {
                "COMPLETE" -> {
                    return BuildProgress(
                        phase = BuildPhase.COMPLETE,
                        progress = 1f,
                        downloadUrl = status.downloadUrl,
                        apkSize = status.apkSize
                    )
                }
                "FAILED" -> {
                    throw BuildException(status.error ?: "Build failed")
                }
            }
        }
    }

    private fun mapPhase(serverPhase: String): BuildPhase {
        return when (serverPhase.lowercase()) {
            "analyzing your idea" -> BuildPhase.ANALYZING
            "planning application architecture" -> BuildPhase.PLANNING
            "generating user interface" -> BuildPhase.UI_GENERATION
            "generating app code" -> BuildPhase.LOGIC_GENERATION
            "creating database" -> BuildPhase.DATABASE
            "creating apis" -> BuildPhase.API_CREATION
            "creating navigation" -> BuildPhase.NAVIGATION
            "creating settings" -> BuildPhase.SETTINGS
            "generating assets" -> BuildPhase.ASSETS
            "compiling apk" -> BuildPhase.COMPILING
            "finalizing project" -> BuildPhase.SETTINGS
            "app ready" -> BuildPhase.COMPLETE
            else -> BuildPhase.ANALYZING
        }
    }
}

class BuildException(message: String) : Exception(message)