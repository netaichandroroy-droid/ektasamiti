package com.example.data.local

import android.content.Context

class SessionPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveLoggedInSession(userId: Long, email: String) {
        prefs.edit()
            .putLong(KEY_USER_ID, userId)
            .putString(KEY_USER_EMAIL, email)
            .commit()
    }

    fun getLoggedInUserId(): Long? {
        val id = prefs.getLong(KEY_USER_ID, -1L)
        return if (id > 0L) id else null
    }

    fun getLoggedInUserEmail(): String? {
        return prefs.getString(KEY_USER_EMAIL, null)
    }

    fun clearSession() {
        prefs.edit()
            .remove(KEY_USER_ID)
            .remove(KEY_USER_EMAIL)
            .commit()
    }

    companion object {
        private const val PREFS_NAME = "ekta_samiti_session"
        private const val KEY_USER_ID = "logged_in_user_id"
        private const val KEY_USER_EMAIL = "logged_in_user_email"
    }
}
