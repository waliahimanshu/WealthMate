package com.waliahimanshu.wealthmate

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.waliahimanshu.wealthmate.components.*
import com.waliahimanshu.wealthmate.dashboard.DashboardScreen
import com.waliahimanshu.wealthmate.expenses.ExpensesScreen
import com.waliahimanshu.wealthmate.goals.GoalsScreen
import com.waliahimanshu.wealthmate.income.IncomeScreen
import com.waliahimanshu.wealthmate.investments.AddInvestmentDialog
import com.waliahimanshu.wealthmate.investments.EditInvestmentDialog
import com.waliahimanshu.wealthmate.investments.InvestmentsScreen
import com.waliahimanshu.wealthmate.investments.investmentsContent
import com.waliahimanshu.wealthmate.navigation.Screen
import com.waliahimanshu.wealthmate.property.PropertyScreen
import com.waliahimanshu.wealthmate.savings.AddSavingsDialog
import com.waliahimanshu.wealthmate.savings.DeleteConfirmationDialog
import com.waliahimanshu.wealthmate.savings.EditSavingsDialog
import com.waliahimanshu.wealthmate.savings.SavingsScreen
import com.waliahimanshu.wealthmate.savings.savingsContent
import com.waliahimanshu.wealthmate.settings.SettingsScreen
import com.waliahimanshu.wealthmate.storage.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun WealthMateApp(
    repository: FinanceRepository,
    currentToken: String?,
    onSaveToken: (String) -> Unit,
    onClearToken: () -> Unit
) {
    var isDarkMode by remember { mutableStateOf(true) }
    var isShowMode by remember { mutableStateOf(false) }

    WealthMateTheme(isDark = isDarkMode) {
        CompositionLocalProvider(LocalShowMode provides isShowMode) {
        var currentScreen by remember { mutableStateOf(Screen.DASHBOARD) }
        val householdData by repository.data.collectAsState()
        val syncStatus by repository.syncStatus.collectAsState()
        val isLoading by repository.isLoading.collectAsState()
        val scope = rememberCoroutineScope()

        var selectedMemberId by remember { mutableStateOf<String?>(null) }

        Surface(
            modifier = Modifier.fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
            color = MaterialTheme.colorScheme.background
        ) {
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val isExpanded = maxWidth > 840.dp
                    val isMedium = maxWidth > 600.dp && maxWidth <= 840.dp
                    val data = householdData ?: HouseholdFinances()

                    if (isExpanded) {
                        // Desktop/Web: Sidebar navigation
                        Row(modifier = Modifier.fillMaxSize()) {
                            SidebarNavigation(
                                currentScreen = currentScreen,
                                onScreenSelected = { currentScreen = it },
                                isDarkMode = isDarkMode,
                                onToggleDarkMode = { isDarkMode = it },
                                isShowMode = isShowMode,
                                onToggleShowMode = { isShowMode = !isShowMode }
                            )
                            ScreenContent(
                                currentScreen = currentScreen,
                                data = data,
                                selectedMemberId = selectedMemberId,
                                onMemberSelected = { selectedMemberId = it },
                                repository = repository,
                                scope = scope,
                                syncStatus = syncStatus,
                                currentToken = currentToken,
                                onSaveToken = onSaveToken,
                                onClearToken = onClearToken,
                                isDarkMode = isDarkMode,
                                onToggleDarkMode = { isDarkMode = it },
                                isShowMode = isShowMode,
                                onToggleShowMode = { isShowMode = !isShowMode },
                                showTopBar = false,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else if (isMedium) {
                        // Tablet: Navigation rail
                        Row(modifier = Modifier.fillMaxSize()) {
                            NavigationRail(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            ) {
                                Spacer(Modifier.height(12.dp))
                                Screen.entries.forEach { screen ->
                                    NavigationRailItem(
                                        selected = currentScreen == screen,
                                        onClick = { currentScreen = screen },
                                        icon = { Icon(screen.icon, contentDescription = screen.displayName) },
                                        label = { Text(screen.displayName, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }
                            ScreenContent(
                                currentScreen = currentScreen,
                                data = data,
                                selectedMemberId = selectedMemberId,
                                onMemberSelected = { selectedMemberId = it },
                                repository = repository,
                                scope = scope,
                                syncStatus = syncStatus,
                                currentToken = currentToken,
                                onSaveToken = onSaveToken,
                                onClearToken = onClearToken,
                                isDarkMode = isDarkMode,
                                onToggleDarkMode = { isDarkMode = it },
                                isShowMode = isShowMode,
                                onToggleShowMode = { isShowMode = !isShowMode },
                                showTopBar = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        // Mobile: Drawer for Goals/Settings + Bottom bar for 4 main screens
                        val drawerState = rememberDrawerState(DrawerValue.Closed)
                        val drawerScope = rememberCoroutineScope()

                        ModalNavigationDrawer(
                            drawerState = drawerState,
                            gesturesEnabled = drawerState.isOpen,
                            drawerContent = {
                                ModalDrawerSheet(modifier = Modifier.width(280.dp)) {
                                    Spacer(Modifier.height(24.dp))
                                    Text(
                                        "WealthMate",
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.height(16.dp))
                                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                                    Spacer(Modifier.height(8.dp))

                                    Screen.mobileDrawerScreens.forEach { screen ->
                                        NavigationDrawerItem(
                                            label = { Text(screen.displayName) },
                                            icon = { Icon(screen.icon, contentDescription = null) },
                                            selected = currentScreen == screen,
                                            onClick = {
                                                currentScreen = screen
                                                drawerScope.launch { drawerState.close() }
                                            },
                                            modifier = Modifier.padding(horizontal = 12.dp)
                                        )
                                    }

                                    Spacer(Modifier.weight(1f))

                                    DarkModeSwitch(
                                        isDarkMode = isDarkMode,
                                        onToggle = { isDarkMode = it }
                                    )
                                    Spacer(Modifier.height(16.dp))
                                }
                            }
                        ) {
                            val isDrawerScreen = currentScreen in Screen.mobileDrawerScreens
                            Column(modifier = Modifier.fillMaxSize()) {
                                ScreenContent(
                                    currentScreen = currentScreen,
                                    data = data,
                                    selectedMemberId = selectedMemberId,
                                    onMemberSelected = { selectedMemberId = it },
                                    repository = repository,
                                    scope = scope,
                                    syncStatus = syncStatus,
                                    currentToken = currentToken,
                                    onSaveToken = onSaveToken,
                                    onClearToken = onClearToken,
                                    isDarkMode = isDarkMode,
                                    onToggleDarkMode = { isDarkMode = it },
                                    isShowMode = isShowMode,
                                    onToggleShowMode = { isShowMode = !isShowMode },
                                    showTopBar = true,
                                    onMenuClick = if (isDrawerScreen) null else {{ drawerScope.launch { drawerState.open() } }},
                                    onBackClick = if (isDrawerScreen) {{ currentScreen = Screen.DASHBOARD }} else null,
                                    modifier = Modifier.weight(1f)
                                )
                                if (!isDrawerScreen) {
                                    NavigationBar(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                                    ) {
                                        Screen.mobileBottomBarScreens.forEach { screen ->
                                            NavigationBarItem(
                                                selected = currentScreen == screen,
                                                onClick = { currentScreen = screen },
                                                icon = { Icon(screen.icon, contentDescription = screen.mobileDisplayName) },
                                                label = { Text(screen.mobileDisplayName, maxLines = 1, style = MaterialTheme.typography.labelSmall) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun SidebarNavigation(
    currentScreen: Screen,
    onScreenSelected: (Screen) -> Unit,
    isDarkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    isShowMode: Boolean,
    onToggleShowMode: () -> Unit
) {
    Surface(
        modifier = Modifier.width(240.dp).fillMaxHeight(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.fillMaxHeight().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                "Wealth Manager",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "Couple Finance Tracker",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))

            Screen.entries.forEach { screen ->
                NavigationDrawerItem(
                    label = { Text(screen.displayName) },
                    icon = { Icon(screen.icon, contentDescription = null) },
                    selected = currentScreen == screen,
                    onClick = { onScreenSelected(screen) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.weight(1f))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onToggleShowMode) {
                    Icon(
                        if (isShowMode) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (isShowMode) "Show real values" else "Hide values"
                    )
                }
                DarkModeSwitch(isDarkMode = isDarkMode, onToggle = onToggleDarkMode)
            }
        }
    }
}

@Composable
private fun ScreenContent(
    currentScreen: Screen,
    data: HouseholdFinances,
    selectedMemberId: String?,
    onMemberSelected: (String?) -> Unit,
    repository: FinanceRepository,
    scope: CoroutineScope,
    syncStatus: SyncStatus,
    currentToken: String?,
    onSaveToken: (String) -> Unit,
    onClearToken: () -> Unit,
    isDarkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    isShowMode: Boolean,
    onToggleShowMode: () -> Unit,
    showTopBar: Boolean,
    onMenuClick: (() -> Unit)? = null,
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxHeight()) {
        SyncStatusBar(syncStatus)

        if (showTopBar) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = if (onMenuClick != null || onBackClick != null) 4.dp else 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (onBackClick != null) {
                            IconButton(onClick = onBackClick) {
                                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                            }
                        } else if (onMenuClick != null) {
                            IconButton(onClick = onMenuClick) {
                                Icon(Icons.Outlined.Menu, contentDescription = "Menu")
                            }
                        }
                        Text(
                            "WealthMate",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(onClick = onToggleShowMode) {
                            Icon(
                                if (isShowMode) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = if (isShowMode) "Show real values" else "Hide values"
                            )
                        }
                        DarkModeSwitch(isDarkMode = isDarkMode, onToggle = onToggleDarkMode)
                    }
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            when (currentScreen) {
                Screen.DASHBOARD -> DashboardScreen(
                    data = data,
                    selectedMemberId = selectedMemberId,
                    onMemberSelected = { onMemberSelected(it) }
                )
                Screen.INCOME -> IncomeScreen(data = data)
                Screen.EXPENSES -> ExpensesScreen(
                    data = data,
                    onUpdateSharedOutgoings = { outgoings ->
                        scope.launch { repository.updateData { it.copy(sharedOutgoings = outgoings) } }
                    },
                    onUpdateMemberOutgoings = { memberId, outgoings ->
                        scope.launch {
                            repository.updateData {
                                it.copy(members = it.members.map { m ->
                                    if (m.id == memberId) m.copy(outgoings = outgoings) else m
                                })
                            }
                        }
                    },
                    onAddCustomCategory = { category ->
                        scope.launch {
                            repository.updateData {
                                it.copy(customOutgoingCategories = it.customOutgoingCategories + category)
                            }
                        }
                    }
                )
                Screen.SAVINGS_INVESTMENTS -> SavingsAndInvestmentsScreen(
                    data = data,
                    repository = repository,
                    scope = scope
                )
                Screen.PROPERTY -> PropertyScreen(
                    data = data,
                    onUpdateMortgage = { mortgage ->
                        scope.launch { repository.updateData { it.copy(mortgage = mortgage) } }
                    }
                )
                Screen.GOALS -> GoalsScreen(
                    data = data,
                    onUpdateGoals = { goals ->
                        scope.launch { repository.updateData { it.copy(sharedGoals = goals) } }
                    },
                    onAddCustomCategory = { category ->
                        scope.launch {
                            repository.updateData {
                                it.copy(customGoalCategories = it.customGoalCategories + category)
                            }
                        }
                    }
                )
                Screen.SETTINGS -> SettingsScreen(
                    data = data,
                    syncStatus = syncStatus,
                    currentToken = currentToken,
                    onSaveToken = onSaveToken,
                    onClearToken = onClearToken,
                    onSync = { scope.launch { repository.syncWithCloud() } },
                    onForceRefresh = { scope.launch { repository.forceRefreshFromCloud() } },
                    onAddMember = { member ->
                        scope.launch { repository.updateData { it.copy(members = it.members + member) } }
                    },
                    onUpdateMember = { member ->
                        scope.launch {
                            repository.updateData {
                                it.copy(members = it.members.map { m ->
                                    if (m.id == member.id) member else m
                                })
                            }
                        }
                    },
                    onDeleteMember = { memberId ->
                        scope.launch {
                            repository.updateData {
                                it.copy(members = it.members.filter { m -> m.id != memberId })
                            }
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsAndInvestmentsScreen(
    data: HouseholdFinances,
    repository: FinanceRepository,
    scope: CoroutineScope
) {
    var selectedSegment by remember { mutableStateOf(0) }
    val segments = listOf("Savings", "Investments", "Pensions")

    val pensionTypes = listOf(UKAccountType.PENSION, UKAccountType.WORKPLACE_PENSION, UKAccountType.SIPP)
    val totalPensions = data.investments.filter { it.accountType in pensionTypes }.sumOf { it.currentValue }
    val totalWealth = data.totalSavings + data.totalPortfolioValue
    val totalWealthSafe = totalWealth.coerceAtLeast(1.0)

    // Savings state
    var savingsSelectedTab by remember { mutableStateOf(0) }
    var savingsShowAddDialog by remember { mutableStateOf(false) }
    var savingsAddingForMemberId by remember { mutableStateOf<String?>(null) }
    var savingsEditingAccount by remember { mutableStateOf<SavingsAccount?>(null) }
    var savingsEditingForMemberId by remember { mutableStateOf<String?>(null) }
    var savingsDeletingAccount by remember { mutableStateOf<SavingsAccount?>(null) }

    val savingsTabs = listOf("Overview", "Joint") + data.members.map { it.name }
    val savingsCurrentAccounts = when (savingsSelectedTab) {
        0 -> data.allSavings
        1 -> data.sharedAccounts
        else -> data.members.getOrNull(savingsSelectedTab - 2)?.savings ?: emptyList()
    }
    val savingsCurrentMemberId = when (savingsSelectedTab) {
        0 -> null
        1 -> null
        else -> data.members.getOrNull(savingsSelectedTab - 2)?.id
    }

    // Investments state
    var investmentsSelectedTab by remember { mutableStateOf(0) }
    var investmentsShowAddDialog by remember { mutableStateOf(false) }
    var investmentsEditingInvestment by remember { mutableStateOf<Investment?>(null) }
    var investmentsDeletingInvestment by remember { mutableStateOf<Investment?>(null) }

    val investmentsData = if (selectedSegment == 2) {
        data.copy(investments = data.investments.filter { it.accountType in pensionTypes })
    } else data

    val investmentsTabs = listOf("Overview") + investmentsData.members.map { it.name } + listOf("Kids")
    val investmentsCurrentMemberId = when {
        investmentsSelectedTab == 0 -> null
        investmentsSelectedTab == investmentsTabs.size - 1 -> null
        else -> investmentsData.members.getOrNull(investmentsSelectedTab - 1)?.id
    }
    val investmentsIsKidsTab = investmentsSelectedTab == investmentsTabs.size - 1
    val investmentsFiltered = when {
        investmentsSelectedTab == 0 -> investmentsData.investments
        investmentsSelectedTab == investmentsTabs.size - 1 -> investmentsData.kidsInvestments
        else -> {
            val memberId = investmentsData.members.getOrNull(investmentsSelectedTab - 1)?.id
            investmentsData.investments.filter { it.ownerId == memberId && !it.isForKids }
        }
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text("Savings & Investments", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Track all your accounts and investments", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Stat cards
        item {
            StatCardRow {
                StatCard(
                    title = "Total Wealth",
                    value = displayCurrency(totalWealth),
                    icon = Icons.Outlined.AccountBalance,
                    subtitle = "All accounts combined",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Savings",
                    value = displayCurrency(data.totalSavings),
                    icon = Icons.Outlined.Star,
                    valueColor = Color(0xFF4CAF50),
                    subtitle = "${((data.totalSavings / totalWealthSafe) * 100).toInt()}% of total",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Investments",
                    value = displayCurrency(data.totalPortfolioValue - totalPensions),
                    icon = Icons.Outlined.TrendingUp,
                    valueColor = MaterialTheme.colorScheme.primary,
                    subtitle = "${(((data.totalPortfolioValue - totalPensions) / totalWealthSafe) * 100).toInt()}% of total",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Pensions",
                    value = displayCurrency(totalPensions),
                    icon = Icons.Outlined.AccountBalance,
                    valueColor = Color(0xFFF44336),
                    subtitle = "${((totalPensions / totalWealthSafe) * 100).toInt()}% of total",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Segmented button row
        item {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                segments.forEachIndexed { index, label ->
                    SegmentedButton(
                        selected = selectedSegment == index,
                        onClick = { selectedSegment = index },
                        shape = SegmentedButtonDefaults.itemShape(index, segments.size)
                    ) { Text(label) }
                }
            }
        }

        // Inline sub-screen content based on selected segment
        when (selectedSegment) {
            0 -> savingsContent(
                data = data,
                selectedTab = savingsSelectedTab,
                onTabSelected = { savingsSelectedTab = it },
                tabs = savingsTabs,
                currentAccounts = savingsCurrentAccounts,
                currentMemberId = savingsCurrentMemberId,
                onAddClick = {
                    savingsAddingForMemberId = savingsCurrentMemberId
                    savingsShowAddDialog = true
                },
                onEditClick = { account ->
                    savingsEditingAccount = account
                    savingsEditingForMemberId = account.ownerId
                },
                onDeleteClick = { account -> savingsDeletingAccount = account }
            )
            else -> investmentsContent(
                data = investmentsData,
                selectedTab = investmentsSelectedTab,
                onTabSelected = { investmentsSelectedTab = it },
                tabs = investmentsTabs,
                filteredInvestments = investmentsFiltered,
                isKidsTab = investmentsIsKidsTab,
                onAddClick = { investmentsShowAddDialog = true },
                onEditClick = { investmentsEditingInvestment = it },
                onDeleteClick = { investmentsDeletingInvestment = it }
            )
        }
    }

    // Savings dialogs
    if (savingsShowAddDialog) {
        AddSavingsDialog(
            onDismiss = { savingsShowAddDialog = false },
            onAdd = { account ->
                if (savingsAddingForMemberId == null) {
                    scope.launch { repository.updateData { it.copy(sharedAccounts = it.sharedAccounts + account) } }
                } else {
                    scope.launch {
                        repository.updateData {
                            it.copy(members = it.members.map { m ->
                                if (m.id == savingsAddingForMemberId) m.copy(savings = m.savings + account) else m
                            })
                        }
                    }
                }
                savingsShowAddDialog = false
            }
        )
    }

    savingsEditingAccount?.let { account ->
        EditSavingsDialog(
            account = account,
            onDismiss = { savingsEditingAccount = null },
            onSave = { updated ->
                if (savingsEditingForMemberId == null) {
                    scope.launch { repository.updateData { it.copy(sharedAccounts = it.sharedAccounts.map { a -> if (a.id == updated.id) updated else a }) } }
                } else {
                    scope.launch {
                        repository.updateData {
                            it.copy(members = it.members.map { m ->
                                if (m.id == savingsEditingForMemberId) m.copy(savings = m.savings.map { a -> if (a.id == updated.id) updated else a }) else m
                            })
                        }
                    }
                }
                savingsEditingAccount = null
            }
        )
    }

    savingsDeletingAccount?.let { account ->
        DeleteConfirmationDialog(
            itemName = account.name,
            onDismiss = { savingsDeletingAccount = null },
            onConfirm = {
                val ownerId = account.ownerId
                if (ownerId == null) {
                    scope.launch { repository.updateData { it.copy(sharedAccounts = it.sharedAccounts.filter { a -> a.id != account.id }) } }
                } else {
                    scope.launch {
                        repository.updateData {
                            it.copy(members = it.members.map { m ->
                                if (m.id == ownerId) m.copy(savings = m.savings.filter { a -> a.id != account.id }) else m
                            })
                        }
                    }
                }
                savingsDeletingAccount = null
            }
        )
    }

    // Investments dialogs
    if (investmentsShowAddDialog) {
        AddInvestmentDialog(
            members = investmentsData.members,
            preselectedMemberId = investmentsCurrentMemberId,
            preselectedIsForKids = investmentsIsKidsTab,
            onDismiss = { investmentsShowAddDialog = false },
            onAdd = { investment ->
                scope.launch { repository.updateData { it.copy(investments = it.investments + investment) } }
                investmentsShowAddDialog = false
            }
        )
    }

    investmentsEditingInvestment?.let { investment ->
        EditInvestmentDialog(
            investment = investment,
            members = investmentsData.members,
            onDismiss = { investmentsEditingInvestment = null },
            onSave = { updated ->
                scope.launch { repository.updateData { it.copy(investments = it.investments.map { i -> if (i.id == updated.id) updated else i }) } }
                investmentsEditingInvestment = null
            }
        )
    }

    investmentsDeletingInvestment?.let { investment ->
        DeleteConfirmationDialog(
            itemName = investment.name,
            onDismiss = { investmentsDeletingInvestment = null },
            onConfirm = {
                if (selectedSegment == 2) {
                    // Pensions - only remove this one, keep non-pensions
                    scope.launch { repository.updateData { it.copy(investments = it.investments.filter { i -> i.id != investment.id }) } }
                } else {
                    scope.launch { repository.updateData { it.copy(investments = it.investments.filter { i -> i.id != investment.id }) } }
                }
                investmentsDeletingInvestment = null
            }
        )
    }
}
