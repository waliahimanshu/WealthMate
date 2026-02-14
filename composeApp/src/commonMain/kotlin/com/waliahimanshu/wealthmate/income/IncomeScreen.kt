package com.waliahimanshu.wealthmate.income

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.waliahimanshu.wealthmate.HouseholdFinances
import com.waliahimanshu.wealthmate.HouseholdMember
import com.waliahimanshu.wealthmate.components.StatCard
import com.waliahimanshu.wealthmate.components.StatCardRow
import com.waliahimanshu.wealthmate.components.displayCurrency
import com.waliahimanshu.wealthmate.components.parseColor

@Composable
fun IncomeScreen(data: HouseholdFinances) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text("Income Tracking", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Manage all income sources", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
        }

        // Stat cards
        item {
            StatCardRow {
                StatCard(
                    title = "Total Monthly Income",
                    value = displayCurrency(data.totalHouseholdIncome),
                    icon = Icons.AutoMirrored.Outlined.TrendingUp,
                    valueColor = Color(0xFF4CAF50),
                    subtitle = "${displayCurrency(data.totalHouseholdIncome * 12)} annually",
                    modifier = Modifier.weight(1f)
                )
                data.members.forEachIndexed { index, member ->
                    StatCard(
                        title = "${member.name} Income",
                        value = displayCurrency(member.salary),
                        icon = Icons.AutoMirrored.Outlined.TrendingUp,
                        subtitle = "${((member.salary / data.totalHouseholdIncome) * 100).toInt()}% of total",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // All Income Sources
        item {
            Spacer(Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("All Income Sources", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))

                    if (data.members.isEmpty()) {
                        Text("Add members in Settings to track income.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        items(data.members) { member ->
            IncomeSourceCard(member)
        }
    }
}

@Composable
fun IncomeSourceCard(member: HouseholdMember) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Salary", fontWeight = FontWeight.Bold)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = parseColor(member.color).copy(alpha = 0.2f)
                    ) {
                        Text(
                            member.name,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = parseColor(member.color),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            "monthly",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "${displayCurrency(member.salary)} monthly",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
