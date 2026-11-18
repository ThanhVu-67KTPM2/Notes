package vn.thanh.ghichu.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.settingsStore by preferencesDataStore(name = "app_settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

class SettingsRepository(private val context: Context) {
    private val themeKey = stringPreferencesKey("theme_mode")
    private val notifyKey = booleanPreferencesKey("notifications_enabled")
    private val weekKey = stringPreferencesKey("first_day_of_week")
    private val soundUriKey = stringPreferencesKey("notification_sound_uri")
    val theme = context.settingsStore.data.map { prefs -> runCatching { ThemeMode.valueOf(prefs[themeKey] ?: "SYSTEM") }.getOrDefault(ThemeMode.SYSTEM) }
    val notifications = context.settingsStore.data.map { it[notifyKey] ?: true }
    val firstDayOfWeek = context.settingsStore.data.map { it[weekKey] ?: "Thứ Hai" }
    val soundUri = context.settingsStore.data.map { it[soundUriKey] }
    suspend fun setTheme(mode: ThemeMode) { context.settingsStore.edit { it[themeKey] = mode.name } }
    suspend fun setNotifications(enabled: Boolean) { context.settingsStore.edit { it[notifyKey] = enabled } }
    suspend fun setFirstDay(value: String) { context.settingsStore.edit { it[weekKey] = value } }
    suspend fun setSoundUri(uri: String?) { context.settingsStore.edit { if (uri == null) it.remove(soundUriKey) else it[soundUriKey] = uri } }
}
