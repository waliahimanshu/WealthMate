package com.waliahimanshu.wealthmate

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.waliahimanshu.wealthmate.storage.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class Screen {
    DASHBOARD,
    MEMBERS,
    OUTGOINGS,
    SAVINGS,
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
    var currentScreen by remember { mutableStateOf(Screen.DASHBOARD) }
    val householdData by repository.data.collectAsState()
    val syncStatus by repository.syncStatus.collectAsState()
    val isLoading by repository.isLoading.collectAsState()
    val scope = rememberCoroutineScope()

    // Selected member for detailed view (null = household view)
    var selectedMemberId by remember { mutableStateOf<String?>(null) }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF4CAF50),
            secondary = Color(0xFF81C784),
            tertiary = Color(0xFF64B5F6),
            surface = Color(0xFF1E1E1E),
            surfaceVariant = Color(0xFF2D2D2D),
            background = Color(0xFF121212),
            onPrimary = Color.White,
            onSurface = Color.White,
            onBackground = Color.White
        )
    ) {
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
                            Screen.GOALS -> GoalsScreen(
                                data = data,
                                onUpdateGoals = { goals ->
                                    scope.launch {
                                        repository.updateData { it.copy(sharedGoals = goals) }
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
        is SyncStatus.Idle -> Color.Gray to ""
        is SyncStatus.Syncing -> Color(0xFF2196F3) to "Syncing..."
        is SyncStatus.NotConfigured -> Color(0xFFFF9800) to "Cloud sync not configured"
        is SyncStatus.Success -> Color(0xFF4CAF50) to status.message
        is SyncStatus.Error -> Color(0xFFF44336) to status.message
    }

    if (text.isNotEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(color.copy(alpha = 0.2f))
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text, style = MaterialTheme.typography.bodySmall, color = color)
        }
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
            Text(
                "WealthMate",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))

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
            // Household overview
            item {
                SummaryCard("Total Household Income", formatCurrency(data.totalHouseholdIncome), Color(0xFF4CAF50))
            }
            item {
                SummaryCard("Total Outgoings", formatCurrency(data.totalOutgoings), Color(0xFFF44336))
            }
            item {
                SummaryCard("Net Monthly", formatCurrency(data.netMonthlyHousehold),
                    if (data.netMonthlyHousehold >= 0) Color(0xFF2196F3) else Color(0xFFF44336))
            }
            item {
                SummaryCard("Total Savings", formatCurrency(data.totalSavings), Color(0xFF9C27B0))
            }

            // Goals progress
            if (data.sharedGoals.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Text("Shared Goals", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                items(data.sharedGoals.take(3)) { goal ->
                    GoalProgressCard(goal)
                }
            }

            // Members breakdown
            if (data.members.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Text("Members", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
                color = Color.Gray
            )
        }
    }
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
                    color = Color.Gray
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(formatCurrency(member.totalSavings), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("savings", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
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
                    Text("Add yourself and your partner to get started", color = Color.Gray)
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
                    Text("Salary: ${formatCurrency(member.salary)}/mo", color = Color.Gray)
                }
                IconButton(onClick = onEdit) {
                    Text("✏️")
                }
                IconButton(onClick = onDelete) {
                    Text("🗑️", color = Color.Red)
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
        Text(label, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
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
    onUpdateMortgage: (MortgageInfo?) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var addingForMemberId by remember { mutableStateOf<String?>(null) }

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
            }
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
                Text("${mortgage.provider} - ${formatCurrency(mortgage.monthlyPayment)}/mo", color = Color.Gray)
                Text("${mortgage.interestRate}% - ${mortgage.termRemainingMonths} months remaining", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
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
fun OutgoingCard(outgoing: Outgoing, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(outgoing.name, fontWeight = FontWeight.Bold)
                Text(outgoing.category.name.replace("_", " "), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(formatCurrency(outgoing.amount), fontWeight = FontWeight.Bold, color = Color(0xFFF44336))
                IconButton(onClick = onDelete) {
                    Text("✕", color = Color.Red)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOutgoingDialog(onDismiss: () -> Unit, onAdd: (Outgoing) -> Unit) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(OutgoingCategory.OTHER) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Outgoing") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), prefix = { Text("£") }, modifier = Modifier.fillMaxWidth())
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(value = category.name.replace("_", " "), onValueChange = {}, readOnly = true, label = { Text("Category") }, modifier = Modifier.fillMaxWidth().menuAnchor())
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        OutgoingCategory.entries.forEach { cat ->
                            DropdownMenuItem(text = { Text(cat.name.replace("_", " ")) }, onClick = { category = cat; expanded = false })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isNotBlank() && (amount.toDoubleOrNull() ?: 0.0) > 0) {
                    onAdd(Outgoing(name = name, amount = amount.toDoubleOrNull() ?: 0.0, category = category))
                }
            }) { Text("Add") }
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

        Text("Total: ${formatCurrency(currentAccounts.sumOf { it.balance })}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)

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
}

@Composable
fun SavingsAccountCard(account: SavingsAccount, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(account.name, fontWeight = FontWeight.Bold)
                Text("${account.provider} - ${account.accountType.name.replace("_", " ")}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Text("${account.interestRate}% AER", style = MaterialTheme.typography.bodySmall, color = Color(0xFF4CAF50))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(formatCurrency(account.balance), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                IconButton(onClick = onDelete) {
                    Text("✕", color = Color.Red)
                }
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
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(value = accountType.name.replace("_", " "), onValueChange = {}, readOnly = true, label = { Text("Account Type") }, modifier = Modifier.fillMaxWidth().menuAnchor())
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        UKAccountType.entries.forEach { type ->
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
fun GoalsScreen(
    data: HouseholdFinances,
    onUpdateGoals: (List<SharedGoal>) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var contributingToGoal by remember { mutableStateOf<SharedGoal?>(null) }

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
                    Text("Add a shared goal like a house deposit or holiday", color = Color.Gray, textAlign = TextAlign.Center)
                }
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(data.sharedGoals) { goal ->
                GoalCard(
                    goal = goal,
                    members = data.members,
                    onContribute = { contributingToGoal = goal },
                    onDelete = { onUpdateGoals(data.sharedGoals.filter { it.id != goal.id }) }
                )
            }
        }
    }

    if (showAddDialog) {
        AddGoalDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { goal ->
                onUpdateGoals(data.sharedGoals + goal)
                showAddDialog = false
            }
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
fun GoalCard(goal: SharedGoal, members: List<HouseholdMember>, onContribute: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(goal.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(goal.category.name.replace("_", " "), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
                Row {
                    TextButton(onClick = onContribute) { Text("Contribute") }
                    IconButton(onClick = onDelete) { Text("🗑️") }
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
                Text("${formatCurrency(goal.remainingAmount)} to go", color = Color.Gray)
            }
            Text("Target: ${formatCurrency(goal.targetAmount)}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)

            if (goal.contributions.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("Recent contributions:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                goal.contributions.takeLast(3).reversed().forEach { contribution ->
                    Text("${contribution.memberName}: ${formatCurrency(contribution.amount)}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGoalDialog(onDismiss: () -> Unit, onAdd: (SharedGoal) -> Unit) {
    var name by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(GoalCategory.OTHER) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Shared Goal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Goal Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = target, onValueChange = { target = it }, label = { Text("Target Amount") }, prefix = { Text("£") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(value = category.name.replace("_", " "), onValueChange = {}, readOnly = true, label = { Text("Category") }, modifier = Modifier.fillMaxWidth().menuAnchor())
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        GoalCategory.entries.forEach { cat ->
                            DropdownMenuItem(text = { Text(cat.name.replace("_", " ")) }, onClick = { category = cat; expanded = false })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isNotBlank() && (target.toDoubleOrNull() ?: 0.0) > 0) {
                    onAdd(SharedGoal(name = name, targetAmount = target.toDoubleOrNull() ?: 0.0, category = category))
                }
            }) { Text("Add") }
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
                                    color = Color.Gray
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
                        Text("Enter your GitHub token to sync across devices:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
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
                                    if (tokenInput.isNotBlank()) {
                                        onSaveToken(tokenInput)
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
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = onSync) { Text("Sync Now") }
                            OutlinedButton(onClick = onForceRefresh) { Text("Force Refresh") }
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
                    Text("1. Go to github.com → Settings → Developer settings → Personal access tokens → Fine-grained tokens", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Spacer(Modifier.height(4.dp))
                    Text("2. Click 'Generate new token'", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Spacer(Modifier.height(4.dp))
                    Text("3. Give it a name like 'WealthMate'", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Spacer(Modifier.height(4.dp))
                    Text("4. Under 'Account permissions' → 'Gists' → Select 'Read and write'", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Spacer(Modifier.height(4.dp))
                    Text("5. Click 'Generate token' and copy it", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
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
                    Text("Version 2.0", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Text("Your data is stored locally and synced to a private GitHub Gist.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Spacer(Modifier.height(8.dp))
                    Text("Data is encrypted in transit and only accessible with your token.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
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
