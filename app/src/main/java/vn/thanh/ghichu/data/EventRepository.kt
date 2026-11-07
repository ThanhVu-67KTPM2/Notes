package vn.thanh.ghichu.data

class EventRepository(private val dao: EventDao) {
    val events = dao.observeAll()
    suspend fun save(event: EventEntity): Long = dao.upsert(event)
    suspend fun delete(id: Long) = dao.deleteById(id)
    suspend fun get(id: Long) = dao.getById(id)
}
