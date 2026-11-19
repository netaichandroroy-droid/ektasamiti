package com.example

import android.content.Context
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.Member
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.data.repository.SamitiRepository
import com.example.ui.screens.CollectInstallmentDialog
import com.example.ui.screens.LoansScreen
import com.example.ui.screens.WeeklyCollectionScreen
import com.example.ui.screens.calculateTwoMonthsFridays
import com.example.ui.theme.EktaSamitiTheme
import com.example.ui.viewmodel.SamitiViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp")
class LoanSheetTest {

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

    private lateinit var memberNetai: Member
    private lateinit var memberRahim: Member

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

        runBlocking {
            val id1 = repository.addMember(
                Member(
                    memberCode = "01",
                    name = "netai",
                    mobile = "",
                    savingsBalance = 500.0,
                    registeredBy = "Nitai"
                )
            )
            val id2 = repository.addMember(
                Member(
                    memberCode = "02",
                    name = "rahim",
                    mobile = "",
                    savingsBalance = 300.0,
                    registeredBy = "Nitai"
                )
            )
            memberNetai = database.memberDao().getMemberById(id1)!!
            memberRahim = database.memberDao().getMemberById(id2)!!
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun weeklyCollectionScreen_switchesBetweenSavingsSheetAndLoanSheet() {
        val now = Calendar.getInstance()
        val year = now.get(Calendar.YEAR)
        val month = now.get(Calendar.MONTH)
        val firstFriday = calculateTwoMonthsFridays(year, month).first().fridays.first()

        runBlocking {
            val loan = repository.disburseLoan(
                member = memberNetai,
                principalAmount = 10000.0,
                chargeAmount = 1000.0,
                totalInstallments = 10,
                frequency = "WEEKLY",
                purpose = "ব্যবসা",
                disbursedBy = "Nitai"
            )
            repository.collectInstallment(
                loan = loan,
                amount = 1100.0,
                penalty = 0.0,
                collectedBy = "Nitai",
                notes = "১ম কিস্তি",
                paymentDate = firstFriday.dateMillis
            )
            repository.recordSavingsDeposit(
                member = memberNetai,
                amount = 100.0,
                savingsPlan = "WEEKLY",
                note = "Weekly savings",
                collectedBy = "Nitai",
                role = UserRole.ADMIN,
                customDate = firstFriday.dateMillis
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
            viewModel.members.value.size == 2 && viewModel.loansList.value.isNotEmpty()
        }

        // Switch to ঋণ শীট
        composeRule.onNodeWithTag("btn_weekly_mode_loan_sheet").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("loan_sheet_table").assertIsDisplayed()
        composeRule.onNodeWithText("Ekta Samiti — ঋণ শীট").assertIsDisplayed()
        composeRule.onNodeWithText("মোট ঋণ").assertIsDisplayed()
        composeRule.onNodeWithText("বকেয়া").assertIsDisplayed()
        composeRule.onAllNodesWithText("11000").assertCountEquals(2)
        composeRule.onAllNodesWithText("11000").onFirst().assertIsDisplayed()
        composeRule.onAllNodesWithText("9900").assertCountEquals(2)
        composeRule.onAllNodesWithText("9900").onFirst().assertIsDisplayed()

        // Tap netai's cell on firstFriday -> opens loan cell collection dialog with existing entry & duplicate guard
        composeRule.onNodeWithTag("loan_sheet_cell_01_${firstFriday.columnKey}")
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("dialog_loan_cell_collection").assertIsDisplayed()
        composeRule.onNodeWithTag("warning_duplicate_cell_date").assertIsDisplayed()
        composeRule.onNodeWithTag("btn_confirm_cell_collection").assertIsNotEnabled()
    }

    @Test
    fun loansScreen_showsLoanSheetByDefaultAndOpensNewLoanOrInstallmentOnCellTap() {
        val now = Calendar.getInstance()
        val year = now.get(Calendar.YEAR)
        val month = now.get(Calendar.MONTH)
        val firstFriday = calculateTwoMonthsFridays(year, month).first().fridays.first()

        runBlocking {
            repository.disburseLoan(
                member = memberNetai,
                principalAmount = 5000.0,
                chargeAmount = 500.0,
                totalInstallments = 10,
                frequency = "WEEKLY",
                purpose = "দোকান",
                disbursedBy = "Nitai"
            )
            // Also record a savings deposit on firstFriday to verify savings does NOT block loan installment
            repository.recordSavingsDeposit(
                member = memberNetai,
                amount = 100.0,
                savingsPlan = "WEEKLY",
                note = "Weekly savings",
                collectedBy = "Nitai",
                role = UserRole.ADMIN,
                customDate = firstFriday.dateMillis
            )
            assertTrue(repository.hasSavingsDepositOnDate(memberNetai.id, firstFriday.dateMillis))
            assertFalse(repository.hasInstallmentPaymentOnDate(memberNetai.id, firstFriday.dateMillis))
        }

        composeRule.setContent {
            EktaSamitiTheme {
                LoansScreen(
                    currentUser = adminUser,
                    viewModel = viewModel
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = 5000) {
            viewModel.members.value.size == 2 && viewModel.activeLoans.value.isNotEmpty()
        }

        // Summary cards should not be shown on Loan page
        composeRule.onNodeWithText("চলমান মোট বকেয়া ঋণ").assertDoesNotExist()
        composeRule.onNodeWithText("সর্বমোট কিস্তি আদায়").assertDoesNotExist()

        // Default tab is ঋণ শীট
        composeRule.onNodeWithTag("loan_sheet_table").assertIsDisplayed()
        composeRule.onAllNodesWithText("5500").assertCountEquals(4)
        composeRule.onAllNodesWithText("5500").onFirst().assertIsDisplayed()

        // Tap memberNetai's cell (has active loan, no installment yet on firstFriday even though savings was paid)
        composeRule.onNodeWithTag("loan_sheet_cell_01_${firstFriday.columnKey}")
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("dialog_loan_cell_collection").assertIsDisplayed()
        composeRule.onNodeWithTag("btn_confirm_cell_collection").assertIsEnabled().performClick()
        composeRule.waitUntil(timeoutMillis = 5000) {
            viewModel.installmentsList.value.size == 1
        }

        assertEquals(1, viewModel.installmentsList.value.size)

        // Tap date update button in ঋণ শীট tab
        composeRule.onNodeWithTag("btn_loan_sheet_update_date").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("dialog_weekly_date_update").assertIsDisplayed()
    }

    @Test
    fun loansScreen_cellClickForMemberWithoutLoanOpensPreselectedLoanDialog() {
        val now = Calendar.getInstance()
        val year = now.get(Calendar.YEAR)
        val month = now.get(Calendar.MONTH)
        val firstFriday = calculateTwoMonthsFridays(year, month).first().fridays.first()

        composeRule.setContent {
            EktaSamitiTheme {
                LoansScreen(
                    currentUser = adminUser,
                    viewModel = viewModel
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = 5000) {
            viewModel.members.value.size == 2
        }

        composeRule.onNodeWithTag("loan_sheet_cell_02_${firstFriday.columnKey}")
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("btn_confirm_disburse_loan")
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextEquals("rahim - ঋণ নিশ্চিত")
    }

    @Test
    fun collectInstallmentDialog_blocksDuplicateInstallmentOnSameDate() {
        val todayMillis = System.currentTimeMillis()
        val loan = runBlocking {
            repository.disburseLoan(
                member = memberNetai,
                principalAmount = 10000.0,
                chargeAmount = 1000.0,
                totalInstallments = 10,
                frequency = "WEEKLY",
                purpose = "ব্যবসা",
                disbursedBy = "Nitai"
            )
        }
        val payment = runBlocking {
            repository.collectInstallment(
                loan = loan,
                amount = 1100.0,
                penalty = 0.0,
                collectedBy = "Nitai",
                notes = "Test",
                paymentDate = todayMillis
            )
        }

        composeRule.setContent {
            EktaSamitiTheme {
                CollectInstallmentDialog(
                    activeLoans = listOf(loan),
                    preselectedLoan = loan,
                    currentUser = adminUser,
                    installmentsList = listOf(payment),
                    onDismiss = {},
                    onSubmit = { _, _, _, _, _ -> }
                )
            }
        }

        composeRule.onNodeWithTag("warning_duplicate_installment_date")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("btn_confirm_collect_installment")
            .performScrollTo()
            .assertIsNotEnabled()
    }
}
