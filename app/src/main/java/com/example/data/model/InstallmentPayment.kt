package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "installment_payments",
    indices = [
        Index(value = ["loanId"]),
        Index(value = ["memberId"]),
        Index(value = ["paymentDate"])
    ]
)
data class InstallmentPayment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val loanId: Long,
    val memberId: Long,
    val memberCode: String,
    val memberName: String,
    val installmentNumber: Int,
    val paidAmount: Double,
    val penaltyAmount: Double = 0.0,
    val paymentDate: Long = System.currentTimeMillis(),
    val remainingAfterPayment: Double = 0.0,
    val collectedBy: String = "Admin",
    val receiptNumber: String = "",
    val notes: String = ""
)
