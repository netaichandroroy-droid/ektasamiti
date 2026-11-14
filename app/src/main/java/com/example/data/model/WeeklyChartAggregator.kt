package com.example.data.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class WeeklyChartBucket(
    val weekIndex: Int, // 0 (oldest) .. 3 (current week)
    val shortLabel: String, // e.g. "17-23 Oct"
    val fullLabel: String, // e.g. "চলতি সপ্তাহ (07 Nov - 13 Nov)"
    val savingsTotal: Double,
    val loanCollectionTotal: Double
) {
    val combinedTotal: Double get() = savingsTotal + loanCollectionTotal
}

object WeeklyChartAggregator {

    fun computeLast4Weeks(
        savingsList: List<SavingsTransaction>,
        installmentsList: List<InstallmentPayment>,
        nowMillis: Long = System.currentTimeMillis()
    ): List<WeeklyChartBucket> {
        val endCal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val endOfToday = endCal.timeInMillis

        val startOfTodayCal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfToday = startOfTodayCal.timeInMillis
        val oneDayMs = 86_400_000L

        val shortSdf = SimpleDateFormat("dd/MM", Locale.getDefault())
        val weekTitles = listOf("৩ সপ্তাহ আগে", "২ সপ্তাহ আগে", "গত সপ্তাহ", "চলতি সপ্তাহ")

        return (0..3).map { idx ->
            // idx = 0 => 27..21 days ago; idx = 3 => 6..0 days ago
            val daysBackEnd = (3 - idx) * 7
            val daysBackStart = daysBackEnd + 6

            val bucketStart = startOfToday - (daysBackStart.toLong() * oneDayMs)
            val bucketEnd = if (idx == 3) {
                endOfToday
            } else {
                startOfToday - ((daysBackEnd - 1).toLong() * oneDayMs) - 1L
            }

            val savingsSum = savingsList
                .filter { it.type == "DEPOSIT" && it.date in bucketStart..bucketEnd }
                .sumOf { it.amount }

            val loanSum = installmentsList
                .filter { it.paymentDate in bucketStart..bucketEnd }
                .sumOf { it.paidAmount }

            val startStr = shortSdf.format(Date(bucketStart))
            val endStr = shortSdf.format(Date(bucketEnd))

            WeeklyChartBucket(
                weekIndex = idx,
                shortLabel = "$startStr-$endStr",
                fullLabel = "${weekTitles[idx]} ($startStr - $endStr)",
                savingsTotal = savingsSum,
                loanCollectionTotal = loanSum
            )
        }
    }
}
