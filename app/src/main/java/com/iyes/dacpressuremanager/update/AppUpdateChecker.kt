package com.iyes.dacpressuremanager.update

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

fun interface AppUpdateChecker {
    suspend fun check(currentVersion: String): UpdateCheckResult
}

sealed interface UpdateCheckResult {
    data class Available(
        val version: String,
        val releaseUrl: String,
    ) : UpdateCheckResult

    data object Current : UpdateCheckResult
    data object Failed : UpdateCheckResult
}

class GitHubReleaseUpdateChecker : AppUpdateChecker {
    override suspend fun check(currentVersion: String): UpdateCheckResult = withContext(Dispatchers.IO) {
        runCatching {
            val connection = URL(LATEST_RELEASE_API).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "GET"
                connection.connectTimeout = TIMEOUT_MS
                connection.readTimeout = TIMEOUT_MS
                connection.setRequestProperty("Accept", "application/vnd.github+json")
                connection.setRequestProperty("User-Agent", "DAC-Pressure-Manager-Android")
                if (connection.responseCode !in 200..299) error("HTTP ${connection.responseCode}")
                parseRelease(
                    json = connection.inputStream.bufferedReader().use { it.readText() },
                    currentVersion = currentVersion,
                )
            } finally {
                connection.disconnect()
            }
        }.getOrElse { UpdateCheckResult.Failed }
    }

    internal fun parseRelease(json: String, currentVersion: String): UpdateCheckResult {
        val release = Json.parseToJsonElement(json).jsonObject
        val latestVersion = release.getValue("tag_name").jsonPrimitive.content
            .substringFromFirstDigit()
        val releaseUrl = release.getValue("html_url").jsonPrimitive.content
        return if (compareVersions(latestVersion, currentVersion) > 0) {
            UpdateCheckResult.Available(latestVersion, releaseUrl)
        } else {
            UpdateCheckResult.Current
        }
    }

    internal fun compareVersions(first: String, second: String): Int {
        val firstParts = first.substringFromFirstDigit().numericParts()
        val secondParts = second.substringFromFirstDigit().numericParts()
        return (0 until maxOf(firstParts.size, secondParts.size))
            .firstNotNullOfOrNull { index ->
                val comparison = (firstParts.getOrElse(index) { 0 })
                    .compareTo(secondParts.getOrElse(index) { 0 })
                comparison.takeIf { it != 0 }
            } ?: 0
    }

    private fun String.substringFromFirstDigit(): String =
        dropWhile { !it.isDigit() }

    private fun String.numericParts(): List<Int> =
        split('.', '-', '+').mapNotNull { part ->
            part.takeWhile(Char::isDigit).toIntOrNull()
        }.ifEmpty { listOf(0) }

    private companion object {
        const val LATEST_RELEASE_API =
            "https://api.github.com/repos/IYESUNO/DAC-Pressure-Manager/releases/latest"
        const val TIMEOUT_MS = 6_000
    }
}
