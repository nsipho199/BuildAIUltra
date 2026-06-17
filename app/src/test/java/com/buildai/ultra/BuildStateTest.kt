package com.buildai.ultra

import com.buildai.ultra.model.BuildPhase
import com.buildai.ultra.model.BuildState
import org.junit.Test
import org.junit.Assert.*

class BuildStateTest {

    @Test
    fun `default state is not running`() {
        val state = BuildState()
        assertFalse(state.isRunning)
        assertFalse(state.isComplete)
        assertNull(state.errorMessage)
        assertEquals(0f, state.progress)
        assertEquals(BuildPhase.ANALYZING, state.phase)
    }

    @Test
    fun `build state transitions correctly`() {
        val running = BuildState(isRunning = true, phase = BuildPhase.UI_GENERATION, progress = 0.3f)
        assertTrue(running.isRunning)
        assertEquals(0.3f, running.progress)
        assertEquals(BuildPhase.UI_GENERATION, running.phase)
    }

    @Test
    fun `complete state has download info`() {
        val complete = BuildState(
            isComplete = true,
            downloadUrl = "http://example.com/app.apk",
            apkSize = 10_000_000L
        )
        assertTrue(complete.isComplete)
        assertEquals("http://example.com/app.apk", complete.downloadUrl)
        assertEquals(10_000_000L, complete.apkSize)
    }

    @Test
    fun `error state preserves error message`() {
        val error = BuildState(errorMessage = "Build failed: network error")
        assertEquals("Build failed: network error", error.errorMessage)
    }
}
