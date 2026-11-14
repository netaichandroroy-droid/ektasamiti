package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.SavingsTransaction
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingsDao {
    @Query("SELECT * FROM savings_transactions ORDER BY date DESC")
    fun getAllTransactionsFlow(): Flow<List<SavingsTransaction>>

    @Query("SELECT * FROM savings_transactions WHERE memberId = :memberId ORDER BY date DESC")
    fun getTransactionsForMemberFlow(memberId: Long): Flow<List<SavingsTransaction>>

    @Query("SELECT * FROM savings_transactions WHERE date >= :startOfDay AND date <= :endOfDay ORDER BY date DESC")
    fun getTransactionsBetweenDatesFlow(startOfDay: Long, endOfDay: Long): Flow<List<SavingsTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: SavingsTransaction): Long

    @Delete
    suspend fun deleteTransaction(transaction: SavingsTransaction)

    @Query("SELECT * FROM savings_transactions")
    suspend fun getAllTransactionsList(): List<SavingsTransaction>

    @Query("SELECT * FROM savings_transactions WHERE receiptNumber = :receiptNumber LIMIT 1")
    suspend fun getTransactionByReceipt(receiptNumber: String): SavingsTransaction?

    @Query("DELETE FROM savings_transactions")
    suspend fun deleteAllTransactions()
}
