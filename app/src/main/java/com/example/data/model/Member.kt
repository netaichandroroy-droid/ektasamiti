package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "members",
    indices = [Index(value = ["memberCode"], unique = true)]
)
data class Member(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val memberCode: String,
    val name: String,
    val mobile: String,
    val fatherOrHusbandName: String = "",
    val nid: String = "",
    val address: String = "",
    val memberType: String = "WEEKLY", // "WEEKLY" (সাপ্তাহিক সঞ্চয়ী)
    val nomineeName: String = "",
    val nomineeRelation: String = "",
    val photoUri: String? = null,
    val admissionFee: Double = 100.0,
    val savingsBalance: Double = 0.0,
    val activeLoanCount: Int = 0,
    val totalLoanDue: Double = 0.0,
    val registrationDate: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val registeredBy: String = "Admin"
)
