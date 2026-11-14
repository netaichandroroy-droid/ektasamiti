package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.InstallmentPayment
import kotlinx.coroutines.flow.Flow

@Dao
interface InstallmentDao {
    @Query("SELECT * FROM installment_payments ORDER BY paymentDate DESC")
    fun getAllPaymentsFlow(): Flow<List<InstallmentPayment>>

    @Query("SELECT * FROM installment_payments WHERE loanId = :loanId ORDER BY installmentNumber ASC")
    fun getPaymentsForLoanFlow(loanId: Long): Flow<List<InstallmentPayment>>

    @Query("SELECT * FROM installment_payments WHERE memberId = :memberId ORDER BY paymentDate DESC")
    fun getPaymentsForMemberFlow(memberId: Long): Flow<List<InstallmentPayment>>

    @Query("SELECT * FROM installment_payments WHERE paymentDate >= :startOfDay AND paymentDate <= :endOfDay ORDER BY paymentDate DESC")
    fun getPaymentsBetweenDatesFlow(startOfDay: Long, endOfDay: Long): Flow<List<InstallmentPayment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: InstallmentPayment): Long

    @Delete
    suspend fun deletePayment(payment: InstallmentPayment)

    @Query("SELECT * FROM installment_payments")
    suspend fun getAllPaymentsList(): List<InstallmentPayment>

    @Query("SELECT * FROM installment_payments WHERE receiptNumber = :receiptNumber LIMIT 1")
    suspend fun getPaymentByReceipt(receiptNumber: String): InstallmentPayment?

    @Query("DELETE FROM installment_payments")
    suspend fun deleteAllPayments()
}
