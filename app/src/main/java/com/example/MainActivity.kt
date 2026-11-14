package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.cloud.CloudAutoSyncManager
import com.example.data.cloud.CloudSyncState
import com.example.data.cloud.CloudSyncStatus
import com.example.data.cloud.GoogleAuthManager
import com.example.data.cloud.GoogleAuthManager.authStateFlow
import com.example.data.local.AppDatabase
import com.example.data.local.SessionPreferences
import com.example.data.model.Member
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.data.repository.SamitiRepository
import com.example.notifications.LoanReminderCalculator
import com.example.notifications.LoanReminderNotificationHelper
import com.example.notifications.LoanReminderPreferences
import com.example.ui.components.ReceiptDialog
import com.example.ui.components.SuccessPopupDialog
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.LoansScreen
import com.example.ui.screens.MembersScreen
import com.example.ui.screens.ReportsAndAdminScreen
import com.example.ui.screens.SavingsScreen
import com.example.ui.screens.WeeklyCollectionScreen
import com.example.ui.theme.EktaSamitiTheme
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.SamitiViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val openLoansFromNotificationState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (intent?.getBooleanExtra(LoanReminderNotificationHelper.EXTRA_OPEN_LOANS, false) == true) {
            openLoansFromNotificationState.value = true
        }

        val database = AppDatabase.getInstance(this)
        val cloudAutoSyncManager = CloudAutoSyncManager(applicationContext, database)
        val repository = SamitiRepository(database, cloudAutoSyncManager)
        val sessionPreferences = SessionPreferences(applicationContext)

        setContent {
            EktaSamitiTheme {
                val context = LocalContext.current
                val scope = rememberCoroutineScope()
                val credentialManager = remember(context) { CredentialManager.create(context) }

                val authViewModel = remember { AuthViewModel(repository, sessionPreferences) }
                val samitiViewModel = remember { SamitiViewModel(repository) }

                val authState by authViewModel.uiState.collectAsState()
                val currentUser = authState.currentUser
                val firebaseUser by Firebase.auth.authStateFlow().collectAsStateWithLifecycle(
                    initialValue = Firebase.auth.currentUser
                )
                val cloudSyncState by cloudAutoSyncManager.syncState.collectAsStateWithLifecycle()

                // Silent Google auto-sign-in attempt on startup
                LaunchedEffect(Unit) {
                    if (Firebase.auth.currentUser == null) {
                        GoogleAuthManager.attemptAutoSignIn(
                            context = context,
                            credentialManager = credentialManager,
                            scope = scope,
                            onAuthSuccess = { fbUser ->
                                authViewModel.onGoogleFirebaseSignedIn(
                                    email = fbUser.email,
                                    displayName = fbUser.displayName
                                )
                            },
                            onUnauthenticated = {}
                        )
                    }
                }

                // Auth-gate all Firestore listeners and sync operations behind firebaseUser != null
                DisposableEffect(firebaseUser?.uid) {
                    val activeFbUser = firebaseUser
                    if (activeFbUser != null) {
                        cloudAutoSyncManager.startForUser(activeFbUser.uid, activeFbUser.email)
                    } else {
                        cloudAutoSyncManager.stop()
                    }
                    onDispose {
                        if (Firebase.auth.currentUser == null) {
                            cloudAutoSyncManager.stop()
                        }
                    }
                }

                when {
                    authState.isCheckingSession -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    currentUser == null -> {
                        LoginScreen(
                            authViewModel = authViewModel,
                            onLoginSuccess = {}
                        )
                    }
                    else -> {
                        MainAppScaffold(
                            currentUser = currentUser,
                            authViewModel = authViewModel,
                            samitiViewModel = samitiViewModel,
                            cloudSyncState = cloudSyncState,
                            onSyncNow = { cloudAutoSyncManager.triggerAutoSyncNow() },
                            onGoogleSignInSuccess = {
                                Firebase.auth.currentUser?.let { fbUser ->
                                    cloudAutoSyncManager.startForUser(fbUser.uid, fbUser.email)
                                }
                            },
                            onLogout = { authViewModel.logout(credentialManager) },
                            openLoansFromNotification = openLoansFromNotificationState.value,
                            onConsumedNotificationNavigation = { openLoansFromNotificationState.value = false }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(LoanReminderNotificationHelper.EXTRA_OPEN_LOANS, false)) {
            openLoansFromNotificationState.value = true
        }
    }
}

data class NavTabItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
    val isCenterButton: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(
    currentUser: User,
    authViewModel: AuthViewModel,
    samitiViewModel: SamitiViewModel,
    cloudSyncState: CloudSyncState = CloudSyncState(),
    onSyncNow: () -> Unit = {},
    onGoogleSignInSuccess: () -> Unit = {},
    onLogout: () -> Unit = { authViewModel.logout() },
    openLoansFromNotification: Boolean = false,
    onConsumedNotificationNavigation: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Home, 1: Members, 2: Weekly Collection (+), 3: Savings/Loans, 4: Reports
    val snackbarHostState = remember { SnackbarHostState() }

    val snackbarMsg by samitiViewModel.snackbarMessage.collectAsState()
    val activeReceipt by samitiViewModel.activeReceipt.collectAsState()
    val activeSuccessPopup by samitiViewModel.activeSuccessPopup.collectAsState()
    val loansList by samitiViewModel.loansList.collectAsState()

    // Trigger snackbar when ViewModel posts message
    LaunchedEffect(snackbarMsg) {
        snackbarMsg?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            samitiViewModel.clearSnackbar()
        }
    }

    // Modal shortcut triggers
    var memberForDeposit by remember { mutableStateOf<Member?>(null) }
    var memberForLoan by remember { mutableStateOf<Member?>(null) }
    var openAddMemberDirectly by remember { mutableStateOf(false) }
    var openDepositDirectly by remember { mutableStateOf(false) }
    var openNewLoanDirectly by remember { mutableStateOf(false) }
    var openLoansSubTabDirectly by remember { mutableStateOf(false) }
    var openInstallmentDirectly by remember { mutableStateOf(false) }
    var openWeeklyCollectionPopupDirectly by remember { mutableStateOf(false) }

    // Route directly to Loans sub-tab when user taps a local reminder notification
    LaunchedEffect(openLoansFromNotification) {
        if (openLoansFromNotification) {
            selectedTab = 3
            openLoansSubTabDirectly = true
            onConsumedNotificationNavigation()
        }
    }

    // Schedule daily reminder and run once-per-day check for the Director
    LaunchedEffect(currentUser.id, loansList) {
        if (currentUser.role == UserRole.ADMIN) {
            val settings = LoanReminderPreferences.getSettings(context)
            if (settings.isEnabled) {
                LoanReminderNotificationHelper.scheduleDailyReminder(context)
                if (loansList.isNotEmpty() &&
                    LoanReminderNotificationHelper.hasNotificationPermission(context) &&
                    !LoanReminderPreferences.wasNotifiedToday(context)
                ) {
                    val upcoming = LoanReminderCalculator.findUpcomingWeeklyRepayments(
                        loans = loansList,
                        leadDays = settings.leadDays
                    )
                    if (upcoming.isNotEmpty()) {
                        val sent = LoanReminderNotificationHelper.showWeeklyRepaymentNotification(
                            context = context,
                            upcoming = upcoming,
                            isTestNotification = false
                        )
                        if (sent) {
                            LoanReminderPreferences.markNotifiedToday(context)
                        }
                    }
                }
            }
        }
    }

    val navTabs = listOf(
        NavTabItem("হোম", Icons.Filled.Dashboard, Icons.Outlined.Dashboard, "tab_home"),
        NavTabItem("সদস্য", Icons.Filled.Group, Icons.Outlined.Group, "tab_members"),
        NavTabItem("Weekly Collection", Icons.Filled.Add, Icons.Filled.Add, "tab_weekly_collection", isCenterButton = true),
        NavTabItem("সঞ্চয় ও ঋণ", Icons.Filled.Savings, Icons.Outlined.Savings, "tab_savings_loans"),
        NavTabItem("হিসাব", Icons.Filled.Assessment, Icons.Outlined.Assessment, "tab_reports")
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_ekta_samiti_logo_1794238685688),
                            contentDescription = "Logo",
                            modifier = Modifier.size(32.dp).clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "একতা সমিতি",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    // Compact live Cloud Auto-Sync status indicator in TopAppBar
                    IconButton(
                        onClick = {
                            if (cloudSyncState.status == CloudSyncStatus.NEEDS_GOOGLE_SIGN_IN) {
                                selectedTab = 4
                            } else {
                                onSyncNow()
                            }
                        },
                        modifier = Modifier.testTag("btn_topbar_sync_status")
                    ) {
                        when (cloudSyncState.status) {
                            CloudSyncStatus.SYNCING -> CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color(0xFF1D4ED8),
                                strokeWidth = 2.dp
                            )
                            CloudSyncStatus.SYNCED -> Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = "ক্লাউড সিঙ্ক সম্পন্ন",
                                tint = Color(0xFF0D683B)
                            )
                            CloudSyncStatus.OFFLINE_PENDING -> Icon(
                                imageVector = Icons.Default.CloudOff,
                                contentDescription = "অফলাইন অপেক্ষমাণ",
                                tint = Color(0xFFB45309)
                            )
                            else -> Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = "ক্লাউড অটো সিঙ্ক",
                                tint = Color(0xFF1D4ED8)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (currentUser.role == UserRole.ADMIN) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = if (currentUser.role == UserRole.ADMIN) "অ্যাডমিন" else "কালেক্টর",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (currentUser.role == UserRole.ADMIN) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    IconButton(
                        onClick = onLogout,
                        modifier = Modifier.testTag("btn_logout")
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "লগআউট", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("bottom_nav_bar"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                navTabs.forEachIndexed { index, tab ->
                    if (tab.isCenterButton) {
                        // Center Round (+) Button for Weekly Collection
                        val isSelected = selectedTab == index
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                selectedTab = index
                                openWeeklyCollectionPopupDirectly = true
                            },
                            icon = {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .shadow(elevation = if (isSelected) 6.dp else 3.dp, shape = CircleShape)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else Color(0xFF0D683B)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Weekly Collection",
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            },
                            label = {
                                Text(
                                    text = "Weekly Collection",
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.testTag("btn_center_weekly_collection")
                        )
                    } else {
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == index) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.title
                                )
                            },
                            label = {
                                Text(
                                    text = tab.title,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    fontSize = 11.sp
                                )
                            },
                            modifier = Modifier.testTag(tab.testTag)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> DashboardScreen(
                    currentUser = currentUser,
                    viewModel = samitiViewModel,
                    onNavigateToTab = { selectedTab = it },
                    onOpenAddMember = {
                        selectedTab = 1
                        openAddMemberDirectly = true
                    },
                    onOpenDeposit = {
                        selectedTab = 3
                        openDepositDirectly = true
                    },
                    onOpenNewLoan = {
                        selectedTab = 3
                        openNewLoanDirectly = true
                    },
                    onOpenCollectInstallment = {
                        // Open Weekly Collection Screen
                        selectedTab = 2
                    },
                    onOpenLoansTab = {
                        selectedTab = 3
                        openLoansSubTabDirectly = true
                    },
                    onShowMessage = { msg ->
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(msg)
                        }
                    },
                    cloudSyncState = cloudSyncState,
                    onSyncNow = onSyncNow,
                    onGoogleSignInSuccess = onGoogleSignInSuccess
                )

                1 -> MembersScreen(
                    currentUser = currentUser,
                    viewModel = samitiViewModel,
                    showAddDialogInitially = openAddMemberDirectly,
                    onOpenDepositForMember = { member ->
                        memberForDeposit = member
                        selectedTab = 3
                    },
                    onOpenLoanForMember = { member ->
                        memberForLoan = member
                        selectedTab = 3
                    }
                )

                2 -> WeeklyCollectionScreen(
                    currentUser = currentUser,
                    viewModel = samitiViewModel,
                    initiallyOpenAddPopup = openWeeklyCollectionPopupDirectly,
                    onPopupDismissed = { openWeeklyCollectionPopupDirectly = false }
                )

                3 -> SavingsAndLoansSubScreen(
                    currentUser = currentUser,
                    viewModel = samitiViewModel,
                    preselectedMemberForDeposit = memberForDeposit,
                    preselectedMemberForLoan = memberForLoan,
                    initialDepositOpen = openDepositDirectly,
                    initialNewLoanOpen = openNewLoanDirectly,
                    openLoansTabInitially = openLoansSubTabDirectly,
                    onConsumedLoansTabFlag = { openLoansSubTabDirectly = false }
                )

                4 -> ReportsAndAdminScreen(
                    currentUser = currentUser,
                    viewModel = samitiViewModel,
                    onShowMessage = { msg ->
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(msg)
                        }
                    },
                    cloudSyncState = cloudSyncState,
                    onSyncNow = onSyncNow,
                    onGoogleSignInSuccess = onGoogleSignInSuccess
                )
            }
        }
    }

    // Reset one-time flags after consumption
    if (openAddMemberDirectly) openAddMemberDirectly = false
    if (openDepositDirectly) openDepositDirectly = false
    if (openNewLoanDirectly) openNewLoanDirectly = false
    if (openInstallmentDirectly) openInstallmentDirectly = false
    if (memberForDeposit != null && selectedTab != 3) memberForDeposit = null
    if (memberForLoan != null && selectedTab != 3) memberForLoan = null

    // Branded Money Receipt Dialog Modal
    activeReceipt?.let { receipt ->
        ReceiptDialog(
            receipt = receipt,
            onDismiss = { samitiViewModel.closeReceipt() }
        )
    }

    // Global Success Pop-Up Modal on Data Submissions
    activeSuccessPopup?.let { popupData ->
        SuccessPopupDialog(
            data = popupData,
            onDismiss = { samitiViewModel.dismissSuccessPopup() }
        )
    }
}

@Composable
fun SavingsAndLoansSubScreen(
    currentUser: User,
    viewModel: SamitiViewModel,
    preselectedMemberForDeposit: Member?,
    preselectedMemberForLoan: Member?,
    initialDepositOpen: Boolean,
    initialNewLoanOpen: Boolean,
    openLoansTabInitially: Boolean = false,
    onConsumedLoansTabFlag: () -> Unit = {}
) {
    var subTab by remember {
        mutableIntStateOf(if (preselectedMemberForLoan != null || initialNewLoanOpen || openLoansTabInitially) 1 else 0)
    }

    LaunchedEffect(openLoansTabInitially) {
        if (openLoansTabInitially) {
            subTab = 1
            onConsumedLoansTabFlag()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = subTab,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Tab(
                selected = subTab == 0,
                onClick = { subTab = 0 },
                text = { Text("সাপ্তাহিক সঞ্চয় জমা", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Savings, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = subTab == 1,
                onClick = { subTab = 1 },
                text = { Text("ঋণ ও কিস্তি", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (subTab == 0) {
                SavingsScreen(
                    currentUser = currentUser,
                    viewModel = viewModel,
                    preselectedMemberForDeposit = preselectedMemberForDeposit,
                    showDepositDialogInitially = initialDepositOpen
                )
            } else {
                LoansScreen(
                    currentUser = currentUser,
                    viewModel = viewModel,
                    preselectedMemberForLoan = preselectedMemberForLoan,
                    showNewLoanInitially = initialNewLoanOpen
                )
            }
        }
    }
}
