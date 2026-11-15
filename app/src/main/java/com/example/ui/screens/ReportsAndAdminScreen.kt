package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.User
import com.example.ui.components.MonthlySummaryExportCard
import com.example.ui.viewmodel.SamitiViewModel

@Composable
fun ReportsAndAdminScreen(
    currentUser: User,
    viewModel: SamitiViewModel,
    onShowMessage: (String) -> Unit = {}
) {
    val members by viewModel.members.collectAsState()
    val savingsList by viewModel.savingsList.collectAsState()
    val loansList by viewModel.loansList.collectAsState()
    val activeLoans by viewModel.activeLoans.collectAsState()
    val installmentsList by viewModel.installmentsList.collectAsState()

    val todaySavings = remember(savingsList) { viewModel.getTodaySavingsTotal() }
    val todayInstallments = remember(installmentsList) { viewModel.getTodayInstallmentTotal() }
    val totalSavingsFund = remember(members) { members.sumOf { it.savingsBalance } }
    val totalLoansDisbursed = remember(loansList) { loansList.sumOf { it.principalAmount } }
    val totalProfitCharge = remember(loansList) { loansList.sumOf { it.chargeAmount } }
    val totalCollectedInstallments = remember(installmentsList) { installmentsList.sumOf { it.paidAmount } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("reports_and_admin_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Financial Report Overview
        item {
            Text(
                text = "সমিতির আর্থিক হিসাব ও রিপোর্ট",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF0F172A)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ReportRow(label = "আজকের সঞ্চয় জমা:", value = "৳ ${todaySavings.toInt()}", color = Color(0xFF0A6C3C))
                    ReportRow(label = "আজকের কিস্তি আদায়:", value = "৳ ${todayInstallments.toInt()}", color = Color(0xFFB45309))
                    ReportRow(label = "আজকের সর্বমোট আদায়:", value = "৳ ${(todaySavings + todayInstallments).toInt()}", isBold = true)

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(8.dp))

                    ReportRow(label = "মোট সদস্য সংখ্যা:", value = "${members.size} জন")
                    ReportRow(label = "মোট সঞ্চয় আমানত তহবিল:", value = "৳ ${totalSavingsFund.toInt()}", color = Color(0xFF0D683B))
                    ReportRow(label = "মোট ঋণ বিতরণ:", value = "৳ ${totalLoansDisbursed.toInt()}")
                    ReportRow(label = "নির্ধারিত সার্ভিস চার্জ / লাভ:", value = "৳ ${totalProfitCharge.toInt()}", color = Color(0xFF0369A1))
                    ReportRow(label = "সর্বমোট কিস্তি আদায়কৃত:", value = "৳ ${totalCollectedInstallments.toInt()}", color = Color(0xFFB45309))
                    ReportRow(label = "চলমান বকেয়া ঋণ:", value = "৳ ${activeLoans.sumOf { it.remainingDue }.toInt()}", color = Color(0xFFC62828))
                }
            }
        }

        // Monthly Financial Summary Export (Text Share + Download Picture Only)
        item {
            MonthlySummaryExportCard(
                members = members,
                savingsList = savingsList,
                loansList = loansList,
                installmentsList = installmentsList,
                generatedBy = currentUser.fullName,
                onFeedbackMessage = onShowMessage
            )
        }
    }
}

@Composable
fun ReportRow(
    label: String,
    value: String,
    color: Color = Color.Unspecified,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = if (isBold) Color(0xFF0F172A) else Color(0xFF1E293B)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.ExtraBold,
            color = if (color != Color.Unspecified) color else Color(0xFF0F172A)
        )
    }
}
