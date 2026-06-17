package com.buildai.ultra

import com.buildai.ultra.build.ApiConfig
import org.junit.Test
import org.junit.Assert.*

class ApiConfigTest {

    @Test
    fun `default server URL points to localhost emulator`() {
        assertEquals("http://10.0.2.2:8080", ApiConfig.serverUrl)
    }

    @Test
    fun `config values are reasonable`() {
        assertTrue("Poll interval should be >= 500ms", ApiConfig.POLL_INTERVAL_MS >= 500)
        assertTrue("Connect timeout should be >= 5000ms", ApiConfig.CONNECT_TIMEOUT_MS >= 5000)
        assertTrue("Read timeout should be >= 30000ms", ApiConfig.READ_TIMEOUT_MS >= 30000)
    }

    @Test
    fun `server URL can be changed at runtime`() {
        val original = ApiConfig.serverUrl
        ApiConfig.serverUrl = "http://192.168.1.100:8080"
        assertEquals("http://192.168.1.100:8080", ApiConfig.serverUrl)
        ApiConfig.serverUrl = original
    }
}
