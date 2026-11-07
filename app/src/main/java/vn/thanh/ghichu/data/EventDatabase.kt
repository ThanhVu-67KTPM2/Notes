package vn.thanh.ghichu.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [EventEntity::class], version = 1, exportSchema = false)
abstract class EventDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
    companion object {
        @Volatile private var instance: EventDatabase? = null
        fun get(context: Context): EventDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, EventDatabase::class.java, "countdown_events.db").build().also { instance = it }
        }
    }
}
