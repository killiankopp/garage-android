package fr.skit.garage

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val PREFS_NAME = "settings"

class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _url = MutableStateFlow(prefs.getString("url", "") ?: "")
    private val _token = MutableStateFlow(prefs.getString("token", "") ?: "")
    private val _openTime = MutableStateFlow(prefs.getInt("open_time_seconds", 5))
    private val _closeTime = MutableStateFlow(prefs.getInt("close_time_seconds", 5))

    val urlFlow: StateFlow<String> = _url.asStateFlow()
    val tokenFlow: StateFlow<String> = _token.asStateFlow()
    val openTimeFlow: StateFlow<Int> = _openTime.asStateFlow()
    val closeTimeFlow: StateFlow<Int> = _closeTime.asStateFlow()

    suspend fun saveUrl(url: String) {
        prefs.edit().putString("url", url).apply()
        _url.value = url
    }

    suspend fun saveToken(token: String) {
        prefs.edit().putString("token", token).apply()
        _token.value = token
    }

    suspend fun saveOpenTime(seconds: Int) {
        prefs.edit().putInt("open_time_seconds", seconds).apply()
        _openTime.value = seconds
    }

    suspend fun saveCloseTime(seconds: Int) {
        prefs.edit().putInt("close_time_seconds", seconds).apply()
        _closeTime.value = seconds
    }
}
