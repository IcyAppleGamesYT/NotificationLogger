package com.discordnotificationlogger.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.discordnotificationlogger.database.AppDatabase
import com.discordnotificationlogger.database.NotificationEntity
import com.discordnotificationlogger.repository.NotificationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotificationViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: NotificationRepository
    val allNotificationsLive: LiveData<List<NotificationEntity>>
    val keywordNotificationsLive: LiveData<List<NotificationEntity>>
    val notificationCount: LiveData<Int>
    val keywordMatchCount: LiveData<Int>

    private val _wordFrequency = MutableLiveData<Map<String, Int>>()
    val wordFrequency: LiveData<Map<String, Int>> = _wordFrequency

    init {
        val dao = AppDatabase.getDatabase(application).notificationDao()
        repository = NotificationRepository(dao)
        allNotificationsLive = repository.allNotificationsLive
        keywordNotificationsLive = repository.keywordNotificationsLive
        notificationCount = repository.notificationCount
        keywordMatchCount = repository.keywordMatchCount
    }

    fun insert(notification: NotificationEntity) = viewModelScope.launch(Dispatchers.IO) {
        repository.insert(notification)
    }

    fun delete(notification: NotificationEntity) = viewModelScope.launch(Dispatchers.IO) {
        repository.delete(notification)
    }

    fun deleteAll() = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteAll()
    }

    fun computeWordFrequency() = viewModelScope.launch(Dispatchers.IO) {
        val notifications = repository.getRecentNotifications(500)
        val stopWords = setOf(
            "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for",
            "of", "with", "by", "from", "is", "it", "this", "that", "was", "are",
            "be", "been", "has", "have", "had", "do", "did", "will", "would",
            "could", "should", "may", "might", "new", "your", "you", "i", "me",
            "my", "we", "our", "us", "he", "she", "they", "them", "their",
            "not", "no", "can", "as", "if", "up", "out", "so", "its", "into"
        )

        val wordCount = mutableMapOf<String, Int>()
        notifications.forEach { notif ->
            val combined = "${notif.title} ${notif.text}"
            val words = combined.lowercase()
                .replace(Regex("[^a-z0-9 ]"), " ")
                .split(" ")
                .filter { it.length > 2 && it !in stopWords }

            words.forEach { word ->
                wordCount[word] = (wordCount[word] ?: 0) + 1
            }
        }

        val sorted = wordCount.entries
            .sortedByDescending { it.value }
            .take(15)
            .associate { it.key to it.value }

        withContext(Dispatchers.Main) {
            _wordFrequency.value = sorted
        }
    }
}
