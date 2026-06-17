package com.buildai.ultra

import com.buildai.ultra.model.BuildPhase
import org.junit.Test
import org.junit.Assert.*

class BuildPhaseTest {

    @Test
    fun `all build phases exist`() {
        val phases = BuildPhase.values()
        assertTrue(phases.contains(BuildPhase.ANALYZING))
        assertTrue(phases.contains(BuildPhase.PLANNING))
        assertTrue(phases.contains(BuildPhase.UI_GENERATION))
        assertTrue(phases.contains(BuildPhase.LOGIC_GENERATION))
        assertTrue(phases.contains(BuildPhase.DATABASE))
        assertTrue(phases.contains(BuildPhase.API_CREATION))
        assertTrue(phases.contains(BuildPhase.NAVIGATION))
        assertTrue(phases.contains(BuildPhase.SETTINGS))
        assertTrue(phases.contains(BuildPhase.ASSETS))
        assertTrue(phases.contains(BuildPhase.COMPILING))
        assertTrue(phases.contains(BuildPhase.COMPLETE))
        assertEquals("Should have 11 phases", 11, phases.size)
    }

    @Test
    fun `phase ordering is correct`() {
        val phases = BuildPhase.values()
        assertEquals(BuildPhase.ANALYZING, phases[0])
        assertEquals(BuildPhase.COMPLETE, phases[phases.size - 1])
    }
}
