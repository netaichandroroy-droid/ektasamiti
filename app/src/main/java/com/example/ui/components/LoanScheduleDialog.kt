package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Schedule
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.InstallmentPayment
import com.example.data.model.Loan
import com.example.data.model.LoanScheduleCalculator
import com.example.data.model.RepaymentScheduleItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LoanScheduleDialog(
    loan: Loan,
    payments: List<InstallmentPayment>,
    onDismiss: () -> Unit,
    onCollectNextClick: () -> Unit
) {
    val schedule = remember(loan, payments) {
        LoanScheduleCalculator.generateSchedule(loan, payments)
    }
    val rate = remember(loan) { LoanScheduleCalculator.effectiveInterestRate(loan) }
    val rateText = remember(rate) { LoanScheduleCalculator.formatInterestRate(rate) }
    val dateSdf = remember { SimpleDateFormat("dd MMM yy", Locale.getDefault()) }
    val freqLabel = if (loan.frequency == "MONTHLY") "মাসিক কিস্তি" else "সাপ্তাহিক কিস্তি"

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE6E9EE)),
            border = BorderStroke(1.5.dp, Color(0xFF1565C0)),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .testTag("dialog_loan_schedule")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = Color(0xFF1565C0),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "ঋণ পরিশোধের সময়সূচী",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF111827)
                            )
                            Text(
                                text = "${loan.memberName} (${loan.memberCode})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF374151)
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "বন্ধ করুন", tint = Color(0xFF111827))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Summary Grid Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFEFF6FF),
                    border = BorderStroke(1.2.dp, Color(0xFF93C5FD)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            ScheduleMetricCell("মূল ঋণ", "৳${loan.principalAmount.toInt()}", Color(0xFF0F172A))
                            ScheduleMetricCell("সুদের হার", "$rateText% (+৳${loan.chargeAmount.toInt()})", Color(0xFFBE185D))
                            ScheduleMetricCell("মোট প্রদেয়", "৳${loan.totalPayable.toInt()}", Color(0xFF1D4ED8))
                        }
                        HorizontalDivider(color = Color(0xFFBFDBFE))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            ScheduleMetricCell("ধরণ", freqLabel, Color(0xFF334155))
                            ScheduleMetricCell("পরিশোধিত", "${loan.paidInstallments}/${loan.totalInstallments} টি", Color(0xFF15803D))
                            ScheduleMetricCell("বকেয়া", "৳${loan.remainingDue.toInt()}", Color(0xFFDC2626))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Table Column Headers
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFDCE1E8), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("নং", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E293B), modifier = Modifier.width(34.dp))
                    Text("প্রত্যাশিত তারিখ", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E293B), modifier = Modifier.weight(1.2f))
                    Text("কিস্তি (মূল+সুদ)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E293B), modifier = Modifier.weight(1.1f), textAlign = TextAlign.End)
                    Text("অবস্থা", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E293B), modifier = Modifier.weight(0.95f), textAlign = TextAlign.End)
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Schedule Rows
                Box(modifier = Modifier.weight(1f)) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(schedule, key = { it.installmentNumber }) { item ->
                            ScheduleRowCard(item = item, dateSdf = dateSdf)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Action Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("বন্ধ করুন", fontWeight = FontWeight.Bold)
                    }

                    if (loan.status == "ACTIVE") {
                        Button(
                            onClick = {
                                onDismiss()
                                onCollectNextClick()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                            modifier = Modifier.weight(1.3f).testTag("btn_schedule_collect_next")
                        ) {
                            Icon(Icons.Default.Paid, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("কিস্তি আদায় করুন", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleMetricCell(label: String, value: String, valueColor: Color) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF475569)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.ExtraBold,
            color = valueColor
        )
    }
}

@Composable
private fun ScheduleRowCard(
    item: RepaymentScheduleItem,
    dateSdf: SimpleDateFormat
) {
    val bgColor = when {
        item.isPaid -> Color(0xFFE8F1FC)
        item.isNextDue -> Color(0xFFFFF7E6)
        else -> Color(0xFFFEF2F2)
    }
    val borderColor = when {
        item.isPaid -> Color(0xFF90CAF9)
        item.isNextDue -> Color(0xFFF59E0B)
        else -> Color(0xFFFECACA)
    }
    val statusColor = when {
        item.isPaid -> Color(0xFF1976D2)
        item.isNextDue -> Color(0xFFB45309)
        else -> Color(0xFFD32F2F)
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("schedule_row_${item.installmentNumber}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${item.installmentNumber}নং",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF111827),
                modifier = Modifier.width(34.dp)
            )

            Column(modifier = Modifier.weight(1.2f)) {
                Text(
                    text = dateSdf.format(Date(item.dueDateMillis)),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )
                Text(
                    text = if (item.isPaid && item.paidDateMillis != null) {
                        "পরিশোধ: ${dateSdf.format(Date(item.paidDateMillis))}"
                    } else {
                        "অবশিষ্ট: ৳${item.remainingBalanceAfter.toInt()}"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF4B5563)
                )
            }

            Column(
                modifier = Modifier.weight(1.1f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "৳${item.expectedAmount.toInt()}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF111827)
                )
                Text(
                    text = "${item.principalPart.toInt()}+${item.interestPart.toInt()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF4B5563)
                )
            }

            Box(
                modifier = Modifier.weight(0.95f),
                contentAlignment = Alignment.CenterEnd
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusColor
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (item.isPaid) Icons.Default.CheckCircle else Icons.Default.Schedule,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = when {
                                item.isPaid -> "পরিশোধিত"
                                item.isNextDue -> "পরবর্তী"
                                else -> "বাকি"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
