package com.example.reports

import com.example.data.model.InstallmentPayment
import com.example.data.model.Loan
import com.example.data.model.Member
import com.example.data.model.SavingsTransaction
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class MemberMonthlyBreakdown(
    val memberId: Long,
    val memberCode: String,
    val memberName: String,
    val savingsDeposited: Double,
    val savingsWithdrawn: Double,
    val loanRepaid: Double
) {
    val totalContributed: Double get() = savingsDeposited + loanRepaid
}

data class MonthlyFinancialSummary(
    val year: Int,
    val month: Int, // Calendar.JANUARY (0) .. Calendar.DECEMBER (11)
    val monthTitleBn: String,
    val monthFileSlug: String,
    val savingsDepositTotal: Double,
    val savingsDepositCount: Int,
    val savingsWithdrawalTotal: Double,
    val savingsWithdrawalCount: Int,
    val netSavings: Double,
    val loanRepaymentTotal: Double,
    val loanPenaltyTotal: Double,
    val loanRepaymentCount: Int,
    val loanDisbursedTotal: Double,
    val loanDisbursedCount: Int,
    val totalMonthlyCollection: Double,
    val totalActiveMembers: Int,
    val totalSavingsFundBalance: Double,
    val activeLoansCount: Int,
    val totalOutstandingLoanDue: Double,
    val memberBreakdowns: List<MemberMonthlyBreakdown>,
    val generatedBy: String,
    val generatedAtMillis: Long
)

object MonthlySummaryCalculator {

    private val bengaliMonths = listOf(
        "জানুয়ারি",
        "ফেব্রুয়ারি",
        "মার্চ",
        "এপ্রিল",
        "মে",
        "জুন",
        "জুলাই",
        "আগস্ট",
        "সেপ্টেম্বর",
        "অক্টোবর",
        "নভেম্বর",
        "ডিসেম্বর"
    )

    fun monthTitleBengali(year: Int, month: Int): String {
        val monthName = bengaliMonths.getOrElse(month) { "মাস ${month + 1}" }
        return "$monthName $year"
    }

    fun calculateMonthlySummary(
        year: Int,
        month: Int,
        members: List<Member>,
        savingsList: List<SavingsTransaction>,
        loansList: List<Loan>,
        installmentsList: List<InstallmentPayment>,
        generatedBy: String,
        generatedAtMillis: Long = System.currentTimeMillis()
    ): MonthlyFinancialSummary {
        val startCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val monthStart = startCal.timeInMillis

        val endCal = (startCal.clone() as Calendar).apply {
            add(Calendar.MONTH, 1)
        }
        val monthEnd = endCal.timeInMillis

        val monthSavings = savingsList.filter { it.date in monthStart until monthEnd }
        val deposits = monthSavings.filter { it.type == "DEPOSIT" }
        val withdrawals = monthSavings.filter { it.type == "WITHDRAWAL" }

        val monthInstallments = installmentsList.filter { it.paymentDate in monthStart until monthEnd }
        val monthLoansDisbursed = loansList.filter { it.disbursedDate in monthStart until monthEnd }

        val savingsDepositTotal = deposits.sumOf { it.amount }
        val savingsWithdrawalTotal = withdrawals.sumOf { it.amount }
        val netSavings = savingsDepositTotal - savingsWithdrawalTotal

        val loanRepaymentTotal = monthInstallments.sumOf { it.paidAmount }
        val loanPenaltyTotal = monthInstallments.sumOf { it.penaltyAmount }
        val loanDisbursedTotal = monthLoansDisbursed.sumOf { it.principalAmount }

        val totalMonthlyCollection = savingsDepositTotal + loanRepaymentTotal

        val activeLoans = loansList.filter { it.status == "ACTIVE" }
        val totalSavingsFundBalance = members.sumOf { it.savingsBalance }
        val totalOutstandingLoanDue = activeLoans.sumOf { it.remainingDue }

        // Per-member contribution breakdown for the selected month
        val breakdownMap = linkedMapOf<Long, MutableBreakdown>()
        deposits.forEach { tx ->
            val entry = breakdownMap.getOrPut(tx.memberId) {
                MutableBreakdown(tx.memberId, tx.memberCode, tx.memberName)
            }
            entry.savingsDeposited += tx.amount
        }
        withdrawals.forEach { tx ->
            val entry = breakdownMap.getOrPut(tx.memberId) {
                MutableBreakdown(tx.memberId, tx.memberCode, tx.memberName)
            }
            entry.savingsWithdrawn += tx.amount
        }
        monthInstallments.forEach { pay ->
            val entry = breakdownMap.getOrPut(pay.memberId) {
                MutableBreakdown(pay.memberId, pay.memberCode, pay.memberName)
            }
            entry.loanRepaid += pay.paidAmount
        }

        val memberBreakdowns = breakdownMap.values
            .map {
                MemberMonthlyBreakdown(
                    memberId = it.memberId,
                    memberCode = it.memberCode,
                    memberName = it.memberName,
                    savingsDeposited = it.savingsDeposited,
                    savingsWithdrawn = it.savingsWithdrawn,
                    loanRepaid = it.loanRepaid
                )
            }
            .sortedByDescending { it.totalContributed }

        val monthTwoDigits = (month + 1).toString().padStart(2, '0')
        return MonthlyFinancialSummary(
            year = year,
            month = month,
            monthTitleBn = monthTitleBengali(year, month),
            monthFileSlug = "${year}_${monthTwoDigits}",
            savingsDepositTotal = savingsDepositTotal,
            savingsDepositCount = deposits.size,
            savingsWithdrawalTotal = savingsWithdrawalTotal,
            savingsWithdrawalCount = withdrawals.size,
            netSavings = netSavings,
            loanRepaymentTotal = loanRepaymentTotal,
            loanPenaltyTotal = loanPenaltyTotal,
            loanRepaymentCount = monthInstallments.size,
            loanDisbursedTotal = loanDisbursedTotal,
            loanDisbursedCount = monthLoansDisbursed.size,
            totalMonthlyCollection = totalMonthlyCollection,
            totalActiveMembers = members.size,
            totalSavingsFundBalance = totalSavingsFundBalance,
            activeLoansCount = activeLoans.size,
            totalOutstandingLoanDue = totalOutstandingLoanDue,
            memberBreakdowns = memberBreakdowns,
            generatedBy = generatedBy,
            generatedAtMillis = generatedAtMillis
        )
    }

    fun formatAmount(amount: Double): String {
        val nf = NumberFormat.getIntegerInstance(Locale.US)
        return "৳${nf.format(amount.toLong())}"
    }

    fun formatAsShareableText(summary: MonthlyFinancialSummary): String {
        val dateSdf = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
        val generatedDateText = dateSdf.format(Date(summary.generatedAtMillis))

        return buildString {
            appendLine("📊 *একতা সঞ্চয় ও ঋণ সমিতি*")
            appendLine("মাসিক আর্থিক সারসংক্ষেপ — ${summary.monthTitleBn}")
            appendLine("━━━━━━━━━━━━━━━━━━━━")
            appendLine()
            appendLine("💰 *সঞ্চয় হিসাব (${summary.monthTitleBn})*")
            appendLine("• মোট সঞ্চয় জমা: ${formatAmount(summary.savingsDepositTotal)} (${summary.savingsDepositCount}টি লেনদেন)")
            appendLine("• মোট সঞ্চয় উত্তোলন: ${formatAmount(summary.savingsWithdrawalTotal)} (${summary.savingsWithdrawalCount}টি লেনদেন)")
            appendLine("• নিট মাসিক সঞ্চয়: ${formatAmount(summary.netSavings)}")
            appendLine()
            appendLine("🏦 *ঋণ ও কিস্তি আদায় (${summary.monthTitleBn})*")
            appendLine("• কিস্তি আদায়: ${formatAmount(summary.loanRepaymentTotal)} (${summary.loanRepaymentCount}টি কিস্তি)")
            if (summary.loanPenaltyTotal > 0.0) {
                appendLine("• জরিমানা আদায়: ${formatAmount(summary.loanPenaltyTotal)}")
            }
            appendLine("• নতুন ঋণ বিতরণ: ${formatAmount(summary.loanDisbursedTotal)} (${summary.loanDisbursedCount}টি ঋণ)")
            appendLine()
            appendLine("📈 *মাসিক সর্বমোট আদায়*")
            appendLine("• সঞ্চয় জমা + কিস্তি আদায়: *${formatAmount(summary.totalMonthlyCollection)}*")
            appendLine()
            appendLine("🏛️ *সমিতির সার্বিক স্থিতি*")
            appendLine("• সক্রিয় সদস্য: ${summary.totalActiveMembers} জন")
            appendLine("• মোট সঞ্চয় তহবিল: ${formatAmount(summary.totalSavingsFundBalance)}")
            appendLine("• চলমান ঋণ: ${summary.activeLoansCount}টি (বকেয়া: ${formatAmount(summary.totalOutstandingLoanDue)})")

            if (summary.memberBreakdowns.isNotEmpty()) {
                appendLine()
                appendLine("👥 *সদস্যভিত্তিক আদায় (শীর্ষ তালিকা)*")
                summary.memberBreakdowns.take(10).forEachIndexed { index, item ->
                    val parts = mutableListOf<String>()
                    if (item.savingsDeposited > 0.0) {
                        parts.add("সঞ্চয় ${formatAmount(item.savingsDeposited)}")
                    }
                    if (item.loanRepaid > 0.0) {
                        parts.add("কিস্তি ${formatAmount(item.loanRepaid)}")
                    }
                    if (item.savingsWithdrawn > 0.0) {
                        parts.add("উত্তোলন ${formatAmount(item.savingsWithdrawn)}")
                    }
                    appendLine("${index + 1}. ${item.memberName} (${item.memberCode}): ${parts.joinToString(", ")}")
                }
                if (summary.memberBreakdowns.size > 10) {
                    appendLine("... এবং আরও ${summary.memberBreakdowns.size - 10} জন সদস্য")
                }
            }

            appendLine()
            appendLine("━━━━━━━━━━━━━━━━━━━━")
            appendLine("প্রস্তুতকারক: ${summary.generatedBy}")
            append("সময়: $generatedDateText")
        }
    }

    private class MutableBreakdown(
        val memberId: Long,
        val memberCode: String,
        val memberName: String,
        var savingsDeposited: Double = 0.0,
        var savingsWithdrawn: Double = 0.0,
        var loanRepaid: Double = 0.0
    )
}
