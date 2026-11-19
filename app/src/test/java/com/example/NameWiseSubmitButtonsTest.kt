package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
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
import com.example.ui.screens.NewLoanDisbursalDialog
import com.example.ui.screens.SavingsDepositDialog
import com.example.ui.screens.SavingsScreen
import com.example.ui.screens.SavingsWithdrawDialog
import com.example.ui.theme.EktaSamitiTheme
import com.example.ui.viewmodel.SamitiViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp")
class NameWiseSubmitButtonsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var database: AppDatabase
    private lateinit var repository: SamitiRepository
    private lateinit var samitiViewModel: SamitiViewModel

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
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor { it.run() }
            .setTransactionExecutor { it.run() }
            .build()
        repository = SamitiRepository(database)
        samitiViewModel = SamitiViewModel(repository)

        runBlocking {
            val id1 = repository.addMember(
                Member(
                    memberCode = "01",
                    name = "netai",
                    mobile = "",
                    savingsBalance = 1500.0,
                    registeredBy = "Nitai"
                )
            )
            val id2 = repository.addMember(
                Member(
                    memberCode = "02",
                    name = "rahim",
                    mobile = "",
                    savingsBalance = 2000.0,
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
    fun savingsScreen_quickMemberButtonOpensNameWiseDepositSubmitButton() {
        composeRule.setContent {
            EktaSamitiTheme {
                SavingsScreen(
                    currentUser = adminUser,
                    viewModel = samitiViewModel
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = 5000) {
            samitiViewModel.members.value.size == 2
        }
        composeRule.onNodeWithText("আজকের সঞ্চয় জমা").assertDoesNotExist()
        composeRule.onNodeWithText("মোট সঞ্চয় তহবিল").assertDoesNotExist()
        composeRule.onNodeWithText("সঞ্চয় জমা নিন").assertDoesNotExist()
        composeRule.onNodeWithText("উত্তোলন").assertDoesNotExist()
        composeRule.onNodeWithText("সকল সঞ্চয় লেনদেন বিবরণী").assertDoesNotExist()
        composeRule.onNodeWithTag("btn_open_deposit").assertDoesNotExist()
        composeRule.onNodeWithTag("btn_open_withdraw").assertDoesNotExist()
        composeRule.onNodeWithTag("savings_quick_member_row").assertIsDisplayed()
        composeRule.onNodeWithTag("btn_savings_member_02").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("btn_confirm_deposit")
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextEquals("rahim - জমা নিশ্চিত")

        composeRule.onNodeWithTag("chip_deposit_member_01").performScrollTo().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("btn_confirm_deposit")
            .performScrollTo()
            .assertTextEquals("netai - জমা নিশ্চিত")
    }

    @Test
    fun savingsScreen_displaysTransactionsInHeaderBasedList() {
        val tx = runBlocking {
            repository.recordSavingsDeposit(
                member = memberNetai,
                amount = 100.0,
                savingsPlan = "WEEKLY",
                note = "সাপ্তাহিক জমা",
                collectedBy = "নিতাই চন্দ্র রায়",
                role = UserRole.ADMIN,
                customDate = System.currentTimeMillis()
            )
        }

        composeRule.setContent {
            EktaSamitiTheme {
                SavingsScreen(
                    currentUser = adminUser,
                    viewModel = samitiViewModel
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = 5000) {
            samitiViewModel.savingsList.value.isNotEmpty()
        }

        composeRule.onNodeWithTag("savings_list_table").assertIsDisplayed()
        composeRule.onNodeWithTag("savings_list_header").assertIsDisplayed()
        composeRule.onNodeWithText("SL").assertIsDisplayed()
        composeRule.onNodeWithText("নাম ও তারিখ").assertIsDisplayed()
        composeRule.onNodeWithText("টাকা").assertIsDisplayed()
        composeRule.onNodeWithText("অ্যাকশন").assertIsDisplayed()

        composeRule.onNodeWithTag("savings_item_${tx.receiptNumber}").assertIsDisplayed()
        composeRule.onNodeWithText("+৳100").assertIsDisplayed()
        composeRule.onNodeWithTag("btn_edit_savings_date_${tx.receiptNumber}").assertIsDisplayed()
        composeRule.onNodeWithTag("btn_delete_savings_${tx.receiptNumber}").assertIsDisplayed()
    }

    @Test
    fun savingsWithdrawDialog_displaysAndSwitchesNameWiseSubmitButton() {
        composeRule.setContent {
            EktaSamitiTheme {
                SavingsWithdrawDialog(
                    members = listOf(memberNetai, memberRahim),
                    preselectedMember = memberNetai,
                    currentUser = adminUser,
                    onDismiss = {},
                    onSubmit = { _, _, _, _ -> }
                )
            }
        }

        composeRule.onNodeWithTag("btn_confirm_withdraw")
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextEquals("netai - উত্তোলন নিশ্চিত")

        composeRule.onNodeWithTag("chip_withdraw_member_02").performScrollTo().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("btn_confirm_withdraw")
            .performScrollTo()
            .assertTextEquals("rahim - উত্তোলন নিশ্চিত")
    }

    @Test
    fun loansScreen_quickMemberButtonOpensNameWiseLoanDisbursalSubmitButton() {
        composeRule.setContent {
            EktaSamitiTheme {
                LoansScreen(
                    currentUser = adminUser,
                    viewModel = samitiViewModel
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = 5000) {
            samitiViewModel.members.value.size == 2
        }
        composeRule.onNodeWithTag("loans_quick_member_row").assertIsDisplayed()
        composeRule.onNodeWithTag("btn_loan_member_02").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("btn_confirm_disburse_loan")
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextEquals("rahim - ঋণ নিশ্চিত")

        composeRule.onNodeWithTag("chip_loan_member_01").performScrollTo().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("btn_confirm_disburse_loan")
            .performScrollTo()
            .assertTextEquals("netai - ঋণ নিশ্চিত")
    }

    @Test
    fun collectInstallmentDialog_displaysAndSwitchesNameWiseSubmitButton() {
        val loan1 = runBlocking {
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
        val loan2 = runBlocking {
            repository.disburseLoan(
                member = memberRahim,
                principalAmount = 20000.0,
                chargeAmount = 2000.0,
                totalInstallments = 20,
                frequency = "WEEKLY",
                purpose = "কৃষি",
                disbursedBy = "Nitai"
            )
        }

        composeRule.setContent {
            EktaSamitiTheme {
                CollectInstallmentDialog(
                    activeLoans = listOf(loan1, loan2),
                    preselectedLoan = loan1,
                    currentUser = adminUser,
                    onDismiss = {},
                    onSubmit = { _, _, _, _, _ -> }
                )
            }
        }

        composeRule.onNodeWithTag("btn_confirm_collect_installment")
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextEquals("netai - আদায় নিশ্চিত")

        composeRule.onNodeWithTag("chip_collect_loan_02_${loan2.id}").performScrollTo().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("btn_confirm_collect_installment")
            .performScrollTo()
            .assertTextEquals("rahim - আদায় নিশ্চিত")
    }
}
