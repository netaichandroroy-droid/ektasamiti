package com.example

import com.example.data.model.InstallmentPayment
import com.example.data.model.Loan
import com.example.data.model.Member
import com.example.data.model.SavingsTransaction
import com.example.reports.MonthlySummaryCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class MonthlySummaryCalculatorTest {

    private fun dateMillis(year: Int, month: Int, day: Int, hour: Int = 10): Long {
        return Calendar.getInstance().apply {
            set(year, month, day, hour, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    @Test
    fun calculateMonthlySummary_filtersBySelectedMonthAndAggregatesCorrectly() {
        val nov6 = dateMillis(2026, Calendar.NOVEMBER, 6)
        val nov13 = dateMillis(2026, Calendar.NOVEMBER, 13)
        val oct28 = dateMillis(2026, Calendar.OCTOBER, 28)
        val dec4 = dateMillis(2026, Calendar.DECEMBER, 4)

        val members = listOf(
            Member(id = 1, memberCode = "ES-1001", name = "Netai Chandro Roy", mobile = "01711", address = "A", savingsBalance = 1200.0),
            Member(id = 2, memberCode = "ES-1002", name = "Sumon Roy", mobile = "01722", address = "B", savingsBalance = 800.0)
        )

        val savings = listOf(
            SavingsTransaction(id = 1, memberId = 1, memberCode = "ES-1001", memberName = "Netai Chandro Roy", type = "DEPOSIT", amount = 500.0, date = nov6),
            SavingsTransaction(id = 2, memberId = 2, memberCode = "ES-1002", memberName = "Sumon Roy", type = "DEPOSIT", amount = 300.0, date = nov13),
            SavingsTransaction(id = 3, memberId = 2, memberCode = "ES-1002", memberName = "Sumon Roy", type = "WITHDRAWAL", amount = 100.0, date = nov13),
            // Out-of-month entries that should NOT be included in November summary
            SavingsTransaction(id = 4, memberId = 1, memberCode = "ES-1001", memberName = "Netai Chandro Roy", type = "DEPOSIT", amount = 999.0, date = oct28),
            SavingsTransaction(id = 5, memberId = 1, memberCode = "ES-1001", memberName = "Netai Chandro Roy", type = "DEPOSIT", amount = 888.0, date = dec4)
        )

        val loans = listOf(
            Loan(
                id = 1,
                memberId = 1,
                memberCode = "ES-1001",
                memberName = "Netai Chandro Roy",
                principalAmount = 5000.0,
                interestRate = 10.0,
                chargeAmount = 500.0,
                totalPayable = 5500.0,
                totalInstallments = 10,
                perInstallmentAmount = 550.0,
                remainingDue = 4400.0,
                disbursedDate = nov6,
                status = "ACTIVE"
            ),
            Loan(
                id = 2,
                memberId = 2,
                memberCode = "ES-1002",
                memberName = "Sumon Roy",
                principalAmount = 3000.0,
                interestRate = 10.0,
                chargeAmount = 300.0,
                totalPayable = 3300.0,
                totalInstallments = 10,
                perInstallmentAmount = 330.0,
                remainingDue = 3300.0,
                disbursedDate = oct28,
                status = "ACTIVE"
            )
        )

        val installments = listOf(
            InstallmentPayment(id = 1, loanId = 1, memberId = 1, memberCode = "ES-1001", memberName = "Netai Chandro Roy", installmentNumber = 1, paidAmount = 550.0, penaltyAmount = 20.0, paymentDate = nov13),
            InstallmentPayment(id = 2, loanId = 2, memberId = 2, memberCode = "ES-1002", memberName = "Sumon Roy", installmentNumber = 1, paidAmount = 330.0, penaltyAmount = 0.0, paymentDate = nov13),
            // Out-of-month installment
            InstallmentPayment(id = 3, loanId = 1, memberId = 1, memberCode = "ES-1001", memberName = "Netai Chandro Roy", installmentNumber = 2, paidAmount = 550.0, penaltyAmount = 0.0, paymentDate = dec4)
        )

        val summary = MonthlySummaryCalculator.calculateMonthlySummary(
            year = 2026,
            month = Calendar.NOVEMBER,
            members = members,
            savingsList = savings,
            loansList = loans,
            installmentsList = installments,
            generatedBy = "নিতাই চন্দ্র রায়",
            generatedAtMillis = nov13
        )

        assertEquals("নভেম্বর 2026", summary.monthTitleBn)
        assertEquals("2026_11", summary.monthFileSlug)
        assertEquals(800.0, summary.savingsDepositTotal, 0.01)
        assertEquals(2, summary.savingsDepositCount)
        assertEquals(100.0, summary.savingsWithdrawalTotal, 0.01)
        assertEquals(1, summary.savingsWithdrawalCount)
        assertEquals(700.0, summary.netSavings, 0.01)
        assertEquals(880.0, summary.loanRepaymentTotal, 0.01)
        assertEquals(20.0, summary.loanPenaltyTotal, 0.01)
        assertEquals(2, summary.loanRepaymentCount)
        assertEquals(5000.0, summary.loanDisbursedTotal, 0.01)
        assertEquals(1, summary.loanDisbursedCount)
        assertEquals(1680.0, summary.totalMonthlyCollection, 0.01)
        assertEquals(2, summary.totalActiveMembers)
        assertEquals(2000.0, summary.totalSavingsFundBalance, 0.01)
        assertEquals(2, summary.activeLoansCount)
        assertEquals(7700.0, summary.totalOutstandingLoanDue, 0.01)

        // Member breakdown sorted by total contributed descending: Netai (500+550=1050) > Sumon (300+330=630)
        assertEquals(2, summary.memberBreakdowns.size)
        assertEquals("Netai Chandro Roy", summary.memberBreakdowns[0].memberName)
        assertEquals(1050.0, summary.memberBreakdowns[0].totalContributed, 0.01)
        assertEquals("Sumon Roy", summary.memberBreakdowns[1].memberName)
        assertEquals(630.0, summary.memberBreakdowns[1].totalContributed, 0.01)
    }

    @Test
    fun formatAsShareableText_containsRequiredSectionsAndTotals() {
        val nov13 = dateMillis(2026, Calendar.NOVEMBER, 13)
        val summary = MonthlySummaryCalculator.calculateMonthlySummary(
            year = 2026,
            month = Calendar.NOVEMBER,
            members = listOf(
                Member(id = 1, memberCode = "ES-1001", name = "Kajol Roy", mobile = "017", address = "C", savingsBalance = 1500.0)
            ),
            savingsList = listOf(
                SavingsTransaction(id = 1, memberId = 1, memberCode = "ES-1001", memberName = "Kajol Roy", type = "DEPOSIT", amount = 1000.0, date = nov13)
            ),
            loansList = emptyList(),
            installmentsList = listOf(
                InstallmentPayment(id = 1, loanId = 1, memberId = 1, memberCode = "ES-1001", memberName = "Kajol Roy", installmentNumber = 1, paidAmount = 500.0, paymentDate = nov13)
            ),
            generatedBy = "নিতাই চন্দ্র রায়",
            generatedAtMillis = nov13
        )

        val text = MonthlySummaryCalculator.formatAsShareableText(summary)
        assertTrue(text.contains("একতা সঞ্চয় ও ঋণ সমিতি"))
        assertTrue(text.contains("নভেম্বর 2026"))
        assertTrue(text.contains("৳1,000"))
        assertTrue(text.contains("৳500"))
        assertTrue(text.contains("৳1,500"))
        assertTrue(text.contains("Kajol Roy"))
        assertTrue(text.contains("নিতাই চন্দ্র রায়"))
    }
}
