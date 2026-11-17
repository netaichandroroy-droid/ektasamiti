package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.InstallmentPayment
import com.example.data.model.Loan
import com.example.data.model.Member
import com.example.data.model.SavingsTransaction
import com.example.data.model.User
import com.example.ui.components.DatePickerField
import com.example.ui.components.SamitiDatePickerDialog
import com.example.ui.components.SamitiDateUtils
import com.example.ui.viewmodel.SamitiViewModel
import java.util.Calendar
import java.util.Locale

data class FridayDateInfo(
    val weekIndex: Int,
    val dayOfMonth: Int,
    val dateMillis: Long,
    val displayLabel: String,
    val fullDateString: String,
    val shortDateString: String, // e.g. "6.11.26" matching user's spreadsheet image
    val columnKey: String = "",
    val defaultDateMillis: Long = dateMillis,
    val isCustomDate: Boolean = false
)

object WeeklyCollectionDatePrefs {
    private const val PREFS_NAME = "weekly_collection_date_prefs"

    fun loadAllOverrides(context: Context): Map<String, Long> {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val result = mutableMapOf<String, Long>()
        prefs.all.forEach { (k, v) ->
            val millis = when (v) {
                is Long -> v
                is Int -> v.toLong()
                else -> null
            }
            if (millis != null) {
                result[k] = millis
            }
        }
        return result
    }

    fun saveOverride(context: Context, key: String, millis: Long) {
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(key, millis)
            .apply()
    }

    fun clearOverride(context: Context, key: String) {
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(key)
            .apply()
    }
}

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

fun calculateFridaysForMonth(
    year: Int,
    monthIndex: Int,
    overrides: Map<String, Long> = emptyMap()
): List<FridayDateInfo> {
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
            val key = "${year}_${monthIndex}_$weekNum"
            val defaultMillis = cal.timeInMillis
            val overrideMillis = overrides[key]
            val effectiveMillis = overrideMillis ?: defaultMillis
            val effectiveCal = Calendar.getInstance().apply { timeInMillis = effectiveMillis }
            val effDay = effectiveCal.get(Calendar.DAY_OF_MONTH)
            val effMonth = effectiveCal.get(Calendar.MONTH) + 1
            val effYear = effectiveCal.get(Calendar.YEAR)
            val shortYear = effYear % 100
            val label = if (overrideMillis != null) {
                "$ordinal কলাম ($effDay/$effMonth/$shortYear)"
            } else {
                "$ordinal শুক্রবার ($day তারিখ)"
            }
            fridays.add(
                FridayDateInfo(
                    weekIndex = weekNum,
                    dayOfMonth = effDay,
                    dateMillis = effectiveMillis,
                    displayLabel = label,
                    fullDateString = String.format(Locale.getDefault(), "%02d/%02d/%d", effDay, effMonth, effYear),
                    shortDateString = "$effDay.$effMonth.$shortYear",
                    columnKey = key,
                    defaultDateMillis = defaultMillis,
                    isCustomDate = overrideMillis != null
                )
            )
            weekNum++
        }
    }
    return fridays
}

fun calculateTwoMonthsFridays(
    year: Int,
    startMonthIndex: Int,
    overrides: Map<String, Long> = emptyMap()
): List<MonthFridaysGroup> {
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
        val fridays = calculateFridaysForMonth(y, mIdx, overrides)
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

fun fridayWeekWindowStart(centerMillis: Long): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = centerMillis
        add(Calendar.DAY_OF_MONTH, -3)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}

fun fridayWeekWindowEnd(centerMillis: Long): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = centerMillis
        add(Calendar.DAY_OF_MONTH, 3)
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }
    return cal.timeInMillis
}

/**
 * FULL PAGE Weekly Collection Screen (সাপ্তাহিক খাতা / শীট)
 * As requested: "weekly collection page ta full page hobe"
 * Displays the complete spreadsheet table from user's image across the full screen height!
 */
@Composable
fun WeeklyCollectionScreen(
    currentUser: User,
    viewModel: SamitiViewModel
) {
    val context = LocalContext.current
    val currentCal = remember { Calendar.getInstance() }
    var selectedYear by remember { mutableIntStateOf(currentCal.get(Calendar.YEAR)) }
    var selectedMonthIndex by remember { mutableIntStateOf(currentCal.get(Calendar.MONTH)) }

    val members by viewModel.members.collectAsState()
    val savingsList by viewModel.savingsList.collectAsState()
    val loansList by viewModel.loansList.collectAsState()
    val activeLoans by viewModel.activeLoans.collectAsState()
    val installmentsList by viewModel.installmentsList.collectAsState()

    var customDateOverrides by remember {
        mutableStateOf(WeeklyCollectionDatePrefs.loadAllOverrides(context))
    }
    var showSingleCellDialog by remember { mutableStateOf<Pair<Member, FridayDateInfo>?>(null) }
    var showWeeklyDateUpdateDialog by remember { mutableStateOf(false) }
    var preselectedWeekForDateUpdate by remember { mutableStateOf<FridayDateInfo?>(null) }

    val twoMonthsGroups = remember(selectedYear, selectedMonthIndex, customDateOverrides) {
        calculateTwoMonthsFridays(selectedYear, selectedMonthIndex, customDateOverrides)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .testTag("weekly_collection_screen")
        ) {
            // Full Page Top Header: Clean, compact, professional
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Weekly Collection",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF1B7A49),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFE8F8EE),
                                border = BorderStroke(1.dp, Color(0xFF1B7A49).copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "${twoMonthsGroups.firstOrNull()?.monthLabelShort ?: ""} ও ${twoMonthsGroups.getOrNull(1)?.monthLabelShort ?: ""}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1B7A49),
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        OutlinedButton(
                            onClick = {
                                preselectedWeekForDateUpdate = null
                                showWeeklyDateUpdateDialog = true
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.2.dp, Color(0xFF1565C0)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("btn_weekly_update_date")
                        ) {
                            Icon(
                                imageVector = Icons.Default.EditCalendar,
                                contentDescription = "তারিখ আপডেট",
                                tint = Color(0xFF1565C0),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "তারিখ আপডেট",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1565C0),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
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

                        // Year Buttons & Today jump chip
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val isViewingCurrentMonth =
                                selectedYear == currentCal.get(Calendar.YEAR) &&
                                    selectedMonthIndex == currentCal.get(Calendar.MONTH)
                            if (!isViewingCurrentMonth) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF1B7A49),
                                    modifier = Modifier
                                        .clickable {
                                            selectedYear = currentCal.get(Calendar.YEAR)
                                            selectedMonthIndex = currentCal.get(Calendar.MONTH)
                                        }
                                        .testTag("btn_weekly_jump_today")
                                ) {
                                    Text(
                                        text = "আজ",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                            }
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
                    onHeaderDateClick = { friday ->
                        preselectedWeekForDateUpdate = friday
                        showWeeklyDateUpdateDialog = true
                    },
                    onCellClick = { member, friday ->
                        showSingleCellDialog = Pair(member, friday)
                    }
                )
            }
        }
    }

    // Modal 2: Single Cell Collection Dialog (also supports updating existing cell entry dates)
    showSingleCellDialog?.let { (member, friday) ->
        val memberActiveLoan = activeLoans.firstOrNull { it.memberId == member.id }
        val windowStart = fridayWeekWindowStart(friday.dateMillis)
        val windowEnd = fridayWeekWindowEnd(friday.dateMillis)
        val existingCellSavings = remember(savingsList, member.id, windowStart, windowEnd) {
            savingsList.filter {
                it.memberId == member.id && it.date in windowStart..windowEnd && it.type == "DEPOSIT"
            }
        }
        val existingCellInstallments = remember(installmentsList, member.id, windowStart, windowEnd) {
            installmentsList.filter {
                it.memberId == member.id && it.paymentDate in windowStart..windowEnd
            }
        }
        RecordFridayCollectionDialog(
            member = member,
            friday = friday,
            activeLoan = memberActiveLoan,
            currentUser = currentUser,
            existingSavings = existingCellSavings,
            existingInstallments = existingCellInstallments,
            onUpdateSavingsDate = { tx, newMillis ->
                viewModel.updateSavingsTransactionDate(tx, newMillis)
            },
            onUpdateInstallmentDate = { pay, newMillis ->
                viewModel.updateInstallmentPaymentDate(pay, newMillis)
            },
            onDismiss = { showSingleCellDialog = null },
            onSubmit = { savingsAmount, loanAmount, penalty, _, selectedDateMillis, selectedDateLabel ->
                viewModel.recordFridayCellCollection(
                    member = member,
                    savingsAmount = savingsAmount,
                    loanAmount = loanAmount,
                    activeLoan = memberActiveLoan,
                    penalty = penalty,
                    fridayDateMillis = selectedDateMillis,
                    fridayLabel = selectedDateLabel,
                    collectedBy = currentUser.fullName,
                    role = currentUser.role,
                    onComplete = {
                        showSingleCellDialog = null
                    }
                )
            }
        )
    }

    // Modal 3: Weekly Collection Page Date Update Dialog (header column date + entry dates)
    if (showWeeklyDateUpdateDialog) {
        WeeklyDateUpdateDialog(
            monthGroups = twoMonthsGroups,
            initialWeek = preselectedWeekForDateUpdate,
            savingsList = savingsList,
            installmentsList = installmentsList,
            onApplyWeekColumnDate = { targetWeek, newMillis ->
                val windowStart = fridayWeekWindowStart(targetWeek.dateMillis)
                val windowEnd = fridayWeekWindowEnd(targetWeek.dateMillis)
                val matchingSavings = savingsList.filter {
                    it.date in windowStart..windowEnd && it.type == "DEPOSIT"
                }
                val matchingInstallments = installmentsList.filter {
                    it.paymentDate in windowStart..windowEnd
                }
                if (targetWeek.columnKey.isNotBlank()) {
                    WeeklyCollectionDatePrefs.saveOverride(context, targetWeek.columnKey, newMillis)
                    customDateOverrides = WeeklyCollectionDatePrefs.loadAllOverrides(context)
                }
                viewModel.updateWeeklyEntriesDate(
                    savingsTransactions = matchingSavings,
                    installmentPayments = matchingInstallments,
                    newDateMillis = newMillis
                )
            },
            onResetWeekColumnDate = { targetWeek ->
                val windowStart = fridayWeekWindowStart(targetWeek.dateMillis)
                val windowEnd = fridayWeekWindowEnd(targetWeek.dateMillis)
                val matchingSavings = savingsList.filter {
                    it.date in windowStart..windowEnd && it.type == "DEPOSIT"
                }
                val matchingInstallments = installmentsList.filter {
                    it.paymentDate in windowStart..windowEnd
                }
                if (targetWeek.columnKey.isNotBlank()) {
                    WeeklyCollectionDatePrefs.clearOverride(context, targetWeek.columnKey)
                    customDateOverrides = WeeklyCollectionDatePrefs.loadAllOverrides(context)
                }
                viewModel.updateWeeklyEntriesDate(
                    savingsTransactions = matchingSavings,
                    installmentPayments = matchingInstallments,
                    newDateMillis = targetWeek.defaultDateMillis
                )
            },
            onUpdateSingleSavingsDate = { tx, newMillis ->
                viewModel.updateSavingsTransactionDate(tx, newMillis)
            },
            onUpdateSingleInstallmentDate = { pay, newMillis ->
                viewModel.updateInstallmentPaymentDate(pay, newMillis)
            },
            onDismiss = {
                showWeeklyDateUpdateDialog = false
                preselectedWeekForDateUpdate = null
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
    onHeaderDateClick: (FridayDateInfo) -> Unit = {},
    onCellClick: (Member, FridayDateInfo) -> Unit
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
                    text = "নতুন সদস্য যোগ করতে উপরের + আইকনে চাপ দিন",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF4B5563)
                )
            }
        }
        return
    }

    val totalTableWidth = slColWidth + nameColWidth + (dateColWidth * allFridays.size)

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                        .background(MaterialTheme.colorScheme.surface)
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
                    .height(48.dp)
            ) {
                // SL Header
                Box(
                    modifier = Modifier
                        .width(slColWidth)
                        .fillMaxHeight()
                        .background(Color(0xFFEBF3EE))
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
                        .background(Color(0xFFEBF3EE))
                        .border(1.dp, borderColor)
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Name", fontWeight = FontWeight.ExtraBold, fontSize = 13.5.sp, color = Color(0xFF0F172A))
                }

                // Friday Date Headers (clickable to update date directly from table)
                allFridays.forEach { fDate ->
                    Box(
                        modifier = Modifier
                            .width(dateColWidth)
                            .fillMaxHeight()
                            .background(if (fDate.isCustomDate) Color(0xFFDBEAFE) else Color(0xFFEBF3EE))
                            .border(1.dp, borderColor)
                            .clickable { onHeaderDateClick(fDate) }
                            .testTag("btn_edit_week_date_${fDate.columnKey}"),
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

            // DATA ROWS: Fills the entire remaining screen height!
            LazyColumn(
                modifier = Modifier
                    .width(totalTableWidth)
                    .weight(1f)
            ) {
                itemsIndexed(members) { index, member ->
                    val sl = index + 1
                    val rowBg = if (index % 2 == 0) Color(0xFFFFFFFF) else Color(0xFFF4F8F6)

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
                            val windowStart = fridayWeekWindowStart(fDate.dateMillis)
                            val windowEnd = fridayWeekWindowEnd(fDate.dateMillis)

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
                    val windowStart = fridayWeekWindowStart(fDate.dateMillis)
                    val windowEnd = fridayWeekWindowEnd(fDate.dateMillis)

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

@Composable
fun RecordFridayCollectionDialog(
    member: Member,
    friday: FridayDateInfo,
    activeLoan: Loan?,
    currentUser: User,
    existingSavings: List<SavingsTransaction> = emptyList(),
    existingInstallments: List<InstallmentPayment> = emptyList(),
    onUpdateSavingsDate: (SavingsTransaction, Long) -> Unit = { _, _ -> },
    onUpdateInstallmentDate: (InstallmentPayment, Long) -> Unit = { _, _ -> },
    onDismiss: () -> Unit,
    onSubmit: (Double, Double, Double, String, Long, String) -> Unit
) {
    var savingsAmountText by remember { mutableStateOf("100") }
    var loanAmountText by remember { mutableStateOf(if (activeLoan != null) "${activeLoan.perInstallmentAmount.toInt()}" else "0") }
    var penaltyText by remember { mutableStateOf("0") }
    var selectedDateMillis by remember(friday) { mutableStateOf(friday.dateMillis) }
    var editingSavingsTx by remember { mutableStateOf<SavingsTransaction?>(null) }
    var editingInstallmentPay by remember { mutableStateOf<InstallmentPayment?>(null) }
    val selectedDateLabel = remember(selectedDateMillis, friday) {
        if (SamitiDateUtils.isSameLocalDay(selectedDateMillis, friday.dateMillis)) {
            friday.displayLabel
        } else {
            SamitiDateUtils.formatDateDisplay(selectedDateMillis)
        }
    }
    var note by remember { mutableStateOf("${friday.displayLabel} এর সাপ্তাহিক আদায়") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF0A6C3C)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
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
                            text = "$selectedDateLabel (${SamitiDateUtils.formatShortDate(selectedDateMillis)})",
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

                if (existingSavings.isNotEmpty() || existingInstallments.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFEFF6FF),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF93C5FD)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "এই সপ্তাহের এন্ট্রি (তারিখ আপডেট করুন)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF1E3A8A)
                            )
                            existingSavings.forEach { tx ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "সঞ্চয়: ৳${tx.amount.toInt()} (${SamitiDateUtils.formatShortDate(tx.date)})",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFDBEAFE),
                                        modifier = Modifier
                                            .clickable { editingSavingsTx = tx }
                                            .testTag("btn_update_weekly_savings_date_${tx.id}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.EditCalendar,
                                                contentDescription = "তারিখ আপডেট",
                                                tint = Color(0xFF1D4ED8),
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "তারিখ আপডেট",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1D4ED8)
                                            )
                                        }
                                    }
                                }
                            }
                            existingInstallments.forEach { pay ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "কিস্তি #${pay.installmentNumber}: ৳${pay.paidAmount.toInt()} (${SamitiDateUtils.formatShortDate(pay.paymentDate)})",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFDBEAFE),
                                        modifier = Modifier
                                            .clickable { editingInstallmentPay = pay }
                                            .testTag("btn_update_weekly_inst_date_${pay.id}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.EditCalendar,
                                                contentDescription = "তারিখ আপডেট",
                                                tint = Color(0xFF1D4ED8),
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "তারিখ আপডেট",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1D4ED8)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                DatePickerField(
                    label = "কালেকশনের তারিখ",
                    selectedDateMillis = selectedDateMillis,
                    onDateSelected = { newMillis ->
                        selectedDateMillis = newMillis
                    },
                    testTag = "btn_pick_cell_collection_date",
                    accentColor = Color(0xFF1B7A49)
                )

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
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        modifier = Modifier.weight(0.85f)
                    ) {
                        Text(
                            text = "বাতিল",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF374151),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Button(
                        onClick = {
                            val savAmt = savingsAmountText.toDoubleOrNull() ?: 0.0
                            val loanAmt = loanAmountText.toDoubleOrNull() ?: 0.0
                            val penalty = penaltyText.toDoubleOrNull() ?: 0.0
                            onSubmit(savAmt, loanAmt, penalty, note, selectedDateMillis, selectedDateLabel)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B7A49)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        modifier = Modifier.weight(1.15f)
                    ) {
                        Text(
                            text = "জমা নিন",
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

    editingSavingsTx?.let { tx ->
        SamitiDatePickerDialog(
            initialDateMillis = tx.date,
            title = "সঞ্চয় জমার তারিখ আপডেট করুন",
            onDateSelected = { newMillis ->
                onUpdateSavingsDate(tx, newMillis)
                editingSavingsTx = null
            },
            onDismiss = { editingSavingsTx = null }
        )
    }

    editingInstallmentPay?.let { pay ->
        SamitiDatePickerDialog(
            initialDateMillis = pay.paymentDate,
            title = "কিস্তি আদায়ের তারিখ আপডেট করুন",
            onDateSelected = { newMillis ->
                onUpdateInstallmentDate(pay, newMillis)
                editingInstallmentPay = null
            },
            onDismiss = { editingInstallmentPay = null }
        )
    }
}

@Composable
fun WeeklyDateUpdateDialog(
    monthGroups: List<MonthFridaysGroup>,
    initialWeek: FridayDateInfo?,
    savingsList: List<SavingsTransaction>,
    installmentsList: List<InstallmentPayment>,
    onApplyWeekColumnDate: (FridayDateInfo, Long) -> Unit,
    onResetWeekColumnDate: (FridayDateInfo) -> Unit,
    onUpdateSingleSavingsDate: (SavingsTransaction, Long) -> Unit,
    onUpdateSingleInstallmentDate: (InstallmentPayment, Long) -> Unit,
    onDismiss: () -> Unit
) {
    val allFridays = remember(monthGroups) { monthGroups.flatMap { it.fridays } }
    val defaultSelectedKey = remember(allFridays, initialWeek) {
        initialWeek?.columnKey
            ?: run {
                val now = System.currentTimeMillis()
                allFridays.minByOrNull { kotlin.math.abs(it.dateMillis - now) }?.columnKey
            }
            ?: ""
    }
    var selectedColumnKey by remember(defaultSelectedKey) { mutableStateOf(defaultSelectedKey) }
    val selectedWeek = allFridays.firstOrNull { it.columnKey == selectedColumnKey } ?: allFridays.firstOrNull()

    var pickedDateMillis by remember(selectedWeek?.columnKey, selectedWeek?.dateMillis) {
        mutableStateOf(selectedWeek?.dateMillis ?: System.currentTimeMillis())
    }
    var editingSavingsTx by remember { mutableStateOf<SavingsTransaction?>(null) }
    var editingInstallmentPay by remember { mutableStateOf<InstallmentPayment?>(null) }

    val weekSavings = remember(selectedWeek, savingsList) {
        if (selectedWeek == null) emptyList()
        else {
            val start = fridayWeekWindowStart(selectedWeek.dateMillis)
            val end = fridayWeekWindowEnd(selectedWeek.dateMillis)
            savingsList.filter { it.date in start..end && it.type == "DEPOSIT" }
        }
    }
    val weekInstallments = remember(selectedWeek, installmentsList) {
        if (selectedWeek == null) emptyList()
        else {
            val start = fridayWeekWindowStart(selectedWeek.dateMillis)
            val end = fridayWeekWindowEnd(selectedWeek.dateMillis)
            installmentsList.filter { it.paymentDate in start..end }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.2.dp, Color(0xFF1565C0)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("dialog_weekly_date_update")
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
                            text = "সাপ্তাহিক তারিখ আপডেট",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1565C0)
                        )
                        Text(
                            text = "কলামের তারিখ বা সদস্যের এন্ট্রির তারিখ পরিবর্তন করুন",
                            style = MaterialTheme.typography.labelSmall,
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
                        text = "সপ্তাহ / কলাম নির্বাচন করুন:",
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
                        allFridays.forEach { week ->
                            val isSelected = selectedWeek?.columnKey == week.columnKey
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Color(0xFF1565C0) else Color(0xFFF1F5F9),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) Color(0xFF1565C0) else Color(0xFFCBD5E1)
                                ),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .clickable { selectedColumnKey = week.columnKey }
                                    .testTag("chip_update_week_${week.columnKey}")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = week.shortDateString,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else Color(0xFF0F172A)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (selectedWeek != null) {
                    DatePickerField(
                        label = "নতুন তারিখ (${selectedWeek.shortDateString} কলাম)",
                        selectedDateMillis = pickedDateMillis,
                        onDateSelected = { pickedDateMillis = it },
                        testTag = "btn_pick_weekly_column_new_date",
                        accentColor = Color(0xFF1565C0)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            onApplyWeekColumnDate(selectedWeek, pickedDateMillis)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("btn_apply_week_column_date")
                    ) {
                        Icon(Icons.Default.EditCalendar, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "কলাম ও এন্ট্রির তারিখ আপডেট করুন",
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (selectedWeek.isCustomDate) {
                        TextButton(
                            onClick = {
                                onResetWeekColumnDate(selectedWeek)
                                onDismiss()
                            },
                            modifier = Modifier
                                .align(Alignment.End)
                                .testTag("btn_reset_week_column_date")
                        ) {
                            Text("ডিফল্ট শুক্রবারে রিসেট করুন", color = Color(0xFFDC2626))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFE2E8F0))
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "এই সপ্তাহের এন্ট্রিসমূহ (${weekSavings.size + weekInstallments.size} টি):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (weekSavings.isEmpty() && weekInstallments.isEmpty()) {
                    Text(
                        text = "এই সপ্তাহে কোনো সংগৃহীত এন্ট্রি নেই। উপরের বাটন থেকে কলামের তারিখ পরিবর্তন করতে পারবেন।",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        weekSavings.forEach { tx ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF0FDF4),
                                border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${tx.memberName} (সঞ্চয়: ৳${tx.amount.toInt()})",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = "বর্তমান তারিখ: ${SamitiDateUtils.formatShortDate(tx.date)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF475569)
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFDBEAFE),
                                        modifier = Modifier
                                            .heightIn(min = 40.dp)
                                            .clickable { editingSavingsTx = tx }
                                            .testTag("btn_update_sheet_savings_date_${tx.id}")
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Icon(Icons.Default.EditCalendar, contentDescription = null, tint = Color(0xFF1D4ED8), modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("তারিখ বদলান", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF1D4ED8))
                                        }
                                    }
                                }
                            }
                        }

                        weekInstallments.forEach { pay ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFFFFBEB),
                                border = BorderStroke(1.dp, Color(0xFFFCD34D)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${pay.memberName} (কিস্তি #${pay.installmentNumber}: ৳${pay.paidAmount.toInt()})",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = "বর্তমান তারিখ: ${SamitiDateUtils.formatShortDate(pay.paymentDate)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF475569)
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFDBEAFE),
                                        modifier = Modifier
                                            .heightIn(min = 40.dp)
                                            .clickable { editingInstallmentPay = pay }
                                            .testTag("btn_update_sheet_inst_date_${pay.id}")
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Icon(Icons.Default.EditCalendar, contentDescription = null, tint = Color(0xFF1D4ED8), modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("তারিখ বদলান", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF1D4ED8))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    editingSavingsTx?.let { tx ->
        SamitiDatePickerDialog(
            initialDateMillis = tx.date,
            title = "সঞ্চয় জমার তারিখ আপডেট করুন",
            onDateSelected = { newMillis ->
                onUpdateSingleSavingsDate(tx, newMillis)
                editingSavingsTx = null
            },
            onDismiss = { editingSavingsTx = null }
        )
    }

    editingInstallmentPay?.let { pay ->
        SamitiDatePickerDialog(
            initialDateMillis = pay.paymentDate,
            title = "কিস্তি আদায়ের তারিখ আপডেট করুন",
            onDateSelected = { newMillis ->
                onUpdateSingleInstallmentDate(pay, newMillis)
                editingInstallmentPay = null
            },
            onDismiss = { editingInstallmentPay = null }
        )
    }
}
