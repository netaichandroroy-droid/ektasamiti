package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddToDrive
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.backup.SamitiBackupSnapshot
import com.example.data.cloud.DriveAuthOutcome
import com.example.data.cloud.GoogleDriveBackupManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class PendingDriveAction {
    CONNECT_ONLY,
    BACKUP_NOW,
    RESTORE_NOW
}

@Composable
fun GoogleDriveBackupCard(
    onBuildBackupJson: suspend () -> Pair<String, Int>,
    onParseBackupJson: (String) -> Result<SamitiBackupSnapshot>,
    onConfirmRestoreSnapshot: (SamitiBackupSnapshot, String, (Boolean, String) -> Unit) -> Unit,
    onFeedbackMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivityOrNull() }
    val coroutineScope = rememberCoroutineScope()
    val driveManager = remember(context) { GoogleDriveBackupManager(context) }
    val driveState by driveManager.state.collectAsState()

    var pendingAction by remember { mutableStateOf(PendingDriveAction.CONNECT_ONLY) }
    var pendingRestoreSnapshot by remember { mutableStateOf<Pair<SamitiBackupSnapshot, String>?>(null) }

    fun executeActionWithToken(token: String, action: PendingDriveAction) {
        coroutineScope.launch {
            when (action) {
                PendingDriveAction.CONNECT_ONLY -> {
                    driveManager.setBusy(true, "Google Drive সংযুক্ত হচ্ছে...")
                    driveManager.markConnectedWithToken(token).onSuccess { state ->
                        val emailText = state.accountEmail?.let { " ($it)" }.orEmpty()
                        onFeedbackMessage("Google Drive সফলভাবে যুক্ত হয়েছে$emailText")
                    }.onFailure { err ->
                        onFeedbackMessage(err.message ?: "Google Drive সংযোগ ব্যর্থ হয়েছে")
                    }
                }
                PendingDriveAction.BACKUP_NOW -> {
                    driveManager.setBusy(true, "Google Drive-এ ব্যাকআপ আপলোড হচ্ছে...")
                    val (jsonPayload, totalCount) = withContext(Dispatchers.IO) {
                        onBuildBackupJson()
                    }
                    driveManager.uploadBackup(token, jsonPayload, totalCount).onSuccess {
                        onFeedbackMessage("Google Drive-এ $totalCount টি রেকর্ডের ব্যাকআপ সম্পন্ন হয়েছে!")
                    }.onFailure { err ->
                        val msg = err.message ?: "Google Drive ব্যাকআপ ব্যর্থ হয়েছে"
                        driveManager.setError(msg)
                        onFeedbackMessage(msg)
                    }
                }
                PendingDriveAction.RESTORE_NOW -> {
                    driveManager.setBusy(true, "Google Drive থেকে ব্যাকআপ খোঁজা হচ্ছে...")
                    driveManager.downloadLatestBackup(token).onSuccess { downloaded ->
                        driveManager.setBusy(false, null)
                        onParseBackupJson(downloaded.jsonContent).onSuccess { snapshot ->
                            pendingRestoreSnapshot = snapshot to "Google Drive"
                        }.onFailure { parseErr ->
                            val msg = parseErr.message ?: "ব্যাকআপ ফাইল পড়া যায়নি"
                            driveManager.setError(msg)
                            onFeedbackMessage(msg)
                        }
                    }.onFailure { err ->
                        val msg = err.message ?: "Google Drive থেকে ব্যাকআপ পাওয়া যায়নি"
                        driveManager.setError(msg)
                        onFeedbackMessage(msg)
                    }
                }
            }
        }
    }

    val consentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val hostActivity = activity
        if (hostActivity == null) {
            driveManager.setError("Activity পাওয়া যায়নি")
            return@rememberLauncherForActivityResult
        }
        if (result.resultCode != Activity.RESULT_OK) {
            driveManager.setBusy(false, null)
            onFeedbackMessage("Google Drive অনুমোদন বাতিল করা হয়েছে")
            return@rememberLauncherForActivityResult
        }
        when (val outcome = driveManager.extractTokenFromConsentResult(hostActivity, result.data)) {
            is DriveAuthOutcome.Authorized -> {
                executeActionWithToken(outcome.accessToken, pendingAction)
            }
            is DriveAuthOutcome.NeedsConsent -> {
                driveManager.setError("পুনরায় অনুমোদন প্রয়োজন")
            }
            is DriveAuthOutcome.Error -> {
                driveManager.setError(outcome.message)
                onFeedbackMessage(outcome.message)
            }
        }
    }

    fun startDriveAction(action: PendingDriveAction) {
        val hostActivity = activity
        if (hostActivity == null) {
            onFeedbackMessage("Google Drive চালু করতে অ্যাক্টিভিটি পাওয়া যায়নি")
            return
        }
        pendingAction = action
        driveManager.setBusy(true, "Google Drive অনুমতি যাচাই হচ্ছে...")
        coroutineScope.launch {
            when (val outcome = driveManager.requestDriveAuthorization(hostActivity)) {
                is DriveAuthOutcome.Authorized -> {
                    executeActionWithToken(outcome.accessToken, action)
                }
                is DriveAuthOutcome.NeedsConsent -> {
                    consentLauncher.launch(
                        IntentSenderRequest.Builder(outcome.pendingIntent.intentSender).build()
                    )
                }
                is DriveAuthOutcome.Error -> {
                    driveManager.setError(outcome.message)
                    onFeedbackMessage(outcome.message)
                }
            }
        }
    }

    // Zero-permission SAF fallback: works directly with phone's Google Drive / Files provider
    val createFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        coroutineScope.launch {
            try {
                val (jsonPayload, totalCount) = withContext(Dispatchers.IO) {
                    onBuildBackupJson()
                }
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(jsonPayload.toByteArray(Charsets.UTF_8))
                    } ?: error("ফাইল লেখা যায়নি")
                }
                onFeedbackMessage("ড্রাইভ / ফাইলে $totalCount টি রেকর্ডের ব্যাকআপ সেভ হয়েছে!")
            } catch (e: Exception) {
                onFeedbackMessage("ফাইল সেভ করা যায়নি: ${e.localizedMessage ?: "ত্রুটি"}")
            }
        }
    }

    val openFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        coroutineScope.launch {
            try {
                val rawJson = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                        it.readText()
                    } ?: error("ফাইল পড়া যায়নি")
                }
                onParseBackupJson(rawJson).onSuccess { snapshot ->
                    pendingRestoreSnapshot = snapshot to "নির্বাচিত ব্যাকআপ ফাইল"
                }.onFailure { err ->
                    onFeedbackMessage(err.message ?: "বৈধ ব্যাকআপ ফাইল নয়")
                }
            } catch (e: Exception) {
                onFeedbackMessage("ফাইল রিস্টোর করা যায়নি: ${e.localizedMessage ?: "ত্রুটি"}")
            }
        }
    }

    val lastBackupFormatted = remember(driveState.lastBackupAtMillis) {
        driveState.lastBackupAtMillis?.let { ts ->
            SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(ts))
        }
    }

    val borderColor = if (driveState.isConnected) Color(0xFF0F9D58) else Color(0xFF4285F4)
    val bgColor = if (driveState.isConnected) Color(0xFFECFDF5) else Color(0xFFEFF6FF)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(1.5.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_google_drive_backup")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
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
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(1.dp, borderColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (driveState.isBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = borderColor,
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.AddToDrive,
                                contentDescription = "Google Drive",
                                tint = borderColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Google Drive ব্যাকআপ",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (driveState.isConnected) Color(0xFFD1FAE5) else Color(0xFFDBEAFE),
                                border = BorderStroke(1.dp, borderColor.copy(alpha = 0.6f))
                            ) {
                                Text(
                                    text = if (driveState.isConnected) "সংযুক্ত" else "যুক্ত নয়",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (driveState.isConnected) Color(0xFF065F46) else Color(0xFF1E40AF),
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = when {
                                driveState.isBusy && !driveState.statusMessage.isNullOrBlank() ->
                                    driveState.statusMessage ?: ""
                                driveState.isConnected -> buildString {
                                    if (!driveState.accountEmail.isNullOrBlank()) {
                                        append(driveState.accountEmail)
                                    } else {
                                        append("আপনার Google Drive অ্যাকাউন্ট যুক্ত আছে")
                                    }
                                    if (lastBackupFormatted != null) {
                                        append("\nসর্বশেষ ব্যাকআপ: $lastBackupFormatted (${driveState.lastBackupRecordCount}টি রেকর্ড)")
                                    }
                                }
                                else -> "আপনার নিজস্ব Google Drive-এ সমিতির সব ডাটা নিরাপদে ব্যাকআপ রাখুন ও যেকোনো সময় রিস্টোর করুন।"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF1E293B)
                        )
                    }
                }
            }

            if (!driveState.errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFEE2E2),
                    border = BorderStroke(1.dp, Color(0xFFDC2626).copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = driveState.errorMessage ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF991B1B),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (!driveState.isConnected) {
                Button(
                    onClick = { startDriveAction(PendingDriveAction.CONNECT_ONLY) },
                    enabled = !driveState.isBusy,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1A73E8),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("btn_connect_google_drive")
                ) {
                    Icon(Icons.Default.AddToDrive, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Google Drive যুক্ত করুন",
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { startDriveAction(PendingDriveAction.BACKUP_NOW) },
                        enabled = !driveState.isBusy,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0F9D58),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("btn_backup_to_google_drive")
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Drive-এ ব্যাকআপ",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    OutlinedButton(
                        onClick = { startDriveAction(PendingDriveAction.RESTORE_NOW) },
                        enabled = !driveState.isBusy,
                        border = BorderStroke(1.2.dp, Color(0xFF0F9D58)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("btn_restore_from_google_drive")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = Color(0xFF065F46),
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Drive থেকে রিস্টোর",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF065F46),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            driveManager.disconnectDrive()
                            onFeedbackMessage("Google Drive অ্যাকাউন্ট সংযোগ বিচ্ছিন্ন হয়েছে")
                        },
                        enabled = !driveState.isBusy,
                        modifier = Modifier.testTag("btn_disconnect_google_drive")
                    ) {
                        Icon(
                            imageVector = Icons.Default.LinkOff,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Drive সংযোগ বিচ্ছিন্ন করুন",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFDC2626)
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = borderColor.copy(alpha = 0.25f)
            )

            // Secondary zero-permission SAF backup/restore (works with Google Drive app & local storage)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                        createFileLauncher.launch("EktaSamiti_Backup_$stamp.json")
                    },
                    enabled = !driveState.isBusy,
                    border = BorderStroke(1.dp, Color(0xFF475569).copy(alpha = 0.45f)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .testTag("btn_export_drive_file")
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null,
                        tint = Color(0xFF1E293B),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "ড্রাইভ/ফাইলে সেভ",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = {
                        openFileLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                    },
                    enabled = !driveState.isBusy,
                    border = BorderStroke(1.dp, Color(0xFF475569).copy(alpha = 0.45f)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .testTag("btn_import_drive_file")
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = Color(0xFF1E293B),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "ফাইল থেকে রিস্টোর",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }

    pendingRestoreSnapshot?.let { (snapshot, sourceLabel) ->
        val dateText = remember(snapshot.exportedAtMillis) {
            SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(snapshot.exportedAtMillis))
        }
        AlertDialog(
            onDismissRequest = { pendingRestoreSnapshot = null },
            title = {
                Text(
                    text = "ব্যাকআপ রিস্টোর নিশ্চিত করুন",
                    fontWeight = FontWeight.ExtraBold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "$sourceLabel থেকে পাওয়া ব্যাকআপ ফাইলটি রিস্টোর করতে চান?",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "ব্যাকআপের তারিখ: $dateText",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "• সদস্য: ${snapshot.members.size} জন\n• সঞ্চয় লেনদেন: ${snapshot.savingsTransactions.size} টি\n• ঋণ: ${snapshot.loans.size} টি\n• কিস্তি আদায়: ${snapshot.installmentPayments.size} টি",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF065F46)
                    )
                    Text(
                        text = "সতর্কতা: রিস্টোর করলে ফোনের বর্তমান সদস্য ও লেনদেন তথ্য এই ব্যাকআপ দিয়ে প্রতিস্থাপিত হবে।",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFB91C1C),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingRestoreSnapshot = null
                        onConfirmRestoreSnapshot(snapshot, sourceLabel) { _, msg ->
                            onFeedbackMessage(msg)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F9D58)),
                    modifier = Modifier.testTag("btn_confirm_drive_restore")
                ) {
                    Text("হ্যাঁ, রিস্টোর করুন", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestoreSnapshot = null }) {
                    Text("বাতিল")
                }
            }
        )
    }
}

private tailrec fun Context.findActivityOrNull(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivityOrNull()
    else -> null
}
