package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.Member
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.data.repository.SamitiRepository
import com.example.ui.screens.CollectInstallmentDialog
import com.example.ui.screens.LoansScreen
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp")
class NonMemberLoanTest {

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
    fun loansScreen_disbursesNonMemberLoanEvenWithZeroRegisteredMembers() {
        composeRule.setContent {
            EktaSamitiTheme {
                LoansScreen(
                    currentUser = adminUser,
                    viewModel = viewModel
                )
            }
        }

        // Open new loan dialog with 0 members registered
        composeRule.onNodeWithTag("btn_open_disburse_loan").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("input_non_member_loan_name")
            .performScrollTo()
            .assertIsDisplayed()
            .performTextInput("করিম মিয়া")
        composeRule.onNodeWithTag("input_non_member_loan_mobile")
            .performScrollTo()
            .performTextInput("01799887766")

        composeRule.onNodeWithTag("btn_confirm_disburse_loan")
            .performScrollTo()
            .assertTextEquals("করিম মিয়া - ঋণ নিশ্চিত")
            .performClick()

        composeRule.waitUntil(timeoutMillis = 5000) {
            viewModel.loansList.value.size == 1
        }

        val savedLoan = viewModel.loansList.value.first()
        assertEquals(0L, savedLoan.memberId)
        assertEquals("NM-01", savedLoan.memberCode)
        assertEquals("করিম মিয়া", savedLoan.memberName)
        assertEquals("01799887766", savedLoan.memberMobile)

        // Appears in ঋণ শীট and চলমান tabs
        composeRule.onNodeWithTag("loan_sheet_table").assertIsDisplayed()
        composeRule.onNodeWithText("করিম মিয়া (বহিঃস্থ)").assertIsDisplayed()

        composeRule.onNodeWithTag("tab_loans_active").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("loan_card_${savedLoan.id}").assertIsDisplayed()
        composeRule.onNodeWithText("মেম্বার ছাড়া").assertIsDisplayed()
    }

    @Test
    fun loansScreen_switchesFromMemberToNonMemberModeAndGeneratesSequentialCodes() {
        runBlocking {
            repository.addMember(
                Member(
                    memberCode = "01",
                    name = "netai",
                    mobile = "01700000001",
                    savingsBalance = 500.0,
                    registeredBy = "Nitai"
                )
            )
            repository.disburseNonMemberLoan(
                borrowerName = "রফিকুল",
                borrowerMobile = "",
                principalAmount = 5000.0,
                chargeAmount = 500.0,
                totalInstallments = 10,
                frequency = "WEEKLY",
                purpose = "ব্যবসা",
                disbursedBy = "Nitai"
            )
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
            viewModel.members.value.size == 1 && viewModel.loansList.value.size == 1
        }

        composeRule.onNodeWithTag("btn_open_disburse_loan").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        // Defaults to registered member mode when members exist
        composeRule.onNodeWithTag("btn_confirm_disburse_loan")
            .performScrollTo()
            .assertTextEquals("netai - ঋণ নিশ্চিত")

        // Switch to মেম্বার ছাড়া
        composeRule.onNodeWithTag("chip_loan_borrower_non_member")
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("input_non_member_loan_name")
            .performScrollTo()
            .performTextInput("সুমন দাস")

        composeRule.onNodeWithTag("btn_confirm_disburse_loan")
            .performScrollTo()
            .assertTextEquals("সুমন দাস - ঋণ নিশ্চিত")
            .performClick()

        composeRule.waitUntil(timeoutMillis = 5000) {
            viewModel.loansList.value.size == 2
        }

        val codes = viewModel.loansList.value.map { it.memberCode }.toSet()
        assertTrue(codes.contains("NM-01"))
        assertTrue(codes.contains("NM-02"))
    }

    @Test
    fun nonMemberLoans_duplicateDateCheckIsolatesDifferentBorrowers() = runBlocking<Unit> {
        val todayMillis = System.currentTimeMillis()
        val loanA = repository.disburseNonMemberLoan(
            borrowerName = "আলী",
            borrowerMobile = "",
            principalAmount = 10000.0,
            chargeAmount = 1000.0,
            totalInstallments = 10,
            frequency = "WEEKLY",
            purpose = "ব্যবসা",
            disbursedBy = "Nitai"
        )
        val loanB = repository.disburseNonMemberLoan(
            borrowerName = "হাসান",
            borrowerMobile = "",
            principalAmount = 8000.0,
            chargeAmount = 800.0,
            totalInstallments = 10,
            frequency = "WEEKLY",
            purpose = "ব্যবসা",
            disbursedBy = "Nitai"
        )

        val paymentA = repository.collectInstallment(
            loan = loanA,
            amount = 1100.0,
            penalty = 0.0,
            collectedBy = "Nitai",
            notes = "১ম কিস্তি",
            paymentDate = todayMillis
        )

        // Repository checks per loan for non-members: loanA is blocked, loanB is NOT blocked!
        assertTrue(repository.hasInstallmentPaymentForLoanOnDate(loanA.id, todayMillis))
        assertFalse(repository.hasInstallmentPaymentForLoanOnDate(loanB.id, todayMillis))
        assertFalse(repository.hasInstallmentPaymentOnDate(0L, todayMillis))

        composeRule.setContent {
            EktaSamitiTheme {
                CollectInstallmentDialog(
                    activeLoans = listOf(loanA, loanB),
                    preselectedLoan = loanB,
                    currentUser = adminUser,
                    installmentsList = listOf(paymentA),
                    onDismiss = {},
                    onSubmit = { _, _, _, _, _ -> }
                )
            }
        }

        // For loanB (হাসান), today's payment for loanA (আলী) must NOT disable submit button
        composeRule.onNodeWithTag("btn_confirm_collect_installment")
            .performScrollTo()
            .assertIsEnabled()

        // Switching to loanA (আলী) DOES show duplicate warning and disables submit
        composeRule.onNodeWithTag("chip_collect_loan_${loanA.memberCode}_${loanA.id}")
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("warning_duplicate_installment_date")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("btn_confirm_collect_installment")
            .performScrollTo()
            .assertIsNotEnabled()
    }
}
