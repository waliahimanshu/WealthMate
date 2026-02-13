package com.waliahimanshu.wealthmate.investments// ============================================
// INVESTMENTS SCREEN
// ============================================

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
import androidx.compose.material3.Icon
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
import com.waliahimanshu.wealthmate.AssetClass
import com.waliahimanshu.wealthmate.HouseholdFinances
import com.waliahimanshu.wealthmate.HouseholdMember
import com.waliahimanshu.wealthmate.Investment
import com.waliahimanshu.wealthmate.InvestmentFrequency
import com.waliahimanshu.wealthmate.UKAccountType
import com.waliahimanshu.wealthmate.components.*
import com.waliahimanshu.wealthmate.currentTimeMillis
import com.waliahimanshu.wealthmate.dashboard.ProjectedGrowthCard
import com.waliahimanshu.wealthmate.savings.SavingsScreen
import com.waliahimanshu.wealthmate.storage.*
import kotlin.math.roundToInt

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

        // Projected Growth Card
//        if (data.investments.isNotEmpty()) {
//            Spacer(Modifier.height(16.dp))
//            ProjectedGrowthCard(
//                currentValue = data.totalPortfolioValue,
//                monthlyContribution = data.totalMonthlyInvestments
//            )
//        }

        Button(onClick = { showAddDialog = true }) {
            Text("Add Investment")
        }

        // Pie chart for Overview tab
//        if (selectedTab == 0 && filteredInvestments.isNotEmpty()) {
//            // Asset class allocation pie chart
//            val assetAllocation = filteredInvestments
//                .groupBy { it.assetClass }
//                .map { (assetClass, investments) ->
//                    PieSlice(
//                        label = assetClass.name,
//                        value = investments.sumOf { it.currentValue },
//                        color = ChartColors.getColor(AssetClass.entries.indexOf(assetClass))
//                    )
//                }
//                .filter { it.value > 0 }
//
//            if (assetAllocation.isNotEmpty()) {
//                PieChartCard(
//                    title = "Allocation by Asset Class",
//                    slices = assetAllocation
//                )
//            }
//        }

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
        Column(modifier = Modifier.padding(8.dp)) {
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

            Spacer(Modifier.height(8.dp))

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