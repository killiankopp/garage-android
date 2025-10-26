package fr.skit.garage

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import java.util.concurrent.TimeUnit

private val httpClient: OkHttpClient by lazy {
    // WARNING: HEADERS logging will print Authorization header to logcat. Use only for local debugging.
    val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.HEADERS }
    OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .callTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(logging)
        .build()
}

data class GateStatus(
    val status: String,
    val sensorClosed: Boolean = false,
    val sensorOpen: Boolean = false,
    val alertActive: Boolean = false,
    val autoCloseEnabled: Boolean = false,
    val operationTime: Long? = null,
    val timeoutRemaining: Long? = null,
    val autoCloseTime: Long? = null,
    val autoCloseRemaining: Long? = null
)

object ApiClient {
    private fun makeUrl(base: String, path: String): String {
        val b = base.trimEnd('/')
        val p = path.trimStart('/')
        return "$b/$p"
    }

    suspend fun checkHealth(baseUrl: String): Boolean = withContext(Dispatchers.IO) {
        val url = makeUrl(baseUrl, "health")
        val req = Request.Builder()
            .url(url)
            .get()
            .addHeader("Accept", "application/json")
            .build()
        httpClient.newCall(req).execute().use { resp ->
            return@withContext resp.isSuccessful && resp.code == 200
        }
    }

    suspend fun getGateStatus(baseUrl: String): GateStatus? = withContext(Dispatchers.IO) {
        val url = makeUrl(baseUrl, "gate/status")
        val req = Request.Builder()
            .url(url)
            .get()
            .addHeader("Accept", "application/json")
            .build()
        httpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return@withContext null
            val body = resp.body?.string() ?: return@withContext null
            try {
                val j = JSONObject(body)
                val status = j.optString("status", "unknown")
                val sensorClosed = j.optBoolean("sensor_closed", false)
                val sensorOpen = j.optBoolean("sensor_open", false)
                val alertActive = j.optBoolean("alert_active", false)
                val autoCloseEnabled = j.optBoolean("auto_close_enabled", false)
                val operationTime = if (j.has("operation_time")) j.optLong("operation_time") else null
                val timeoutRemaining = if (j.has("timeout_remaining")) j.optLong("timeout_remaining") else null
                val autoCloseTime = if (j.has("auto_close_time")) j.optLong("auto_close_time") else null
                val autoCloseRemaining = if (j.has("auto_close_remaining")) j.optLong("auto_close_remaining") else null
                return@withContext GateStatus(
                    status = status,
                    sensorClosed = sensorClosed,
                    sensorOpen = sensorOpen,
                    alertActive = alertActive,
                    autoCloseEnabled = autoCloseEnabled,
                    operationTime = operationTime,
                    timeoutRemaining = timeoutRemaining,
                    autoCloseTime = autoCloseTime,
                    autoCloseRemaining = autoCloseRemaining
                )
            } catch (_: Exception) {
                return@withContext null
            }
        }
    }

    // Send GET /gate/open with Authorization header. Returns true if request succeeded (HTTP 2xx).
    suspend fun openGate(baseUrl: String, bearerToken: String): Boolean = withContext(Dispatchers.IO) {
        val url = makeUrl(baseUrl, "gate/open")
        val req = Request.Builder()
            .url(url)
            .get()
            .addHeader("Accept", "application/json")
            .addHeader("Authorization", "Bearer $bearerToken")
            .build()
        httpClient.newCall(req).execute().use { resp ->
            return@withContext resp.isSuccessful
        }
    }

    // Send GET /gate/close with Authorization header. Returns true if request succeeded (HTTP 2xx).
    suspend fun closeGate(baseUrl: String, bearerToken: String): Boolean = withContext(Dispatchers.IO) {
        val url = makeUrl(baseUrl, "gate/close")
        val req = Request.Builder()
            .url(url)
            .get()
            .addHeader("Accept", "application/json")
            .addHeader("Authorization", "Bearer $bearerToken")
            .build()
        httpClient.newCall(req).execute().use { resp ->
            return@withContext resp.isSuccessful
        }
    }
}
