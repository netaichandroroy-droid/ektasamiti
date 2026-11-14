package com.example.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Loan
import com.example.data.model.Member
import com.example.data.model.SavingsTransaction
import com.example.data.model.User
import com.example.ui.viewmodel.SamitiViewModel
import java.util.Calendar
import java.util.Locale

data class FridayDateInfo(
    val weekIndex: Int,
    val dayOfMonth: Int,
    val dateMillis: Long,
    val displayLabel: String,
    val fullDateString: String,
    val shortDateString: String // e.g. "6.11.26" matching user's spreadsheet image
)

data class MonthFridaysGroup(
    val monthIndex: Int,
    val year: Int,
    val monthNameBangla: String,
    val monthLabelShort: String, // e.g. "Nov'26"
    val fridays: List<FridayDateInfo>
)

val MONTH_NAMES_BANGLA = listOf(
    "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
    "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"
)

val MONTH_NAMES_SHORT = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
)

fun calculateFridaysForMonth(year: Int, monthIndex: Int): List<FridayDateInfo> {
    val fridays = mutableListOf<FridayDateInfo>()
    val cal = Calendar.getInstance()
    cal.set(Calendar.YEAR, year)
    cal.set(Calendar.MONTH, monthIndex)
    cal.set(Calendar.DAY_OF_MONTH, 1)
    cal.set(Calendar.HOUR_OF_DAY, 12)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)

    val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    var weekNum = 1

    for (day in 1..maxDays) {
        cal.set(Calendar.DAY_OF_MONTH, day)
        if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY) {
            val ordinal = when (weekNum) {
                1 -> "১ম"
                2 -> "২য়"
                3 -> "৩য়"
                4 -> "৪র্থ"
                5 -> "৫ম"
                else -> "${weekNum}ম"
            }
            val shortYear = year % 100
            fridays.add(
                FridayDateInfo(
                    weekIndex = weekNum,
                    dayOfMonth = day,
                    dateMillis = cal.timeInMillis,
                    displayLabel = "$ordinal শুক্রবার ($day তারিখ)",
                    fullDateString = String.format(Locale.getDefault(), "%02d/%02d/%d", day, monthIndex + 1, year),
                    shortDateString = "$day.${monthIndex + 1}.$shortYear"
                )
            )
            weekNum++
        }
    }
    return fridays
}

fun calculateTwoMonthsFridays(year: Int, startMonthIndex: Int): List<MonthFridaysGroup> {
    val groups = mutableListOf<MonthFridaysGroup>()
    for (i in 0..1) {
        var mIdx = startMonthIndex + i
        var y = year
        if (mIdx > 11) {
            mIdx -= 12
            y += 1
        }
        val shortLabel = "${MONTH_NAMES_SHORT[mIdx]}'${y % 100}"
        val banglaName = MONTH_NAMES_BANGLA[mIdx]
        val fridays = calculateFridaysForMonth(y, mIdx)
        groups.add(
            MonthFridaysGroup(
                monthIndex = mIdx,
                year = y,
                monthNameBangla = banglaName,
                monthLabelShort = shortLabel,
                fridays = fridays
            )
        )
    }
    return groups
}

/**
 * FULL PAGE Weekly Collection Screen (সাপ্তাহিক খাতা / শীট)
 * As requested: "weekly collection page ta full page hobe"
 * Displays the complete spreadsheet table from user's image across the full screen height!
 */
@Composable
fun WeeklyCollectionScreen(
    currentUser: User,
    viewModel: SamitiViewModel,
    initiallyOpenAddPopup: Boolean = false,
    onPopupDismissed: () -> Unit = {}
) {
    val currentCal = remember { Calendar.getInstance() }
    var selectedYear by remember { mutableIntStateOf(currentCal.get(Calendar.YEAR)) }
    var selectedMonthIndex by remember { mutableIntStateOf(currentCal.get(Calendar.MONTH)) }

    val members by viewModel.members.collectAsState()
    val savingsList by viewModel.savingsList.collectAsState()
    val loansList by viewModel.loansList.collectAsState()
    val activeLoans by viewModel.activeLoans.collectAsState()
    val installmentsList by viewModel.installmentsList.collectAsState()

    var showAddBatchCollectionPopup by remember { mutableStateOf(initiallyOpenAddPopup) }
    var showSingleCellDialog by remember { mutableStateOf<Pair<Member, FridayDateInfo>?>(null) }

    LaunchedEffect(initiallyOpenAddPopup) {
        if (initiallyOpenAddPopup) {
            showAddBatchCollectionPopup = true
        }
    }

    val twoMonthsGroups = remember(selectedYear, selectedMonthIndex) {
        calculateTwoMonthsFridays(selectedYear, selectedMonthIndex)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFD5DAE1))
                .testTag("weekly_collection_screen")
        ) {
            // Full Page Top Header: Clean, compact, professional
            Surface(
                color = Color(0xFFE6E9EE),
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Weekly Collection",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF1B7A49)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFE8F8EE),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1B7A49).copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "${twoMonthsGroups.firstOrNull()?.monthLabelShort ?: ""} ও ${twoMonthsGroups.getOrNull(1)?.monthLabelShort ?: ""}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1B7A49),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // "+ কালেকশন" Button
                        Button(
                            onClick = { showAddBatchCollectionPopup = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B7A49)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("btn_add_weekly_collection_top")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ কালেকশন", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Month Selector & Year Navigator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Horizontal Month Chips
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            MONTH_NAMES_BANGLA.forEachIndexed { index, name ->
                                val isSelected = selectedMonthIndex == index
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) Color(0xFF1B7A49) else Color(0xFFE8ECE9),
                                    modifier = Modifier
                                        .clickable { selectedMonthIndex = index }
                                        .padding(vertical = 1.dp)
                                ) {
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isSelected) Color.White else Color(0xFF0F172A),
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Year Buttons
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { selectedYear-- }, modifier = Modifier.size(26.dp)) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Year", modifier = Modifier.size(18.dp))
                            }
                            Text(
                                text = "$selectedYear",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF1B7A49)
                            )
                            IconButton(onClick = { selectedYear++ }, modifier = Modifier.size(26.dp)) {
                                Icon(Icons.Default.ChevronRight, contentDescription = "Next Year", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            // FULL PAGE SPREADSHEET TABLE: Fills the entire remaining screen height!
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                FullPageWeeklyCollectionTableView(
                    monthGroups = twoMonthsGroups,
                    members = members,
                    savingsList = savingsList,
                    installmentsList = installmentsList,
                    onCellClick = { member, friday ->
                        showSingleCellDialog = Pair(member, friday)
                    },
                    onAddCollectionClick = {
                        showAddBatchCollectionPopup = true
                    }
                )
            }
        }

        // Floating Action Button to Add Weekly Collection
        FloatingActionButton(
            onClick = { showAddBatchCollectionPopup = true },
            containerColor = Color(0xFF1B7A49),
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 16.dp)
                .shadow(6.dp, CircleShape)
                .testTag("fab_add_weekly_collection")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Collection", modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("কালেকশন যোগ", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }

    // Modal 1: "+ menur sob option remove koro" - Clean & simple popup without extra option buttons!
    if (showAddBatchCollectionPopup) {
        BatchWeeklyCollectionDialog(
            members = members,
            monthGroups = twoMonthsGroups,
            currentUser = currentUser,
            onDismiss = {
                showAddBatchCollectionPopup = false
                onPopupDismissed()
            },
            onSaveCollections = { collectionsList, selectedFriday ->
                viewModel.batchRecordWeeklyCollections(
                    collections = collectionsList,
                    fridayDateMillis = selectedFriday.dateMillis,
                    fridayLabel = selectedFriday.displayLabel,
                    collectedBy = currentUser.fullName,
                    role = currentUser.role,
                    onComplete = {
                        showAddBatchCollectionPopup = false
                        onPopupDismissed()
                    }
                )
            }
        )
    }

    // Modal 2: Single Cell Collection Dialog
    showSingleCellDialog?.let { (member, friday) ->
        val memberActiveLoan = activeLoans.firstOrNull { it.memberId == member.id }
        RecordFridayCollectionDialog(
            member = member,
            friday = friday,
            activeLoan = memberActiveLoan,
            currentUser = currentUser,
            onDismiss = { showSingleCellDialog = null },
            onSubmit = { savingsAmount, loanAmount, penalty, _ ->
                viewModel.recordFridayCellCollection(
                    member = member,
                    savingsAmount = savingsAmount,
                    loanAmount = loanAmount,
                    activeLoan = memberActiveLoan,
                    penalty = penalty,
                    fridayDateMillis = friday.dateMillis,
                    fridayLabel = friday.displayLabel,
                    collectedBy = currentUser.fullName,
                    role = currentUser.role,
                    onComplete = {
                        showSingleCellDialog = null
                    }
                )
            }
        )
    }
}

/**
 * FULL PAGE Spreadsheet Table View matching the User's Screenshot:
 *
 *               Ekta Samiti
 *   SL | Name |     Nov'26        |     Dec'26
 *             | 6.11.26 | 13.11.26| 4.12.26 | 11.12.26
 *    1 | Netai|   100   |   100   |   104   |   105
 *   Total=    |   500   |   500   |   512   |   515
 */
@Composable
fun FullPageWeeklyCollectionTableView(
    monthGroups: List<MonthFridaysGroup>,
    members: List<Member>,
    savingsList: List<SavingsTransaction>,
    installmentsList: List<com.example.data.model.InstallmentPayment>,
    onCellClick: (Member, FridayDateInfo) -> Unit,
    onAddCollectionClick: () -> Unit
) {
    val allFridays = remember(monthGroups) {
        monthGroups.flatMap { it.fridays }
    }

    val tableScrollState = rememberScrollState()

    val slColWidth = 48.dp
    val nameColWidth = 158.dp
    val dateColWidth = 82.dp
    val borderColor = Color(0xFF1E293B)

    if (members.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
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
                    text = "সদস্য তালিকা ও কালেকশন এন্ট্রি করতে নিচের বাটনে চাপ দিন",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF4B5563)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onAddCollectionClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B7A49))
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("+ সাপ্তাহিক কালেকশন যোগ করুন")
                }
            }
        }
        return
    }

    val totalTableWidth = slColWidth + nameColWidth + (dateColWidth * allFridays.size)

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE6E9EE)),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(tableScrollState)
        ) {
            // ROW 1: "Ekta Samiti" Title Banner (Full Width, Centered, Soft Peach Background matching picture)
            Box(
                modifier = Modifier
                    .width(totalTableWidth)
                    .background(Color(0xFFFDE8E0))
                    .border(1.dp, borderColor)
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Ekta Samiti",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A),
                    letterSpacing = 1.sp
                )
            }

            // ROW 2: Header Row with Months side-by-side (Cyan Background matching picture)
            Row(
                modifier = Modifier
                    .width(totalTableWidth)
                    .height(38.dp)
            ) {
                // Empty corner cell above SL & Name
                Box(
                    modifier = Modifier
                        .width(slColWidth + nameColWidth)
                        .fillMaxHeight()
                        .background(Color(0xFFE6E9EE))
                        .border(1.dp, borderColor)
                )

                // Month Group Headers (Cyan #00B0F0 background, bold centered text)
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

            // ROW 3: Date Sub-Header Row: SL, Name, 6.11.26, 13.11.26, 20.11.26, 27.11.26, 4.12.26...
            Row(
                modifier = Modifier
                    .width(totalTableWidth)
                    .height(36.dp)
            ) {
                // SL Header
                Box(
                    modifier = Modifier
                        .width(slColWidth)
                        .fillMaxHeight()
                        .background(Color(0xFFDCE1E8))
                        .border(1.dp, borderColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "SL", fontWeight = FontWeight.ExtraBold, fontSize = 13.5.sp, color = Color(0xFF0F172A))
                }

                // Name Header
                Box(
                    modifier = Modifier
                        .width(nameColWidth)
                        .fillMaxHeight()
                        .background(Color(0xFFDCE1E8))
                        .border(1.dp, borderColor)
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Name", fontWeight = FontWeight.ExtraBold, fontSize = 13.5.sp, color = Color(0xFF0F172A))
                }

                // Friday Date Headers (e.g. 6.11.26, 13.11.26...)
                allFridays.forEach { fDate ->
                    Box(
                        modifier = Modifier
                            .width(dateColWidth)
                            .fillMaxHeight()
                            .background(Color(0xFFDCE1E8))
                            .border(1.dp, borderColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = fDate.shortDateString,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = Color(0xFF0F172A),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // DATA ROWS: Fills the entire remaining screen height!
            LazyColumn(
                modifier = Modifier
                    .width(totalTableWidth)
                    .weight(1f)
            ) {
                itemsIndexed(members) { index, member ->
                    val sl = index + 1
                    val rowBg = if (index % 2 == 0) Color(0xFFE6E9EE) else Color(0xFFDCE1E8)

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
                                maxLines = 1
                            )
                        }

                        // Friday Date Amount Cells
                        allFridays.forEach { fDate ->
                            val windowStart = fDate.dateMillis - 86400000L * 3
                            val windowEnd = fDate.dateMillis + 86400000L * 3

                            val memberSavings = savingsList.filter {
                                it.memberId == member.id && it.date in windowStart..windowEnd && it.type == "DEPOSIT"
                            }
                            val memberInst = installmentsList.filter {
                                it.memberId == member.id && it.paymentDate in windowStart..windowEnd
                            }
                            val totalCellAmount = (memberSavings.sumOf { it.amount } + memberInst.sumOf { it.paidAmount }).toInt()
                            val isPaid = totalCellAmount > 0

                            Box(
                                modifier = Modifier
                                    .width(dateColWidth)
                                    .fillMaxHeight()
                                    .border(0.8.dp, borderColor)
                                    .background(if (isPaid) Color(0xFFBFDBFE) else Color.Transparent)
                                    .clickable { onCellClick(member, fDate) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isPaid) {
                                    Text(
                                        text = "$totalCellAmount",
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
                // Total Label spanning SL and Name
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

                // Column Totals for Each Friday
                allFridays.forEach { fDate ->
                    val windowStart = fDate.dateMillis - 86400000L * 3
                    val windowEnd = fDate.dateMillis + 86400000L * 3

                    val totalColSavings = savingsList.filter {
                        it.date in windowStart..windowEnd && it.type == "DEPOSIT"
                    }.sumOf { it.amount }

                    val totalColInst = installmentsList.filter {
                        it.paymentDate in windowStart..windowEnd
                    }.sumOf { it.paidAmount }

                    val colTotal = (totalColSavings + totalColInst).toInt()

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

/**
 * CLEAN BATCH POPUP: "+ menur sob option remove koro"
 * All extra option buttons (default amount chips, filters, etc.) have been removed!
 * Shows pure Friday date selection and clean member list with amount input!
 */
@Composable
fun BatchWeeklyCollectionDialog(
    members: List<Member>,
    monthGroups: List<MonthFridaysGroup>,
    currentUser: User,
    onDismiss: () -> Unit,
    onSaveCollections: (List<Pair<Member, Double>>, FridayDateInfo) -> Unit
) {
    val allFridays = remember(monthGroups) {
        monthGroups.flatMap { it.fridays }
    }

    var selectedFridayIndex by remember { mutableIntStateOf(0) }
    val currentSelectedFriday = allFridays.getOrNull(selectedFridayIndex) ?: allFridays.firstOrNull()

    // Member ID -> Amount text map (defaults to 100 as in user's image)
    val amountMap = remember {
        mutableStateMapOf<Long, String>().apply {
            members.forEach { m ->
                put(m.id, "100")
            }
        }
    }

    val enteredCount = members.count { (amountMap[it.id]?.toDoubleOrNull() ?: 0.0) > 0.0 }
    val totalEnteredAmount = members.sumOf { amountMap[it.id]?.toDoubleOrNull() ?: 0.0 }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE6E9EE)),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF1B7A49)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
                .testTag("dialog_batch_weekly_collection")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header: Clean and focused
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE8F8EE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF1B7A49), modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Weekly Collection এন্ট্রি",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF1B7A49)
                            )
                            Text(
                                text = "সদস্য তালিকা অনুযায়ী কালেকশন",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4B5563)
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF1F2937))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color(0xFFE5E7EB))
                Spacer(modifier = Modifier.height(10.dp))

                // Select Friday Date
                Text(
                    text = "শুক্রবার তারিখ নির্বাচন করুন:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF111827)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    allFridays.forEachIndexed { idx, fDate ->
                        val isSelected = selectedFridayIndex == idx
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Color(0xFF1976D2) else Color(0xFFF3F4F6),
                            border = androidx.compose.foundation.BorderStroke(1.2.dp, if (isSelected) Color(0xFF0D47A1) else Color(0xFFD1D5DB)),
                            modifier = Modifier.clickable { selectedFridayIndex = idx }
                        ) {
                            Text(
                                text = "${fDate.shortDateString} (${fDate.weekIndex}ম শুক্র)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isSelected) Color.White else Color(0xFF111827),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Member List: Clean, direct list without extra option menus!
                if (members.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("কোন সদস্য পাওয়া যায়নি", color = Color(0xFF4B5563), fontWeight = FontWeight.Bold)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(290.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(members) { idx, member ->
                            val currentVal = amountMap[member.id] ?: ""
                            val isAmountEntered = (currentVal.toDoubleOrNull() ?: 0.0) > 0.0

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isAmountEntered) Color(0xFFF0FDF4) else Color(0xFFF9FAFB),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.2.dp,
                                    if (isAmountEntered) Color(0xFF86EFAC) else Color(0xFFE5E7EB)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Member Info
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = if (isAmountEntered) Color(0xFF1B7A49) else Color(0xFF9CA3AF),
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "${idx + 1}",
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Column {
                                            Text(
                                                text = member.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFF111827),
                                                maxLines = 1
                                            )
                                            Text(
                                                text = "হিসাব নং: ${member.memberCode}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF4B5563)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Amount Input Box
                                    OutlinedTextField(
                                        value = currentVal,
                                        onValueChange = { amountMap[member.id] = it },
                                        placeholder = { Text("0") },
                                        prefix = { Text("৳", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827)) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .width(96.dp)
                                            .height(48.dp)
                                            .testTag("input_collection_${member.id}")
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFE5E7EB))
                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Summary Bar: Total & Save
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "মোট সদস্য: $enteredCount জন",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4B5563)
                        )
                        Text(
                            text = "সর্বমোট: ৳${totalEnteredAmount.toInt()}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1B7A49)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("বাতিল", fontWeight = FontWeight.Bold, color = Color(0xFF374151))
                        }

                        Button(
                            onClick = {
                                if (currentSelectedFriday != null) {
                                    val toSave = members.mapNotNull { m ->
                                        val amt = amountMap[m.id]?.toDoubleOrNull() ?: 0.0
                                        if (amt > 0.0) Pair(m, amt) else null
                                    }
                                    onSaveCollections(toSave, currentSelectedFriday)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B7A49)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("btn_save_batch_weekly_collection")
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("সংরক্ষণ করুন", fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecordFridayCollectionDialog(
    member: Member,
    friday: FridayDateInfo,
    activeLoan: Loan?,
    currentUser: User,
    onDismiss: () -> Unit,
    onSubmit: (Double, Double, Double, String) -> Unit
) {
    var savingsAmountText by remember { mutableStateOf("100") }
    var loanAmountText by remember { mutableStateOf(if (activeLoan != null) "${activeLoan.perInstallmentAmount.toInt()}" else "0") }
    var penaltyText by remember { mutableStateOf("0") }
    var note by remember { mutableStateOf("${friday.displayLabel} এর সাপ্তাহিক আদায়") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE6E9EE)),
            border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF1B7A49)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "সাপ্তাহিক কালেকশন এন্ট্রি",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1B7A49)
                        )
                        Text(
                            text = "${friday.displayLabel} (${friday.shortDateString})",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4B5563)
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF111827))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color(0xFFE5E7EB))
                Spacer(modifier = Modifier.height(10.dp))

                // Member Summary Card (Distinct soft green background, bold dark text)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF0FDF4),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF86EFAC)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = member.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF111827)
                        )
                        Text(
                            text = "হিসাব নং: ${member.memberCode} | মোবা: ${member.mobile}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF374151)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "বর্তমান সঞ্চয়: ৳${member.savingsBalance.toInt()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF1B7A49),
                                fontWeight = FontWeight.ExtraBold
                            )
                            if (activeLoan != null) {
                                Text(
                                    text = "বকেয়া ঋণ: ৳${activeLoan.remainingDue.toInt()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFD32F2F),
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Savings Amount
                OutlinedTextField(
                    value = savingsAmountText,
                    onValueChange = { savingsAmountText = it },
                    label = { Text("সাপ্তাহিক সঞ্চয় জমা (টাকা)", fontWeight = FontWeight.Bold) },
                    leadingIcon = { Icon(Icons.Default.Savings, contentDescription = null, tint = Color(0xFF1B7A49)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Loan Installment Amount (if loan active)
                if (activeLoan != null) {
                    OutlinedTextField(
                        value = loanAmountText,
                        onValueChange = { loanAmountText = it },
                        label = { Text("সাপ্তাহিক কিস্তি আদায় (টাকা)", fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Default.Paid, contentDescription = null, tint = Color(0xFFB57300)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Note
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("মন্তব্য / নোট", fontWeight = FontWeight.Bold) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("বাতিল", fontWeight = FontWeight.Bold, color = Color(0xFF374151))
                    }

                    Button(
                        onClick = {
                            val savAmt = savingsAmountText.toDoubleOrNull() ?: 0.0
                            val loanAmt = loanAmountText.toDoubleOrNull() ?: 0.0
                            val penalty = penaltyText.toDoubleOrNull() ?: 0.0
                            onSubmit(savAmt, loanAmt, penalty, note)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B7A49)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("জমা নিন", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}
