package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "loans",
    indices = [
        Index(value = ["memberId"]),
        Index(value = ["status"])
    ]
)
data class Loan(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val memberId: Long,
    val memberCode: String,
    val memberName: String,
    val memberMobile: String = "",
    val principalAmount: Double, // মূল ঋণ যেমন ২০,০০০ টাকা
    val interestRate: Double = 0.0, // সুদের হার (%) যেমন ১০%
    val chargeAmount: Double, // নির্দিষ্ট সুদ / চার্জ যেমন ২,০০০ টাকা (অথবা ১০%)
    val totalPayable: Double, // মোট পরিশোধযোগ্য = ২২,০০০ টাকা
    val totalPaid: Double = 0.0, // এ পর্যন্ত আদায়কৃত কিস্তি
    val remainingDue: Double = totalPayable, // বকেয়া ঋণ
    val totalInstallments: Int = 20, // মোট কিস্তি সংখ্যা যেমন ২০ কিস্তি
    val paidInstallments: Int = 0, // আদায়কৃত কিস্তির সংখ্যা
    val perInstallmentAmount: Double, // প্রতি কিস্তির পরিমাণ যেমন ১,১০০ টাকা
    val frequency: String = "WEEKLY", // "WEEKLY" (সাপ্তাহিক) or "MONTHLY" (মাসিক)
    val disbursedDate: Long = System.currentTimeMillis(),
    val disbursedBy: String = "Admin",
    val status: String = "ACTIVE", // "ACTIVE" (চলমান) or "CLOSED" (পরিশোধিত)
    val purpose: String = "ব্যবসা সম্প্রসারণ" // ঋণের উদ্দেশ্য
)
