package com.example.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R

object AppNotificationManager {
    private const val CHANNEL_UPDATES = "mohamy_updates"
    private const val CHANNEL_ACTIVITY = "mohamy_activity"

    private const val UPDATE_NOTIFICATION_ID = 1001
    private const val DOCUMENT_NOTIFICATION_ID = 1002
    private const val BACKUP_NOTIFICATION_ID = 1003

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channels = listOf(
            NotificationChannel(
                CHANNEL_UPDATES,
                "تحديثات التطبيق",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "تنبيهات تحديثات التطبيق ونسخ GitHub الجديدة"
            },
            NotificationChannel(
                CHANNEL_ACTIVITY,
                "نشاط التطبيق",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "تنبيهات حفظ المستندات والنسخ الاحتياطية"
            }
        )
        channels.forEach(manager::createNotificationChannel)
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            true
        } else {
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    fun notifyUpdateAvailable(
        context: Context,
        versionName: String,
        notes: String,
        releaseTitle: String = ""
    ) {
        postNotification(
            context = context,
            channelId = CHANNEL_UPDATES,
            notificationId = UPDATE_NOTIFICATION_ID,
            title = releaseTitle.ifBlank { "يوجد تحديث جديد للتطبيق" },
            message = buildString {
                append("الإصدار المتاح: ")
                append(versionName.ifBlank { "نسخة جديدة" })
                if (notes.isNotBlank()) {
                    append("\n")
                    append(notes.trim())
                }
            }
        )
    }

    fun notifyDocumentStored(context: Context, fileName: String, caseTitle: String) {
        postNotification(
            context = context,
            channelId = CHANNEL_ACTIVITY,
            notificationId = DOCUMENT_NOTIFICATION_ID,
            title = "تم حفظ مستند جديد",
            message = buildString {
                append(fileName)
                if (caseTitle.isNotBlank()) {
                    append("\n")
                    append("مرتبط بالقضية: ")
                    append(caseTitle)
                }
            }
        )
    }

    fun notifyBackupCreated(context: Context, fileName: String) {
        postNotification(
            context = context,
            channelId = CHANNEL_ACTIVITY,
            notificationId = BACKUP_NOTIFICATION_ID,
            title = "تم إنشاء نسخة احتياطية",
            message = "تم تجهيز ملف النسخة: $fileName"
        )
    }

    fun notifyInfo(context: Context, title: String, message: String, notificationId: Int = 1999) {
        postNotification(
            context = context,
            channelId = CHANNEL_ACTIVITY,
            notificationId = notificationId,
            title = title,
            message = message
        )
    }

    private fun postNotification(
        context: Context,
        channelId: String,
        notificationId: Int,
        title: String,
        message: String
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)

        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or pendingIntentImmutableFlag()
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message.lines().firstOrNull().orEmpty())
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        NotificationManagerCompat.from(context).notify(notificationId, builder.build())
    }

    private fun pendingIntentImmutableFlag(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE
        } else {
            0
        }
    }
}
