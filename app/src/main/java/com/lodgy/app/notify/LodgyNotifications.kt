package com.lodgy.app.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.lodgy.app.MainActivity
import com.lodgy.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Extra carrying the in-app route a notification should land on once the app is unlocked. */
const val EXTRA_NOTIFICATION_ROUTE = "com.lodgy.app.NOTIFICATION_ROUTE"

const val CHANNEL_VACANCY = "vacancy"
const val CHANNEL_DUES = "dues"

/**
 * All notification posting goes through here so the permission check, the channel setup and the
 * tap target are decided once. Every send is best-effort: notifications are a nudge, and a warden
 * who denied the permission must still get a working app (LODGY-59 AC 5).
 */
class LodgyNotifications @Inject constructor(@ApplicationContext private val context: Context) {

    fun ensureChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_VACANCY,
                context.getString(R.string.notify_channel_vacancy),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_DUES,
                context.getString(R.string.notify_channel_dues),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
    }

    fun canPost(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * [route] is the destination to open on tap; null just brings the app up. The route is carried
     * as an extra rather than a deep link because the app is PIN-gated - it has to be replayed
     * after unlock, not navigated to immediately.
     */
    fun post(channelId: String, notificationId: Int, title: String, text: String, route: String?) {
        if (!canPost()) return
        ensureChannels()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            route?.let { putExtra(EXTRA_NOTIFICATION_ROUTE, it) }
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        // Throws only if the permission was revoked between the check above and here; a missed
        // nudge is not worth crashing a warden's app over.
        runCatching { NotificationManagerCompat.from(context).notify(notificationId, notification) }
    }
}
