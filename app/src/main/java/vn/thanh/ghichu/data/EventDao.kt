package vn.thanh.ghichu.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Query("SELECT * FROM events ORDER BY dateTimeMillis ASC") fun observeAll(): Flow<List<EventEntity>>
    @Query("SELECT * FROM events WHERE id = :id") suspend fun getById(id: Long): EventEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(event: EventEntity): Long
    @Delete suspend fun delete(event: EventEntity)
    @Query("DELETE FROM events WHERE id = :id") suspend fun deleteById(id: Long)
}
