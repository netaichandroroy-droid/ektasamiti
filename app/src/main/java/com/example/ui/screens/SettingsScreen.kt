package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.data.model.WeeklyChartAggregator
import com.example.notifications.LoanReminderCalculator
import com.example.notifications.LoanReminderNotificationHelper
import com.example.notifications.LoanReminderPreferences
import com.example.ui.components.MonthlySummaryPreviewDialog
import com.example.ui.components.WeeklyCollectionBarChartCard
import com.example.ui.theme.SilverCardBg
import com.example.ui.theme.SilverCardBorder
import com.example.ui.viewmodel.SamitiViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    currentUser: User,
    viewModel: SamitiViewModel,
    onLogout: () -> Unit = {},
    onOpenLoansTab: () -> Unit = {},
    onShowMessage: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val isAdmin = currentUser.role == UserRole.ADMIN

    val members by viewModel.members.collectAsState()
    val savingsList by viewModel.savingsList.collectAsState()
    val loansList by viewModel.loansList.collectAsState()
    val installmentsList by viewModel.installmentsList.collectAsState()
    val collectors by viewModel.collectorsList.collectAsState()
    val backupData by viewModel.backupData.collectAsState()

    val last4WeeksData = remember(savingsList, installmentsList) {
        WeeklyChartAggregator.computeLast4Weeks(
            savingsList = savingsList,
            installmentsList = installmentsList
        )
    }

    var reminderSettings by remember { mutableStateOf(LoanReminderPreferences.getSettings(context)) }
    var showLeadDaysChooser by remember { mutableStateOf(false) }
    var showWeeklyChart by remember { mutableStateOf(false) }
    var showMonthlyExportDialog by remember { mutableStateOf(false) }
    var showCollectorsList by remember { mutableStateOf(false) }
    var showAddCollectorDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    val upcomingCount = remember(loansList, reminderSettings.leadDays) {
        LoanReminderCalculator.findUpcomingWeeklyRepayments(
            loans = loansList,
            leadDays = reminderSettings.leadDays
        ).size
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 92.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Screen Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "সেটিংস",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "সেটিংস",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "সকল অ্যাপ অপশন তালিকা আকারে দেখুন ও পরিবর্তন করুন",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF334155)
                    )
                }
            }
        }

        // 1. Account Section List
        item {
            SettingsSectionLabel(title = "অ্যাকাউন্ট ও প্রোফাইল")
            Spacer(modifier = Modifier.height(6.dp))
            SettingsListGroupCard(testTag = "card_settings_profile") {
                SettingsOptionRow(
                    icon = Icons.Default.Person,
                    iconBg = Color(0xFFE8F7EE),
                    iconTint = Color(0xFF1B7A49),
                    title = currentUser.fullName,
                    subtitle = currentUser.email,
                    trailingContent = {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isAdmin) Color(0xFFDBEAFE) else Color(0xFFD1FAE5),
                            border = BorderStroke(1.dp, if (isAdmin) Color(0xFF2563EB) else Color(0xFF16A34A))
                        ) {
                            Text(
                                text = if (isAdmin) "অ্যাডমিন" else "কালেক্টর",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isAdmin) Color(0xFF1E40AF) else Color(0xFF065F46),
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                )
                HorizontalDivider(color = SilverCardBorder)
                SettingsOptionRow(
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    iconBg = Color(0xFFFEE2E2),
                    iconTint = Color(0xFFDC2626),
                    title = "লগআউট করুন",
                    subtitle = "বর্তমান অ্যাকাউন্ট থেকে বের হয়ে যান",
                    titleColor = Color(0xFFDC2626),
                    onClick = onLogout,
                    testTag = "btn_settings_logout"
                )
            }
        }

        // 2. Notification & Reminder Settings List (Moved from Home Dashboard)
        item {
            SettingsSectionLabel(title = "নোটিফিকেশন ও কিস্তি রিমাইন্ডার")
            Spacer(modifier = Modifier.height(6.dp))
            SettingsListGroupCard(testTag = "card_settings_reminders") {
                // Loan Reminder Toggle Row
                SettingsOptionRow(
                    icon = Icons.Default.NotificationsActive,
                    iconBg = Color(0xFFFEF3C7),
                    iconTint = Color(0xFFD97706),
                    title = "সাপ্তাহিক কিস্তি রিমাইন্ডার",
                    subtitle = if (reminderSettings.isEnabled) "চালু (প্রতিদিন সকাল ৯:০০ টা)" else "বন্ধ আছে",
                    onClick = {
                        val updated = reminderSettings.copy(isEnabled = !reminderSettings.isEnabled)
                        reminderSettings = updated
                        LoanReminderPreferences.saveSettings(context, updated)
                        if (updated.isEnabled) {
                            LoanReminderNotificationHelper.scheduleDailyReminder(context)
                            onShowMessage("সাপ্তাহিক কিস্তি রিমাইন্ডার চালু করা হয়েছে")
                        } else {
                            LoanReminderNotificationHelper.cancelDailyReminder(context)
                            onShowMessage("সাপ্তাহিক কিস্তি রিমাইন্ডার বন্ধ করা হয়েছে")
                        }
                    },
                    trailingContent = {
                        Switch(
                            checked = reminderSettings.isEnabled,
                            onCheckedChange = { enabled ->
                                val updated = reminderSettings.copy(isEnabled = enabled)
                                reminderSettings = updated
                                LoanReminderPreferences.saveSettings(context, updated)
                                if (enabled) {
                                    LoanReminderNotificationHelper.scheduleDailyReminder(context)
                                    onShowMessage("সাপ্তাহিক কিস্তি রিমাইন্ডার চালু করা হয়েছে")
                                } else {
                                    LoanReminderNotificationHelper.cancelDailyReminder(context)
                                    onShowMessage("সাপ্তাহিক কিস্তি রিমাইন্ডার বন্ধ করা হয়েছে")
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF0D683B)
                            ),
                            modifier = Modifier.testTag("switch_settings_reminders")
                        )
                    }
                )

                HorizontalDivider(color = SilverCardBorder)

                // Reminder Lead Days Row
                SettingsOptionRow(
                    icon = Icons.Default.Schedule,
                    iconBg = Color(0xFFE0F2FE),
                    iconTint = Color(0xFF0369A1),
                    title = "কতদিন আগে রিমাইন্ডার দেখাবে",
                    subtitle = "${reminderSettings.leadDays} দিন আগে সতর্কবার্তা",
                    onClick = { showLeadDaysChooser = !showLeadDaysChooser },
                    trailingContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "নির্বাচন করুন",
                            tint = Color(0xFF475569)
                        )
                    },
                    testTag = "row_settings_lead_days"
                )

                AnimatedVisibility(visible = showLeadDaysChooser) {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(1, 2, 3, 7).forEach { days ->
                            val selected = reminderSettings.leadDays == days
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    val updated = reminderSettings.copy(leadDays = days)
                                    reminderSettings = updated
                                    LoanReminderPreferences.saveSettings(context, updated)
                                    onShowMessage("$days দিন আগে রিমাইন্ডার সেট করা হয়েছে")
                                },
                                label = {
                                    Text(
                                        text = "$days দিন আগে",
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFFEF3C7),
                                    selectedLabelColor = Color(0xFF92400E)
                                )
                            )
                        }
                    }
                }

                HorizontalDivider(color = SilverCardBorder)

                // Test Notification Row
                SettingsOptionRow(
                    icon = Icons.Default.NotificationsNone,
                    iconBg = Color(0xFFF3E8FF),
                    iconTint = Color(0xFF7C3AED),
                    title = "এখনই রিমাইন্ডার পাঠান",
                    subtitle = if (upcomingCount > 0) "আগামী ${reminderSettings.leadDays} দিনে $upcomingCount টি কিস্তি রয়েছে" else "রিমাইন্ডার নোটিফিকেশন যাচাই করুন",
                    onClick = {
                        val upcoming = LoanReminderCalculator.findUpcomingWeeklyRepayments(
                            loans = loansList,
                            leadDays = reminderSettings.leadDays
                        )
                        val shown = LoanReminderNotificationHelper.showWeeklyRepaymentNotification(
                            context = context,
                            upcoming = upcoming,
                            isTestNotification = true
                        )
                        if (shown) {
                            onShowMessage("রিমাইন্ডার নোটিফিকেশন পাঠানো হয়েছে")
                        } else {
                            onShowMessage("নোটিফিকেশন দেখাতে অনুমতির প্রয়োজন হতে পারে")
                        }
                    },
                    trailingContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "পাঠান",
                            tint = Color(0xFF475569)
                        )
                    },
                    testTag = "row_settings_test_notification"
                )

                HorizontalDivider(color = SilverCardBorder)

                // Direct shortcut to Loans tab
                SettingsOptionRow(
                    icon = Icons.Default.CreditCard,
                    iconBg = Color(0xFFE8F7EE),
                    iconTint = Color(0xFF0D683B),
                    title = "ঋণ ও কিস্তি পাতায় যান",
                    subtitle = "চলমান ঋণ ও পরিশোধের সময়সূচী দেখুন",
                    onClick = onOpenLoansTab,
                    trailingContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "ঋণ পাতা",
                            tint = Color(0xFF475569)
                        )
                    },
                    testTag = "row_settings_open_loans"
                )
            }
        }

        // 3. Reports, Chart & Export Settings List
        item {
            SettingsSectionLabel(title = "রিপোর্ট, চার্ট ও এক্সপোর্ট")
            Spacer(modifier = Modifier.height(6.dp))
            SettingsListGroupCard(testTag = "card_settings_reports") {
                SettingsOptionRow(
                    icon = Icons.Default.BarChart,
                    iconBg = Color(0xFFE8F7EE),
                    iconTint = Color(0xFF0D683B),
                    title = "গত ৪ সপ্তাহের সঞ্চয় ও ঋণ আদায় চার্ট",
                    subtitle = "সাপ্তাহিক তুলনামূলক বার চার্ট দেখুন",
                    onClick = { showWeeklyChart = !showWeeklyChart },
                    trailingContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "চার্ট দেখুন",
                            tint = Color(0xFF475569)
                        )
                    },
                    testTag = "row_settings_weekly_chart"
                )

                AnimatedVisibility(visible = showWeeklyChart) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 10.dp, end = 10.dp, bottom = 12.dp)
                    ) {
                        WeeklyCollectionBarChartCard(buckets = last4WeeksData)
                    }
                }

                HorizontalDivider(color = SilverCardBorder)

                SettingsOptionRow(
                    icon = Icons.Default.Summarize,
                    iconBg = Color(0xFFDBEAFE),
                    iconTint = Color(0xFF1D4ED8),
                    title = "মাসিক আর্থিক সারসংক্ষেপ এক্সপোর্ট",
                    subtitle = "মেসেজিং অ্যাপে টেক্সট শেয়ার বা ছবি ডাউনলোড",
                    onClick = { showMonthlyExportDialog = true },
                    trailingContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "এক্সপোর্ট",
                            tint = Color(0xFF475569)
                        )
                    },
                    testTag = "btn_settings_monthly_export"
                )
            }
        }

        // 4. Data & Backup Settings List
        item {
            SettingsSectionLabel(title = "ডাটাবেজ ও ব্যাকআপ")
            Spacer(modifier = Modifier.height(6.dp))
            SettingsListGroupCard(testTag = "card_settings_backup") {
                SettingsOptionRow(
                    icon = Icons.Default.Storage,
                    iconBg = Color(0xFFE8F7EE),
                    iconTint = Color(0xFF0D683B),
                    title = "১০ জিবি স্থায়ী ডাটাবেজ ধারণক্ষমতা",
                    subtitle = "ডাটা নিরাপদ ও স্থায়ীভাবে সংরক্ষিত থাকবে"
                )
                HorizontalDivider(color = SilverCardBorder)
                SettingsOptionRow(
                    icon = Icons.Default.Download,
                    iconBg = Color(0xFFDBEAFE),
                    iconTint = Color(0xFF1D4ED8),
                    title = "সম্পূর্ণ ডাটা ব্যাকআপ এক্সপোর্ট",
                    subtitle = "সদস্য, সঞ্চয় ও ঋণের সম্পূর্ণ ব্যাকআপ নিন",
                    onClick = { viewModel.generateBackup() },
                    trailingContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "ব্যাকআপ নিন",
                            tint = Color(0xFF475569)
                        )
                    },
                    testTag = "btn_settings_backup"
                )
            }
        }

        // 4. Admin & Collector Management List
        item {
            SettingsSectionLabel(title = "মাঠকর্মী ও অ্যাডমিন ব্যবস্থাপনা")
            Spacer(modifier = Modifier.height(6.dp))
            SettingsListGroupCard(testTag = "card_settings_admin") {
                if (!isAdmin) {
                    SettingsOptionRow(
                        icon = Icons.Default.Lock,
                        iconBg = Color(0xFFF1F5F9),
                        iconTint = Color(0xFF475569),
                        title = "কালেক্টর অ্যাক্সেস",
                        subtitle = "নতুন কালেক্টর যোগ বা ডাটা রিসেট শুধুমাত্র অ্যাডমিন করতে পারবেন"
                    )
                } else {
                    SettingsOptionRow(
                        icon = Icons.Default.SupervisorAccount,
                        iconBg = Color(0xFFDBEAFE),
                        iconTint = Color(0xFF1D4ED8),
                        title = "মাঠকর্মী ও সংগ্রাহক তালিকা",
                        subtitle = "নিবন্ধিত কালেক্টর: ${collectors.size} জন",
                        onClick = { showCollectorsList = !showCollectorsList },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "তালিকা দেখুন",
                                tint = Color(0xFF475569)
                            )
                        },
                        testTag = "row_settings_collectors_list"
                    )

                    AnimatedVisibility(visible = showCollectorsList) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (collectors.isEmpty()) {
                                Text(
                                    text = "এখনো কোনো কালেক্টর নিবন্ধিত নেই",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF475569)
                                )
                            } else {
                                collectors.forEach { collector ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color.White.copy(alpha = 0.78f),
                                        border = BorderStroke(1.dp, SilverCardBorder),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = collector.fullName,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF0F172A),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${collector.email} | ${collector.mobile}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color(0xFF475569),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color(0xFFE8F7EE),
                                                border = BorderStroke(1.dp, Color(0xFF1B7A49).copy(alpha = 0.4f))
                                            ) {
                                                Text(
                                                    text = "সক্রিয়",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF0D683B),
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = SilverCardBorder)

                    SettingsOptionRow(
                        icon = Icons.Default.PersonAdd,
                        iconBg = Color(0xFFE8F7EE),
                        iconTint = Color(0xFF0D683B),
                        title = "নতুন কালেক্টর যোগ করুন",
                        subtitle = "নতুন মাঠকর্মীর লগইন তৈরি করুন",
                        onClick = { showAddCollectorDialog = true },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "যোগ করুন",
                                tint = Color(0xFF475569)
                            )
                        },
                        testTag = "btn_settings_add_collector"
                    )

                    HorizontalDivider(color = SilverCardBorder)

                    SettingsOptionRow(
                        icon = Icons.Default.DeleteForever,
                        iconBg = Color(0xFFFEE2E2),
                        iconTint = Color(0xFFDC2626),
                        title = "সকল ডাটা মুছে ফেলুন",
                        subtitle = "সদস্য, সঞ্চয় ও ঋণের সব তথ্য রিসেট করুন",
                        titleColor = Color(0xFFDC2626),
                        onClick = { showResetConfirmDialog = true },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "রিসেট করুন",
                                tint = Color(0xFFDC2626)
                            )
                        },
                        testTag = "btn_settings_reset_data"
                    )
                }
            }
        }

        // 5. About App Section List
        item {
            SettingsSectionLabel(title = "অ্যাপ পরিচিতি")
            Spacer(modifier = Modifier.height(6.dp))
            SettingsListGroupCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_ekta_samiti_logo_1794238685688),
                        contentDescription = "একতা সমিতি লোগো",
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "একতা সঞ্চয় ও ঋণ সমিতি",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "সংস্করণ ১.০ • সাপ্তাহিক সঞ্চয় ও ঋণ ব্যবস্থাপনা",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF475569)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFF64748B)
                    )
                }
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

    if (showAddCollectorDialog) {
        AddCollectorDialog(
            onDismiss = { showAddCollectorDialog = false },
            onSubmit = { name, email, pass, phone ->
                viewModel.addCollector(name, email, pass, phone) {
                    showAddCollectorDialog = false
                }
            }
        )
    }

    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("সকল ডাটা মুছে ফেলার নিশ্চয়তা") },
            text = {
                Text("আপনি কি নিশ্চিত যে সকল সদস্য, ঋণ, সঞ্চয় ও কিস্তি আদায়ের ডাটা মুছে ফেলতে চান? এডমিন ও কালেক্টর লগইন অ্যাকাউন্ট অপরিবর্তিত থাকবে।")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllSamitiData(currentUser.role) {
                            showResetConfirmDialog = false
                        }
                    }
                ) {
                    Text(
                        text = "হ্যাঁ, সব মুছুন",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("বাতিল", maxLines = 1, softWrap = false)
                }
            }
        )
    }

    backupData?.let { data ->
        Dialog(
            onDismissRequest = { viewModel.clearBackupData() },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ডাটা ব্যাকআপ তৈরি হয়েছে",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .verticalScroll(rememberScrollState())
                            .padding(10.dp)
                    ) {
                        Text(text = data, style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.clearBackupData() },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                            modifier = Modifier.weight(0.9f)
                        ) {
                            Text(
                                text = "বন্ধ করুন",
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Button(
                            onClick = {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, data)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "ব্যাকআপ ফাইল শেয়ার করুন"))
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                            modifier = Modifier.weight(1.1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "শেয়ার / সেভ",
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddCollectorDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Text(
                    text = "নতুন কালেক্টর / মাঠকর্মী নিয়োগ",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "মাঠকর্মীর লগইন তথ্য ও মোবাইল নম্বর দিন",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; error = null },
                    label = { Text("মাঠকর্মীর পুরো নাম *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; error = null },
                    label = { Text("ইমেইল (লগইন আইডি) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; error = null },
                    label = { Text("পাসওয়ার্ড *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = mobile,
                    onValueChange = { mobile = it },
                    label = { Text("মোবাইল নম্বর") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

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
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Button(
                        onClick = {
                            if (name.isBlank() || email.isBlank() || password.isBlank()) {
                                error = "সকল আবশ্যক তথ্য পূরণ করুন"
                                return@Button
                            }
                            onSubmit(name, email, password, mobile)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        modifier = Modifier
                            .weight(1.15f)
                            .testTag("btn_submit_add_collector")
                    ) {
                        Text(
                            text = "নিবন্ধন করুন",
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionLabel(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.ExtraBold,
        color = Color(0xFF0F172A),
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

@Composable
private fun SettingsListGroupCard(
    testTag: String? = null,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SilverCardBg),
        border = BorderStroke(1.2.dp, SilverCardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

@Composable
private fun SettingsOptionRow(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String? = null,
    titleColor: Color = Color(0xFF0F172A),
    subtitleColor: Color = Color(0xFF334155),
    onClick: (() -> Unit)? = null,
    testTag: String? = null,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(21.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = titleColor
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = subtitleColor
                )
            }
        }

        if (trailingContent != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailingContent()
        }
    }
}
