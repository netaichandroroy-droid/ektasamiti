package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.Member
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.data.repository.SamitiRepository
import com.example.ui.screens.WeeklyCollectionDatePrefs
import com.example.ui.screens.WeeklyCollectionScreen
import com.example.ui.screens.calculateFridaysForMonth
import com.example.ui.screens.calculateTwoMonthsFridays
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
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp")
class WeeklyCollectionDateUpdateTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var repository: SamitiRepository
    private lateinit var viewModel: SamitiViewModel

    private val adminUser = User(
        id = 1L,
        email = "Nitai",
        passwordHash = "Nemai",
        fullName = "নিতাই চন্দ্র রায়",
        mobile = "01711002233",
        role = UserRole.ADMIN
    )

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("weekly_collection_date_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()

        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor { it.run() }
            .setTransactionExecutor { it.run() }
            .build()
        repository = SamitiRepository(database)
        viewModel = SamitiViewModel(repository)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun weeklyCollectionScreen_topButtonAndHeaderDateClickOpenUpdateDialog() {
        val now = Calendar.getInstance()
        val year = now.get(Calendar.YEAR)
        val month = now.get(Calendar.MONTH)
        val firstWeek = calculateTwoMonthsFridays(year, month).first().fridays.first()

        runBlocking {
            repository.addMember(
                Member(
                    memberCode = "01",
                    name = "netai",
                    mobile = "",
                    savingsBalance = 500.0,
                    registeredBy = "Nitai"
                )
            )
        }

        composeRule.setContent {
            EktaSamitiTheme {
                WeeklyCollectionScreen(
                    currentUser = adminUser,
                    viewModel = viewModel
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = 5000) {
            viewModel.members.value.isNotEmpty()
        }

        // Verify batch collection popup and its triggers do not appear
        composeRule.onNodeWithTag("dialog_batch_weekly_collection").assertDoesNotExist()
        composeRule.onNodeWithTag("btn_add_weekly_collection_top").assertDoesNotExist()
        composeRule.onNodeWithTag("fab_add_weekly_collection").assertDoesNotExist()

        // 1. Clicking top "তারিখ আপডেট" button opens WeeklyDateUpdateDialog
        composeRule.onNodeWithTag("btn_weekly_update_date").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("dialog_weekly_date_update").assertIsDisplayed()
        composeRule.onNodeWithTag("btn_apply_week_column_date").assertIsDisplayed()
    }

    @Test
    fun weeklyCollectionHeaderDateClick_opensDialogAndPersistsOverrideAndUpdatesEntries() {
        val now = Calendar.getInstance()
        val year = now.get(Calendar.YEAR)
        val month = now.get(Calendar.MONTH)
        val firstWeek = calculateFridaysForMonth(year, month).first()

        val customCal = Calendar.getInstance().apply {
            timeInMillis = firstWeek.dateMillis
            add(Calendar.DAY_OF_MONTH, 2) // shift e.g. Friday -> Sunday
        }
        val expectedDay = customCal.get(Calendar.DAY_OF_MONTH)
        val expectedMonth = customCal.get(Calendar.MONTH) + 1
        val expectedYearShort = customCal.get(Calendar.YEAR) % 100
        val expectedShort = "$expectedDay.$expectedMonth.$expectedYearShort"

        val mId = runBlocking {
            repository.addMember(
                Member(
                    memberCode = "01",
                    name = "netai",
                    mobile = "",
                    savingsBalance = 100.0,
                    registeredBy = "Nitai"
                )
            )
        }
        val member = runBlocking { database.memberDao().getMemberById(mId)!! }
        val tx = runBlocking {
            repository.recordSavingsDeposit(
                member = member,
                amount = 100.0,
                savingsPlan = "WEEKLY",
                note = "Weekly",
                collectedBy = "Nitai",
                role = UserRole.ADMIN,
                customDate = firstWeek.dateMillis
            )
        }

        WeeklyCollectionDatePrefs.saveOverride(context, firstWeek.columnKey, customCal.timeInMillis)
        val updatedGroups = calculateFridaysForMonth(
            year,
            month,
            WeeklyCollectionDatePrefs.loadAllOverrides(context)
        )
        assertEquals(expectedShort, updatedGroups.first().shortDateString)

        composeRule.setContent {
            EktaSamitiTheme {
                WeeklyCollectionScreen(
                    currentUser = adminUser,
                    viewModel = viewModel
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = 5000) {
            viewModel.members.value.isNotEmpty()
        }

        // Verify updated date header shows in spreadsheet table & clicking it opens update dialog
        composeRule.onNodeWithText(expectedShort).assertIsDisplayed()
        composeRule.onNodeWithTag("btn_edit_week_date_${firstWeek.columnKey}").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("dialog_weekly_date_update").assertIsDisplayed()
        composeRule.onNodeWithTag("btn_reset_week_column_date").assertIsDisplayed()

        // Verify batch update helper updates Room transaction date
        runBlocking {
            repository.updateSavingsTransactionDate(tx, customCal.timeInMillis)
            val reloaded = database.savingsDao().getTransactionByReceipt(tx.receiptNumber)
            assertEquals(customCal.timeInMillis, reloaded?.date)
        }
    }
}
