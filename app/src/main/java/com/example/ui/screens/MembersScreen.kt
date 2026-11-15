package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.Member
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.notifications.LoanReminderCalculator
import com.example.notifications.UpcomingWeeklyRepayment
import com.example.ui.components.DatePickerField
import com.example.ui.components.PicturePicker
import com.example.ui.components.SamitiDatePickerDialog
import com.example.ui.theme.SilverCardAltBg
import com.example.ui.theme.SilverCardBg
import com.example.ui.theme.SilverCardBorder
import com.example.ui.viewmodel.SamitiViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MembersScreen(
    currentUser: User,
    viewModel: SamitiViewModel,
    showAddDialogInitially: Boolean = false,
    onOpenDepositForMember: (Member) -> Unit = {},
    onOpenLoanForMember: (Member) -> Unit = {}
) {
    val members by viewModel.members.collectAsState()
    val activeLoans by viewModel.activeLoans.collectAsState()
    val filterType by viewModel.selectedMemberTypeFilter.collectAsState()
    val passbookMember by viewModel.passbookMember.collectAsState()
    var showUpcomingDueOnly by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.setSearchQuery("")
    }

    val upcomingDueByMember = remember(activeLoans) {
        LoanReminderCalculator.findUpcomingMemberRepayments(
            loans = activeLoans,
            withinDays = 3,
            includeOverdue = false
        )
    }

    val displayedMembers = remember(members, showUpcomingDueOnly, upcomingDueByMember) {
        if (showUpcomingDueOnly) {
            members.filter { upcomingDueByMember.containsKey(it.memberCode) }
        } else {
            members
        }
    }

    var showAddDialog by remember { mutableStateOf(showAddDialogInitially) }
    var memberToEditDate by remember { mutableStateOf<Member?>(null) }
    LaunchedEffect(showAddDialogInitially) {
        if (showAddDialogInitially) {
            showAddDialog = true
        }
    }

    val totalSavings = remember(members) { members.sumOf { it.savingsBalance } }
    val totalDue = remember(members) { members.sumOf { it.totalLoanDue } }

    if (passbookMember != null) {
        MemberDetailScreen(
            member = passbookMember!!,
            currentUserRole = currentUser.role,
            viewModel = viewModel,
            onBack = { viewModel.closePassbook() },
            onOpenDepositForMember = { m ->
                viewModel.closePassbook()
                onOpenDepositForMember(m)
            },
            onOpenLoanForMember = { m ->
                viewModel.closePassbook()
                onOpenLoanForMember(m)
            }
        )
        return
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_add_member")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = "নতুন সদস্য যোগ করুন")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("নতুন সদস্য", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("members_screen")
        ) {
            // Top Profile Summary Banner + Search Bar & Filter Chips
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(listOf(Color(0xFF5A45FE), Color(0xFF7D62FA)))
                                )
                                .padding(horizontal = 10.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.22f),
                                modifier = Modifier.align(Alignment.BottomEnd).size(32.dp)
                            )
                            Column {
                                Text(
                                    text = "মোট সদস্য",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.92f)
                                )
                                Text(
                                    text = "${members.size} জন",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(listOf(Color(0xFF008488), Color(0xFF00A8A8)))
                                )
                                .padding(horizontal = 10.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.22f),
                                modifier = Modifier.align(Alignment.BottomEnd).size(32.dp)
                            )
                            Column {
                                Text(
                                    text = "মোট সঞ্চয়",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.92f)
                                )
                                Text(
                                    text = "৳${totalSavings.toInt()}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(listOf(Color(0xFFEA356B), Color(0xFFF4608B)))
                                )
                                .padding(horizontal = 10.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Payments,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.22f),
                                modifier = Modifier.align(Alignment.BottomEnd).size(32.dp)
                            )
                            Column {
                                Text(
                                    text = "বকেয়া ঋণ",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.92f)
                                )
                                Text(
                                    text = "৳${totalDue.toInt()}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = filterType == "ALL" && !showUpcomingDueOnly,
                            onClick = {
                                showUpcomingDueOnly = false
                                viewModel.setMemberFilter("ALL")
                            },
                            label = { Text("সকল সদস্য (${members.size})") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = filterType == "WEEKLY" && !showUpcomingDueOnly,
                            onClick = {
                                showUpcomingDueOnly = false
                                viewModel.setMemberFilter("WEEKLY")
                            },
                            label = { Text("সাপ্তাহিক সঞ্চয়ী") }
                        )
                    }
                    item {
                        val upcomingCount = members.count { upcomingDueByMember.containsKey(it.memberCode) }
                        FilterChip(
                            selected = showUpcomingDueOnly,
                            onClick = {
                                showUpcomingDueOnly = !showUpcomingDueOnly
                                if (showUpcomingDueOnly) {
                                    viewModel.setMemberFilter("ALL")
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = if (showUpcomingDueOnly) Color.White else Color(0xFFB45309),
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = "৩ দিনে পরিশোধ ($upcomingCount)",
                                    fontWeight = if (upcomingCount > 0 || showUpcomingDueOnly) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = if (upcomingCount > 0) Color(0xFFFEF3C7) else Color.Transparent,
                                labelColor = if (upcomingCount > 0) Color(0xFF92400E) else MaterialTheme.colorScheme.onSurface,
                                selectedContainerColor = Color(0xFFD97706),
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("filter_upcoming_due_3d")
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Member list
            if (displayedMembers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (showUpcomingDueOnly) Icons.Default.NotificationsActive else Icons.Default.Person,
                            contentDescription = null,
                            tint = if (showUpcomingDueOnly) Color(0xFFD97706) else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = when {
                                showUpcomingDueOnly -> "আগামী ৩ দিনের মধ্যে কোনো সদস্যের কিস্তি পরিশোধের তারিখ নেই"
                                else -> "এখনো কোন সদস্য নিবন্ধিত হয়নি"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF374151)
                        )
                        if (!showUpcomingDueOnly) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { showAddDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("btn_empty_add_member")
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("নতুন সদস্য যোগ করুন", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 84.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(displayedMembers, key = { it.id }) { member ->
                        val upcomingDue = upcomingDueByMember[member.memberCode]
                        MemberCard(
                            member = member,
                            currentUserRole = currentUser.role,
                            upcomingDue = upcomingDue,
                            onPassbookClick = { viewModel.openPassbook(member) },
                            onDepositClick = { onOpenDepositForMember(member) },
                            onLoanClick = { onOpenLoanForMember(member) },
                            onEditDateClick = { memberToEditDate = member }
                        )
                    }
                }
            }
        }
    }

    // Add Member Dialog
    if (showAddDialog) {
        MemberRegistrationDialog(
            currentUser = currentUser,
            viewModel = viewModel,
            onDismiss = { showAddDialog = false }
        )
    }

    memberToEditDate?.let { m ->
        SamitiDatePickerDialog(
            initialDateMillis = m.registrationDate,
            title = "সদস্য নিবন্ধনের তারিখ পরিবর্তন করুন",
            onDateSelected = { newMillis ->
                viewModel.updateMemberRegistrationDate(m, newMillis)
                memberToEditDate = null
            },
            onDismiss = { memberToEditDate = null }
        )
    }
}

@Composable
fun MemberCard(
    member: Member,
    currentUserRole: UserRole,
    upcomingDue: UpcomingWeeklyRepayment? = null,
    onPassbookClick: () -> Unit,
    onDepositClick: () -> Unit,
    onLoanClick: () -> Unit,
    onEditDateClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val shortDateSdf = remember { SimpleDateFormat("dd MMM", Locale.getDefault()) }

    val isDueToday = upcomingDue?.daysUntilDue == 0
    val highlightAccent = when {
        upcomingDue == null -> null
        isDueToday -> Color(0xFFDC2626)
        else -> Color(0xFFD97706)
    }
    val highlightBannerBg = when {
        upcomingDue == null -> Color.Transparent
        isDueToday -> Color(0xFFFEE2E2)
        else -> Color(0xFFFEF3C7)
    }
    val highlightBannerText = when {
        upcomingDue == null -> Color.Unspecified
        isDueToday -> Color(0xFF991B1B)
        else -> Color(0xFF92400E)
    }
    val cardBackgroundBrush = when {
        upcomingDue == null -> Brush.linearGradient(listOf(SilverCardBg, SilverCardBg))
        isDueToday -> Brush.linearGradient(listOf(Color(0xFFFFF5F5), Color(0xFFFEE2E2).copy(alpha = 0.58f)))
        else -> Brush.linearGradient(listOf(Color(0xFFFFFBEB), Color(0xFFFEF3C7).copy(alpha = 0.65f)))
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (upcomingDue != null) Color.Transparent else SilverCardBg),
        border = BorderStroke(
            width = if (highlightAccent != null) 2.dp else 1.2.dp,
            color = highlightAccent ?: SilverCardBorder
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (upcomingDue != null) 6.dp else 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPassbookClick() }
            .testTag("member_card_${member.memberCode}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBackgroundBrush)
                .then(
                    if (highlightAccent != null) {
                        Modifier.drawBehind {
                            drawRect(
                                color = highlightAccent,
                                topLeft = Offset.Zero,
                                size = Size(width = 5.dp.toPx(), height = size.height)
                            )
                        }
                    } else {
                        Modifier
                    }
                )
                .padding(start = if (highlightAccent != null) 16.dp else 14.dp, end = 14.dp, top = 14.dp, bottom = 14.dp)
        ) {
            if (upcomingDue != null && highlightAccent != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = highlightBannerBg,
                    border = BorderStroke(1.2.dp, highlightAccent.copy(alpha = 0.65f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .testTag("member_due_highlight_${member.memberCode}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "ঋণ পরিশোধের তারিখ আসন্ন",
                                tint = highlightAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = LoanReminderCalculator.formatDueBadgeText(upcomingDue.daysUntilDue),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = highlightBannerText
                                )
                                Text(
                                    text = "কিস্তি #${upcomingDue.nextInstallmentNumber} • তারিখ: ${shortDateSdf.format(Date(upcomingDue.dueDateMillis))}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = highlightBannerText.copy(alpha = 0.9f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = highlightAccent
                        ) {
                            Text(
                                text = "৳${upcomingDue.expectedAmount.toInt()}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile Avatar / Picture
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(if (highlightAccent != null) highlightBannerBg else Color(0xFFE8F8EE))
                        .border(
                            width = if (highlightAccent != null) 2.dp else 1.5.dp,
                            color = highlightAccent ?: Color(0xFF1B7A49).copy(alpha = 0.4f),
                            shape = CircleShape
                        ),
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
                            contentDescription = member.name,
                            tint = highlightAccent ?: Color(0xFF1B7A49),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = member.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF111827),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFE8F7EE),
                            border = BorderStroke(1.dp, Color(0xFF1B7A49).copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "সাপ্তাহিক",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF1B7A49),
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Badge,
                            contentDescription = null,
                            tint = Color(0xFF1565C0),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "হিসাব নং: ${member.memberCode} • নিবন্ধন: ${sdf.format(Date(member.registrationDate))}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF374151),
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFE0F2FE),
                            modifier = Modifier
                                .clickable { onEditDateClick() }
                                .testTag("btn_edit_member_date_${member.memberCode}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.EditCalendar,
                                contentDescription = "নিবন্ধনের তারিখ পরিবর্তন করুন",
                                tint = Color(0xFF1565C0),
                                modifier = Modifier.padding(4.dp).size(14.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Profile Summary & Contact Information Box
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = SilverCardAltBg,
                border = BorderStroke(1.dp, SilverCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "মোবাইল নম্বর",
                                tint = Color(0xFF15803D),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "মোবাইল: ${member.mobile}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF111827)
                            )
                        }

                        if (member.mobile.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFE8F8EE),
                                border = BorderStroke(1.dp, Color(0xFF1B7A49).copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .clickable {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${member.mobile}"))
                                        context.startActivity(intent)
                                    }
                                    .testTag("btn_call_${member.memberCode}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Call,
                                        contentDescription = "কল করুন",
                                        tint = Color(0xFF15803D),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "কল করুন",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF15803D)
                                    )
                                }
                            }
                        }
                    }

                    if (member.address.isNotBlank()) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "ঠিকানা",
                                tint = Color(0xFF1565C0),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ঠিকানা: ${member.address}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF334155)
                            )
                        }
                    }

                    if (member.fatherOrHusbandName.isNotBlank() || member.nomineeName.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SupervisorAccount,
                                contentDescription = "অভিভাবক ও নমিনী",
                                tint = Color(0xFF7C3AED),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            val profileLine = buildString {
                                if (member.fatherOrHusbandName.isNotBlank()) {
                                    append("পিতা/স্বামী: ${member.fatherOrHusbandName}")
                                }
                                if (member.nomineeName.isNotBlank()) {
                                    if (isNotEmpty()) append(" • ")
                                    append("নমিনী: ${member.nomineeName}")
                                    if (member.nomineeRelation.isNotBlank()) {
                                        append(" (${member.nomineeRelation})")
                                    }
                                }
                            }
                            Text(
                                text = profileLine,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF475569)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Financial Summary Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFE8F8EE),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF86EFAC)),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Savings, contentDescription = null, tint = Color(0xFF15803D), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text("সঞ্চয় স্থিতি", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF166534))
                            Text("৳${member.savingsBalance.toInt()}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.ExtraBold, color = Color(0xFF15803D))
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (member.totalLoanDue > 0) Color(0xFFFEE2E2) else Color(0xFFF1F5F9),
                    border = androidx.compose.foundation.BorderStroke(
                        1.2.dp,
                        if (member.totalLoanDue > 0) Color(0xFFFCA5A5) else Color(0xFFCBD5E1)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Payments,
                            contentDescription = null,
                            tint = if (member.totalLoanDue > 0) Color(0xFFDC2626) else Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                "বকেয়া ঋণ",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (member.totalLoanDue > 0) Color(0xFF991B1B) else Color(0xFF475569)
                            )
                            Text(
                                "৳${member.totalLoanDue.toInt()}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (member.totalLoanDue > 0) Color(0xFFDC2626) else Color(0xFF0F172A)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = onPassbookClick,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    modifier = Modifier.weight(1.25f)
                ) {
                    Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "বিস্তারিত",
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Button(
                    onClick = onDepositClick,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    modifier = Modifier.weight(0.9f)
                ) {
                    Icon(Icons.Default.Savings, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "জমা",
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Button(
                    onClick = onLoanClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E6586)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    modifier = Modifier.weight(0.9f)
                ) {
                    Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "ঋণ",
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun MemberRegistrationDialog(
    currentUser: User,
    viewModel: SamitiViewModel,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var fatherOrHusband by remember { mutableStateOf("") }
    var nid by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    val memberType = "WEEKLY"
    var nomineeName by remember { mutableStateOf("") }
    var nomineeRelation by remember { mutableStateOf("") }
    var selectedPhotoUri by remember { mutableStateOf<String?>("avatar_1") }
    var admissionFee by remember { mutableStateOf("100") }
    var initialSavings by remember { mutableStateOf("200") }
    var registrationDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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
                .testTag("dialog_member_registration")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Text(
                    text = "নতুন সদস্য নিবন্ধন ফরম",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "একতা সমবায়ের নিয়ম অনুযায়ী সঠিক তথ্য দিন",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Picture Picker with photo capture & presets
                PicturePicker(
                    selectedPhotoUri = selectedPhotoUri,
                    onPhotoSelected = { selectedPhotoUri = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMessage = null },
                    label = { Text("সদস্যের পুরো নাম *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_member_name")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Mobile
                OutlinedTextField(
                    value = mobile,
                    onValueChange = { mobile = it; errorMessage = null },
                    label = { Text("মোবাইল নম্বর *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_member_mobile")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Father/Husband
                OutlinedTextField(
                    value = fatherOrHusband,
                    onValueChange = { fatherOrHusband = it },
                    label = { Text("পিতা / স্বামীর নাম") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // NID
                OutlinedTextField(
                    value = nid,
                    onValueChange = { nid = it },
                    label = { Text("জাতীয় পরিচয়পত্র (NID) / জন্ম নিবন্ধন") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Address
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("বর্তমান ঠিকানা / গ্রাম / ওয়ার্ড") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Member Type - Weekly Saver
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "সদস্যের সঞ্চয় ধরণ: সাপ্তাহিক সঞ্চয়ী",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "সাপ্তাহিক নিয়মিত কালেকশন ও জমা",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Nominee info
                OutlinedTextField(
                    value = nomineeName,
                    onValueChange = { nomineeName = it },
                    label = { Text("নমিনীর নাম") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = nomineeRelation,
                    onValueChange = { nomineeRelation = it },
                    label = { Text("নমিনীর সাথে সম্পর্ক") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Initial Fees
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = admissionFee,
                        onValueChange = { admissionFee = it },
                        label = { Text("ভর্তি ফি (৳)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = initialSavings,
                        onValueChange = { initialSavings = it },
                        label = { Text("প্রারম্ভিক সঞ্চয় (৳)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("input_initial_savings")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                DatePickerField(
                    label = "সদস্য নিবন্ধনের তারিখ",
                    selectedDateMillis = registrationDateMillis,
                    onDateSelected = { registrationDateMillis = it },
                    testTag = "btn_pick_member_reg_date"
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Action buttons
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
                            if (name.isBlank() || mobile.isBlank()) {
                                errorMessage = "সদস্যের নাম এবং মোবাইল নম্বর আবশ্যক!"
                                return@Button
                            }
                            val fee = admissionFee.toDoubleOrNull() ?: 0.0
                            val savings = initialSavings.toDoubleOrNull() ?: 0.0

                            viewModel.registerNewMember(
                                name = name,
                                mobile = mobile,
                                fatherOrHusbandName = fatherOrHusband,
                                nid = nid,
                                address = address,
                                memberType = memberType,
                                nomineeName = nomineeName,
                                nomineeRelation = nomineeRelation,
                                photoUri = selectedPhotoUri,
                                admissionFee = fee,
                                initialSavings = savings,
                                registeredBy = currentUser.fullName,
                                role = currentUser.role,
                                registrationDate = registrationDateMillis,
                                onSuccess = onDismiss
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        modifier = Modifier.weight(1.2f).testTag("btn_submit_member")
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
