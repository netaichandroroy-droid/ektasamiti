package com.example

import com.example.data.model.InstallmentPayment
import com.example.data.model.SavingsTransaction
import com.example.data.model.WeeklyChartAggregator
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class WeeklyChartAggregatorTest {

    @Test
    fun `computeLast4Weeks groups savings deposits and loan collections into 4 weekly buckets`() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 13, 14, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val oneDayMs = 86_400_000L

        val savings = listOf(
            // Current week (today) deposit: 500
            SavingsTransaction(
                memberId = 1L,
                memberCode = "ES-1001",
                memberName = "A",
                type = "DEPOSIT",
                savingsPlan = "WEEKLY",
                amount = 500.0,
                balanceAfter = 500.0,
                date = now,
                receiptNumber = "R1"
            ),
            // Current week withdrawal should NOT count in deposit total
            SavingsTransaction(
                memberId = 1L,
                memberCode = "ES-1001",
                memberName = "A",
                type = "WITHDRAWAL",
                savingsPlan = "WEEKLY",
                amount = 200.0,
                balanceAfter = 300.0,
                date = now,
                receiptNumber = "R2"
            ),
            // 10 days ago (bucket index 2: "গত সপ্তাহ") deposit: 300
            SavingsTransaction(
                memberId = 2L,
                memberCode = "ES-1002",
                memberName = "B",
                type = "DEPOSIT",
                savingsPlan = "WEEKLY",
                amount = 300.0,
                balanceAfter = 300.0,
                date = now - (10 * oneDayMs),
                receiptNumber = "R3"
            ),
            // 35 days ago (outside 4 weeks) deposit: 999
            SavingsTransaction(
                memberId = 3L,
                memberCode = "ES-1003",
                memberName = "C",
                type = "DEPOSIT",
                savingsPlan = "WEEKLY",
                amount = 999.0,
                balanceAfter = 999.0,
                date = now - (35 * oneDayMs),
                receiptNumber = "R4"
            )
        )

        val installments = listOf(
            // 23 days ago (bucket index 0: "৩ সপ্তাহ আগে") installment: 1100
            InstallmentPayment(
                loanId = 1L,
                memberId = 1L,
                memberCode = "ES-1001",
                memberName = "A",
                installmentNumber = 1,
                paidAmount = 1100.0,
                paymentDate = now - (23 * oneDayMs),
                remainingAfterPayment = 9900.0,
                collectedBy = "Admin",
                receiptNumber = "I1"
            ),
            // Current week installment: 1200
            InstallmentPayment(
                loanId = 1L,
                memberId = 1L,
                memberCode = "ES-1001",
                memberName = "A",
                installmentNumber = 2,
                paidAmount = 1200.0,
                paymentDate = now - (2 * oneDayMs),
                remainingAfterPayment = 8700.0,
                collectedBy = "Admin",
                receiptNumber = "I2"
            )
        )

        val buckets = WeeklyChartAggregator.computeLast4Weeks(savings, installments, now)
        assertEquals(4, buckets.size)

        // Oldest week (index 0)
        assertEquals(0.0, buckets[0].savingsTotal, 0.01)
        assertEquals(1100.0, buckets[0].loanCollectionTotal, 0.01)

        // 2 weeks ago (index 1)
        assertEquals(0.0, buckets[1].savingsTotal, 0.01)
        assertEquals(0.0, buckets[1].loanCollectionTotal, 0.01)

        // Last week (index 2)
        assertEquals(300.0, buckets[2].savingsTotal, 0.01)
        assertEquals(0.0, buckets[2].loanCollectionTotal, 0.01)

        // Current week (index 3)
        assertEquals(500.0, buckets[3].savingsTotal, 0.01)
        assertEquals(1200.0, buckets[3].loanCollectionTotal, 0.01)
        assertEquals(1700.0, buckets[3].combinedTotal, 0.01)
    }
}
