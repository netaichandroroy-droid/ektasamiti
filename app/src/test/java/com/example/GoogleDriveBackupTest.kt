package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.backup.SamitiBackupSerializer
import com.example.data.backup.SamitiBackupSnapshot
import com.example.data.local.AppDatabase
import com.example.data.model.InstallmentPayment
import com.example.data.model.Loan
import com.example.data.model.Member
import com.example.data.model.SavingsTransaction
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.data.repository.SamitiRepository
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.EktaSamitiTheme
import com.example.ui.viewmodel.SamitiViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
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
class GoogleDriveBackupTest {

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
    fun backupSerializerRoundTripPreservesAllRecords() {
        val snapshot = SamitiBackupSnapshot(
            exportedAtMillis = 1795000000000L,
            members = listOf(
                Member(
                    id = 5L,
                    memberCode = "01",
                    name = "রহিম উদ্দিন",
                    mobile = "01711112222",
                    savingsBalance = 1500.0,
                    activeLoanCount = 1,
                    totalLoanDue = 5500.0,
                    registrationDate = 1794000000000L
                )
            ),
            savingsTransactions = listOf(
                SavingsTransaction(
                    id = 9L,
                    memberId = 5L,
                    memberCode = "01",
                    memberName = "রহিম উদ্দিন",
                    type = "DEPOSIT",
                    amount = 500.0,
                    balanceAfter = 1500.0,
                    date = 1794500000000L,
                    receiptNumber = "REC-SAV-100"
                )
            ),
            loans = listOf(
                Loan(
                    id = 12L,
                    memberId = 0L,
                    memberCode = "NM-01",
                    memberName = "করিম মিয়া",
                    memberMobile = "01812345678",
                    principalAmount = 5000.0,
                    interestRate = 10.0,
                    chargeAmount = 500.0,
                    totalPayable = 5500.0,
                    totalPaid = 1100.0,
                    remainingDue = 4400.0,
                    totalInstallments = 10,
                    paidInstallments = 2,
                    perInstallmentAmount = 550.0,
                    disbursedDate = 1794600000000L
                )
            ),
            installmentPayments = listOf(
                InstallmentPayment(
                    id = 21L,
                    loanId = 12L,
                    memberId = 0L,
                    memberCode = "NM-01",
                    memberName = "করিম মিয়া",
                    installmentNumber = 1,
                    paidAmount = 550.0,
                    paymentDate = 1794700000000L,
                    remainingAfterPayment = 4950.0,
                    receiptNumber = "REC-INS-200"
                )
            )
        )

        val encoded = SamitiBackupSerializer.encode(snapshot)
        val decodedResult = SamitiBackupSerializer.decode(encoded)
        assertTrue(decodedResult.isSuccess)

        val decoded = decodedResult.getOrThrow()
        assertEquals(1, decoded.members.size)
        assertEquals("রহিম উদ্দিন", decoded.members[0].name)
        assertEquals(1, decoded.savingsTransactions.size)
        assertEquals("REC-SAV-100", decoded.savingsTransactions[0].receiptNumber)
        assertEquals(1, decoded.loans.size)
        assertEquals("NM-01", decoded.loans[0].memberCode)
        assertEquals(1, decoded.installmentPayments.size)
        assertEquals("REC-INS-200", decoded.installmentPayments[0].receiptNumber)
    }

    @Test
    fun backupSerializerRejectsInvalidJsonPayload() {
        assertTrue(SamitiBackupSerializer.decode("").isFailure)
        assertTrue(SamitiBackupSerializer.decode("{\"app\":\"OtherApp\"}").isFailure)
        assertTrue(SamitiBackupSerializer.decode("not json").isFailure)
    }

    @Test
    fun repositoryExportAndRestoreRemapsIdsAndPreservesUsers() = runTest(testDispatcher) {
        repository.registerUser(
            User(
                email = "Nitai",
                passwordHash = "Nemai",
                fullName = "নিতাই চন্দ্র রায়",
                mobile = "01711002233",
                role = UserRole.ADMIN
            )
        )

        val memberId = repository.addMember(
            Member(
                memberCode = "01",
                name = "সুমনা বেগম",
                mobile = "01799001122",
                savingsBalance = 0.0
            )
        )
        val member = repository.getMemberById(memberId)!!
        repository.recordSavingsDeposit(
            member = member,
            amount = 300.0,
            savingsPlan = "WEEKLY",
            note = "সাপ্তাহিক জমা",
            collectedBy = "নিতাই",
            role = UserRole.ADMIN
        )
        val loan = repository.disburseLoan(
            member = repository.getMemberById(memberId)!!,
            principalAmount = 2000.0,
            chargeAmount = 200.0,
            totalInstallments = 10,
            frequency = "WEEKLY",
            purpose = "ব্যবসা",
            disbursedBy = "নিতাই"
        )
        repository.collectInstallment(
            loan = loan,
            amount = 220.0,
            penalty = 0.0,
            collectedBy = "নিতাই",
            notes = "১ম কিস্তি"
        )

        val (jsonBackup, count) = repository.exportBackupJsonString()
        assertEquals(4, count)

        // Wipe local Samiti data before restoring
        assertTrue(repository.clearAllSamitiData(UserRole.ADMIN))
        assertEquals(0, repository.getAllMembersFlow().first().size)

        val parsed = SamitiBackupSerializer.decode(jsonBackup).getOrThrow()
        val restoredCount = repository.restoreFromBackupSnapshot(parsed)
        assertEquals(4, restoredCount)

        val restoredMembers = repository.getAllMembersFlow().first()
        val restoredSavings = repository.getAllSavingsFlow().first()
        val restoredLoans = repository.getAllLoansFlow().first()
        val restoredInstallments = repository.getAllInstallmentsFlow().first()

        assertEquals(1, restoredMembers.size)
        assertEquals("সুমনা বেগম", restoredMembers[0].name)
        assertEquals(1, restoredSavings.size)
        assertEquals(restoredMembers[0].id, restoredSavings[0].memberId)
        assertEquals(1, restoredLoans.size)
        assertEquals(restoredMembers[0].id, restoredLoans[0].memberId)
        assertEquals(1, restoredInstallments.size)
        assertEquals(restoredLoans[0].id, restoredInstallments[0].loanId)

        // Users table must stay untouched
        assertNotNull(repository.getUserByEmail("Nitai"))
    }

    @Test
    fun settingsScreenDisplaysGoogleDriveBackupOptionUnderDatabaseSection() {
        val viewModel = SamitiViewModel(repository)
        val adminUser = User(
            id = 1L,
            email = "Nitai",
            passwordHash = "Nemai",
            fullName = "নিতাই চন্দ্র রায়",
            mobile = "01711002233",
            role = UserRole.ADMIN
        )

        composeRule.setContent {
            EktaSamitiTheme {
                SettingsScreen(
                    currentUser = adminUser,
                    viewModel = viewModel
                )
            }
        }

        composeRule.onNodeWithTag("settings_screen")
            .performScrollToNode(androidx.compose.ui.test.hasTestTag("card_google_drive_backup"))

        composeRule.onNodeWithTag("card_google_drive_backup").assertIsDisplayed()
        composeRule.onNodeWithText("Google Drive ব্যাকআপ").assertIsDisplayed()
        composeRule.onNodeWithTag("btn_connect_google_drive").assertIsDisplayed()
        composeRule.onNodeWithTag("btn_export_drive_file").assertIsDisplayed()
        composeRule.onNodeWithTag("btn_import_drive_file").assertIsDisplayed()
    }
}
