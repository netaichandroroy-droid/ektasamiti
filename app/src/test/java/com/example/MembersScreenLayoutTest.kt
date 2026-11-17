package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.data.repository.SamitiRepository
import com.example.ui.screens.MemberRegistrationDialog
import com.example.ui.screens.MembersScreen
import com.example.ui.screens.WeeklyCollectionScreen
import com.example.ui.screens.calculateFridaysForMonth
import com.example.ui.theme.EktaSamitiTheme
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.SamitiViewModel
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.Calendar
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp")
class MembersScreenLayoutTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var database: AppDatabase
    private lateinit var repository: SamitiRepository
    private lateinit var samitiViewModel: SamitiViewModel
    private lateinit var authViewModel: AuthViewModel

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
        samitiViewModel = SamitiViewModel(repository)
        authViewModel = AuthViewModel(repository)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun membersScreen_hidesFilterChipsEmptyTextAndFab() {
        composeRule.setContent {
            EktaSamitiTheme {
                MembersScreen(
                    currentUser = adminUser,
                    viewModel = samitiViewModel
                )
            }
        }

        composeRule.onNodeWithTag("members_screen").assertIsDisplayed()
        composeRule.onNodeWithText("সকল সদস্য (0)").assertDoesNotExist()
        composeRule.onNodeWithText("সাপ্তাহিক সঞ্চয়ী").assertDoesNotExist()
        composeRule.onNodeWithTag("filter_upcoming_due_3d").assertDoesNotExist()
        composeRule.onNodeWithText("এখনো কোন সদস্য নিবন্ধিত হয়নি").assertDoesNotExist()
        composeRule.onNodeWithTag("btn_empty_add_member").assertDoesNotExist()
        composeRule.onNodeWithText("নতুন সদস্য যোগ করুন").assertDoesNotExist()
        composeRule.onNodeWithTag("fab_add_member").assertDoesNotExist()
        composeRule.onNodeWithText("নতুন সদস্য").assertDoesNotExist()
        composeRule.onNodeWithText("মোট সদস্য").assertDoesNotExist()
        composeRule.onNodeWithText("মোট সঞ্চয়").assertDoesNotExist()
        composeRule.onNodeWithText("বকেয়া ঋণ").assertDoesNotExist()
    }

    @Test
    fun membersScreen_showsOnlySerialNameAndTakaListWithoutExtras() {
        kotlinx.coroutines.runBlocking {
            repository.addMember(
                com.example.data.model.Member(
                    memberCode = "01",
                    name = "netai",
                    mobile = "01314277737",
                    fatherOrHusbandName = "jagodish",
                    nid = "01556636",
                    address = "jadobpur",
                    memberType = "WEEKLY",
                    savingsBalance = 500.0,
                    registeredBy = "Nitai"
                )
            )
        }

        composeRule.setContent {
            EktaSamitiTheme {
                MembersScreen(
                    currentUser = adminUser,
                    viewModel = samitiViewModel
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = 5000) {
            samitiViewModel.members.value.isNotEmpty()
        }
        composeRule.onNodeWithTag("members_list_header").assertIsDisplayed()
        composeRule.onNodeWithText("SL").assertIsDisplayed()
        composeRule.onNodeWithText("নাম").assertIsDisplayed()
        composeRule.onNodeWithText("টাকা").assertIsDisplayed()

        composeRule.onNodeWithTag("member_card_01").assertIsDisplayed()
        composeRule.onNodeWithText("01").assertIsDisplayed()
        composeRule.onNodeWithText("netai").assertIsDisplayed()
        composeRule.onNodeWithTag("btn_member_submit_01")
            .assertIsDisplayed()
            .assertTextEquals("netai - সাবমিট")
        composeRule.onNodeWithText("৳500").assertIsDisplayed()

        // Ensure extra badges, contact details, loan cards, and date-edit buttons do NOT show on the members list
        composeRule.onNodeWithText("সাপ্তাহিক").assertDoesNotExist()
        composeRule.onNodeWithText("বকেয়া ঋণ").assertDoesNotExist()
        composeRule.onNodeWithText("সঞ্চয় স্থিতি").assertDoesNotExist()
        composeRule.onNodeWithText("বিস্তারিত").assertDoesNotExist()
        composeRule.onNodeWithText("কল করুন").assertDoesNotExist()
        composeRule.onNodeWithTag("btn_edit_member_date_01").assertDoesNotExist()
    }

    @Test
    fun memberNameWiseSubmitButton_addsDateWiseAmountToWeeklyCollection() {
        val now = Calendar.getInstance()
        val firstFriday = calculateFridaysForMonth(
            year = now.get(Calendar.YEAR),
            monthIndex = now.get(Calendar.MONTH)
        ).first()

        kotlinx.coroutines.runBlocking {
            repository.addMember(
                com.example.data.model.Member(
                    memberCode = "01",
                    name = "netai",
                    mobile = "",
                    memberType = "WEEKLY",
                    savingsBalance = 500.0,
                    registeredBy = "Nitai"
                )
            )
        }

        val showWeeklyState = androidx.compose.runtime.mutableStateOf(false)

        composeRule.setContent {
            EktaSamitiTheme {
                if (!showWeeklyState.value) {
                    MembersScreen(
                        currentUser = adminUser,
                        viewModel = samitiViewModel
                    )
                } else {
                    WeeklyCollectionScreen(
                        currentUser = adminUser,
                        viewModel = samitiViewModel
                    )
                }
            }
        }

        composeRule.waitUntil(timeoutMillis = 5000) {
            samitiViewModel.members.value.isNotEmpty()
        }

        // Tap name-wise submit button next to netai's name
        composeRule.onNodeWithTag("btn_member_submit_01")
            .assertIsDisplayed()
            .assertTextEquals("netai - সাবমিট")
            .performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("dialog_member_weekly_submit").assertIsDisplayed()
        composeRule.onNodeWithTag("chip_member_week_${firstFriday.columnKey}")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag("input_member_weekly_savings")
            .performScrollTo()
            .performTextReplacement("250")
        composeRule.onNodeWithTag("btn_confirm_member_weekly_submit")
            .performScrollTo()
            .assertTextEquals("netai - সাবমিট নিশ্চিত")
            .performClick()

        composeRule.waitUntil(timeoutMillis = 5000) {
            samitiViewModel.members.value.any { it.savingsBalance == 750.0 }
        }
        composeRule.waitForIdle()

        // After submit, the submit button must no longer show for this member
        composeRule.onNodeWithTag("btn_member_submit_01").assertDoesNotExist()
        composeRule.onNodeWithText("netai - সাবমিট").assertDoesNotExist()

        // Verify transaction stored with selected date and updated balance
        kotlinx.coroutines.runBlocking {
            val savedTx = database.savingsDao().getAllTransactionsList().first()
            org.junit.Assert.assertEquals(250.0, savedTx.amount, 0.01)
            org.junit.Assert.assertTrue(
                com.example.ui.components.SamitiDateUtils.isSameLocalDay(savedTx.date, firstFriday.dateMillis)
            )
            val updatedMember = database.memberDao().getMemberByCode("01")!!
            org.junit.Assert.assertEquals(750.0, updatedMember.savingsBalance, 0.01)
        }

        // Switch to Weekly Collection Screen and verify 250 appears in the date-wise spreadsheet
        showWeeklyState.value = true
        composeRule.waitUntil(timeoutMillis = 5000) {
            samitiViewModel.savingsList.value.any { it.amount == 250.0 }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("weekly_collection_screen").assertIsDisplayed()
        composeRule.onNodeWithText(firstFriday.shortDateString).assertIsDisplayed()
        composeRule.onNodeWithText("netai").assertIsDisplayed()
        org.junit.Assert.assertTrue(
            composeRule.onAllNodes(androidx.compose.ui.test.hasText("250")).fetchSemanticsNodes().isNotEmpty()
        )

        // Switch back to MembersScreen and ensure submit button remains hidden
        showWeeklyState.value = false
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("members_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("btn_member_submit_01").assertDoesNotExist()
    }

    @Test
    fun memberRegistrationDialog_showsOnlySerialNameAndTakaFields() {
        composeRule.setContent {
            EktaSamitiTheme {
                MemberRegistrationDialog(
                    currentUser = adminUser,
                    viewModel = samitiViewModel,
                    onDismiss = {}
                )
            }
        }

        composeRule.onNodeWithTag("dialog_member_registration").assertIsDisplayed()
        composeRule.onNodeWithTag("input_auto_serial_number").assertIsDisplayed()
        composeRule.onNodeWithText("01").assertIsDisplayed()
        composeRule.onNodeWithTag("input_member_name").assertIsDisplayed()
        composeRule.onNodeWithTag("input_member_taka").assertIsDisplayed()

        // Removed fields should no longer exist in New Member Entry
        composeRule.onNodeWithTag("input_member_mobile").assertDoesNotExist()
        composeRule.onNodeWithTag("input_member_father_or_husband").assertDoesNotExist()
        composeRule.onNodeWithTag("input_member_id_no").assertDoesNotExist()
        composeRule.onNodeWithTag("input_member_gram").assertDoesNotExist()
        composeRule.onNodeWithTag("btn_pick_member_reg_date").assertDoesNotExist()
        composeRule.onNodeWithText("সদস্যের ছবি / প্রোফাইল ছবি").assertDoesNotExist()
        composeRule.onNodeWithText("নমিনীর নাম").assertDoesNotExist()
        composeRule.onNodeWithText("নমিনীর সাথে সম্পর্ক").assertDoesNotExist()
        composeRule.onNodeWithText("ভর্তি ফি (৳)").assertDoesNotExist()
    }

    @Test
    fun memberRegistrationDialog_submitsSerialNameAndTakaCleanly() {
        var dismissed = false
        composeRule.setContent {
            EktaSamitiTheme {
                MemberRegistrationDialog(
                    currentUser = adminUser,
                    viewModel = samitiViewModel,
                    onDismiss = { dismissed = true }
                )
            }
        }

        composeRule.onNodeWithTag("input_member_name").performTextInput("netai")
        composeRule.onNodeWithTag("input_member_taka").performTextInput("500")
        composeRule.onNodeWithTag("btn_submit_member").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 5000) { dismissed }
        composeRule.waitForIdle()

        kotlinx.coroutines.runBlocking {
            val savedMembers = database.memberDao().getAllMembersList()
            org.junit.Assert.assertEquals(1, savedMembers.size)
            val saved = savedMembers.first()
            org.junit.Assert.assertEquals("01", saved.memberCode)
            org.junit.Assert.assertEquals("netai", saved.name)
            org.junit.Assert.assertEquals("", saved.mobile)
            org.junit.Assert.assertEquals(0.0, saved.admissionFee, 0.01)
            org.junit.Assert.assertEquals(500.0, saved.savingsBalance, 0.01)
            org.junit.Assert.assertTrue(dismissed)
        }
    }

    @Test
    fun mainAppScaffold_topHeaderAddButtonOpensMemberRegistrationDialog() {
        composeRule.setContent {
            EktaSamitiTheme {
                MainAppScaffold(
                    currentUser = adminUser,
                    authViewModel = authViewModel,
                    samitiViewModel = samitiViewModel
                )
            }
        }

        composeRule.onNodeWithTag("btn_top_header_add").assertIsDisplayed()
        composeRule.onNodeWithTag("btn_top_header_add").performClick()
        composeRule.onNodeWithTag("dialog_member_registration").assertIsDisplayed()
    }

    @Test
    fun autoSerialNumber_incrementsSequentiallyWithoutCollision() {
        kotlinx.coroutines.runBlocking {
            org.junit.Assert.assertEquals("01", repository.getNextMemberCode())

            repository.addMember(
                com.example.data.model.Member(
                    memberCode = "01",
                    name = "First Member",
                    mobile = "01711111111",
                    memberType = "WEEKLY",
                    registeredBy = "Nitai"
                )
            )
            org.junit.Assert.assertEquals("02", repository.getNextMemberCode())

            val m2Id = repository.addMember(
                com.example.data.model.Member(
                    memberCode = "02",
                    name = "Second Member",
                    mobile = "01722222222",
                    memberType = "WEEKLY",
                    registeredBy = "Nitai"
                )
            )
            org.junit.Assert.assertEquals("03", repository.getNextMemberCode())

            // Even if an earlier member is removed, next serial stays strictly ahead
            val first = repository.getMemberById(1L)
            if (first != null) {
                repository.deleteMember(first, UserRole.ADMIN)
            }
            org.junit.Assert.assertEquals("03", repository.getNextMemberCode())
            org.junit.Assert.assertTrue(m2Id > 0L)
        }
    }
}
