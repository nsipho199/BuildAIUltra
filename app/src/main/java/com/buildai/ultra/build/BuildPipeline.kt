package com.buildai.ultra.build

import com.buildai.ultra.model.BuildPhase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

data class BuildProgress(
    val progress: Int,
    val phase: BuildPhase,
    val phaseProgress: Float
)

class BuildPipeline {

    fun execute(idea: String): Flow<BuildProgress> = flow {
        val phases = listOf(
            PhaseConfig(BuildPhase.ANALYZING, 0..10, "Analyzing your idea…"),
            PhaseConfig(BuildPhase.PLANNING, 10..25, "Planning architecture…"),
            PhaseConfig(BuildPhase.DESIGNING, 25..35, "Designing user interface…"),
            PhaseConfig(BuildPhase.CODING, 35..55, "Generating application logic…"),
            PhaseConfig(BuildPhase.DATABASE, 55..65, "Creating database schema…"),
            PhaseConfig(BuildPhase.API, 65..75, "Building APIs…"),
            PhaseConfig(BuildPhase.NAVIGATION, 75..85, "Setting up navigation…"),
            PhaseConfig(BuildPhase.ASSETS, 85..95, "Generating assets…"),
            PhaseConfig(BuildPhase.COMPILING, 95..100, "Compiling APK…")
        )

        val totalIdeaWords = idea.length.coerceIn(10, 5000)
        val baseDelay = (3000L + totalIdeaWords * 2L).coerceAtMost(8000L)

        for ((index, config) in phases.withIndex()) {
            val range = config.progressRange
            val phaseDuration = baseDelay + (index * 200L)
            val steps = 20
            val stepDuration = phaseDuration / steps

            for (step in 1..steps) {
                val stepProgress = step.toFloat() / steps
                val currentProgress = range.first + ((range.last - range.first) * stepProgress).toInt()
                emit(
                    BuildProgress(
                        progress = currentProgress.coerceIn(0, 100),
                        phase = config.phase,
                        phaseProgress = stepProgress
                    )
                )
                delay(stepDuration)
            }
        }
    }

    private data class PhaseConfig(
        val phase: BuildPhase,
        val progressRange: IntRange,
        val displayName: String
    )
}
