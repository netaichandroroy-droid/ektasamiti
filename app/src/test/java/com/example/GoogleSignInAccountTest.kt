package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.cloud.GoogleAuthManager
import com.example.data.local.AppDatabase
import com.example.data.local.SessionPreferences
import com.example.data.model.UserRole
import com.example.data.repository.SamitiRepository
import com.example.ui.screens.LoginScreen
import com.example.ui.theme.EktaSamitiTheme
import com.example.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp-xxhdpi")
class GoogleSignInAccountTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var repository: SamitiRepository
    private lateinit var sessionPreferences: SessionPreferences

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor { it.run() }
            .setTransactionExecutor { it.run() }
            .build()
        repository = SamitiRepository(database)
        sessionPreferences = SessionPreferences(context)
        sessionPreferences.clearSession()
    }

    @After
    fun teardown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun samitiGoogleAccountConstantMatchesRequestedEmail() {
        assertEquals("jadobpursamiti@gmail.com", GoogleAuthManager.SAMITI_GOOGLE_EMAIL)
    }

    @Test
    fun resolveWebClientIdHandlesPresentAndMissingResourceSafely() {
        // When google-services.json is present, default_web_client_id resolves from R.string
        val resolved = GoogleAuthManager.resolveWebClientId(context)
        assertNotNull(resolved)
        assertTrue(resolved!!.isNotBlank())

        // When missing (e.g. CI without google-services.json), returns null cleanly without throwing
        val missing = GoogleAuthManager.resolveStringResByName(context, "non_existent_client_id_field")
        assertEquals(null, missing)
    }

    @Test
    fun googleSignInForJadobpurSamitiResolvesAdminAndPersistsSession() = runTest(testDispatcher) {
        val authViewModel = AuthViewModel(repository, sessionPreferences)
        advanceUntilIdle()

        var callbackInvoked = false
        authViewModel.onGoogleFirebaseSignedIn(
            email = "jadobpursamiti@gmail.com",
            displayName = null,
            onSuccess = { callbackInvoked = true }
        )
        advanceUntilIdle()

        assertTrue(callbackInvoked)
        val signedInUser = authViewModel.uiState.value.currentUser
        assertNotNull(signedInUser)
        assertEquals("jadobpursamiti@gmail.com", signedInUser?.email)
        assertEquals(UserRole.ADMIN, signedInUser?.role)

        // Case-insensitive repository lookup works for Google account
        val lookedUp = repository.getUserByEmail("JadobpurSamiti@gmail.com")
        assertNotNull(lookedUp)
        assertEquals("jadobpursamiti@gmail.com", lookedUp?.email)

        // Session persists across ViewModel recreation
        val restartedViewModel = AuthViewModel(repository, sessionPreferences)
        advanceUntilIdle()
        assertEquals("jadobpursamiti@gmail.com", restartedViewModel.uiState.value.currentUser?.email)
        assertEquals(UserRole.ADMIN, restartedViewModel.uiState.value.currentUser?.role)
    }

    @Test
    fun migration4To5InsertsJadobpurSamitiGoogleAdmin() {
        val dbName = "migration_test_${System.nanoTime()}.db"
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(4) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS users (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "email TEXT NOT NULL, " +
                            "passwordHash TEXT NOT NULL, " +
                            "fullName TEXT NOT NULL, " +
                            "mobile TEXT NOT NULL, " +
                            "role TEXT NOT NULL, " +
                            "isActive INTEGER NOT NULL, " +
                            "createdAt INTEGER NOT NULL)"
                    )
                    db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_users_email ON users (email)")
                    db.execSQL(
                        "INSERT INTO users (email, passwordHash, fullName, mobile, role, isActive, createdAt) " +
                            "VALUES ('Nitai', 'Nemai', 'নিতাই চন্দ্র রায় (পরিচালক)', '01711002233', 'ADMIN', 1, 1000)"
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val db = helper.writableDatabase
        try {
            AppDatabase.MIGRATION_4_5.migrate(db)
            val cursor = db.query(
                "SELECT email, role, isActive FROM users WHERE email = 'jadobpursamiti@gmail.com'"
            )
            cursor.use {
                assertTrue("Expected jadobpursamiti@gmail.com to be inserted by MIGRATION_4_5", it.moveToFirst())
                assertEquals("jadobpursamiti@gmail.com", it.getString(0))
                assertEquals("ADMIN", it.getString(1))
                assertEquals(1, it.getInt(2))
            }
        } finally {
            db.close()
            helper.close()
            context.deleteDatabase(dbName)
        }
    }

    @Test
    fun loginScreenDisplaysGoogleSignInButtonAndSamitiEmail() {
        val authViewModel = AuthViewModel(repository, sessionPreferences = null)
        composeRule.setContent {
            EktaSamitiTheme {
                LoginScreen(authViewModel = authViewModel, onLoginSuccess = {})
            }
        }

        composeRule.onNodeWithTag("btn_google_sign_in").assertIsDisplayed()
        composeRule.onNodeWithText("Sign in with Google").assertIsDisplayed()
        composeRule.onNodeWithTag("text_samiti_google_account")
            .assertIsDisplayed()
            .assertTextContains("jadobpursamiti@gmail.com", substring = true)
    }
}
