package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.cloud.CloudAutoSyncManager
import com.example.data.cloud.FirebaseSafeHelper
import com.example.data.cloud.FirestoreSamitiSyncSource
import com.example.data.local.AppDatabase
import com.example.notifications.LoanReminderNotificationHelper
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class StartupCrashResilienceTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `manifest declares valid INTERNET and ACCESS_NETWORK_STATE permissions`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS
        )
        val requested = packageInfo.requestedPermissions?.toList().orEmpty()
        assertTrue(
            "Expected android.permission.INTERNET in manifest",
            requested.contains(Manifest.permission.INTERNET)
        )
        assertTrue(
            "Expected android.permission.ACCESS_NETWORK_STATE in manifest",
            requested.contains(Manifest.permission.ACCESS_NETWORK_STATE)
        )
    }

    @Test
    fun `mainActivity launches to login screen cleanly without Firebase crash`() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("btn_login").assertIsDisplayed()
        composeRule.onNodeWithTag("input_email").assertIsDisplayed()
        composeRule.onNodeWithTag("input_password").assertIsDisplayed()
        composeRule.onNodeWithTag("btn_google_sign_in").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("text_samiti_google_account").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `cloudSync and notification helpers degrade gracefully without throwing`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val syncSource = FirestoreSamitiSyncSource(context)
            assertNotNull(syncSource)

            val syncManager = CloudAutoSyncManager(context, db, syncSource)
            syncManager.triggerAutoSyncNow()
            syncManager.stop()

            // Safe Firebase lookup must not throw even when FirebaseApp is not initialized
            FirebaseSafeHelper.authOrNull(context)
            FirebaseSafeHelper.firestoreOrNull(context)

            // Notification channel and daily reminder scheduling must not throw SecurityException
            LoanReminderNotificationHelper.scheduleDailyReminder(context)
            LoanReminderNotificationHelper.cancelDailyReminder(context)
        } finally {
            db.close()
        }
    }
}
