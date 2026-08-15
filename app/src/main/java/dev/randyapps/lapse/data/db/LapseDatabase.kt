package dev.randyapps.lapse.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [ItemEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class LapseDatabase : RoomDatabase() {

    abstract fun itemDao(): ItemDao

    companion object {
        const val NAME = "lapse.db"
    }
}
