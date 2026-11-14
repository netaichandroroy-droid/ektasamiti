package com.example.ui.screens

import android.content.Intent
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.cloud.CloudSyncState
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.ui.components.CloudAutoSyncBanner
import com.example.ui.components.MonthlySummaryExportCard
import com.example.ui.viewmodel.SamitiViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReportsAndAdminScreen(
    currentUser: User,
    viewModel: SamitiViewModel,
    onShowMessage: (String) -> Unit = {},
    cloudSyncState: CloudSyncState = CloudSyncState(),
    onSyncNow: () -> Unit = {},
    onGoogleSignInSuccess: () -> Unit = {}
) {
    val context = LocalContext.current
    val isAdmin = currentUser.role == UserRole.ADMIN

    val members by viewModel.members.collectAsState()
    val savingsList by viewModel.savingsList.collectAsState()
    val loansList by viewModel.loansList.collectAsState()
    val activeLoans by viewModel.activeLoans.collectAsState()
    val installmentsList by viewModel.installmentsList.collectAsState()
    val collectors by viewModel.collectorsList.collectAsState()
    val backupData by viewModel.backupData.collectAsState()

    var showAddCollectorDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

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
        // Cloud Auto-Synchronization Status Card
        item {
            CloudAutoSyncBanner(
                syncState = cloudSyncState,
                onSyncNow = onSyncNow,
                onGoogleSignInSuccess = onGoogleSignInSuccess,
                onFeedbackMessage = onShowMessage
            )
        }

        // High Capacity Data Safety Guarantee Banner
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE6E9EE)),
                border = androidx.compose.foundation.BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.5.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE8F7EE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            tint = Color(0xFF1B7A49),
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "১০ জিবি স্থায়ী ডাটাবেজ ধারণক্ষমতা",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0D683B)
                        )
                        Text(
                            text = "ডাটা কোন দিনও নিজে থেকে ডিলিট হবে না। শুধুমাত্র অ্যাডমিন পূর্ণ নিরাপত্তা কোড দিয়ে ডাটা মুছে ফেলতে বা ব্যাকআপ নিতে পারেন।",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF1E293B)
                        )
                    }
                }
            }
        }

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
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE6E9EE)),
                border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFB3BCC8)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.5.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ReportRow(label = "আজকের সঞ্চয় জমা:", value = "৳ ${todaySavings.toInt()}", color = Color(0xFF0D683B))
                    ReportRow(label = "আজকের কিস্তি আদায়:", value = "৳ ${todayInstallments.toInt()}", color = Color(0xFFB45309))
                    ReportRow(label = "আজকের সর্বমোট আদায়:", value = "৳ ${(todaySavings + todayInstallments).toInt()}", isBold = true)

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = Color(0xFFB3BCC8))
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

        // Backup & Export Button
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE6E9EE)),
                border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFB3BCC8)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.5.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "সম্পূর্ণ ডাটা ব্যাকআপ এক্সপোর্ট",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "সকল সদস্যের তথ্য, সঞ্চয় ও ঋণের লেজার টেক্সট বা ফাইল আকারে সেভ করে রাখুন।",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1E293B)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.generateBackup() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth().testTag("btn_export_backup")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ডাটা ব্যাকআপ তৈরি ও ডাউনলোড করুন")
                    }
                }
            }
        }

        // Collector & User Management (Admin Only)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "মাঠকর্মী ও সংগ্রাহক ব্যবস্থাপনা",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (isAdmin) {
                    OutlinedButton(
                        onClick = { showAddCollectorDialog = true },
                        modifier = Modifier.testTag("btn_open_add_collector")
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("নতুন কালেক্টর", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        if (!isAdmin) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "কালেক্টর রোল: আপনি সীমিত অ্যাক্সেসে আছেন। নতুন কালেক্টর যোগ বা ডাটা ডিলিট করার অধিকার শুধুমাত্র প্রধান অ্যাডমিনের রয়েছে।",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            // Show Collector List
            if (collectors.isEmpty()) {
                item {
                    Text(
                        text = "কোন মাঠকর্মী নিবন্ধিত নেই",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF334155)
                    )
                }
            } else {
                items(collectors) { collector ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE6E9EE)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFB3BCC8)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.secondaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SupervisorAccount,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = collector.fullName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "${collector.email} | ${collector.mobile}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF334155)
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFE8F7EE),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1B7A49).copy(alpha = 0.35f))
                            ) {
                                Text(
                                    text = "সক্রিয়",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF1B7A49),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (isAdmin) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF0F0)),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFD32F2F).copy(alpha = 0.5f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.5.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "সকল ডাটা মুছে ফেলা (শুধুমাত্র এডমিন)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "অটো কোন সদস্য বা কালেকশন ডাটা থাকবে না। প্রয়োজন হলে সকল ডাটা এক ক্লিকে মুছে নতুন করে শুরু করতে পারেন। শুধুমাত্র এডমিন যা এন্ট্রি করবেন সেগুলোই সংরক্ষিত থাকবে।",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { showResetConfirmDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth().testTag("btn_reset_all_data")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("সকল সদস্য ও কালেকশন ডাটা রিসেট করুন", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Add Collector Dialog (Admin only)
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

    // Reset Data Confirmation Dialog (Admin Only)
    if (showResetConfirmDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("সকল ডাটা মুছে ফেলার নিশ্চয়তা") },
            text = {
                Text("আপনি কি নিশ্চিত যে সকল সদস্য, ঋণ, সঞ্চয় ও কিস্তি আদায়ের ডাটা মুছে ফেলতে চান? শুধুমাত্র এডমিন যা ডাটা এন্ট্রি করবেন সেগুলোই থাকবে। এডমিন ও কালেক্টর লগইন অ্যাকাউন্ট অপরিবর্তিত থাকবে।")
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        viewModel.clearAllSamitiData(currentUser.role) {
                            showResetConfirmDialog = false
                        }
                    }
                ) {
                    Text("হ্যাঁ, সব মুছে ফেলুন", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }

    // Backup Data Viewer / Share Dialog
    backupData?.let { data ->
        Dialog(onDismissRequest = { viewModel.clearBackupData() }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "ডাটা ব্যাকআপ তৈরি হয়েছে",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
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
                        Button(
                            onClick = {
                                val sendIntent: Intent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, data)
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "ব্যাকআপ ফাইল শেয়ার করুন")
                                context.startActivity(shareIntent)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("শেয়ার / সেভ")
                        }

                        OutlinedButton(
                            onClick = { viewModel.clearBackupData() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("বন্ধ করুন")
                        }
                    }
                }
            }
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

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(4.dp)
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
                    Text(text = error ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("বাতিল")
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
                        modifier = Modifier.weight(1f).testTag("btn_submit_add_collector")
                    ) {
                        Text("নিবন্ধন করুন")
                    }
                }
            }
        }
    }
}
