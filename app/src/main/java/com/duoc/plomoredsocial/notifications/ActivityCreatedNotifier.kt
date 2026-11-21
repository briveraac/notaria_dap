package com.duoc.plomoredsocial.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.duoc.plomoredsocial.MainActivity
import com.duoc.plomoredsocial.R
import com.duoc.plomoredsocial.model.ActivityEntity

/** Aviso local; nunca participa en la transacción Room ni impide crear la actividad. */
class ActivityCreatedNotifier(private val context: Context) {
    fun show(activity: ActivityEntity): Boolean {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(NotificationChannel(
                CHANNEL_ID, "Actividades creadas", NotificationManager.IMPORTANCE_DEFAULT
            ))
        }
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(context, activity.id.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_activity_notification)
            .setContentTitle("Actividad creada")
            .setContentText(activity.title)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        return try {
            // El mismo ID reemplaza el aviso si se reintenta, sin duplicarlo.
            manager.notify(activity.id.hashCode(), notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    companion object {
        const val CHANNEL_ID = "mygos_activity_created"
    }
}
