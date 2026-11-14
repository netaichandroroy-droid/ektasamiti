package com.example.notifications

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LoanReminderSettings(
    val isEnabled: Boolean = true,
    val leadDays: Int = 2,
    val reminderHour: Int = 9
)

object LoanReminderPreferences {
    private const val PREFS_NAME = "ekta_samiti_loan_reminders"
    private const val KEY_ENABLED = "reminders_enabled"
    private const val KEY_LEAD_DAYS = "lead_days"
    private const val KEY_REMINDER_HOUR = "reminder_hour"
    private const val KEY_LAST_NOTIFIED_DAY = "last_notified_day"

    fun getSettings(context: Context): LoanReminderSettings {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return LoanReminderSettings(
            isEnabled = prefs.getBoolean(KEY_ENABLED, true),
            leadDays = prefs.getInt(KEY_LEAD_DAYS, 2),
            reminderHour = prefs.getInt(KEY_REMINDER_HOUR, 9)
        )
    }

    fun saveSettings(context: Context, settings: LoanReminderSettings) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_ENABLED, settings.isEnabled)
            .putInt(KEY_LEAD_DAYS, settings.leadDays)
            .putInt(KEY_REMINDER_HOUR, settings.reminderHour)
            .apply()
    }

    fun wasNotifiedToday(context: Context, nowMillis: Long = System.currentTimeMillis()): Boolean {
        val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(nowMillis))
        val lastKey = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LAST_NOTIFIED_DAY, null)
        return lastKey == todayKey
    }

    fun markNotifiedToday(context: Context, nowMillis: Long = System.currentTimeMillis()) {
        val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(nowMillis))
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_LAST_NOTIFIED_DAY, todayKey)
            .apply()
    }
}
