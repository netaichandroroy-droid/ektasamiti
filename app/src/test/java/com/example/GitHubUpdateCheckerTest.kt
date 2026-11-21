package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.ui.components.AppUpdateAvailableDialog
import com.example.ui.theme.EktaSamitiTheme
import com.example.update.AppUpdatePreferences
import com.example.update.GitHubReleaseInfo
import com.example.update.GitHubUpdateChecker
import com.example.update.UpdateCheckResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GitHubUpdateCheckerTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun normalizeGithubRepo_parsesUrlsAndSlugsCorrectly() {
        assertEquals("nitai/ekta-samiti", AppUpdatePreferences.normalizeGithubRepo("nitai/ekta-samiti"))
        assertEquals(
            "nitai/ekta-samiti",
            AppUpdatePreferences.normalizeGithubRepo("https://github.com/nitai/ekta-samiti")
        )
        assertEquals(
            "nitai/ekta-samiti",
            AppUpdatePreferences.normalizeGithubRepo("https://github.com/nitai/ekta-samiti.git")
        )
        assertEquals(
            "nitai/ekta-samiti",
            AppUpdatePreferences.normalizeGithubRepo("github.com/nitai/ekta-samiti/")
        )
        assertEquals("", AppUpdatePreferences.normalizeGithubRepo("only-owner"))
        assertEquals("", AppUpdatePreferences.normalizeGithubRepo(""))
    }

    @Test
    fun isNewerRelease_comparesTagsAndVersionCodesAccurately() {
        assertTrue(GitHubUpdateChecker.isNewerRelease("v1.0.5", 1, "1.0"))
        assertTrue(GitHubUpdateChecker.isNewerRelease("v1.0.6", 5, "1.0.5"))
        assertTrue(GitHubUpdateChecker.isNewerRelease("v1.1.0", 9, "1.0.9"))
        assertFalse(GitHubUpdateChecker.isNewerRelease("v1.0.5", 5, "1.0.5"))
        assertFalse(GitHubUpdateChecker.isNewerRelease("v1.0.4", 5, "1.0.5"))
        assertFalse(GitHubUpdateChecker.isNewerRelease("invalid-tag", 1, "1.0"))
    }

    @Test
    fun parseLatestReleaseJson_prefersApkAssetUrlAndDetectsUpdate() {
        val sampleJson = """
            {
              "tag_name": "v1.0.8",
              "name": "Ekta Samiti v1.0.8",
              "body": "নতুন আপডেট ও বাগ ফিক্স",
              "html_url": "https://github.com/nitai/ekta-samiti/releases/tag/v1.0.8",
              "assets": [
                {
                  "name": "EktaSamiti-v1.0.8.apk",
                  "browser_download_url": "https://github.com/nitai/ekta-samiti/releases/download/v1.0.8/EktaSamiti-v1.0.8.apk"
                }
              ]
            }
        """.trimIndent()

        val availableResult = GitHubUpdateChecker.parseLatestReleaseJson(
            jsonString = sampleJson,
            currentVersionCode = 5,
            currentVersionName = "1.0.5"
        )
        assertTrue(availableResult is UpdateCheckResult.UpdateAvailable)
        val release = (availableResult as UpdateCheckResult.UpdateAvailable).release
        assertEquals("v1.0.8", release.tagName)
        assertEquals(
            "https://github.com/nitai/ekta-samiti/releases/download/v1.0.8/EktaSamiti-v1.0.8.apk",
            release.downloadUrl
        )

        val upToDateResult = GitHubUpdateChecker.parseLatestReleaseJson(
            jsonString = sampleJson,
            currentVersionCode = 8,
            currentVersionName = "1.0.8"
        )
        assertTrue(upToDateResult is UpdateCheckResult.UpToDate)
    }

    @Test
    fun appUpdateAvailableDialog_triggersDownloadAndDismissCallbacks() {
        var dismissed = false
        var downloadedUrl: String? = null
        val release = GitHubReleaseInfo(
            tagName = "v1.0.10",
            releaseName = "Ekta Samiti v1.0.10",
            releaseNotes = "অটো আপডেট সমর্থন",
            downloadUrl = "https://github.com/nitai/ekta-samiti/releases/download/v1.0.10/EktaSamiti-v1.0.10.apk",
            releasePageUrl = "https://github.com/nitai/ekta-samiti/releases/tag/v1.0.10"
        )

        composeRule.setContent {
            EktaSamitiTheme {
                AppUpdateAvailableDialog(
                    release = release,
                    onDismiss = { dismissed = true },
                    onDownloadClick = { downloadedUrl = it }
                )
            }
        }

        composeRule.onNodeWithTag("dialog_app_update_available").assertIsDisplayed()
        composeRule.onNodeWithTag("btn_open_update_download").performClick()
        assertEquals(release.downloadUrl, downloadedUrl)

        composeRule.onNodeWithTag("btn_dismiss_update").performClick()
        assertTrue(dismissed)
    }

    @Test
    fun permanentApkDownloadUrl_andEktaSamitiAssetPreference_workConsistently() {
        assertEquals(
            "https://github.com/nitai/ekta-samiti/releases/latest/download/EktaSamiti.apk",
            GitHubUpdateChecker.permanentApkDownloadUrl("https://github.com/nitai/ekta-samiti.git")
        )
        assertEquals("", GitHubUpdateChecker.permanentApkDownloadUrl("invalid"))

        val releaseJson = """
            {
              "tag_name": "v1.0.28018680",
              "name": "Ekta Samiti v1.0.28018680",
              "body": "Permanent asset release",
              "html_url": "https://github.com/nitai/ekta-samiti/releases/tag/v1.0.28018680",
              "assets": [
                {
                  "name": "EktaSamiti-v1.0.28018680.apk",
                  "browser_download_url": "https://github.com/nitai/ekta-samiti/releases/download/v1.0.28018680/EktaSamiti-v1.0.28018680.apk"
                },
                {
                  "name": "EktaSamiti.apk",
                  "browser_download_url": "https://github.com/nitai/ekta-samiti/releases/download/v1.0.28018680/EktaSamiti.apk"
                }
              ]
            }
        """.trimIndent()

        val result = GitHubUpdateChecker.parseLatestReleaseJson(
            jsonString = releaseJson,
            currentVersionCode = 5,
            currentVersionName = "1.0.5"
        )
        assertTrue(result is UpdateCheckResult.UpdateAvailable)
        val info = (result as UpdateCheckResult.UpdateAvailable).release
        assertEquals(
            "https://github.com/nitai/ekta-samiti/releases/download/v1.0.28018680/EktaSamiti.apk",
            info.downloadUrl
        )
    }

    @Test
    fun parseReleasesListJson_picksHighestPublishedReleaseIgnoringDrafts() {
        val listJson = """
            [
              {
                "tag_name": "v1.0.99999",
                "draft": true,
                "prerelease": false,
                "html_url": "https://github.com/nitai/ekta-samiti/releases/tag/v1.0.99999",
                "assets": []
              },
              {
                "tag_name": "v1.0.5",
                "draft": false,
                "prerelease": false,
                "html_url": "https://github.com/nitai/ekta-samiti/releases/tag/v1.0.5",
                "assets": [
                  {
                    "name": "EktaSamiti.apk",
                    "browser_download_url": "https://github.com/nitai/ekta-samiti/releases/download/v1.0.5/EktaSamiti.apk"
                  }
                ]
              },
              {
                "tag_name": "v1.0.9117000",
                "draft": false,
                "prerelease": false,
                "html_url": "https://github.com/nitai/ekta-samiti/releases/tag/v1.0.9117000",
                "assets": [
                  {
                    "name": "EktaSamiti.apk",
                    "browser_download_url": "https://github.com/nitai/ekta-samiti/releases/download/v1.0.9117000/EktaSamiti.apk"
                  }
                ]
              }
            ]
        """.trimIndent()

        val result = GitHubUpdateChecker.parseReleasesListJson(
            jsonArrayString = listJson,
            currentVersionCode = 5,
            currentVersionName = "1.0.5"
        )
        assertTrue(result is UpdateCheckResult.UpdateAvailable)
        assertEquals("v1.0.9117000", (result as UpdateCheckResult.UpdateAvailable).release.tagName)
    }
}
