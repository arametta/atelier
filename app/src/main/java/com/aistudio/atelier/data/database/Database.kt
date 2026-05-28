package com.aistudio.atelier.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "bottles")
data class Bottle(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val house: String,
    val name: String,
    val concentration: String, // EDT, EDP, Parfum, EDC, Cologne, Other
    val sizeMl: Int,
    val price: Double,
    val currency: String, // EUR, USD, GBP, AED, CHF
    val purchaseDate: String, // YYYY-MM-DD
    val mlPerSpray: Double = 0.10,
    val imageUrl: String? = null,
    val topNotes: String, // Comma separated
    val middleNotes: String, // Comma separated (Heart notes)
    val baseNotes: String, // Comma separated
    val family: String, // Olfactory family
    val year: Int? = null,
    val description: String,
    val perfumer: String? = null,
    val personalNotes: String
) {
    fun totalSprays(): Int {
        val count = (sizeMl / mlPerSpray).toInt()
        return if (count <= 0) 1000 else count
    }
}

@Entity(tableName = "logs")
data class LogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bottleId: Int,
    val date: String, // YYYY-MM-DD
    val sprays: Int,
    val notes: String
)

@Dao
interface FragranceDao {
    @Query("SELECT * FROM bottles ORDER BY id DESC")
    fun getAllBottles(): Flow<List<Bottle>>

    @Query("SELECT * FROM bottles WHERE id = :id LIMIT 1")
    suspend fun getBottleById(id: Int): Bottle?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBottle(bottle: Bottle): Long

    @Delete
    suspend fun deleteBottle(bottle: Bottle)

    @Query("DELETE FROM bottles WHERE id = :bottleId")
    suspend fun deleteBottleById(bottleId: Int)

    @Query("SELECT * FROM logs ORDER BY date DESC, id DESC")
    fun getAllLogs(): Flow<List<LogEntity>>

    @Query("SELECT * FROM logs WHERE bottleId = :bottleId ORDER BY date DESC, id DESC")
    fun getLogsForBottle(bottleId: Int): Flow<List<LogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: LogEntity): Long

    @Delete
    suspend fun deleteLog(log: LogEntity)

    @Query("DELETE FROM logs WHERE bottleId = :bottleId")
    suspend fun deleteLogsForBottle(bottleId: Int)
}

@Database(entities = [Bottle::class, LogEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun fragranceDao(): FragranceDao
}
