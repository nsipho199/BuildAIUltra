package com.buildai.ultra

import com.buildai.ultra.build.DEMO_PHASES
import com.buildai.ultra.build.DemoBuildPipeline
import com.buildai.ultra.model.BuildPhase
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*

class DemoBuildPipelineTest {

    private val demoPipeline = DemoBuildPipeline()

    @Test
    fun `demo pipeline progresses through all phases`() = runTest {
        val phases = mutableListOf<BuildPhase>()
        val result = demoPipeline.execute("Test app") { progress ->
            phases.add(progress.phase)
        }
        assertTrue("Should end with COMPLETE", result.phase == BuildPhase.COMPLETE)
        assertTrue("Progress should be 1f", result.progress == 1f)
        assertNotNull("Should have download URL", result.downloadUrl)
        assertTrue("APK size should be positive", result.apkSize > 0)
    }

    @Test
    fun `demo pipeline generates realistic APK size`() = runTest {
        val result = demoPipeline.execute("A large enterprise app") { }
        assertTrue("APK size should be between 5MB and 20MB", result.apkSize in 5_000_000..20_000_000)
    }

    @Test
    fun `demo phases cover all BuildPhase values`() {
        val phaseSteps = DEMO_PHASES
        val coveredPhases = phaseSteps.map { it.phase }.toSet()
        assertTrue("ANALYZING should be included", coveredPhases.contains(BuildPhase.ANALYZING))
        assertTrue("PLANNING should be included", coveredPhases.contains(BuildPhase.PLANNING))
        assertTrue("COMPILING should be included", coveredPhases.contains(BuildPhase.COMPILING))

        val progressStarts = phaseSteps.map { it.progressStart }
        val progressEnds = phaseSteps.map { it.progressEnd }
        assertTrue("Progress should be monotonically increasing",
            progressStarts.zipWithNext().all { (a, b) -> a <= b })
        assertTrue("End progresses should be >= start progresses",
            progressStarts.zip(progressEnds).all { (s, e) -> s <= e })
    }
}
