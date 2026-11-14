package com.example.data.model

import java.text.NumberFormat
import java.util.Locale

enum class MemberHistoryType {
    SAVINGS_DEPOSIT,
    SAVINGS_WITHDRAWAL,
    LOAN_PAYMENT,
    LOAN_DISBURSED
}

data class MemberHistoryItem(
    val id: String,
    val type: MemberHistoryType,
    val title: String,
    val subtitle: String,
    val amount: Double,
    val secondaryText: String,
    val timestamp: Long,
    val receiptNumber: String,
    val collectorName: String,
    val notes: String,
    val relatedLoanId: Long? = null
)

data class MemberTransactionSummary(
    val totalSavingsDeposited: Double,
    val savingsDepositCount: Int,
    val totalSavingsWithdrawn: Double,
    val savingsWithdrawalCount: Int,
    val currentSavingsBalance: Double,
    val totalLoanRepaid: Double,
    val totalPenaltyPaid: Double,
    val loanPaymentCount: Int,
    val totalLoanPrincipalTaken: Double,
    val loansTakenCount: Int,
    val totalRemainingLoanDue: Double,
    val timeline: List<MemberHistoryItem>
)

object MemberHistoryAggregator {

    fun buildMemberSummary(
        member: Member,
        savingsList: List<SavingsTransaction>,
        loansList: List<Loan>,
        installmentsList: List<InstallmentPayment>
    ): MemberTransactionSummary {
        val memberSavings = savingsList.filter { it.memberId == member.id }
        val memberLoans = loansList.filter { it.memberId == member.id }
        val memberPayments = installmentsList.filter { it.memberId == member.id }

        val deposits = memberSavings.filter { it.type == "DEPOSIT" }
        val withdrawals = memberSavings.filter { it.type == "WITHDRAWAL" }

        val totalDeposited = deposits.sumOf { it.amount }
        val totalWithdrawn = withdrawals.sumOf { it.amount }
        val totalRepaid = memberPayments.sumOf { it.paidAmount }
        val totalPenalty = memberPayments.sumOf { it.penaltyAmount }
        val totalPrincipal = memberLoans.sumOf { it.principalAmount }
        val totalRemainingDue = memberLoans
            .filter { it.status == "ACTIVE" }
            .sumOf { it.remainingDue }

        val items = mutableListOf<MemberHistoryItem>()

        memberSavings.forEach { tx ->
            val isDeposit = tx.type == "DEPOSIT"
            items.add(
                MemberHistoryItem(
                    id = "sav_${tx.id}",
                    type = if (isDeposit) MemberHistoryType.SAVINGS_DEPOSIT else MemberHistoryType.SAVINGS_WITHDRAWAL,
                    title = if (isDeposit) "সঞ্চয় জমা (${tx.savingsPlan})" else "সঞ্চয় উত্তোলন",
                    subtitle = "সংগ্রাহক: ${tx.collectedBy}",
                    amount = tx.amount,
                    secondaryText = "স্থিতি: ৳${formatInt(tx.balanceAfter)}",
                    timestamp = tx.date,
                    receiptNumber = tx.receiptNumber,
                    collectorName = tx.collectedBy,
                    notes = tx.note
                )
            )
        }

        memberPayments.forEach { pay ->
            val penaltyNote = if (pay.penaltyAmount > 0) " • জরিমানা: ৳${formatInt(pay.penaltyAmount)}" else ""
            items.add(
                MemberHistoryItem(
                    id = "pay_${pay.id}",
                    type = MemberHistoryType.LOAN_PAYMENT,
                    title = "ঋণের কিস্তি পরিশোধ (#${pay.installmentNumber})",
                    subtitle = "ঋণ নং #${pay.loanId} • সংগ্রাহক: ${pay.collectedBy}$penaltyNote",
                    amount = pay.paidAmount,
                    secondaryText = "বকেয়া: ৳${formatInt(pay.remainingAfterPayment)}",
                    timestamp = pay.paymentDate,
                    receiptNumber = pay.receiptNumber,
                    collectorName = pay.collectedBy,
                    notes = pay.notes,
                    relatedLoanId = pay.loanId
                )
            )
        }

        memberLoans.forEach { loan ->
            val rate = LoanScheduleCalculator.formatInterestRate(
                LoanScheduleCalculator.effectiveInterestRate(loan)
            )
            val freqLabel = if (loan.frequency == "MONTHLY") "মাসিক" else "সাপ্তাহিক"
            items.add(
                MemberHistoryItem(
                    id = "loan_${loan.id}",
                    type = MemberHistoryType.LOAN_DISBURSED,
                    title = "নতুন ঋণ গ্রহণ (#${loan.id})",
                    subtitle = "$freqLabel • ${loan.totalInstallments} কিস্তি • সুদ $rate%",
                    amount = loan.principalAmount,
                    secondaryText = "মোট পরিশোধযোগ্য: ৳${formatInt(loan.totalPayable)}",
                    timestamp = loan.disbursedDate,
                    receiptNumber = "LN-${loan.id}",
                    collectorName = loan.disbursedBy,
                    notes = loan.purpose,
                    relatedLoanId = loan.id
                )
            )
        }

        val sortedTimeline = items.sortedByDescending { it.timestamp }

        return MemberTransactionSummary(
            totalSavingsDeposited = totalDeposited,
            savingsDepositCount = deposits.size,
            totalSavingsWithdrawn = totalWithdrawn,
            savingsWithdrawalCount = withdrawals.size,
            currentSavingsBalance = member.savingsBalance,
            totalLoanRepaid = totalRepaid,
            totalPenaltyPaid = totalPenalty,
            loanPaymentCount = memberPayments.size,
            totalLoanPrincipalTaken = totalPrincipal,
            loansTakenCount = memberLoans.size,
            totalRemainingLoanDue = totalRemainingDue,
            timeline = sortedTimeline
        )
    }

    private fun formatInt(value: Double): String {
        return NumberFormat.getIntegerInstance(Locale.US).format(value.toLong())
    }
}
