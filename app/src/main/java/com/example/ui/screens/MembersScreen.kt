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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.InstallmentPayment
import com.example.data.model.Loan
import com.example.data.model.Member
import com.example.data.model.SavingsTransaction
import com.example.data.model.User
import com.example.ui.components.DatePickerField
import com.example.ui.components.SamitiDateUtils
import com.example.ui.viewmodel.SamitiViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MembersScreen(
    currentUser: User,
    viewModel: SamitiViewModel,
    showAddDialogInitially: Boolean = false,
    onConsumedAddDialogFlag: () -> Unit = {},
    onOpenDepositForMember: (Member) -> Unit = {},
    onOpenLoanForMember: (Member) -> Unit = {}
) {
    val context = LocalContext.current
    val members by viewModel.members.collectAsState()
    val activeLoans by viewModel.activeLoans.collectAsState()
    val savingsList by viewModel.savingsList.collectAsState()
    val installmentsList by viewModel.installmentsList.collectAsState()
    val submittedWeeklyMemberIds by viewModel.submittedWeeklyMemberIds.collectAsState()
    val passbookMember by viewModel.passbookMember.collectAsState()
    var memberForWeeklySubmit by remember { mutableStateOf<Member?>(null) }

    val activeWeekWindow = remember {
        val nowCal = Calendar.getInstance()
        val overrides = WeeklyCollectionDatePrefs.loadAllOverrides(context)
        val fridays = calculateTwoMonthsFridays(
            year = nowCal.get(Calendar.YEAR),
            startMonthIndex = nowCal.get(Calendar.MONTH),
            overrides = overrides
        ).flatMap { it.fridays }
        val now = System.currentTimeMillis()
        val currentWeek = fridays.minByOrNull { kotlin.math.abs(it.dateMillis - now) }
        if (currentWeek != null) {
            fridayWeekWindowStart(currentWeek.dateMillis) to fridayWeekWindowEnd(currentWeek.dateMillis)
        } else {
            fridayWeekWindowStart(now) to fridayWeekWindowEnd(now)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.setSearchQuery("")
        viewModel.setMemberFilter("ALL")
    }

    var showAddDialog by remember { mutableStateOf(showAddDialogInitially) }
    LaunchedEffect(showAddDialogInitially) {
        if (showAddDialogInitially) {
            showAddDialog = true
            onConsumedAddDialogFlag()
        }
    }

    if (passbookMember != null) {
        MemberDetailScreen(
            member = passbookMember!!,
            currentUserRole = currentUser.role,
            viewModel = viewModel,
            onBack = { viewModel.closePassbook() },
            onOpenDepositForMember = { m ->
                viewModel.closePassbook()
                onOpenDepositForMember(m)
            },
            onOpenLoanForMember = { m ->
                viewModel.closePassbook()
                onOpenLoanForMember(m)
            }
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("members_screen")
    ) {
        if (members.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                shadowElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // List Header: SL | নাম | টাকা
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFE8F5E9))
                            .padding(horizontal = 12.dp, vertical = 12.dp)
                            .testTag("members_list_header")
                    ) {
                        Text(
                            text = "SL",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF065F46),
                            modifier = Modifier.weight(0.14f)
                        )
                        Text(
                            text = "নাম",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF065F46),
                            modifier = Modifier.weight(0.64f)
                        )
                        Text(
                            text = "টাকা",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF065F46),
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(0.22f)
                        )
                    }

                    HorizontalDivider(color = Color(0xFF86EFAC))

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        itemsIndexed(members, key = { _, m -> m.id }) { index, member ->
                            val (weekStart, weekEnd) = activeWeekWindow
                            val isSubmitted = member.id in submittedWeeklyMemberIds ||
                                savingsList.any { tx ->
                                    tx.memberId == member.id &&
                                        tx.type == "DEPOSIT" &&
                                        tx.savingsPlan == "WEEKLY" &&
                                        tx.note != "প্রারম্ভিক সঞ্চয় জমা" &&
                                        tx.date in weekStart..weekEnd
                                } ||
                                installmentsList.any { pay ->
                                    pay.memberId == member.id && pay.paymentDate in weekStart..weekEnd
                                }
                            MemberListRow(
                                member = member,
                                isStriped = index % 2 == 1,
                                showSubmitButton = !isSubmitted,
                                onClick = { viewModel.openPassbook(member) },
                                onSubmitClick = { memberForWeeklySubmit = member }
                            )
                            if (index < members.lastIndex) {
                                HorizontalDivider(color = Color(0xFFE2E8F0))
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Member Dialog
    if (showAddDialog) {
        MemberRegistrationDialog(
            currentUser = currentUser,
            viewModel = viewModel,
            onDismiss = { showAddDialog = false }
        )
    }

    // Name-wise Weekly Collection Date & Taka Submit Dialog
    memberForWeeklySubmit?.let { selectedMember ->
        val memberActiveLoan = activeLoans.firstOrNull { it.memberId == selectedMember.id }
        val memberSavings = remember(savingsList, selectedMember.id) {
            savingsList.filter { it.memberId == selectedMember.id && it.type == "DEPOSIT" && it.note != "প্রারম্ভিক সঞ্চয় জমা" }
        }
        val memberInstallments = remember(installmentsList, selectedMember.id) {
            installmentsList.filter { it.memberId == selectedMember.id }
        }
        MemberWeeklySubmitDialog(
            member = selectedMember,
            activeLoan = memberActiveLoan,
            memberSavings = memberSavings,
            memberInstallments = memberInstallments,
            onDismiss = { memberForWeeklySubmit = null },
            onSubmit = { savingsAmt, loanAmt, dateMillis, dateLabel ->
                viewModel.recordFridayCellCollection(
                    member = selectedMember,
                    savingsAmount = savingsAmt,
                    loanAmount = loanAmt,
                    activeLoan = memberActiveLoan,
                    penalty = 0.0,
                    fridayDateMillis = dateMillis,
                    fridayLabel = dateLabel,
                    collectedBy = currentUser.fullName,
                    role = currentUser.role,
                    onComplete = { memberForWeeklySubmit = null }
                )
            }
        )
    }
}

@Composable
fun MemberListRow(
    member: Member,
    isStriped: Boolean,
    showSubmitButton: Boolean = true,
    onClick: () -> Unit,
    onSubmitClick: () -> Unit = {}
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .background(if (isStriped) Color(0xFFF8FAFC) else Color.White)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("member_card_${member.memberCode}")
    ) {
        Text(
            text = member.memberCode,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E3A8A),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(0.14f)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(0.64f)
        ) {
            Text(
                text = member.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0F172A),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(if (showSubmitButton) 0.42f else 1f)
            )
            if (showSubmitButton) {
                Button(
                    onClick = onSubmitClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(0.58f)
                        .heightIn(min = 48.dp)
                        .testTag("btn_member_submit_${member.memberCode}")
                ) {
                    Text(
                        text = "${member.name} - সাবমিট",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Text(
            text = "৳${member.savingsBalance.toInt()}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF15803D),
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(0.22f)
        )
    }
}

@Composable
fun MemberWeeklySubmitDialog(
    member: Member,
    activeLoan: Loan?,
    memberSavings: List<SavingsTransaction> = emptyList(),
    memberInstallments: List<InstallmentPayment> = emptyList(),
    onDismiss: () -> Unit,
    onSubmit: (savingsAmount: Double, loanAmount: Double, dateMillis: Long, dateLabel: String) -> Unit
) {
    val context = LocalContext.current
    val nowCal = remember { Calendar.getInstance() }
    val overrides = remember { WeeklyCollectionDatePrefs.loadAllOverrides(context) }
    val allFridays = remember(overrides) {
        calculateTwoMonthsFridays(
            year = nowCal.get(Calendar.YEAR),
            startMonthIndex = nowCal.get(Calendar.MONTH),
            overrides = overrides
        ).flatMap { it.fridays }
    }
    val defaultWeek = remember(allFridays) {
        val now = System.currentTimeMillis()
        allFridays.minByOrNull { kotlin.math.abs(it.dateMillis - now) }
    }

    var selectedDateMillis by remember(defaultWeek) {
        mutableStateOf(defaultWeek?.dateMillis ?: System.currentTimeMillis())
    }
    var savingsAmountText by remember { mutableStateOf("100") }
    var loanAmountText by remember {
        mutableStateOf(if (activeLoan != null) "${activeLoan.perInstallmentAmount.toInt()}" else "0")
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val matchingWeek = allFridays.firstOrNull { SamitiDateUtils.isSameLocalDay(it.dateMillis, selectedDateMillis) }
    val selectedDateLabel = matchingWeek?.displayLabel ?: SamitiDateUtils.formatDateDisplay(selectedDateMillis)
    val isAlreadySubmittedOnSelectedDate = remember(selectedDateMillis, memberSavings, memberInstallments) {
        memberSavings.any { SamitiDateUtils.isSameLocalDay(it.date, selectedDateMillis) } ||
            memberInstallments.any { SamitiDateUtils.isSameLocalDay(it.paymentDate, selectedDateMillis) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.2.dp, Color(0xFF0A6C3C)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("dialog_member_weekly_submit")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${member.name} — Weekly Collection",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0A6C3C),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "SL: ${member.memberCode} | বর্তমান টাকা: ৳${member.savingsBalance.toInt()}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF475569)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color(0xFFE2E8F0))
                Spacer(modifier = Modifier.height(10.dp))

                if (allFridays.isNotEmpty()) {
                    Text(
                        text = "Weekly Collection তারিখ নির্বাচন করুন:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        allFridays.forEach { fDate ->
                            val isSelected = SamitiDateUtils.isSameLocalDay(selectedDateMillis, fDate.dateMillis)
                            val isDoneOnDay = memberSavings.any { SamitiDateUtils.isSameLocalDay(it.date, fDate.dateMillis) } ||
                                memberInstallments.any { SamitiDateUtils.isSameLocalDay(it.paymentDate, fDate.dateMillis) }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = when {
                                    isSelected && isDoneOnDay -> Color(0xFFDC2626)
                                    isSelected -> Color(0xFF0A6C3C)
                                    isDoneOnDay -> Color(0xFFDCFCE7)
                                    else -> Color(0xFFF1F5F9)
                                },
                                border = BorderStroke(
                                    1.dp,
                                    when {
                                        isSelected && isDoneOnDay -> Color(0xFFDC2626)
                                        isSelected -> Color(0xFF0A6C3C)
                                        isDoneOnDay -> Color(0xFF16A34A)
                                        else -> Color(0xFFCBD5E1)
                                    }
                                ),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .clickable {
                                        selectedDateMillis = fDate.dateMillis
                                        errorMessage = null
                                    }
                                    .testTag("chip_member_week_${fDate.columnKey}")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = if (isDoneOnDay) "${fDate.shortDateString} (জমা)" else fDate.shortDateString,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = when {
                                            isSelected -> Color.White
                                            isDoneOnDay -> Color(0xFF15803D)
                                            else -> Color(0xFF0F172A)
                                        }
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                DatePickerField(
                    label = "কালেকশনের তারিখ",
                    selectedDateMillis = selectedDateMillis,
                    onDateSelected = {
                        selectedDateMillis = it
                        errorMessage = null
                    },
                    testTag = "btn_pick_member_weekly_date",
                    accentColor = Color(0xFF0A6C3C)
                )

                if (isAlreadySubmittedOnSelectedDate) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFEF2F2),
                        border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("warning_duplicate_date_submit")
                    ) {
                        Text(
                            text = "এই তারিখে (${SamitiDateUtils.formatShortDate(selectedDateMillis)}) ইতিমধ্যে সাবমিট করা হয়েছে! এক তারিখে দুইবার সাবমিট হবে না।",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB91C1C),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = savingsAmountText,
                    onValueChange = { savingsAmountText = it; errorMessage = null },
                    label = { Text("টাকা / সঞ্চয় জমা (৳)", fontWeight = FontWeight.Bold) },
                    leadingIcon = { Icon(Icons.Default.Savings, contentDescription = null, tint = Color(0xFF0A6C3C)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_member_weekly_savings")
                )

                if (activeLoan != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = loanAmountText,
                        onValueChange = { loanAmountText = it; errorMessage = null },
                        label = { Text("সাপ্তাহিক কিস্তি আদায় (৳)", fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Default.Paid, contentDescription = null, tint = Color(0xFFB57300)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_member_weekly_loan")
                    )
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        modifier = Modifier
                            .weight(0.75f)
                            .heightIn(min = 48.dp)
                    ) {
                        Text(
                            text = "বাতিল",
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Button(
                        onClick = {
                            if (isAlreadySubmittedOnSelectedDate) {
                                errorMessage = "এক তারিখে দুইবার সাবমিট হবে না!"
                                return@Button
                            }
                            val savAmt = savingsAmountText.trim().toDoubleOrNull() ?: 0.0
                            val loanAmt = if (activeLoan != null) loanAmountText.trim().toDoubleOrNull() ?: 0.0 else 0.0
                            if (savAmt <= 0.0 && loanAmt <= 0.0) {
                                errorMessage = "অনুগ্রহ করে ০ টাকার বেশি পরিমাণ লিখুন!"
                                return@Button
                            }
                            onSubmit(savAmt, loanAmt, selectedDateMillis, selectedDateLabel)
                        },
                        enabled = !isAlreadySubmittedOnSelectedDate,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A6C3C)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        modifier = Modifier
                            .weight(1.25f)
                            .heightIn(min = 48.dp)
                            .testTag("btn_confirm_member_weekly_submit")
                    ) {
                        Text(
                            text = "${member.name} - সাবমিট নিশ্চিত",
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MemberRegistrationDialog(
    currentUser: User,
    viewModel: SamitiViewModel,
    onDismiss: () -> Unit
) {
    val nextMemberCode by viewModel.nextMemberCode.collectAsState()
    var name by remember { mutableStateOf("") }
    var taka by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("dialog_member_registration")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Text(
                    text = "নতুন সদস্য এন্ট্রি",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "SL, নাম এবং টাকার পরিমাণ দিয়ে সদস্য নিবন্ধন করুন",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 1. SL (Auto Serial Number)
                OutlinedTextField(
                    value = nextMemberCode,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("SL / সিরিয়াল নং (অটো)") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Badge,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFD3F5E2),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = "অটো",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0A6C3C),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_auto_serial_number")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMessage = null },
                    label = { Text("নাম (Name) *") },
                    placeholder = { Text("যেমন: netai") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_member_name")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 3. Taka
                OutlinedTextField(
                    value = taka,
                    onValueChange = { taka = it; errorMessage = null },
                    label = { Text("টাকা (৳)") },
                    placeholder = { Text("যেমন: 500") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_member_taka")
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        modifier = Modifier.weight(0.8f)
                    ) {
                        Text(
                            text = "বাতিল",
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                errorMessage = "সদস্যের নাম আবশ্যক!"
                                return@Button
                            }
                            val amount = taka.trim().toDoubleOrNull() ?: 0.0

                            viewModel.registerNewMember(
                                name = name,
                                mobile = "",
                                fatherOrHusbandName = "",
                                nid = "",
                                address = "",
                                memberType = "WEEKLY",
                                nomineeName = "",
                                nomineeRelation = "",
                                photoUri = null,
                                admissionFee = 0.0,
                                initialSavings = amount,
                                registeredBy = currentUser.fullName,
                                role = currentUser.role,
                                registrationDate = System.currentTimeMillis(),
                                onSuccess = onDismiss
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        modifier = Modifier.weight(1.2f).testTag("btn_submit_member")
                    ) {
                        Text(
                            text = "নিবন্ধন করুন",
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
