package com.example

import com.example.data.model.InstallmentPayment
import com.example.data.model.Loan
import com.example.data.model.Member
import com.example.data.model.MemberHistoryAggregator
import com.example.data.model.MemberHistoryType
import com.example.data.model.SavingsTransaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MemberHistoryAggregatorTest {

    @Test
    fun `buildMemberSummary filters strictly for target member and aggregates totals accurately`() {
        val targetMember = Member(
            id = 10L,
            memberCode = "MEM-010",
            name = "রহিম উদ্দিন",
            mobile = "01711000010",
            savingsBalance = 3500.0
        )

        val savingsList = listOf(
            SavingsTransaction(
                id = 1L,
                memberId = 10L,
                memberCode = "MEM-010",
                memberName = "রহিম উদ্দিন",
                type = "DEPOSIT",
                savingsPlan = "WEEKLY",
                amount = 2000.0,
                balanceAfter = 2000.0,
                date = 1_000L,
                collectedBy = "Admin",
                receiptNumber = "SAV-1"
            ),
            SavingsTransaction(
                id = 2L,
                memberId = 10L,
                memberCode = "MEM-010",
                memberName = "রহিম উদ্দিন",
                type = "DEPOSIT",
                savingsPlan = "WEEKLY",
                amount = 2000.0,
                balanceAfter = 4000.0,
                date = 3_000L,
                collectedBy = "Collector A",
                receiptNumber = "SAV-2"
            ),
            SavingsTransaction(
                id = 3L,
                memberId = 10L,
                memberCode = "MEM-010",
                memberName = "রহিম উদ্দিন",
                type = "WITHDRAWAL",
                savingsPlan = "WEEKLY",
                amount = 500.0,
                balanceAfter = 3500.0,
                date = 5_000L,
                collectedBy = "Admin",
                receiptNumber = "WDR-1"
            ),
            // Another member's savings - must be excluded
            SavingsTransaction(
                id = 4L,
                memberId = 99L,
                memberCode = "MEM-099",
                memberName = "অন্য সদস্য",
                type = "DEPOSIT",
                savingsPlan = "WEEKLY",
                amount = 9000.0,
                balanceAfter = 9000.0,
                date = 6_000L
            )
        )

        val loansList = listOf(
            Loan(
                id = 101L,
                memberId = 10L,
                memberCode = "MEM-010",
                memberName = "রহিম উদ্দিন",
                principalAmount = 10000.0,
                interestRate = 10.0,
                chargeAmount = 1000.0,
                totalPayable = 11000.0,
                totalPaid = 11000.0,
                remainingDue = 0.0,
                totalInstallments = 10,
                paidInstallments = 10,
                perInstallmentAmount = 1100.0,
                frequency = "WEEKLY",
                disbursedDate = 500L,
                status = "CLOSED"
            ),
            Loan(
                id = 102L,
                memberId = 10L,
                memberCode = "MEM-010",
                memberName = "রহিম উদ্দিন",
                principalAmount = 20000.0,
                interestRate = 10.0,
                chargeAmount = 2000.0,
                totalPayable = 22000.0,
                totalPaid = 2200.0,
                remainingDue = 19800.0,
                totalInstallments = 20,
                paidInstallments = 2,
                perInstallmentAmount = 1100.0,
                frequency = "WEEKLY",
                disbursedDate = 2_000L,
                status = "ACTIVE"
            ),
            // Another member's active loan - must be excluded
            Loan(
                id = 103L,
                memberId = 99L,
                memberCode = "MEM-099",
                memberName = "অন্য সদস্য",
                principalAmount = 50000.0,
                chargeAmount = 5000.0,
                totalPayable = 55000.0,
                remainingDue = 55000.0,
                perInstallmentAmount = 2750.0,
                disbursedDate = 7_000L,
                status = "ACTIVE"
            )
        )

        val installmentsList = listOf(
            InstallmentPayment(
                id = 201L,
                loanId = 102L,
                memberId = 10L,
                memberCode = "MEM-010",
                memberName = "রহিম উদ্দিন",
                installmentNumber = 1,
                paidAmount = 1100.0,
                penaltyAmount = 0.0,
                paymentDate = 4_000L,
                remainingAfterPayment = 20900.0,
                receiptNumber = "REC-1"
            ),
            InstallmentPayment(
                id = 202L,
                loanId = 102L,
                memberId = 10L,
                memberCode = "MEM-010",
                memberName = "রহিম উদ্দিন",
                installmentNumber = 2,
                paidAmount = 1100.0,
                penaltyAmount = 50.0,
                paymentDate = 6_500L,
                remainingAfterPayment = 19800.0,
                receiptNumber = "REC-2"
            ),
            // Another member's payment - must be excluded
            InstallmentPayment(
                id = 203L,
                loanId = 103L,
                memberId = 99L,
                memberCode = "MEM-099",
                memberName = "অন্য সদস্য",
                installmentNumber = 1,
                paidAmount = 2750.0,
                penaltyAmount = 100.0,
                paymentDate = 8_000L,
                remainingAfterPayment = 52250.0
            )
        )

        val summary = MemberHistoryAggregator.buildMemberSummary(
            member = targetMember,
            savingsList = savingsList,
            loansList = loansList,
            installmentsList = installmentsList
        )

        assertEquals(4000.0, summary.totalSavingsDeposited, 0.001)
        assertEquals(2, summary.savingsDepositCount)
        assertEquals(500.0, summary.totalSavingsWithdrawn, 0.001)
        assertEquals(1, summary.savingsWithdrawalCount)
        assertEquals(3500.0, summary.currentSavingsBalance, 0.001)

        assertEquals(2200.0, summary.totalLoanRepaid, 0.001)
        assertEquals(50.0, summary.totalPenaltyPaid, 0.001)
        assertEquals(2, summary.loanPaymentCount)

        assertEquals(30000.0, summary.totalLoanPrincipalTaken, 0.001)
        assertEquals(2, summary.loansTakenCount)
        assertEquals(19800.0, summary.totalRemainingLoanDue, 0.001)

        // 3 savings + 2 loans + 2 installment payments = 7 timeline entries
        assertEquals(7, summary.timeline.size)

        // Verify descending chronological order
        val timestamps = summary.timeline.map { it.timestamp }
        assertEquals(listOf(6_500L, 5_000L, 4_000L, 3_000L, 2_000L, 1_000L, 500L), timestamps)

        // Verify types at key positions
        assertEquals(MemberHistoryType.LOAN_PAYMENT, summary.timeline[0].type)
        assertEquals(MemberHistoryType.SAVINGS_WITHDRAWAL, summary.timeline[1].type)
        assertEquals(MemberHistoryType.LOAN_PAYMENT, summary.timeline[2].type)
        assertEquals(MemberHistoryType.SAVINGS_DEPOSIT, summary.timeline[3].type)
        assertEquals(MemberHistoryType.LOAN_DISBURSED, summary.timeline[4].type)
        assertEquals(MemberHistoryType.SAVINGS_DEPOSIT, summary.timeline[5].type)
        assertEquals(MemberHistoryType.LOAN_DISBURSED, summary.timeline[6].type)
    }

    @Test
    fun `buildMemberSummary handles member with no transactions gracefully`() {
        val emptyMember = Member(
            id = 5L,
            memberCode = "MEM-005",
            name = "নতুন সদস্য",
            mobile = "01800000005",
            savingsBalance = 0.0
        )

        val summary = MemberHistoryAggregator.buildMemberSummary(
            member = emptyMember,
            savingsList = emptyList(),
            loansList = emptyList(),
            installmentsList = emptyList()
        )

        assertEquals(0.0, summary.totalSavingsDeposited, 0.001)
        assertEquals(0.0, summary.totalSavingsWithdrawn, 0.001)
        assertEquals(0.0, summary.totalLoanRepaid, 0.001)
        assertEquals(0.0, summary.totalRemainingLoanDue, 0.001)
        assertTrue(summary.timeline.isEmpty())
    }
}
