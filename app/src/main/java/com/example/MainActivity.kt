package com.example

import android.content.Intent
import android.os.Bundle
import java.util.TimeZone
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.cloud.CloudAutoSyncManager
import com.example.data.cloud.FirebaseSafeHelper
import com.example.data.cloud.GoogleAuthManager
import com.example.data.local.AppDatabase
import com.example.data.local.SessionPreferences
import com.example.data.model.Member
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.data.repository.SamitiRepository
import com.example.notifications.LoanReminderCalculator
import com.example.notifications.LoanReminderNotificationHelper
import com.example.notifications.LoanReminderPreferences
import com.example.ui.components.AppUpdateAvailableDialog
import com.example.ui.components.ReceiptDialog
import com.example.ui.components.SuccessPopupDialog
import com.example.update.AppUpdatePreferences
import com.example.update.GitHubReleaseInfo
import com.example.update.GitHubUpdateChecker
import com.example.update.UpdateCheckResult
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.LoansScreen
import com.example.ui.screens.MembersScreen
import com.example.ui.screens.ReportsAndAdminScreen
import com.example.ui.screens.SavingsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.WeeklyCollectionScreen
import com.example.ui.theme.EktaSamitiTheme
import com.example.ui.theme.SilverCardBorder
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.SamitiViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val openLoansFromNotificationState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Dhaka"))
        } catch (_: Exception) {
        }
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
                val credentialManager = remember(context) {
                    try {
                        CredentialManager.create(context)
                    } catch (_: Throwable) {
                        null
                    }
                }

                val authViewModel = remember { AuthViewModel(repository, sessionPreferences) }
                val samitiViewModel = remember { SamitiViewModel(repository) }

                val authState by authViewModel.uiState.collectAsState()
                val currentUser = authState.currentUser
                val firebaseUser by remember(context) {
                    FirebaseSafeHelper.authStateFlow(context)
                }.collectAsStateWithLifecycle(
                    initialValue = FirebaseSafeHelper.authOrNull(context)?.currentUser
                )

                // Silent Google auto-sign-in attempt on startup
                LaunchedEffect(Unit) {
                    val cm = credentialManager ?: return@LaunchedEffect
                    if (FirebaseSafeHelper.authOrNull(context)?.currentUser == null) {
                        GoogleAuthManager.attemptAutoSignIn(
                            context = context,
                            credentialManager = cm,
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
                        if (FirebaseSafeHelper.authOrNull(context)?.currentUser == null) {
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
    onLogout: () -> Unit = { authViewModel.logout() },
    openLoansFromNotification: Boolean = false,
    onConsumedNotificationNavigation: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Home, 1: Members, 2: Weekly Collection (+), 3: Savings/Loans, 4: Reports, 5: Settings
    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler(enabled = selectedTab != 0) {
        selectedTab = 0
    }

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

    var pendingAppUpdate by remember { mutableStateOf<GitHubReleaseInfo?>(null) }

    // Route directly to Loans sub-tab when user taps a local reminder notification
    LaunchedEffect(openLoansFromNotification) {
        if (openLoansFromNotification) {
            selectedTab = 3
            openLoansSubTabDirectly = true
            onConsumedNotificationNavigation()
        }
    }

    // Automatically check GitHub Releases for a newer APK build on startup
    LaunchedEffect(Unit) {
        if (AppUpdatePreferences.isAutoCheckEnabled(context)) {
            when (val result = GitHubUpdateChecker.checkForUpdate(context)) {
                is UpdateCheckResult.UpdateAvailable -> {
                    val dismissedTag = AppUpdatePreferences.getDismissedTag(context)
                    if (result.release.tagName != dismissedTag) {
                        pendingAppUpdate = result.release
                    }
                }
                else -> Unit
            }
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
        NavTabItem("Weekly", Icons.Filled.Add, Icons.Filled.Add, "tab_weekly_collection", isCenterButton = true),
        NavTabItem("সঞ্চয়/ঋণ", Icons.Filled.Savings, Icons.Outlined.Savings, "tab_savings_loans"),
        NavTabItem("হিসাব", Icons.Filled.Assessment, Icons.Outlined.Assessment, "tab_reports"),
        NavTabItem("সেটিংস", Icons.Filled.Settings, Icons.Outlined.Settings, "tab_settings")
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(
                color = Color(0xFFFFFFFF),
                shadowElevation = 3.dp
            ) {
                Column {
                    CenterAlignedTopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), CircleShape)
                                        .padding(2.dp)
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_ekta_samiti_logo_1794238685688),
                                        contentDescription = "Logo",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "একতা সমিতি",
                                    fontWeight = FontWeight.ExtraBold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = onLogout,
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .testTag("btn_logout")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFEE2E2).copy(alpha = 0.7f))
                                        .border(1.dp, Color(0xFFDC2626).copy(alpha = 0.25f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                        contentDescription = "লগআউট",
                                        tint = Color(0xFFB91C1C),
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = Color.Transparent
                        )
                    )
                    HorizontalDivider(thickness = 1.dp, color = SilverCardBorder.copy(alpha = 0.7f))
                }
            }
        },
        bottomBar = {
            Surface(
                shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
                color = Color(0xFFFFFFFF),
                tonalElevation = 6.dp,
                shadowElevation = 14.dp,
                border = BorderStroke(1.dp, SilverCardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bottom_nav_bar")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    navTabs.forEachIndexed { index, tab ->
                        val isSelected = selectedTab == index
                        if (tab.isCenterButton) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 56.dp)
                                    .minimumInteractiveComponentSize()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        selectedTab = index
                                        openWeeklyCollectionPopupDirectly = true
                                    }
                                    .padding(vertical = 2.dp)
                                    .testTag("btn_center_weekly_collection"),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .shadow(
                                            elevation = if (isSelected) 8.dp else 4.dp,
                                            shape = CircleShape
                                        )
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                colors = if (isSelected) {
                                                    listOf(Color(0xFF07562E), Color(0xFF0A6C3C))
                                                } else {
                                                    listOf(Color(0xFF0A6C3C), Color(0xFF18A960))
                                                }
                                            )
                                        )
                                        .border(1.5.dp, Color.White.copy(alpha = 0.9f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Weekly Collection",
                                        tint = Color.White,
                                        modifier = Modifier.size(23.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = tab.title,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 10.5.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF0A6C3C),
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        } else {
                            val pillColor by animateColorAsState(
                                targetValue = if (isSelected) Color(0xFFD3F5E2) else Color.Transparent,
                                animationSpec = tween(durationMillis = 200),
                                label = "nav_pill"
                            )
                            val itemColor by animateColorAsState(
                                targetValue = if (isSelected) Color(0xFF0A6C3C) else Color(0xFF475569),
                                animationSpec = tween(durationMillis = 200),
                                label = "nav_tint"
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 56.dp)
                                    .minimumInteractiveComponentSize()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { selectedTab = index }
                                    .padding(vertical = 4.dp)
                                    .testTag(tab.testTag),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(pillColor)
                                        .then(
                                            if (isSelected) {
                                                Modifier.border(
                                                    1.dp,
                                                    Color(0xFF0D683B).copy(alpha = 0.22f),
                                                    RoundedCornerShape(12.dp)
                                                )
                                            } else {
                                                Modifier
                                            }
                                        )
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.title,
                                        tint = itemColor,
                                        modifier = Modifier.size(21.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = tab.title,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    fontSize = 10.5.sp,
                                    color = itemColor,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
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
                    }
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
                    }
                )

                5 -> SettingsScreen(
                    currentUser = currentUser,
                    viewModel = samitiViewModel,
                    onLogout = onLogout,
                    onOpenLoansTab = {
                        selectedTab = 3
                        openLoansSubTabDirectly = true
                    },
                    onShowMessage = { msg ->
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(msg)
                        }
                    }
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

    // Auto-detected GitHub Release App Update Modal
    pendingAppUpdate?.let { release ->
        AppUpdateAvailableDialog(
            release = release,
            onDismiss = {
                AppUpdatePreferences.setDismissedTag(context, release.tagName)
                pendingAppUpdate = null
            },
            onDownloadClick = { downloadUrl ->
                val opened = GitHubUpdateChecker.openUpdateDownloadUrl(context, downloadUrl)
                pendingAppUpdate = null
                if (!opened) {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("ডাউনলোড লিংক খোলা যায়নি, ব্রাউজার থেকে চেষ্টা করুন")
                    }
                }
            }
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
