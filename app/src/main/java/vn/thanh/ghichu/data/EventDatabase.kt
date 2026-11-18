package vn.thanh.ghichu.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [EventEntity::class], version = 3, exportSchema = false)
abstract class EventDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE events ADD COLUMN customCategoryName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE events ADD COLUMN createdAtMillis INTEGER NOT NULL DEFAULT 0")
            }
        }
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE events ADD COLUMN timeMode TEXT NOT NULL DEFAULT 'DATE'")
                db.execSQL("ALTER TABLE events ADD COLUMN relativeDays INTEGER NOT NULL DEFAULT 0")
            }
        }
        @Volatile private var instance: EventDatabase? = null
        fun get(context: Context): EventDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, EventDatabase::class.java, "countdown_events.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { instance = it }
        }
    }
}
