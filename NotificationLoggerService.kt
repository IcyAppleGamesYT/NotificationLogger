package com.discordnotificationlogger.service

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.discordnotificationlogger.database.AppDatabase
import com.discordnotificationlogger.database.NotificationEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class NotificationLoggerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val TAG = "NotifLoggerService"

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn ?: return

        // Skip our own app's notifications
        if (sbn.packageName == packageName) return

        // Skip system and ongoing notifications
        val notification = sbn.notification
        if (notification.flags and Notification.FLAG_ONGOING_EVENT != 0) return

        val extras = notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim() ?: ""

        val finalText = when {
            bigText.isNotEmpty() -> bigText
            text.isNotEmpty() -> text
            else -> return
        }

        if (title.isEmpty() && finalText.isEmpty()) return

        val appName = getAppName(sbn.packageName)
        val prefs = getSharedPreferences("nl_prefs", Context.MODE_PRIVATE)
        val keywordsJson = prefs.getString("keywords", "[]") ?: "[]"
        val type = object : TypeToken<List<String>>() {}.type
        val keywords: List<String> = try {
            Gson().fromJson(keywordsJson, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        val combinedText = "$title $finalText".lowercase(Locale.getDefault())
        val matchedKeywords = keywords.filter { kw ->
            kw.isNotBlank() && combinedText.contains(kw.trim().lowercase(Locale.getDefault()))
        }
        val isMatch = matchedKeywords.isNotEmpty()

        val entity = NotificationEntity(
            appName = appName,
            appPackage = sbn.packageName,
            title = title,
            text = finalText,
            timestamp = System.currentTimeMillis(),
            matchedKeywords = matchedKeywords.joinToString(", "),
            isKeywordMatch = isMatch
        )

        serviceScope.launch {
            try {
                AppDatabase.getDatabase(applicationContext).notificationDao().insert(entity)
                saveToFile(entity, prefs)
                // Broadcast to update UI in real-time
                val intent = Intent("com.discordnotificationlogger.NEW_NOTIFICATION")
                sendBroadcast(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error saving notification", e)
            }
        }
    }

    private fun getAppName(packageName: String): String {
        return try {
            val pm = packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        } catch (e: Exception) {
            packageName
        }
    }

    private fun saveToFile(entity: NotificationEntity, prefs: android.content.SharedPreferences) {
        val folderUriString = prefs.getString("folder_uri", null) ?: return
        val folderName = prefs.getString("folder_name", "NotificationLogs") ?: "NotificationLogs"

        try {
            val treeUri = Uri.parse(folderUriString)
            val resolver = contentResolver
            val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
            val rootDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, rootDocId)
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, rootDocId)

            // Find or create subfolder
            var subfolderDocId: String? = null
            resolver.query(
                childrenUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE
                ),
                null, null, null
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val name = cursor.getString(1)
                    val mime = cursor.getString(2)
                    if (name == folderName && mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                        subfolderDocId = cursor.getString(0)
                        break
                    }
                }
            }

            val subfolderUri = if (subfolderDocId != null) {
                DocumentsContract.buildDocumentUriUsingTree(treeUri, subfolderDocId!!)
            } else {
                DocumentsContract.createDocument(
                    resolver, rootDocUri,
                    DocumentsContract.Document.MIME_TYPE_DIR,
                    folderName
                )
            } ?: return

            val newSubfolderDocId = DocumentsContract.getDocumentId(subfolderUri)
            val subchildrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, newSubfolderDocId)

            // Use daily log file
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dateStr = sdf.format(Date(entity.timestamp))
            val fileName = "notif_log_$dateStr.txt"

            var fileUri: Uri? = null
            resolver.query(
                subchildrenUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME
                ),
                null, null, null
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val name = cursor.getString(1)
                    if (name == fileName) {
                        val docId = cursor.getString(0)
                        fileUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                        break
                    }
                }
            }

            if (fileUri == null) {
                fileUri = DocumentsContract.createDocument(
                    resolver, subfolderUri, "text/plain", fileName
                )
            }

            fileUri?.let { uri ->
                resolver.openOutputStream(uri, "wa")?.use { stream ->
                    val sdfFull = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    val keywordTag = if (entity.isKeywordMatch) " [🏷 ${entity.matchedKeywords}]" else ""
                    val line = "[${sdfFull.format(Date(entity.timestamp))}] [${entity.appName}] ${entity.title}: ${entity.text}$keywordTag\n"
                    stream.write(line.toByteArray(Charsets.UTF_8))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error writing to file", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
