package com.example.data.cloud

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.example.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class CloudSyncStatus {
    NEEDS_GOOGLE_SIGN_IN,
    SYNCING,
    SYNCED,
    OFFLINE_PENDING,
    ERROR
}

data class CloudSyncState(
    val status: CloudSyncStatus = CloudSyncStatus.NEEDS_GOOGLE_SIGN_IN,
    val accountEmail: String? = null,
    val lastSyncedAtMillis: Long? = null,
    val syncedRecordCount: Int = 0,
    val errorMessage: String? = null,
    val isNetworkOnline: Boolean = true,
    val isAutoSyncEnabled: Boolean = true
)

class CloudAutoSyncManager(
    private val context: Context,
    private val database: AppDatabase,
    private val syncSource: FirestoreSamitiSyncSource = FirestoreSamitiSyncSource(context)
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val syncMutex = Mutex()
    private val reconcileMutex = Mutex()

    private val _syncState = MutableStateFlow(CloudSyncState())
    val syncState: StateFlow<CloudSyncState> = _syncState.asStateFlow()

    private var activeUserId: String? = null
    private var listenerJobs: List<Job> = emptyList()
    private var networkCallbackRegistered = false
    private var cachedCloudMembers: List<CloudMember> = emptyList()
    private var cachedCloudSavings: List<CloudSavingsTransaction> = emptyList()
    private var cachedCloudLoans: List<CloudLoan> = emptyList()
    private var cachedCloudInstallments: List<CloudInstallmentPayment> = emptyList()
    private val connectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _syncState.value = _syncState.value.copy(isNetworkOnline = true)
            if (activeUserId != null && _syncState.value.isAutoSyncEnabled) {
                triggerAutoSyncNow()
            }
        }

        override fun onLost(network: Network) {
            val stillOnline = isCurrentlyOnline()
            _syncState.value = _syncState.value.copy(
                isNetworkOnline = stillOnline,
                status = if (!stillOnline && activeUserId != null) {
                    CloudSyncStatus.OFFLINE_PENDING
                } else {
                    _syncState.value.status
                }
            )
        }
    }

    init {
        _syncState.value = _syncState.value.copy(isNetworkOnline = isCurrentlyOnline())
    }

    private fun currentFirebaseUid(): String? {
        return FirebaseSafeHelper.authOrNull(context)?.currentUser?.uid
    }

    private fun isCurrentlyOnline(): Boolean {
        return try {
            val cm = connectivityManager ?: return true
            val activeNetwork = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (t: Throwable) {
            Log.w(TAG, "Unable to inspect network state: ${t.message}")
            true
        }
    }

    fun startForUser(uid: String, email: String?) {
        if (activeUserId == uid && listenerJobs.isNotEmpty()) {
            return
        }
        stop()
        activeUserId = uid
        _syncState.value = _syncState.value.copy(
            status = CloudSyncStatus.SYNCING,
            accountEmail = email,
            errorMessage = null,
            isNetworkOnline = isCurrentlyOnline()
        )
        registerNetworkMonitor()
        startRealtimeListeners(uid)
        triggerAutoSyncNow()
    }

    fun stop() {
        listenerJobs.forEach { it.cancel() }
        listenerJobs = emptyList()
        activeUserId = null
        cachedCloudMembers = emptyList()
        cachedCloudSavings = emptyList()
        cachedCloudLoans = emptyList()
        cachedCloudInstallments = emptyList()
        unregisterNetworkMonitor()
        _syncState.value = CloudSyncState(
            status = CloudSyncStatus.NEEDS_GOOGLE_SIGN_IN,
            accountEmail = null,
            isNetworkOnline = isCurrentlyOnline()
        )
    }

    private fun registerNetworkMonitor() {
        if (networkCallbackRegistered || connectivityManager == null) return
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager.registerNetworkCallback(request, networkCallback)
            networkCallbackRegistered = true
        } catch (e: Exception) {
            Log.w(TAG, "Unable to register NetworkCallback: ${e.message}")
        }
    }

    private fun unregisterNetworkMonitor() {
        if (!networkCallbackRegistered || connectivityManager == null) return
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            Log.w(TAG, "Unable to unregister NetworkCallback: ${e.message}")
        } finally {
            networkCallbackRegistered = false
        }
    }

    fun triggerAutoSyncNow() {
        val uid = activeUserId ?: currentFirebaseUid() ?: return
        if (currentFirebaseUid() != uid) return

        scope.launch {
            syncMutex.withLock {
                if (currentFirebaseUid() != uid) return@withLock
                val online = isCurrentlyOnline()
                _syncState.value = _syncState.value.copy(
                    status = if (online) CloudSyncStatus.SYNCING else CloudSyncStatus.OFFLINE_PENDING,
                    isNetworkOnline = online,
                    errorMessage = null
                )
                try {
                    pushAllLocalDataToCloud()
                    val count = countLocalRecords()
                    _syncState.value = _syncState.value.copy(
                        status = if (isCurrentlyOnline()) CloudSyncStatus.SYNCED else CloudSyncStatus.OFFLINE_PENDING,
                        lastSyncedAtMillis = System.currentTimeMillis(),
                        syncedRecordCount = count,
                        errorMessage = null
                    )
                } catch (e: Exception) {
                    val fsError = unwrapFirestoreException(e)
                    val isOffline = !isCurrentlyOnline()
                    _syncState.value = _syncState.value.copy(
                        status = if (isOffline) CloudSyncStatus.OFFLINE_PENDING else CloudSyncStatus.ERROR,
                        errorMessage = fsError?.localizedMessage ?: e.localizedMessage ?: "সিঙ্ক করা যায়নি"
                    )
                }
            }
        }
    }

    fun pushMemberAsync(memberCode: String) {
        if (activeUserId == null && currentFirebaseUid() == null) return
        scope.launch {
            syncMutex.withLock {
                try {
                    val member = database.memberDao().getMemberByCode(memberCode) ?: return@withLock
                    syncSource.upsertMember(member)
                    markSyncedNow()
                } catch (e: Exception) {
                    Log.w(TAG, "Background member sync deferred: ${e.message}")
                }
            }
        }
    }

    fun pushSavingsByReceiptAsync(receiptNumber: String, memberCode: String) {
        if (activeUserId == null && currentFirebaseUid() == null) return
        scope.launch {
            syncMutex.withLock {
                try {
                    database.memberDao().getMemberByCode(memberCode)?.let { syncSource.upsertMember(it) }
                    database.savingsDao().getTransactionByReceipt(receiptNumber)?.let {
                        syncSource.createSavingsTransactionIfMissing(it)
                    }
                    markSyncedNow()
                } catch (e: Exception) {
                    Log.w(TAG, "Background savings sync deferred: ${e.message}")
                }
            }
        }
    }

    fun pushLoanAndInstallmentAsync(
        memberCode: String,
        disbursedDate: Long,
        receiptNumber: String? = null
    ) {
        if (activeUserId == null && currentFirebaseUid() == null) return
        scope.launch {
            syncMutex.withLock {
                try {
                    database.memberDao().getMemberByCode(memberCode)?.let { syncSource.upsertMember(it) }
                    val loan = database.loanDao().getLoanByMemberCodeAndDate(memberCode, disbursedDate)
                    if (loan != null) {
                        syncSource.upsertLoan(loan)
                        if (!receiptNumber.isNullOrBlank()) {
                            database.installmentDao().getPaymentByReceipt(receiptNumber)?.let { payment ->
                                val loanKey = FirestoreDocIds.loanKey(loan.memberCode, loan.disbursedDate)
                                syncSource.createInstallmentPaymentIfMissing(payment, loanKey)
                            }
                        }
                    }
                    markSyncedNow()
                } catch (e: Exception) {
                    Log.w(TAG, "Background loan/installment sync deferred: ${e.message}")
                }
            }
        }
    }

    fun deleteMemberFromCloudAsync(memberCode: String) {
        if (activeUserId == null && currentFirebaseUid() == null) return
        scope.launch {
            try {
                syncSource.deleteMember(memberCode)
                markSyncedNow()
            } catch (e: Exception) {
                Log.w(TAG, "Cloud delete member failed: ${e.message}")
            }
        }
    }

    fun deleteSavingsFromCloudAsync(receiptNumber: String) {
        if (activeUserId == null && currentFirebaseUid() == null) return
        scope.launch {
            try {
                syncSource.deleteSavingsTransaction(receiptNumber)
                markSyncedNow()
            } catch (e: Exception) {
                Log.w(TAG, "Cloud delete savings failed: ${e.message}")
            }
        }
    }

    fun deleteLoanFromCloudAsync(memberCode: String, disbursedDate: Long) {
        if (activeUserId == null && currentFirebaseUid() == null) return
        scope.launch {
            try {
                syncSource.deleteLoan(memberCode, disbursedDate)
                markSyncedNow()
            } catch (e: Exception) {
                Log.w(TAG, "Cloud delete loan failed: ${e.message}")
            }
        }
    }

    fun deleteInstallmentFromCloudAsync(receiptNumber: String) {
        if (receiptNumber.isBlank()) return
        if (activeUserId == null && currentFirebaseUid() == null) return
        scope.launch {
            try {
                syncSource.deleteInstallmentPayment(receiptNumber)
                markSyncedNow()
            } catch (e: Exception) {
                Log.w(TAG, "Cloud delete installment failed: ${e.message}")
            }
        }
    }

    fun resyncMemberRegistrationDateAsync(memberCode: String) {
        if (activeUserId == null && currentFirebaseUid() == null) return
        scope.launch {
            try {
                val member = database.memberDao().getMemberByCode(memberCode) ?: return@launch
                syncSource.deleteMember(memberCode)
                syncSource.upsertMember(member)
                markSyncedNow()
            } catch (e: Exception) {
                Log.w(TAG, "Cloud member date resync failed: ${e.message}")
            }
        }
    }

    fun resyncSavingsTransactionDateAsync(receiptNumber: String) {
        if (activeUserId == null && currentFirebaseUid() == null) return
        scope.launch {
            try {
                val tx = database.savingsDao().getTransactionByReceipt(receiptNumber) ?: return@launch
                syncSource.deleteSavingsTransaction(receiptNumber)
                syncSource.createSavingsTransactionIfMissing(tx)
                markSyncedNow()
            } catch (e: Exception) {
                Log.w(TAG, "Cloud savings date resync failed: ${e.message}")
            }
        }
    }

    fun resyncLoanDateAsync(
        memberCode: String,
        oldDisbursedDate: Long,
        newDisbursedDate: Long,
        loanId: Long
    ) {
        if (activeUserId == null && currentFirebaseUid() == null) return
        scope.launch {
            try {
                val loan = database.loanDao().getLoanByMemberCodeAndDate(memberCode, newDisbursedDate)
                    ?: return@launch
                val newLoanKey = FirestoreDocIds.loanKey(loan.memberCode, newDisbursedDate)
                val payments = database.installmentDao().getPaymentsForLoanList(loanId)
                payments.forEach { p ->
                    if (p.receiptNumber.isNotBlank()) {
                        syncSource.deleteInstallmentPayment(p.receiptNumber)
                    }
                }
                syncSource.deleteLoan(memberCode, oldDisbursedDate)
                syncSource.upsertLoan(loan)
                payments.forEach { p ->
                    syncSource.createInstallmentPaymentIfMissing(p, newLoanKey)
                }
                markSyncedNow()
            } catch (e: Exception) {
                Log.w(TAG, "Cloud loan date resync failed: ${e.message}")
            }
        }
    }

    fun resyncInstallmentDateAsync(receiptNumber: String, loanId: Long) {
        if (activeUserId == null && currentFirebaseUid() == null) return
        scope.launch {
            try {
                val payment = database.installmentDao().getPaymentByReceipt(receiptNumber) ?: return@launch
                val loan = database.loanDao().getLoanById(loanId) ?: return@launch
                val loanKey = FirestoreDocIds.loanKey(loan.memberCode, loan.disbursedDate)
                syncSource.deleteInstallmentPayment(receiptNumber)
                syncSource.createInstallmentPaymentIfMissing(payment, loanKey)
                markSyncedNow()
            } catch (e: Exception) {
                Log.w(TAG, "Cloud installment date resync failed: ${e.message}")
            }
        }
    }

    private suspend fun markSyncedNow() {
        val count = countLocalRecords()
        _syncState.value = _syncState.value.copy(
            status = if (isCurrentlyOnline()) CloudSyncStatus.SYNCED else CloudSyncStatus.OFFLINE_PENDING,
            lastSyncedAtMillis = System.currentTimeMillis(),
            syncedRecordCount = count,
            errorMessage = null
        )
    }

    private suspend fun pushAllLocalDataToCloud() {
        val members = database.memberDao().getAllMembersList()
        members.forEach { member ->
            syncSource.upsertMember(member)
        }

        val savings = database.savingsDao().getAllTransactionsList()
        savings.forEach { tx ->
            syncSource.createSavingsTransactionIfMissing(tx)
        }

        val loans = database.loanDao().getAllLoansList()
        val loanIdToKey = mutableMapOf<Long, String>()
        loans.forEach { loan ->
            val key = FirestoreDocIds.loanKey(loan.memberCode, loan.disbursedDate)
            loanIdToKey[loan.id] = key
            syncSource.upsertLoan(loan)
        }

        val installments = database.installmentDao().getAllPaymentsList()
        installments.forEach { payment ->
            val loanKey = loanIdToKey[payment.loanId]
                ?: database.loanDao().getLoanById(payment.loanId)?.let {
                    FirestoreDocIds.loanKey(it.memberCode, it.disbursedDate)
                }
                ?: FirestoreDocIds.loanKey(payment.memberCode, payment.paymentDate)
            syncSource.createInstallmentPaymentIfMissing(payment, loanKey)
        }
    }

    private fun startRealtimeListeners(uid: String) {
        val membersJob = scope.launch {
            syncSource.observeMembers()
                .catch { error ->
                    val root = unwrapFirestoreException(error) ?: error
                    Log.w(TAG, "Realtime members listener error", root)
                    if (currentFirebaseUid() == uid) {
                        _syncState.value = _syncState.value.copy(
                            status = CloudSyncStatus.ERROR,
                            errorMessage = root.localizedMessage ?: "সদস্য সিঙ্ক ত্রুটি"
                        )
                    }
                }
                .collect { cloudMembers ->
                    reconcileMutex.withLock {
                        cachedCloudMembers = cloudMembers
                        reconcileAllFromCloudLocked()
                    }
                }
        }

        val savingsJob = scope.launch {
            syncSource.observeSavingsTransactions()
                .catch { error ->
                    val root = unwrapFirestoreException(error) ?: error
                    Log.w(TAG, "Realtime savings listener error", root)
                }
                .collect { cloudSavings ->
                    reconcileMutex.withLock {
                        cachedCloudSavings = cloudSavings
                        reconcileAllFromCloudLocked()
                    }
                }
        }

        val loansJob = scope.launch {
            syncSource.observeLoans()
                .catch { error ->
                    val root = unwrapFirestoreException(error) ?: error
                    Log.w(TAG, "Realtime loans listener error", root)
                }
                .collect { cloudLoans ->
                    reconcileMutex.withLock {
                        cachedCloudLoans = cloudLoans
                        reconcileAllFromCloudLocked()
                    }
                }
        }

        val installmentsJob = scope.launch {
            syncSource.observeInstallmentPayments()
                .catch { error ->
                    val root = unwrapFirestoreException(error) ?: error
                    Log.w(TAG, "Realtime installments listener error", root)
                }
                .collect { cloudInstallments ->
                    reconcileMutex.withLock {
                        cachedCloudInstallments = cloudInstallments
                        reconcileAllFromCloudLocked()
                    }
                }
        }

        listenerJobs = listOf(membersJob, savingsJob, loansJob, installmentsJob)
    }

    /**
     * Reconciles cloud collections in strict foreign-key dependency order:
     * Members -> Loans -> Savings -> Installments
     * so out-of-order Firestore snapshot arrivals never drop dependent records.
     */
    private suspend fun reconcileAllFromCloudLocked() {
        val memberDao = database.memberDao()
        val loanDao = database.loanDao()
        val savingsDao = database.savingsDao()
        val installmentDao = database.installmentDao()

        val cloudSavingsReceipts = cachedCloudSavings
            .map { it.receiptNumber.ifBlank { it.id } }
            .filter { it.isNotBlank() }
            .toSet()
        val cloudInstallmentReceipts = cachedCloudInstallments
            .map { it.receiptNumber.ifBlank { it.id } }
            .filter { it.isNotBlank() }
            .toSet()
        val localPendingMemberCodes = buildSet {
            savingsDao.getAllTransactionsList().forEach { tx ->
                if (tx.receiptNumber.isBlank() || tx.receiptNumber !in cloudSavingsReceipts) {
                    add(tx.memberCode)
                }
            }
            installmentDao.getAllPaymentsList().forEach { pay ->
                if (pay.receiptNumber.isBlank() || pay.receiptNumber !in cloudInstallmentReceipts) {
                    add(pay.memberCode)
                }
            }
        }

        for (remote in cachedCloudMembers) {
            if (remote.memberCode.isBlank()) continue
            val existing = memberDao.getMemberByCode(remote.memberCode)
            if (existing == null) {
                memberDao.insertMember(remote.toRoomMember())
            } else if (remote.memberCode !in localPendingMemberCodes) {
                val merged = remote.toRoomMember(existingLocalId = existing.id)
                if (merged != existing) {
                    memberDao.updateMember(merged)
                }
            }
        }

        for (remote in cachedCloudLoans) {
            if (remote.memberCode.isBlank() || remote.disbursedDate <= 0L) continue
            val localMember = memberDao.getMemberByCode(remote.memberCode)
            val resolvedMemberId = localMember?.id ?: 0L
            val existing = loanDao.getLoanByMemberCodeAndDate(
                remote.memberCode,
                remote.disbursedDate
            )
            if (existing == null) {
                loanDao.insertLoan(remote.toRoomLoan(resolvedMemberId = resolvedMemberId))
            } else if (remote.memberCode !in localPendingMemberCodes) {
                val merged = remote.toRoomLoan(
                    resolvedMemberId = resolvedMemberId,
                    existingLocalId = existing.id
                )
                if (merged != existing) {
                    loanDao.updateLoan(merged)
                }
            }
        }

        for (remote in cachedCloudSavings) {
            val receipt = remote.receiptNumber.ifBlank { remote.id }
            if (receipt.isBlank()) continue
            if (savingsDao.getTransactionByReceipt(receipt) == null) {
                val localMember = memberDao.getMemberByCode(remote.memberCode) ?: continue
                savingsDao.insertTransaction(
                    remote.toRoomTransaction(resolvedMemberId = localMember.id)
                )
            }
        }

        val allLoans = loanDao.getAllLoansList()
        for (remote in cachedCloudInstallments) {
            val receipt = remote.receiptNumber.ifBlank { remote.id }
            if (receipt.isBlank()) continue
            if (installmentDao.getPaymentByReceipt(receipt) == null) {
                val localMember = memberDao.getMemberByCode(remote.memberCode)
                val matchedLoan = allLoans.firstOrNull { loan ->
                    FirestoreDocIds.loanKey(loan.memberCode, loan.disbursedDate) == remote.loanKey
                } ?: allLoans.firstOrNull { it.memberCode == remote.memberCode } ?: continue
                val resolvedMemberId = localMember?.id ?: matchedLoan.memberId
                installmentDao.insertPayment(
                    remote.toRoomPayment(
                        resolvedLoanId = matchedLoan.id,
                        resolvedMemberId = resolvedMemberId
                    )
                )
            }
        }

        markSyncedNow()
    }

    private suspend fun countLocalRecords(): Int {
        val m = database.memberDao().getMemberCount()
        val s = database.savingsDao().getAllTransactionsList().size
        val l = database.loanDao().getAllLoansList().size
        val i = database.installmentDao().getAllPaymentsList().size
        return m + s + l + i
    }

    private companion object {
        const val TAG = "CloudAutoSyncManager"
    }
}
