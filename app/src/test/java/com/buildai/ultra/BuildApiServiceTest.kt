package com.buildai.ultra

import com.buildai.ultra.build.BuildApiService
import com.buildai.ultra.build.BuildStatus
import org.junit.Test
import org.junit.Assert.*

class BuildApiServiceTest {

    private val apiService = BuildApiService()

    @Test
    fun `parseBuildStatus extracts all fields correctly`() {
        val json = """{
            "build_id": "abc123",
            "status": "COMPLETE",
            "progress": 100,
            "phase": "App ready",
            "download_url": "http://example.com/abc123.apk",
            "apk_size": 5000000,
            "error": null
        }"""

        val method = BuildApiService::class.java.getDeclaredMethod("parseBuildStatus", String::class.java)
        method.isAccessible = true
        val result = method.invoke(apiService, json) as BuildStatus

        assertEquals("abc123", result.buildId)
        assertEquals("COMPLETE", result.status)
        assertEquals(100, result.progress)
        assertEquals("App ready", result.phase)
        assertEquals("http://example.com/abc123.apk", result.downloadUrl)
        assertEquals(5_000_000L, result.apkSize)
    }

    @Test
    fun `parseBuildStatus handles missing optional fields`() {
        val json = """{
            "build_id": "abc123",
            "status": "RUNNING",
            "progress": 50,
            "phase": "Compiling"
        }"""

        val method = BuildApiService::class.java.getDeclaredMethod("parseBuildStatus", String::class.java)
        method.isAccessible = true
        val result = method.invoke(apiService, json) as BuildStatus

        assertEquals("abc123", result.buildId)
        assertNull("downloadUrl should be null", result.downloadUrl)
        assertEquals(0L, result.apkSize)
        assertNull("error should be null", result.error)
    }

    @Test
    fun `parseBuildStatus handles error response`() {
        val json = """{
            "build_id": "abc123",
            "status": "FAILED",
            "progress": 50,
            "phase": "Compiling",
            "error": "Compilation error: missing dependencies"
        }"""

        val method = BuildApiService::class.java.getDeclaredMethod("parseBuildStatus", String::class.java)
        method.isAccessible = true
        val result = method.invoke(apiService, json) as BuildStatus

        assertEquals("FAILED", result.status)
        assertEquals("Compilation error: missing dependencies", result.error)
    }
}
