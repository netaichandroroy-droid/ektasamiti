package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.InstallmentPayment
import com.example.data.model.Loan
import com.example.data.model.Member
import com.example.data.model.SavingsTransaction
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.data.repository.SamitiRepository
import com.example.ui.components.SamitiDateUtils
import com.example.ui.components.SuccessPopupData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class ReceiptData(
    val title: String,
    val receiptNumber: String,
    val memberCode: String,
    val memberName: String,
    val transactionType: String,
    val amount: Double,
    val balanceOrDue: Double,
    val collectedBy: String,
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = ""
)

class SamitiViewModel(private val repository: SamitiRepository) : ViewModel() {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedMemberTypeFilter = MutableStateFlow("ALL") // "ALL", "DAILY", "MONTHLY"
    val selectedMemberTypeFilter: StateFlow<String> = _selectedMemberTypeFilter.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val members: StateFlow<List<Member>> = combine(
        _searchQuery.flatMapLatest { query -> repository.searchMembers(query) },
        _selectedMemberTypeFilter
    ) { memberList, filter ->
        if (filter == "ALL") memberList
        else memberList.filter { it.memberType == filter }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savingsList: StateFlow<List<SavingsTransaction>> = repository.getAllSavingsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val loansList: StateFlow<List<Loan>> = repository.getAllLoansFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeLoans: StateFlow<List<Loan>> = repository.getActiveLoansFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val installmentsList: StateFlow<List<InstallmentPayment>> = repository.getAllInstallmentsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val collectorsList: StateFlow<List<User>> = repository.getAllCollectorsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Receipt display
    private val _activeReceipt = MutableStateFlow<ReceiptData?>(null)
    val activeReceipt: StateFlow<ReceiptData?> = _activeReceipt.asStateFlow()

    // Passbook Member
    private val _passbookMember = MutableStateFlow<Member?>(null)
    val passbookMember: StateFlow<Member?> = _passbookMember.asStateFlow()

    // Backup text
    private val _backupData = MutableStateFlow<String?>(null)
    val backupData: StateFlow<String?> = _backupData.asStateFlow()

    // Status Message / SnackBar
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    // Prominent Success Pop-Up Modal on any data submission
    private val _activeSuccessPopup = MutableStateFlow<SuccessPopupData?>(null)
    val activeSuccessPopup: StateFlow<SuccessPopupData?> = _activeSuccessPopup.asStateFlow()

    fun showSuccessPopup(data: SuccessPopupData) {
        _activeSuccessPopup.value = data
    }

    fun dismissSuccessPopup() {
        _activeSuccessPopup.value = null
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setMemberFilter(filter: String) {
        _selectedMemberTypeFilter.value = filter
    }

    fun showReceipt(receipt: ReceiptData) {
        _activeReceipt.value = receipt
    }

    fun closeReceipt() {
        _activeReceipt.value = null
    }

    fun openPassbook(member: Member) {
        _passbookMember.value = member
    }

    fun closePassbook() {
        _passbookMember.value = null
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    // Member Registration
    fun registerNewMember(
        name: String,
        mobile: String,
        fatherOrHusbandName: String,
        nid: String,
        address: String,
        memberType: String,
        nomineeName: String,
        nomineeRelation: String,
        photoUri: String?,
        admissionFee: Double,
        initialSavings: Double,
        registeredBy: String,
        role: UserRole,
        registrationDate: Long = System.currentTimeMillis(),
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val codeNumber = (1001 + (members.value.maxOfOrNull { it.id } ?: 0L)).toInt()
            val memberCode = "ES-$codeNumber"
            val newMember = Member(
                memberCode = memberCode,
                name = name.trim(),
                mobile = mobile.trim(),
                fatherOrHusbandName = fatherOrHusbandName.trim(),
                nid = nid.trim(),
                address = address.trim(),
                memberType = memberType,
                nomineeName = nomineeName.trim(),
                nomineeRelation = nomineeRelation.trim(),
                photoUri = photoUri,
                admissionFee = admissionFee,
                savingsBalance = initialSavings,
                registrationDate = registrationDate,
                registeredBy = registeredBy
            )
            val memberId = repository.addMember(newMember)

            if (initialSavings > 0.0) {
                repository.recordSavingsDeposit(
                    member = newMember.copy(id = memberId),
                    amount = initialSavings,
                    savingsPlan = memberType,
                    note = "প্রারম্ভিক সঞ্চয় জমা",
                    collectedBy = registeredBy,
                    role = role,
                    customDate = registrationDate
                )
            }

            _snackbarMessage.value = "নতুন সদস্য সফলভাবে নিবন্ধিত হয়েছে (কোড: $memberCode)"
            _activeSuccessPopup.value = SuccessPopupData(
                title = "সদস্য নিবন্ধন সফল!",
                message = "${name.trim()} সফলভাবে সমিতির সদস্য তালিকায় যুক্ত হয়েছেন।",
                details = buildList {
                    add("সদস্যের নাম" to name.trim())
                    add("সদস্য কোড" to memberCode)
                    add("নিবন্ধনের তারিখ" to SamitiDateUtils.formatDateDisplay(registrationDate))
                    if (mobile.isNotBlank()) add("মোবাইল" to mobile.trim())
                    add("ভর্তি ফি" to "৳${admissionFee.toInt()}")
                    if (initialSavings > 0.0) {
                        add("প্রারম্ভিক সঞ্চয়" to "৳${initialSavings.toInt()}")
                    }
                }
            )
            onSuccess()
        }
    }

    // Savings Deposit
    fun depositSavings(
        member: Member,
        amount: Double,
        savingsPlan: String,
        note: String,
        collectedBy: String,
        role: UserRole,
        customDate: Long = System.currentTimeMillis(),
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            val transaction = repository.recordSavingsDeposit(
                member = member,
                amount = amount,
                savingsPlan = savingsPlan,
                note = note,
                collectedBy = collectedBy,
                role = role,
                customDate = customDate
            )
            _activeReceipt.value = ReceiptData(
                title = "সঞ্চয় জমা মানি রসিদ",
                receiptNumber = transaction.receiptNumber,
                memberCode = member.memberCode,
                memberName = member.name,
                transactionType = "জমা ($savingsPlan)",
                amount = amount,
                balanceOrDue = transaction.balanceAfter,
                collectedBy = collectedBy,
                timestamp = transaction.date,
                notes = note
            )
            _snackbarMessage.value = "৳${amount.toInt()} টাকা সঞ্চয় জমা সম্পন্ন হয়েছে"
            onComplete()
        }
    }

    // Batch Weekly Collection Recording (তালিকা আকারে সদস্যদের কালেকশন একযোগে এন্ট্রি)
    fun batchRecordWeeklyCollections(
        collections: List<Pair<Member, Double>>,
        fridayDateMillis: Long,
        fridayLabel: String,
        collectedBy: String,
        role: UserRole,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            var count = 0
            var totalAmount = 0.0
            for ((member, amount) in collections) {
                if (amount > 0.0) {
                    repository.recordSavingsDeposit(
                        member = member,
                        amount = amount,
                        savingsPlan = "WEEKLY",
                        note = "$fridayLabel এর সাপ্তাহিক কালেকশন জমা",
                        collectedBy = collectedBy,
                        role = role,
                        customDate = fridayDateMillis
                    )
                    count++
                    totalAmount += amount
                }
            }
            if (count > 0) {
                _snackbarMessage.value = "$count জন সদস্যের মোট ৳${totalAmount.toInt()} সাপ্তাহিক কালেকশন সফলভাবে জমা হয়েছে"
                _activeSuccessPopup.value = SuccessPopupData(
                    title = "সাপ্তাহিক কালেকশন সফল!",
                    message = "$fridayLabel এর সাপ্তাহিক কালেকশন সফলভাবে সংরক্ষিত হয়েছে।",
                    details = listOf(
                        "তারিখ / শুক্রবার" to fridayLabel,
                        "মোট সদস্য" to "$count জন",
                        "সর্বমোট আদায়" to "৳${totalAmount.toInt()}",
                        "সংগ্রাহক" to collectedBy
                    )
                )
            } else {
                _snackbarMessage.value = "কোন কালেকশন এন্ট্রি করা হয়নি"
                _activeSuccessPopup.value = SuccessPopupData(
                    title = "কোনো টাকা এন্ট্রি করা হয়নি",
                    message = "অনুগ্রহ করে অন্তত একজন সদস্যের জন্য ০ টাকার বেশি পরিমাণ লিখুন।",
                    isWarning = true
                )
            }
            onComplete()
        }
    }

    fun recordFridayCellCollection(
        member: Member,
        savingsAmount: Double,
        loanAmount: Double,
        activeLoan: Loan?,
        penalty: Double,
        fridayDateMillis: Long,
        fridayLabel: String,
        collectedBy: String,
        role: UserRole,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            val didSavings = savingsAmount > 0.0
            val didLoan = loanAmount > 0.0 && activeLoan != null

            if (didSavings && !didLoan) {
                val tx = repository.recordSavingsDeposit(
                    member = member,
                    amount = savingsAmount,
                    savingsPlan = "WEEKLY",
                    note = "$fridayLabel এর সাপ্তাহিক সঞ্চয় জমা",
                    collectedBy = collectedBy,
                    role = role,
                    customDate = fridayDateMillis
                )
                _activeReceipt.value = ReceiptData(
                    title = "সঞ্চয় জমা মানি রসিদ",
                    receiptNumber = tx.receiptNumber,
                    memberCode = member.memberCode,
                    memberName = member.name,
                    transactionType = "সাপ্তাহিক সঞ্চয় জমা",
                    amount = savingsAmount,
                    balanceOrDue = tx.balanceAfter,
                    collectedBy = collectedBy,
                    timestamp = tx.date,
                    notes = "$fridayLabel এর সাপ্তাহিক সঞ্চয়"
                )
            } else if (!didSavings && didLoan && activeLoan != null) {
                val payment = repository.collectInstallment(
                    loan = activeLoan,
                    amount = loanAmount,
                    penalty = penalty,
                    collectedBy = collectedBy,
                    notes = "$fridayLabel এর সাপ্তাহিক কিস্তি আদায়",
                    paymentDate = fridayDateMillis
                )
                _activeReceipt.value = ReceiptData(
                    title = "ঋণের কিস্তি আদায় রসিদ",
                    receiptNumber = payment.receiptNumber,
                    memberCode = activeLoan.memberCode,
                    memberName = activeLoan.memberName,
                    transactionType = "কিস্তি নং ${payment.installmentNumber}/${activeLoan.totalInstallments}",
                    amount = loanAmount,
                    balanceOrDue = payment.remainingAfterPayment,
                    collectedBy = collectedBy,
                    timestamp = payment.paymentDate,
                    notes = "$fridayLabel এর কিস্তি আদায়"
                )
            } else if (didSavings && didLoan && activeLoan != null) {
                val tx = repository.recordSavingsDeposit(
                    member = member,
                    amount = savingsAmount,
                    savingsPlan = "WEEKLY",
                    note = "$fridayLabel এর সাপ্তাহিক সঞ্চয় জমা",
                    collectedBy = collectedBy,
                    role = role,
                    customDate = fridayDateMillis
                )
                val payment = repository.collectInstallment(
                    loan = activeLoan,
                    amount = loanAmount,
                    penalty = penalty,
                    collectedBy = collectedBy,
                    notes = "$fridayLabel এর সাপ্তাহিক কিস্তি আদায়",
                    paymentDate = fridayDateMillis
                )
                _activeSuccessPopup.value = SuccessPopupData(
                    title = "সাপ্তাহিক কালেকশন সফল!",
                    message = "${member.name} (${member.memberCode}) এর সঞ্চয় ও কিস্তি আদায় সংরক্ষিত হয়েছে।",
                    details = listOf(
                        "তারিখ" to fridayLabel,
                        "সঞ্চয় জমা" to "৳${savingsAmount.toInt()} (স্থিতি: ৳${tx.balanceAfter.toInt()})",
                        "কিস্তি আদায় (#${payment.installmentNumber})" to "৳${loanAmount.toInt()} (বকেয়া: ৳${payment.remainingAfterPayment.toInt()})",
                        "সর্বমোট জমা" to "৳${(savingsAmount + loanAmount).toInt()}"
                    )
                )
            } else {
                _activeSuccessPopup.value = SuccessPopupData(
                    title = "কোনো পরিমাণ দেওয়া হয়নি",
                    message = "অনুগ্রহ করে সঞ্চয় বা কিস্তির পরিমাণ লিখুন।",
                    isWarning = true
                )
            }
            onComplete()
        }
    }

    // Savings Withdrawal
    fun withdrawSavings(
        member: Member,
        amount: Double,
        note: String,
        collectedBy: String,
        role: UserRole,
        customDate: Long = System.currentTimeMillis(),
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            val transaction = repository.recordSavingsWithdrawal(
                member = member,
                amount = amount,
                note = note,
                collectedBy = collectedBy,
                role = role,
                customDate = customDate
            )
            if (transaction != null) {
                _activeReceipt.value = ReceiptData(
                    title = "সঞ্চয় উত্তোলন রসিদ",
                    receiptNumber = transaction.receiptNumber,
                    memberCode = member.memberCode,
                    memberName = member.name,
                    transactionType = "উত্তোলন",
                    amount = amount,
                    balanceOrDue = transaction.balanceAfter,
                    collectedBy = collectedBy,
                    timestamp = transaction.date,
                    notes = note
                )
                _snackbarMessage.value = "৳${amount.toInt()} টাকা উত্তোলন সম্পন্ন হয়েছে"
                onComplete()
            } else {
                _snackbarMessage.value = "অপর্যাপ্ত সঞ্চয় স্থিতি! বর্তমান ব্যালেন্স: ৳${member.savingsBalance}"
                _activeSuccessPopup.value = SuccessPopupData(
                    title = "উত্তোলন সম্ভব নয়",
                    message = "অপর্যাপ্ত সঞ্চয় স্থিতি! বর্তমান ব্যালেন্স: ৳${member.savingsBalance.toInt()}",
                    isWarning = true
                )
            }
        }
    }

    // Disburse Loan with interest rate & charge
    fun disburseLoan(
        member: Member,
        principalAmount: Double,
        chargeAmount: Double,
        totalInstallments: Int,
        frequency: String,
        purpose: String,
        disbursedBy: String,
        interestRate: Double = if (principalAmount > 0) kotlin.math.round((chargeAmount / principalAmount) * 10000.0) / 100.0 else 0.0,
        disbursedDate: Long = System.currentTimeMillis(),
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            val loan = repository.disburseLoan(
                member = member,
                principalAmount = principalAmount,
                chargeAmount = chargeAmount,
                totalInstallments = totalInstallments,
                frequency = frequency,
                purpose = purpose,
                disbursedBy = disbursedBy,
                interestRate = interestRate,
                disbursedDate = disbursedDate
            )
            val freqLabel = if (frequency == "MONTHLY") "মাসিক" else "সাপ্তাহিক"
            _activeReceipt.value = ReceiptData(
                title = "ঋণ বিতরণ অনুমোদন পত্র",
                receiptNumber = "LN-${loan.id}-${member.memberCode}",
                memberCode = member.memberCode,
                memberName = member.name,
                transactionType = "ঋণ প্রদান (সুদ ${com.example.data.model.LoanScheduleCalculator.formatInterestRate(loan.interestRate)}%)",
                amount = principalAmount,
                balanceOrDue = loan.totalPayable,
                collectedBy = disbursedBy,
                timestamp = loan.disbursedDate,
                notes = "সুদ/চার্জ: ৳${chargeAmount.toInt()} (${com.example.data.model.LoanScheduleCalculator.formatInterestRate(loan.interestRate)}%) | মোট কিস্তি: $totalInstallments টি ($freqLabel)"
            )
            _snackbarMessage.value = "৳${principalAmount.toInt()} টাকা ঋণ বিতরণ সম্পন্ন হয়েছে"
            onComplete()
        }
    }

    // Collect Installment
    fun collectInstallment(
        loan: Loan,
        amount: Double,
        penalty: Double,
        collectedBy: String,
        notes: String,
        paymentDate: Long = System.currentTimeMillis(),
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            val payment = repository.collectInstallment(
                loan = loan,
                amount = amount,
                penalty = penalty,
                collectedBy = collectedBy,
                notes = notes,
                paymentDate = paymentDate
            )
            _activeReceipt.value = ReceiptData(
                title = "ঋণের কিস্তি আদায় রসিদ",
                receiptNumber = payment.receiptNumber,
                memberCode = loan.memberCode,
                memberName = loan.memberName,
                transactionType = "কিস্তি নং ${payment.installmentNumber}/${loan.totalInstallments}",
                amount = amount,
                balanceOrDue = payment.remainingAfterPayment,
                collectedBy = collectedBy,
                timestamp = payment.paymentDate,
                notes = if (penalty > 0) "জরিমানা: ৳$penalty | ${notes}" else notes
            )
            _snackbarMessage.value = "৳${amount.toInt()} টাকা কিস্তি আদায় সম্পন্ন হয়েছে"
            onComplete()
        }
    }

    // Date Updates for Existing Records
    fun updateMemberRegistrationDate(member: Member, newDateMillis: Long) {
        viewModelScope.launch {
            repository.updateMemberRegistrationDate(member, newDateMillis)
            _snackbarMessage.value = "তারিখ সফলভাবে আপডেট করা হয়েছে"
        }
    }

    fun updateSavingsTransactionDate(transaction: SavingsTransaction, newDateMillis: Long) {
        viewModelScope.launch {
            repository.updateSavingsTransactionDate(transaction, newDateMillis)
            _snackbarMessage.value = "তারিখ সফলভাবে আপডেট করা হয়েছে"
        }
    }

    fun updateLoanDisbursedDate(loan: Loan, newDateMillis: Long) {
        viewModelScope.launch {
            repository.updateLoanDisbursedDate(loan, newDateMillis)
            _snackbarMessage.value = "তারিখ সফলভাবে আপডেট করা হয়েছে"
        }
    }

    fun updateInstallmentPaymentDate(payment: InstallmentPayment, newDateMillis: Long) {
        viewModelScope.launch {
            repository.updateInstallmentPaymentDate(payment, newDateMillis)
            _snackbarMessage.value = "তারিখ সফলভাবে আপডেট করা হয়েছে"
        }
    }

    // Add Collector (Admin only)
    fun addCollector(
        name: String,
        email: String,
        pass: String,
        mobile: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            repository.registerUser(
                User(
                    email = email.trim(),
                    passwordHash = pass.trim(),
                    fullName = name.trim(),
                    mobile = mobile.trim(),
                    role = UserRole.COLLECTOR
                )
            )
            _snackbarMessage.value = "নতুন মাঠকর্মী/কালেক্টর যোগ করা হয়েছে"
            _activeSuccessPopup.value = SuccessPopupData(
                title = "মাঠকর্মী নিয়োগ সফল!",
                message = "${name.trim()} সফলভাবে কালেক্টর হিসেবে যুক্ত হয়েছেন।",
                details = buildList {
                    add("নাম" to name.trim())
                    add("ইমেইল" to email.trim())
                    if (mobile.isNotBlank()) add("মোবাইল" to mobile.trim())
                }
            )
            onSuccess()
        }
    }

    // Secure Deletions (Admin only)
    fun deleteMember(member: Member, role: UserRole) {
        viewModelScope.launch {
            val success = repository.deleteMember(member, role)
            if (success) {
                _snackbarMessage.value = "${member.name} এর তথ্য মুছে ফেলা হয়েছে"
                _activeSuccessPopup.value = SuccessPopupData(
                    title = "সদস্য মুছে ফেলা সফল!",
                    message = "${member.name} (${member.memberCode}) এর সকল তথ্য ডাটাবেজ থেকে মুছে ফেলা হয়েছে।"
                )
            } else {
                _snackbarMessage.value = "নিরাপত্তা সতর্কতা: শুধুমাত্র অ্যাডমিন ডাটা মুছতে পারবেন!"
            }
        }
    }

    fun deleteLoan(loan: Loan, role: UserRole) {
        viewModelScope.launch {
            val success = repository.deleteLoan(loan, role)
            if (success) {
                _snackbarMessage.value = "ঋণ রেকর্ড মুছে ফেলা হয়েছে"
                _activeSuccessPopup.value = SuccessPopupData(
                    title = "ঋণ রেকর্ড মুছে ফেলা সফল!",
                    message = "${loan.memberName} এর ঋণ (#${loan.id}) সফলভাবে মুছে ফেলা হয়েছে।"
                )
            } else {
                _snackbarMessage.value = "নিরাপত্তা সতর্কতা: শুধুমাত্র অ্যাডমিন ডাটা মুছতে পারবেন!"
            }
        }
    }

    fun deleteSavingsTransaction(transaction: SavingsTransaction, role: UserRole) {
        viewModelScope.launch {
            val success = repository.deleteSavingsTransaction(transaction, role)
            if (success) {
                _snackbarMessage.value = "সঞ্চয় লেনদেন মুছে ফেলা হয়েছে"
                _activeSuccessPopup.value = SuccessPopupData(
                    title = "লেনদেন মুছে ফেলা সফল!",
                    message = "${transaction.memberName} এর ৳${transaction.amount.toInt()} লেনদেনটি মুছে ফেলা হয়েছে।"
                )
            } else {
                _snackbarMessage.value = "শুধুমাত্র অ্যাডমিন মুছতে পারবেন!"
            }
        }
    }

    // Backup Generation
    fun generateBackup() {
        viewModelScope.launch {
            val data = repository.generateBackupDataString()
            _backupData.value = data
        }
    }

    fun clearBackupData() {
        _backupData.value = null
    }

    // Calculations for today's summary
    fun getTodaySavingsTotal(): Double {
        val now = System.currentTimeMillis()
        return savingsList.value
            .filter { it.type == "DEPOSIT" && SamitiDateUtils.isSameLocalDay(it.date, now) }
            .sumOf { it.amount }
    }

    fun getTodayInstallmentTotal(): Double {
        val now = System.currentTimeMillis()
        return installmentsList.value
            .filter { SamitiDateUtils.isSameLocalDay(it.paymentDate, now) }
            .sumOf { it.paidAmount }
    }

    // Reset / Clear all samiti members & transactions (Admin only, requires Admin ID & password verification)
    fun clearAllSamitiDataWithAdminAuth(
        role: UserRole,
        adminEmail: String,
        adminPassword: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            if (role != UserRole.ADMIN) {
                onError("নিরাপত্তা সতর্কতা: শুধুমাত্র অ্যাডমিন ডাটা রিসেট করতে পারবেন।")
                return@launch
            }
            if (adminEmail.isBlank() || adminPassword.isBlank()) {
                onError("ডাটা রিসেট করতে এডমিনের আইডি (ইমেইল) ও পাসওয়ার্ড দিন।")
                return@launch
            }
            val verified = repository.verifyAdminCredentials(adminEmail, adminPassword)
            if (!verified) {
                onError("এডমিনের আইডি অথবা পাসওয়ার্ড সঠিক নয়!")
                return@launch
            }
            val success = repository.clearAllSamitiData(role)
            if (success) {
                _snackbarMessage.value = "সকল সদস্য ও লেনদেনের ডাটা সম্পূর্ণ মুছে ফেলা হয়েছে"
                _activeSuccessPopup.value = SuccessPopupData(
                    title = "সকল ডাটা রিসেট সফল!",
                    message = "এডমিন যাচাইকরণের পর সকল সদস্য, ঋণ, সঞ্চয় এবং কিস্তি আদায়ের তথ্য পরিষ্কার করা হয়েছে।"
                )
                onSuccess()
            } else {
                onError("ডাটা রিসেট করা যায়নি। আবার চেষ্টা করুন।")
            }
        }
    }

    // Legacy direct reset kept for internal/unit callers; UI uses clearAllSamitiDataWithAdminAuth
    fun clearAllSamitiData(role: UserRole, onComplete: () -> Unit) {
        viewModelScope.launch {
            val success = repository.clearAllSamitiData(role)
            if (success) {
                _snackbarMessage.value = "সকল সদস্য ও লেনদেনের ডাটা সম্পূর্ণ মুছে ফেলা হয়েছে"
                _activeSuccessPopup.value = SuccessPopupData(
                    title = "সকল ডাটা রিসেট সফল!",
                    message = "সকল সদস্য, ঋণ, সঞ্চয় এবং কিস্তি আদায়ের তথ্য পরিষ্কার করা হয়েছে।"
                )
            }
            onComplete()
        }
    }
}
