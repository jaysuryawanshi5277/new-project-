package com.example.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.database.MedRemindDatabase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DoseAlarmScheduler {
    private const val TAG = "DoseAlarmScheduler"

    /**
     * Schedules an alarm with the Android AlarmManager for a specific medicine schedule.
     */
    fun scheduleAlarm(
        context: Context,
        scheduleId: String,
        medicineName: String,
        medicineDosage: String,
        timeStr: String
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val parsedTime = parseTimeStr(timeStr) ?: return
        val (hour, minute) = parsedTime

        val intent = Intent(context, DoseAlarmReceiver::class.java).apply {
            action = "com.example.notification.ACTION_DOSE_ALARM"
            putExtra("SCHEDULE_ID", scheduleId)
            putExtra("MEDICINE_NAME", medicineName)
            putExtra("MEDICINE_DOSAGE", medicineDosage)
            putExtra("SCHEDULE_TIME", timeStr)
        }

        val requestCode = scheduleId.hashCode()

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            
            // If the time has already passed today, schedule it for tomorrow
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
            Log.d(TAG, "Scheduled alarm for $medicineName ($medicineDosage) at $timeStr (mills: ${calendar.timeInMillis})")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException scheduling exact alarm: ${e.message}, falling back to setAndAllowWhileIdle")
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling alarm: ${e.message}")
        }
    }

    /**
     * Cancels an alarm.
     */
    fun cancelAlarm(context: Context, scheduleId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, DoseAlarmReceiver::class.java).apply {
            action = "com.example.notification.ACTION_DOSE_ALARM"
        }
        val requestCode = scheduleId.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Cancelled alarm for schedule ID: $scheduleId")
        }
    }

    /**
     * Loads and schedules/reschedules all active alarms from the database.
     */
    fun scheduleAllFromDb(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = MedRemindDatabase.getDatabase(context)
                val medicines = db.medicineDao().getAllMedicines()
                val schedules = db.scheduleDao().getAllSchedules()

                Log.d(TAG, "Scheduling all alarms from DB. Size: ${schedules.size}")
                for (schedule in schedules) {
                    val med = medicines.find { it.id == schedule.medicineId }
                    if (med != null && med.isActive) {
                        scheduleAlarm(
                            context = context,
                            scheduleId = schedule.id,
                            medicineName = med.name,
                            medicineDosage = med.dosage,
                            timeStr = schedule.scheduledTime
                        )
                    } else {
                        cancelAlarm(context, schedule.id)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during batch reschedule: ${e.message}")
            }
        }
    }

    private fun parseTimeStr(timeStr: String): Pair<Int, Int>? {
        return try {
            val clean = timeStr.trim().uppercase(Locale.US)
            val parts = clean.split(" ")
            if (parts.size != 2) return null
            val timeParts = parts[0].split(":")
            var hour = timeParts[0].toInt()
            val minute = timeParts[1].toInt()
            val amPm = parts[1]
            if (amPm == "PM" && hour < 12) hour += 12
            if (amPm == "AM" && hour == 12) hour = 0
            Pair(hour, minute)
        } catch (e: Exception) {
            null
        }
    }
}
