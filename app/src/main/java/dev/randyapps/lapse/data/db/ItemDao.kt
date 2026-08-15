package dev.randyapps.lapse.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {

    /** Sorted soonest-first, which is the order Home renders in. */
    @Query("SELECT * FROM items ORDER BY expiryDate ASC, name ASC")
    fun observeAll(): Flow<List<ItemEntity>>

    /** Emits null once the item is deleted, so an open edit screen can react. */
    @Query("SELECT * FROM items WHERE id = :id")
    fun observeById(id: Long): Flow<ItemEntity?>

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun getById(id: Long): ItemEntity?

    /** One-shot read for rescheduling reminders after boot, where there's no Flow to collect. */
    @Query("SELECT * FROM items")
    suspend fun getAll(): List<ItemEntity>

    /** Returns the row id, so a freshly inserted item can be scheduled immediately. */
    @Upsert
    suspend fun upsert(item: ItemEntity): Long

    @Delete
    suspend fun delete(item: ItemEntity)

    @Query("DELETE FROM items WHERE id = :id")
    suspend fun deleteById(id: Long)
}
