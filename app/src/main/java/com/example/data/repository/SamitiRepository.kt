package com.example.data.repository

import com.example.data.cloud.CloudAutoSyncManager
import com.example.data.local.AppDatabase
import com.example.data.model.InstallmentPayment
import com.example.data.model.Loan
import com.example.data.model.Member
import com.example.data.model.SavingsTransaction
import com.example.data.model.User
import com.example.data.model.UserRole
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.roundToInt

class SamitiRepository(
    private val database: AppDatabase,
    private val cloudAutoSyncManager: CloudAutoSyncManager? = null
) {
    private val userDao = database.userDao()
    private val memberDao = database.memberDao()
    private val savingsDao = database.savingsDao()
    private val loanDao = database.loanDao()
    private val installmentDao = database.installmentDao()
    private val receiptCounter = AtomicInteger(10)

    private fun nextReceipt(prefix: String): String {
        val seq = receiptCounter.incrementAndGet() % 10000
        return "$prefix-${System.currentTimeMillis()}-$seq"
    }

    // Auth
    suspend fun authenticate(email: String, password: String): User? {
        val user = userDao.getUserByEmail(email.trim()) ?: return null
        if (user.passwordHash == password.trim() && user.isActive) {
            return user
        }
        return null
    }

    suspend fun getUserById(userId: Long): User? = userDao.getUserById(userId)

    suspend fun getUserByEmail(email: String): User? {
        val user = userDao.getUserByEmail(email.trim())
        return if (user?.isActive == true) user else null
    }

    suspend fun registerUser(user: User): Long {
        return userDao.insertUser(user)
    }

    fun getAllCollectorsFlow(): Flow<List<User>> = userDao.getAllCollectorsFlow()

    // Members
    fun getAllMembersFlow(): Flow<List<Member>> = memberDao.getAllMembersFlow()

    fun searchMembers(query: String): Flow<List<Member>> {
        return if (query.isBlank()) {
            memberDao.getAllMembersFlow()
        } else {
            memberDao.searchMembers(query)
        }
    }

    suspend fun getMemberById(id: Long): Member? = memberDao.getMemberById(id)

    suspend fun addMember(member: Member): Long {
        val id = memberDao.insertMember(member)
        cloudAutoSyncManager?.pushMemberAsync(member.memberCode)
        return id
    }

    suspend fun updateMember(member: Member) {
        memberDao.updateMember(member)
        cloudAutoSyncManager?.pushMemberAsync(member.memberCode)
    }

    suspend fun deleteMember(member: Member, currentUserRole: UserRole): Boolean {
        if (currentUserRole != UserRole.ADMIN) {
            return false // Restricted! Only Admin can delete data
        }
        memberDao.deleteMember(member)
        cloudAutoSyncManager?.deleteMemberFromCloudAsync(member.memberCode)
        return true
    }

    // Savings
    fun getAllSavingsFlow(): Flow<List<SavingsTransaction>> = savingsDao.getAllTransactionsFlow()

    fun getSavingsForMember(memberId: Long): Flow<List<SavingsTransaction>> =
        savingsDao.getTransactionsForMemberFlow(memberId)

    suspend fun recordSavingsDeposit(
        member: Member,
        amount: Double,
        savingsPlan: String,
        note: String,
        collectedBy: String,
        role: UserRole,
        customDate: Long = System.currentTimeMillis()
    ): SavingsTransaction {
        val newBalance = member.savingsBalance + amount
        val receipt = nextReceipt("REC-SAV")
        val transaction = SavingsTransaction(
            memberId = member.id,
            memberCode = member.memberCode,
            memberName = member.name,
            type = "DEPOSIT",
            savingsPlan = savingsPlan,
            amount = amount,
            balanceAfter = newBalance,
            date = customDate,
            note = note.ifBlank { "নিয়মিত সঞ্চয় জমা" },
            collectedBy = collectedBy,
            collectedByRole = role.name,
            receiptNumber = receipt
        )
        val id = savingsDao.insertTransaction(transaction)
        memberDao.updateMember(member.copy(savingsBalance = newBalance))
        cloudAutoSyncManager?.pushSavingsByReceiptAsync(receipt, member.memberCode)
        return transaction.copy(id = id)
    }

    suspend fun recordSavingsWithdrawal(
        member: Member,
        amount: Double,
        note: String,
        collectedBy: String,
        role: UserRole
    ): SavingsTransaction? {
        if (member.savingsBalance < amount) return null
        val newBalance = member.savingsBalance - amount
        val receipt = nextReceipt("REC-WTH")
        val transaction = SavingsTransaction(
            memberId = member.id,
            memberCode = member.memberCode,
            memberName = member.name,
            type = "WITHDRAWAL",
            savingsPlan = member.memberType,
            amount = amount,
            balanceAfter = newBalance,
            date = System.currentTimeMillis(),
            note = note.ifBlank { "সঞ্চয় উত্তোলন" },
            collectedBy = collectedBy,
            collectedByRole = role.name,
            receiptNumber = receipt
        )
        val id = savingsDao.insertTransaction(transaction)
        memberDao.updateMember(member.copy(savingsBalance = newBalance))
        cloudAutoSyncManager?.pushSavingsByReceiptAsync(receipt, member.memberCode)
        return transaction.copy(id = id)
    }

    // Loans
    fun getAllLoansFlow(): Flow<List<Loan>> = loanDao.getAllLoansFlow()

    fun getActiveLoansFlow(): Flow<List<Loan>> = loanDao.getActiveLoansFlow()

    fun getLoansForMember(memberId: Long): Flow<List<Loan>> = loanDao.getLoansForMemberFlow(memberId)

    suspend fun disburseLoan(
        member: Member,
        principalAmount: Double,
        chargeAmount: Double,
        totalInstallments: Int,
        frequency: String,
        purpose: String,
        disbursedBy: String,
        interestRate: Double = if (principalAmount > 0) ((chargeAmount / principalAmount) * 10000.0).roundToInt() / 100.0 else 0.0
    ): Loan {
        val totalPayable = principalAmount + chargeAmount
        val perInstallment = ((totalPayable / totalInstallments) * 100.0).roundToInt() / 100.0
        val loan = Loan(
            memberId = member.id,
            memberCode = member.memberCode,
            memberName = member.name,
            memberMobile = member.mobile,
            principalAmount = principalAmount,
            interestRate = interestRate,
            chargeAmount = chargeAmount,
            totalPayable = totalPayable,
            totalPaid = 0.0,
            remainingDue = totalPayable,
            totalInstallments = totalInstallments,
            paidInstallments = 0,
            perInstallmentAmount = perInstallment,
            frequency = frequency,
            disbursedDate = System.currentTimeMillis(),
            disbursedBy = disbursedBy,
            status = "ACTIVE",
            purpose = purpose.ifBlank { "ব্যবসায়িক বিনিয়োগ" }
        )
        val loanId = loanDao.insertLoan(loan)
        memberDao.updateMember(
            member.copy(
                activeLoanCount = member.activeLoanCount + 1,
                totalLoanDue = member.totalLoanDue + totalPayable
            )
        )
        cloudAutoSyncManager?.pushLoanAndInstallmentAsync(loan.memberCode, loan.disbursedDate)
        return loan.copy(id = loanId)
    }

    // Installments
    fun getAllInstallmentsFlow(): Flow<List<InstallmentPayment>> = installmentDao.getAllPaymentsFlow()

    fun getInstallmentsForLoan(loanId: Long): Flow<List<InstallmentPayment>> =
        installmentDao.getPaymentsForLoanFlow(loanId)

    suspend fun collectInstallment(
        loan: Loan,
        amount: Double,
        penalty: Double,
        collectedBy: String,
        notes: String
    ): InstallmentPayment {
        val newTotalPaid = loan.totalPaid + amount
        val newRemaining = (loan.totalPayable - newTotalPaid).coerceAtLeast(0.0)
        val newPaidInstallments = loan.paidInstallments + 1
        val isClosed = newRemaining <= 0.0

        val receipt = nextReceipt("REC-INS")
        val payment = InstallmentPayment(
            loanId = loan.id,
            memberId = loan.memberId,
            memberCode = loan.memberCode,
            memberName = loan.memberName,
            installmentNumber = newPaidInstallments,
            paidAmount = amount,
            penaltyAmount = penalty,
            paymentDate = System.currentTimeMillis(),
            remainingAfterPayment = newRemaining,
            collectedBy = collectedBy,
            receiptNumber = receipt,
            notes = notes.ifBlank { "নিয়মিত কিস্তি আদায়" }
        )
        val paymentId = installmentDao.insertPayment(payment)

        // Update loan
        loanDao.updateLoan(
            loan.copy(
                totalPaid = newTotalPaid,
                paidInstallments = newPaidInstallments,
                remainingDue = newRemaining,
                status = if (isClosed) "CLOSED" else "ACTIVE"
            )
        )

        // Update member's totalLoanDue
        val member = memberDao.getMemberById(loan.memberId)
        if (member != null) {
            val updatedActiveLoans = if (isClosed) (member.activeLoanCount - 1).coerceAtLeast(0) else member.activeLoanCount
            val updatedDue = (member.totalLoanDue - amount).coerceAtLeast(0.0)
            memberDao.updateMember(
                member.copy(
                    activeLoanCount = updatedActiveLoans,
                    totalLoanDue = updatedDue
                )
            )
        }

        cloudAutoSyncManager?.pushLoanAndInstallmentAsync(loan.memberCode, loan.disbursedDate, receipt)
        return payment.copy(id = paymentId)
    }

    // Admin Deletions & Security
    suspend fun deleteLoan(loan: Loan, currentUserRole: UserRole): Boolean {
        if (currentUserRole != UserRole.ADMIN) return false
        loanDao.deleteLoan(loan)
        cloudAutoSyncManager?.deleteLoanFromCloudAsync(loan.memberCode, loan.disbursedDate)
        if (loan.status == "ACTIVE") {
            val member = memberDao.getMemberById(loan.memberId)
            if (member != null) {
                val updated = member.copy(
                    activeLoanCount = (member.activeLoanCount - 1).coerceAtLeast(0),
                    totalLoanDue = (member.totalLoanDue - loan.remainingDue).coerceAtLeast(0.0)
                )
                memberDao.updateMember(updated)
                cloudAutoSyncManager?.pushMemberAsync(updated.memberCode)
            }
        }
        return true
    }

    suspend fun deleteSavingsTransaction(transaction: SavingsTransaction, currentUserRole: UserRole): Boolean {
        if (currentUserRole != UserRole.ADMIN) return false
        savingsDao.deleteTransaction(transaction)
        cloudAutoSyncManager?.deleteSavingsFromCloudAsync(transaction.receiptNumber)
        return true
    }

    suspend fun deleteInstallmentPayment(payment: InstallmentPayment, currentUserRole: UserRole): Boolean {
        if (currentUserRole != UserRole.ADMIN) return false
        installmentDao.deletePayment(payment)
        return true
    }

    // Data Export & Backup Summary
    suspend fun generateBackupDataString(): String {
        val members = memberDao.getAllMembersList()
        val savings = savingsDao.getAllTransactionsList()
        val loans = loanDao.getAllLoansList()
        val installments = installmentDao.getAllPaymentsList()

        val sdf = SimpleDateFormat("dd-MMM-yyyy hh:mm a", Locale.getDefault())
        return buildString {
            append("=========================================\n")
            append("     একতা সমিতি - ব্যাকআপ ডাটা ফাইল       \n")
            append("তারিখ: ${sdf.format(Date())}\n")
            append("মোট সদস্য: ${members.size} জন\n")
            append("মোট সঞ্চয় লেনদেন: ${savings.size} টি\n")
            append("মোট ঋণ বিতরণ: ${loans.size} টি\n")
            append("মোট কিস্তি আদায়: ${installments.size} টি\n")
            append("=========================================\n\n")

            append("[সদস্য তালিকা]\n")
            members.forEach { m ->
                append("আইডি: ${m.memberCode} | নাম: ${m.name} | মোবাইল: ${m.mobile} | সঞ্চয় স্থিতি: ৳${m.savingsBalance} | বকেয়া ঋণ: ৳${m.totalLoanDue}\n")
            }

            append("\n[চলমান ও অতীত ঋণ]\n")
            loans.forEach { l ->
                val rate = com.example.data.model.LoanScheduleCalculator.effectiveInterestRate(l)
                append("সদস্য: ${l.memberName} (${l.memberCode}) | মূল ঋণ: ৳${l.principalAmount} | সুদের হার: $rate% | সার্ভিস চার্জ: ৳${l.chargeAmount} | সর্বমোট: ৳${l.totalPayable} | আদায়: ৳${l.totalPaid} | বকেয়া: ৳${l.remainingDue} | অবস্থা: ${if (l.status == "ACTIVE") "চলমান" else "পরিশোধিত"}\n")
            }

            append("\n[সর্বশেষ সঞ্চয় লেনদেন]\n")
            savings.take(50).forEach { s ->
                append("রসিদ: ${s.receiptNumber} | সদস্য: ${s.memberName} | ধরণ: ${if (s.type == "DEPOSIT") "জমা" else "উত্তোলন"} | পরিমাণ: ৳${s.amount} | তারিখ: ${sdf.format(Date(s.date))} | সংগ্রাহক: ${s.collectedBy}\n")
            }

            append("\n[সর্বশেষ কিস্তি আদায়]\n")
            installments.take(50).forEach { i ->
                append("রসিদ: ${i.receiptNumber} | সদস্য: ${i.memberName} | কিস্তি নং: ${i.installmentNumber} | পরিমাণ: ৳${i.paidAmount} | তারিখ: ${sdf.format(Date(i.paymentDate))} | সংগ্রাহক: ${i.collectedBy}\n")
            }
        }
    }

    suspend fun clearAllSamitiData(role: UserRole): Boolean {
        if (role != UserRole.ADMIN) return false
        installmentDao.deleteAllPayments()
        loanDao.deleteAllLoans()
        savingsDao.deleteAllTransactions()
        memberDao.deleteAllMembers()
        return true
    }
}
