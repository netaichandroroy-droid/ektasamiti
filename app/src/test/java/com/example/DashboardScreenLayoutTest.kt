package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.data.repository.SamitiRepository
import com.example.ui.screens.DashboardScreen
import com.example.ui.theme.EktaSamitiTheme
import com.example.ui.viewmodel.SamitiViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DashboardScreenLayoutTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var database: AppDatabase
    private lateinit var repository: SamitiRepository
    private lateinit var viewModel: SamitiViewModel

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = SamitiRepository(database)
        viewModel = SamitiViewModel(repository)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun dashboardScreen_showsOnlyQuickActionsAndFinancialSummaryCards() {
        val admin = User(
            id = 1L,
            email = "Nitai",
            passwordHash = "Nemai",
            fullName = "নিতাই চন্দ্র রায়",
            mobile = "01711002233",
            role = UserRole.ADMIN
        )

        composeRule.setContent {
            EktaSamitiTheme {
                DashboardScreen(
                    currentUser = admin,
                    viewModel = viewModel,
                    onNavigateToTab = {},
                    onOpenAddMember = {},
                    onOpenDeposit = {},
                    onOpenNewLoan = {},
                    onOpenCollectInstallment = {}
                )
            }
        }

        composeRule.onNodeWithTag("section_quick_actions").assertIsDisplayed()
        composeRule.onNodeWithText("দ্রুত সেবা ও লেনদেন").assertIsDisplayed()
        composeRule.onNodeWithTag("quick_btn_new_member").assertIsDisplayed()
        composeRule.onNodeWithTag("quick_btn_deposit").assertIsDisplayed()
        composeRule.onNodeWithTag("quick_btn_installment").assertIsDisplayed()
        composeRule.onNodeWithTag("quick_btn_new_loan").assertIsDisplayed()

        composeRule.onNodeWithTag("section_financial_summary").assertIsDisplayed()
        composeRule.onNodeWithText("সমিতির আর্থিক সারসংক্ষেপ").assertIsDisplayed()
        composeRule.onNodeWithText("মোট সক্রিয় সদস্য").assertIsDisplayed()

        composeRule.onNodeWithText("স্বাগতম,").assertDoesNotExist()
        composeRule.onNodeWithTag("dashboard_digital_clock").assertDoesNotExist()
        composeRule.onNodeWithText("সাম্প্রতিক লেনদেন ও কালেকশন").assertDoesNotExist()
        composeRule.onNodeWithText("এখনো কোন লেনদেন সম্পন্ন হয়নি").assertDoesNotExist()
    }

    @Test
    fun samitiData_persistsAcrossDatabaseReopen() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<android.content.Context>()
            val dbName = "persist_verify_${System.currentTimeMillis()}.db"
            val db1 = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
                .allowMainThreadQueries()
                .build()
            val repo1 = SamitiRepository(db1)
            val memberId = repo1.addMember(
                com.example.data.model.Member(
                    memberCode = "ES-901",
                    name = "Test Persist Member",
                    mobile = "01700000000",
                    memberType = "WEEKLY",
                    savingsBalance = 250.0,
                    registeredBy = "Nitai"
                )
            )
            assertEquals(true, memberId > 0L)
            db1.close()

            val db2 = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
                .allowMainThreadQueries()
                .build()
            val persistedMembers = db2.memberDao().getAllMembersList()
            assertEquals(1, persistedMembers.size)
            assertEquals("ES-901", persistedMembers.first().memberCode)
            assertEquals(250.0, persistedMembers.first().savingsBalance, 0.01)
            db2.close()
            context.deleteDatabase(dbName)
        }
    }
}
