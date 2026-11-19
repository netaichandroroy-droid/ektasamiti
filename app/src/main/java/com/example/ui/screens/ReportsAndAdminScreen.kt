package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.DateWiseCashLedgerCalculator
import com.example.data.model.DateWiseCashLedgerEntry
import com.example.data.model.User
import com.example.ui.components.MonthlySummaryExportCard
import com.example.ui.viewmodel.SamitiViewModel
import kotlin.math.roundToInt

private val LedgerBannerBg = Color(0xFFB3DCE8)
private val LedgerBlueLine = Color(0xFF2563EB)

@Composable
fun ReportsAndAdminScreen(
    currentUser: User,
    viewModel: SamitiViewModel,
    onShowMessage: (String) -> Unit = {}
) {
    val members by viewModel.members.collectAsState()
    val savingsList by viewModel.savingsList.collectAsState()
    val loansList by viewModel.loansList.collectAsState()
    val installmentsList by viewModel.installmentsList.collectAsState()

    val ledger = remember(savingsList, loansList, installmentsList) {
        DateWiseCashLedgerCalculator.compute(
            savingsList = savingsList,
            loansList = loansList,
            installmentsList = installmentsList
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("reports_and_admin_screen"),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            DateWiseCashLedgerCard(
                entries = ledger.entries,
                totalCash = ledger.totalCash
            )
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
fun DateWiseCashLedgerCard(
    entries: List<DateWiseCashLedgerEntry>,
    totalCash: Double
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("cash_ledger_card")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top light-blue Cash Total header banner matching "ক্যাশ মোট= ৯০০০ টাকা"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LedgerBannerBg)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .testTag("cash_ledger_total_banner")
            ) {
                Text(
                    text = "ক্যাশ মোট=",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${totalCash.roundToInt()}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (totalCash >= 0.0) Color(0xFF0F172A) else Color(0xFFDC2626),
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(end = 10.dp)
                )
                Text(
                    text = "টাকা",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A),
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(44.dp)
                )
            }

            if (entries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "এখনো কোনো লেনদেন বা ক্যাশ হিসাব নেই",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    entries.forEachIndexed { index, entry ->
                        DateWiseLedgerBlock(
                            entry = entry,
                            showCarryInSection = index > 0,
                            isLastBlock = index == entries.lastIndex
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DateWiseLedgerBlock(
    entry: DateWiseCashLedgerEntry,
    showCarryInSection: Boolean,
    isLastBlock: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("cash_ledger_block_${entry.dayKey}")
    ) {
        val itemRows = buildList<Triple<String, Int, Color>> {
            if (entry.savingsDeposit > 0.0) {
                add(Triple("সঞ্চয়=", entry.savingsDeposit.roundToInt(), Color(0xFF0F172A)))
            }
            if (entry.installmentCollected > 0.0) {
                add(Triple("কিস্তি আদায়=", entry.installmentCollected.roundToInt(), Color(0xFF065F46)))
            }
            if (entry.savingsWithdrawal > 0.0) {
                add(Triple("উত্তোলন=", -entry.savingsWithdrawal.roundToInt(), Color(0xFFDC2626)))
            }
            if (entry.loanDisbursed > 0.0) {
                add(Triple("ঋণ প্রদান=", -entry.loanDisbursed.roundToInt(), Color(0xFFDC2626)))
            }
        }

        itemRows.forEachIndexed { rowIdx, (label, amountInt, amountColor) ->
            LedgerRow(
                dateText = if (rowIdx == 0) entry.displayDate else "",
                label = label,
                amountText = amountInt.toString(),
                amountColor = amountColor,
                isBold = rowIdx == 0
            )
        }

        // Blue horizontal rule separating day lines from "মোট আদায়="
        HorizontalDivider(
            thickness = 2.dp,
            color = LedgerBlueLine,
            modifier = Modifier.padding(vertical = 3.dp)
        )

        LedgerRow(
            dateText = "",
            label = "মোট আদায়=",
            amountText = "${entry.netDayCollection.roundToInt()}",
            amountColor = if (entry.netDayCollection >= 0.0) Color(0xFF0F172A) else Color(0xFFDC2626),
            isBold = true
        )

        if (showCarryInSection) {
            LedgerRow(
                dateText = "",
                label = "ক্যাশ জমা=",
                amountText = "${entry.openingCash.roundToInt()}",
                amountColor = Color(0xFF0F172A),
                isBold = true
            )

            HorizontalDivider(
                thickness = 2.dp,
                color = LedgerBlueLine,
                modifier = Modifier.padding(vertical = 3.dp)
            )

            LedgerRow(
                dateText = "",
                label = if (isLastBlock) "ক্যাশ মোট=" else "ক্যাশ জমা=",
                amountText = "${entry.closingCash.roundToInt()}",
                amountColor = if (entry.closingCash >= 0.0) Color(0xFF0F172A) else Color(0xFFDC2626),
                unitText = if (isLastBlock) "" else "টাকা",
                isBold = true,
                rowBg = if (isLastBlock) Color(0xFFF0F9FF) else Color.Transparent
            )
        } else {
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun LedgerRow(
    dateText: String,
    label: String,
    amountText: String,
    amountColor: Color,
    unitText: String = "টাকা",
    isBold: Boolean = false,
    rowBg: Color = Color.Transparent
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBg)
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            text = dateText,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A),
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.width(86.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = Color(0xFF0F172A),
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        )
        Text(
            text = amountText,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.ExtraBold,
            color = amountColor,
            textAlign = TextAlign.End,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier
                .width(72.dp)
                .padding(end = 8.dp)
        )
        Text(
            text = unitText,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold,
            color = Color(0xFF0F172A),
            textAlign = TextAlign.End,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.width(42.dp)
        )
    }
}
