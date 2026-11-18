package vn.thanh.ghichu

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.app.PendingIntent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import vn.thanh.ghichu.settings.SettingsRepository

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val title = inputData.getString("title") ?: "Sự kiện sắp tới"
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!androidx.core.app.NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()) return Result.success()
        val settings = SettingsRepository(applicationContext)
        val selectedUri = settings.soundUri.first()?.let(Uri::parse)
        val soundUri = selectedUri?.takeIf { canReadUri(it) } ?: run {
            if (selectedUri != null) settings.setSoundUri(null)
            RingtoneUri.defaultUri
        }
        try {
            ensureChannel(manager, soundUri)
            val eventId = inputData.getLong("eventId", -1L)
            val contentIntent = eventId.takeIf { it > 0 }?.let { id ->
                val intent = Intent(applicationContext, MainActivity::class.java).apply {
                    action = ACTION_OPEN_EVENT
                    data = Uri.parse("ghichu://event/$id")
                    putExtra(EXTRA_EVENT_ID, id)
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                PendingIntent.getActivity(
                    applicationContext,
                    id.hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }
            val notificationBuilder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Sự kiện sắp tới")
                .setContentText(title)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            contentIntent?.let(notificationBuilder::setContentIntent)
            val notification = notificationBuilder.build()
            manager.notify(inputData.getLong("eventId", System.currentTimeMillis()).toInt(), notification)
        } catch (_: Exception) {
            settings.setSoundUri(null)
            ensureChannel(manager, RingtoneUri.defaultUri)
            val eventId = inputData.getLong("eventId", -1L)
            val intent = Intent(applicationContext, MainActivity::class.java).apply {
                action = ACTION_OPEN_EVENT
                data = Uri.parse("ghichu://event/$eventId")
                putExtra(EXTRA_EVENT_ID, eventId)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val contentIntent = PendingIntent.getActivity(
                applicationContext, eventId.hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Sự kiện sắp tới")
                .setContentText(title)
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .build()
            manager.notify(eventId.toInt(), notification)
        }
        return Result.success()
    }

    private fun canReadUri(uri: Uri): Boolean = try { applicationContext.contentResolver.openAssetFileDescriptor(uri, "r")?.use { true } ?: false } catch (_: Exception) { false }

    companion object {
        const val CHANNEL_ID = "event_reminders"
        const val ACTION_OPEN_EVENT = "vn.thanh.ghichu.action.OPEN_EVENT"
        const val EXTRA_EVENT_ID = "vn.thanh.ghichu.extra.EVENT_ID"
        private object RingtoneUri { val defaultUri: Uri get() = Uri.parse("android.resource://vn.thanh.ghichu/${R.raw.ringtone}") }
        fun resetNotificationChannels(context: Context) {
            if (Build.VERSION.SDK_INT >= 26) (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).deleteNotificationChannel(CHANNEL_ID)
        }
        private fun ensureChannel(manager: NotificationManager, sound: Uri) {
            if (Build.VERSION.SDK_INT < 26) return
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(CHANNEL_ID, "Nhắc nhở sự kiện", NotificationManager.IMPORTANCE_DEFAULT)
                channel.setSound(sound, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                manager.createNotificationChannel(channel)
            }
        }
    }
}
