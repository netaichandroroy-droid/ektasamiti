package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.User
import com.example.ui.components.StatCard
import com.example.ui.viewmodel.SamitiViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
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
    onShowMessage: (String) -> Unit = {}
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

    val dhakaTimeZone = remember { TimeZone.getTimeZone("Asia/Dhaka") }
    val sdf = remember(dhakaTimeZone) {
        SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).apply { timeZone = dhakaTimeZone }
    }

    var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTimeMillis = System.currentTimeMillis()
            delay(1000L)
        }
    }
    val dhakaDateFormat = remember(dhakaTimeZone) {
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).apply { timeZone = dhakaTimeZone }
    }
    val digitalClockTimeFormat = remember(dhakaTimeZone) {
        SimpleDateFormat("hh:mm:ss", Locale.ENGLISH).apply { timeZone = dhakaTimeZone }
    }
    val digitalClockAmPmFormat = remember(dhakaTimeZone) {
        SimpleDateFormat("a", Locale.ENGLISH).apply { timeZone = dhakaTimeZone }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF07562E),
                                    Color(0xFF0B733F),
                                    Color(0xFF148E51)
                                )
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
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
                                Spacer(modifier = Modifier.height(8.dp))
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color.White.copy(alpha = 0.18f),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                                        modifier = Modifier.align(Alignment.CenterVertically)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CalendarMonth,
                                                contentDescription = "আজকের তারিখ",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = "আজ: ${dhakaDateFormat.format(Date(currentTimeMillis))}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }

                                    // Large Borderless Bold Red Digital Clock (Asia/Dhaka) beside Date
                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.CenterVertically)
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                            .testTag("dashboard_digital_clock"),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = "ডিজিটাল ঘড়ি (বাংলাদেশ সময়)",
                                            tint = Color(0xFFFF3B30),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = digitalClockTimeFormat.format(Date(currentTimeMillis)),
                                            style = TextStyle(
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 23.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFFFF2E2E),
                                                shadow = Shadow(
                                                    color = Color.Black.copy(alpha = 0.72f),
                                                    offset = Offset(1f, 2f),
                                                    blurRadius = 6f
                                                )
                                            ),
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = digitalClockAmPmFormat.format(Date(currentTimeMillis)),
                                            style = TextStyle(
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFFFF4D4D),
                                                shadow = Shadow(
                                                    color = Color.Black.copy(alpha = 0.72f),
                                                    offset = Offset(1f, 1.5f),
                                                    blurRadius = 5f
                                                )
                                            ),
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
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
    val cardBg = if (color == Color(0xFFB57300) || color == Color(0xFFB45309)) Color(0xFFFFFBEB)
                 else if (isPositive) Color(0xFFECFDF5)
                 else Color(0xFFFEF2F2)
    val cardBorder = if (color == Color(0xFFB57300) || color == Color(0xFFB45309)) Color(0xFFF59E0B)
                     else if (isPositive) Color(0xFF10B981)
                     else Color(0xFFEF4444)
    val boldColor = if (color == Color(0xFFB57300) || color == Color(0xFFB45309)) Color(0xFF92400E)
                    else if (isPositive) Color(0xFF065F46)
                    else Color(0xFF991B1B)

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, cardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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
                        .background(Color.White)
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
