package com.waliahimanshu.wealthmate

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.waliahimanshu.wealthmate.components.*
import com.waliahimanshu.wealthmate.storage.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class Screen {
    DASHBOARD,
    MEMBERS,
    OUTGOINGS,
    SAVINGS,
    INVESTMENTS,
    GOALS,
    SETTINGS
}

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

        // Selected member for detailed view (null = household view)
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
                Column(modifier = Modifier.fillMaxSize()) {
                    // Sync status indicator
                    SyncStatusBar(syncStatus)

                    // Top bar with dark mode toggle
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
                            DarkModeSwitch(
                                isDarkMode = isDarkMode,
                                onToggle = { isDarkMode = it }
                            )
                        }
                    }

                    // Navigation
                    ScrollableTabRow(
                        selectedTabIndex = Screen.entries.indexOf(currentScreen),
                        containerColor = MaterialTheme.colorScheme.surface,
                        edgePadding = 8.dp
                    ) {
                        Screen.entries.forEach { screen ->
                            Tab(
                                selected = currentScreen == screen,
                                onClick = { currentScreen = screen },
                                text = { Text(screen.name.replace("_", " "), maxLines = 1) }
                            )
                        }
                    }

                    // Content
                    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        val data = householdData ?: HouseholdFinances()

                        when (currentScreen) {
                            Screen.DASHBOARD -> DashboardScreen(
                                data = data,
                                selectedMemberId = selectedMemberId,
                                onMemberSelected = { selectedMemberId = it }
                            )
                            Screen.MEMBERS -> MembersScreen(
                                data = data,
                                onAddMember = { member ->
                                    scope.launch {
                                        repository.updateData { it.copy(members = it.members + member) }
                                    }
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
                            Screen.OUTGOINGS -> OutgoingsScreen(
                                data = data,
                                onUpdateSharedOutgoings = { outgoings ->
                                    scope.launch {
                                        repository.updateData { it.copy(sharedOutgoings = outgoings) }
                                    }
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
                                    scope.launch {
                                        repository.updateData { it.copy(mortgage = mortgage) }
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
                            Screen.SAVINGS -> SavingsScreen(
                                data = data,
                                onUpdateSharedAccounts = { accounts ->
                                    scope.launch {
                                        repository.updateData { it.copy(sharedAccounts = accounts) }
                                    }
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
                            Screen.INVESTMENTS -> InvestmentsScreen(
                                data = data,
                                onUpdateInvestments = { investments ->
                                    scope.launch {
                                        repository.updateData { it.copy(investments = investments) }
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
                            Screen.GOALS -> GoalsScreen(
                                data = data,
                                onUpdateGoals = { goals ->
                                    scope.launch {
                                        repository.updateData { it.copy(sharedGoals = goals) }
                                    }
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
                                syncStatus = syncStatus,
                                currentToken = currentToken,
                                onSaveToken = onSaveToken,
                                onClearToken = onClearToken,
                                onSync = { scope.launch { repository.syncWithCloud() } },
                                onForceRefresh = { scope.launch { repository.forceRefreshFromCloud() } }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SyncStatusBar(status: SyncStatus) {
    val (color, text) = when (status) {
        is SyncStatus.Idle -> MaterialTheme.colorScheme.outline to ""
        is SyncStatus.Syncing -> MaterialTheme.colorScheme.tertiary to "Syncing..."
        is SyncStatus.NotConfigured -> Color(0xFFFF9800) to "Cloud sync not configured"
        is SyncStatus.Success -> MaterialTheme.colorScheme.primary to status.message
        is SyncStatus.Error -> MaterialTheme.colorScheme.error to status.message
    }

    if (text.isNotEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(color.copy(alpha = 0.15f))
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text, style = MaterialTheme.typography.bodySmall, color = color)
        }
    }
}

@Composable
fun DarkModeSwitch(
    isDarkMode: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = if (isDarkMode) "\u263D" else "\u2600\uFE0F",
            style = MaterialTheme.typography.titleMedium
        )
        Switch(
            checked = isDarkMode,
            onCheckedChange = onToggle,
            thumbContent = {
                Box(
                    modifier = Modifier.size(SwitchDefaults.IconSize),
                    contentAlignment = Alignment.Center
                ) {
                    val rotation by animateFloatAsState(
                        targetValue = if (isDarkMode) 360f else 0f,
                        animationSpec = tween(400)
                    )
                    Text(
                        text = if (isDarkMode) "\u263D" else "\u2600\uFE0F",
                        modifier = Modifier.graphicsLayer { rotationZ = rotation }
                    )
                }
            }
        )
    }
}

@Composable
fun DashboardScreen(
    data: HouseholdFinances,
    selectedMemberId: String?,
    onMemberSelected: (String?) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Header with member selector
        item {
            // Member chips for filtering
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilterChip(
                    selected = selectedMemberId == null,
                    onClick = { onMemberSelected(null) },
                    label = { Text("Household") }
                )
                data.members.forEach { member ->
                    FilterChip(
                        selected = selectedMemberId == member.id,
                        onClick = { onMemberSelected(member.id) },
                        label = { Text(member.name) }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // Show household or member stats
        if (selectedMemberId == null) {
            // ============================================
            // SECTION 1: INCOME vs OUTGOINGS
            // ============================================
            item {
                Text("Income & Expenses", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            item {
                IncomeOutgoingsOverviewCard(data)
            }

            // Outgoings breakdown chart
            val allOutgoings = data.sharedOutgoings + data.members.flatMap { it.outgoings }
            if (allOutgoings.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(4.dp))
                    OutgoingsSummaryCard(data)
                }
            }

            // ============================================
            // SECTION 2: SAVINGS
            // ============================================
            item {
                Spacer(Modifier.height(12.dp))
                Text("Savings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            item {
                SavingsOverviewCard(data)
            }

            // Savings breakdown chart by account type
            if (data.allSavings.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(4.dp))
                    SavingsSummaryCard(data)
                }
            }

            // ============================================
            // SECTION 3: INVESTMENTS
            // ============================================
            if (data.investments.isNotEmpty() || data.totalMonthlyInvestments > 0) {
                item {
                    Spacer(Modifier.height(12.dp))
                    Text("Investments", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                item {
                    InvestmentsSummaryCard(data)
                }
            }

            // ============================================
            // SECTION 4: PROJECTED GROWTH
            // ============================================
            if (data.investments.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(12.dp))
                    ProjectedGrowthCard(
                        currentValue = data.totalPortfolioValue,
                        monthlyContribution = data.totalMonthlyInvestments
                    )
                }
            }

            // ============================================
            // SECTION 5: GOALS
            // ============================================
            if (data.sharedGoals.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(12.dp))
                    Text("Shared Goals", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                items(data.sharedGoals.take(3)) { goal ->
                    GoalProgressCard(goal)
                }
            }

            // ============================================
            // SECTION 6: MEMBERS
            // ============================================
            if (data.members.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(12.dp))
                    Text("Members", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                items(data.members) { member ->
                    MemberSummaryCard(member, onClick = { onMemberSelected(member.id) })
                }
            }
        } else {
            // Individual member view
            val member = data.members.find { it.id == selectedMemberId }
            if (member != null) {
                item {
                    Text(member.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                item {
                    SummaryCard("Salary", formatCurrency(member.salary), Color(0xFF4CAF50))
                }
                item {
                    SummaryCard("Personal Outgoings", formatCurrency(member.totalOutgoings), Color(0xFFF44336))
                }
                item {
                    SummaryCard("Net Monthly", formatCurrency(member.netMonthly),
                        if (member.netMonthly >= 0) Color(0xFF2196F3) else Color(0xFFF44336))
                }
                item {
                    SummaryCard("Personal Savings", formatCurrency(member.totalSavings), Color(0xFF9C27B0))
                }
            }
        }
    }
}

@Composable
fun SummaryCard(title: String, value: String, color: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun GoalProgressCard(goal: SharedGoal) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(goal.name, fontWeight = FontWeight.Bold)
                Text("${goal.progressPercent.roundToInt()}%", color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (goal.progressPercent / 100).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${formatCurrency(goal.currentAmount)} / ${formatCurrency(goal.targetAmount)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun InvestmentsSummaryCard(data: HouseholdFinances) {
    val totalValue = data.totalPortfolioValue
    val totalContributed = data.totalInvested
    val gainLoss = totalValue - totalContributed
    val gainPercent = if (totalContributed > 0) (gainLoss / totalContributed * 100) else 0.0

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Portfolio Value", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatCurrency(totalValue), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }

                // Mini pie chart for asset allocation
                if (data.investments.isNotEmpty()) {
                    val assetSlices = data.investments
                        .groupBy { it.assetClass }
                        .map { (assetClass, investments) ->
                            PieSlice(
                                label = assetClass.name,
                                value = investments.sumOf { it.currentValue },
                                color = ChartColors.getColor(AssetClass.entries.indexOf(assetClass))
                            )
                        }
                        .filter { it.value > 0 }

                    CompactPieChart(slices = assetSlices)
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Monthly Contributions", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${formatCurrency(data.totalMonthlyInvestments)}/mo", fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Total Gain/Loss", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val gainColor = if (gainLoss >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                    Text(
                        "${if (gainLoss >= 0) "+" else ""}${formatCurrency(gainLoss)} (${if (gainLoss >= 0) "+" else ""}${gainPercent.roundToInt()}%)",
                        fontWeight = FontWeight.Bold,
                        color = gainColor
                    )
                }
            }
        }
    }
}

@Composable
fun OutgoingsSummaryCard(data: HouseholdFinances) {
    // Group outgoings by category for pie chart
    val allOutgoings = data.sharedOutgoings + data.members.flatMap { it.outgoings }
    val categoryTotals = allOutgoings
        .groupBy { it.displayCategory }
        .map { (category, outgoings) ->
            PieSlice(
                label = category,
                value = outgoings.sumOf { it.amount },
                color = ChartColors.getColor(allOutgoings.indexOfFirst { it.displayCategory == category })
            )
        }
        .filter { it.value > 0 }
        .sortedByDescending { it.value }
        .take(6) // Top 6 categories

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Spending Breakdown", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            if (categoryTotals.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CompactPieChart(slices = categoryTotals)
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        categoryTotals.take(4).forEach { slice ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(slice.color)
                                )
                                Text(
                                    "${slice.label}: ${formatCurrency(slice.value)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            } else {
                Text("No outgoings recorded", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun SavingsSummaryCard(data: HouseholdFinances) {
    // Group savings by account type for pie chart
    val allSavings = data.sharedAccounts + data.members.flatMap { it.savings }
    val typeTotals = allSavings
        .groupBy { it.accountType }
        .map { (accountType, accounts) ->
            PieSlice(
                label = accountType.name.replace("_", " "),
                value = accounts.sumOf { it.balance },
                color = ChartColors.getColor(UKAccountType.entries.indexOf(accountType))
            )
        }
        .filter { it.value > 0 }
        .sortedByDescending { it.value }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Savings by Account Type", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            if (typeTotals.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CompactPieChart(slices = typeTotals)
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        typeTotals.take(4).forEach { slice ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(slice.color)
                                )
                                Text(
                                    "${slice.label}: ${formatCurrency(slice.value)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            } else {
                Text("No savings accounts", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun IncomeOutgoingsOverviewCard(data: HouseholdFinances) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Income
                Column {
                    Text("Income", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        formatCurrency(data.totalHouseholdIncome),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                    Text("/month", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                // Outgoings
                Column(horizontalAlignment = Alignment.End) {
                    Text("Outgoings", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        formatCurrency(data.totalOutgoings),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF44336)
                    )
                    Text("/month", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            // Net Monthly
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Net Monthly: ", style = MaterialTheme.typography.titleMedium)
                val netColor = if (data.netMonthlyHousehold >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                Text(
                    "${if (data.netMonthlyHousehold >= 0) "+" else ""}${formatCurrency(data.netMonthlyHousehold)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = netColor
                )
            }
        }
    }
}

@Composable
fun SavingsOverviewCard(data: HouseholdFinances) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Total savings header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Total Savings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    formatCurrency(data.totalSavings),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(12.dp))

            // Easy Access vs Locked
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Easy Access
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Easy Access", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            formatCurrency(data.easyAccessSavings),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                        Text("available now", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(Modifier.width(8.dp))

                // Locked
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFF9800).copy(alpha = 0.1f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Locked", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            formatCurrency(data.lockedSavings),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF9800)
                        )
                        Text("fixed term", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun ProjectedGrowthCard(
    currentValue: Double,
    monthlyContribution: Double,
    years: Int = 5,
    annualReturn: Double = 0.07 // 7% default
) {
    // Calculate projected growth using compound interest + regular contributions
    val projectedValue = calculateProjectedGrowth(currentValue, monthlyContribution, years, annualReturn)
    val totalContributions = currentValue + (monthlyContribution * 12 * years)
    val projectedGain = projectedValue - totalContributions

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Projected Growth ($years years @ ${(annualReturn * 100).roundToInt()}% return)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("If you keep investing", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "${formatCurrency(monthlyContribution)}/mo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Potential value in $years years", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        formatCurrency(projectedValue),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Progress visualization
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Now: ${formatCurrency(currentValue)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "(+${formatCurrency(projectedGain)} potential growth)",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF4CAF50)
                )
            }

            Spacer(Modifier.height(4.dp))
            Text(
                "Based on historical average returns. Actual returns may vary.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

// Helper function for compound interest calculation
fun calculateProjectedGrowth(
    currentValue: Double,
    monthlyContribution: Double,
    years: Int,
    annualReturn: Double
): Double {
    val monthlyRate = annualReturn / 12
    val months = years * 12

    // Future value of current investments (compound annually)
    var futureValueCurrent = currentValue
    for (i in 1..years) {
        futureValueCurrent *= (1 + annualReturn)
    }

    // Future value of monthly contributions (simplified compound calculation)
    var futureValueContributions = 0.0
    for (month in 1..months) {
        // Each contribution grows for remaining months
        val monthsRemaining = months - month
        var contributionGrowth = monthlyContribution
        // Apply monthly compounding for remaining months
        for (m in 1..monthsRemaining) {
            contributionGrowth *= (1 + monthlyRate)
        }
        futureValueContributions += contributionGrowth
    }

    return futureValueCurrent + futureValueContributions
}

@Composable
fun MemberSummaryCard(member: HouseholdMember, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(parseColor(member.color)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    member.name.take(1).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(member.name, fontWeight = FontWeight.Bold)
                Text(
                    "Net: ${formatCurrency(member.netMonthly)}/mo",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(formatCurrency(member.totalSavings), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("savings", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun MembersScreen(
    data: HouseholdFinances,
    onAddMember: (HouseholdMember) -> Unit,
    onUpdateMember: (HouseholdMember) -> Unit,
    onDeleteMember: (String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingMember by remember { mutableStateOf<HouseholdMember?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Household Members", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Button(onClick = { showAddDialog = true }) {
                Text("Add Member")
            }
        }

        if (data.members.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("No members yet", style = MaterialTheme.typography.titleMedium)
                    Text("Add yourself and your partner to get started", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(data.members) { member ->
                MemberCard(
                    member = member,
                    onEdit = { editingMember = member },
                    onDelete = { onDeleteMember(member.id) }
                )
            }
        }
    }

    if (showAddDialog) {
        AddMemberDialog(
            onDismiss = { showAddDialog = false },
            onAdd = {
                onAddMember(it)
                showAddDialog = false
            }
        )
    }

    editingMember?.let { member ->
        EditMemberDialog(
            member = member,
            onDismiss = { editingMember = null },
            onSave = {
                onUpdateMember(it)
                editingMember = null
            }
        )
    }
}

@Composable
fun MemberCard(
    member: HouseholdMember,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(parseColor(member.color)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(member.name.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(member.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("Salary: ${formatCurrency(member.salary)}/mo", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatItem("Outgoings", formatCurrency(member.totalOutgoings), Color(0xFFF44336))
                StatItem("Savings", formatCurrency(member.totalSavings), Color(0xFF4CAF50))
                StatItem("Net", formatCurrency(member.netMonthly), Color(0xFF2196F3))
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun AddMemberDialog(onDismiss: () -> Unit, onAdd: (HouseholdMember) -> Unit) {
    var name by remember { mutableStateOf("") }
    var salary by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("#4CAF50") }

    val colors = listOf("#4CAF50", "#2196F3", "#9C27B0", "#FF9800", "#E91E63", "#00BCD4")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Member") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = salary,
                    onValueChange = { salary = it },
                    label = { Text("Monthly Salary (Net)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    prefix = { Text("£") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Color", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    colors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(parseColor(color))
                                .clickable { selectedColor = color }
                                .then(
                                    if (selectedColor == color) Modifier.background(Color.White.copy(alpha = 0.3f))
                                    else Modifier
                                )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onAdd(HouseholdMember(
                            name = name,
                            salary = salary.toDoubleOrNull() ?: 0.0,
                            color = selectedColor
                        ))
                    }
                }
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun EditMemberDialog(member: HouseholdMember, onDismiss: () -> Unit, onSave: (HouseholdMember) -> Unit) {
    var name by remember { mutableStateOf(member.name) }
    var salary by remember { mutableStateOf(member.salary.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Member") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = salary,
                    onValueChange = { salary = it },
                    label = { Text("Monthly Salary (Net)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    prefix = { Text("£") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(member.copy(name = name, salary = salary.toDoubleOrNull() ?: 0.0))
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutgoingsScreen(
    data: HouseholdFinances,
    onUpdateSharedOutgoings: (List<Outgoing>) -> Unit,
    onUpdateMemberOutgoings: (String, List<Outgoing>) -> Unit,
    onUpdateMortgage: (MortgageInfo?) -> Unit,
    onAddCustomCategory: (String) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var addingForMemberId by remember { mutableStateOf<String?>(null) }
    var editingOutgoing by remember { mutableStateOf<Outgoing?>(null) }
    var editingForMemberId by remember { mutableStateOf<String?>(null) }

    val tabs = listOf("Shared") + data.members.map { it.name }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Monthly Outgoings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 0.dp) {
            tabs.forEachIndexed { index, title ->
                Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title) })
            }
        }

        val currentOutgoings = if (selectedTab == 0) {
            data.sharedOutgoings
        } else {
            data.members.getOrNull(selectedTab - 1)?.outgoings ?: emptyList()
        }
        val currentMemberId = if (selectedTab == 0) null else data.members.getOrNull(selectedTab - 1)?.id

        Text("Total: ${formatCurrency(currentOutgoings.sumOf { it.amount })}", color = Color(0xFFF44336), fontWeight = FontWeight.Bold)

        Button(onClick = {
            addingForMemberId = currentMemberId
            showAddDialog = true
        }) {
            Text("Add Outgoing")
        }

        if (selectedTab == 0) {
            MortgageSection(data.mortgage, onUpdateMortgage)
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(currentOutgoings) { outgoing ->
                OutgoingCard(
                    outgoing = outgoing,
                    onEdit = {
                        editingOutgoing = outgoing
                        editingForMemberId = currentMemberId
                    },
                    onDelete = {
                        if (currentMemberId == null) {
                            onUpdateSharedOutgoings(data.sharedOutgoings.filter { it.id != outgoing.id })
                        } else {
                            onUpdateMemberOutgoings(currentMemberId, currentOutgoings.filter { it.id != outgoing.id })
                        }
                    }
                )
            }
        }
    }

    if (showAddDialog) {
        AddOutgoingDialog(
            customCategories = data.customOutgoingCategories,
            onDismiss = { showAddDialog = false },
            onAdd = { outgoing ->
                if (addingForMemberId == null) {
                    onUpdateSharedOutgoings(data.sharedOutgoings + outgoing)
                } else {
                    val member = data.members.find { it.id == addingForMemberId }
                    if (member != null) {
                        onUpdateMemberOutgoings(addingForMemberId!!, member.outgoings + outgoing)
                    }
                }
                showAddDialog = false
            },
            onAddCustomCategory = onAddCustomCategory
        )
    }

    editingOutgoing?.let { outgoing ->
        EditOutgoingDialog(
            outgoing = outgoing,
            customCategories = data.customOutgoingCategories,
            onDismiss = { editingOutgoing = null },
            onSave = { updated ->
                if (editingForMemberId == null) {
                    onUpdateSharedOutgoings(data.sharedOutgoings.map { if (it.id == updated.id) updated else it })
                } else {
                    val member = data.members.find { it.id == editingForMemberId }
                    if (member != null) {
                        onUpdateMemberOutgoings(editingForMemberId!!, member.outgoings.map { if (it.id == updated.id) updated else it })
                    }
                }
                editingOutgoing = null
            },
            onAddCustomCategory = onAddCustomCategory
        )
    }
}

@Composable
fun MortgageSection(mortgage: MortgageInfo?, onUpdate: (MortgageInfo?) -> Unit) {
    var showMortgageDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Mortgage", fontWeight = FontWeight.Bold)
                TextButton(onClick = { showMortgageDialog = true }) {
                    Text(if (mortgage == null) "Add" else "Edit")
                }
            }
            if (mortgage != null) {
                Text("${mortgage.provider} - ${formatCurrency(mortgage.monthlyPayment)}/mo", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${mortgage.interestRate}% - ${mortgage.termRemainingMonths} months remaining", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    if (showMortgageDialog) {
        MortgageDialog(
            mortgage = mortgage,
            onDismiss = { showMortgageDialog = false },
            onSave = {
                onUpdate(it)
                showMortgageDialog = false
            }
        )
    }
}

@Composable
fun OutgoingCard(outgoing: Outgoing, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(outgoing.name, fontWeight = FontWeight.Bold)
                Text(outgoing.displayCategory, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(formatCurrency(outgoing.amount), fontWeight = FontWeight.Bold, color = Color(0xFFF44336))
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOutgoingDialog(
    customCategories: List<String>,
    onDismiss: () -> Unit,
    onAdd: (Outgoing) -> Unit,
    onAddCustomCategory: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(OutgoingCategory.OTHER) }
    var customCategory by remember { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf(false) }
    var showAddCategoryField by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }

    val displayCategory = customCategory ?: category.name.replace("_", " ")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Outgoing") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), prefix = { Text("£") }, modifier = Modifier.fillMaxWidth())
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(value = displayCategory, onValueChange = {}, readOnly = true, label = { Text("Category") }, modifier = Modifier.fillMaxWidth().menuAnchor())
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        // Built-in categories
                        OutgoingCategory.entries.forEach { cat ->
                            DropdownMenuItem(text = { Text(cat.name.replace("_", " ")) }, onClick = { category = cat; customCategory = null; expanded = false })
                        }
                        // Custom categories
                        if (customCategories.isNotEmpty()) {
                            HorizontalDivider()
                            Text("Custom", modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        customCategories.forEach { custom ->
                            DropdownMenuItem(text = { Text(custom) }, onClick = { customCategory = custom; expanded = false })
                        }
                        HorizontalDivider()
                        DropdownMenuItem(text = { Text("+ Add Custom Category", color = MaterialTheme.colorScheme.primary) }, onClick = { showAddCategoryField = true; expanded = false })
                    }
                }
                if (showAddCategoryField) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newCategoryName,
                            onValueChange = { newCategoryName = it },
                            label = { Text("New Category") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Button(onClick = {
                            if (newCategoryName.isNotBlank()) {
                                onAddCustomCategory(newCategoryName)
                                customCategory = newCategoryName
                                newCategoryName = ""
                                showAddCategoryField = false
                            }
                        }) { Text("Add") }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isNotBlank() && (amount.toDoubleOrNull() ?: 0.0) > 0) {
                    onAdd(Outgoing(name = name, amount = amount.toDoubleOrNull() ?: 0.0, category = category, customCategory = customCategory))
                }
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditOutgoingDialog(
    outgoing: Outgoing,
    customCategories: List<String>,
    onDismiss: () -> Unit,
    onSave: (Outgoing) -> Unit,
    onAddCustomCategory: (String) -> Unit
) {
    var name by remember { mutableStateOf(outgoing.name) }
    var amount by remember { mutableStateOf(outgoing.amount.toString()) }
    var category by remember { mutableStateOf(outgoing.category) }
    var customCategory by remember { mutableStateOf(outgoing.customCategory) }
    var expanded by remember { mutableStateOf(false) }
    var showAddCategoryField by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }

    val displayCategory = customCategory ?: category.name.replace("_", " ")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Outgoing") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), prefix = { Text("£") }, modifier = Modifier.fillMaxWidth())
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(value = displayCategory, onValueChange = {}, readOnly = true, label = { Text("Category") }, modifier = Modifier.fillMaxWidth().menuAnchor())
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        OutgoingCategory.entries.forEach { cat ->
                            DropdownMenuItem(text = { Text(cat.name.replace("_", " ")) }, onClick = { category = cat; customCategory = null; expanded = false })
                        }
                        if (customCategories.isNotEmpty()) {
                            HorizontalDivider()
                            Text("Custom", modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        customCategories.forEach { custom ->
                            DropdownMenuItem(text = { Text(custom) }, onClick = { customCategory = custom; expanded = false })
                        }
                        HorizontalDivider()
                        DropdownMenuItem(text = { Text("+ Add Custom Category", color = MaterialTheme.colorScheme.primary) }, onClick = { showAddCategoryField = true; expanded = false })
                    }
                }
                if (showAddCategoryField) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newCategoryName,
                            onValueChange = { newCategoryName = it },
                            label = { Text("New Category") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Button(onClick = {
                            if (newCategoryName.isNotBlank()) {
                                onAddCustomCategory(newCategoryName)
                                customCategory = newCategoryName
                                newCategoryName = ""
                                showAddCategoryField = false
                            }
                        }) { Text("Add") }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isNotBlank()) {
                    onSave(outgoing.copy(name = name, amount = amount.toDoubleOrNull() ?: 0.0, category = category, customCategory = customCategory))
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun MortgageDialog(mortgage: MortgageInfo?, onDismiss: () -> Unit, onSave: (MortgageInfo?) -> Unit) {
    var provider by remember { mutableStateOf(mortgage?.provider ?: "") }
    var balance by remember { mutableStateOf(mortgage?.remainingBalance?.toString() ?: "") }
    var payment by remember { mutableStateOf(mortgage?.monthlyPayment?.toString() ?: "") }
    var rate by remember { mutableStateOf(mortgage?.interestRate?.toString() ?: "") }
    var term by remember { mutableStateOf(mortgage?.termRemainingMonths?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mortgage Details") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = provider, onValueChange = { provider = it }, label = { Text("Lender") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = balance, onValueChange = { balance = it }, label = { Text("Remaining Balance") }, prefix = { Text("£") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = payment, onValueChange = { payment = it }, label = { Text("Monthly Payment") }, prefix = { Text("£") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = rate, onValueChange = { rate = it }, label = { Text("Interest Rate") }, suffix = { Text("%") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = term, onValueChange = { term = it }, label = { Text("Remaining Term (months)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = {
                if (provider.isNotBlank()) {
                    onSave(MortgageInfo(provider = provider, remainingBalance = balance.toDoubleOrNull() ?: 0.0, monthlyPayment = payment.toDoubleOrNull() ?: 0.0, interestRate = rate.toDoubleOrNull() ?: 0.0, termRemainingMonths = term.toIntOrNull() ?: 0))
                }
            }) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (mortgage != null) {
                    TextButton(onClick = { onSave(null) }) { Text("Remove", color = Color.Red) }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsScreen(
    data: HouseholdFinances,
    onUpdateSharedAccounts: (List<SavingsAccount>) -> Unit,
    onUpdateMemberSavings: (String, List<SavingsAccount>) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var addingForMemberId by remember { mutableStateOf<String?>(null) }
    var editingAccount by remember { mutableStateOf<SavingsAccount?>(null) }
    var editingForMemberId by remember { mutableStateOf<String?>(null) }

    val tabs = listOf("Joint Accounts") + data.members.map { it.name }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Savings Accounts", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 0.dp) {
            tabs.forEachIndexed { index, title ->
                Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title) })
            }
        }

        val currentAccounts = if (selectedTab == 0) {
            data.sharedAccounts
        } else {
            data.members.getOrNull(selectedTab - 1)?.savings ?: emptyList()
        }
        val currentMemberId = if (selectedTab == 0) null else data.members.getOrNull(selectedTab - 1)?.id

        // Summary card with Easy Access vs Locked
        SavingsOverviewCard(data)

        // Pie chart for current tab's accounts
        if (currentAccounts.isNotEmpty()) {
            val accountTypeSlices = currentAccounts
                .groupBy { it.accountType }
                .map { (accountType, accounts) ->
                    PieSlice(
                        label = accountType.name.replace("_", " "),
                        value = accounts.sumOf { it.balance },
                        color = ChartColors.getColor(UKAccountType.entries.indexOf(accountType))
                    )
                }
                .filter { it.value > 0 }
                .sortedByDescending { it.value }

            PieChartCard(
                title = "Breakdown by Account Type",
                slices = accountTypeSlices
            )
        }

        Button(onClick = {
            addingForMemberId = currentMemberId
            showAddDialog = true
        }) {
            Text("Add Account")
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(currentAccounts) { account ->
                SavingsAccountCard(
                    account = account,
                    onEdit = {
                        editingAccount = account
                        editingForMemberId = currentMemberId
                    },
                    onDelete = {
                        if (currentMemberId == null) {
                            onUpdateSharedAccounts(data.sharedAccounts.filter { it.id != account.id })
                        } else {
                            onUpdateMemberSavings(currentMemberId, currentAccounts.filter { it.id != account.id })
                        }
                    }
                )
            }
        }
    }

    if (showAddDialog) {
        AddSavingsDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { account ->
                if (addingForMemberId == null) {
                    onUpdateSharedAccounts(data.sharedAccounts + account)
                } else {
                    val member = data.members.find { it.id == addingForMemberId }
                    if (member != null) {
                        onUpdateMemberSavings(addingForMemberId!!, member.savings + account)
                    }
                }
                showAddDialog = false
            }
        )
    }

    editingAccount?.let { account ->
        EditSavingsDialog(
            account = account,
            onDismiss = { editingAccount = null },
            onSave = { updated ->
                if (editingForMemberId == null) {
                    onUpdateSharedAccounts(data.sharedAccounts.map { if (it.id == updated.id) updated else it })
                } else {
                    val member = data.members.find { it.id == editingForMemberId }
                    if (member != null) {
                        onUpdateMemberSavings(editingForMemberId!!, member.savings.map { if (it.id == updated.id) updated else it })
                    }
                }
                editingAccount = null
            }
        )
    }
}

@Composable
fun SavingsAccountCard(account: SavingsAccount, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(account.name, fontWeight = FontWeight.Bold)
                Text("${account.provider} - ${account.accountType.name.replace("_", " ")}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${account.interestRate}% AER", style = MaterialTheme.typography.bodySmall, color = Color(0xFF4CAF50))
            }
            Text(formatCurrency(account.balance), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSavingsDialog(onDismiss: () -> Unit, onAdd: (SavingsAccount) -> Unit) {
    var name by remember { mutableStateOf("") }
    var provider by remember { mutableStateOf("") }
    var balance by remember { mutableStateOf("") }
    var interestRate by remember { mutableStateOf("") }
    var accountType by remember { mutableStateOf(UKAccountType.EASY_ACCESS) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Savings Account") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Account Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = provider, onValueChange = { provider = it }, label = { Text("Provider") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = balance, onValueChange = { balance = it }, label = { Text("Balance") }, prefix = { Text("£") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = interestRate, onValueChange = { interestRate = it }, label = { Text("Interest Rate (AER)") }, suffix = { Text("%") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                // Only show cash-based account types for Savings
                val savingsAccountTypes = listOf(
                    UKAccountType.EASY_ACCESS,
                    UKAccountType.REGULAR_SAVER,
                    UKAccountType.NOTICE_ACCOUNT,
                    UKAccountType.FIXED_TERM,
                    UKAccountType.CASH_ISA,
                    UKAccountType.PREMIUM_BONDS,
                    UKAccountType.CURRENT_ACCOUNT
                )
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(value = accountType.name.replace("_", " "), onValueChange = {}, readOnly = true, label = { Text("Account Type") }, modifier = Modifier.fillMaxWidth().menuAnchor())
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        savingsAccountTypes.forEach { type ->
                            DropdownMenuItem(text = { Text(type.name.replace("_", " ")) }, onClick = { accountType = type; expanded = false })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isNotBlank() && provider.isNotBlank()) {
                    onAdd(SavingsAccount(name = name, provider = provider, balance = balance.toDoubleOrNull() ?: 0.0, interestRate = interestRate.toDoubleOrNull() ?: 0.0, accountType = accountType))
                }
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSavingsDialog(account: SavingsAccount, onDismiss: () -> Unit, onSave: (SavingsAccount) -> Unit) {
    var name by remember { mutableStateOf(account.name) }
    var provider by remember { mutableStateOf(account.provider) }
    var balance by remember { mutableStateOf(account.balance.toString()) }
    var interestRate by remember { mutableStateOf(account.interestRate.toString()) }
    var accountType by remember { mutableStateOf(account.accountType) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Savings Account") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Account Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = provider, onValueChange = { provider = it }, label = { Text("Provider") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = balance, onValueChange = { balance = it }, label = { Text("Balance") }, prefix = { Text("£") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = interestRate, onValueChange = { interestRate = it }, label = { Text("Interest Rate (AER)") }, suffix = { Text("%") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                // Only show cash-based account types for Savings
                val savingsAccountTypes = listOf(
                    UKAccountType.EASY_ACCESS,
                    UKAccountType.REGULAR_SAVER,
                    UKAccountType.NOTICE_ACCOUNT,
                    UKAccountType.FIXED_TERM,
                    UKAccountType.CASH_ISA,
                    UKAccountType.PREMIUM_BONDS,
                    UKAccountType.CURRENT_ACCOUNT
                )
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(value = accountType.name.replace("_", " "), onValueChange = {}, readOnly = true, label = { Text("Account Type") }, modifier = Modifier.fillMaxWidth().menuAnchor())
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        savingsAccountTypes.forEach { type ->
                            DropdownMenuItem(text = { Text(type.name.replace("_", " ")) }, onClick = { accountType = type; expanded = false })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isNotBlank() && provider.isNotBlank()) {
                    onSave(account.copy(name = name, provider = provider, balance = balance.toDoubleOrNull() ?: 0.0, interestRate = interestRate.toDoubleOrNull() ?: 0.0, accountType = accountType))
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    data: HouseholdFinances,
    onUpdateGoals: (List<SharedGoal>) -> Unit,
    onAddCustomCategory: (String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var contributingToGoal by remember { mutableStateOf<SharedGoal?>(null) }
    var editingGoal by remember { mutableStateOf<SharedGoal?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Shared Goals", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Button(onClick = { showAddDialog = true }) { Text("Add Goal") }
        }

        if (data.sharedGoals.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No goals yet", style = MaterialTheme.typography.titleMedium)
                    Text("Add a shared goal like a house deposit or holiday", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(data.sharedGoals) { goal ->
                GoalCard(
                    goal = goal,
                    members = data.members,
                    onContribute = { contributingToGoal = goal },
                    onEdit = { editingGoal = goal },
                    onDelete = { onUpdateGoals(data.sharedGoals.filter { it.id != goal.id }) }
                )
            }
        }
    }

    if (showAddDialog) {
        AddGoalDialog(
            customCategories = data.customGoalCategories,
            onDismiss = { showAddDialog = false },
            onAdd = { goal ->
                onUpdateGoals(data.sharedGoals + goal)
                showAddDialog = false
            },
            onAddCustomCategory = onAddCustomCategory
        )
    }

    editingGoal?.let { goal ->
        EditGoalDialog(
            goal = goal,
            customCategories = data.customGoalCategories,
            onDismiss = { editingGoal = null },
            onSave = { updated ->
                onUpdateGoals(data.sharedGoals.map { if (it.id == updated.id) updated else it })
                editingGoal = null
            },
            onAddCustomCategory = onAddCustomCategory
        )
    }

    contributingToGoal?.let { goal ->
        ContributeDialog(
            goal = goal,
            members = data.members,
            onDismiss = { contributingToGoal = null },
            onContribute = { memberId, memberName, amount ->
                val contribution = GoalContribution(memberId = memberId, memberName = memberName, amount = amount)
                val updatedGoal = goal.copy(
                    currentAmount = goal.currentAmount + amount,
                    contributions = goal.contributions + contribution
                )
                onUpdateGoals(data.sharedGoals.map { if (it.id == goal.id) updatedGoal else it })
                contributingToGoal = null
            }
        )
    }
}

@Composable
fun GoalCard(goal: SharedGoal, members: List<HouseholdMember>, onContribute: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(goal.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(goal.displayCategory, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row {
                    TextButton(onClick = onContribute) { Text("Contribute") }
                    IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary) }
                    IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red) }
                }
            }

            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { (goal.progressPercent / 100).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${formatCurrency(goal.currentAmount)} saved", color = MaterialTheme.colorScheme.primary)
                Text("${formatCurrency(goal.remainingAmount)} to go", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("Target: ${formatCurrency(goal.targetAmount)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            if (goal.contributions.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("Recent contributions:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                goal.contributions.takeLast(3).reversed().forEach { contribution ->
                    Text("${contribution.memberName}: ${formatCurrency(contribution.amount)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGoalDialog(
    customCategories: List<String>,
    onDismiss: () -> Unit,
    onAdd: (SharedGoal) -> Unit,
    onAddCustomCategory: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(GoalCategory.OTHER) }
    var customCategory by remember { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf(false) }
    var showAddCategoryField by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }

    val displayCategory = customCategory ?: category.name.replace("_", " ")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Shared Goal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Goal Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = target, onValueChange = { target = it }, label = { Text("Target Amount") }, prefix = { Text("£") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(value = displayCategory, onValueChange = {}, readOnly = true, label = { Text("Category") }, modifier = Modifier.fillMaxWidth().menuAnchor())
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        GoalCategory.entries.forEach { cat ->
                            DropdownMenuItem(text = { Text(cat.name.replace("_", " ")) }, onClick = { category = cat; customCategory = null; expanded = false })
                        }
                        if (customCategories.isNotEmpty()) {
                            HorizontalDivider()
                            Text("Custom", modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        customCategories.forEach { custom ->
                            DropdownMenuItem(text = { Text(custom) }, onClick = { customCategory = custom; expanded = false })
                        }
                        HorizontalDivider()
                        DropdownMenuItem(text = { Text("+ Add Custom Category", color = MaterialTheme.colorScheme.primary) }, onClick = { showAddCategoryField = true; expanded = false })
                    }
                }
                if (showAddCategoryField) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newCategoryName,
                            onValueChange = { newCategoryName = it },
                            label = { Text("New Category") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Button(onClick = {
                            if (newCategoryName.isNotBlank()) {
                                onAddCustomCategory(newCategoryName)
                                customCategory = newCategoryName
                                newCategoryName = ""
                                showAddCategoryField = false
                            }
                        }) { Text("Add") }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isNotBlank() && (target.toDoubleOrNull() ?: 0.0) > 0) {
                    onAdd(SharedGoal(name = name, targetAmount = target.toDoubleOrNull() ?: 0.0, category = category, customCategory = customCategory))
                }
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditGoalDialog(
    goal: SharedGoal,
    customCategories: List<String>,
    onDismiss: () -> Unit,
    onSave: (SharedGoal) -> Unit,
    onAddCustomCategory: (String) -> Unit
) {
    var name by remember { mutableStateOf(goal.name) }
    var target by remember { mutableStateOf(goal.targetAmount.toString()) }
    var currentAmount by remember { mutableStateOf(goal.currentAmount.toString()) }
    var category by remember { mutableStateOf(goal.category) }
    var customCategory by remember { mutableStateOf(goal.customCategory) }
    var expanded by remember { mutableStateOf(false) }
    var showAddCategoryField by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }

    val displayCategory = customCategory ?: category.name.replace("_", " ")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Goal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Goal Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = target, onValueChange = { target = it }, label = { Text("Target Amount") }, prefix = { Text("£") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = currentAmount, onValueChange = { currentAmount = it }, label = { Text("Current Amount Saved") }, prefix = { Text("£") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(value = displayCategory, onValueChange = {}, readOnly = true, label = { Text("Category") }, modifier = Modifier.fillMaxWidth().menuAnchor())
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        GoalCategory.entries.forEach { cat ->
                            DropdownMenuItem(text = { Text(cat.name.replace("_", " ")) }, onClick = { category = cat; customCategory = null; expanded = false })
                        }
                        if (customCategories.isNotEmpty()) {
                            HorizontalDivider()
                            Text("Custom", modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        customCategories.forEach { custom ->
                            DropdownMenuItem(text = { Text(custom) }, onClick = { customCategory = custom; expanded = false })
                        }
                        HorizontalDivider()
                        DropdownMenuItem(text = { Text("+ Add Custom Category", color = MaterialTheme.colorScheme.primary) }, onClick = { showAddCategoryField = true; expanded = false })
                    }
                }
                if (showAddCategoryField) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newCategoryName,
                            onValueChange = { newCategoryName = it },
                            label = { Text("New Category") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Button(onClick = {
                            if (newCategoryName.isNotBlank()) {
                                onAddCustomCategory(newCategoryName)
                                customCategory = newCategoryName
                                newCategoryName = ""
                                showAddCategoryField = false
                            }
                        }) { Text("Add") }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isNotBlank()) {
                    onSave(goal.copy(
                        name = name,
                        targetAmount = target.toDoubleOrNull() ?: goal.targetAmount,
                        currentAmount = currentAmount.toDoubleOrNull() ?: goal.currentAmount,
                        category = category,
                        customCategory = customCategory
                    ))
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContributeDialog(goal: SharedGoal, members: List<HouseholdMember>, onDismiss: () -> Unit, onContribute: (String, String, Double) -> Unit) {
    var amount by remember { mutableStateOf("") }
    var selectedMember by remember { mutableStateOf(members.firstOrNull()) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Contribute to ${goal.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (members.isNotEmpty()) {
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                        OutlinedTextField(
                            value = selectedMember?.name ?: "Select member",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Who is contributing?") },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            members.forEach { member ->
                                DropdownMenuItem(text = { Text(member.name) }, onClick = { selectedMember = member; expanded = false })
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    prefix = { Text("£") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val amountValue = amount.toDoubleOrNull() ?: 0.0
                if (selectedMember != null && amountValue > 0) {
                    onContribute(selectedMember!!.id, selectedMember!!.name, amountValue)
                }
            }) { Text("Contribute") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ============================================
// INVESTMENTS SCREEN
// ============================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestmentsScreen(
    data: HouseholdFinances,
    onUpdateInvestments: (List<Investment>) -> Unit,
    onAddCustomCategory: (String) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingInvestment by remember { mutableStateOf<Investment?>(null) }

    // Tabs: Overview, each member, and Kids
    val memberTabs = data.members.map { it.name }
    val tabs = listOf("Overview") + memberTabs + listOf("Kids")

    // These need to be outside Column so dialogs can access them
    val currentMemberId = when {
        selectedTab == 0 -> null // Overview
        selectedTab == tabs.size - 1 -> null // Kids
        else -> data.members.getOrNull(selectedTab - 1)?.id
    }
    val isKidsTab = selectedTab == tabs.size - 1

    // Filter investments based on selected tab
    val filteredInvestments = when {
        selectedTab == 0 -> data.investments // Overview - all
        selectedTab == tabs.size - 1 -> data.kidsInvestments // Kids tab
        else -> {
            val memberId = data.members.getOrNull(selectedTab - 1)?.id
            data.investments.filter { it.ownerId == memberId && !it.isForKids }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Investments", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 0.dp) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        if (title == "Kids") {
                            Text("Kids")
                        } else {
                            Text(title)
                        }
                    }
                )
            }
        }

        // Summary stats
        val totalValue = filteredInvestments.sumOf { it.currentValue }
        val totalContributed = filteredInvestments.sumOf { it.totalContributed }
        val totalGainLoss = totalValue - totalContributed
        val totalMonthly = filteredInvestments.sumOf { it.monthlyContribution }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Portfolio Value", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatCurrency(totalValue), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Total Gain/Loss", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val gainColor = if (totalGainLoss >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                        val gainPercent = if (totalContributed > 0) (totalGainLoss / totalContributed * 100) else 0.0
                        Text(
                            "${if (totalGainLoss >= 0) "+" else ""}${formatCurrency(totalGainLoss)} (${if (totalGainLoss >= 0) "+" else ""}${gainPercent.roundToInt()}%)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = gainColor
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Monthly Contributions: ${formatCurrency(totalMonthly)}/mo", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Button(onClick = { showAddDialog = true }) {
            Text("Add Investment")
        }

        // Pie chart for Overview tab
        if (selectedTab == 0 && filteredInvestments.isNotEmpty()) {
            // Asset class allocation pie chart
            val assetAllocation = filteredInvestments
                .groupBy { it.assetClass }
                .map { (assetClass, investments) ->
                    PieSlice(
                        label = assetClass.name,
                        value = investments.sumOf { it.currentValue },
                        color = ChartColors.getColor(AssetClass.entries.indexOf(assetClass))
                    )
                }
                .filter { it.value > 0 }

            if (assetAllocation.isNotEmpty()) {
                PieChartCard(
                    title = "Allocation by Asset Class",
                    slices = assetAllocation
                )
            }
        }

        // Investments list
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Recurring investments
            val recurring = filteredInvestments.filter { it.frequency != InvestmentFrequency.ONE_TIME }
            if (recurring.isNotEmpty()) {
                item {
                    Text("Recurring Investments", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                items(recurring) { investment ->
                    InvestmentCard(
                        investment = investment,
                        onEdit = { editingInvestment = investment },
                        onDelete = { onUpdateInvestments(data.investments.filter { it.id != investment.id }) }
                    )
                }
            }

            // One-time investments
            val oneTime = filteredInvestments.filter { it.frequency == InvestmentFrequency.ONE_TIME }
            if (oneTime.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Text("One-time Investments", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                items(oneTime) { investment ->
                    InvestmentCard(
                        investment = investment,
                        onEdit = { editingInvestment = investment },
                        onDelete = { onUpdateInvestments(data.investments.filter { it.id != investment.id }) }
                    )
                }
            }

            // Empty state
            if (filteredInvestments.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                if (isKidsTab) "No kids investments yet" else "No investments yet",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                if (isKidsTab) "Add Junior ISAs or other investments for your children"
                                else "Track your ISAs, Trading 212, and other investments",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddInvestmentDialog(
            members = data.members,
            preselectedMemberId = currentMemberId,
            preselectedIsForKids = isKidsTab,
            onDismiss = { showAddDialog = false },
            onAdd = { investment ->
                onUpdateInvestments(data.investments + investment)
                showAddDialog = false
            }
        )
    }

    editingInvestment?.let { investment ->
        EditInvestmentDialog(
            investment = investment,
            members = data.members,
            onDismiss = { editingInvestment = null },
            onSave = { updated ->
                onUpdateInvestments(data.investments.map { if (it.id == updated.id) updated else it })
                editingInvestment = null
            }
        )
    }
}

@Composable
fun InvestmentCard(
    investment: Investment,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(investment.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    if (investment.fundName.isNotBlank()) {
                        Text(
                            "Fund: ${investment.fundName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        "${investment.provider} - ${investment.accountType.name.replace("_", " ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (investment.ownerName != null) {
                        Text(
                            "Owner: ${investment.ownerName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (investment.isForKids) {
                        Text(
                            "For Kids",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFF9800),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    if (investment.frequency != InvestmentFrequency.ONE_TIME) {
                        Text(
                            "${formatCurrency(investment.contributionAmount)}/${investment.displayFrequency.lowercase()}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            "Invested: ${formatCurrency(investment.totalContributed)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Value: ${formatCurrency(investment.currentValue)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    val gainColor = if (investment.gainLoss >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                    Text(
                        "${if (investment.gainLoss >= 0) "+" else ""}${formatCurrency(investment.gainLoss)} (${if (investment.gainLoss >= 0) "+" else ""}${investment.gainLossPercent.roundToInt()}%)",
                        style = MaterialTheme.typography.bodySmall,
                        color = gainColor
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddInvestmentDialog(
    members: List<HouseholdMember>,
    preselectedMemberId: String?,
    preselectedIsForKids: Boolean,
    onDismiss: () -> Unit,
    onAdd: (Investment) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var fundName by remember { mutableStateOf("") }
    var provider by remember { mutableStateOf("") }
    var accountType by remember { mutableStateOf(UKAccountType.STOCKS_SHARES_ISA) }
    var assetClass by remember { mutableStateOf(AssetClass.STOCKS) }
    var frequency by remember { mutableStateOf(InvestmentFrequency.MONTHLY) }
    var contributionAmount by remember { mutableStateOf("") }
    var currentValue by remember { mutableStateOf("") }
    var totalContributed by remember { mutableStateOf("") }
    var selectedMember by remember { mutableStateOf(members.find { it.id == preselectedMemberId } ?: members.firstOrNull()) }
    var isForKids by remember { mutableStateOf(preselectedIsForKids) }

    var accountTypeExpanded by remember { mutableStateOf(false) }
    var assetClassExpanded by remember { mutableStateOf(false) }
    var frequencyExpanded by remember { mutableStateOf(false) }
    var memberExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Investment") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Investment Name") },
                        placeholder = { Text("e.g., ISA 2024, Trading Account") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = fundName,
                        onValueChange = { fundName = it },
                        label = { Text("Fund Name (optional)") },
                        placeholder = { Text("e.g., Vanguard S&P 500 ETF") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = provider,
                        onValueChange = { provider = it },
                        label = { Text("Provider") },
                        placeholder = { Text("e.g., Trading 212, Vanguard") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    ExposedDropdownMenuBox(expanded = accountTypeExpanded, onExpandedChange = { accountTypeExpanded = it }) {
                        OutlinedTextField(
                            value = accountType.name.replace("_", " "),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Account Type") },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(expanded = accountTypeExpanded, onDismissRequest = { accountTypeExpanded = false }) {
                            // Show investment-related account types
                            listOf(
                                UKAccountType.STOCKS_SHARES_ISA,
                                UKAccountType.JUNIOR_ISA,
                                UKAccountType.LIFETIME_ISA,
                                UKAccountType.GENERAL_INVESTMENT,
                                UKAccountType.SIPP,
                                UKAccountType.WORKPLACE_PENSION,
                                UKAccountType.CRYPTO,
                                UKAccountType.OTHER
                            ).forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.name.replace("_", " ")) },
                                    onClick = { accountType = type; accountTypeExpanded = false }
                                )
                            }
                        }
                    }
                }
                item {
                    ExposedDropdownMenuBox(expanded = assetClassExpanded, onExpandedChange = { assetClassExpanded = it }) {
                        OutlinedTextField(
                            value = assetClass.name,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Asset Class") },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(expanded = assetClassExpanded, onDismissRequest = { assetClassExpanded = false }) {
                            AssetClass.entries.forEach { asset ->
                                DropdownMenuItem(
                                    text = { Text(asset.name) },
                                    onClick = { assetClass = asset; assetClassExpanded = false }
                                )
                            }
                        }
                    }
                }
                item {
                    ExposedDropdownMenuBox(expanded = frequencyExpanded, onExpandedChange = { frequencyExpanded = it }) {
                        OutlinedTextField(
                            value = when (frequency) {
                                InvestmentFrequency.ONE_TIME -> "One-time"
                                InvestmentFrequency.WEEKLY -> "Weekly"
                                InvestmentFrequency.MONTHLY -> "Monthly"
                                InvestmentFrequency.QUARTERLY -> "Quarterly"
                                InvestmentFrequency.ANNUALLY -> "Annually"
                            },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Contribution Frequency") },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(expanded = frequencyExpanded, onDismissRequest = { frequencyExpanded = false }) {
                            InvestmentFrequency.entries.forEach { freq ->
                                DropdownMenuItem(
                                    text = {
                                        Text(when (freq) {
                                            InvestmentFrequency.ONE_TIME -> "One-time"
                                            InvestmentFrequency.WEEKLY -> "Weekly"
                                            InvestmentFrequency.MONTHLY -> "Monthly"
                                            InvestmentFrequency.QUARTERLY -> "Quarterly"
                                            InvestmentFrequency.ANNUALLY -> "Annually"
                                        })
                                    },
                                    onClick = { frequency = freq; frequencyExpanded = false }
                                )
                            }
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = contributionAmount,
                        onValueChange = { contributionAmount = it },
                        label = { Text(if (frequency == InvestmentFrequency.ONE_TIME) "Investment Amount" else "Contribution Amount") },
                        prefix = { Text("£") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = currentValue,
                        onValueChange = { currentValue = it },
                        label = { Text("Current Portfolio Value") },
                        prefix = { Text("£") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = totalContributed,
                        onValueChange = { totalContributed = it },
                        label = { Text("Total Amount Contributed") },
                        prefix = { Text("£") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (members.isNotEmpty()) {
                    item {
                        ExposedDropdownMenuBox(expanded = memberExpanded, onExpandedChange = { memberExpanded = it }) {
                            OutlinedTextField(
                                value = selectedMember?.name ?: "Select owner",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Owner") },
                                modifier = Modifier.fillMaxWidth().menuAnchor()
                            )
                            ExposedDropdownMenu(expanded = memberExpanded, onDismissRequest = { memberExpanded = false }) {
                                members.forEach { member ->
                                    DropdownMenuItem(
                                        text = { Text(member.name) },
                                        onClick = { selectedMember = member; memberExpanded = false }
                                    )
                                }
                            }
                        }
                    }
                }
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = isForKids,
                            onCheckedChange = { isForKids = it }
                        )
                        Text("This is for kids (Junior ISA, etc.)")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && provider.isNotBlank()) {
                        val contribAmount = contributionAmount.toDoubleOrNull() ?: 0.0
                        val currValue = currentValue.toDoubleOrNull() ?: 0.0
                        val totalContrib = totalContributed.toDoubleOrNull() ?: contribAmount

                        onAdd(Investment(
                            name = name,
                            fundName = fundName,
                            provider = provider,
                            accountType = accountType,
                            assetClass = assetClass,
                            frequency = frequency,
                            contributionAmount = contribAmount,
                            currentValue = currValue,
                            totalContributed = totalContrib,
                            ownerId = selectedMember?.id,
                            ownerName = selectedMember?.name,
                            isForKids = isForKids
                        ))
                    }
                }
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditInvestmentDialog(
    investment: Investment,
    members: List<HouseholdMember>,
    onDismiss: () -> Unit,
    onSave: (Investment) -> Unit
) {
    var name by remember { mutableStateOf(investment.name) }
    var fundName by remember { mutableStateOf(investment.fundName) }
    var provider by remember { mutableStateOf(investment.provider) }
    var accountType by remember { mutableStateOf(investment.accountType) }
    var assetClass by remember { mutableStateOf(investment.assetClass) }
    var frequency by remember { mutableStateOf(investment.frequency) }
    var contributionAmount by remember { mutableStateOf(investment.contributionAmount.toString()) }
    var currentValue by remember { mutableStateOf(investment.currentValue.toString()) }
    var totalContributed by remember { mutableStateOf(investment.totalContributed.toString()) }
    var selectedMember by remember { mutableStateOf(members.find { it.id == investment.ownerId }) }
    var isForKids by remember { mutableStateOf(investment.isForKids) }

    var accountTypeExpanded by remember { mutableStateOf(false) }
    var assetClassExpanded by remember { mutableStateOf(false) }
    var frequencyExpanded by remember { mutableStateOf(false) }
    var memberExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Investment") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Investment Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = fundName,
                        onValueChange = { fundName = it },
                        label = { Text("Fund Name (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = provider,
                        onValueChange = { provider = it },
                        label = { Text("Provider") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    ExposedDropdownMenuBox(expanded = accountTypeExpanded, onExpandedChange = { accountTypeExpanded = it }) {
                        OutlinedTextField(
                            value = accountType.name.replace("_", " "),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Account Type") },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(expanded = accountTypeExpanded, onDismissRequest = { accountTypeExpanded = false }) {
                            listOf(
                                UKAccountType.STOCKS_SHARES_ISA,
                                UKAccountType.JUNIOR_ISA,
                                UKAccountType.LIFETIME_ISA,
                                UKAccountType.GENERAL_INVESTMENT,
                                UKAccountType.SIPP,
                                UKAccountType.WORKPLACE_PENSION,
                                UKAccountType.CRYPTO,
                                UKAccountType.OTHER
                            ).forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.name.replace("_", " ")) },
                                    onClick = { accountType = type; accountTypeExpanded = false }
                                )
                            }
                        }
                    }
                }
                item {
                    ExposedDropdownMenuBox(expanded = assetClassExpanded, onExpandedChange = { assetClassExpanded = it }) {
                        OutlinedTextField(
                            value = assetClass.name,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Asset Class") },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(expanded = assetClassExpanded, onDismissRequest = { assetClassExpanded = false }) {
                            AssetClass.entries.forEach { asset ->
                                DropdownMenuItem(
                                    text = { Text(asset.name) },
                                    onClick = { assetClass = asset; assetClassExpanded = false }
                                )
                            }
                        }
                    }
                }
                item {
                    ExposedDropdownMenuBox(expanded = frequencyExpanded, onExpandedChange = { frequencyExpanded = it }) {
                        OutlinedTextField(
                            value = when (frequency) {
                                InvestmentFrequency.ONE_TIME -> "One-time"
                                InvestmentFrequency.WEEKLY -> "Weekly"
                                InvestmentFrequency.MONTHLY -> "Monthly"
                                InvestmentFrequency.QUARTERLY -> "Quarterly"
                                InvestmentFrequency.ANNUALLY -> "Annually"
                            },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Contribution Frequency") },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(expanded = frequencyExpanded, onDismissRequest = { frequencyExpanded = false }) {
                            InvestmentFrequency.entries.forEach { freq ->
                                DropdownMenuItem(
                                    text = {
                                        Text(when (freq) {
                                            InvestmentFrequency.ONE_TIME -> "One-time"
                                            InvestmentFrequency.WEEKLY -> "Weekly"
                                            InvestmentFrequency.MONTHLY -> "Monthly"
                                            InvestmentFrequency.QUARTERLY -> "Quarterly"
                                            InvestmentFrequency.ANNUALLY -> "Annually"
                                        })
                                    },
                                    onClick = { frequency = freq; frequencyExpanded = false }
                                )
                            }
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = contributionAmount,
                        onValueChange = { contributionAmount = it },
                        label = { Text(if (frequency == InvestmentFrequency.ONE_TIME) "Investment Amount" else "Contribution Amount") },
                        prefix = { Text("£") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = currentValue,
                        onValueChange = { currentValue = it },
                        label = { Text("Current Portfolio Value") },
                        prefix = { Text("£") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = totalContributed,
                        onValueChange = { totalContributed = it },
                        label = { Text("Total Amount Contributed") },
                        prefix = { Text("£") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (members.isNotEmpty()) {
                    item {
                        ExposedDropdownMenuBox(expanded = memberExpanded, onExpandedChange = { memberExpanded = it }) {
                            OutlinedTextField(
                                value = selectedMember?.name ?: "Select owner",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Owner") },
                                modifier = Modifier.fillMaxWidth().menuAnchor()
                            )
                            ExposedDropdownMenu(expanded = memberExpanded, onDismissRequest = { memberExpanded = false }) {
                                members.forEach { member ->
                                    DropdownMenuItem(
                                        text = { Text(member.name) },
                                        onClick = { selectedMember = member; memberExpanded = false }
                                    )
                                }
                            }
                        }
                    }
                }
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = isForKids,
                            onCheckedChange = { isForKids = it }
                        )
                        Text("This is for kids (Junior ISA, etc.)")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && provider.isNotBlank()) {
                        onSave(investment.copy(
                            name = name,
                            fundName = fundName,
                            provider = provider,
                            accountType = accountType,
                            assetClass = assetClass,
                            frequency = frequency,
                            contributionAmount = contributionAmount.toDoubleOrNull() ?: 0.0,
                            currentValue = currentValue.toDoubleOrNull() ?: 0.0,
                            totalContributed = totalContributed.toDoubleOrNull() ?: 0.0,
                            ownerId = selectedMember?.id,
                            ownerName = selectedMember?.name,
                            isForKids = isForKids,
                            lastUpdated = currentTimeMillis()
                        ))
                    }
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun SettingsScreen(
    syncStatus: SyncStatus,
    currentToken: String?,
    onSaveToken: (String) -> Unit,
    onClearToken: () -> Unit,
    onSync: () -> Unit,
    onForceRefresh: () -> Unit
) {
    var tokenInput by remember { mutableStateOf("") }
    var showToken by remember { mutableStateOf(false) }
    val hasToken = !currentToken.isNullOrBlank()

    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }

        // Token Input Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("GitHub Token", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))

                    if (hasToken) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("✓ Token configured", color = Color(0xFF4CAF50))
                                Text(
                                    if (showToken) currentToken!! else "••••••••••••${currentToken!!.takeLast(4)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row {
                                TextButton(onClick = { showToken = !showToken }) {
                                    Text(if (showToken) "Hide" else "Show")
                                }
                                TextButton(onClick = onClearToken) {
                                    Text("Remove", color = Color.Red)
                                }
                            }
                        }
                    } else {
                        Text("Enter your GitHub token to sync across devices:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = tokenInput,
                            onValueChange = { tokenInput = it },
                            label = { Text("GitHub Token") },
                            placeholder = { Text("ghp_xxxxxxxxxxxx") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = if (showToken) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation()
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    val trimmedToken = tokenInput.trim()
                                    if (trimmedToken.isNotBlank()) {
                                        onSaveToken(trimmedToken)
                                        tokenInput = ""
                                    }
                                },
                                enabled = tokenInput.isNotBlank()
                            ) {
                                Text("Save Token")
                            }
                            TextButton(onClick = { showToken = !showToken }) {
                                Text(if (showToken) "Hide" else "Show")
                            }
                        }
                    }
                }
            }
        }

        // Sync Status Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Cloud Sync", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))

                    val statusText = when (syncStatus) {
                        is SyncStatus.Idle -> if (hasToken) "Ready to sync" else "Add token to enable sync"
                        is SyncStatus.Syncing -> "Syncing..."
                        is SyncStatus.NotConfigured -> "Add GitHub token above to enable"
                        is SyncStatus.Success -> "✓ ${syncStatus.message}"
                        is SyncStatus.Error -> "✗ ${syncStatus.message}"
                    }
                    Text(statusText, color = when (syncStatus) {
                        is SyncStatus.Error -> Color.Red
                        is SyncStatus.Success -> Color(0xFF4CAF50)
                        is SyncStatus.NotConfigured -> Color(0xFFFF9800)
                        else -> Color.Gray
                    })

                    if (hasToken) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Sync downloads the latest data from cloud. Use when switching between browsers/devices.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = onForceRefresh,
                            enabled = syncStatus !is SyncStatus.Syncing
                        ) {
                            Text(if (syncStatus is SyncStatus.Syncing) "Syncing..." else "Sync from Cloud")
                        }
                    }
                }
            }
        }

        // Instructions Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("How to get a GitHub Token", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("1. Go to github.com → Settings → Developer settings → Personal access tokens → Fine-grained tokens", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text("2. Click 'Generate new token'", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text("3. Give it a name like 'WealthMate'", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text("4. Under 'Account permissions' → 'Gists' → Select 'Read and write'", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text("5. Click 'Generate token' and copy it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    Text("💡 Share this same token with your partner so you both see the same data!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // About Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("About WealthMate", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("Version 2.0", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Your data is stored locally and synced to a private GitHub Gist.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text("Data is encrypted in transit and only accessible with your token.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// Utility functions
fun formatCurrency(amount: Double): String {
    val wholePart = amount.toLong()
    val decimalPart = ((amount - wholePart) * 100).roundToInt()
    val decimalStr = decimalPart.toString().padStart(2, '0')
    return "£$wholePart.$decimalStr"
}

fun parseColor(hex: String): Color {
    return try {
        Color(hex.removePrefix("#").toLong(16) or 0xFF000000)
    } catch (e: Exception) {
        Color(0xFF4CAF50)
    }
}
