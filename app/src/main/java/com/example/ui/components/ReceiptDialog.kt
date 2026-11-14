package com.example.ui.components

import android.content.Context
import android.content.Intent
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.SilverCardAltBg
import com.example.ui.theme.SilverCardBg
import com.example.ui.theme.SilverCardBorder
import com.example.ui.viewmodel.ReceiptData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReceiptDialog(
    receipt: ReceiptData,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
    val formattedDate = sdf.format(Date(receipt.timestamp))

    val shareText = buildString {
        append("==== একতা সমিতি মানি রসিদ ====\n")
        append("রসিদ নং: ${receipt.receiptNumber}\n")
        append("সদস্য: ${receipt.memberName} (${receipt.memberCode})\n")
        append("লেনদেন: ${receipt.transactionType}\n")
        append("টাকা: ৳${receipt.amount.toInt()}\n")
        append("বর্তমান স্থিতি/বকেয়া: ৳${receipt.balanceOrDue.toInt()}\n")
        append("সংগ্রাহক: ${receipt.collectedBy}\n")
        append("তারিখ: $formattedDate\n")
        if (receipt.notes.isNotBlank()) append("মন্তব্য: ${receipt.notes}\n")
        append("============================\n")
        append("ধন্যবাদ - একতা সমিতি")
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SilverCardBg),
            border = BorderStroke(1.5.dp, Color(0xFF0D683B)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("receipt_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Prominent Success Banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFD1F4DE),
                    border = BorderStroke(1.dp, Color(0xFF15803D)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "সফল",
                                tint = Color(0xFF0D683B),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "সফলভাবে সাবমিট হয়েছে!",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF064E3B)
                            )
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF0F172A))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "একতা সঞ্চয় ও ঋণ সমিতি",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0D683B)
                )
                Text(
                    text = receipt.title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = "রসিদ নং: ${receipt.receiptNumber}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF334155)
                )

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = SilverCardBorder)
                Spacer(modifier = Modifier.height(10.dp))

                // Amount Highlight Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SilverCardAltBg)
                        .border(1.dp, SilverCardBorder, RoundedCornerShape(12.dp))
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "আদায়ের পরিমাণ",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF334155)
                        )
                        Text(
                            text = "৳ ${receipt.amount.toInt()}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0D683B)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Details Rows
                ReceiptRow(label = "সদস্যের নাম:", value = receipt.memberName)
                ReceiptRow(label = "সদস্য আইডি:", value = receipt.memberCode)
                ReceiptRow(label = "লেনদেনের ধরণ:", value = receipt.transactionType)
                ReceiptRow(label = "বর্তমান ব্যালেন্স/বকেয়া:", value = "৳ ${receipt.balanceOrDue.toInt()}")
                ReceiptRow(label = "সংগ্রাহক / প্রতিনিধি:", value = receipt.collectedBy)
                ReceiptRow(label = "তারিখ ও সময়:", value = formattedDate)

                if (receipt.notes.isNotBlank()) {
                    ReceiptRow(label = "মন্তব্য:", value = receipt.notes)
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = SilverCardBorder)
                Spacer(modifier = Modifier.height(12.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val sendIntent: Intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, "রসিদ পাঠান")
                            context.startActivity(shareIntent)
                        },
                        border = BorderStroke(1.2.dp, Color(0xFF0D683B)),
                        modifier = Modifier.weight(1f).testTag("btn_share_receipt")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFF0D683B), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("শেয়ার", color = Color(0xFF0D683B), fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).testTag("btn_done_receipt"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0D683B),
                            contentColor = Color.White
                        )
                    ) {
                        Text("ঠিক আছে", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

@Composable
fun ReceiptRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF334155)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF0F172A),
            textAlign = TextAlign.End
        )
    }
}
