package com.example.notifications

import com.example.data.model.Loan
import com.example.data.model.LoanScheduleCalculator
import java.util.Calendar
import java.util.concurrent.TimeUnit

data class UpcomingWeeklyRepayment(
    val loan: Loan,
    val nextInstallmentNumber: Int,
    val dueDateMillis: Long,
    val daysUntilDue: Int,
    val expectedAmount: Double
) {
    val isOverdue: Boolean get() = daysUntilDue < 0
    val isDueToday: Boolean get() = daysUntilDue == 0
}

object LoanReminderCalculator {

    fun findUpcomingWeeklyRepayments(
        loans: List<Loan>,
        nowMillis: Long = System.currentTimeMillis(),
        leadDays: Int = 2
    ): List<UpcomingWeeklyRepayment> {
        val todayStart = startOfDayMillis(nowMillis)

        return loans
            .filter {
                it.status == "ACTIVE" &&
                    it.frequency == "WEEKLY" &&
                    it.paidInstallments < it.totalInstallments &&
                    it.remainingDue > 0.0
            }
            .mapNotNull { loan ->
                val nextInstallment = loan.paidInstallments + 1
                val dueDateMillis = LoanScheduleCalculator.computeDueDateMillis(
                    disbursedDate = loan.disbursedDate,
                    installmentNumber = nextInstallment,
                    frequency = loan.frequency
                )
                val dueDayStart = startOfDayMillis(dueDateMillis)
                val diffDays = TimeUnit.MILLISECONDS.toDays(dueDayStart - todayStart).toInt()

                if (diffDays <= leadDays) {
                    val expected = minOf(loan.perInstallmentAmount, loan.remainingDue)
                    UpcomingWeeklyRepayment(
                        loan = loan,
                        nextInstallmentNumber = nextInstallment,
                        dueDateMillis = dueDateMillis,
                        daysUntilDue = diffDays,
                        expectedAmount = expected
                    )
                } else {
                    null
                }
            }
            .sortedBy { it.dueDateMillis }
    }

    fun formatDueBadgeText(daysUntilDue: Int): String {
        return when {
            daysUntilDue < 0 -> "${-daysUntilDue} দিন বকেয়া"
            daysUntilDue == 0 -> "আজ পরিশোধের দিন"
            daysUntilDue == 1 -> "আগামীকাল"
            else -> "$daysUntilDue দিন বাকি"
        }
    }

    private fun startOfDayMillis(timeMillis: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timeMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
