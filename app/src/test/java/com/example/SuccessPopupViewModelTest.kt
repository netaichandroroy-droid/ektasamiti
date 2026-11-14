package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.Member
import com.example.data.model.UserRole
import com.example.data.repository.SamitiRepository
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SuccessPopupViewModelTest {

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

    @Test
    fun registeringNewMemberShowsAndDismissesSuccessPopup() = runTest(testDispatcher) {
        assertNull(viewModel.activeSuccessPopup.value)

        viewModel.registerNewMember(
            name = "করিম মিয়া",
            mobile = "01711223344",
            fatherOrHusbandName = "আলী মিয়া",
            nid = "1234567890",
            address = "ঢাকা",
            memberType = "WEEKLY",
            nomineeName = "সালমা",
            nomineeRelation = "স্ত্রী",
            photoUri = null,
            admissionFee = 100.0,
            initialSavings = 500.0,
            registeredBy = "Admin",
            role = UserRole.ADMIN,
            onSuccess = {}
        )
        advanceUntilIdle()

        val popup = viewModel.activeSuccessPopup.value
        assertNotNull(popup)
        assertEquals("সদস্য নিবন্ধন সফল!", popup?.title)
        assertEquals(false, popup?.isWarning)

        viewModel.dismissSuccessPopup()
        assertNull(viewModel.activeSuccessPopup.value)
    }

    @Test
    fun batchWeeklyCollectionShowsSuccessPopupWithTotals() = runTest(testDispatcher) {
        val m1Id = repository.addMember(
            Member(memberCode = "ES-1001", name = "নিতাই", mobile = "01700000001")
        )
        val m2Id = repository.addMember(
            Member(memberCode = "ES-1002", name = "রহিম", mobile = "01700000002")
        )
        val m1 = Member(id = m1Id, memberCode = "ES-1001", name = "নিতাই", mobile = "01700000001")
        val m2 = Member(id = m2Id, memberCode = "ES-1002", name = "রহিম", mobile = "01700000002")

        viewModel.batchRecordWeeklyCollections(
            collections = listOf(m1 to 100.0, m2 to 200.0),
            fridayDateMillis = System.currentTimeMillis(),
            fridayLabel = "১ম শুক্রবার",
            collectedBy = "Admin",
            role = UserRole.ADMIN,
            onComplete = {}
        )
        advanceUntilIdle()

        val popup = viewModel.activeSuccessPopup.value
        assertNotNull(popup)
        assertEquals("সাপ্তাহিক কালেকশন সফল!", popup?.title)
        assertEquals("৳300", popup?.details?.firstOrNull { it.first == "সর্বমোট আদায়" }?.second)
        assertEquals("2 জন", popup?.details?.firstOrNull { it.first == "মোট সদস্য" }?.second)
    }

    @Test
    fun addCollectorShowsSuccessPopup() = runTest(testDispatcher) {
        viewModel.addCollector(
            name = "মামুন",
            email = "mamun@ektasamiti.com",
            pass = "123456",
            mobile = "01811223344",
            onSuccess = {}
        )
        advanceUntilIdle()

        val popup = viewModel.activeSuccessPopup.value
        assertNotNull(popup)
        assertEquals("মাঠকর্মী নিয়োগ সফল!", popup?.title)
        assertEquals("মামুন", popup?.details?.firstOrNull { it.first == "নাম" }?.second)
    }
}
