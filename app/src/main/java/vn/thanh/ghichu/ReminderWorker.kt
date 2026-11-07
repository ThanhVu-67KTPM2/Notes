package vn.thanh.ghichu

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val title = inputData.getString("title") ?: "Sự kiện sắp tới"
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(NotificationChannel("event_reminders", "Nhắc nhở sự kiện", NotificationManager.IMPORTANCE_DEFAULT))
        val notification = NotificationCompat.Builder(applicationContext, "event_reminders").setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle("Sự kiện sắp tới").setContentText(title).setAutoCancel(true).build()
        manager.notify(inputData.getLong("eventId", System.currentTimeMillis()).toInt(), notification)
        return Result.success()
    }
}
