package vn.thanh.ghichu

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import vn.thanh.ghichu.data.*
import vn.thanh.ghichu.settings.SettingsRepository
import vn.thanh.ghichu.settings.ThemeMode
import java.util.concurrent.TimeUnit

class EventViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = EventRepository(EventDatabase.get(application).eventDao())
    private val settings = SettingsRepository(application)
    val events = repository.events.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val theme = settings.theme.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)
    val notificationsEnabled = settings.notifications.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)
    val firstDayOfWeek = settings.firstDayOfWeek.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "Thứ Hai")
    val soundUri = settings.soundUri.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    fun save(event: EventEntity) = viewModelScope.launch {
        if (event.title.isBlank()) { _message.value = "Vui lòng nhập tên sự kiện"; return@launch }
        if (event.dateTimeMillis <= System.currentTimeMillis()) { _message.value = "Ngày và giờ sự kiện phải ở tương lai"; return@launch }
        if (event.category == "Khác" && event.customCategoryName.isBlank()) { _message.value = "Vui lòng nhập tên danh mục"; return@launch }
        val id = repository.save(event.copy(customCategoryName = event.customCategoryName.take(20)))
        scheduleReminder(event.copy(id = id))
        CountdownWidget().updateAll(getApplication())
        _message.value = "Đã lưu sự kiện"
    }
    fun delete(id: Long) = viewModelScope.launch {
        WorkManager.getInstance(getApplication()).cancelUniqueWork("event_reminder_" + id)
        repository.delete(id)
        CountdownWidget().updateAll(getApplication())
        _message.value = "Đã xóa sự kiện"
    }
    fun setTheme(mode: ThemeMode) = viewModelScope.launch { settings.setTheme(mode) }
    fun setFirstDay(value: String) = viewModelScope.launch { settings.setFirstDay(value) }
    fun setNotifications(enabled: Boolean) = viewModelScope.launch {
        settings.setNotifications(enabled)
        if (!enabled) WorkManager.getInstance(getApplication()).cancelAllWorkByTag("event_reminder") else events.value.forEach { scheduleReminder(it) }
    }
    fun setSoundUri(uri: String?) = viewModelScope.launch {
        settings.setSoundUri(uri)
        ReminderWorker.resetNotificationChannels(getApplication())
        _message.value = if (uri == null) "Đã dùng âm thanh mặc định" else "Đã lưu âm thanh thông báo"
    }
    suspend fun findEvent(id: Long): EventEntity? = repository.get(id)
    fun clearMessage() { _message.value = null }
    private fun scheduleReminder(event: EventEntity) {
        WorkManager.getInstance(getApplication()).cancelUniqueWork("event_reminder_" + event.id)
        if (!notificationsEnabled.value) return
        val delay = event.dateTimeMillis - event.reminderMinutes * 60_000L - System.currentTimeMillis()
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay.coerceAtLeast(0), TimeUnit.MILLISECONDS)
            .setInputData(workDataOf("title" to event.title, "eventId" to event.id))
            .addTag("event_reminder")
            .build()
        WorkManager.getInstance(getApplication()).enqueueUniqueWork("event_reminder_" + event.id, ExistingWorkPolicy.REPLACE, request)
    }
}
