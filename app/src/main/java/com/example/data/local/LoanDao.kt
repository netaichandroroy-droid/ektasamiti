package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Loan
import kotlinx.coroutines.flow.Flow

@Dao
interface LoanDao {
    @Query("SELECT * FROM loans ORDER BY id DESC")
    fun getAllLoansFlow(): Flow<List<Loan>>

    @Query("SELECT * FROM loans WHERE status = 'ACTIVE' ORDER BY id DESC")
    fun getActiveLoansFlow(): Flow<List<Loan>>

    @Query("SELECT * FROM loans WHERE memberId = :memberId ORDER BY id DESC")
    fun getLoansForMemberFlow(memberId: Long): Flow<List<Loan>>

    @Query("SELECT * FROM loans WHERE id = :id LIMIT 1")
    suspend fun getLoanById(id: Long): Loan?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoan(loan: Loan): Long

    @Update
    suspend fun updateLoan(loan: Loan)

    @Delete
    suspend fun deleteLoan(loan: Loan)

    @Query("SELECT * FROM loans")
    suspend fun getAllLoansList(): List<Loan>

    @Query("SELECT * FROM loans WHERE memberCode = :memberCode AND disbursedDate = :disbursedDate LIMIT 1")
    suspend fun getLoanByMemberCodeAndDate(memberCode: String, disbursedDate: Long): Loan?

    @Query("DELETE FROM loans")
    suspend fun deleteAllLoans()
}
