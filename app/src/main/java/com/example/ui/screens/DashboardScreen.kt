package com.example.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import com.example.data.cloud.CloudSyncState
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.data.model.WeeklyChartAggregator
import com.example.ui.components.CloudAutoSyncBanner
import com.example.ui.components.MonthlySummaryPreviewDialog
import com.example.ui.components.StatCard
import com.example.ui.components.UpcomingLoanRemindersCard
import com.example.ui.components.WeeklyCollectionBarChartCard
import com.example.ui.viewmodel.SamitiViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    currentUser: User,
    viewModel: SamitiViewModel,
    onNavigateToTab: (Int) -> Unit,
    onOpenAddMember: () -> Unit,
    onOpenDeposit: () -> Unit,
    onOpenNewLoan: () -> Unit,
    onOpenCollectInstallment: () -> Unit,
    onOpenLoansTab: () -> Unit = {},
    onShowMessage: (String) -> Unit = {},
    cloudSyncState: CloudSyncState = CloudSyncState(),
    onSyncNow: () -> Unit = {},
    onGoogleSignInSuccess: () -> Unit = {}
) {
    val members by viewModel.members.collectAsState()
    val savingsList by viewModel.savingsList.collectAsState()
    val loansList by viewModel.loansList.collectAsState()
    val activeLoans by viewModel.activeLoans.collectAsState()
    val installmentsList by viewModel.installmentsList.collectAsState()

    val todaySavings = remember(savingsList) { viewModel.getTodaySavingsTotal() }
    val todayInstallments = remember(installmentsList) { viewModel.getTodayInstallmentTotal() }
    val todayTotalCollection = todaySavings + todayInstallments

    val totalSavingsBalance = remember(members) { members.sumOf { it.savingsBalance } }
    val totalActiveLoanDue = remember(activeLoans) { activeLoans.sumOf { it.remainingDue } }
    val totalLoansDisbursed = remember(loansList) { loansList.sumOf { it.principalAmount } }
    val totalLoansServiceCharge = remember(loansList) { loansList.sumOf { it.chargeAmount } }
    val last4WeeksData = remember(savingsList, installmentsList) {
        WeeklyChartAggregator.computeLast4Weeks(
            savingsList = savingsList,
            installmentsList = installmentsList
        )
    }

    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    var showMonthlyExportDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Cloud Auto-Sync Live Status Banner
        item {
            CloudAutoSyncBanner(
                syncState = cloudSyncState,
                onSyncNow = onSyncNow,
                onGoogleSignInSuccess = onGoogleSignInSuccess,
                onFeedbackMessage = onShowMessage
            )
        }

        // Welcome Header Banner
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    Color(0xFF1B7A49)
                                )
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "স্বাগতম,",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                                Text(
                                    text = currentUser.fullName,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (currentUser.role == UserRole.ADMIN) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = if (currentUser.role == UserRole.ADMIN) "অ্যাডমিন (Admin)" else "মাঠকর্মী (Collector)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (currentUser.role == UserRole.ADMIN) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.25f))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Today's total summary banner
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "আজকের মোট আদায় (সঞ্চয় + কিস্তি)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                                Text(
                                    text = "৳ ${todayTotalCollection.toInt()}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "সঞ্চয় জমা: ৳${todaySavings.toInt()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                                Text(
                                    text = "কিস্তি আদায়: ৳${todayInstallments.toInt()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Quick Action Buttons
        item {
            Text(
                text = "দ্রুত সেবা ও লেনদেন",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickActionButton(
                    icon = Icons.Default.PersonAdd,
                    label = "নতুন সদস্য",
                    color = Color(0xFF148549),
                    containerColor = Color(0xFF1FB364),
                    modifier = Modifier.weight(1f).testTag("quick_btn_new_member"),
                    onClick = onOpenAddMember
                )

                QuickActionButton(
                    icon = Icons.Default.Savings,
                    label = "সঞ্চয় জমা",
                    color = Color(0xFF008488),
                    containerColor = Color(0xFF00A8A8),
                    modifier = Modifier.weight(1f).testTag("quick_btn_deposit"),
                    onClick = onOpenDeposit
                )

                QuickActionButton(
                    icon = Icons.Default.Paid,
                    label = "কিস্তি আদায়",
                    color = Color(0xFFDF8200),
                    containerColor = Color(0xFFF6A10B),
                    modifier = Modifier.weight(1f).testTag("quick_btn_installment"),
                    onClick = onOpenCollectInstallment
                )

                QuickActionButton(
                    icon = Icons.Default.CreditCard,
                    label = "নতুন ঋণ",
                    color = Color(0xFF5A45FE),
                    containerColor = Color(0xFF7D62FA),
                    modifier = Modifier.weight(1f).testTag("quick_btn_new_loan"),
                    onClick = onOpenNewLoan
                )
            }
        }

        // Statistics Grid
        item {
            Text(
                text = "সমিতির আর্থিক সারসংক্ষেপ",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "মোট সক্রিয় সদস্য",
                        amountText = "${members.size} জন",
                        subtitle = "সাপ্তাহিক সঞ্চয়ী ও কালেকশন",
                        icon = Icons.Default.Group,
                        accentColor = Color(0xFF5A45FE),
                        gradientEndColor = Color(0xFF7D62FA),
                        containerColor = Color(0xFF5A45FE),
                        modifier = Modifier.weight(1f)
                    )

                    StatCard(
                        title = "মোট সঞ্চয় জমা স্থিতি",
                        amountText = "৳ ${totalSavingsBalance.toInt()}",
                        subtitle = "সদস্যদের আমানত",
                        icon = Icons.Default.AccountBalanceWallet,
                        accentColor = Color(0xFF008488),
                        gradientEndColor = Color(0xFF00A8A8),
                        containerColor = Color(0xFF008488),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "চলমান বকেয়া ঋণ",
                        amountText = "৳ ${totalActiveLoanDue.toInt()}",
                        subtitle = "${activeLoans.size} টি চলমান ঋণ",
                        icon = Icons.Default.Payments,
                        accentColor = Color(0xFFEA356B),
                        gradientEndColor = Color(0xFFF4608B),
                        containerColor = Color(0xFFEA356B),
                        modifier = Modifier.weight(1f)
                    )

                    StatCard(
                        title = "মোট ঋণ বিতরণ",
                        amountText = "৳ ${totalLoansDisbursed.toInt()}",
                        subtitle = "চার্জ: ৳${totalLoansServiceCharge.toInt()}",
                        icon = Icons.Default.AccountBalance,
                        accentColor = Color(0xFFDF8200),
                        gradientEndColor = Color(0xFFF6A10B),
                        containerColor = Color(0xFFDF8200),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val totalInstallmentsCollected = remember(loansList) { loansList.sumOf { it.totalPaid } }
                    StatCard(
                        title = "সর্বমোট কিস্তি আদায়",
                        amountText = "৳ ${totalInstallmentsCollected.toInt()}",
                        subtitle = "${installmentsList.size} টি কিস্তি সংগৃহীত",
                        icon = Icons.Default.Paid,
                        accentColor = Color(0xFF148549),
                        gradientEndColor = Color(0xFF1FB364),
                        containerColor = Color(0xFF148549),
                        modifier = Modifier.weight(1f)
                    )

                    StatCard(
                        title = "আজকের মোট আদায়",
                        amountText = "৳ ${todayTotalCollection.toInt()}",
                        subtitle = "সঞ্চয় ৳${todaySavings.toInt()} + কিস্তি ৳${todayInstallments.toInt()}",
                        icon = Icons.Default.CheckCircle,
                        accentColor = Color(0xFFF25C00),
                        gradientEndColor = Color(0xFFFF7F1A),
                        containerColor = Color(0xFFF25C00),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Last 4 Weeks Bar Chart Visualization
        item {
            WeeklyCollectionBarChartCard(buckets = last4WeeksData)
        }

        // Monthly Summary Export Shortcut Banner (Text Share + Download Picture Only)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE6E9EE)),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF1D4ED8)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.5.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showMonthlyExportDialog = true }
                    .testTag("card_dashboard_monthly_export")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFDBEAFE))
                                .border(1.2.dp, Color(0xFF1D4ED8), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Summarize,
                                contentDescription = "মাসিক হিসাব এক্সপোর্ট",
                                tint = Color(0xFF1D4ED8),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "মাসিক আর্থিক সারসংক্ষেপ এক্সপোর্ট",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "মেসেজিং অ্যাপে টেক্সট শেয়ার বা শুধুমাত্র ছবি ডাউনলোড",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1E293B)
                            )
                        }
                    }

                    Button(
                        onClick = { showMonthlyExportDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1D4ED8),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("btn_dashboard_open_monthly_export")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("এক্সপোর্ট", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Director Local Notification & Upcoming Weekly Loan Repayments Reminder Card
        if (currentUser.role == UserRole.ADMIN) {
            item {
                UpcomingLoanRemindersCard(
                    loansList = loansList,
                    onOpenLoansTab = { onOpenLoansTab() },
                    onFeedbackMessage = onShowMessage
                )
            }
        }

        // Recent Collections & Activities
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "সাম্প্রতিক লেনদেন ও কালেকশন",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A)
                )

                Text(
                    text = "সর্বশেষ ২০টি",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
            }
        }

        val recentSavings = savingsList.take(5)
        val recentInstallments = installmentsList.take(5)

        if (recentSavings.isEmpty() && recentInstallments.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "এখনো কোন লেনদেন সম্পন্ন হয়নি",
                        color = Color(0xFF1E293B),
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            items(recentInstallments) { ins ->
                ActivityRow(
                    title = "কিস্তি আদায় (${ins.memberName})",
                    subtitle = "${ins.memberCode} | কিস্তি নং ${ins.installmentNumber} | সংগ্রাহক: ${ins.collectedBy}",
                    amount = "+৳${ins.paidAmount.toInt()}",
                    time = sdf.format(Date(ins.paymentDate)),
                    isPositive = true,
                    icon = Icons.Default.Paid,
                    color = Color(0xFFB57300)
                )
            }

            items(recentSavings) { sav ->
                ActivityRow(
                    title = "${if (sav.type == "DEPOSIT") "সঞ্চয় জমা" else "সঞ্চয় উত্তোলন"} (${sav.memberName})",
                    subtitle = "${sav.memberCode} | রসিদ: ${sav.receiptNumber} | সংগ্রাহক: ${sav.collectedBy}",
                    amount = "${if (sav.type == "DEPOSIT") "+" else "-"}৳${sav.amount.toInt()}",
                    time = sdf.format(Date(sav.date)),
                    isPositive = sav.type == "DEPOSIT",
                    icon = Icons.Default.Savings,
                    color = if (sav.type == "DEPOSIT") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
        }
    }

    if (showMonthlyExportDialog) {
        MonthlySummaryPreviewDialog(
            members = members,
            savingsList = savingsList,
            loansList = loansList,
            installmentsList = installmentsList,
            generatedBy = currentUser.fullName,
            onDismiss = { showMonthlyExportDialog = false },
            onFeedbackMessage = onShowMessage
        )
    }
}

@Composable
fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    containerColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(color, containerColor)
                    )
                )
                .padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                maxLines = 1
            )
        }
    }
}

@Composable
fun ActivityRow(
    title: String,
    subtitle: String,
    amount: String,
    time: String,
    isPositive: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    val cardBg = if (color == Color(0xFFB57300) || color == Color(0xFFB45309)) Color(0xFFFEF3C7)
                 else if (isPositive) Color(0xFFD1FAE5)
                 else Color(0xFFFEE2E2)
    val cardBorder = if (color == Color(0xFFB57300) || color == Color(0xFFB45309)) Color(0xFFD97706)
                     else if (isPositive) Color(0xFF16A34A)
                     else Color(0xFFDC2626)
    val boldColor = if (color == Color(0xFFB57300) || color == Color(0xFFB45309)) Color(0xFF92400E)
                    else if (isPositive) Color(0xFF065F46)
                    else Color(0xFF991B1B)

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, cardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth()
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
                        .background(Color(0xFFE6E9EE))
                        .border(1.5.dp, cardBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = boldColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = amount,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = boldColor
                )
                Text(
                    text = time,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF334155)
                )
            }
        }
    }
}
