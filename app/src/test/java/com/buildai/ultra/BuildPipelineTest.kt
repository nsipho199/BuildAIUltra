package com.buildai.ultra

import com.buildai.ultra.build.BuildPipeline
import com.buildai.ultra.model.BuildPhase
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*

class BuildPipelineTest {

    private val pipeline = BuildPipeline()

    @Test
    fun `mapPhase maps server phase strings correctly`() {
        val mappings = mapOf(
            "analyzing your idea" to BuildPhase.ANALYZING,
            "planning application architecture" to BuildPhase.PLANNING,
            "generating user interface" to BuildPhase.UI_GENERATION,
            "generating app code" to BuildPhase.LOGIC_GENERATION,
            "creating database" to BuildPhase.DATABASE,
            "creating apis" to BuildPhase.API_CREATION,
            "creating navigation" to BuildPhase.NAVIGATION,
            "creating settings" to BuildPhase.SETTINGS,
            "generating assets" to BuildPhase.ASSETS,
            "compiling apk" to BuildPhase.COMPILING,
            "app ready" to BuildPhase.COMPLETE,
        )
        val method = BuildPipeline::class.java.getDeclaredMethod("mapPhase", String::class.java)
        method.isAccessible = true
        for ((input, expected) in mappings) {
            val result = method.invoke(pipeline, input)
            assertEquals("Phase mapping for '$input'", expected, result)
        }
    }

    @Test
    fun `mapPhase returns ANALYZING for unknown phase`() {
        val method = BuildPipeline::class.java.getDeclaredMethod("mapPhase", String::class.java)
        method.isAccessible = true
        val result = method.invoke(pipeline, "unknown phase string")
        assertEquals(BuildPhase.ANALYZING, result)
    }

    @Test
    fun `mapPhase is case insensitive`() {
        val method = BuildPipeline::class.java.getDeclaredMethod("mapPhase", String::class.java)
        method.isAccessible = true
        assertEquals(BuildPhase.ANALYZING, method.invoke(pipeline, "ANALYZING YOUR IDEA"))
        assertEquals(BuildPhase.COMPLETE, method.invoke(pipeline, "APP READY"))
        assertEquals(BuildPhase.COMPILING, method.invoke(pipeline, "COMPILING APK"))
    }
}
