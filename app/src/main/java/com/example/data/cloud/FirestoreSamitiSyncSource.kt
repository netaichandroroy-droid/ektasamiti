package com.example.data.cloud

import android.content.Context
import com.example.R
import com.example.data.model.InstallmentPayment
import com.example.data.model.Loan
import com.example.data.model.Member
import com.example.data.model.SavingsTransaction
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirestoreSamitiSyncSource(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        auth = Firebase.auth
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Google Sign-In required before accessing Firestore sync.")
    }

    fun observeMembers(): Flow<List<CloudMember>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/members"
        emitAll(
            db.collection("users").document(uid).collection("members")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CloudMember::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = unwrapFirestoreException(error)
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeSavingsTransactions(): Flow<List<CloudSavingsTransaction>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/savings_transactions"
        emitAll(
            db.collection("users").document(uid).collection("savings_transactions")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CloudSavingsTransaction::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = unwrapFirestoreException(error)
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeLoans(): Flow<List<CloudLoan>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/loans"
        emitAll(
            db.collection("users").document(uid).collection("loans")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CloudLoan::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = unwrapFirestoreException(error)
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeInstallmentPayments(): Flow<List<CloudInstallmentPayment>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/installment_payments"
        emitAll(
            db.collection("users").document(uid).collection("installment_payments")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CloudInstallmentPayment::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = unwrapFirestoreException(error)
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun upsertMember(member: Member): String {
        val uid = requireUserId()
        val docId = FirestoreDocIds.memberId(member.memberCode)
        val docRef = db.collection("users").document(uid).collection("members").document(docId)
        try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(member.toFirestoreUpdateMap()).await()
            } else {
                docRef.set(member.toFirestoreCreateMap(uid)).await()
            }
            return docId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun createSavingsTransactionIfMissing(tx: SavingsTransaction): String {
        val uid = requireUserId()
        val receipt = tx.receiptNumber.ifBlank { "REC-SAV-${tx.id}_${tx.date}" }
        val docId = FirestoreDocIds.savingsId(receipt)
        val docRef = db.collection("users").document(uid).collection("savings_transactions").document(docId)
        try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                docRef.set(tx.copy(receiptNumber = receipt).toFirestoreCreateMap(uid)).await()
            }
            return docId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun upsertLoan(loan: Loan): String {
        val uid = requireUserId()
        val docId = FirestoreDocIds.loanKey(loan.memberCode, loan.disbursedDate)
        val docRef = db.collection("users").document(uid).collection("loans").document(docId)
        try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                val remoteStatus = snap.getString("status")
                if (remoteStatus != "CLOSED") {
                    docRef.update(loan.toFirestoreUpdateMap()).await()
                }
            } else {
                docRef.set(loan.toFirestoreCreateMap(uid)).await()
            }
            return docId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun createInstallmentPaymentIfMissing(
        payment: InstallmentPayment,
        loanKey: String
    ): String {
        val uid = requireUserId()
        val receipt = payment.receiptNumber.ifBlank { "REC-INS-${payment.id}_${payment.paymentDate}" }
        val docId = FirestoreDocIds.installmentId(receipt)
        val docRef = db.collection("users").document(uid).collection("installment_payments").document(docId)
        try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                docRef.set(payment.copy(receiptNumber = receipt).toFirestoreCreateMap(uid, loanKey)).await()
            }
            return docId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteMember(memberCode: String) {
        val uid = requireUserId()
        val docId = FirestoreDocIds.memberId(memberCode)
        val docRef = db.collection("users").document(uid).collection("members").document(docId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun deleteSavingsTransaction(receiptNumber: String) {
        val uid = requireUserId()
        val docId = FirestoreDocIds.savingsId(receiptNumber)
        val docRef = db.collection("users").document(uid).collection("savings_transactions").document(docId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun deleteLoan(memberCode: String, disbursedDate: Long) {
        val uid = requireUserId()
        val docId = FirestoreDocIds.loanKey(memberCode, disbursedDate)
        val docRef = db.collection("users").document(uid).collection("loans").document(docId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun deleteInstallmentPayment(receiptNumber: String) {
        val uid = requireUserId()
        val docId = FirestoreDocIds.installmentId(receiptNumber)
        val docRef = db.collection("users").document(uid).collection("installment_payments").document(docId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun fetchAllMembersOnce(): List<CloudMember> {
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("members")
        return try {
            colRef.get().await().toObjects(
                CloudMember::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, colRef.path)
            throw e
        }
    }
}
