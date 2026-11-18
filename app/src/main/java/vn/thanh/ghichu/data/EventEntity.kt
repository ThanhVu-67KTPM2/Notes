package vn.thanh.ghichu.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val dateTimeMillis: Long,
    val category: String = "Khác",
    val customCategoryName: String = "",
    val note: String = "",
    val reminderMinutes: Int = 60,
    val emoji: String = "🎉",
    val createdAtMillis: Long = System.currentTimeMillis(),
    val timeMode: String = "DATE",
    val relativeDays: Int = 0
) {
    val displayCategory: String get() = if (category == "Khác" && customCategoryName.isNotBlank()) customCategoryName else category
}
