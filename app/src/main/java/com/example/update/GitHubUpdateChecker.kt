package com.example.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class GitHubReleaseInfo(
    val tagName: String,
    val releaseName: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val releasePageUrl: String
)

sealed class UpdateCheckResult {
    data class UpdateAvailable(val release: GitHubReleaseInfo) : UpdateCheckResult()
    data class UpToDate(val currentVersion: String, val latestTag: String?) : UpdateCheckResult()
    data object RepoNotConfigured : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}

object GitHubUpdateChecker {
    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .build()
    }

    suspend fun checkForUpdate(
        context: Context,
        repoOverride: String? = null
    ): UpdateCheckResult = withContext(Dispatchers.IO) {
        val repo = repoOverride?.let { AppUpdatePreferences.normalizeGithubRepo(it) }
            ?: AppUpdatePreferences.resolveEffectiveRepository(context)
        if (repo.isBlank()) {
            return@withContext UpdateCheckResult.RepoNotConfigured
        }

        val apiUrl = "https://api.github.com/repos/$repo/releases/latest"
        try {
            val request = Request.Builder()
                .url(apiUrl)
                .header("Accept", "application/vnd.github+json")
                .header("Cache-Control", "no-cache")
                .header("Pragma", "no-cache")
                .header("User-Agent", "EktaSamitiAndroid/${BuildConfig.VERSION_NAME}")
                .build()

            val latestResult = httpClient.newCall(request).execute().use { response ->
                if (response.code == 404) {
                    return@use UpdateCheckResult.Error(
                        "রিপোজিটরিতে এখনো কোনো GitHub Release পাওয়া যায়নি ($repo)।"
                    )
                }
                if (!response.isSuccessful) {
                    return@use UpdateCheckResult.Error(
                        "GitHub আপডেট যাচাই করা যায়নি (HTTP ${response.code})।"
                    )
                }
                val bodyString = response.body?.string().orEmpty()
                if (bodyString.isBlank()) {
                    return@use UpdateCheckResult.Error("সার্ভার থেকে খালি উত্তর এসেছে।")
                }
                parseLatestReleaseJson(
                    jsonString = bodyString,
                    currentVersionCode = BuildConfig.VERSION_CODE,
                    currentVersionName = BuildConfig.VERSION_NAME
                )
            }

            if (latestResult is UpdateCheckResult.UpdateAvailable) {
                return@withContext latestResult
            }

            // Fallback check against recent releases list in case GitHub /releases/latest cache lags
            val listUrl = "https://api.github.com/repos/$repo/releases?per_page=5"
            val listRequest = Request.Builder()
                .url(listUrl)
                .header("Accept", "application/vnd.github+json")
                .header("Cache-Control", "no-cache")
                .header("Pragma", "no-cache")
                .header("User-Agent", "EktaSamitiAndroid/${BuildConfig.VERSION_NAME}")
                .build()
            val listFallback = runCatching {
                httpClient.newCall(listRequest).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val listBody = resp.body?.string().orEmpty()
                        if (listBody.isNotBlank()) {
                            parseReleasesListJson(
                                jsonArrayString = listBody,
                                currentVersionCode = BuildConfig.VERSION_CODE,
                                currentVersionName = BuildConfig.VERSION_NAME
                            )
                        } else null
                    } else null
                }
            }.getOrNull()

            if (listFallback is UpdateCheckResult.UpdateAvailable) {
                return@withContext listFallback
            }

            latestResult
        } catch (e: Exception) {
            UpdateCheckResult.Error("ইন্টারনেট সংযোগ না থাকায় আপডেট চেক করা যায়নি।")
        }
    }

    fun parseLatestReleaseJson(
        jsonString: String,
        currentVersionCode: Int,
        currentVersionName: String
    ): UpdateCheckResult {
        return try {
            val root = JSONObject(jsonString)
            val tagName = root.optString("tag_name").trim()
            val releaseName = root.optString("name").trim().ifBlank { tagName }
            val releaseNotes = root.optString("body").trim()
            val htmlUrl = root.optString("html_url").trim()

            var apkDownloadUrl: String? = null
            var fallbackApkUrl: String? = null
            val assets = root.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.optJSONObject(i) ?: continue
                    val assetName = asset.optString("name").lowercase()
                    val browserUrl = asset.optString("browser_download_url").trim()
                    if (assetName == "ektasamiti.apk" && browserUrl.startsWith("https://")) {
                        apkDownloadUrl = browserUrl
                        break
                    } else if (fallbackApkUrl == null && assetName.endsWith(".apk") && browserUrl.startsWith("https://")) {
                        fallbackApkUrl = browserUrl
                    }
                }
            }

            val finalUrl = apkDownloadUrl ?: fallbackApkUrl ?: htmlUrl
            if (tagName.isBlank() || !finalUrl.startsWith("https://")) {
                return UpdateCheckResult.Error("সঠিক রিলিজ ডাউনলোড লিংক পাওয়া যায়নি।")
            }

            if (isNewerRelease(tagName, currentVersionCode, currentVersionName)) {
                UpdateCheckResult.UpdateAvailable(
                    GitHubReleaseInfo(
                        tagName = tagName,
                        releaseName = releaseName,
                        releaseNotes = releaseNotes,
                        downloadUrl = finalUrl,
                        releasePageUrl = if (htmlUrl.startsWith("https://")) htmlUrl else finalUrl
                    )
                )
            } else {
                UpdateCheckResult.UpToDate(
                    currentVersion = currentVersionName,
                    latestTag = tagName
                )
            }
        } catch (_: Exception) {
            UpdateCheckResult.Error("রিলিজ তথ্য পড়তে সমস্যা হয়েছে।")
        }
    }

    fun parseReleasesListJson(
        jsonArrayString: String,
        currentVersionCode: Int,
        currentVersionName: String
    ): UpdateCheckResult? {
        return try {
            val releases = JSONArray(jsonArrayString)
            var bestCandidate: GitHubReleaseInfo? = null
            for (i in 0 until releases.length()) {
                val item = releases.optJSONObject(i) ?: continue
                if (item.optBoolean("draft", false) || item.optBoolean("prerelease", false)) {
                    continue
                }
                val parsed = parseLatestReleaseJson(
                    jsonString = item.toString(),
                    currentVersionCode = currentVersionCode,
                    currentVersionName = currentVersionName
                )
                if (parsed is UpdateCheckResult.UpdateAvailable) {
                    val existing = bestCandidate
                    if (existing == null || compareVersionTags(parsed.release.tagName, existing.tagName) > 0) {
                        bestCandidate = parsed.release
                    }
                }
            }
            bestCandidate?.let { UpdateCheckResult.UpdateAvailable(it) }
        } catch (_: Exception) {
            null
        }
    }

    private fun compareVersionTags(tagA: String, tagB: String): Int {
        val partsA = extractNumericParts(tagA)
        val partsB = extractNumericParts(tagB)
        val maxLen = maxOf(partsA.size, partsB.size)
        for (i in 0 until maxLen) {
            val a = partsA.getOrElse(i) { 0 }
            val b = partsB.getOrElse(i) { 0 }
            if (a != b) return a.compareTo(b)
        }
        return 0
    }

    fun isNewerRelease(
        remoteTag: String,
        currentVersionCode: Int,
        currentVersionName: String
    ): Boolean {
        val remoteParts = extractNumericParts(remoteTag)
        val localParts = extractNumericParts(currentVersionName)
        if (remoteParts.isEmpty()) return false

        // Compare semantic parts first (e.g., 1.0.15 vs 1.0.12)
        val maxLen = maxOf(remoteParts.size, localParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val l = localParts.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false
        }

        // Also compare final build number against currentVersionCode when format is e.g. 1.0.<run_number>
        val remoteBuildNumber = remoteParts.lastOrNull() ?: 0
        return remoteParts.size >= 3 && remoteBuildNumber > currentVersionCode
    }

    private fun extractNumericParts(versionText: String): List<Int> {
        return Regex("\\d+")
            .findAll(versionText)
            .mapNotNull { it.value.toIntOrNull() }
            .toList()
    }

    fun permanentApkDownloadUrl(repoInput: String): String {
        val normalized = AppUpdatePreferences.normalizeGithubRepo(repoInput)
        if (normalized.isBlank()) return ""
        return "https://github.com/$normalized/releases/latest/download/EktaSamiti.apk"
    }

    fun openUpdateDownloadUrl(context: Context, url: String): Boolean {
        return try {
            if (!url.startsWith("https://")) return false
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }
}
