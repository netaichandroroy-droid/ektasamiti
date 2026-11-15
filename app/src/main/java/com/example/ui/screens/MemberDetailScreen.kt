package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.Loan
import com.example.data.model.LoanScheduleCalculator
import com.example.data.model.Member
import com.example.data.model.MemberHistoryAggregator
import com.example.data.model.MemberHistoryItem
import com.example.data.model.MemberHistoryType
import com.example.data.model.UserRole
import com.example.ui.components.LoanScheduleDialog
import com.example.ui.components.SamitiDatePickerDialog
import com.example.ui.theme.SilverCardAltBg
import com.example.ui.theme.SilverCardBg
import com.example.ui.theme.SilverCardBorder
import com.example.ui.viewmodel.SamitiViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberDetailScreen(
    member: Member,
    currentUserRole: UserRole,
    viewModel: SamitiViewModel,
    onBack: () -> Unit,
    onOpenDepositForMember: (Member) -> Unit,
    onOpenLoanForMember: (Member) -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val members by viewModel.members.collectAsState()
    val savingsList by viewModel.savingsList.collectAsState()
    val loansList by viewModel.loansList.collectAsState()
    val installmentsList by viewModel.installmentsList.collectAsState()

    // Live member object so balances refresh immediately after transactions
    val liveMember = remember(members, member) {
        members.firstOrNull { it.id == member.id } ?: member
    }

    val summary = remember(liveMember, savingsList, loansList, installmentsList) {
        MemberHistoryAggregator.buildMemberSummary(
            member = liveMember,
            savingsList = savingsList,
            loansList = loansList,
            installmentsList = installmentsList
        )
    }

    val memberSavings = remember(savingsList, liveMember.id) {
        savingsList.filter { it.memberId == liveMember.id }
    }
    val memberLoans = remember(loansList, liveMember.id) {
        loansList.filter { it.memberId == liveMember.id }
    }
    val memberPayments = remember(installmentsList, liveMember.id) {
        installmentsList.filter { it.memberId == liveMember.id }
    }

    var selectedTab by remember { mutableIntStateOf(0) } // 0: সকল লেনদেন, 1: সঞ্চয়, 2: কিস্তি পরিশোধ, 3: ঋণ তালিকা
    var selectedLoanForSchedule by remember { mutableStateOf<Loan?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showEditRegDatePicker by remember { mutableStateOf(false) }
    var historyItemToEditDate by remember { mutableStateOf<MemberHistoryItem?>(null) }
    var loanToEditDate by remember { mutableStateOf<Loan?>(null) }

    val dateTimeSdf = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    val dateSdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    val shareText = remember(liveMember, summary) {
        buildString {
            appendLine("📋 *সদস্যের পূর্ণাঙ্গ হিসাব বিবরণী*")
            appendLine("একতা সঞ্চয় ও ঋণ সমিতি")
            appendLine("━━━━━━━━━━━━━━━━━━━━")
            appendLine("নাম: ${liveMember.name} (${liveMember.memberCode})")
            appendLine("মোবাইল: ${liveMember.mobile}")
            appendLine("বর্তমান সঞ্চয় স্থিতি: ৳${liveMember.savingsBalance.toInt()}")
            appendLine("মোট সঞ্চয় জমা: ৳${summary.totalSavingsDeposited.toInt()} (${summary.savingsDepositCount} বার)")
            appendLine("মোট সঞ্চয় উত্তোলন: ৳${summary.totalSavingsWithdrawn.toInt()}")
            appendLine("মোট কিস্তি পরিশোধ: ৳${summary.totalLoanRepaid.toInt()} (${summary.loanPaymentCount}টি কিস্তি)")
            appendLine("বর্তমান বকেয়া ঋণ: ৳${liveMember.totalLoanDue.toInt()}")
            appendLine("━━━━━━━━━━━━━━━━━━━━")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("member_detail_screen")
    ) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = liveMember.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "হিসাব নং: ${liveMember.memberCode} • সম্পূর্ণ লেনদেন ইতিহাস",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            navigationIcon = {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("btn_back_member_detail")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "সদস্য তালিকায় ফিরুন"
                    )
                }
            },
            actions = {
                IconButton(
                    onClick = {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "সদস্য বিবরণী শেয়ার করুন"))
                    },
                    modifier = Modifier.testTag("btn_share_member_history")
                ) {
                    Icon(Icons.Default.Share, contentDescription = "শেয়ার করুন")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = SilverCardBg
            )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Profile & Contact Info Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SilverCardBg),
                    border = BorderStroke(1.2.dp, SilverCardBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(62.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFD1F4DE))
                                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!liveMember.photoUri.isNullOrEmpty() && liveMember.photoUri.startsWith("content://")) {
                                    AsyncImage(
                                        model = liveMember.photoUri,
                                        contentDescription = liveMember.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.size(62.dp).clip(CircleShape)
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (liveMember.photoUri == "avatar_1" || liveMember.photoUri == "avatar_4") {
                                            Icons.Default.Face
                                        } else {
                                            Icons.Default.Person
                                        },
                                        contentDescription = liveMember.name,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(34.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = liveMember.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF111827)
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Badge,
                                        contentDescription = null,
                                        tint = Color(0xFF1D4ED8),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${liveMember.memberCode} • যোগদান: ${dateSdf.format(Date(liveMember.registrationDate))}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF374151),
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFE0F2FE),
                                        modifier = Modifier
                                            .clickable { showEditRegDatePicker = true }
                                            .testTag("btn_detail_edit_member_reg_date")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.EditCalendar,
                                            contentDescription = "নিবন্ধনের তারিখ পরিবর্তন করুন",
                                            tint = Color(0xFF1D4ED8),
                                            modifier = Modifier.padding(4.dp).size(14.dp)
                                        )
                                    }
                                }
                                if (liveMember.nid.isNotBlank()) {
                                    Text(
                                        text = "NID: ${liveMember.nid} • ভর্তি ফি: ৳${liveMember.admissionFee.toInt()}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF4B5563)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SilverCardAltBg,
                            border = BorderStroke(1.dp, SilverCardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF15803D), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = liveMember.mobile.ifBlank { "মোবাইল নেই" },
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF111827)
                                        )
                                    }
                                    if (liveMember.mobile.isNotBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF148549),
                                            modifier = Modifier.clickable {
                                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${liveMember.mobile}"))
                                                context.startActivity(intent)
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Call, contentDescription = "কল করুন", tint = Color.White, modifier = Modifier.size(13.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("কল করুন", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White)
                                            }
                                        }
                                    }
                                }

                                if (liveMember.address.isNotBlank()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF1D4ED8), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "ঠিকানা: ${liveMember.address}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF1F2937)
                                        )
                                    }
                                }

                                if (liveMember.fatherOrHusbandName.isNotBlank() || liveMember.nomineeName.isNotBlank()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.SupervisorAccount, contentDescription = null, tint = Color(0xFF6D28D9), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        val info = buildList {
                                            if (liveMember.fatherOrHusbandName.isNotBlank()) add("পিতা/স্বামী: ${liveMember.fatherOrHusbandName}")
                                            if (liveMember.nomineeName.isNotBlank()) {
                                                val rel = if (liveMember.nomineeRelation.isNotBlank()) " (${liveMember.nomineeRelation})" else ""
                                                add("নমিনী: ${liveMember.nomineeName}$rel")
                                            }
                                        }.joinToString(" • ")
                                        Text(
                                            text = info,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF374151)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Vibrant Gradient Financial Breakdown Cards (2x2 Grid)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MemberGradientMetricCard(
                            title = "বর্তমান সঞ্চয় স্থিতি",
                            amount = "৳ ${liveMember.savingsBalance.toInt()}",
                            subtitle = "মোট জমা ৳${summary.totalSavingsDeposited.toInt()} (${summary.savingsDepositCount} বার)",
                            icon = Icons.Default.AccountBalanceWallet,
                            startColor = Color(0xFF008488),
                            endColor = Color(0xFF00A8A8),
                            modifier = Modifier.weight(1f)
                        )
                        MemberGradientMetricCard(
                            title = "মোট কিস্তি পরিশোধ",
                            amount = "৳ ${summary.totalLoanRepaid.toInt()}",
                            subtitle = "${summary.loanPaymentCount}টি কিস্তি • জরিমানা ৳${summary.totalPenaltyPaid.toInt()}",
                            icon = Icons.Default.Paid,
                            startColor = Color(0xFF148549),
                            endColor = Color(0xFF1FB364),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MemberGradientMetricCard(
                            title = "বর্তমান বকেয়া ঋণ",
                            amount = "৳ ${liveMember.totalLoanDue.toInt()}",
                            subtitle = "মোট গৃহীত ঋণ ৳${summary.totalLoanPrincipalTaken.toInt()} (${summary.loansTakenCount}টি)",
                            icon = Icons.Default.Payments,
                            startColor = Color(0xFFEA356B),
                            endColor = Color(0xFFF4608B),
                            modifier = Modifier.weight(1f)
                        )
                        MemberGradientMetricCard(
                            title = "সঞ্চয় উত্তোলন",
                            amount = "৳ ${summary.totalSavingsWithdrawn.toInt()}",
                            subtitle = "${summary.savingsWithdrawalCount} বার উত্তোলন করা হয়েছে",
                            icon = Icons.Default.Savings,
                            startColor = Color(0xFFDF8200),
                            endColor = Color(0xFFF6A10B),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 3. Quick Member Actions Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onOpenDepositForMember(liveMember) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF148549)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        modifier = Modifier.weight(1f).testTag("btn_detail_deposit")
                    ) {
                        Icon(Icons.Default.Savings, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "সঞ্চয় জমা",
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Button(
                        onClick = { onOpenLoanForMember(liveMember) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5A45FE)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        modifier = Modifier.weight(1f).testTag("btn_detail_new_loan")
                    ) {
                        Icon(Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "নতুন ঋণ",
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // 4. Filterable Transaction History Tabs
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SilverCardBg),
                    border = BorderStroke(1.dp, SilverCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ScrollableTabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = SilverCardBg,
                        edgePadding = 8.dp
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = {
                                Text(
                                    "সকল লেনদেন (${summary.timeline.size})",
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            modifier = Modifier.testTag("tab_member_all_history")
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = {
                                Text(
                                    "সঞ্চয় (${summary.savingsDepositCount + summary.savingsWithdrawalCount})",
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            modifier = Modifier.testTag("tab_member_savings_history")
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = {
                                Text(
                                    "কিস্তি আদায় (${summary.loanPaymentCount})",
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            modifier = Modifier.testTag("tab_member_loan_payments")
                        )
                        Tab(
                            selected = selectedTab == 3,
                            onClick = { selectedTab = 3 },
                            text = {
                                Text(
                                    "ঋণ সমূহ (${memberLoans.size})",
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            modifier = Modifier.testTag("tab_member_loans_list")
                        )
                    }
                }
            }

            // 5. Tab Content
            when (selectedTab) {
                0 -> {
                    if (summary.timeline.isEmpty()) {
                        item {
                            EmptyHistoryCard("এই সদস্যের এখনো কোন লেনদেন রেকর্ড করা হয়নি।")
                        }
                    } else {
                        items(summary.timeline, key = { it.id }) { item ->
                            val matchingLoan = item.relatedLoanId?.let { lid -> memberLoans.firstOrNull { it.id == lid } }
                            MemberHistoryRowCard(
                                item = item,
                                formattedTime = dateTimeSdf.format(Date(item.timestamp)),
                                onClick = {
                                    if (matchingLoan != null) {
                                        selectedLoanForSchedule = matchingLoan
                                    }
                                },
                                onEditDateClick = { historyItemToEditDate = item }
                            )
                        }
                    }
                }

                1 -> {
                    val savingsTimeline = summary.timeline.filter {
                        it.type == MemberHistoryType.SAVINGS_DEPOSIT ||
                            it.type == MemberHistoryType.SAVINGS_WITHDRAWAL
                    }
                    if (savingsTimeline.isEmpty()) {
                        item { EmptyHistoryCard("কোন সঞ্চয় জমা বা উত্তোলন ইতিহাস নেই।") }
                    } else {
                        items(savingsTimeline, key = { it.id }) { item ->
                            MemberHistoryRowCard(
                                item = item,
                                formattedTime = dateTimeSdf.format(Date(item.timestamp)),
                                onClick = {},
                                onEditDateClick = { historyItemToEditDate = item }
                            )
                        }
                    }
                }

                2 -> {
                    val loanPaymentTimeline = summary.timeline.filter {
                        it.type == MemberHistoryType.LOAN_PAYMENT
                    }
                    if (loanPaymentTimeline.isEmpty()) {
                        item { EmptyHistoryCard("এখনো কোন ঋণের কিস্তি পরিশোধের ইতিহাস নেই।") }
                    } else {
                        items(loanPaymentTimeline, key = { it.id }) { item ->
                            val matchingLoan = item.relatedLoanId?.let { lid -> memberLoans.firstOrNull { it.id == lid } }
                            MemberHistoryRowCard(
                                item = item,
                                formattedTime = dateTimeSdf.format(Date(item.timestamp)),
                                onClick = {
                                    if (matchingLoan != null) {
                                        selectedLoanForSchedule = matchingLoan
                                    }
                                },
                                onEditDateClick = { historyItemToEditDate = item }
                            )
                        }
                    }
                }

                else -> {
                    if (memberLoans.isEmpty()) {
                        item { EmptyHistoryCard("এই সদস্য এখনো কোন ঋণ গ্রহণ করেননি।") }
                    } else {
                        items(memberLoans, key = { it.id }) { loan ->
                            val effectiveRate = LoanScheduleCalculator.effectiveInterestRate(loan)
                            val rateText = LoanScheduleCalculator.formatInterestRate(effectiveRate)
                            val freqLabel = if (loan.frequency == "MONTHLY") "মাসিক" else "সাপ্তাহিক"
                            val progress = if (loan.totalPayable > 0) {
                                (loan.totalPaid / loan.totalPayable).toFloat().coerceIn(0f, 1f)
                            } else 0f

                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = SilverCardBg),
                                border = BorderStroke(1.2.dp, SilverCardBorder),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedLoanForSchedule = loan }
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "ঋণ #${loan.id} • মূল ৳${loan.principalAmount.toInt()}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF111827)
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (loan.status == "ACTIVE") Color(0xFFFEE2E2) else Color(0xFFD1F4DE)
                                        ) {
                                            Text(
                                                text = if (loan.status == "ACTIVE") "চলমান" else "পরিশোধিত",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (loan.status == "ACTIVE") Color(0xFFB91C1C) else Color(0xFF15803D),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "সুদ $rateText% (৳${loan.chargeAmount.toInt()}) • মোট পরিশোধযোগ্য: ৳${loan.totalPayable.toInt()}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF374151),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "কিস্তি: ${loan.paidInstallments}/${loan.totalInstallments} টি ($freqLabel • ৳${loan.perInstallmentAmount.toInt()}/কিস্তি)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF4B5563)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LinearProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)),
                                        color = if (loan.status == "ACTIVE") Color(0xFFD97706) else Color(0xFF148549),
                                        trackColor = SilverCardBorder
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "পরিশোধ: ৳${loan.totalPaid.toInt()}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF15803D)
                                        )
                                        Text(
                                            text = "বকেয়া: ৳${loan.remainingDue.toInt()}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (loan.remainingDue > 0) Color(0xFFDC2626) else Color(0xFF15803D)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color(0xFF1D4ED8), modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "বিতরণ: ${dateSdf.format(Date(loan.disbursedDate))} • সময়সূচী দেখতে ট্যাপ করুন",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1D4ED8)
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFFE0F2FE),
                                            modifier = Modifier
                                                .clickable { loanToEditDate = loan }
                                                .testTag("btn_detail_edit_loan_date_${loan.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.EditCalendar,
                                                contentDescription = "ঋণ বিতরণের তারিখ পরিবর্তন",
                                                tint = Color(0xFF1D4ED8),
                                                modifier = Modifier.padding(4.dp).size(15.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Admin-only Delete button
            if (currentUserRole == UserRole.ADMIN) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth().testTag("btn_admin_delete_member")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("অ্যাডমিন: সদস্য ডিলিট করুন", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    selectedLoanForSchedule?.let { loan ->
        val liveLoan = memberLoans.firstOrNull { it.id == loan.id } ?: loan
        LoanScheduleDialog(
            loan = liveLoan,
            payments = memberPayments.filter { it.loanId == liveLoan.id },
            onDismiss = { selectedLoanForSchedule = null },
            onCollectNextClick = {
                selectedLoanForSchedule = null
                onOpenLoanForMember(liveMember)
            },
            onUpdateDisbursedDate = { newMillis ->
                viewModel.updateLoanDisbursedDate(liveLoan, newMillis)
            }
        )
    }

    if (showEditRegDatePicker) {
        SamitiDatePickerDialog(
            initialDateMillis = liveMember.registrationDate,
            title = "সদস্য নিবন্ধনের তারিখ পরিবর্তন করুন",
            onDateSelected = { newMillis ->
                viewModel.updateMemberRegistrationDate(liveMember, newMillis)
                showEditRegDatePicker = false
            },
            onDismiss = { showEditRegDatePicker = false }
        )
    }

    loanToEditDate?.let { loan ->
        SamitiDatePickerDialog(
            initialDateMillis = loan.disbursedDate,
            title = "ঋণ বিতরণের তারিখ পরিবর্তন করুন",
            onDateSelected = { newMillis ->
                viewModel.updateLoanDisbursedDate(loan, newMillis)
                loanToEditDate = null
            },
            onDismiss = { loanToEditDate = null }
        )
    }

    historyItemToEditDate?.let { item ->
        SamitiDatePickerDialog(
            initialDateMillis = item.timestamp,
            title = "লেনদেনের তারিখ পরিবর্তন করুন",
            onDateSelected = { newMillis ->
                when (item.type) {
                    MemberHistoryType.SAVINGS_DEPOSIT,
                    MemberHistoryType.SAVINGS_WITHDRAWAL -> {
                        val tx = memberSavings.firstOrNull { "sav_${it.id}" == item.id }
                        if (tx != null) viewModel.updateSavingsTransactionDate(tx, newMillis)
                    }
                    MemberHistoryType.LOAN_PAYMENT -> {
                        val pay = memberPayments.firstOrNull { "pay_${it.id}" == item.id }
                        if (pay != null) viewModel.updateInstallmentPaymentDate(pay, newMillis)
                    }
                    MemberHistoryType.LOAN_DISBURSED -> {
                        val loan = memberLoans.firstOrNull { "loan_${it.id}" == item.id }
                        if (loan != null) viewModel.updateLoanDisbursedDate(loan, newMillis)
                    }
                }
                historyItemToEditDate = null
            },
            onDismiss = { historyItemToEditDate = null }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("সদস্য ডিলিট নিশ্চিতকরণ") },
            text = { Text("আপনি কি নিশ্চিত যে '${liveMember.name}' এর সকল তথ্য ডাটাবেজ থেকে মুছে ফেলতে চান?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteMember(liveMember, currentUserRole)
                        showDeleteConfirm = false
                        onBack()
                    }
                ) {
                    Text("হ্যাঁ, ডিলিট করুন", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("বাতিল")
                }
            }
        )
    }
}

@Composable
private fun MemberGradientMetricCard(
    title: String,
    amount: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    startColor: Color,
    endColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(startColor, endColor)))
                .padding(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.22f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 4.dp, y = 4.dp)
                    .size(40.dp)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.94f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = amount,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.88f),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun MemberHistoryRowCard(
    item: MemberHistoryItem,
    formattedTime: String,
    onClick: () -> Unit,
    onEditDateClick: (() -> Unit)? = null
) {
    val (accentColor, badgeBg, icon, amountPrefix) = when (item.type) {
        MemberHistoryType.SAVINGS_DEPOSIT -> listOf(
            Color(0xFF148549),
            Color(0xFFD1F4DE),
            Icons.Default.ArrowDownward,
            "+"
        )
        MemberHistoryType.SAVINGS_WITHDRAWAL -> listOf(
            Color(0xFFDC2626),
            Color(0xFFFEE2E2),
            Icons.Default.ArrowUpward,
            "-"
        )
        MemberHistoryType.LOAN_PAYMENT -> listOf(
            Color(0xFFD97706),
            Color(0xFFFEF3C7),
            Icons.Default.Paid,
            "+"
        )
        MemberHistoryType.LOAN_DISBURSED -> listOf(
            Color(0xFF5A45FE),
            Color(0xFFEDE9FE),
            Icons.Default.AccountBalance,
            ""
        )
    }
    val typedAccent = accentColor as Color
    val typedBadgeBg = badgeBg as Color
    val typedIcon = icon as androidx.compose.ui.graphics.vector.ImageVector
    val prefix = amountPrefix as String

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SilverCardBg),
        border = BorderStroke(1.dp, SilverCardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = item.relatedLoanId != null) { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(typedBadgeBg)
                    .border(1.dp, typedAccent.copy(alpha = 0.45f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = typedIcon,
                    contentDescription = item.title,
                    tint = typedAccent,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF111827)
                )
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF374151)
                )
                Text(
                    text = "$formattedTime • রসিদ: ${item.receiptNumber}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF4B5563)
                )
                if (item.notes.isNotBlank()) {
                    Text(
                        text = "মন্তব্য: ${item.notes}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF6B7280),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${prefix}৳${item.amount.toInt()}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = typedAccent
                )
                Text(
                    text = item.secondaryText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF374151)
                )
                if (onEditDateClick != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFE0F2FE),
                        modifier = Modifier
                            .clickable { onEditDateClick() }
                            .testTag("btn_edit_history_date_${item.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.EditCalendar,
                                contentDescription = "তারিখ পরিবর্তন করুন",
                                tint = Color(0xFF1D4ED8),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "তারিখ",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1D4ED8),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyHistoryCard(message: String) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SilverCardBg),
        border = BorderStroke(1.dp, SilverCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                tint = Color(0xFF64748B),
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF475569)
            )
        }
    }
}
