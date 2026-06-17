package com.buildai.ultra.build

import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class BuildApiException(message: String, val code: Int = -1) : Exception(message)

data class BuildStatus(
    val buildId: String,
    val status: String,
    val progress: Int,
    val phase: String,
    val downloadUrl: String? = null,
    val apkSize: Long = 0,
    val error: String? = null
)

class BuildApiService {

    fun startBuild(idea: String): BuildStatus {
        val url = URL("${ApiConfig.serverUrl}/api/build")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.connectTimeout = ApiConfig.CONNECT_TIMEOUT_MS
        conn.readTimeout = ApiConfig.READ_TIMEOUT_MS
        conn.setRequestProperty("Content-Type", "application/json")

        val body = JSONObject().apply {
            put("idea", idea)
        }.toString()

        OutputStreamWriter(conn.outputStream).use { it.write(body) }

        val code = conn.responseCode
        if (code != 200) {
            val error = try { conn.errorStream.bufferedReader().readText() } catch (e: Exception) { "Unknown error" }
            throw BuildApiException("Server returned $code: $error", code)
        }

        val responseText = conn.inputStream.bufferedReader().readText()
        return parseBuildStatus(responseText)
    }

    fun pollStatus(buildId: String): BuildStatus {
        val url = URL("${ApiConfig.serverUrl}/api/build/$buildId")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = ApiConfig.CONNECT_TIMEOUT_MS
        conn.readTimeout = ApiConfig.READ_TIMEOUT_MS

        val code = conn.responseCode
        if (code != 200) {
            val error = try { conn.errorStream.bufferedReader().readText() } catch (e: Exception) { "Unknown error" }
            throw BuildApiException("Server returned $code: $error", code)
        }

        val responseText = conn.inputStream.bufferedReader().readText()
        return parseBuildStatus(responseText)
    }

    private fun parseBuildStatus(json: String): BuildStatus {
        val obj = JSONObject(json)
        val download = if (obj.has("download_url") && !obj.isNull("download_url"))
            obj.optString("download_url", "") else null
        val err = if (obj.has("error") && !obj.isNull("error"))
            obj.optString("error", "") else null
        return BuildStatus(
            buildId = obj.optString("build_id", ""),
            status = obj.optString("status", ""),
            progress = obj.optInt("progress", 0),
            phase = obj.optString("phase", ""),
            downloadUrl = download,
            apkSize = obj.optLong("apk_size", 0),
            error = err
        )
    }
}