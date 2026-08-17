package ai.amani.sample.data.remote

import ai.amani.sample.domain.model.ProfileUrlInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Ports the verify app's QR → profile/url resolution (ProfileURLRepoImp).
 *
 * The scanned QR is a URL carrying `pid` and `server_url` query params. We call
 * `GET {server_url}/api/v2/profile/url?pid={pid}` and read `access_token` + `server_url` from the
 * JSON response — that access token is what the SDK is started with.
 */
class ProfileUrlRemoteDataSource {

    /** Reads a query-param value straight from the raw URL text (no decoding), like the verify app. */
    private fun extractValue(rawUrl: String, key: String): String? = try {
        val query = URL(rawUrl).query ?: return null
        query.split("&").firstOrNull { it.startsWith("$key=") }?.substringAfter("=")
    } catch (e: Exception) {
        null
    }

    suspend fun getProfileUrl(scannedUrl: String): ProfileUrlInfo? = withContext(Dispatchers.IO) {
        val pid = extractValue(scannedUrl, "pid") ?: return@withContext null
        val serverUrl = extractValue(scannedUrl, "server_url") ?: return@withContext null

        val endpoint = "$serverUrl/api/v2/profile/url?pid=$pid"
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("Accept", "application/json")
        }
        try {
            if (connection.responseCode !in 200..299) return@withContext null
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val token = json.optString("access_token", "")
            if (token.isBlank()) return@withContext null
            ProfileUrlInfo(token = token, serverUrl = json.optString("server_url", serverUrl))
        } catch (e: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }
}
