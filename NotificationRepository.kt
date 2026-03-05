package com.discordnotificationlogger.repository

import androidx.lifecycle.LiveData
import com.discordnotificationlogger.database.NotificationDao
import com.discordnotificationlogger.database.NotificationEntity
import kotlinx.coroutines.flow.Flow

class NotificationRepository(private val dao: NotificationDao) {

    val allNotifications: Flow<List<NotificationEntity>> = dao.getAllNotifications()
    val keywordNotifications: Flow<List<NotificationEntity>> = dao.getKeywordNotifications()
    val allNotificationsLive: LiveData<List<NotificationEntity>> = dao.getAllNotificationsLive()
    val keywordNotificationsLive: LiveData<List<NotificationEntity>> = dao.getKeywordNotificationsLive()
    val notificationCount: LiveData<Int> = dao.getNotificationCount()
    val keywordMatchCount: LiveData<Int> = dao.getKeywordMatchCount()

    suspend fun insert(notification: NotificationEntity): Long = dao.insert(notification)

    suspend fun delete(notification: NotificationEntity) = dao.delete(notification)

    suspend fun deleteAll() = dao.deleteAll()

    suspend fun getRecentNotifications(limit: Int = 500) = dao.getRecentNotifications(limit)
}
