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

    fun findUpcomingMemberRepayments(
        loans: List<Loan>,
        nowMillis: Long = System.currentTimeMillis(),
        withinDays: Int = 3,
        includeOverdue: Boolean = false
    ): Map<String, UpcomingWeeklyRepayment> {
        val todayStart = startOfDayMillis(nowMillis)
        val result = linkedMapOf<String, UpcomingWeeklyRepayment>()

        loans
            .asSequence()
            .filter {
                it.status == "ACTIVE" &&
                    it.paidInstallments < it.totalInstallments &&
                    it.remainingDue > 0.0 &&
                    it.memberCode.isNotBlank()
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
                val inRange = if (includeOverdue) diffDays <= withinDays else diffDays in 0..withinDays

                if (inRange) {
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
            .forEach { repayment ->
                val key = repayment.loan.memberCode
                if (!result.containsKey(key)) {
                    result[key] = repayment
                }
            }

        return result
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
