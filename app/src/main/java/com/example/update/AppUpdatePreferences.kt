package com.example.update

import android.content.Context
import com.example.BuildConfig

object AppUpdatePreferences {
    private const val PREFS_NAME = "ekta_samiti_app_updates"
    private const val KEY_AUTO_CHECK_ENABLED = "auto_check_enabled"
    private const val KEY_CUSTOM_GITHUB_REPO = "custom_github_repo"
    private const val KEY_DISMISSED_TAG = "dismissed_tag"

    fun isAutoCheckEnabled(context: Context): Boolean {
        return try {
            context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_AUTO_CHECK_ENABLED, true)
        } catch (_: Exception) {
            true
        }
    }

    fun setAutoCheckEnabled(context: Context, enabled: Boolean) {
        try {
            context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_AUTO_CHECK_ENABLED, enabled)
                .apply()
        } catch (_: Exception) {
        }
    }

    fun getCustomRepository(context: Context): String {
        return try {
            context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_CUSTOM_GITHUB_REPO, "")
                .orEmpty()
        } catch (_: Exception) {
            ""
        }
    }

    fun saveCustomRepository(context: Context, rawInput: String) {
        val normalized = normalizeGithubRepo(rawInput)
        try {
            context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_CUSTOM_GITHUB_REPO, normalized)
                .apply()
        } catch (_: Exception) {
        }
    }

    fun resolveEffectiveRepository(context: Context): String {
        val custom = normalizeGithubRepo(getCustomRepository(context))
        if (custom.isNotBlank()) return custom
        return normalizeGithubRepo(BuildConfig.GITHUB_REPOSITORY)
    }

    fun getDismissedTag(context: Context): String? {
        return try {
            context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_DISMISSED_TAG, null)
        } catch (_: Exception) {
            null
        }
    }

    fun setDismissedTag(context: Context, tag: String) {
        try {
            context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_DISMISSED_TAG, tag)
                .apply()
        } catch (_: Exception) {
        }
    }

    /**
     * Accepts either "owner/repo" or "https://github.com/owner/repo(.git)" and returns "owner/repo".
     * Returns "" if invalid.
     */
    fun normalizeGithubRepo(rawInput: String): String {
        val cleaned = rawInput.trim()
            .removePrefix("https://github.com/")
            .removePrefix("http://github.com/")
            .removePrefix("github.com/")
            .trim('/')
            .removeSuffix(".git")
        val parts = cleaned.split("/").filter { it.isNotBlank() }
        if (parts.size < 2) return ""
        val owner = parts[0]
        val repo = parts[1]
        val validRegex = Regex("^[A-Za-z0-9._-]+$")
        return if (validRegex.matches(owner) && validRegex.matches(repo)) {
            "$owner/$repo"
        } else {
            ""
        }
    }
}
