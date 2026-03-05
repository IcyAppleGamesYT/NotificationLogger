package com.discordnotificationlogger.database

import androidx.lifecycle.LiveData
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {

    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE isKeywordMatch = 1 ORDER BY timestamp DESC")
    fun getKeywordNotifications(): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotificationsLive(): LiveData<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE isKeywordMatch = 1 ORDER BY timestamp DESC")
    fun getKeywordNotificationsLive(): LiveData<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: NotificationEntity): Long

    @Delete
    suspend fun delete(notification: NotificationEntity)

    @Query("DELETE FROM notifications")
    suspend fun deleteAll()

    @Query("SELECT * FROM notifications ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentNotifications(limit: Int = 500): List<NotificationEntity>

    @Query("SELECT COUNT(*) FROM notifications")
    fun getNotificationCount(): LiveData<Int>

    @Query("SELECT COUNT(*) FROM notifications WHERE isKeywordMatch = 1")
    fun getKeywordMatchCount(): LiveData<Int>
}
