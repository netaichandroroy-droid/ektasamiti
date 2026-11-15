package com.example.notifications

import android.Manifest
import android.app.AlarmManager
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
import com.example.MainActivity
import com.example.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object LoanReminderNotificationHelper {
    const val CHANNEL_ID = "ekta_samiti_loan_reminders"
    private const val NOTIFICATION_ID = 4001
    private const val ALARM_REQUEST_CODE = 5001
    const val EXTRA_OPEN_LOANS = "extra_open_loans"

    fun ensureNotificationChannel(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "সাপ্তাহিক ঋণ পরিশোধ রিমাইন্ডার",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "সদস্যদের আসন্ন সাপ্তাহিক ঋণের কিস্তি পরিশোধের তারিখ সম্পর্কে পরিচালককে স্মরণ করিয়ে দেয়।"
                    enableVibration(true)
                }
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
                manager.createNotificationChannel(channel)
            }
        } catch (_: Exception) {
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                NotificationManagerCompat.from(context).areNotificationsEnabled()
            }
        } catch (_: Exception) {
            false
        }
    }

    fun showWeeklyRepaymentNotification(
        context: Context,
        upcoming: List<UpcomingWeeklyRepayment>,
        isTestNotification: Boolean = false
    ): Boolean {
        ensureNotificationChannel(context)
        if (!hasNotificationPermission(context)) return false

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_OPEN_LOANS, true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dateSdf = SimpleDateFormat("dd MMM", Locale.getDefault())
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_samiti)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        if (upcoming.isEmpty()) {
            if (!isTestNotification) return false
            builder
                .setContentTitle("একতা সমিতি • সাপ্তাহিক কিস্তি রিমাইন্ডার")
                .setContentText("আগামী কয়েকদিনে কোনো সাপ্তাহিক ঋণের কিস্তি বকেয়া বা আসন্ন নেই।")
        } else {
            val totalExpected = upcoming.sumOf { it.expectedAmount }.toInt()
            val overdueCount = upcoming.count { it.isOverdue }
            val title = if (overdueCount > 0) {
                "পরিচালক রিমাইন্ডার: ${upcoming.size}টি সাপ্তাহিক কিস্তি ($overdueCount টি বকেয়া)"
            } else {
                "পরিচালক রিমাইন্ডার: ${upcoming.size}টি আসন্ন সাপ্তাহিক কিস্তি"
            }
            val summaryText = "মোট প্রত্যাশিত আদায়: ৳$totalExpected • বিস্তারিত দেখতে ট্যাপ করুন"

            val inboxStyle = NotificationCompat.InboxStyle()
                .setBigContentTitle(title)
                .setSummaryText("পরিচালক: নিতাই চন্দ্র রায় • মোট ৳$totalExpected")

            upcoming.take(6).forEach { item ->
                val dueBadge = LoanReminderCalculator.formatDueBadgeText(item.daysUntilDue)
                inboxStyle.addLine(
                    "${item.loan.memberName} (${item.nextInstallmentNumber}নং) - ৳${item.expectedAmount.toInt()} • ${dateSdf.format(Date(item.dueDateMillis))} ($dueBadge)"
                )
            }
            if (upcoming.size > 6) {
                inboxStyle.addLine("+ আরও ${upcoming.size - 6} জন সদস্য...")
            }

            builder
                .setContentTitle(title)
                .setContentText(summaryText)
                .setStyle(inboxStyle)
        }

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
            return true
        } catch (_: SecurityException) {
            return false
        }
    }

    fun scheduleDailyReminder(context: Context) {
        try {
            val settings = LoanReminderPreferences.getSettings(context)
            if (!settings.isEnabled) {
                cancelDailyReminder(context)
                return
            }

            ensureNotificationChannel(context)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val pendingIntent = buildReceiverPendingIntent(context)

            val triggerCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, settings.reminderHour)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                triggerCal.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
        } catch (_: Exception) {
        }
    }

    fun cancelDailyReminder(context: Context) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            alarmManager.cancel(buildReceiverPendingIntent(context))
        } catch (_: Exception) {
        }
    }

    private fun buildReceiverPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, LoanRepaymentReminderReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
