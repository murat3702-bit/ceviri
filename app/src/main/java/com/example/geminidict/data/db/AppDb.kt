package com.example.geminidict.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity
data class HistoryItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val query: String,
    val result: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface HistoryDao {
    @Query("SELECT * FROM HistoryItem ORDER BY createdAt DESC LIMIT 100")
    fun observeAll(): Flow<List<HistoryItem>>

    @Insert
    suspend fun insert(item: HistoryItem)

    @Query("DELETE FROM HistoryItem")
    suspend fun clear()
}

@Database(entities = [HistoryItem::class], version = 1, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
}
