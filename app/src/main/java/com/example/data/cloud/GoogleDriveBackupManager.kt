package com.example.data.cloud

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class GoogleDriveState(
    val isConnected: Boolean = false,
    val accountEmail: String? = null,
    val accountName: String? = null,
    val lastBackupAtMillis: Long? = null,
    val lastBackupRecordCount: Int = 0,
    val lastBackupFileName: String = GoogleDriveBackupManager.BACKUP_FILE_NAME,
    val isBusy: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null
)

sealed interface DriveAuthOutcome {
    data class Authorized(val accessToken: String) : DriveAuthOutcome
    data class NeedsConsent(val pendingIntent: PendingIntent) : DriveAuthOutcome
    data class Error(val message: String) : DriveAuthOutcome
}

data class DriveBackupUploadResult(
    val fileId: String,
    val fileName: String,
    val uploadedAtMillis: Long
)

data class DriveBackupDownloadResult(
    val fileId: String,
    val fileName: String,
    val modifiedTimeText: String?,
    val jsonContent: String
)

class GoogleDriveBackupManager(
    context: Context,
    private val httpClient: OkHttpClient = defaultHttpClient()
) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(loadSavedState())
    val state: StateFlow<GoogleDriveState> = _state.asStateFlow()

    private fun loadSavedState(): GoogleDriveState {
        val isConnected = prefs.getBoolean(KEY_CONNECTED, false)
        val email = prefs.getString(KEY_EMAIL, null)?.takeIf { it.isNotBlank() }
        val name = prefs.getString(KEY_NAME, null)?.takeIf { it.isNotBlank() }
        val lastAt = prefs.getLong(KEY_LAST_BACKUP_MILLIS, 0L).takeIf { it > 0L }
        val lastCount = prefs.getInt(KEY_LAST_RECORD_COUNT, 0)
        return GoogleDriveState(
            isConnected = isConnected,
            accountEmail = email,
            accountName = name,
            lastBackupAtMillis = lastAt,
            lastBackupRecordCount = lastCount
        )
    }

    fun setBusy(busy: Boolean, statusMessage: String? = null) {
        _state.value = _state.value.copy(
            isBusy = busy,
            statusMessage = statusMessage,
            errorMessage = if (busy) null else _state.value.errorMessage
        )
    }

    fun setError(message: String) {
        _state.value = _state.value.copy(
            isBusy = false,
            statusMessage = null,
            errorMessage = message
        )
    }

    suspend fun requestDriveAuthorization(activity: Activity): DriveAuthOutcome {
        return try {
            val request = AuthorizationRequest.builder()
                .setRequestedScopes(listOf(Scope(DRIVE_FILE_SCOPE)))
                .build()
            val result = Identity.getAuthorizationClient(activity)
                .authorize(request)
                .await()
            val token = result.accessToken
            when {
                result.hasResolution() && result.pendingIntent != null -> {
                    DriveAuthOutcome.NeedsConsent(result.pendingIntent!!)
                }
                !token.isNullOrBlank() -> {
                    DriveAuthOutcome.Authorized(token)
                }
                else -> {
                    DriveAuthOutcome.Error("Google Drive অ্যাক্সেস টোকেন পাওয়া যায়নি")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Google Drive authorization request failed", e)
            DriveAuthOutcome.Error(mapAuthorizationExceptionMessage(e))
        }
    }

    fun extractTokenFromConsentResult(activity: Activity, data: Intent?): DriveAuthOutcome {
        return try {
            val authResult = Identity.getAuthorizationClient(activity)
                .getAuthorizationResultFromIntent(data)
            val token = authResult.accessToken
            if (!token.isNullOrBlank()) {
                DriveAuthOutcome.Authorized(token)
            } else {
                DriveAuthOutcome.Error("Google Drive অনুমোদন সম্পন্ন হয়নি")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Google Drive authorization intent parsing failed", e)
            DriveAuthOutcome.Error(mapAuthorizationExceptionMessage(e))
        }
    }

    suspend fun markConnectedWithToken(accessToken: String): Result<GoogleDriveState> {
        return withContext(Dispatchers.IO) {
            try {
                val (email, displayName) = fetchDriveUserProfile(accessToken)
                prefs.edit()
                    .putBoolean(KEY_CONNECTED, true)
                    .putString(KEY_EMAIL, email ?: "")
                    .putString(KEY_NAME, displayName ?: "")
                    .apply()
                val updated = _state.value.copy(
                    isConnected = true,
                    accountEmail = email ?: _state.value.accountEmail,
                    accountName = displayName ?: _state.value.accountName,
                    isBusy = false,
                    statusMessage = "Google Drive সফলভাবে সংযুক্ত হয়েছে",
                    errorMessage = null
                )
                _state.value = updated
                Result.success(updated)
            } catch (e: Exception) {
                Log.w(TAG, "Connected to Drive but profile fetch had issue: ${e.message}")
                prefs.edit().putBoolean(KEY_CONNECTED, true).apply()
                val updated = _state.value.copy(
                    isConnected = true,
                    isBusy = false,
                    statusMessage = "Google Drive সংযুক্ত হয়েছে",
                    errorMessage = null
                )
                _state.value = updated
                Result.success(updated)
            }
        }
    }

    fun disconnectDrive() {
        prefs.edit()
            .remove(KEY_CONNECTED)
            .remove(KEY_EMAIL)
            .remove(KEY_NAME)
            .remove(KEY_LAST_FILE_ID)
            .apply()
        _state.value = _state.value.copy(
            isConnected = false,
            accountEmail = null,
            accountName = null,
            isBusy = false,
            statusMessage = "Google Drive সংযোগ বিচ্ছিন্ন করা হয়েছে",
            errorMessage = null
        )
    }

    suspend fun uploadBackup(
        accessToken: String,
        jsonContent: String,
        recordCount: Int
    ): Result<DriveBackupUploadResult> = withContext(Dispatchers.IO) {
        try {
            val (email, displayName) = runCatching { fetchDriveUserProfile(accessToken) }
                .getOrElse { null to null }

            val folderId = ensureBackupFolder(accessToken)
            val existingFileId = findExistingBackupFileId(accessToken, folderId)
            val now = System.currentTimeMillis()

            val fileId = if (existingFileId != null) {
                updateDriveFileContent(
                    accessToken = accessToken,
                    fileId = existingFileId,
                    jsonContent = jsonContent
                )
            } else {
                createDriveJsonFile(
                    accessToken = accessToken,
                    parentFolderId = folderId,
                    fileName = BACKUP_FILE_NAME,
                    jsonContent = jsonContent
                )
            }

            prefs.edit()
                .putBoolean(KEY_CONNECTED, true)
                .apply {
                    if (!email.isNullOrBlank()) putString(KEY_EMAIL, email)
                    if (!displayName.isNullOrBlank()) putString(KEY_NAME, displayName)
                }
                .putLong(KEY_LAST_BACKUP_MILLIS, now)
                .putInt(KEY_LAST_RECORD_COUNT, recordCount)
                .putString(KEY_LAST_FILE_ID, fileId)
                .apply()

            _state.value = _state.value.copy(
                isConnected = true,
                accountEmail = email ?: _state.value.accountEmail,
                accountName = displayName ?: _state.value.accountName,
                lastBackupAtMillis = now,
                lastBackupRecordCount = recordCount,
                isBusy = false,
                statusMessage = "Google Drive-এ ব্যাকআপ সফলভাবে সংরক্ষিত হয়েছে",
                errorMessage = null
            )

            Result.success(
                DriveBackupUploadResult(
                    fileId = fileId,
                    fileName = BACKUP_FILE_NAME,
                    uploadedAtMillis = now
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Google Drive backup upload failed", e)
            val msg = e.message ?: "Google Drive-এ ব্যাকআপ আপলোড করা যায়নি"
            _state.value = _state.value.copy(isBusy = false, errorMessage = msg)
            Result.failure(e)
        }
    }

    suspend fun downloadLatestBackup(accessToken: String): Result<DriveBackupDownloadResult> =
        withContext(Dispatchers.IO) {
            try {
                val query = URLEncoder.encode(
                    "name='$BACKUP_FILE_NAME' and trashed=false",
                    "UTF-8"
                )
                val listUrl =
                    "$DRIVE_FILES_URL?q=$query&orderBy=modifiedTime%20desc&pageSize=1&fields=files(id,name,modifiedTime)"
                val listReq = Request.Builder()
                    .url(listUrl)
                    .header("Authorization", "Bearer $accessToken")
                    .get()
                    .build()

                val fileMeta = httpClient.newCall(listReq).execute().use { resp ->
                    val bodyStr = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) {
                        throw IllegalStateException(extractGoogleApiError(resp.code, bodyStr))
                    }
                    val files = JSONObject(bodyStr).optJSONArray("files")
                    if (files == null || files.length() == 0) {
                        throw IllegalStateException("Google Drive-এ কোনো EktaSamiti_Backup.json ফাইল পাওয়া যায়নি")
                    }
                    files.getJSONObject(0)
                }

                val fileId = fileMeta.getString("id")
                val fileName = fileMeta.optString("name", BACKUP_FILE_NAME)
                val modifiedTime = fileMeta.optString("modifiedTime", "").takeIf { it.isNotBlank() }

                val downloadReq = Request.Builder()
                    .url("$DRIVE_FILES_URL/$fileId?alt=media")
                    .header("Authorization", "Bearer $accessToken")
                    .get()
                    .build()

                val jsonContent = httpClient.newCall(downloadReq).execute().use { resp ->
                    val bodyStr = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) {
                        throw IllegalStateException(extractGoogleApiError(resp.code, bodyStr))
                    }
                    bodyStr
                }

                Result.success(
                    DriveBackupDownloadResult(
                        fileId = fileId,
                        fileName = fileName,
                        modifiedTimeText = modifiedTime,
                        jsonContent = jsonContent
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Google Drive backup download failed", e)
                val msg = e.message ?: "Google Drive থেকে ব্যাকআপ ডাউনলোড করা যায়নি"
                _state.value = _state.value.copy(isBusy = false, errorMessage = msg)
                Result.failure(e)
            }
        }

    private fun fetchDriveUserProfile(accessToken: String): Pair<String?, String?> {
        val req = Request.Builder()
            .url(DRIVE_ABOUT_URL)
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()
        httpClient.newCall(req).execute().use { resp ->
            val bodyStr = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw IllegalStateException(extractGoogleApiError(resp.code, bodyStr))
            }
            val user = JSONObject(bodyStr).optJSONObject("user")
            val email = user?.optString("emailAddress", "")?.takeIf { it.isNotBlank() }
            val name = user?.optString("displayName", "")?.takeIf { it.isNotBlank() }
            return email to name
        }
    }

    private fun ensureBackupFolder(accessToken: String): String {
        val query = URLEncoder.encode(
            "name='$BACKUP_FOLDER_NAME' and mimeType='$MIME_FOLDER' and trashed=false",
            "UTF-8"
        )
        val searchReq = Request.Builder()
            .url("$DRIVE_FILES_URL?q=$query&pageSize=1&fields=files(id,name)")
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()

        httpClient.newCall(searchReq).execute().use { resp ->
            val bodyStr = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw IllegalStateException(extractGoogleApiError(resp.code, bodyStr))
            }
            val files = JSONObject(bodyStr).optJSONArray("files")
            if (files != null && files.length() > 0) {
                return files.getJSONObject(0).getString("id")
            }
        }

        val metadata = JSONObject().apply {
            put("name", BACKUP_FOLDER_NAME)
            put("mimeType", MIME_FOLDER)
        }.toString().toRequestBody(MEDIA_TYPE_JSON)

        val createReq = Request.Builder()
            .url("$DRIVE_FILES_URL?fields=id")
            .header("Authorization", "Bearer $accessToken")
            .post(metadata)
            .build()

        httpClient.newCall(createReq).execute().use { resp ->
            val bodyStr = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw IllegalStateException(extractGoogleApiError(resp.code, bodyStr))
            }
            return JSONObject(bodyStr).getString("id")
        }
    }

    private fun findExistingBackupFileId(accessToken: String, folderId: String): String? {
        val query = URLEncoder.encode(
            "name='$BACKUP_FILE_NAME' and '$folderId' in parents and trashed=false",
            "UTF-8"
        )
        val req = Request.Builder()
            .url("$DRIVE_FILES_URL?q=$query&pageSize=1&fields=files(id)")
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()
        httpClient.newCall(req).execute().use { resp ->
            val bodyStr = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) return null
            val files = JSONObject(bodyStr).optJSONArray("files")
            return if (files != null && files.length() > 0) {
                files.getJSONObject(0).getString("id")
            } else {
                null
            }
        }
    }

    private fun createDriveJsonFile(
        accessToken: String,
        parentFolderId: String,
        fileName: String,
        jsonContent: String
    ): String {
        val metadata = JSONObject().apply {
            put("name", fileName)
            put("mimeType", MIME_JSON)
            put("parents", org.json.JSONArray().put(parentFolderId))
        }.toString()

        val multipart = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addPart(metadata.toRequestBody(MEDIA_TYPE_JSON))
            .addPart(jsonContent.toRequestBody(MEDIA_TYPE_JSON))
            .build()

        val req = Request.Builder()
            .url("$DRIVE_UPLOAD_URL?uploadType=multipart&fields=id")
            .header("Authorization", "Bearer $accessToken")
            .post(multipart)
            .build()

        httpClient.newCall(req).execute().use { resp ->
            val bodyStr = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw IllegalStateException(extractGoogleApiError(resp.code, bodyStr))
            }
            return JSONObject(bodyStr).getString("id")
        }
    }

    private fun updateDriveFileContent(
        accessToken: String,
        fileId: String,
        jsonContent: String
    ): String {
        val req = Request.Builder()
            .url("$DRIVE_UPLOAD_URL/$fileId?uploadType=media&fields=id")
            .header("Authorization", "Bearer $accessToken")
            .patch(jsonContent.toRequestBody(MEDIA_TYPE_JSON))
            .build()

        httpClient.newCall(req).execute().use { resp ->
            val bodyStr = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw IllegalStateException(extractGoogleApiError(resp.code, bodyStr))
            }
            return JSONObject(bodyStr).optString("id", fileId)
        }
    }

    private fun extractGoogleApiError(code: Int, body: String): String {
        val parsedMsg = runCatching {
            JSONObject(body).optJSONObject("error")?.optString("message")
        }.getOrNull()
        return when {
            code == 401 -> "Google Drive অনুমোদনের মেয়াদ শেষ হয়েছে; অনুগ্রহ করে আবার যুক্ত করুন"
            code == 403 && parsedMsg?.contains("Drive API has not been used", ignoreCase = true) == true ->
                "প্রজেক্টে Google Drive API চালু নেই — Google Cloud Console থেকে Drive API এনাবল করুন"
            !parsedMsg.isNullOrBlank() -> "Google Drive ত্রুটি ($code): $parsedMsg"
            else -> "Google Drive অনুরোধ ব্যর্থ হয়েছে (কোড $code)"
        }
    }

    private fun mapAuthorizationExceptionMessage(e: Exception): String {
        val msg = e.message.orEmpty()
        return when {
            msg.contains("DEVELOPER_ERROR", ignoreCase = true) || msg.contains("10:") ->
                "Google Drive OAuth কনফিগারেশন (SHA-1 বা Client ID) মিলছে না। সরাসরি 'ফাইল / ড্রাইভে সেভ (.json)' ব্যবহার করুন।"
            msg.contains("CANCELED", ignoreCase = true) || msg.contains("16:") ->
                "Google Drive অনুমোদন বাতিল করা হয়েছে"
            msg.contains("NETWORK_ERROR", ignoreCase = true) ->
                "ইন্টারনেট সংযোগ পাওয়া যায়নি। নেট চালু করে আবার চেষ্টা করুন।"
            else -> "Google Drive সংযোগ করা যায়নি: ${e.localizedMessage ?: "পুনরায় চেষ্টা করুন"}"
        }
    }

    companion object {
        private const val TAG = "GoogleDriveBackupMgr"
        const val DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive.file"
        const val BACKUP_FOLDER_NAME = "Ekta Samiti Backups"
        const val BACKUP_FILE_NAME = "EktaSamiti_Backup.json"

        private const val DRIVE_FILES_URL = "https://www.googleapis.com/drive/v3/files"
        private const val DRIVE_UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3/files"
        private const val DRIVE_ABOUT_URL =
            "https://www.googleapis.com/drive/v3/about?fields=user(emailAddress,displayName)"

        private const val MIME_FOLDER = "application/vnd.google-apps.folder"
        private const val MIME_JSON = "application/json"
        private val MEDIA_TYPE_JSON = "application/json; charset=utf-8".toMediaType()

        private const val PREFS_NAME = "ekta_drive_backup_prefs"
        private const val KEY_CONNECTED = "drive_connected"
        private const val KEY_EMAIL = "drive_email"
        private const val KEY_NAME = "drive_name"
        private const val KEY_LAST_BACKUP_MILLIS = "drive_last_backup_millis"
        private const val KEY_LAST_RECORD_COUNT = "drive_last_record_count"
        private const val KEY_LAST_FILE_ID = "drive_last_file_id"

        private fun defaultHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
