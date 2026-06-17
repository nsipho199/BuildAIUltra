package com.buildai.ultra.build

object ApiConfig {
    var serverUrl: String = "http://10.0.2.2:8080"
    const val POLL_INTERVAL_MS: Long = 1000
    const val CONNECT_TIMEOUT_MS: Int = 30000
    const val READ_TIMEOUT_MS: Int = 120000
}
