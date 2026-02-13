package com.waliahimanshu.wealthmate

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
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
import com.waliahimanshu.wealthmate.investments.InvestmentsScreen
import com.waliahimanshu.wealthmate.navigation.Screen
import com.waliahimanshu.wealthmate.savings.SavingsScreen
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

    WealthMateTheme(isDark = isDarkMode) {
        var currentScreen by remember { mutableStateOf(Screen.DASHBOARD) }
        val householdData by repository.data.collectAsState()
        val syncStatus by repository.syncStatus.collectAsState()
        val isLoading by repository.isLoading.collectAsState()
        val scope = rememberCoroutineScope()

        var selectedMemberId by remember { mutableStateOf<String?>(null) }

        Surface(
            modifier = Modifier.fillMaxSize(),
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
                                onToggleDarkMode = { isDarkMode = it }
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
                                showTopBar = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        // Mobile: Bottom navigation bar
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
                                showTopBar = true,
                                modifier = Modifier.weight(1f)
                            )
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            ) {
                                Screen.entries.forEach { screen ->
                                    NavigationBarItem(
                                        selected = currentScreen == screen,
                                        onClick = { currentScreen = screen },
                                        icon = { Icon(screen.icon, contentDescription = screen.displayName) },
                                        label = { Text(screen.displayName, maxLines = 1, style = MaterialTheme.typography.labelSmall) }
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

@Composable
private fun SidebarNavigation(
    currentScreen: Screen,
    onScreenSelected: (Screen) -> Unit,
    isDarkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit
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

            DarkModeSwitch(isDarkMode = isDarkMode, onToggle = onToggleDarkMode)
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
    showTopBar: Boolean,
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
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "WealthMate",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    DarkModeSwitch(isDarkMode = isDarkMode, onToggle = onToggleDarkMode)
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
                    onUpdateMortgage = { mortgage ->
                        scope.launch { repository.updateData { it.copy(mortgage = mortgage) } }
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

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Savings & Investments", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Track all your accounts and investments", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        // Stat cards
        StatCardRow {
            StatCard(
                title = "Total Wealth",
                value = formatCurrency(totalWealth),
                icon = Icons.Outlined.AccountBalance,
                subtitle = "All accounts combined",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Savings",
                value = formatCurrency(data.totalSavings),
                icon = Icons.Outlined.Star,
                valueColor = Color(0xFF4CAF50),
                subtitle = "${((data.totalSavings / totalWealthSafe) * 100).toInt()}% of total",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Investments",
                value = formatCurrency(data.totalPortfolioValue - totalPensions),
                icon = Icons.Outlined.TrendingUp,
                valueColor = MaterialTheme.colorScheme.primary,
                subtitle = "${(((data.totalPortfolioValue - totalPensions) / totalWealthSafe) * 100).toInt()}% of total",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Pensions",
                value = formatCurrency(totalPensions),
                icon = Icons.Outlined.AccountBalance,
                valueColor = Color(0xFFF44336),
                subtitle = "${((totalPensions / totalWealthSafe) * 100).toInt()}% of total",
                modifier = Modifier.weight(1f)
            )
        }

        // Segmented button row
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            segments.forEachIndexed { index, label ->
                SegmentedButton(
                    selected = selectedSegment == index,
                    onClick = { selectedSegment = index },
                    shape = SegmentedButtonDefaults.itemShape(index, segments.size)
                ) { Text(label) }
            }
        }

        // Content based on selected segment
        when (selectedSegment) {
            0 -> SavingsScreen(
                data = data,
                onUpdateSharedAccounts = { accounts ->
                    scope.launch { repository.updateData { it.copy(sharedAccounts = accounts) } }
                },
                onUpdateMemberSavings = { memberId, savings ->
                    scope.launch {
                        repository.updateData {
                            it.copy(members = it.members.map { m ->
                                if (m.id == memberId) m.copy(savings = savings) else m
                            })
                        }
                    }
                }
            )
            1 -> InvestmentsScreen(
                data = data,
                onUpdateInvestments = { investments ->
                    scope.launch { repository.updateData { it.copy(investments = investments) } }
                },
                onAddCustomCategory = { category ->
                    scope.launch {
                        repository.updateData {
                            it.copy(customInvestmentCategories = it.customInvestmentCategories + category)
                        }
                    }
                }
            )
            2 -> {
                // Pensions - filtered view
                val pensionInvestments = data.investments.filter { it.accountType in pensionTypes }
                InvestmentsScreen(
                    data = data.copy(investments = pensionInvestments),
                    onUpdateInvestments = { investments ->
                        scope.launch {
                            val nonPension = data.investments.filter { it.accountType !in pensionTypes }
                            repository.updateData { it.copy(investments = nonPension + investments) }
                        }
                    },
                    onAddCustomCategory = { category ->
                        scope.launch {
                            repository.updateData {
                                it.copy(customInvestmentCategories = it.customInvestmentCategories + category)
                            }
                        }
                    }
                )
            }
        }
    }
}
