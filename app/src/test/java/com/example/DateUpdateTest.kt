package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.LoanScheduleCalculator
import com.example.data.model.Member
import com.example.data.model.UserRole
import com.example.data.repository.SamitiRepository
import com.example.ui.components.SamitiDateUtils
import com.example.ui.viewmodel.SamitiViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DateUpdateTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var repository: SamitiRepository
    private lateinit var viewModel: SamitiViewModel

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
        viewModel = SamitiViewModel(repository)
    }

    @After
    fun teardown() {
        database.close()
        Dispatchers.resetMain()
    }

    private fun dateMillis(year: Int, monthZeroBased: Int, day: Int, hour: Int = 10): Long {
        return Calendar.getInstance().apply {
            set(year, monthZeroBased, day, hour, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    @Test
    fun datePickerRoundTripPreservesLocalCalendarDay() {
        val original = dateMillis(2025, Calendar.MARCH, 15, hour = 14)
        val utcPicker = SamitiDateUtils.localTimestampToUtcPickerMillis(original)
        val roundTripped = SamitiDateUtils.utcPickerMillisToLocalTimestamp(utcPicker, original)

        assertTrue(SamitiDateUtils.isSameLocalDay(original, roundTripped))

        val tomorrow = dateMillis(2025, Calendar.MARCH, 16, hour = 14)
        assertFalse(SamitiDateUtils.isSameLocalDay(original, tomorrow))
    }

    @Test
    fun customCreationDatesAndReceiptTimestampsAreStoredAccurately() = runTest(testDispatcher) {
        val customRegDate = dateMillis(2025, Calendar.JANUARY, 5)
        val customDepositDate = dateMillis(2025, Calendar.JANUARY, 12)
        val customLoanDate = dateMillis(2025, Calendar.FEBRUARY, 1)
        val customPaymentDate = dateMillis(2025, Calendar.FEBRUARY, 8)

        viewModel.registerNewMember(
            name = "রফিকুল ইসলাম",
            mobile = "01711112222",
            fatherOrHusbandName = "",
            nid = "",
            address = "",
            memberType = "WEEKLY",
            nomineeName = "",
            nomineeRelation = "",
            photoUri = null,
            admissionFee = 100.0,
            initialSavings = 300.0,
            registeredBy = "Admin",
            role = UserRole.ADMIN,
            registrationDate = customRegDate,
            onSuccess = {}
        )
        advanceUntilIdle()

        val savedMembers = database.memberDao().getAllMembersList()
        assertEquals(1, savedMembers.size)
        val member = savedMembers.first()
        assertEquals(customRegDate, member.registrationDate)

        // Custom deposit date propagates to savings transaction and ReceiptData
        viewModel.depositSavings(
            member = member,
            amount = 200.0,
            savingsPlan = "WEEKLY",
            note = "সাপ্তাহিক জমা",
            collectedBy = "Admin",
            role = UserRole.ADMIN,
            customDate = customDepositDate,
            onComplete = {}
        )
        advanceUntilIdle()
        assertEquals(customDepositDate, viewModel.activeReceipt.value?.timestamp)

        // Custom loan disbursal date propagates to loan and ReceiptData
        val updatedMember = database.memberDao().getMemberById(member.id)!!
        viewModel.disburseLoan(
            member = updatedMember,
            principalAmount = 10000.0,
            chargeAmount = 1000.0,
            totalInstallments = 10,
            frequency = "WEEKLY",
            purpose = "ব্যবসা",
            disbursedBy = "Admin",
            interestRate = 10.0,
            disbursedDate = customLoanDate,
            onComplete = {}
        )
        advanceUntilIdle()
        assertEquals(customLoanDate, viewModel.activeReceipt.value?.timestamp)

        val loan = database.loanDao().getAllLoansList().first()
        assertEquals(customLoanDate, loan.disbursedDate)

        // Custom installment payment date propagates to payment and ReceiptData
        viewModel.collectInstallment(
            loan = loan,
            amount = 1100.0,
            penalty = 0.0,
            collectedBy = "Admin",
            notes = "১ম কিস্তি",
            paymentDate = customPaymentDate,
            onComplete = {}
        )
        advanceUntilIdle()
        assertEquals(customPaymentDate, viewModel.activeReceipt.value?.timestamp)

        val payment = database.installmentDao().getAllPaymentsList().first()
        assertEquals(customPaymentDate, payment.paymentDate)
    }

    @Test
    fun updatingExistingRecordDatesUpdatesDatabaseAndLoanSchedule() = runTest(testDispatcher) {
        val mId = repository.addMember(
            Member(
                memberCode = "ES-2001",
                name = "সালমা বেগম",
                mobile = "01811112222",
                registrationDate = dateMillis(2025, Calendar.JANUARY, 1)
            )
        )
        val member = database.memberDao().getMemberById(mId)!!
        val tx = repository.recordSavingsDeposit(
            member = member,
            amount = 500.0,
            savingsPlan = "WEEKLY",
            note = "জমা",
            collectedBy = "Admin",
            role = UserRole.ADMIN,
            customDate = dateMillis(2025, Calendar.JANUARY, 2)
        )
        val loan = repository.disburseLoan(
            member = database.memberDao().getMemberById(mId)!!,
            principalAmount = 5000.0,
            chargeAmount = 500.0,
            totalInstallments = 5,
            frequency = "WEEKLY",
            purpose = "কৃষি",
            disbursedBy = "Admin",
            disbursedDate = dateMillis(2025, Calendar.JANUARY, 10)
        )
        val payment = repository.collectInstallment(
            loan = loan,
            amount = 1100.0,
            penalty = 0.0,
            collectedBy = "Admin",
            notes = "কিস্তি",
            paymentDate = dateMillis(2025, Calendar.JANUARY, 17)
        )

        val newRegDate = dateMillis(2025, Calendar.FEBRUARY, 1)
        val newTxDate = dateMillis(2025, Calendar.FEBRUARY, 2)
        val newLoanDate = dateMillis(2025, Calendar.FEBRUARY, 10)
        val newPayDate = dateMillis(2025, Calendar.FEBRUARY, 17)

        viewModel.updateMemberRegistrationDate(member, newRegDate)
        viewModel.updateSavingsTransactionDate(tx, newTxDate)
        viewModel.updateLoanDisbursedDate(loan, newLoanDate)
        viewModel.updateInstallmentPaymentDate(payment, newPayDate)
        advanceUntilIdle()

        assertEquals(newRegDate, database.memberDao().getMemberById(mId)?.registrationDate)
        assertEquals(newTxDate, database.savingsDao().getTransactionByReceipt(tx.receiptNumber)?.date)

        val updatedLoan = database.loanDao().getLoanById(loan.id)
        assertNotNull(updatedLoan)
        assertEquals(newLoanDate, updatedLoan?.disbursedDate)

        val updatedPayment = database.installmentDao().getPaymentByReceipt(payment.receiptNumber)
        assertEquals(newPayDate, updatedPayment?.paymentDate)

        // Verify schedule recalculates first due date 7 days after newLoanDate
        val schedule = LoanScheduleCalculator.generateSchedule(updatedLoan!!, listOf(updatedPayment!!))
        val expectedFirstDue = LoanScheduleCalculator.computeDueDateMillis(newLoanDate, 1, "WEEKLY")
        assertEquals(expectedFirstDue, schedule.first().dueDateMillis)
        assertEquals(newPayDate, schedule.first().paidDateMillis)
    }
}
