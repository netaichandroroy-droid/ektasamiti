package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.InstallmentPayment
import com.example.data.model.Loan
import com.example.data.model.Member

/**
 * ঋণ শীট (Loan Sheet) — modeled identically after the Weekly Collection / সঞ্চয় শীট spreadsheet:
 *
 *                    Ekta Samiti — ঋণ শীট
 *   SL | Name | মোট ঋণ | বকেয়া |     Nov'26        |     Dec'26
 *                                | 6.11.26 | 13.11.26| 4.12.26 | 11.12.26
 *    1 | Netai| 22000  | 20900 |  1100   |   1100  |  1100   |  1100
 *   Total=    | 22000  | 20900 |  1100   |   1100  |  1100   |  1100
 */
@Composable
fun FullPageLoanSheetTableView(
    monthGroups: List<MonthFridaysGroup>,
    members: List<Member>,
    loansList: List<Loan>,
    installmentsList: List<InstallmentPayment>,
    onHeaderDateClick: (FridayDateInfo) -> Unit = {},
    onCellClick: (Member, FridayDateInfo) -> Unit
) {
    val allFridays = remember(monthGroups) {
        monthGroups.flatMap { it.fridays }
    }

    val tableScrollState = rememberScrollState()

    val slColWidth = 44.dp
    val nameColWidth = 136.dp
    val loanTotalColWidth = 72.dp
    val loanDueColWidth = 72.dp
    val dateColWidth = 82.dp
    val borderColor = Color(0xFF1E293B)

    if (members.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
                .testTag("loan_sheet_empty"),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.TableChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "এখনো কোন সদস্য নিবন্ধিত নেই",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "নতুন সদস্য যোগ করতে উপরের + আইকনে চাপ দিন",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF4B5563)
                )
            }
        }
        return
    }

    val totalTableWidth =
        slColWidth + nameColWidth + loanTotalColWidth + loanDueColWidth + (dateColWidth * allFridays.size)

    val grandLoanTotal = remember(members, loansList) {
        members.sumOf { member ->
            val memberLoans = loansList.filter { it.memberId == member.id }
            val activeLoans = memberLoans.filter { it.status == "ACTIVE" }
            if (activeLoans.isNotEmpty()) {
                activeLoans.sumOf { it.totalPayable }.toInt()
            } else {
                memberLoans.sumOf { it.totalPayable }.toInt()
            }
        }
    }

    val grandDueTotal = remember(members, loansList) {
        members.sumOf { member ->
            loansList.filter { it.memberId == member.id && it.status == "ACTIVE" }
                .sumOf { it.remainingDue }
                .toInt()
        }
    }

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxSize()
            .testTag("loan_sheet_table")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ROW 1: Title Banner (Always centered in viewport, Soft Peach Background matching Savings Sheet)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFDE8E0))
                    .border(1.dp, borderColor)
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Ekta Samiti — ঋণ শীট",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A),
                    letterSpacing = 0.8.sp
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .horizontalScroll(tableScrollState)
            ) {

            // ROW 2: Header Row with Months side-by-side (Cyan Background matching Savings Sheet)
            Row(
                modifier = Modifier
                    .width(totalTableWidth)
                    .height(38.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(slColWidth + nameColWidth + loanTotalColWidth + loanDueColWidth)
                        .fillMaxHeight()
                        .background(Color(0xFFF8FAFC))
                        .border(1.dp, borderColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ঋণ ও বকেয়া হিসাব",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1E3A8A)
                    )
                }

                monthGroups.forEachIndexed { groupIdx, mGroup ->
                    val groupWidth = dateColWidth * mGroup.fridays.size
                    Box(
                        modifier = Modifier
                            .width(groupWidth)
                            .fillMaxHeight()
                            .background(Color(0xFF00B0F0))
                            .border(1.dp, borderColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mGroup.monthLabelShort,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = if (groupIdx == 0) Color(0xFF8B0000) else Color(0xFF0A2E6E)
                        )
                    }
                }
            }

            // ROW 3: Sub-Header Row: SL, Name, মোট ঋণ, বকেয়া, Friday Dates
            Row(
                modifier = Modifier
                    .width(totalTableWidth)
                    .height(48.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(slColWidth)
                        .fillMaxHeight()
                        .background(Color(0xFFEBF3EE))
                        .border(1.dp, borderColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "SL", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = Color(0xFF0F172A))
                }

                Box(
                    modifier = Modifier
                        .width(nameColWidth)
                        .fillMaxHeight()
                        .background(Color(0xFFEBF3EE))
                        .border(1.dp, borderColor)
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Name", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = Color(0xFF0F172A))
                }

                Box(
                    modifier = Modifier
                        .width(loanTotalColWidth)
                        .fillMaxHeight()
                        .background(Color(0xFFEFF6FF))
                        .border(1.dp, borderColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "মোট ঋণ",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.5.sp,
                        color = Color(0xFF1E3A8A),
                        maxLines = 1
                    )
                }

                Box(
                    modifier = Modifier
                        .width(loanDueColWidth)
                        .fillMaxHeight()
                        .background(Color(0xFFFEF2F2))
                        .border(1.dp, borderColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "বকেয়া",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.5.sp,
                        color = Color(0xFFB91C1C),
                        maxLines = 1
                    )
                }

                allFridays.forEach { fDate ->
                    Box(
                        modifier = Modifier
                            .width(dateColWidth)
                            .fillMaxHeight()
                            .background(if (fDate.isCustomDate) Color(0xFFDBEAFE) else Color(0xFFEBF3EE))
                            .border(1.dp, borderColor)
                            .clickable { onHeaderDateClick(fDate) }
                            .testTag("btn_edit_loan_week_date_${fDate.columnKey}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 3.dp)
                        ) {
                            Text(
                                text = fDate.shortDateString,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.5.sp,
                                color = if (fDate.isCustomDate) Color(0xFF1D4ED8) else Color(0xFF0F172A),
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.Default.EditCalendar,
                                contentDescription = "তারিখ আপডেট",
                                tint = Color(0xFF1565C0),
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                }
            }

            // DATA ROWS
            LazyColumn(
                modifier = Modifier
                    .width(totalTableWidth)
                    .weight(1f)
            ) {
                itemsIndexed(members, key = { _, m -> m.id }) { index, member ->
                    val sl = index + 1
                    val rowBg = if (index % 2 == 0) Color(0xFFFFFFFF) else Color(0xFFF4F8F6)
                    val memberLoans = loansList.filter { it.memberId == member.id }
                    val activeLoansForMember = memberLoans.filter { it.status == "ACTIVE" }
                    val hasAnyLoan = memberLoans.isNotEmpty()
                    val hasActiveLoan = activeLoansForMember.isNotEmpty()
                    val memberTotalPayable = if (hasActiveLoan) {
                        activeLoansForMember.sumOf { it.totalPayable }.toInt()
                    } else {
                        memberLoans.sumOf { it.totalPayable }.toInt()
                    }
                    val memberRemainingDue = activeLoansForMember.sumOf { it.remainingDue }.toInt()

                    Row(
                        modifier = Modifier
                            .width(totalTableWidth)
                            .height(44.dp)
                            .background(rowBg)
                    ) {
                        // SL Column
                        Box(
                            modifier = Modifier
                                .width(slColWidth)
                                .fillMaxHeight()
                                .border(0.8.dp, borderColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$sl",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0F172A)
                            )
                        }

                        // Name Column
                        Box(
                            modifier = Modifier
                                .width(nameColWidth)
                                .fillMaxHeight()
                                .border(0.8.dp, borderColor)
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = member.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0F172A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // মোট ঋণ Column
                        Box(
                            modifier = Modifier
                                .width(loanTotalColWidth)
                                .fillMaxHeight()
                                .border(0.8.dp, borderColor)
                                .background(if (hasAnyLoan) Color(0xFFF8FAFC) else Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (hasAnyLoan) "$memberTotalPayable" else "-",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (hasAnyLoan) Color(0xFF1E3A8A) else Color(0xFF94A3B8)
                            )
                        }

                        // বকেয়া Column
                        Box(
                            modifier = Modifier
                                .width(loanDueColWidth)
                                .fillMaxHeight()
                                .border(0.8.dp, borderColor)
                                .background(
                                    when {
                                        !hasAnyLoan -> Color.Transparent
                                        hasActiveLoan && memberRemainingDue > 0 -> Color(0xFFFEF2F2)
                                        else -> Color(0xFFDBEAFE)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when {
                                    !hasAnyLoan -> "-"
                                    hasActiveLoan && memberRemainingDue > 0 -> "$memberRemainingDue"
                                    else -> "০ ✓"
                                },
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = when {
                                    !hasAnyLoan -> Color(0xFF94A3B8)
                                    hasActiveLoan && memberRemainingDue > 0 -> Color(0xFFC62828)
                                    else -> Color(0xFF1D4ED8)
                                }
                            )
                        }

                        // Friday Date Installment Cells
                        allFridays.forEach { fDate ->
                            val windowStart = fridayWeekWindowStart(fDate.dateMillis)
                            val windowEnd = fridayWeekWindowEnd(fDate.dateMillis)

                            val memberInst = installmentsList.filter {
                                it.memberId == member.id && it.paymentDate in windowStart..windowEnd
                            }
                            val paidInWeek = memberInst.sumOf { it.paidAmount }.toInt()
                            val isPaid = paidInWeek > 0

                            Box(
                                modifier = Modifier
                                    .width(dateColWidth)
                                    .fillMaxHeight()
                                    .border(0.8.dp, borderColor)
                                    .background(if (isPaid) Color(0xFFBFDBFE) else Color.Transparent)
                                    .clickable { onCellClick(member, fDate) }
                                    .testTag("loan_sheet_cell_${member.memberCode}_${fDate.columnKey}"),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isPaid) {
                                    Text(
                                        text = "$paidInWeek",
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF0D47A1)
                                    )
                                } else {
                                    Text(
                                        text = "-",
                                        fontSize = 14.sp,
                                        color = Color(0xFFC62828),
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // BOTTOM ROW: "Total=" Row with bold golden amber background
            Row(
                modifier = Modifier
                    .width(totalTableWidth)
                    .height(46.dp)
                    .background(Color(0xFFFDE68A))
            ) {
                Box(
                    modifier = Modifier
                        .width(slColWidth + nameColWidth)
                        .fillMaxHeight()
                        .border(1.2.dp, borderColor)
                        .padding(end = 12.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Text(
                        text = "Total=",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A)
                    )
                }

                Box(
                    modifier = Modifier
                        .width(loanTotalColWidth)
                        .fillMaxHeight()
                        .border(1.2.dp, borderColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$grandLoanTotal",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1E3A8A)
                    )
                }

                Box(
                    modifier = Modifier
                        .width(loanDueColWidth)
                        .fillMaxHeight()
                        .border(1.2.dp, borderColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$grandDueTotal",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (grandDueTotal > 0) Color(0xFFC62828) else Color(0xFF065F46)
                    )
                }

                allFridays.forEach { fDate ->
                    val windowStart = fridayWeekWindowStart(fDate.dateMillis)
                    val windowEnd = fridayWeekWindowEnd(fDate.dateMillis)

                    val colTotal = installmentsList.filter {
                        it.paymentDate in windowStart..windowEnd
                    }.sumOf { it.paidAmount }.toInt()

                    Box(
                        modifier = Modifier
                            .width(dateColWidth)
                            .fillMaxHeight()
                            .border(1.2.dp, borderColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$colTotal",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (colTotal > 0) Color(0xFF065F46) else Color(0xFF334155)
                        )
                    }
                }
            }
            }
        }
    }
}
