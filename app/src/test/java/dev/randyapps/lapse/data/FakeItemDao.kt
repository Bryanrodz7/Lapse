package dev.randyapps.lapse.data

import dev.randyapps.lapse.data.db.ItemDao
import dev.randyapps.lapse.data.db.ItemEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory stand-in for the real DAO. Mirrors the real one's contract — sorted by soonest
 * expiry, auto-incrementing ids, upsert-by-id — so repository tests exercise real behaviour
 * without SQLite. The genuine SQL is covered separately by ItemDaoTest on device.
 */
class FakeItemDao(initial: List<ItemEntity> = emptyList()) : ItemDao {

    private val rows = MutableStateFlow(initial)
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0L) + 1

    private fun sorted(items: List<ItemEntity>) =
        items.sortedWith(compareBy({ it.expiryDate }, { it.name }))

    override fun observeAll(): Flow<List<ItemEntity>> = rows.map(::sorted)

    override fun observeById(id: Long): Flow<ItemEntity?> =
        rows.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun getById(id: Long): ItemEntity? = rows.value.firstOrNull { it.id == id }

    override suspend fun getAll(): List<ItemEntity> = sorted(rows.value)

    override suspend fun upsert(item: ItemEntity): Long {
        val id = if (item.id == 0L) nextId++ else item.id
        val stored = item.copy(id = id)
        rows.value = rows.value.filterNot { it.id == id } + stored
        return id
    }

    override suspend fun delete(item: ItemEntity) = deleteById(item.id)

    override suspend fun deleteById(id: Long) {
        rows.value = rows.value.filterNot { it.id == id }
    }
}
