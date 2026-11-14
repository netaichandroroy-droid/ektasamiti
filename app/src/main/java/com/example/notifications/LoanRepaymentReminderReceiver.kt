package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LoanRepaymentReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val appContext = context.applicationContext
        val settings = LoanReminderPreferences.getSettings(appContext)
        if (!settings.isEnabled) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val loans = AppDatabase.getInstance(appContext).loanDao().getAllLoansList()
                val upcoming = LoanReminderCalculator.findUpcomingWeeklyRepayments(
                    loans = loans,
                    leadDays = settings.leadDays
                )
                if (upcoming.isNotEmpty()) {
                    val sent = LoanReminderNotificationHelper.showWeeklyRepaymentNotification(
                        context = appContext,
                        upcoming = upcoming,
                        isTestNotification = false
                    )
                    if (sent) {
                        LoanReminderPreferences.markNotifiedToday(appContext)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
