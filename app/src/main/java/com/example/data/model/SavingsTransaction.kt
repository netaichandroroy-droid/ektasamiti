package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "savings_transactions",
    indices = [
        Index(value = ["memberId"]),
        Index(value = ["date"])
    ]
)
data class SavingsTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val memberId: Long,
    val memberCode: String,
    val memberName: String,
    val type: String = "DEPOSIT", // "DEPOSIT" (জমা) or "WITHDRAWAL" (উত্তোলন)
    val savingsPlan: String = "WEEKLY", // "WEEKLY" (সাপ্তাহিক সঞ্চয়)
    val amount: Double,
    val balanceAfter: Double = 0.0,
    val date: Long = System.currentTimeMillis(),
    val note: String = "",
    val collectedBy: String = "Admin",
    val collectedByRole: String = "ADMIN",
    val receiptNumber: String = ""
)
