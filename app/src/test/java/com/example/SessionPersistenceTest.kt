package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.SessionPreferences
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.data.repository.SamitiRepository
import com.example.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SessionPersistenceTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var repository: SamitiRepository
    private lateinit var sessionPreferences: SessionPreferences

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
        sessionPreferences = SessionPreferences(context)
        sessionPreferences.clearSession()
    }

    @After
    fun teardown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun loginPersistsAcrossViewModelRecreatonUntilExplicitLogout() = runTest(testDispatcher) {
        val admin = User(
            id = 1L,
            email = "admin@ektasamiti.com",
            passwordHash = "admin123",
            fullName = "নিতাই চন্দ্র রায় (পরিচালক)",
            mobile = "01711002233",
            role = UserRole.ADMIN,
            isActive = true
        )
        repository.registerUser(admin)

        // First launch: user logs in
        val initialViewModel = AuthViewModel(repository, sessionPreferences)
        advanceUntilIdle()
        assertNull(initialViewModel.uiState.value.currentUser)

        initialViewModel.login("admin@ektasamiti.com", "admin123")
        advanceUntilIdle()
        assertEquals("admin@ektasamiti.com", initialViewModel.uiState.value.currentUser?.email)

        // Simulate app restart (new AuthViewModel instance reading SessionPreferences)
        val restartedViewModel = AuthViewModel(repository, sessionPreferences)
        advanceUntilIdle()
        assertEquals("admin@ektasamiti.com", restartedViewModel.uiState.value.currentUser?.email)
        assertEquals(false, restartedViewModel.uiState.value.isCheckingSession)

        // Explicit logout clears persisted session
        restartedViewModel.logout()
        assertNull(restartedViewModel.uiState.value.currentUser)

        val afterLogoutViewModel = AuthViewModel(repository, sessionPreferences)
        advanceUntilIdle()
        assertNull(afterLogoutViewModel.uiState.value.currentUser)
    }
}
