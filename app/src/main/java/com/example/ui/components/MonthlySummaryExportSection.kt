package com.example.ui.components

import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material.icons.filled.Visibility
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.InstallmentPayment
import com.example.data.model.Loan
import com.example.data.model.Member
import com.example.data.model.SavingsTransaction
import com.example.reports.MonthlyFinancialSummary
import com.example.reports.MonthlySummaryCalculator
import com.example.reports.MonthlySummaryImageExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

@Composable
fun MonthlySummaryExportCard(
    members: List<Member>,
    savingsList: List<SavingsTransaction>,
    loansList: List<Loan>,
    installmentsList: List<InstallmentPayment>,
    generatedBy: String,
    onFeedbackMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val nowCal = remember { Calendar.getInstance() }
    var selectedYear by remember { mutableIntStateOf(nowCal.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableIntStateOf(nowCal.get(Calendar.MONTH)) }
    var showPreviewDialog by remember { mutableStateOf(false) }

    val summary = remember(
        selectedYear,
        selectedMonth,
        members,
        savingsList,
        loansList,
        installmentsList,
        generatedBy
    ) {
        MonthlySummaryCalculator.calculateMonthlySummary(
            year = selectedYear,
            month = selectedMonth,
            members = members,
            savingsList = savingsList,
            loansList = loansList,
            installmentsList = installmentsList,
            generatedBy = generatedBy
        )
    }

    val shareableText = remember(summary) {
        MonthlySummaryCalculator.formatAsShareableText(summary)
    }

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    var pendingBitmapForSaf by remember { mutableStateOf<Bitmap?>(null) }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/png")
    ) { uri ->
        val bmp = pendingBitmapForSaf
        pendingBitmapForSaf = null
        if (uri != null && bmp != null) {
            coroutineScope.launch {
                val result = withContext(Dispatchers.IO) {
                    MonthlySummaryImageExporter.writeBitmapToUri(context, bmp, uri)
                }
                if (result.isSuccess) {
                    onFeedbackMessage("মাসিক সারসংক্ষেপের ছবি সফলভাবে ডাউনলোড হয়েছে।")
                } else {
                    onFeedbackMessage("ছবি সংরক্ষণ করতে সমস্যা হয়েছে।")
                }
            }
        }
    }

    fun triggerDownloadPictureOnly() {
        coroutineScope.launch {
            val bitmap = withContext(Dispatchers.Default) {
                MonthlySummaryImageExporter.renderMonthlySummaryBitmap(summary)
            }
            val fileName = MonthlySummaryImageExporter.defaultImageFileName(summary)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val result = withContext(Dispatchers.IO) {
                    MonthlySummaryImageExporter.saveBitmapToPictures(context, bitmap, fileName)
                }
                result.onSuccess { path ->
                    onFeedbackMessage("ছবি ডাউনলোড সম্পন্ন: $path")
                }.onFailure { err ->
                    onFeedbackMessage("ছবি ডাউনলোড ব্যর্থ: ${err.message ?: "অজানা ত্রুটি"}")
                }
            } else {
                pendingBitmapForSaf = bitmap
                createDocumentLauncher.launch(fileName)
            }
        }
    }

    fun shiftMonth(delta: Int) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedYear)
            set(Calendar.MONTH, selectedMonth)
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, delta)
        }
        selectedYear = cal.get(Calendar.YEAR)
        selectedMonth = cal.get(Calendar.MONTH)
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE6E9EE)),
        border = BorderStroke(1.5.dp, Color(0xFF2563EB)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("monthly_summary_export_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFDBEAFE))
                            .border(1.dp, Color(0xFF2563EB), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Summarize,
                            contentDescription = "মাসিক সারসংক্ষেপ এক্সপোর্ট",
                            tint = Color(0xFF1D4ED8),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "মাসিক আর্থিক সারসংক্ষেপ এক্সপোর্ট",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF111827)
                        )
                        Text(
                            text = "মেসেজিং অ্যাপে টেক্সট শেয়ার অথবা শুধুমাত্র ছবি ডাউনলোড",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF374151)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Month Selector Row
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFDCE1E8),
                border = BorderStroke(1.dp, Color(0xFF93C5FD)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = { shiftMonth(-1) },
                        modifier = Modifier.testTag("btn_prev_summary_month")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "আগের মাস",
                            tint = Color(0xFF1D4ED8)
                        )
                    }

                    Text(
                        text = summary.monthTitleBn,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1E3A8A)
                    )

                    IconButton(
                        onClick = { shiftMonth(1) },
                        modifier = Modifier.testTag("btn_next_summary_month")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "পরের মাস",
                            tint = Color(0xFF1D4ED8)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Highlighted Monthly Summary Stats Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryMiniStatBox(
                    label = "মাসিক সঞ্চয় জমা",
                    value = MonthlySummaryCalculator.formatAmount(summary.savingsDepositTotal),
                    subtext = "${summary.savingsDepositCount}টি • উত্তোলন ${MonthlySummaryCalculator.formatAmount(summary.savingsWithdrawalTotal)}",
                    bgColor = Color(0xFFE8F7EE),
                    borderColor = Color(0xFF86EFAC),
                    valueColor = Color(0xFF166534),
                    modifier = Modifier.weight(1f)
                )

                SummaryMiniStatBox(
                    label = "মাসিক কিস্তি আদায়",
                    value = MonthlySummaryCalculator.formatAmount(summary.loanRepaymentTotal),
                    subtext = "${summary.loanRepaymentCount}টি • ঋণ বিতরণ ${MonthlySummaryCalculator.formatAmount(summary.loanDisbursedTotal)}",
                    bgColor = Color(0xFFFFF8E7),
                    borderColor = Color(0xFFFDE68A),
                    valueColor = Color(0xFFB45309),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Net Collection Banner
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFDBEAFE),
                border = BorderStroke(1.dp, Color(0xFF93C5FD)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "মোট মাসিক আদায় (সঞ্চয় + কিস্তি)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E3A8A)
                    )
                    Text(
                        text = MonthlySummaryCalculator.formatAmount(summary.totalMonthlyCollection),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1D4ED8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Primary Export Actions: Share Text & Download Picture Only
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            putExtra(Intent.EXTRA_SUBJECT, "একতা সমিতি - ${summary.monthTitleBn} আর্থিক সারসংক্ষেপ")
                            putExtra(Intent.EXTRA_TEXT, shareableText)
                            type = "text/plain"
                        }
                        context.startActivity(
                            Intent.createChooser(sendIntent, "মেসেজিং অ্যাপে সারসংক্ষেপ শেয়ার করুন")
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1D4ED8),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_share_monthly_text")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "টেক্সট শেয়ার",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = { triggerDownloadPictureOnly() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0D683B),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_download_monthly_picture")
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ছবি ডাউনলোড",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Secondary Row: Copy Text & Full Preview Dialog
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(shareableText))
                        onFeedbackMessage("মাসিক সারসংক্ষেপ টেক্সট ক্লিপবোর্ডে কপি হয়েছে।")
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF2563EB)),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_copy_monthly_text")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        tint = Color(0xFF1D4ED8),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "টেক্সট কপি",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1D4ED8)
                    )
                }

                OutlinedButton(
                    onClick = { showPreviewDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF0D683B)),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_preview_monthly_summary")
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null,
                        tint = Color(0xFF0D683B),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "টেক্সট ও ছবি প্রিভিউ",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0D683B)
                    )
                }
            }
        }
    }

    if (showPreviewDialog) {
        MonthlySummaryPreviewDialog(
            members = members,
            savingsList = savingsList,
            loansList = loansList,
            installmentsList = installmentsList,
            generatedBy = generatedBy,
            initialYear = selectedYear,
            initialMonth = selectedMonth,
            onDismiss = { showPreviewDialog = false },
            onFeedbackMessage = onFeedbackMessage
        )
    }
}

@Composable
fun MonthlySummaryPreviewDialog(
    members: List<Member>,
    savingsList: List<SavingsTransaction>,
    loansList: List<Loan>,
    installmentsList: List<InstallmentPayment>,
    generatedBy: String,
    initialYear: Int? = null,
    initialMonth: Int? = null,
    onDismiss: () -> Unit,
    onFeedbackMessage: (String) -> Unit
) {
    val nowCal = remember { Calendar.getInstance() }
    var selectedYear by remember { mutableIntStateOf(initialYear ?: nowCal.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableIntStateOf(initialMonth ?: nowCal.get(Calendar.MONTH)) }
    var previewTab by remember { mutableIntStateOf(0) } // 0: Picture preview, 1: Formatted Text preview

    val summary = remember(
        selectedYear,
        selectedMonth,
        members,
        savingsList,
        loansList,
        installmentsList,
        generatedBy
    ) {
        MonthlySummaryCalculator.calculateMonthlySummary(
            year = selectedYear,
            month = selectedMonth,
            members = members,
            savingsList = savingsList,
            loansList = loansList,
            installmentsList = installmentsList,
            generatedBy = generatedBy
        )
    }
    val shareableText = remember(summary) {
        MonthlySummaryCalculator.formatAsShareableText(summary)
    }
    val previewBitmap = remember(summary) {
        MonthlySummaryImageExporter.renderMonthlySummaryBitmap(summary)
    }

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/png")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val result = withContext(Dispatchers.IO) {
                    MonthlySummaryImageExporter.writeBitmapToUri(context, previewBitmap, uri)
                }
                if (result.isSuccess) {
                    onFeedbackMessage("শুধুমাত্র ছবি সফলভাবে ডাউনলোড হয়েছে।")
                } else {
                    onFeedbackMessage("ছবি সংরক্ষণ করতে সমস্যা হয়েছে।")
                }
            }
        }
    }

    fun shiftMonth(delta: Int) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedYear)
            set(Calendar.MONTH, selectedMonth)
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, delta)
        }
        selectedYear = cal.get(Calendar.YEAR)
        selectedMonth = cal.get(Calendar.MONTH)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("monthly_summary_preview_dialog")
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "মাসিক সারসংক্ষেপ প্রিভিউ",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "মেসেজিং টেক্সট ও শুধুমাত্র ছবি ডাউনলোড",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "বন্ধ করুন")
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Month Switcher
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFEFF6FF),
                    border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = { shiftMonth(-1) }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "আগের মাস")
                        }
                        Text(
                            text = summary.monthTitleBn,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1E3A8A)
                        )
                        IconButton(onClick = { shiftMonth(1) }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "পরের মাস")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                TabRow(selectedTabIndex = previewTab) {
                    Tab(
                        selected = previewTab == 0,
                        onClick = { previewTab = 0 },
                        text = { Text("ছবি প্রিভিউ", fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    Tab(
                        selected = previewTab == 1,
                        onClick = { previewTab = 1 },
                        text = { Text("টেক্সট প্রিভিউ", fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.Summarize, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 240.dp, max = 350.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF3F4F6))
                        .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(12.dp))
                        .verticalScroll(rememberScrollState())
                        .padding(8.dp)
                ) {
                    if (previewTab == 0) {
                        Image(
                            bitmap = previewBitmap.asImageBitmap(),
                            contentDescription = "মাসিক আর্থিক সারসংক্ষেপ ছবি",
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                        )
                    } else {
                        Text(
                            text = shareableText,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.SansSerif),
                            color = Color(0xFF111827),
                            modifier = Modifier.padding(6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                // Action 1: Download Picture Only
                Button(
                    onClick = {
                        val fileName = MonthlySummaryImageExporter.defaultImageFileName(summary)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            coroutineScope.launch {
                                val result = withContext(Dispatchers.IO) {
                                    MonthlySummaryImageExporter.saveBitmapToPictures(context, previewBitmap, fileName)
                                }
                                result.onSuccess { path ->
                                    onFeedbackMessage("ছবি ডাউনলোড সম্পন্ন: $path")
                                }.onFailure { err ->
                                    onFeedbackMessage("ছবি ডাউনলোড ব্যর্থ: ${err.message ?: "অজানা ত্রুটি"}")
                                }
                            }
                        } else {
                            createDocumentLauncher.launch(fileName)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0D683B),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_dialog_download_picture_only")
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("শুধুমাত্র ছবি ডাউনলোড করুন (PNG)", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Action 2: Share formatted text via messaging apps & Copy
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(shareableText))
                            onFeedbackMessage("টেক্সট ক্লিপবোর্ডে কপি হয়েছে।")
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("টেক্সট কপি", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                putExtra(Intent.EXTRA_SUBJECT, "একতা সমিতি - ${summary.monthTitleBn} আর্থিক সারসংক্ষেপ")
                                putExtra(Intent.EXTRA_TEXT, shareableText)
                                type = "text/plain"
                            }
                            context.startActivity(
                                Intent.createChooser(sendIntent, "মেসেজিং অ্যাপে শেয়ার করুন")
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1D4ED8),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_dialog_share_text")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("টেক্সট শেয়ার", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryMiniStatBox(
    label: String,
    value: String,
    subtext: String,
    bgColor: Color,
    borderColor: Color,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF374151)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = valueColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtext,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF4B5563),
                maxLines = 2
            )
        }
    }
}
