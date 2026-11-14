package com.example.data.model

import java.util.Calendar
import kotlin.math.roundToInt

data class RepaymentScheduleItem(
    val installmentNumber: Int,
    val dueDateMillis: Long,
    val expectedAmount: Double,
    val principalPart: Double,
    val interestPart: Double,
    val remainingBalanceAfter: Double,
    val isPaid: Boolean,
    val isNextDue: Boolean,
    val paidDateMillis: Long? = null,
    val paidAmount: Double? = null
)

object LoanScheduleCalculator {

    fun effectiveInterestRate(loan: Loan): Double {
        if (loan.interestRate > 0.0) return loan.interestRate
        if (loan.principalAmount <= 0.0) return 0.0
        return ((loan.chargeAmount / loan.principalAmount) * 10000.0).roundToInt() / 100.0
    }

    fun formatInterestRate(rate: Double): String {
        return if (rate % 1.0 == 0.0) {
            rate.toInt().toString()
        } else {
            String.format(java.util.Locale.US, "%.1f", rate)
        }
    }

    fun computeDueDateMillis(disbursedDate: Long, installmentNumber: Int, frequency: String): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = disbursedDate
        }
        if (frequency == "MONTHLY") {
            cal.add(Calendar.MONTH, installmentNumber)
        } else {
            cal.add(Calendar.DAY_OF_YEAR, installmentNumber * 7)
        }
        return cal.timeInMillis
    }

    fun generateSchedule(
        loan: Loan,
        payments: List<InstallmentPayment> = emptyList()
    ): List<RepaymentScheduleItem> {
        val count = loan.totalInstallments.coerceAtLeast(1)
        val sortedPayments = payments.sortedBy { it.installmentNumber }

        val baseInstallment = ((loan.totalPayable / count) * 100.0).roundToInt() / 100.0
        val basePrincipal = ((loan.principalAmount / count) * 100.0).roundToInt() / 100.0
        val baseInterest = ((loan.chargeAmount / count) * 100.0).roundToInt() / 100.0

        var accumulatedPayable = 0.0
        var accumulatedPrincipal = 0.0
        var accumulatedInterest = 0.0
        var remainingBalance = loan.totalPayable

        val items = mutableListOf<RepaymentScheduleItem>()
        val nextDueIndex = if (loan.status == "ACTIVE" && loan.paidInstallments < count) {
            loan.paidInstallments + 1
        } else {
            -1
        }

        for (k in 1..count) {
            val isLast = k == count
            val expectedAmount = if (isLast) {
                ((loan.totalPayable - accumulatedPayable) * 100.0).roundToInt() / 100.0
            } else {
                baseInstallment
            }.coerceAtLeast(0.0)

            val principalPart = if (isLast) {
                ((loan.principalAmount - accumulatedPrincipal) * 100.0).roundToInt() / 100.0
            } else {
                basePrincipal
            }.coerceAtLeast(0.0)

            val interestPart = if (isLast) {
                ((loan.chargeAmount - accumulatedInterest) * 100.0).roundToInt() / 100.0
            } else {
                baseInterest
            }.coerceAtLeast(0.0)

            accumulatedPayable += expectedAmount
            accumulatedPrincipal += principalPart
            accumulatedInterest += interestPart
            remainingBalance = (remainingBalance - expectedAmount).coerceAtLeast(0.0)

            val dueDate = computeDueDateMillis(loan.disbursedDate, k, loan.frequency)
            val isPaid = k <= loan.paidInstallments || loan.status == "CLOSED"
            val matchedPayment = sortedPayments.firstOrNull { it.installmentNumber == k }

            items.add(
                RepaymentScheduleItem(
                    installmentNumber = k,
                    dueDateMillis = dueDate,
                    expectedAmount = expectedAmount,
                    principalPart = principalPart,
                    interestPart = interestPart,
                    remainingBalanceAfter = ((remainingBalance) * 100.0).roundToInt() / 100.0,
                    isPaid = isPaid,
                    isNextDue = k == nextDueIndex,
                    paidDateMillis = matchedPayment?.paymentDate,
                    paidAmount = matchedPayment?.paidAmount
                )
            )
        }
        return items
    }
}
