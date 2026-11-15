package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Member
import com.example.data.model.SavingsTransaction
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.ui.components.DatePickerField
import com.example.ui.components.ReceiptDialog
import com.example.ui.components.SamitiDatePickerDialog
import com.example.ui.viewmodel.ReceiptData
import com.example.ui.viewmodel.SamitiViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SavingsScreen(
    currentUser: User,
    viewModel: SamitiViewModel,
    preselectedMemberForDeposit: Member? = null,
    showDepositDialogInitially: Boolean = false
) {
    val members by viewModel.members.collectAsState()
    val savingsList by viewModel.savingsList.collectAsState()
    val todaySavings = remember(savingsList) { viewModel.getTodaySavingsTotal() }
    val totalSavingsFund = remember(members) { members.sumOf { it.savingsBalance } }

    var showDepositDialog by remember { mutableStateOf(showDepositDialogInitially || preselectedMemberForDeposit != null) }
    var showWithdrawDialog by remember { mutableStateOf(false) }
    var selectedMemberForAction by remember { mutableStateOf(preselectedMemberForDeposit) }

    var selectedTransactionForDelete by remember { mutableStateOf<SavingsTransaction?>(null) }
    var transactionToEditDate by remember { mutableStateOf<SavingsTransaction?>(null) }

    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("savings_screen")
    ) {
        // Top stats bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(listOf(Color(0xFF148549), Color(0xFF1FB364)))
                                )
                                .padding(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Savings,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.22f),
                                modifier = Modifier.align(Alignment.BottomEnd).size(42.dp)
                            )
                            Column {
                                Text(
                                    text = "আজকের সঞ্চয় জমা",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.94f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "৳ ${todaySavings.toInt()}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(listOf(Color(0xFF008488), Color(0xFF00A8A8)))
                                )
                                .padding(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Savings,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.22f),
                                modifier = Modifier.align(Alignment.BottomEnd).size(42.dp)
                            )
                            Column {
                                Text(
                                    text = "মোট সঞ্চয় তহবিল",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.94f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "৳ ${totalSavingsFund.toInt()}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Fast Action Buttons: Deposit & Withdrawal
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            selectedMemberForAction = null
                            showDepositDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        modifier = Modifier.weight(1.15f).testTag("btn_open_deposit")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "সঞ্চয় জমা নিন",
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            selectedMemberForAction = null
                            showWithdrawDialog = true
                        },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        modifier = Modifier.weight(0.85f).testTag("btn_open_withdraw")
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "উত্তোলন",
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // Transactions list
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "সকল সঞ্চয় লেনদেন বিবরণী",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${savingsList.size} টি লেনদেন",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }

        if (savingsList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "কোন সঞ্চয় লেনদেনের তথ্য নেই",
                    color = MaterialTheme.colorScheme.outline,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(savingsList) { item ->
                    SavingsTransactionCard(
                        transaction = item,
                        currentUserRole = currentUser.role,
                        onCardClick = {
                            viewModel.showReceipt(
                                ReceiptData(
                                    title = if (item.type == "DEPOSIT") "সঞ্চয় জমা মানি রসিদ" else "সঞ্চয় উত্তোলন রসিদ",
                                    receiptNumber = item.receiptNumber,
                                    memberCode = item.memberCode,
                                    memberName = item.memberName,
                                    transactionType = if (item.type == "DEPOSIT") "জমা (${item.savingsPlan})" else "উত্তোলন",
                                    amount = item.amount,
                                    balanceOrDue = item.balanceAfter,
                                    collectedBy = item.collectedBy,
                                    timestamp = item.date,
                                    notes = item.note
                                )
                            )
                        },
                        onEditDateClick = {
                            transactionToEditDate = item
                        },
                        onDeleteClick = {
                            selectedTransactionForDelete = item
                        }
                    )
                }
            }
        }
    }

    // Deposit Dialog
    if (showDepositDialog) {
        SavingsDepositDialog(
            members = members,
            preselectedMember = selectedMemberForAction,
            currentUser = currentUser,
            onDismiss = { showDepositDialog = false },
            onSubmit = { member, amount, plan, note, dateMillis ->
                viewModel.depositSavings(
                    member = member,
                    amount = amount,
                    savingsPlan = plan,
                    note = note,
                    collectedBy = currentUser.fullName,
                    role = currentUser.role,
                    customDate = dateMillis,
                    onComplete = { showDepositDialog = false }
                )
            }
        )
    }

    // Withdrawal Dialog
    if (showWithdrawDialog) {
        SavingsWithdrawDialog(
            members = members,
            preselectedMember = selectedMemberForAction,
            currentUser = currentUser,
            onDismiss = { showWithdrawDialog = false },
            onSubmit = { member, amount, note, dateMillis ->
                viewModel.withdrawSavings(
                    member = member,
                    amount = amount,
                    note = note,
                    collectedBy = currentUser.fullName,
                    role = currentUser.role,
                    customDate = dateMillis,
                    onComplete = { showWithdrawDialog = false }
                )
            }
        )
    }

    // Edit transaction date dialog
    transactionToEditDate?.let { tx ->
        SamitiDatePickerDialog(
            initialDateMillis = tx.date,
            title = "সঞ্চয় লেনদেনের তারিখ পরিবর্তন করুন",
            onDateSelected = { newMillis ->
                viewModel.updateSavingsTransactionDate(tx, newMillis)
                transactionToEditDate = null
            },
            onDismiss = { transactionToEditDate = null }
        )
    }

    // Delete confirmation dialog (Admin only)
    selectedTransactionForDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { selectedTransactionForDelete = null },
            title = { Text("লেনদেন ডিলিট নিশ্চিতকরণ") },
            text = { Text("রসিদ নং ${item.receiptNumber} (${item.memberName}) এর লেনদেন রেকর্ডটি কি মুছে ফেলতে চান? শুধুমাত্র অ্যাডমিন এটি করতে পারেন।") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSavingsTransaction(item, currentUser.role)
                        selectedTransactionForDelete = null
                    }
                ) {
                    Text("হ্যাঁ, ডিলিট করুন", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedTransactionForDelete = null }) {
                    Text("বাতিল")
                }
            }
        )
    }
}

@Composable
fun SavingsTransactionCard(
    transaction: SavingsTransaction,
    currentUserRole: UserRole,
    onCardClick: () -> Unit,
    onEditDateClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val isDeposit = transaction.type == "DEPOSIT"
    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    val cardBg = if (isDeposit) Color(0xFFD1FAE5) else Color(0xFFFEE2E2)
    val cardBorder = if (isDeposit) Color(0xFF16A34A) else Color(0xFFDC2626)

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, cardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() }
            .testTag("savings_item_${transaction.receiptNumber}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.5.dp, cardBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isDeposit) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                        contentDescription = null,
                        tint = if (isDeposit) Color(0xFF065F46) else Color(0xFF991B1B),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = transaction.memberName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "${transaction.memberCode} | রসিদ: ${transaction.receiptNumber}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = "${sdf.format(Date(transaction.date))} | সংগ্রাহক: ${transaction.collectedBy}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF334155)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${if (isDeposit) "+" else "-"}৳${transaction.amount.toInt()}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDeposit) Color(0xFF065F46) else Color(0xFF991B1B)
                    )
                    Text(
                        text = "স্থিতি: ৳${transaction.balanceAfter.toInt()}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                }

                IconButton(
                    onClick = onEditDateClick,
                    modifier = Modifier.testTag("btn_edit_savings_date_${transaction.receiptNumber}")
                ) {
                    Icon(
                        imageVector = Icons.Default.EditCalendar,
                        contentDescription = "তারিখ পরিবর্তন করুন",
                        tint = Color(0xFF1D4ED8),
                        modifier = Modifier.size(19.dp)
                    )
                }

                if (currentUserRole == UserRole.ADMIN) {
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsDepositDialog(
    members: List<Member>,
    preselectedMember: Member?,
    currentUser: User,
    onDismiss: () -> Unit,
    onSubmit: (Member, Double, String, String, Long) -> Unit
) {
    var selectedMember by remember { mutableStateOf(preselectedMember ?: members.firstOrNull()) }
    var expanded by remember { mutableStateOf(false) }
    var amountText by remember { mutableStateOf("200") }
    val savingsPlan = "WEEKLY"
    var note by remember { mutableStateOf("") }
    var depositDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var error by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Text(
                    text = "সাপ্তাহিক সঞ্চয় জমা গ্রহণ",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "সদস্য নির্বাচন করে জমার পরিমাণ দিন",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Member Dropdown selector
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedMember?.let { "${it.name} (${it.memberCode})" } ?: "সদস্য নির্বাচন করুন",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("সদস্য") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        members.forEach { m ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("${m.name} (${m.memberCode})", fontWeight = FontWeight.SemiBold)
                                        Text("বর্তমান সঞ্চয়: ৳${m.savingsBalance.toInt()}", style = MaterialTheme.typography.labelSmall)
                                    }
                                },
                                onClick = {
                                    selectedMember = m
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                selectedMember?.let { m ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "বর্তমান সঞ্চয় স্থিতি: ৳${m.savingsBalance.toInt()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Amount
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it; error = null },
                    label = { Text("জমার পরিমাণ (টাকা) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_deposit_amount")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Plan - Weekly
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Savings, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "সঞ্চয়ের ধরণ: সাপ্তাহিক সঞ্চয়",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                DatePickerField(
                    label = "জমার তারিখ",
                    selectedDateMillis = depositDateMillis,
                    onDateSelected = { depositDateMillis = it },
                    testTag = "btn_pick_deposit_date"
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Note
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("মন্তব্য (ঐচ্ছিক)") },
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
                    OutlinedButton(
                        onClick = onDismiss,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        modifier = Modifier.weight(0.8f)
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
                            val member = selectedMember
                            val amount = amountText.toDoubleOrNull()
                            if (member == null) {
                                error = "সদস্য নির্বাচন করুন"
                                return@Button
                            }
                            if (amount == null || amount <= 0) {
                                error = "সঠিক পরিমাণ লিখুন"
                                return@Button
                            }
                            onSubmit(member, amount, savingsPlan, note, depositDateMillis)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        modifier = Modifier.weight(1.2f).testTag("btn_confirm_deposit")
                    ) {
                        Text(
                            text = "জমা নিশ্চিত",
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsWithdrawDialog(
    members: List<Member>,
    preselectedMember: Member?,
    currentUser: User,
    onDismiss: () -> Unit,
    onSubmit: (Member, Double, String, Long) -> Unit
) {
    var selectedMember by remember { mutableStateOf(preselectedMember ?: members.firstOrNull()) }
    var expanded by remember { mutableStateOf(false) }
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var withdrawDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var error by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Text(
                    text = "সঞ্চয় উত্তোলন ফরম",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "সদস্যের সঞ্চয় স্থিতি থেকে উত্তোলন করা হবে",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(modifier = Modifier.height(14.dp))

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedMember?.let { "${it.name} (${it.memberCode})" } ?: "সদস্য নির্বাচন করুন",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("সদস্য") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        members.forEach { m ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("${m.name} (${m.memberCode})", fontWeight = FontWeight.SemiBold)
                                        Text("বর্তমান সঞ্চয়: ৳${m.savingsBalance.toInt()}", style = MaterialTheme.typography.labelSmall)
                                    }
                                },
                                onClick = {
                                    selectedMember = m
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                selectedMember?.let { m ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "সর্বোচ্চ উত্তোলনযোগ্য: ৳${m.savingsBalance.toInt()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it; error = null },
                    label = { Text("উত্তোলনের পরিমাণ (টাকা) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_withdraw_amount")
                )

                Spacer(modifier = Modifier.height(10.dp))

                DatePickerField(
                    label = "উত্তোলনের তারিখ",
                    selectedDateMillis = withdrawDateMillis,
                    onDateSelected = { withdrawDateMillis = it },
                    testTag = "btn_pick_withdraw_date",
                    accentColor = MaterialTheme.colorScheme.error
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("উত্তোলনের কারণ / মন্তব্য") },
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
                    OutlinedButton(
                        onClick = onDismiss,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        modifier = Modifier.weight(0.8f)
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
                            val member = selectedMember
                            val amount = amountText.toDoubleOrNull()
                            if (member == null) {
                                error = "সদস্য নির্বাচন করুন"
                                return@Button
                            }
                            if (amount == null || amount <= 0) {
                                error = "সঠিক পরিমাণ লিখুন"
                                return@Button
                            }
                            if (amount > member.savingsBalance) {
                                error = "অপর্যাপ্ত ব্যালেন্স! সঞ্চয় আছে মাত্র ৳${member.savingsBalance.toInt()}"
                                return@Button
                            }
                            onSubmit(member, amount, note, withdrawDateMillis)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        modifier = Modifier.weight(1.2f).testTag("btn_confirm_withdraw")
                    ) {
                        Text(
                            text = "উত্তোলন নিশ্চিত",
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
