package com.example

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.DateWiseCashLedgerCalculator
import com.example.data.model.InstallmentPayment
import com.example.data.model.Loan
import com.example.data.model.Member
import com.example.data.model.SavingsTransaction
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.data.repository.SamitiRepository
import com.example.ui.screens.ReportsAndAdminScreen
import com.example.ui.theme.EktaSamitiTheme
import com.example.ui.viewmodel.SamitiViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
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
class DateWiseCashLedgerTest {

    @get:Rule
    val composeRule = createComposeRule()

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
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
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

    private fun dateMillis(year: Int, monthZeroBased: Int, day: Int): Long {
        return Calendar.getInstance().apply {
            set(year, monthZeroBased, day, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    @Test
    fun calculatorMatchesUserCashLedgerExampleExactMath() {
        val d1 = dateMillis(2026, Calendar.FEBRUARY, 18)
        val d2 = dateMillis(2026, Calendar.FEBRUARY, 25)
        val d3 = dateMillis(2026, Calendar.FEBRUARY, 28)

        val savings = listOf(
            SavingsTransaction(
                memberId = 1L,
                memberCode = "01",
                memberName = "Member A",
                type = "DEPOSIT",
                savingsPlan = "WEEKLY",
                amount = 3000.0,
                balanceAfter = 3000.0,
                date = d1
            ),
            SavingsTransaction(
                memberId = 1L,
                memberCode = "01",
                memberName = "Member A",
                type = "DEPOSIT",
                savingsPlan = "WEEKLY",
                amount = 3000.0,
                balanceAfter = 6000.0,
                date = d2
            ),
            SavingsTransaction(
                memberId = 1L,
                memberCode = "01",
                memberName = "Member A",
                type = "DEPOSIT",
                savingsPlan = "WEEKLY",
                amount = 2500.0,
                balanceAfter = 8500.0,
                date = d3
            )
        )

        val loans = listOf(
            Loan(
                id = 1L,
                memberId = 1L,
                memberCode = "01",
                memberName = "Member A",
                principalAmount = 2000.0,
                chargeAmount = 200.0,
                totalPayable = 2200.0,
                perInstallmentAmount = 220.0,
                remainingDue = 2200.0,
                disbursedDate = d2
            )
        )

        val installments = listOf(
            InstallmentPayment(
                id = 1L,
                loanId = 1L,
                memberId = 1L,
                memberCode = "01",
                memberName = "Member A",
                installmentNumber = 1,
                paidAmount = 2500.0,
                paymentDate = d3,
                remainingAfterPayment = 0.0
            )
        )

        val summary = DateWiseCashLedgerCalculator.compute(savings, loans, installments)
        assertEquals(3, summary.entries.size)
        assertEquals(9000.0, summary.totalCash, 0.01)

        // Day 1: 3000 savings -> মোট আদায় 3000, closing 3000
        assertEquals("18/02/26", summary.entries[0].displayDate)
        assertEquals(3000.0, summary.entries[0].netDayCollection, 0.01)
        assertEquals(0.0, summary.entries[0].openingCash, 0.01)
        assertEquals(3000.0, summary.entries[0].closingCash, 0.01)

        // Day 2: 3000 savings - 2000 loan = 1000 মোট আদায়; 3000 opening -> 4000 closing
        assertEquals("25/02/26", summary.entries[1].displayDate)
        assertEquals(1000.0, summary.entries[1].netDayCollection, 0.01)
        assertEquals(3000.0, summary.entries[1].openingCash, 0.01)
        assertEquals(4000.0, summary.entries[1].closingCash, 0.01)

        // Day 3: 2500 savings + 2500 installment = 5000 মোট আদায়; 4000 opening -> 9000 closing
        assertEquals("28/02/26", summary.entries[2].displayDate)
        assertEquals(5000.0, summary.entries[2].netDayCollection, 0.01)
        assertEquals(4000.0, summary.entries[2].openingCash, 0.01)
        assertEquals(9000.0, summary.entries[2].closingCash, 0.01)
    }

    @Test
    fun calculatorSubtractsSavingsWithdrawalAndHandlesEmpty() {
        val empty = DateWiseCashLedgerCalculator.compute(emptyList(), emptyList(), emptyList())
        assertTrue(empty.entries.isEmpty())
        assertEquals(0.0, empty.totalCash, 0.01)

        val d1 = dateMillis(2026, Calendar.MARCH, 5)
        val summary = DateWiseCashLedgerCalculator.compute(
            savingsList = listOf(
                SavingsTransaction(
                    memberId = 1L,
                    memberCode = "01",
                    memberName = "Netai",
                    type = "DEPOSIT",
                    savingsPlan = "WEEKLY",
                    amount = 1500.0,
                    balanceAfter = 1500.0,
                    date = d1
                ),
                SavingsTransaction(
                    memberId = 1L,
                    memberCode = "01",
                    memberName = "Netai",
                    type = "WITHDRAWAL",
                    savingsPlan = "WEEKLY",
                    amount = 400.0,
                    balanceAfter = 1100.0,
                    date = d1
                )
            ),
            loansList = emptyList(),
            installmentsList = emptyList()
        )
        assertEquals(1, summary.entries.size)
        assertEquals(1100.0, summary.entries.first().netDayCollection, 0.01)
        assertEquals(1100.0, summary.totalCash, 0.01)
    }

    @Test
    fun reportsScreenDisplaysDateWiseCashLedgerBannerAndEntries() {
        val d1 = dateMillis(2026, Calendar.FEBRUARY, 18)
        val d2 = dateMillis(2026, Calendar.FEBRUARY, 25)
        val d3 = dateMillis(2026, Calendar.FEBRUARY, 28)

        runBlocking {
            val mId = repository.addMember(
                Member(
                    memberCode = "01",
                    name = "netai",
                    mobile = "",
                    savingsBalance = 0.0,
                    registeredBy = "Nitai"
                )
            )
            val member = database.memberDao().getMemberById(mId)!!
            repository.recordSavingsDeposit(member, 3000.0, "WEEKLY", "জমা", "Nitai", UserRole.ADMIN, d1)
            repository.recordSavingsDeposit(member, 3000.0, "WEEKLY", "জমা", "Nitai", UserRole.ADMIN, d2)
            val loan = repository.disburseLoan(member, 2000.0, 500.0, 10, "WEEKLY", "ব্যবসা", "Nitai", disbursedDate = d2)
            repository.recordSavingsDeposit(member, 2500.0, "WEEKLY", "জমা", "Nitai", UserRole.ADMIN, d3)
            repository.collectInstallment(loan, 2500.0, 0.0, "Nitai", "কিস্তি", paymentDate = d3)
        }

        composeRule.setContent {
            EktaSamitiTheme {
                ReportsAndAdminScreen(
                    currentUser = adminUser,
                    viewModel = viewModel
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = 5000) {
            viewModel.savingsList.value.size == 3 && viewModel.installmentsList.value.size == 1
        }

        composeRule.onNodeWithTag("cash_ledger_card").assertIsDisplayed()
        composeRule.onNodeWithTag("cash_ledger_total_banner").assertIsDisplayed()
        composeRule.onNodeWithText("18/02/26").assertIsDisplayed()
        composeRule.onNodeWithText("25/02/26").assertIsDisplayed()
        composeRule.onNodeWithText("28/02/26").assertIsDisplayed()
        composeRule.onAllNodesWithText("9000").assertCountEquals(2) // Top banner + final block row
        composeRule.onAllNodesWithText("9000").onFirst().assertIsDisplayed()
        composeRule.onAllNodesWithText("মোট আদায়=").assertCountEquals(3)
        composeRule.onNodeWithText("1000").assertIsDisplayed()
        composeRule.onNodeWithText("5000").assertIsDisplayed()
    }
}
