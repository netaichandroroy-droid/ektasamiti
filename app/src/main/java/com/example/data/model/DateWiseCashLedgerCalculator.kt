package com.example.data.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DateWiseCashLedgerEntry(
    val dayKey: String,
    val displayDate: String,
    val dayStartMillis: Long,
    val savingsDeposit: Double,
    val savingsWithdrawal: Double,
    val installmentCollected: Double,
    val loanDisbursed: Double,
    val netDayCollection: Double,
    val openingCash: Double,
    val closingCash: Double
)

data class DateWiseCashLedgerSummary(
    val entries: List<DateWiseCashLedgerEntry>,
    val totalCash: Double
)

object DateWiseCashLedgerCalculator {

    private data class MutableDayAccumulator(
        val dayKey: String,
        val displayDate: String,
        val dayStartMillis: Long,
        var savingsDeposit: Double = 0.0,
        var savingsWithdrawal: Double = 0.0,
        var installmentCollected: Double = 0.0,
        var loanDisbursed: Double = 0.0
    )

    fun compute(
        savingsList: List<SavingsTransaction>,
        loansList: List<Loan>,
        installmentsList: List<InstallmentPayment>
    ): DateWiseCashLedgerSummary {
        val dayKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val displayDateFormat = SimpleDateFormat("dd/MM/yy", Locale.US)
        val map = linkedMapOf<String, MutableDayAccumulator>()

        fun accumulatorFor(timestamp: Long): MutableDayAccumulator {
            val normalized = startOfLocalDay(timestamp)
            val date = Date(normalized)
            val key = dayKeyFormat.format(date)
            return map.getOrPut(key) {
                MutableDayAccumulator(
                    dayKey = key,
                    displayDate = displayDateFormat.format(date),
                    dayStartMillis = normalized
                )
            }
        }

        savingsList.forEach { tx ->
            if (tx.amount <= 0.0) return@forEach
            val acc = accumulatorFor(tx.date)
            if (tx.type == "WITHDRAWAL") {
                acc.savingsWithdrawal += tx.amount
            } else {
                acc.savingsDeposit += tx.amount
            }
        }

        loansList.forEach { loan ->
            if (loan.principalAmount <= 0.0) return@forEach
            val acc = accumulatorFor(loan.disbursedDate)
            acc.loanDisbursed += loan.principalAmount
        }

        installmentsList.forEach { payment ->
            val collected = payment.paidAmount + payment.penaltyAmount
            if (collected <= 0.0) return@forEach
            val acc = accumulatorFor(payment.paymentDate)
            acc.installmentCollected += collected
        }

        val sortedDays = map.values.sortedBy { it.dayKey }
        var runningCash = 0.0
        val entries = sortedDays.map { day ->
            val netDay = (day.savingsDeposit + day.installmentCollected) -
                (day.savingsWithdrawal + day.loanDisbursed)
            val opening = runningCash
            val closing = opening + netDay
            runningCash = closing
            DateWiseCashLedgerEntry(
                dayKey = day.dayKey,
                displayDate = day.displayDate,
                dayStartMillis = day.dayStartMillis,
                savingsDeposit = day.savingsDeposit,
                savingsWithdrawal = day.savingsWithdrawal,
                installmentCollected = day.installmentCollected,
                loanDisbursed = day.loanDisbursed,
                netDayCollection = netDay,
                openingCash = opening,
                closingCash = closing
            )
        }

        return DateWiseCashLedgerSummary(
            entries = entries,
            totalCash = runningCash
        )
    }

    private fun startOfLocalDay(timestamp: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }
}
