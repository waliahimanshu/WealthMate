package com.waliahimanshu.wealthmate.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.waliahimanshu.wealthmate.*
import com.waliahimanshu.wealthmate.components.*
import kotlin.math.roundToInt

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
                "Dashboard Overview",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Your complete financial snapshot",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
        }

        // Stat cards row
        item {
            StatCardRow {
                StatCard(
                    title = "Total Wealth",
                    value = formatCurrency(data.totalSavings + data.totalPortfolioValue + (data.mortgage?.equity ?: 0.0)),
                    icon = Icons.Outlined.AccountBalance,
                    subtitle = "Across all accounts",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Monthly Income",
                    value = formatCurrency(data.totalHouseholdIncome),
                    icon = Icons.Outlined.TrendingUp,
                    valueColor = Color(0xFF4CAF50),
                    subtitle = "Combined income",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Monthly Expenses",
                    value = formatCurrency(data.totalOutgoings),
                    icon = Icons.Outlined.Receipt,
                    valueColor = Color(0xFFF44336),
                    subtitle = "Total outgoings",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Net Monthly",
                    value = formatCurrency(data.netMonthlyHousehold),
                    icon = Icons.Outlined.Star,
                    valueColor = if (data.netMonthlyHousehold >= 0) Color(0xFF4CAF50) else Color(0xFFF44336),
                    subtitle = "Available to save",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Member chips for filtering
        item {
            Spacer(Modifier.height(4.dp))
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
            Spacer(Modifier.height(8.dp))
        }

        // Show household or member stats
        if (selectedMemberId == null) {
            // Income vs Outgoings
            item {
                IncomeOutgoingsOverviewCard(data)
            }

            // Total Savings
            item {
                Spacer(Modifier.height(4.dp))
                SavingsOverviewCard(accounts = data.allSavings, title = "Total Savings")
            }

            item {
                HorizontalDivider()
            }

            // Investments
            if (data.investments.isNotEmpty() || data.totalMonthlyInvestments > 0) {
                item {
                    InvestmentsSummaryCard(data)
                }
            }

            // Property Equity
            data.mortgage?.let { mortgage ->
                item {
                    PropertyEquityCard(mortgage)
                }
            }

            // Kids JISA
            val kidsInvestments = data.kidsInvestments
            if (kidsInvestments.isNotEmpty()) {
                item {
                    KidsJISACard(kidsInvestments, data.members)
                }
            }

            // Goals
            if (data.sharedGoals.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(4.dp))
                    Text("Goals Progress", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                items(data.sharedGoals.take(3)) { goal ->
                    GoalProgressCard(goal)
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
        shape = RoundedCornerShape(16.dp),
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
        shape = RoundedCornerShape(16.dp),
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

    val typeBreakdown = data.investments
        .groupBy { it.accountType }
        .map { (type, investments) -> type.name.replace("_", " ") to investments.sumOf { it.currentValue } }
        .filter { it.second > 0 }
        .sortedByDescending { it.second }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Investments", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(formatCurrency(totalValue), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(Modifier.height(12.dp))

            if (typeBreakdown.isNotEmpty()) {
                typeBreakdown.forEach { (type, value) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(type, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatCurrency(value), fontWeight = FontWeight.SemiBold)
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Monthly", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${formatCurrency(data.totalMonthlyInvestments)}/mo", fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Gain/Loss", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
fun IncomeOutgoingsOverviewCard(data: HouseholdFinances) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
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
                Column(horizontalAlignment = Alignment.End) {
                    Text("Expenses", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
fun SavingsOverviewCard(
    accounts: List<SavingsAccount>,
    title: String = "Total Savings"
) {
    val easyTypes = listOf(
        UKAccountType.EASY_ACCESS,
        UKAccountType.CURRENT_ACCOUNT,
        UKAccountType.CASH_ISA,
        UKAccountType.REGULAR_SAVER
    )
    val lockedTypes = listOf(
        UKAccountType.FIXED_TERM,
        UKAccountType.NOTICE_ACCOUNT,
        UKAccountType.PREMIUM_BONDS
    )

    val totalSavings = accounts.sumOf { it.balance }
    val easyAccessSavings = accounts.filter { it.accountType in easyTypes }.sumOf { it.balance }
    val lockedSavings = accounts.filter { it.accountType in lockedTypes }.sumOf { it.balance }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                formatCurrency(totalSavings),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Cash Savings", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        formatCurrency(easyAccessSavings),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF4CAF50)
                    )
                    Text("Easy access", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Long-term", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        formatCurrency(lockedSavings),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFF9800)
                    )
                    Text("Fixed/Notice", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun PropertyEquityCard(mortgage: MortgageInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Property Equity", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Icon(Icons.Outlined.Home, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Current Value:", style = MaterialTheme.typography.bodyMedium)
                Text(formatCurrency(mortgage.propertyValue), fontWeight = FontWeight.SemiBold)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Mortgage Balance:", style = MaterialTheme.typography.bodyMedium)
                Text(formatCurrency(mortgage.remainingBalance), fontWeight = FontWeight.SemiBold)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total Equity:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(formatCurrency(mortgage.equity), fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
            }
        }
    }
}

@Composable
fun KidsJISACard(kidsInvestments: List<Investment>, members: List<HouseholdMember>) {
    val totalKids = kidsInvestments.sumOf { it.currentValue }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Kids JISA Accounts", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Icon(Icons.Outlined.FavoriteBorder, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(12.dp))
            kidsInvestments.forEach { inv ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(inv.name, style = MaterialTheme.typography.bodyMedium)
                    Text(formatCurrency(inv.currentValue), fontWeight = FontWeight.SemiBold)
                }
            }
            if (kidsInvestments.size > 1) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total JISA:", fontWeight = FontWeight.Bold)
                    Text(formatCurrency(totalKids), fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
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
    annualReturn: Double = 0.07
) {
    val projectedValue = calculateProjectedGrowth(currentValue, monthlyContribution, years, annualReturn)
    val totalContributions = currentValue + (monthlyContribution * 12 * years)
    val projectedGain = projectedValue - totalContributions

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
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
                    Text("${formatCurrency(monthlyContribution)}/mo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Potential value in $years years", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatCurrency(projectedValue), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Now: ${formatCurrency(currentValue)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                Text("(+${formatCurrency(projectedGain)} potential growth)", style = MaterialTheme.typography.bodySmall, color = Color(0xFF4CAF50))
            }
            Spacer(Modifier.height(4.dp))
            Text("Based on historical average returns. Actual returns may vary.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        }
    }
}

fun calculateProjectedGrowth(
    currentValue: Double,
    monthlyContribution: Double,
    years: Int,
    annualReturn: Double
): Double {
    val monthlyRate = annualReturn / 12
    val months = years * 12
    var futureValueCurrent = currentValue
    for (i in 1..years) {
        futureValueCurrent *= (1 + annualReturn)
    }
    var futureValueContributions = 0.0
    for (month in 1..months) {
        val monthsRemaining = months - month
        var contributionGrowth = monthlyContribution
        for (m in 1..monthsRemaining) {
            contributionGrowth *= (1 + monthlyRate)
        }
        futureValueContributions += contributionGrowth
    }
    return futureValueCurrent + futureValueContributions
}
