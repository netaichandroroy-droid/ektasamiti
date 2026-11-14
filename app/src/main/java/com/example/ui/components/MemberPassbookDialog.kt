package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.Member
import com.example.data.model.UserRole
import com.example.ui.viewmodel.SamitiViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MemberPassbookDialog(
    member: Member,
    currentUserRole: UserRole,
    viewModel: SamitiViewModel,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val allSavings by viewModel.savingsList.collectAsState()
    val memberSavings = remember(allSavings, member.id) {
        allSavings.filter { it.memberId == member.id }
    }

    val allLoans by viewModel.loansList.collectAsState()
    val memberLoans = remember(allLoans, member.id) {
        allLoans.filter { it.memberId == member.id }
    }

    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .testTag("member_passbook_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Top header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "সদস্য পাসবুক ও হিসাব বই",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Member info card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!member.photoUri.isNullOrEmpty() && member.photoUri.startsWith("content://")) {
                            AsyncImage(
                                model = member.photoUri,
                                contentDescription = member.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(56.dp).clip(CircleShape)
                            )
                        } else {
                            Icon(
                                imageVector = if (member.photoUri == "avatar_1" || member.photoUri == "avatar_4") Icons.Default.Face else Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = member.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF111827)
                        )
                        Text(
                            text = "হিসাব নং: ${member.memberCode} (সাপ্তাহিক সঞ্চয়ী)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "মোবাইল: ${member.mobile}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937)
                        )
                        if (member.address.isNotBlank()) {
                            Text(
                                text = "ঠিকানা: ${member.address}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF374151)
                            )
                        }
                        if (member.fatherOrHusbandName.isNotBlank()) {
                            Text(
                                text = "পিতা/স্বামী: ${member.fatherOrHusbandName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF4B5563)
                            )
                        }
                        if (member.nid.isNotBlank()) {
                            Text(
                                text = "এনআইডি: ${member.nid}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF4B5563)
                            )
                        }
                        if (member.nomineeName.isNotBlank()) {
                            val rel = if (member.nomineeRelation.isNotBlank()) " (${member.nomineeRelation})" else ""
                            Text(
                                text = "নমিনী: ${member.nomineeName}$rel",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF4B5563)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Savings and Loan quick cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("মোট সঞ্চয় ব্যালেন্স", style = MaterialTheme.typography.labelSmall)
                            Text(
                                "৳ ${member.savingsBalance.toInt()}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("মোট বকেয়া ঋণ", style = MaterialTheme.typography.labelSmall)
                            Text(
                                "৳ ${member.totalLoanDue.toInt()}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tabs: Savings vs Loans
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("সঞ্চয় বিবরণী (${memberSavings.size})") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("ঋণ বিবরণী (${memberLoans.size})") }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // List content
                Box(modifier = Modifier.weight(1f)) {
                    if (selectedTab == 0) {
                        if (memberSavings.isEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("কোন সঞ্চয় লেনদেন নেই", color = MaterialTheme.colorScheme.outline)
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(memberSavings) { s ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = if (s.type == "DEPOSIT") "সঞ্চয় জমা" else "সঞ্চয় উত্তোলন",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (s.type == "DEPOSIT") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                            )
                                            Text(
                                                text = "${sdf.format(Date(s.date))} | ${s.receiptNumber}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "${if (s.type == "DEPOSIT") "+" else "-"}৳${s.amount.toInt()}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (s.type == "DEPOSIT") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                            )
                                            Text(
                                                text = "ব্যালেন্স: ৳${s.balanceAfter.toInt()}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        if (memberLoans.isEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("এই সদস্যের কোন ঋণ নেই", color = MaterialTheme.colorScheme.outline)
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(memberLoans) { l ->
                                    Card(
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        val effectiveRate = com.example.data.model.LoanScheduleCalculator.effectiveInterestRate(l)
                                        val rateText = com.example.data.model.LoanScheduleCalculator.formatInterestRate(effectiveRate)
                                        val freqLabel = if (l.frequency == "MONTHLY") "মাসিক" else "সাপ্তাহিক"
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "ঋণ: ৳${l.principalAmount.toInt()} (+সুদ $rateText% = ৳${l.chargeAmount.toInt()})",
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                Text(
                                                    text = if (l.status == "ACTIVE") "চলমান" else "পরিশোধিত",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (l.status == "ACTIVE") MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "মোট প্রদেয়: ৳${l.totalPayable.toInt()} | আদায়: ৳${l.totalPaid.toInt()} | বকেয়া: ৳${l.remainingDue.toInt()}",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                            Text(
                                                text = "সময়সূচী: ${l.paidInstallments}/${l.totalInstallments} টি ($freqLabel, কিস্তি: ৳${l.perInstallmentAmount.toInt()})",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF374151)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Admin-only Delete Button
                if (currentUserRole == UserRole.ADMIN) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("btn_admin_delete_member")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("অ্যাডমিন: সদস্য ডিলিট করুন")
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("সদস্য ডিলিট নিশ্চিতকরণ") },
            text = { Text("আপনি কি নিশ্চিত যে '${member.name}' এর সকল তথ্য ডাটাবেজ থেকে মুছে ফেলতে চান? শুধুমাত্র অ্যাডমিন এটি করতে পারেন।") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteMember(member, currentUserRole)
                        showDeleteConfirm = false
                        onDismiss()
                    }
                ) {
                    Text("হ্যাঁ, ডিলিট করুন", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("বাতিল")
                }
            }
        )
    }
}
