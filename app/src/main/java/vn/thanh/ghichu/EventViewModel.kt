package vn.thanh.ghichu

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import vn.thanh.ghichu.data.*
import java.util.concurrent.TimeUnit

class EventViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = EventRepository(EventDatabase.get(application).eventDao())
    val events: StateFlow<List<EventEntity>> = repository.events.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    var message = MutableStateFlow<String?>(null)
        private set

    fun save(event: EventEntity) = viewModelScope.launch {
        if (event.title.isBlank()) { message.value = "Vui lòng nhập tên sự kiện"; return@launch }
        if (event.dateTimeMillis < System.currentTimeMillis()) { message.value = "Ngày sự kiện không được ở quá khứ"; return@launch }
        val id = repository.save(event)
        scheduleReminder(event.copy(id = id))
        message.value = "Đã lưu sự kiện"
    }
    fun delete(id: Long) = viewModelScope.launch {
        WorkManager.getInstance(getApplication()).cancelUniqueWork("event_reminder_" + id)
        repository.delete(id)
        message.value = "Đã xóa sự kiện"
    }
    fun clearMessage() { message.value = null }
    private fun scheduleReminder(event: EventEntity) {
        val delay = (event.dateTimeMillis - event.reminderMinutes * 60_000L - System.currentTimeMillis()).coerceAtLeast(0)
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf("title" to event.title, "eventId" to event.id))
            .build()
        WorkManager.getInstance(getApplication()).enqueueUniqueWork("event_reminder_" + event.id, ExistingWorkPolicy.REPLACE, request)
    }
}
