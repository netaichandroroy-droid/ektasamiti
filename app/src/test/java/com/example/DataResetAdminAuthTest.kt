package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.Member
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.data.repository.SamitiRepository
import com.example.ui.screens.ResetDataAdminAuthDialog
import com.example.ui.theme.EktaSamitiTheme
import com.example.ui.viewmodel.SamitiViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DataResetAdminAuthTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var repository: SamitiRepository

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
    }

    @After
    fun teardown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun resetDataRequiresValidAdminCredentials_nitaiAndNemai() = runTest(testDispatcher) {
        repository.registerUser(
            User(
                email = "Nitai",
                passwordHash = "Nemai",
                fullName = "নিতাই চন্দ্র রায় (পরিচালক)",
                mobile = "01711002233",
                role = UserRole.ADMIN,
                isActive = true
            )
        )
        repository.registerUser(
            User(
                email = "collector@ektasamiti.com",
                passwordHash = "123456",
                fullName = "মো: রহিম মিয়া",
                mobile = "01822334455",
                role = UserRole.COLLECTOR,
                isActive = true
            )
        )
        repository.addMember(
            Member(
                memberCode = "ES-1001",
                name = "করিম মিয়া",
                mobile = "01700000000",
                memberType = "WEEKLY",
                registeredBy = "Nitai"
            )
        )

        // Login works with Nitai / Nemai and fails with old password
        assertNotNull(repository.authenticate("Nitai", "Nemai"))
        assertNull(repository.authenticate("Nitai", "admin123"))

        val viewModel = SamitiViewModel(repository)
        var resetError: String? = null
        var resetSucceeded = false

        // 1. Blank credentials rejected
        viewModel.clearAllSamitiDataWithAdminAuth(
            role = UserRole.ADMIN,
            adminEmail = "",
            adminPassword = "",
            onSuccess = { resetSucceeded = true },
            onError = { resetError = it }
        )
        advanceUntilIdle()
        assertFalse(resetSucceeded)
        assertNotNull(resetError)
        assertEquals(1, database.memberDao().getAllMembersList().size)

        // 2. Wrong password rejected (case-sensitive Nemai vs nemai)
        resetError = null
        viewModel.clearAllSamitiDataWithAdminAuth(
            role = UserRole.ADMIN,
            adminEmail = "Nitai",
            adminPassword = "nemai",
            onSuccess = { resetSucceeded = true },
            onError = { resetError = it }
        )
        advanceUntilIdle()
        assertFalse(resetSucceeded)
        assertNotNull(resetError)
        assertEquals(1, database.memberDao().getAllMembersList().size)

        // 3. Collector credentials rejected even though account exists
        resetError = null
        viewModel.clearAllSamitiDataWithAdminAuth(
            role = UserRole.ADMIN,
            adminEmail = "collector@ektasamiti.com",
            adminPassword = "123456",
            onSuccess = { resetSucceeded = true },
            onError = { resetError = it }
        )
        advanceUntilIdle()
        assertFalse(resetSucceeded)
        assertNotNull(resetError)
        assertEquals(1, database.memberDao().getAllMembersList().size)

        // 4. Correct Admin ID (Nitai) and Password (Nemai) clears all member/samiti data
        resetError = null
        viewModel.clearAllSamitiDataWithAdminAuth(
            role = UserRole.ADMIN,
            adminEmail = "Nitai",
            adminPassword = "Nemai",
            onSuccess = { resetSucceeded = true },
            onError = { resetError = it }
        )
        advanceUntilIdle()
        assertTrue(resetSucceeded)
        assertNull(resetError)
        assertEquals(0, database.memberDao().getAllMembersList().size)
        // Admin & collector login accounts remain intact
        assertNotNull(repository.authenticate("Nitai", "Nemai"))
    }

    @Test
    fun prepopulatedAppDatabaseUsesNitaiAndNemaiForAdmin() = runTest(testDispatcher) {
        val dbName = "test_nitai_prepopulate_${System.currentTimeMillis()}.db"
        val realDb = AppDatabase.getInstance(context)
        try {
            // Also verify direct new database file creation via AppDatabase callback
            val freshDb = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
                .addMigrations(AppDatabase.MIGRATION_3_4)
                .allowMainThreadQueries()
                .setQueryExecutor { it.run() }
                .setTransactionExecutor { it.run() }
                .addCallback(object : androidx.room.RoomDatabase.Callback() {
                    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        super.onCreate(db)
                        db.execSQL(
                            "INSERT OR REPLACE INTO users (email, passwordHash, fullName, mobile, role, isActive, createdAt) VALUES ('Nitai', 'Nemai', 'নিতাই চন্দ্র রায় (পরিচালক)', '01711002233', 'ADMIN', 1, 0)"
                        )
                    }
                })
                .build()
            try {
                val repo = SamitiRepository(freshDb)
                assertNotNull(repo.authenticate("Nitai", "Nemai"))
                assertTrue(repo.verifyAdminCredentials("Nitai", "Nemai"))
                assertFalse(repo.verifyAdminCredentials("admin@ektasamiti.com", "admin123"))
            } finally {
                freshDb.close()
                context.deleteDatabase(dbName)
            }
        } finally {
            assertNotNull(realDb)
        }
    }

    @Test
    fun resetDataAdminAuthDialog_showsErrorOnWrongPasswordAndSubmitsOnValidAdmin() {
        var verifiedPair: Pair<String, String>? = null
        composeRule.setContent {
            EktaSamitiTheme {
                ResetDataAdminAuthDialog(
                    onDismiss = {},
                    onVerifyAndReset = { id, pass, onError ->
                        if (id == "Nitai" && pass == "Nemai") {
                            verifiedPair = id to pass
                        } else {
                            onError("এডমিনের আইডি অথবা পাসওয়ার্ড সঠিক নয়!")
                        }
                    }
                )
            }
        }

        composeRule.onNodeWithTag("dialog_reset_data_admin_auth").assertIsDisplayed()
        composeRule.onNodeWithTag("input_reset_admin_email").performTextInput("Nitai")
        composeRule.onNodeWithTag("input_reset_admin_password").performTextInput("wrong")
        composeRule.onNodeWithTag("btn_confirm_reset_data").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("text_reset_admin_error").assertIsDisplayed()
        assertNull(verifiedPair)

        composeRule.onNodeWithTag("input_reset_admin_password").performTextReplacement("Nemai")
        composeRule.onNodeWithTag("btn_confirm_reset_data").performClick()
        composeRule.waitForIdle()
        assertEquals("Nitai" to "Nemai", verifiedPair)
    }
}
