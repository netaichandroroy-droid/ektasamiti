package com.example

import com.example.data.model.InstallmentPayment
import com.example.data.model.Loan
import com.example.data.model.LoanScheduleCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class LoanScheduleCalculatorTest {

    @Test
    fun `effectiveInterestRate uses stored rate or derives from principal and charge`() {
        val storedRateLoan = Loan(
            id = 1L,
            memberId = 1L,
            memberCode = "ES-1001",
            memberName = "Rahim",
            principalAmount = 20000.0,
            interestRate = 12.5,
            chargeAmount = 2500.0,
            totalPayable = 22500.0,
            perInstallmentAmount = 1125.0
        )
        assertEquals(12.5, LoanScheduleCalculator.effectiveInterestRate(storedRateLoan), 0.01)

        val derivedRateLoan = storedRateLoan.copy(interestRate = 0.0, chargeAmount = 2000.0)
        assertEquals(10.0, LoanScheduleCalculator.effectiveInterestRate(derivedRateLoan), 0.01)
    }

    @Test
    fun `generateSchedule produces exact totalPayable and tracks paid vs next due installments`() {
        val disbursed = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 1, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val loan = Loan(
            id = 2L,
            memberId = 1L,
            memberCode = "ES-1001",
            memberName = "Karim",
            principalAmount = 10000.0,
            interestRate = 10.0,
            chargeAmount = 1000.0,
            totalPayable = 11000.0,
            totalPaid = 3666.67,
            remainingDue = 7333.33,
            totalInstallments = 3,
            paidInstallments = 1,
            perInstallmentAmount = 3666.67,
            frequency = "MONTHLY",
            disbursedDate = disbursed,
            status = "ACTIVE"
        )

        val payments = listOf(
            InstallmentPayment(
                id = 1L,
                loanId = 2L,
                memberId = 1L,
                memberCode = "ES-1001",
                memberName = "Karim",
                installmentNumber = 1,
                paidAmount = 3666.67,
                paymentDate = disbursed + 86400000L,
                remainingAfterPayment = 7333.33,
                collectedBy = "Admin",
                receiptNumber = "REC-1"
            )
        )

        val schedule = LoanScheduleCalculator.generateSchedule(loan, payments)
        assertEquals(3, schedule.size)
        assertEquals(11000.0, schedule.sumOf { it.expectedAmount }, 0.05)
        assertEquals(0.0, schedule.last().remainingBalanceAfter, 0.05)

        assertTrue(schedule[0].isPaid)
        assertFalse(schedule[0].isNextDue)
        assertEquals(3666.67, schedule[0].paidAmount ?: 0.0, 0.01)

        assertFalse(schedule[1].isPaid)
        assertTrue(schedule[1].isNextDue)

        assertFalse(schedule[2].isPaid)
        assertFalse(schedule[2].isNextDue)

        val cal = Calendar.getInstance().apply { timeInMillis = schedule[1].dueDateMillis }
        assertEquals(Calendar.JANUARY, cal.get(Calendar.MONTH))
        assertEquals(2027, cal.get(Calendar.YEAR))
    }
}
