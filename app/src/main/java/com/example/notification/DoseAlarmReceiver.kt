package com.example.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.database.DoseLogEntity
import com.example.data.database.MedRemindDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DoseAlarmReceiver : BroadcastReceiver() {
    private val TAG = "DoseAlarmReceiver"

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        val action = intent.action
        Log.d(TAG, "onReceive action: $action")

        val scheduleId = intent.getStringExtra("SCHEDULE_ID") ?: ""
        val medicineName = intent.getStringExtra("MEDICINE_NAME") ?: "Medicine"
        val medicineDosage = intent.getStringExtra("MEDICINE_DOSAGE") ?: ""
        val timeStr = intent.getStringExtra("SCHEDULE_TIME") ?: ""
        val notificationId = intent.getIntExtra("NOTIFICATION_ID", scheduleId.hashCode())

        when (action) {
            "com.example.notification.ACTION_DOSE_ALARM" -> {
                // Show medication reminder notification
                showNotification(context, scheduleId, medicineName, medicineDosage, timeStr)

                // Reschedule for tomorrow to keep it recurring daily
                if (scheduleId.isNotEmpty() && timeStr.isNotEmpty()) {
                    DoseAlarmScheduler.scheduleAlarm(
                        context = context,
                        scheduleId = scheduleId,
                        medicineName = medicineName,
                        medicineDosage = medicineDosage,
                        timeStr = timeStr
                    )
                }
            }
            "com.example.notification.ACTION_TAKEN" -> {
                // Dismiss active notification
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                notificationManager?.cancel(notificationId)

                // Update Local Database
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = MedRemindDatabase.getDatabase(context)
                        val schedule = db.scheduleDao().getAllSchedules().find { it.id == scheduleId }
                        val medicineId = schedule?.medicineId ?: ""

                        if (medicineId.isNotEmpty()) {
                            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                            val nowStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
                            
                            val allLogs = db.doseLogDao().getAllLogsFlow().first()
                            val logToUpdate = allLogs.find { 
                                it.medicineId == medicineId && 
                                it.scheduledAt.startsWith(todayStr) && 
                                it.status != "TAKEN"
                            } ?: allLogs.find {
                                it.medicineId == medicineId && 
                                it.scheduledAt.startsWith(todayStr)
                            }

                            if (logToUpdate != null) {
                                db.doseLogDao().updateLogStatus(logToUpdate.id, "TAKEN", nowStr)
                            } else {
                                val newLog = DoseLogEntity(
                                    id = "log_${System.currentTimeMillis()}",
                                    medicineId = medicineId,
                                    scheduledAt = "$todayStr $timeStr",
                                    takenAt = nowStr,
                                    status = "TAKEN"
                                )
                                db.doseLogDao().insertLog(newLog)
                            }

                            // Update stock count
                            val medicine = db.medicineDao().getAllMedicines().find { it.id == medicineId }
                            if (medicine != null && medicine.pillCount > 0) {
                                db.medicineDao().updatePillCount(medicine.id, medicine.pillCount - 1)
                            }

                            launch(Dispatchers.Main) {
                                Toast.makeText(context, "$medicineName dose marked as TAKEN ✅", Toast.LENGTH_LONG).show()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error logging taken dose from action: ${e.message}")
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            "com.example.notification.ACTION_SNOOZE" -> {
                // Cancel current notification
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                notificationManager?.cancel(notificationId)

                // Schedule snooze in 10 minutes
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                if (alarmManager != null) {
                    val snoozeIntent = Intent(context, DoseAlarmReceiver::class.java).apply {
                        this.action = "com.example.notification.ACTION_DOSE_ALARM"
                        putExtra("SCHEDULE_ID", scheduleId)
                        putExtra("MEDICINE_NAME", medicineName)
                        putExtra("MEDICINE_DOSAGE", medicineDosage)
                        putExtra("SCHEDULE_TIME", timeStr)
                    }
                    val requestCode = (scheduleId + "_snoozed").hashCode()
                    val snoozePendingIntent = PendingIntent.getBroadcast(
                        context,
                        requestCode,
                        snoozeIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    val triggerTime = System.currentTimeMillis() + 10 * 60 * 1000 // 10 minutes

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (alarmManager.canScheduleExactAlarms()) {
                            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, snoozePendingIntent)
                        } else {
                            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, snoozePendingIntent)
                        }
                    } else {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, snoozePendingIntent)
                    }

                    Toast.makeText(context, "Medication snoozed for 10 minutes ⏰", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showNotification(
        context: Context,
        scheduleId: String,
        medicineName: String,
        medicineDosage: String,
        timeStr: String
    ) {
        val soundOn = try {
            kotlinx.coroutines.runBlocking {
                com.example.data.datastore.UserSettingsStore(context).soundEnabled.first()
            }
        } catch (e: Exception) {
            true
        }
        val vibrationOn = try {
            kotlinx.coroutines.runBlocking {
                com.example.data.datastore.UserSettingsStore(context).vibrationEnabled.first()
            }
        } catch (e: Exception) {
            true
        }

        val channelId = if (soundOn) {
            if (vibrationOn) {
                "med_remind_doses_sound_vibrate"
            } else {
                "med_remind_doses_sound_no_vibrate"
            }
        } else {
            if (vibrationOn) {
                "med_remind_doses_no_sound_vibrate"
            } else {
                "med_remind_doses_no_sound_no_vibrate"
            }
        }

        val notificationId = scheduleId.hashCode()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelImportance = if (soundOn) {
                NotificationManager.IMPORTANCE_HIGH
            } else {
                NotificationManager.IMPORTANCE_DEFAULT
            }
            val channelName = "Medication Reminders - " + when {
                soundOn && vibrationOn -> "Sound & Vibrate"
                soundOn && !vibrationOn -> "Sound Only"
                !soundOn && vibrationOn -> "Vibrate Only"
                else -> "Silent"
            }
            val channel = NotificationChannel(
                channelId,
                channelName,
                channelImportance
            ).apply {
                description = "Reminders for scheduled medication doses"
                enableLights(true)
                if (!vibrationOn) {
                    vibrationPattern = longArrayOf(0)
                    enableVibration(true)
                } else {
                    enableVibration(true)
                }
                if (!soundOn) {
                    setSound(null, null)
                }
            }
            notificationManager.createNotificationChannel(channel)
        }

        val activityIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action Intents
        val takenIntent = Intent(context, DoseAlarmReceiver::class.java).apply {
            action = "com.example.notification.ACTION_TAKEN"
            putExtra("NOTIFICATION_ID", notificationId)
            putExtra("SCHEDULE_ID", scheduleId)
            putExtra("MEDICINE_NAME", medicineName)
            putExtra("MEDICINE_DOSAGE", medicineDosage)
            putExtra("SCHEDULE_TIME", timeStr)
        }
        val takenPendingIntent = PendingIntent.getBroadcast(
            context,
            (scheduleId + "_taken_act").hashCode(),
            takenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, DoseAlarmReceiver::class.java).apply {
            action = "com.example.notification.ACTION_SNOOZE"
            putExtra("NOTIFICATION_ID", notificationId)
            putExtra("SCHEDULE_ID", scheduleId)
            putExtra("MEDICINE_NAME", medicineName)
            putExtra("MEDICINE_DOSAGE", medicineDosage)
            putExtra("SCHEDULE_TIME", timeStr)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            (scheduleId + "_snooze_act").hashCode(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Time to take your medicine 💊")
            .setContentText("Please take $medicineName ($medicineDosage).")
            .setPriority(if (soundOn) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_save, "Taken", takenPendingIntent)
            .addAction(android.R.drawable.ic_media_play, "Snooze 10 min", snoozePendingIntent)

        if (!vibrationOn) {
            notificationBuilder.setVibrate(longArrayOf(0))
        }
        if (!soundOn) {
            notificationBuilder.setSound(null)
        }

        val notification = notificationBuilder.build()

        notificationManager.notify(notificationId, notification)
    }
}
