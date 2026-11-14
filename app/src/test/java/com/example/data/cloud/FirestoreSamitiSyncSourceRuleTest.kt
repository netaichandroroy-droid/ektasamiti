package com.example.data.cloud

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.InstallmentPayment
import com.example.data.model.Loan
import com.example.data.model.Member
import com.example.data.model.SavingsTransaction
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class FirestoreSamitiSyncSourceRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun upsertMember_authenticatedOwner_writesAndObservesInRealtime() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val syncSource = FirestoreSamitiSyncSource(firestore, auth)
        val code = "ES-${UUID.randomUUID().toString().take(8)}"
        val member = Member(
            id = 1L,
            memberCode = code,
            name = "Karim Mia",
            mobile = "01711000111",
            memberType = "WEEKLY",
            savingsBalance = 500.0,
            registeredBy = "Admin"
        )

        withTimeout(DEFAULT_TIMEOUT_MS) {
            syncSource.upsertMember(member)
            val observed = syncSource.observeMembers().first { list ->
                list.any { it.memberCode == code && it.savingsBalance == 500.0 }
            }
            assertTrue(observed.any { it.memberCode == code })

            // Subsequent update preserves createdAt and updates savingsBalance cleanly
            syncSource.upsertMember(member.copy(savingsBalance = 800.0))
            val updatedList = syncSource.observeMembers().first { list ->
                list.any { it.memberCode == code && it.savingsBalance == 800.0 }
            }
            assertEquals(800.0, updatedList.first { it.memberCode == code }.savingsBalance, 0.01)
        }
    }

    @Test
    fun savingsLoanInstallment_authenticatedOwner_syncsCleanly() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val syncSource = FirestoreSamitiSyncSource(firestore, auth)
        val suffix = UUID.randomUUID().toString().take(8)
        val code = "ES-$suffix"
        val now = System.currentTimeMillis()

        val tx = SavingsTransaction(
            id = 1L,
            memberId = 1L,
            memberCode = code,
            memberName = "Rahima Begum",
            type = "DEPOSIT",
            savingsPlan = "WEEKLY",
            amount = 200.0,
            balanceAfter = 200.0,
            date = now,
            note = "Weekly savings",
            collectedBy = "Admin",
            collectedByRole = "ADMIN",
            receiptNumber = "REC-SAV-$suffix"
        )
        val loan = Loan(
            id = 1L,
            memberId = 1L,
            memberCode = code,
            memberName = "Rahima Begum",
            memberMobile = "01811223344",
            principalAmount = 10000.0,
            interestRate = 10.0,
            chargeAmount = 1000.0,
            totalPayable = 11000.0,
            totalPaid = 0.0,
            remainingDue = 11000.0,
            totalInstallments = 20,
            paidInstallments = 0,
            perInstallmentAmount = 550.0,
            frequency = "WEEKLY",
            disbursedDate = now,
            disbursedBy = "Admin",
            status = "ACTIVE",
            purpose = "Business"
        )
        val loanKey = FirestoreDocIds.loanKey(code, now)
        val payment = InstallmentPayment(
            id = 1L,
            loanId = 1L,
            memberId = 1L,
            memberCode = code,
            memberName = "Rahima Begum",
            installmentNumber = 1,
            paidAmount = 550.0,
            penaltyAmount = 0.0,
            paymentDate = now + 1000L,
            remainingAfterPayment = 10450.0,
            collectedBy = "Admin",
            receiptNumber = "REC-INS-$suffix",
            notes = "Installment 1"
        )

        withTimeout(DEFAULT_TIMEOUT_MS) {
            syncSource.createSavingsTransactionIfMissing(tx)
            syncSource.upsertLoan(loan)
            syncSource.createInstallmentPaymentIfMissing(payment, loanKey)

            val savedTx = syncSource.observeSavingsTransactions().first { list ->
                list.any { it.receiptNumber == tx.receiptNumber }
            }
            assertTrue(savedTx.isNotEmpty())

            val savedLoan = syncSource.observeLoans().first { list ->
                list.any { it.loanKey == loanKey }
            }
            assertEquals("ACTIVE", savedLoan.first { it.loanKey == loanKey }.status)

            val savedInstallments = syncSource.observeInstallmentPayments().first { list ->
                list.any { it.receiptNumber == payment.receiptNumber }
            }
            assertEquals(550.0, savedInstallments.first { it.receiptNumber == payment.receiptNumber }.paidAmount, 0.01)
        }
    }

    @Test
    fun observeMembers_unauthenticatedUser_failsWithPermissionDeniedOrAuthGuard() = runBlocking {
        auth.signOut()
        val syncSource = FirestoreSamitiSyncSource(firestore, auth)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                syncSource.observeMembers().first()
            }
            fail("Expected failure for unauthenticated observeMembers()")
        } catch (e: Exception) {
            val firestoreException = e.unwrapFirestoreException()
            assertTrue(
                e is IllegalStateException ||
                    firestoreException?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
            )
        }
    }

    @Test
    fun directFirestoreList_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("users").document("some_uid").collection("members")
                    .get()
                    .let { com.google.android.gms.tasks.Tasks.await(it) }
            }
            fail("Expected PERMISSION_DENIED on unauthenticated read")
        } catch (e: Exception) {
            val fsEx = e.unwrapFirestoreException()
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice_samiti@test.com"
        const val DEFAULT_TIMEOUT_MS = 6000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
