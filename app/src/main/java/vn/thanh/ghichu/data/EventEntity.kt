package vn.thanh.ghichu.data
import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "events")
data class EventEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val title: String, val dateTimeMillis: Long, val category: String = "Khác", val note: String = "", val reminderMinutes: Int = 60, val emoji: String = "🎉")
