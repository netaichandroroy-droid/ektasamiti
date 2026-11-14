package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.InstallmentPayment
import com.example.data.model.Loan
import com.example.data.model.LoanScheduleCalculator
import com.example.data.model.Member
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.ui.components.LoanScheduleDialog
import com.example.ui.components.ReceiptDialog
import com.example.ui.viewmodel.ReceiptData
import com.example.ui.viewmodel.SamitiViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun LoansScreen(
    currentUser: User,
    viewModel: SamitiViewModel,
    preselectedMemberForLoan: Member? = null,
    showNewLoanInitially: Boolean = false,
    showCollectInstallmentInitially: Boolean = false
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: কিস্তি আদায়, 1: চলমান ঋণ, 2: আদায় বিবরণী
    val members by viewModel.members.collectAsState()
    val loansList by viewModel.loansList.collectAsState()
    val activeLoans by viewModel.activeLoans.collectAsState()
    val installmentsList by viewModel.installmentsList.collectAsState()

    var showNewLoanDialog by remember { mutableStateOf(showNewLoanInitially || preselectedMemberForLoan != null) }
    var showCollectDialog by remember { mutableStateOf(showCollectInstallmentInitially) }
    var selectedLoanForCollection by remember { mutableStateOf<Loan?>(null) }
    var selectedLoanForSchedule by remember { mutableStateOf<Loan?>(null) }
    var selectedLoanForDelete by remember { mutableStateOf<Loan?>(null) }
    var loanSearchQuery by remember { mutableStateOf("") }

    LaunchedEffect(showNewLoanInitially, preselectedMemberForLoan) {
        if (showNewLoanInitially || preselectedMemberForLoan != null) {
            showNewLoanDialog = true
        }
    }
    LaunchedEffect(showCollectInstallmentInitially) {
        if (showCollectInstallmentInitially) {
            showCollectDialog = true
        }
    }

    val filteredActiveLoans = remember(activeLoans, loanSearchQuery) {
        if (loanSearchQuery.isBlank()) activeLoans
        else activeLoans.filter {
            it.memberName.contains(loanSearchQuery, ignoreCase = true) ||
                it.memberCode.contains(loanSearchQuery, ignoreCase = true) ||
                it.memberMobile.contains(loanSearchQuery, ignoreCase = true)
        }
    }

    val filteredAllLoans = remember(loansList, loanSearchQuery) {
        if (loanSearchQuery.isBlank()) loansList
        else loansList.filter {
            it.memberName.contains(loanSearchQuery, ignoreCase = true) ||
                it.memberCode.contains(loanSearchQuery, ignoreCase = true) ||
                it.memberMobile.contains(loanSearchQuery, ignoreCase = true)
        }
    }

    val totalActiveLoanDue = remember(activeLoans) { activeLoans.sumOf { it.remainingDue } }
    val totalPaidSoFar = remember(loansList) { loansList.sumOf { it.totalPaid } }
    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("loans_screen")
    ) {
        // Top Header and Metrics
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(listOf(Color(0xFFEA356B), Color(0xFFF4608B)))
                                )
                                .padding(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Payments,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.22f),
                                modifier = Modifier.align(Alignment.BottomEnd).size(44.dp)
                            )
                            Column {
                                Text(
                                    text = "চলমান মোট বকেয়া ঋণ",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.94f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "৳ ${totalActiveLoanDue.toInt()}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    "${activeLoans.size} টি ঋণ চলমান",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.88f),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(listOf(Color(0xFFDF8200), Color(0xFFF6A10B)))
                                )
                                .padding(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Paid,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.22f),
                                modifier = Modifier.align(Alignment.BottomEnd).size(44.dp)
                            )
                            Column {
                                Text(
                                    text = "সর্বমোট কিস্তি আদায়",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.94f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "৳ ${totalPaidSoFar.toInt()}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    "${installmentsList.size} টি কিস্তি সংগৃহীত",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.88f),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            selectedLoanForCollection = activeLoans.firstOrNull()
                            showCollectDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB57300)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).testTag("btn_open_collect_installment")
                    ) {
                        Icon(Icons.Default.Paid, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("কিস্তি আদায় করুন", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { showNewLoanDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).testTag("btn_open_disburse_loan")
                    ) {
                        Icon(Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("নতুন ঋণ বিতরণ", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("চলমান ঋণ (${activeLoans.size})") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("কিস্তি আদায় বিবরণী (${installmentsList.size})") }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("সকল ঋণ (${loansList.size})") }
            )
        }

        // Tab Content
        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTab) {
                0 -> {
                    // Active Loans
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (activeLoans.isNotEmpty()) {
                            OutlinedTextField(
                                value = loanSearchQuery,
                                onValueChange = { loanSearchQuery = it },
                                label = { Text("সদস্যের নাম, কোড বা মোবাইল দিয়ে ঋণ খুঁজুন...") },
                                leadingIcon = {
                                    Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                                trailingIcon = {
                                    if (loanSearchQuery.isNotBlank()) {
                                        IconButton(onClick = { loanSearchQuery = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .testTag("search_loans_input")
                            )
                        }
                        if (filteredActiveLoans.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (loanSearchQuery.isNotBlank()) "কোনো ঋণ খুঁজে পাওয়া যায়নি" else "কোন চলমান ঋণ নেই",
                                    color = Color(0xFF4B5563),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(filteredActiveLoans, key = { it.id }) { loan ->
                                    LoanItemCard(
                                        loan = loan,
                                        currentUserRole = currentUser.role,
                                        onViewScheduleClick = {
                                            selectedLoanForSchedule = loan
                                        },
                                        onCollectClick = {
                                            selectedLoanForCollection = loan
                                            showCollectDialog = true
                                        },
                                        onDeleteClick = {
                                            selectedLoanForDelete = loan
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // Installment Payments History
                    if (installmentsList.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("এখনো কোন কিস্তি আদায়ের বিবরণ নেই", color = MaterialTheme.colorScheme.outline)
                        }
                    } else {
                        val todayInstallmentsTotal = remember(installmentsList) { viewModel.getTodayInstallmentTotal() }
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Summary Banner for Total Installment Collection
                            item {
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7E6)),
                                    border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFB57300).copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = "মোট কিস্তি আদায়ের বিবরণী",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color(0xFF8A5300)
                                                )
                                                Text(
                                                    text = "সমিতির সর্বমোট আদায়কৃত কিস্তির পরিমাণ",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.outline
                                                )
                                            }
                                            Text(
                                                text = "৳ ${totalPaidSoFar.toInt()}",
                                                style = MaterialTheme.typography.headlineSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFFB57300)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        HorizontalDivider(color = Color(0xFFB57300).copy(alpha = 0.2f))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "আজকের কিস্তি আদায়: ৳ ${todayInstallmentsTotal.toInt()}",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1B7A49)
                                            )
                                            Text(
                                                text = "মোট আদায়কৃত কিস্তি: ${installmentsList.size} টি",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }

                            items(installmentsList) { payment ->
                                InstallmentPaymentCard(
                                    payment = payment,
                                    currentUserRole = currentUser.role,
                                    onClick = {
                                        viewModel.showReceipt(
                                            ReceiptData(
                                                title = "ঋণের কিস্তি আদায় রসিদ",
                                                receiptNumber = payment.receiptNumber,
                                                memberCode = payment.memberCode,
                                                memberName = payment.memberName,
                                                transactionType = "কিস্তি নং ${payment.installmentNumber}",
                                                amount = payment.paidAmount,
                                                balanceOrDue = payment.remainingAfterPayment,
                                                collectedBy = payment.collectedBy,
                                                timestamp = payment.paymentDate,
                                                notes = payment.notes
                                            )
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
                2 -> {
                    // All Loans (Active & Closed)
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (loansList.isNotEmpty()) {
                            OutlinedTextField(
                                value = loanSearchQuery,
                                onValueChange = { loanSearchQuery = it },
                                label = { Text("সদস্যের নাম, কোড বা মোবাইল দিয়ে খুঁজুন...") },
                                leadingIcon = {
                                    Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                                trailingIcon = {
                                    if (loanSearchQuery.isNotBlank()) {
                                        IconButton(onClick = { loanSearchQuery = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        if (filteredAllLoans.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("কোনো ঋণ পাওয়া যায়নি", color = Color(0xFF4B5563), fontWeight = FontWeight.Bold)
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(filteredAllLoans, key = { it.id }) { loan ->
                                    LoanItemCard(
                                        loan = loan,
                                        currentUserRole = currentUser.role,
                                        onViewScheduleClick = {
                                            selectedLoanForSchedule = loan
                                        },
                                        onCollectClick = {
                                            if (loan.status == "ACTIVE") {
                                                selectedLoanForCollection = loan
                                                showCollectDialog = true
                                            }
                                        },
                                        onDeleteClick = {
                                            selectedLoanForDelete = loan
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Full Loan Repayment Schedule Dialog
    selectedLoanForSchedule?.let { loan ->
        val loanPayments = remember(installmentsList, loan.id) {
            installmentsList.filter { it.loanId == loan.id }
        }
        LoanScheduleDialog(
            loan = loan,
            payments = loanPayments,
            onDismiss = { selectedLoanForSchedule = null },
            onCollectNextClick = {
                selectedLoanForCollection = loan
                showCollectDialog = true
            }
        )
    }

    // New Loan Dialog
    if (showNewLoanDialog) {
        NewLoanDisbursalDialog(
            members = members,
            preselectedMember = preselectedMemberForLoan,
            currentUser = currentUser,
            onDismiss = { showNewLoanDialog = false },
            onSubmit = { member, principal, rate, charge, installments, freq, purpose ->
                viewModel.disburseLoan(
                    member = member,
                    principalAmount = principal,
                    chargeAmount = charge,
                    totalInstallments = installments,
                    frequency = freq,
                    purpose = purpose,
                    disbursedBy = currentUser.fullName,
                    interestRate = rate,
                    onComplete = { showNewLoanDialog = false }
                )
            }
        )
    }

    // Collect Installment Dialog
    if (showCollectDialog) {
        CollectInstallmentDialog(
            activeLoans = activeLoans,
            preselectedLoan = selectedLoanForCollection,
            currentUser = currentUser,
            onDismiss = { showCollectDialog = false },
            onSubmit = { loan, amount, penalty, notes ->
                viewModel.collectInstallment(
                    loan = loan,
                    amount = amount,
                    penalty = penalty,
                    collectedBy = currentUser.fullName,
                    notes = notes,
                    onComplete = { showCollectDialog = false }
                )
            }
        )
    }

    // Delete Loan confirmation (Admin only)
    selectedLoanForDelete?.let { loan ->
        AlertDialog(
            onDismissRequest = { selectedLoanForDelete = null },
            title = { Text("ঋণ রেকর্ড ডিলিট নিশ্চিতকরণ") },
            text = { Text("সদস্য '${loan.memberName}' এর ঋণ রেকর্ডটি (৳${loan.principalAmount.toInt()}) কি মুছে ফেলতে চান? শুধুমাত্র অ্যাডমিন এটি করতে পারেন।") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteLoan(loan, currentUser.role)
                        selectedLoanForDelete = null
                    }
                ) {
                    Text("হ্যাঁ, ডিলিট করুন", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedLoanForDelete = null }) {
                    Text("বাতিল")
                }
            }
        )
    }
}

@Composable
fun LoanItemCard(
    loan: Loan,
    currentUserRole: UserRole,
    onViewScheduleClick: () -> Unit,
    onCollectClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val progress = if (loan.totalPayable > 0) (loan.totalPaid / loan.totalPayable).toFloat() else 0f
    val isClosed = loan.status == "CLOSED"
    var showInstallmentSchedule by remember { mutableStateOf(false) }

    val effectiveRate = remember(loan) { LoanScheduleCalculator.effectiveInterestRate(loan) }
    val rateText = remember(effectiveRate) { LoanScheduleCalculator.formatInterestRate(effectiveRate) }
    val freqText = if (loan.frequency == "MONTHLY") "মাসিক" else "সাপ্তাহিক"
    val dateSdf = remember { SimpleDateFormat("dd/MM/yy", Locale.getDefault()) }
    val nextDueInstallment = (loan.paidInstallments + 1).coerceAtMost(loan.totalInstallments.coerceAtLeast(1))
    val nextDueDateMillis = remember(loan) {
        LoanScheduleCalculator.computeDueDateMillis(loan.disbursedDate, nextDueInstallment, loan.frequency)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE6E9EE)),
        border = BorderStroke(1.2.dp, if (isClosed) Color(0xFF90CAF9) else Color(0xFFFCA5A5)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.5.dp),
        modifier = Modifier.fillMaxWidth().testTag("loan_card_${loan.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = loan.memberName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF111827)
                    )
                    Text(
                        text = "হিসাব: ${loan.memberCode} | মোবা: ${loan.memberMobile}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF374151)
                    )
                    Text(
                        text = "বিতরণ: ${dateSdf.format(Date(loan.disbursedDate))} • $freqText • সুদ: $rateText%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E40AF)
                    )
                }

                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isClosed) Color(0xFFE8F1FC) else Color(0xFFFDE8E8),
                        border = BorderStroke(1.2.dp, if (isClosed) Color(0xFF1976D2) else Color(0xFFD32F2F))
                    ) {
                        Text(
                            text = if (isClosed) "পরিশোধিত (ব্লু)" else "বাকি রয়েছে (লাল)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isClosed) Color(0xFF1976D2) else Color(0xFFD32F2F),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFFDF2F8),
                        border = BorderStroke(1.dp, Color(0xFFF472B6))
                    ) {
                        Text(
                            text = "সুদের হার: $rateText%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFBE185D),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Loan breakdown stats (মূল ঋণ, সুদ %, প্রদেয়, মোট কিস্তি, বকেয়া)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, Color(0xFF94A3B8))
                ) {
                    Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("মূল ঋণ", style = MaterialTheme.typography.labelSmall, color = Color(0xFF334155), fontWeight = FontWeight.Bold, maxLines = 1)
                        Text("৳${loan.principalAmount.toInt()}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
                    }
                }
                Surface(
                    modifier = Modifier.weight(0.95f),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFDF2F8),
                    border = BorderStroke(1.dp, Color(0xFFF472B6))
                ) {
                    Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("সুদ ($rateText%)", style = MaterialTheme.typography.labelSmall, color = Color(0xFF831843), fontWeight = FontWeight.Bold, maxLines = 1)
                        Text("+৳${loan.chargeAmount.toInt()}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.ExtraBold, color = Color(0xFFBE185D))
                    }
                }
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFEFF6FF),
                    border = BorderStroke(1.dp, Color(0xFF60A5FA))
                ) {
                    Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("প্রদেয়", style = MaterialTheme.typography.labelSmall, color = Color(0xFF1E40AF), fontWeight = FontWeight.Bold, maxLines = 1)
                        Text("৳${loan.totalPayable.toInt()}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1D4ED8))
                    }
                }
                Surface(
                    modifier = Modifier.weight(1.1f),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFEF3C7),
                    border = BorderStroke(1.2.dp, Color(0xFFF59E0B))
                ) {
                    Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("আদায়", style = MaterialTheme.typography.labelSmall, color = Color(0xFF92400E), fontWeight = FontWeight.ExtraBold, maxLines = 1)
                        Text("৳${loan.totalPaid.toInt()}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.ExtraBold, color = Color(0xFFB45309))
                    }
                }
                Surface(
                    modifier = Modifier.weight(1.05f),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFEE2E2),
                    border = BorderStroke(1.2.dp, Color(0xFFEF4444))
                ) {
                    Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("বকেয়া", style = MaterialTheme.typography.labelSmall, color = Color(0xFF991B1B), fontWeight = FontWeight.ExtraBold, maxLines = 1)
                        Text("৳${loan.remainingDue.toInt()}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.ExtraBold, color = Color(0xFFDC2626))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Next expected installment banner for active loans
            if (!isClosed) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFFFBEB),
                    border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "পরবর্তী কিস্তি (${nextDueInstallment}নং): ${dateSdf.format(Date(nextDueDateMillis))}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF92400E)
                            )
                        }
                        Text(
                            text = "৳${loan.perInstallmentAmount.toInt()}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFB45309)
                        )
                    }
                }
            }

            // Progress Bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = if (isClosed) Color(0xFF1976D2) else Color(0xFFD32F2F),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "কিস্তি: ${loan.paidInstallments} / ${loan.totalInstallments} টি সম্পন্ন (প্রতি কিস্তি: ৳${loan.perInstallmentAmount.toInt()})",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF374151)
                )
                Text(
                    text = "${(progress * 100).toInt()}% আদায়",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isClosed) Color(0xFF1976D2) else Color(0xFFD32F2F)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Date-wise Installment Schedule toggle & Full Schedule button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onViewScheduleClick,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                    modifier = Modifier.testTag("btn_open_schedule_${loan.id}")
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color(0xFF1565C0), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "সম্পূর্ণ সময়সূচী (Schedule)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1565C0)
                    )
                }

                TextButton(
                    onClick = { showInstallmentSchedule = !showInstallmentSchedule },
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = if (showInstallmentSchedule) "লুকান ▲" else "দ্রুত বাটন দেখুন ▼",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1976D2)
                    )
                }
            }

            if (showInstallmentSchedule) {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (k in 1..loan.totalInstallments) {
                            val instDate = LoanScheduleCalculator.computeDueDateMillis(loan.disbursedDate, k, loan.frequency)
                            val isPaid = k <= loan.paidInstallments || isClosed
                            val btnColor = if (isPaid) Color(0xFF1976D2) else Color(0xFFD32F2F)

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isPaid) Color(0xFFE8F1FC) else Color(0xFFFDE8E8),
                                border = BorderStroke(1.2.dp, btnColor),
                                modifier = Modifier
                                    .width(118.dp)
                                    .clickable { if (!isPaid) onCollectClick() }
                            ) {
                                Column(
                                    modifier = Modifier.padding(6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "${k}নং (${dateSdf.format(Date(instDate))})",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = btnColor
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = btnColor,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = if (isPaid) "ব্লু: পরিশোধিত" else "লাল: কিস্তি দিন",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            fontWeight = FontWeight.ExtraBold,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            // Bottom Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "উদ্দেশ্য: ${loan.purpose}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF4B5563),
                    modifier = Modifier.weight(1f)
                )

                if (!isClosed) {
                    Button(
                        onClick = onCollectClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Paid, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("কিস্তি দিন (লাল)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1976D2)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("পরিশোধিত (ব্লু)", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (currentUserRole == UserRole.ADMIN) {
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InstallmentPaymentCard(
    payment: InstallmentPayment,
    currentUserRole: UserRole,
    onClick: () -> Unit
) {
    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFD97706)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE6E9EE)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFFB45309),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = payment.memberName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "${payment.memberCode} | কিস্তি নং ${payment.installmentNumber} | রসিদ: ${payment.receiptNumber}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = "${sdf.format(Date(payment.paymentDate))} | সংগ্রাহক: ${payment.collectedBy}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF334155)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "৳ ${payment.paidAmount.toInt()}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF92400E)
                )
                Text(
                    text = "অবশিষ্ট: ৳${payment.remainingAfterPayment.toInt()}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF78350F)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewLoanDisbursalDialog(
    members: List<Member>,
    preselectedMember: Member?,
    currentUser: User,
    onDismiss: () -> Unit,
    onSubmit: (Member, Double, Double, Double, Int, String, String) -> Unit
) {
    var selectedMember by remember { mutableStateOf(preselectedMember ?: members.firstOrNull()) }
    var expanded by remember { mutableStateOf(false) }

    var principalText by remember { mutableStateOf("20000") }
    var interestRateText by remember { mutableStateOf("10") }
    var chargeText by remember { mutableStateOf("2000") }
    var installmentsText by remember { mutableStateOf("20") }
    var frequency by remember { mutableStateOf("WEEKLY") } // "WEEKLY" or "MONTHLY"
    var purpose by remember { mutableStateOf("ব্যবসা সম্প্রসারণ") }
    var error by remember { mutableStateOf<String?>(null) }

    val principal = principalText.toDoubleOrNull() ?: 0.0
    val interestRate = interestRateText.toDoubleOrNull() ?: 0.0
    val charge = chargeText.toDoubleOrNull() ?: 0.0
    val totalPayable = principal + charge
    val totalInstallments = installmentsText.toIntOrNull() ?: 1
    val perInstallment = if (totalInstallments > 0) ((totalPayable / totalInstallments) * 100.0).roundToInt() / 100.0 else 0.0
    val nowMillis = remember { System.currentTimeMillis() }
    val dateSdf = remember { SimpleDateFormat("dd MMM yy", Locale.getDefault()) }
    val firstDueDate = remember(nowMillis, frequency) {
        LoanScheduleCalculator.computeDueDateMillis(nowMillis, 1, frequency)
    }
    val finalDueDate = remember(nowMillis, frequency, totalInstallments) {
        LoanScheduleCalculator.computeDueDateMillis(nowMillis, totalInstallments.coerceAtLeast(1), frequency)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Text(
                    text = "নতুন ঋণ বিতরণ ও সময়সূচী",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "মূল ঋণ, সুদের হার (%) ও কিস্তির সময়সূচী নির্ধারণ করুন",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF4B5563)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Member selection
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedMember?.let { "${it.name} (${it.memberCode})" } ?: "সদস্য নির্বাচন করুন",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("ঋণগ্রহীতা সদস্য") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        members.forEach { m ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("${m.name} (${m.memberCode})", fontWeight = FontWeight.SemiBold)
                                        Text("বর্তমান সঞ্চয়: ৳${m.savingsBalance.toInt()} | বকেয়া: ৳${m.totalLoanDue.toInt()}", style = MaterialTheme.typography.labelSmall)
                                    }
                                },
                                onClick = {
                                    selectedMember = m
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Principal Amount
                OutlinedTextField(
                    value = principalText,
                    onValueChange = {
                        principalText = it
                        val p = it.toDoubleOrNull() ?: 0.0
                        val r = interestRateText.toDoubleOrNull() ?: 0.0
                        chargeText = ((p * r) / 100.0).roundToInt().toString()
                        error = null
                    },
                    label = { Text("মূল ঋণের পরিমাণ (টাকা) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_principal_amount")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Two-way Interest Rate (%) and Charge Amount (৳)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = interestRateText,
                        onValueChange = {
                            interestRateText = it
                            val r = it.toDoubleOrNull() ?: 0.0
                            chargeText = ((principal * r) / 100.0).roundToInt().toString()
                            error = null
                        },
                        label = { Text("সুদের হার (%) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(0.9f).testTag("input_interest_rate")
                    )

                    OutlinedTextField(
                        value = chargeText,
                        onValueChange = {
                            chargeText = it
                            val c = it.toDoubleOrNull() ?: 0.0
                            if (principal > 0) {
                                val computedRate = ((c / principal) * 1000.0).roundToInt() / 10.0
                                interestRateText = if (computedRate % 1.0 == 0.0) computedRate.toInt().toString() else computedRate.toString()
                            }
                            error = null
                        },
                        label = { Text("মোট সুদ/চার্জ (৳) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1.1f).testTag("input_charge_amount")
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Quick Interest Rate Percentage Pills
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(5 to "৫%", 10 to "১০%", 12 to "১২%", 15 to "১৫%", 20 to "২০%").forEach { (pct, label) ->
                        val isSelected = interestRate.toInt() == pct
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Color(0xFF1565C0) else Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, if (isSelected) Color(0xFF1565C0) else Color(0xFFCBD5E1)),
                            modifier = Modifier.clickable {
                                interestRateText = pct.toString()
                                chargeText = ((principal * pct) / 100.0).roundToInt().toString()
                            }
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else Color(0xFF1E293B),
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Installments Count & Repayment Frequency Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = installmentsText,
                        onValueChange = { installmentsText = it },
                        label = { Text("কিস্তি সংখ্যা *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(0.95f).testTag("input_installments_count")
                    )

                    Column(modifier = Modifier.weight(1.25f)) {
                        Text(
                            text = "কিস্তির সময়কাল:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF374151)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = frequency == "WEEKLY",
                                onClick = {
                                    frequency = "WEEKLY"
                                    if (installmentsText == "12") installmentsText = "20"
                                },
                                label = { Text("সাপ্তাহিক", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = frequency == "MONTHLY",
                                onClick = {
                                    frequency = "MONTHLY"
                                    if (installmentsText == "20") installmentsText = "12"
                                },
                                label = { Text("মাসিক", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = purpose,
                    onValueChange = { purpose = it },
                    label = { Text("ঋণের উদ্দেশ্য") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Calculation & Expected Repayment Schedule Preview Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                    border = BorderStroke(1.2.dp, Color(0xFF93C5FD)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("হিসাব ও প্রত্যাশিত সময়সূচী সারসংক্ষেপ", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E40AF))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("সর্বমোট পরিশোধযোগ্য:", style = MaterialTheme.typography.bodySmall, color = Color(0xFF1E293B))
                            Text("৳ ${totalPayable.toInt()} (মূল ৳${principal.toInt()} + সুদ $interestRateText%)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = Color(0xFF0F172A))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("প্রতি কিস্তির পরিমাণ:", style = MaterialTheme.typography.bodySmall, color = Color(0xFF1E293B))
                            Text(
                                text = "৳ $perInstallment / ${if (frequency == "MONTHLY") "মাস" else "সপ্তাহ"}",
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF1565C0)
                            )
                        }
                        HorizontalDivider(color = Color(0xFFBFDBFE))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("১ম কিস্তির তারিখ:", style = MaterialTheme.typography.labelSmall, color = Color(0xFF334155))
                            Text(dateSdf.format(Date(firstDueDate)), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("সম্ভাব্য সমাপ্তির তারিখ:", style = MaterialTheme.typography.labelSmall, color = Color(0xFF334155))
                            Text("${dateSdf.format(Date(finalDueDate))} ($totalInstallments কিস্তি)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        }
                    }
                }

                if (error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = error ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("বাতিল")
                    }

                    Button(
                        onClick = {
                            val member = selectedMember
                            if (member == null) {
                                error = "সদস্য নির্বাচন করুন"
                                return@Button
                            }
                            if (principal <= 0) {
                                error = "মূল ঋণের পরিমাণ দিন"
                                return@Button
                            }
                            if (totalInstallments <= 0) {
                                error = "কিস্তির সংখ্যা দিন"
                                return@Button
                            }
                            onSubmit(member, principal, interestRate, charge, totalInstallments, frequency, purpose)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.weight(1f).testTag("btn_confirm_disburse_loan")
                    ) {
                        Text("ঋণ প্রদান নিশ্চিত")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectInstallmentDialog(
    activeLoans: List<Loan>,
    preselectedLoan: Loan?,
    currentUser: User,
    onDismiss: () -> Unit,
    onSubmit: (Loan, Double, Double, String) -> Unit
) {
    var selectedLoan by remember { mutableStateOf(preselectedLoan ?: activeLoans.firstOrNull()) }
    var expanded by remember { mutableStateOf(false) }

    var amountText by remember {
        mutableStateOf(selectedLoan?.perInstallmentAmount?.toInt()?.toString() ?: "")
    }
    var penaltyText by remember { mutableStateOf("0") }
    var notes by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Text(
                    text = "ঋণের কিস্তি আদায় ফরম",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFB57300)
                )
                Text(
                    text = "চলমান ঋণ নির্বাচন করে কিস্তি গ্রহণ করুন",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(modifier = Modifier.height(14.dp))

                if (activeLoans.isEmpty()) {
                    Text(
                        text = "বর্তমানে আদায় করার মতো কোন চলমান ঋণ নেই!",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text("ফিরে যান")
                    }
                    return@Column
                }

                // Loan Dropdown
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedLoan?.let { "${it.memberName} (বকেয়া: ৳${it.remainingDue.toInt()})" } ?: "ঋণ নির্বাচন করুন",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("চলমান ঋণ") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        activeLoans.forEach { l ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("${l.memberName} (${l.memberCode})", fontWeight = FontWeight.SemiBold)
                                        Text("বকেয়া: ৳${l.remainingDue.toInt()} | কিস্তি: ৳${l.perInstallmentAmount.toInt()}", style = MaterialTheme.typography.labelSmall)
                                    }
                                },
                                onClick = {
                                    selectedLoan = l
                                    amountText = l.perInstallmentAmount.toInt().toString()
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                selectedLoan?.let { l ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("সদস্য কোড: ${l.memberCode} | ফোন: ${l.memberMobile}", style = MaterialTheme.typography.labelSmall)
                            Text("মোট ঋণ: ৳${l.totalPayable.toInt()} | পরিশোধিত: ৳${l.totalPaid.toInt()}", style = MaterialTheme.typography.bodySmall)
                            Text("বকেয়া টাকা: ৳${l.remainingDue.toInt()} (কিস্তি: ${l.paidInstallments}/${l.totalInstallments})", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Installment Amount
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it; error = null },
                    label = { Text("আদায়ের পরিমাণ (টাকা) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_installment_paid_amount")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Late penalty
                OutlinedTextField(
                    value = penaltyText,
                    onValueChange = { penaltyText = it },
                    label = { Text("জরিমানা / বিলম্ব ফি (ঐচ্ছিক)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("মন্তব্য / রশিদ নোট") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = error ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("বাতিল")
                    }

                    Button(
                        onClick = {
                            val loan = selectedLoan
                            val amount = amountText.toDoubleOrNull()
                            val penalty = penaltyText.toDoubleOrNull() ?: 0.0
                            if (loan == null) {
                                error = "ঋণ নির্বাচন করুন"
                                return@Button
                            }
                            if (amount == null || amount <= 0) {
                                error = "সঠিক আদায়ের পরিমাণ দিন"
                                return@Button
                            }
                            onSubmit(loan, amount, penalty, notes)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB57300)),
                        modifier = Modifier.weight(1f).testTag("btn_confirm_collect_installment")
                    ) {
                        Text("আদায় সম্পন্ন করুন")
                    }
                }
            }
        }
    }
}
