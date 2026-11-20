package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.data.cloud.CloudSyncState
import com.example.data.cloud.CloudSyncStatus
import com.example.data.cloud.GoogleAuthManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CloudAutoSyncBanner(
    syncState: CloudSyncState,
    onSyncNow: () -> Unit,
    onGoogleSignInSuccess: () -> Unit,
    onFeedbackMessage: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) {
        try {
            CredentialManager.create(context)
        } catch (_: Throwable) {
            null
        }
    }
    val configStatus = remember(context) { GoogleAuthManager.checkConfiguration(context) }
    var isSigningIn by remember { mutableStateOf(false) }
    var showMissingConfigDialog by remember { mutableStateOf(false) }

    val timeText = remember(syncState.lastSyncedAtMillis) {
        syncState.lastSyncedAtMillis?.let { ts ->
            SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(ts))
        }
    }

    val (bgColor, borderColor, iconTint, titleColor) = when (syncState.status) {
        CloudSyncStatus.SYNCED -> listOf(
            Color(0xFFD1FAE5),
            Color(0xFF16A34A),
            Color(0xFF065F46),
            Color(0xFF065F46)
        )
        CloudSyncStatus.SYNCING -> listOf(
            Color(0xFFDBEAFE),
            Color(0xFF2563EB),
            Color(0xFF1D4ED8),
            Color(0xFF1E3A8A)
        )
        CloudSyncStatus.OFFLINE_PENDING -> listOf(
            Color(0xFFFEF3C7),
            Color(0xFFD97706),
            Color(0xFF92400E),
            Color(0xFF78350F)
        )
        CloudSyncStatus.ERROR -> listOf(
            Color(0xFFFEE2E2),
            Color(0xFFDC2626),
            Color(0xFF991B1B),
            Color(0xFF991B1B)
        )
        CloudSyncStatus.NEEDS_GOOGLE_SIGN_IN -> listOf(
            Color(0xFFE6E9EE),
            Color(0xFF1D4ED8),
            Color(0xFF1D4ED8),
            Color(0xFF0F172A)
        )
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(1.5.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("cloud_auto_sync_banner")
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
                            .background(Color.White.copy(alpha = 0.7f))
                            .border(1.dp, borderColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        when (syncState.status) {
                            CloudSyncStatus.SYNCING -> CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = iconTint,
                                strokeWidth = 2.5.dp
                            )
                            CloudSyncStatus.SYNCED -> Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = "অটো সিঙ্ক চালু",
                                tint = iconTint,
                                modifier = Modifier.size(22.dp)
                            )
                            CloudSyncStatus.OFFLINE_PENDING -> Icon(
                                imageVector = Icons.Default.CloudOff,
                                contentDescription = "অফলাইন অপেক্ষমাণ",
                                tint = iconTint,
                                modifier = Modifier.size(22.dp)
                            )
                            CloudSyncStatus.ERROR -> Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = "সিঙ্ক ত্রুটি",
                                tint = iconTint,
                                modifier = Modifier.size(22.dp)
                            )
                            CloudSyncStatus.NEEDS_GOOGLE_SIGN_IN -> Icon(
                                imageVector = Icons.Default.CloudQueue,
                                contentDescription = "Google অটো সিঙ্ক",
                                tint = iconTint,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when (syncState.status) {
                                CloudSyncStatus.SYNCED -> "ক্লাউড অটো-সিঙ্ক সক্রিয়"
                                CloudSyncStatus.SYNCING -> "ক্লাউড অটো-সিঙ্ক চলছে..."
                                CloudSyncStatus.OFFLINE_PENDING -> "অফলাইন মোড (নেট এলেই সিঙ্ক হবে)"
                                CloudSyncStatus.ERROR -> "সিঙ্ক করতে সমস্যা হয়েছে"
                                CloudSyncStatus.NEEDS_GOOGLE_SIGN_IN -> "ক্লাউড অটো-সিঙ্ক সংযুক্ত করুন"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = titleColor
                        )
                        Text(
                            text = when (syncState.status) {
                                CloudSyncStatus.SYNCED -> buildString {
                                    if (!syncState.accountEmail.isNullOrBlank()) {
                                        append(syncState.accountEmail)
                                        append(" • ")
                                    }
                                    append("মোট ${syncState.syncedRecordCount}টি রেকর্ড")
                                    if (timeText != null) append(" • $timeText")
                                }
                                CloudSyncStatus.SYNCING -> "সদস্য, সঞ্চয় ও কিস্তি ক্লাউডে সিঙ্ক হচ্ছে..."
                                CloudSyncStatus.OFFLINE_PENDING -> "লোকাল ডাটা সুরক্ষিত আছে; ইন্টারনেট সংযোগ পেলেই অটো আপলোড হবে।"
                                CloudSyncStatus.ERROR -> syncState.errorMessage ?: "ইন্টারনেট সংযোগ পরীক্ষা করে আবার চেষ্টা করুন।"
                                CloudSyncStatus.NEEDS_GOOGLE_SIGN_IN -> "সব ডিভাইসে স্বয়ংক্রিয় ব্যাকআপ ও সিঙ্ক পেতে Google (${GoogleAuthManager.SAMITI_GOOGLE_EMAIL}) দিয়ে সাইন-ইন করুন।"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                    }
                }
            }

            if (syncState.status == CloudSyncStatus.NEEDS_GOOGLE_SIGN_IN && !configStatus.isReady) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFEF3C7),
                    border = BorderStroke(1.dp, Color(0xFFD97706)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "কনফিগারেশন তথ্য",
                            tint = Color(0xFF92400E),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "এই APK-তে google-services.json নেই — সমাধান দেখতে বাটনে ট্যাপ করুন।",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF78350F)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (syncState.status == CloudSyncStatus.NEEDS_GOOGLE_SIGN_IN) {
                Button(
                    onClick = {
                        if (!configStatus.isReady) {
                            showMissingConfigDialog = true
                            return@Button
                        }
                        val cm = credentialManager
                        if (cm == null) {
                            onFeedbackMessage("এই ডিভাইসে Google সাইন-ইন সেবা পাওয়া যায়নি")
                            return@Button
                        }
                        isSigningIn = true
                        GoogleAuthManager.onGoogleSignInClicked(
                            context = context,
                            credentialManager = cm,
                            scope = scope,
                            onAuthSuccess = {
                                isSigningIn = false
                                onGoogleSignInSuccess()
                                onFeedbackMessage("Google ক্লাউড অটো-সিঙ্ক সফলভাবে চালু হয়েছে!")
                            },
                            onAuthError = { msg ->
                                isSigningIn = false
                                onFeedbackMessage(msg)
                            },
                            onAuthCancelled = {
                                isSigningIn = false
                            }
                        )
                    },
                    enabled = !isSigningIn,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1D4ED8),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_google_signin_sync")
                ) {
                    if (isSigningIn) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("সংযুক্ত হচ্ছে...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sign in with Google (অটো সিঙ্ক চালু করুন)", fontWeight = FontWeight.ExtraBold)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.7f)
                    ) {
                        Text(
                            text = if (syncState.isAutoSyncEnabled) "স্বয়ংক্রিয় রিয়েল-টাইম সিঙ্ক: চালু" else "স্বয়ংক্রিয় সিঙ্ক: বন্ধ",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = titleColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    OutlinedButton(
                        onClick = onSyncNow,
                        enabled = syncState.status != CloudSyncStatus.SYNCING,
                        border = BorderStroke(1.2.dp, borderColor),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("btn_manual_sync_now")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "এখনই সিঙ্ক",
                            tint = titleColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "এখনই সিঙ্ক",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = titleColor
                        )
                    }
                }
            }
        }
    }

    if (showMissingConfigDialog) {
        AlertDialog(
            onDismissRequest = { showMissingConfigDialog = false },
            title = {
                Text(
                    text = "Google Sign-In সক্রিয় করার নির্দেশনা",
                    fontWeight = FontWeight.ExtraBold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "এই APK ফাইলে google-services.json যুক্ত না থাকায় Google Sign-In এখন বন্ধ আছে (GitHub থেকে বিল্ড করা হলে এটি আলাদাভাবে যোগ করতে হয়)।",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "সমাধান (যেকোনো ১টি অনুসরণ করুন):",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1D4ED8)
                    )
                    Text(
                        text = "১) সরাসরি AI Studio-এর Settings মেনু থেকে APK জেনারেট করে ইনস্টল করুন — সেখানে কনফিগারেশন স্বয়ংক্রিয়ভাবে যুক্ত থাকে।",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "২) অথবা GitHub রিপোজিটরি → Settings → Secrets and variables → Actions → New repository secret-এ GOOGLE_SERVICES_JSON নামে app/google-services.json-এর কনটেন্ট যোগ করে পুনরায় APK বিল্ড করুন।",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showMissingConfigDialog = false },
                    modifier = Modifier.testTag("btn_close_google_config_help")
                ) {
                    Text("বুঝেছি", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
