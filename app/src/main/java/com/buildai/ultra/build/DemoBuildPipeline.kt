package com.buildai.ultra.build

import com.buildai.ultra.model.BuildPhase
import kotlinx.coroutines.delay

data class PhaseStep(
    val phase: BuildPhase,
    val progressStart: Float,
    val progressEnd: Float,
    val description: String
)

val DEMO_PHASES = listOf(
    PhaseStep(BuildPhase.ANALYZING, 0f, 0.05f, "Analyzing your idea"),
    PhaseStep(BuildPhase.PLANNING, 0.05f, 0.10f, "Planning application architecture"),
    PhaseStep(BuildPhase.UI_GENERATION, 0.10f, 0.30f, "Generating user interface components"),
    PhaseStep(BuildPhase.LOGIC_GENERATION, 0.30f, 0.50f, "Implementing app logic and features"),
    PhaseStep(BuildPhase.DATABASE, 0.50f, 0.60f, "Setting up database schema"),
    PhaseStep(BuildPhase.API_CREATION, 0.60f, 0.70f, "Creating API endpoints"),
    PhaseStep(BuildPhase.NAVIGATION, 0.70f, 0.78f, "Setting up navigation"),
    PhaseStep(BuildPhase.SETTINGS, 0.78f, 0.85f, "Configuring settings"),
    PhaseStep(BuildPhase.ASSETS, 0.85f, 0.92f, "Generating app assets and icons"),
    PhaseStep(BuildPhase.COMPILING, 0.92f, 0.99f, "Compiling APK"),
)

class DemoBuildPipeline {

    suspend fun execute(
        @Suppress("UNUSED_PARAMETER") idea: String,
        onProgress: (BuildProgress) -> Unit
    ): BuildProgress {
        for (step in DEMO_PHASES) {
            val stepDuration = ((step.progressEnd - step.progressStart) * 8000).toLong().coerceIn(300, 1500)
            onProgress(BuildProgress(step.phase, step.progressStart))
            delay(stepDuration)
            onProgress(BuildProgress(step.phase, step.progressEnd))
        }
        onProgress(BuildProgress(BuildPhase.COMPLETE, 1f))
        delay(300)
        val mockSize = (5_000_000L..20_000_000L).random()
        return BuildProgress(
            phase = BuildPhase.COMPLETE,
            progress = 1f,
            downloadUrl = "https://buildai.ultra/download/demo.apk",
            apkSize = mockSize
        )
    }
}
