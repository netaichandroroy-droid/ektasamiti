package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.User
import com.example.ui.components.StatCard
import com.example.ui.viewmodel.SamitiViewModel

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
    val totalInstallmentsCollected = remember(loansList) { loansList.sumOf { it.totalPaid } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 92.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Quick Action Buttons
        item {
            Text(
                text = "দ্রুত সেবা ও লেনদেন",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("section_quick_actions")
            )

            Spacer(modifier = Modifier.height(10.dp))

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

        // Financial Summary Cards Grid
        item {
            Text(
                text = "সমিতির আর্থিক সারসংক্ষেপ",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("section_financial_summary")
            )

            Spacer(modifier = Modifier.height(10.dp))

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
    }
}

@Composable
fun QuickActionButton(
    icon: ImageVector,
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
