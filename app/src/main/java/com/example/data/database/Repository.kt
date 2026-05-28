package com.example.data.database

import kotlinx.coroutines.flow.Flow

class FragranceRepository(private val fragranceDao: FragranceDao) {
    val allBottles: Flow<List<Bottle>> = fragranceDao.getAllBottles()
    val allLogs: Flow<List<LogEntity>> = fragranceDao.getAllLogs()

    suspend fun getBottleById(id: Int): Bottle? {
        return fragranceDao.getBottleById(id)
    }

    suspend fun insertBottle(bottle: Bottle): Long {
        return fragranceDao.insertBottle(bottle)
    }

    suspend fun deleteBottle(bottleId: Int) {
        fragranceDao.deleteLogsForBottle(bottleId)
        fragranceDao.deleteBottleById(bottleId)
    }

    fun getLogsForBottle(bottleId: Int): Flow<List<LogEntity>> {
        return fragranceDao.getLogsForBottle(bottleId)
    }

    suspend fun insertLog(log: LogEntity): Long {
        return fragranceDao.insertLog(log)
    }

    suspend fun deleteLog(log: LogEntity) {
        fragranceDao.deleteLog(log)
    }
}
