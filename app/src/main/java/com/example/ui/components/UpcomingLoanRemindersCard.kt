package com.example.ui.components

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Loan
import com.example.notifications.LoanReminderCalculator
import com.example.notifications.LoanReminderNotificationHelper
import com.example.notifications.LoanReminderPreferences
import com.example.notifications.UpcomingWeeklyRepayment
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UpcomingLoanRemindersCard(
    loansList: List<Loan>,
    onOpenLoansTab: (Loan?) -> Unit,
    onFeedbackMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var settings by remember { mutableStateOf(LoanReminderPreferences.getSettings(context)) }
    var hasPermission by remember {
        mutableStateOf(LoanReminderNotificationHelper.hasNotificationPermission(context))
    }
    var permissionDeniedOnce by remember { mutableStateOf(false) }
    var pendingSendAfterPermission by remember { mutableStateOf(false) }

    val upcomingRepayments = remember(loansList, settings.leadDays) {
        LoanReminderCalculator.findUpcomingWeeklyRepayments(
            loans = loansList,
            leadDays = settings.leadDays
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (isGranted) {
            permissionDeniedOnce = false
            LoanReminderNotificationHelper.ensureNotificationChannel(context)
            if (settings.isEnabled) {
                LoanReminderNotificationHelper.scheduleDailyReminder(context)
            }
            if (pendingSendAfterPermission) {
                pendingSendAfterPermission = false
                val sent = LoanReminderNotificationHelper.showWeeklyRepaymentNotification(
                    context = context,
                    upcoming = upcomingRepayments,
                    isTestNotification = true
                )
                if (sent) {
                    LoanReminderPreferences.markNotifiedToday(context)
                    onFeedbackMessage("পরিচালকের ফোনে সাপ্তাহিক কিস্তির রিমাইন্ডার পাঠানো হয়েছে।")
                }
            }
        } else {
            pendingSendAfterPermission = false
            permissionDeniedOnce = true
            onFeedbackMessage("নোটিফিকেশন অনুমতি দেওয়া হয়নি। ফোনের সেটিংস থেকে অনুমতি দিন।")
        }
    }

    val dateSdf = remember { SimpleDateFormat("dd MMM, EEE", Locale.getDefault()) }
    val leadDayOptions = listOf(1, 2, 3, 7)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE6E9EE)),
        border = BorderStroke(1.5.dp, Color(0xFFF59E0B)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("upcoming_loan_reminders_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row with Director reminder title and daily auto-toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFEF3C7))
                            .border(1.dp, Color(0xFFD97706), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (settings.isEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                            contentDescription = "সাপ্তাহিক কিস্তি রিমাইন্ডার",
                            tint = Color(0xFFB45309),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "সাপ্তাহিক কিস্তি রিমাইন্ডার (পরিচালক)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF111827)
                        )
                        Text(
                            text = if (settings.isEnabled) {
                                "প্রতিদিন সকাল ${settings.reminderHour}:00 টায় স্বয়ংক্রিয় সতর্কবার্তা"
                            } else {
                                "স্বয়ংক্রিয় রিমাইন্ডার বন্ধ রয়েছে"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                    }
                }

                Switch(
                    checked = settings.isEnabled,
                    onCheckedChange = { enabled ->
                        val updated = settings.copy(isEnabled = enabled)
                        settings = updated
                        LoanReminderPreferences.saveSettings(context, updated)
                        if (enabled) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasPermission) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            LoanReminderNotificationHelper.scheduleDailyReminder(context)
                            onFeedbackMessage("দৈনিক সাপ্তাহিক ঋণ রিমাইন্ডার চালু করা হয়েছে।")
                        } else {
                            LoanReminderNotificationHelper.cancelDailyReminder(context)
                            onFeedbackMessage("দৈনিক রিমাইন্ডার বন্ধ করা হয়েছে।")
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFFD97706)
                    ),
                    modifier = Modifier.testTag("switch_loan_reminder")
                )
            }

            // Runtime Permission Prompt Banner (Android 13+ or disabled notification channel)
            if (!hasPermission) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFEF2F2),
                    border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (permissionDeniedOnce) {
                                    "নোটিফিকেশন অনুমতি প্রত্যাখ্যাত হয়েছে"
                                } else {
                                    "লোকাল নোটিফিকেশন দেখাতে অনুমতি প্রয়োজন"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF991B1B)
                            )
                            Text(
                                text = "আসন্ন সাপ্তাহিক কিস্তির তারিখ মনে করিয়ে দিতে অনুমতি দিন।",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF7F1D1D)
                            )
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                },
                                border = BorderStroke(1.dp, Color(0xFFDC2626)),
                                modifier = Modifier.testTag("btn_grant_notification_permission")
                            ) {
                                Text(
                                    text = "অনুমতি দিন",
                                    color = Color(0xFF991B1B),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Lead Days Selector Chips
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = Color(0xFFB45309),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "কতদিন আগে রিমাইন্ডার:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                leadDayOptions.forEach { days ->
                    val selected = settings.leadDays == days
                    FilterChip(
                        selected = selected,
                        onClick = {
                            val updated = settings.copy(leadDays = days)
                            settings = updated
                            LoanReminderPreferences.saveSettings(context, updated)
                        },
                        label = {
                            Text(
                                text = "$days দিন আগে",
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color(0xFFDCE1E8),
                            labelColor = Color(0xFF0F172A),
                            selectedContainerColor = Color(0xFFFDE68A),
                            selectedLabelColor = Color(0xFF78350F)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = Color(0xFF64748B),
                            selectedBorderColor = Color(0xFFD97706),
                            borderWidth = 1.2.dp,
                            selectedBorderWidth = 1.5.dp
                        ),
                        modifier = Modifier.testTag("chip_lead_days_$days")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFFDE68A))
            Spacer(modifier = Modifier.height(10.dp))

            // Upcoming Weekly Repayments Preview
            if (upcomingRepayments.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFD1FAE5),
                    border = BorderStroke(1.2.dp, Color(0xFF16A34A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.EventRepeat,
                            contentDescription = null,
                            tint = Color(0xFF065F46),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "আগামী ${settings.leadDays} দিনে কোনো সাপ্তাহিক কিস্তি পরিশোধের তারিখ বাকি নেই।",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF065F46)
                        )
                    }
                }
            } else {
                val totalExpected = upcomingRepayments.sumOf { it.expectedAmount }.toInt()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "আসন্ন/বকেয়া সাপ্তাহিক কিস্তি (${upcomingRepayments.size} জন)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF111827)
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFEF3C7)
                    ) {
                        Text(
                            text = "মোট ৳$totalExpected",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF92400E),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    upcomingRepayments.take(5).forEach { item ->
                        UpcomingRepaymentItemRow(
                            item = item,
                            dateText = dateSdf.format(Date(item.dueDateMillis)),
                            onClick = { onOpenLoansTab(item.loan) }
                        )
                    }
                    if (upcomingRepayments.size > 5) {
                        Text(
                            text = "+ আরও ${upcomingRepayments.size - 5} জন সদস্যের কিস্তি দেখতে ঋণ পাতায় যান",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB45309),
                            modifier = Modifier
                                .clickable { onOpenLoansTab(null) }
                                .padding(vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons: Send Reminder Now & Open Loans
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            !LoanReminderNotificationHelper.hasNotificationPermission(context)
                        ) {
                            pendingSendAfterPermission = true
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            val sent = LoanReminderNotificationHelper.showWeeklyRepaymentNotification(
                                context = context,
                                upcoming = upcomingRepayments,
                                isTestNotification = true
                            )
                            if (sent) {
                                LoanReminderPreferences.markNotifiedToday(context)
                                onFeedbackMessage(
                                    if (upcomingRepayments.isNotEmpty()) {
                                        "${upcomingRepayments.size}টি সাপ্তাহিক কিস্তির নোটিফিকেশন পাঠানো হয়েছে।"
                                    } else {
                                        "রিমাইন্ডার স্ট্যাটাস নোটিফিকেশন পাঠানো হয়েছে।"
                                    }
                                )
                            } else {
                                onFeedbackMessage("নোটিফিকেশন পাঠানো যায়নি। অনুমতি পরীক্ষা করুন।")
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD97706),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_send_reminder_now")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "এখনই রিমাইন্ডার পাঠান",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = { onOpenLoansTab(upcomingRepayments.firstOrNull()?.loan) },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.2.dp, Color(0xFFD97706)),
                    modifier = Modifier.testTag("btn_view_loans_from_reminder")
                ) {
                    Text(
                        text = "ঋণ পাতা",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF92400E)
                    )
                }
            }
        }
    }
}

@Composable
private fun UpcomingRepaymentItemRow(
    item: UpcomingWeeklyRepayment,
    dateText: String,
    onClick: () -> Unit
) {
    val badgeBg = when {
        item.isOverdue -> Color(0xFFFEE2E2)
        item.isDueToday -> Color(0xFFFEF3C7)
        else -> Color(0xFFE0F2FE)
    }
    val badgeTextColor = when {
        item.isOverdue -> Color(0xFFB91C1C)
        item.isDueToday -> Color(0xFFB45309)
        else -> Color(0xFF0369A1)
    }
    val rowBg = if (item.isOverdue) Color(0xFFFFF5F5) else Color(0xFFDCE1E8)
    val rowBorder = if (item.isOverdue) Color(0xFFFECACA) else Color(0xFFFDE68A)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = rowBg,
        border = BorderStroke(1.dp, rowBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${item.loan.memberName} (${item.loan.memberCode})",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${item.nextInstallmentNumber}নং সাপ্তাহিক কিস্তি • $dateText",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "৳${item.expectedAmount.toInt()}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF111827)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeBg
                ) {
                    Text(
                        text = LoanReminderCalculator.formatDueBadgeText(item.daysUntilDue),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = badgeTextColor,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
