package com.example

import com.example.data.model.Loan
import com.example.notifications.LoanReminderCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.concurrent.TimeUnit

class LoanReminderCalculatorTest {

    private fun startOfDay(year: Int, month: Int, day: Int): Long {
        return Calendar.getInstance().apply {
            set(year, month, day, 9, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun createLoan(
        id: Long,
        memberName: String,
        disbursedDate: Long,
        paidInstallments: Int = 0,
        totalInstallments: Int = 10,
        perInstallmentAmount: Double = 550.0,
        remainingDue: Double = 5500.0,
        frequency: String = "WEEKLY",
        status: String = "ACTIVE"
    ): Loan {
        return Loan(
            id = id,
            memberId = id,
            memberName = memberName,
            memberCode = "MEM-00$id",
            principalAmount = 5000.0,
            interestRate = 10.0,
            chargeAmount = 500.0,
            totalPayable = 5500.0,
            totalInstallments = totalInstallments,
            perInstallmentAmount = perInstallmentAmount,
            frequency = frequency,
            paidInstallments = paidInstallments,
            totalPaid = paidInstallments * perInstallmentAmount,
            remainingDue = remainingDue,
            disbursedDate = disbursedDate,
            status = status,
            disbursedBy = "নিতাই চন্দ্র রায়"
        )
    }

    @Test
    fun findUpcomingWeeklyRepayments_includesOverdueDueTodayAndWithinLeadDays() {
        val now = startOfDay(2026, Calendar.NOVEMBER, 13)
        val dayMillis = TimeUnit.DAYS.toMillis(1)

        // Loan 1: next installment (1st) due in 2 days (disbursed 5 days ago -> due at +7d = +2d from now)
        val loanDueIn2Days = createLoan(
            id = 1,
            memberName = "Sumon Roy",
            disbursedDate = now - 5 * dayMillis
        )
        // Loan 2: next installment (1st) due today (disbursed 7 days ago)
        val loanDueToday = createLoan(
            id = 2,
            memberName = "Kajol Roy",
            disbursedDate = now - 7 * dayMillis
        )
        // Loan 3: next installment (1st) overdue by 3 days (disbursed 10 days ago)
        val loanOverdue = createLoan(
            id = 3,
            memberName = "Ratan Das",
            disbursedDate = now - 10 * dayMillis
        )
        // Loan 4: next installment due in 5 days (outside leadDays = 2)
        val loanFarFuture = createLoan(
            id = 4,
            memberName = "Asha Rani",
            disbursedDate = now - 2 * dayMillis
        )
        // Loan 5: monthly loan (should be excluded from weekly reminder)
        val monthlyLoan = createLoan(
            id = 5,
            memberName = "Kamal Hasan",
            disbursedDate = now - 30 * dayMillis,
            frequency = "MONTHLY"
        )
        // Loan 6: closed loan (should be excluded)
        val closedLoan = createLoan(
            id = 6,
            memberName = "Bimal Roy",
            disbursedDate = now - 10 * dayMillis,
            paidInstallments = 10,
            remainingDue = 0.0,
            status = "CLOSED"
        )

        val result = LoanReminderCalculator.findUpcomingWeeklyRepayments(
            loans = listOf(loanDueIn2Days, loanDueToday, loanOverdue, loanFarFuture, monthlyLoan, closedLoan),
            nowMillis = now,
            leadDays = 2
        )

        assertEquals(3, result.size)
        // Sorted chronologically by due date: overdue -> dueToday -> dueIn2Days
        assertEquals("Ratan Das", result[0].loan.memberName)
        assertEquals(-3, result[0].daysUntilDue)
        assertTrue(result[0].isOverdue)

        assertEquals("Kajol Roy", result[1].loan.memberName)
        assertEquals(0, result[1].daysUntilDue)
        assertTrue(result[1].isDueToday)

        assertEquals("Sumon Roy", result[2].loan.memberName)
        assertEquals(2, result[2].daysUntilDue)
    }

    @Test
    fun findUpcomingWeeklyRepayments_capsExpectedAmountToRemainingDue() {
        val now = startOfDay(2026, Calendar.NOVEMBER, 13)
        val dayMillis = TimeUnit.DAYS.toMillis(1)

        // Paid 9 of 10 installments, only 200 remaining even though perInstallmentAmount is 550
        val finalInstallmentLoan = createLoan(
            id = 7,
            memberName = "Nita Rani",
            disbursedDate = now - 70 * dayMillis,
            paidInstallments = 9,
            totalInstallments = 10,
            perInstallmentAmount = 550.0,
            remainingDue = 200.0
        )

        val result = LoanReminderCalculator.findUpcomingWeeklyRepayments(
            loans = listOf(finalInstallmentLoan),
            nowMillis = now,
            leadDays = 2
        )

        assertEquals(1, result.size)
        assertEquals(10, result[0].nextInstallmentNumber)
        assertEquals(200.0, result[0].expectedAmount, 0.01)
    }

    @Test
    fun formatDueBadgeText_formatsBengaliLabelsAccurately() {
        assertEquals("2 দিন বকেয়া", LoanReminderCalculator.formatDueBadgeText(-2))
        assertEquals("আজ পরিশোধের দিন", LoanReminderCalculator.formatDueBadgeText(0))
        assertEquals("আগামীকাল", LoanReminderCalculator.formatDueBadgeText(1))
        assertEquals("3 দিন বাকি", LoanReminderCalculator.formatDueBadgeText(3))
    }

    @Test
    fun findUpcomingMemberRepayments_highlightsOnlyLoansWithinNextThreeDays() {
        val now = startOfDay(2026, Calendar.NOVEMBER, 14)
        val dayMillis = TimeUnit.DAYS.toMillis(1)

        // Due today (0 days): weekly disbursed 7 days ago -> INCLUDED
        val dueToday = createLoan(
            id = 10,
            memberName = "Karim Mia",
            disbursedDate = now - 7 * dayMillis
        )
        // Due in exactly 3 days: weekly disbursed 4 days ago -> due +3d -> INCLUDED
        val dueIn3Days = createLoan(
            id = 11,
            memberName = "Rahima Begum",
            disbursedDate = now - 4 * dayMillis
        )
        // Due in 4 days: weekly disbursed 3 days ago -> due +4d -> EXCLUDED
        val dueIn4Days = createLoan(
            id = 12,
            memberName = "Sajib Hasan",
            disbursedDate = now - 3 * dayMillis
        )
        // Overdue (-1 day): disbursed 8 days ago -> EXCLUDED when includeOverdue = false
        val overdue = createLoan(
            id = 13,
            memberName = "Jamal Uddin",
            disbursedDate = now - 8 * dayMillis
        )
        // Monthly loan due in 2 days (disbursed Oct 16 -> due Nov 16) -> INCLUDED
        val monthlyDueIn2Days = createLoan(
            id = 14,
            memberName = "Salma Akter",
            disbursedDate = startOfDay(2026, Calendar.OCTOBER, 16),
            frequency = "MONTHLY"
        )
        // Closed loan -> EXCLUDED
        val closed = createLoan(
            id = 15,
            memberName = "Nadia Islam",
            disbursedDate = now - 7 * dayMillis,
            paidInstallments = 10,
            remainingDue = 0.0,
            status = "CLOSED"
        )

        val result = LoanReminderCalculator.findUpcomingMemberRepayments(
            loans = listOf(dueToday, dueIn3Days, dueIn4Days, overdue, monthlyDueIn2Days, closed),
            nowMillis = now,
            withinDays = 3,
            includeOverdue = false
        )

        assertEquals(3, result.size)
        assertTrue(result.containsKey("MEM-0010"))
        assertEquals(0, result["MEM-0010"]?.daysUntilDue)

        assertTrue(result.containsKey("MEM-0011"))
        assertEquals(3, result["MEM-0011"]?.daysUntilDue)

        assertTrue(result.containsKey("MEM-0014"))
        assertEquals(2, result["MEM-0014"]?.daysUntilDue)
    }
}
