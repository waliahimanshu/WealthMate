package com.waliahimanshu.wealthmate.property

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.waliahimanshu.wealthmate.*
import com.waliahimanshu.wealthmate.components.StatCard
import com.waliahimanshu.wealthmate.components.StatCardRow
import com.waliahimanshu.wealthmate.components.displayCurrency
import kotlin.math.roundToInt

@Composable
fun PropertyScreen(
    data: HouseholdFinances,
    onUpdateMortgage: (MortgageInfo?) -> Unit
) {
    val mortgage = data.mortgage
    var showEditDialog by remember { mutableStateOf(false) }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(
                "Property & Mortgage",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Track your home equity and mortgage",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (mortgage == null) {
            item {
                EmptyPropertyState(onAdd = { showEditDialog = true })
            }
        } else {
            // Stat cards
            item {
                StatCardRow {
                    StatCard(
                        title = "Property Value",
                        value = displayCurrency(mortgage.propertyValue),
                        icon = Icons.Outlined.Cottage,
                        subtitle = "Current estimate",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Equity",
                        value = displayCurrency(mortgage.equity),
                        icon = Icons.Outlined.TrendingUp,
                        valueColor = Color(0xFF4CAF50),
                        subtitle = "${mortgage.equityPercent.roundToInt()}% ownership",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Mortgage Left",
                        value = displayCurrency(mortgage.remainingBalance),
                        icon = Icons.Outlined.AccountBalance,
                        valueColor = Color(0xFFF44336),
                        subtitle = "LTV ${mortgage.loanToValue.roundToInt()}%",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Monthly Payment",
                        value = displayCurrency(mortgage.monthlyPayment),
                        icon = Icons.Outlined.Payments,
                        valueColor = Color(0xFFFF9800),
                        subtitle = "${mortgage.interestRate}% rate",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Equity progress
            item {
                EquityProgressCard(mortgage)
            }

            // Mortgage details
            item {
                MortgageDetailsCard(mortgage, onEdit = { showEditDialog = true })
            }

            // Deal info
            if (mortgage.dealDescription.isNotBlank() || mortgage.fixedUntil != null) {
                item {
                    DealInfoCard(mortgage)
                }
            }

            // Payment info
            item {
                PaymentInfoCard(mortgage)
            }

            // Notes
            if (mortgage.notes.isNotBlank()) {
                item {
                    NotesCard(mortgage.notes)
                }
            }

            // Remove property button
            item {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { onUpdateMortgage(null) },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF44336)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Remove Property")
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showEditDialog) {
        PropertyEditDialog(
            mortgage = mortgage,
            onDismiss = { showEditDialog = false },
            onSave = {
                onUpdateMortgage(it)
                showEditDialog = false
            }
        )
    }
}

@Composable
private fun EmptyPropertyState(onAdd: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Outlined.Cottage,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "No property added yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Add your property and mortgage details to track equity, payments, and deal information.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onAdd) {
                Text("Add Property")
            }
        }
    }
}

@Composable
private fun EquityProgressCard(mortgage: MortgageInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Equity Progress", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))

            // Progress bar
            val equityFraction = if (mortgage.propertyValue > 0) {
                (mortgage.equity / mortgage.propertyValue).toFloat().coerceIn(0f, 1f)
            } else 0f

            LinearProgressIndicator(
                progress = { equityFraction },
                modifier = Modifier.fillMaxWidth().height(12.dp),
                color = Color(0xFF4CAF50),
                trackColor = Color(0xFFF44336).copy(alpha = 0.3f)
            )

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Your Equity", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "${displayCurrency(mortgage.equity)} (${mortgage.equityPercent.roundToInt()}%)",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Mortgage Left", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "${displayCurrency(mortgage.remainingBalance)} (${(100 - mortgage.equityPercent).roundToInt()}%)",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF44336)
                    )
                }
            }

            if (mortgage.originalMortgageAmount > 0) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Mortgage paid down", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(displayCurrency(mortgage.mortgagePaidDown), fontWeight = FontWeight.SemiBold, color = Color(0xFF4CAF50))
                }
            }

            if (mortgage.purchasePrice > 0 && mortgage.propertyValue > mortgage.purchasePrice) {
                val appreciation = mortgage.propertyValue - mortgage.purchasePrice
                val appreciationPct = (appreciation / mortgage.purchasePrice * 100).roundToInt()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Value appreciation", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "+${displayCurrency(appreciation)} (+$appreciationPct%)",
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF4CAF50)
                    )
                }
            }
        }
    }
}

@Composable
private fun MortgageDetailsCard(mortgage: MortgageInfo, onEdit: () -> Unit) {
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
                Text("Mortgage Details", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onEdit) { Text("Edit") }
            }

            Spacer(Modifier.height(8.dp))

            DetailRow("Lender", mortgage.provider)
            DetailRow("Interest Rate", "${mortgage.interestRate}%")
            DetailRow("Mortgage Type", mortgage.mortgageType.name.replace("_", " ").lowercase()
                .replaceFirstChar { it.uppercase() })
            DetailRow("Remaining Balance", displayCurrency(mortgage.remainingBalance))

            val termDisplay = buildString {
                if (mortgage.termRemainingYears > 0) append("${mortgage.termRemainingYears} years")
                if (mortgage.termRemainingExtraMonths > 0) {
                    if (mortgage.termRemainingYears > 0) append(" ")
                    append("${mortgage.termRemainingExtraMonths} months")
                }
                if (isEmpty()) append("0 months")
            }
            DetailRow("Term Remaining", termDisplay)

            if (mortgage.originalMortgageAmount > 0) {
                DetailRow("Original Mortgage", displayCurrency(mortgage.originalMortgageAmount))
            }
            if (mortgage.purchasePrice > 0) {
                DetailRow("Purchase Price", displayCurrency(mortgage.purchasePrice))
            }
        }
    }
}

@Composable
private fun DealInfoCard(mortgage: MortgageInfo) {
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
                Text("Deal Information", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.height(8.dp))

            if (mortgage.dealDescription.isNotBlank()) {
                DetailRow("Deal Type", mortgage.dealDescription)
            }

            mortgage.fixedUntil?.let { timestamp ->
                val remainingMs = timestamp - currentTimeMillis()
                val remainingMonths = (remainingMs / (30L * 24 * 60 * 60 * 1000)).toInt()

                if (remainingMonths > 0) {
                    val years = remainingMonths / 12
                    val months = remainingMonths % 12
                    val display = buildString {
                        if (years > 0) append("$years years")
                        if (months > 0) {
                            if (years > 0) append(" ")
                            append("$months months")
                        }
                    }
                    DetailRow("Fixed deal ends in", display)

                    // Warning if deal ending soon (within 6 months)
                    if (remainingMonths <= 6) {
                        Spacer(Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFFF9800).copy(alpha = 0.15f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Outlined.Warning, contentDescription = null, tint = Color(0xFFFF9800))
                                Text(
                                    "Your fixed deal ends soon. Consider remortgaging to avoid moving to your lender's SVR.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFFF9800)
                                )
                            }
                        }
                    }
                } else {
                    DetailRow("Fixed deal", "Expired — check your current rate")
                }
            }

            if (mortgage.overpaymentAllowancePercent > 0 && mortgage.originalMortgageAmount > 0) {
                val annualAllowance = mortgage.originalMortgageAmount * mortgage.overpaymentAllowancePercent / 100
                DetailRow("Overpayment Allowance", "${mortgage.overpaymentAllowancePercent.roundToInt()}% (${displayCurrency(annualAllowance)}/year)")
            }
        }
    }
}

@Composable
private fun PaymentInfoCard(mortgage: MortgageInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Payment Summary", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            DetailRow("Monthly Payment", displayCurrency(mortgage.monthlyPayment))
            DetailRow("Annual Cost", displayCurrency(mortgage.monthlyPayment * 12))

            if (mortgage.termRemainingMonths > 0) {
                val totalRemaining = mortgage.monthlyPayment * mortgage.termRemainingMonths
                DetailRow("Total Remaining Payments", displayCurrency(totalRemaining))
                val totalInterest = totalRemaining - mortgage.remainingBalance
                if (totalInterest > 0) {
                    DetailRow("Estimated Interest Left", displayCurrency(totalInterest))
                }
            }
        }
    }
}

@Composable
private fun NotesCard(notes: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Notes", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(notes, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
    }
}

// ============================================================
// EDIT DIALOG
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertyEditDialog(
    mortgage: MortgageInfo?,
    onDismiss: () -> Unit,
    onSave: (MortgageInfo) -> Unit
) {
    var provider by remember { mutableStateOf(mortgage?.provider ?: "") }
    var propertyValue by remember { mutableStateOf(mortgage?.propertyValue?.let { if (it > 0) it.toString() else "" } ?: "") }
    var purchasePrice by remember { mutableStateOf(mortgage?.purchasePrice?.let { if (it > 0) it.toString() else "" } ?: "") }
    var originalMortgageAmount by remember { mutableStateOf(mortgage?.originalMortgageAmount?.let { if (it > 0) it.toString() else "" } ?: "") }
    var remainingBalance by remember { mutableStateOf(mortgage?.remainingBalance?.toString() ?: "") }
    var monthlyPayment by remember { mutableStateOf(mortgage?.monthlyPayment?.toString() ?: "") }
    var interestRate by remember { mutableStateOf(mortgage?.interestRate?.toString() ?: "") }
    var termRemainingMonths by remember { mutableStateOf(mortgage?.termRemainingMonths?.toString() ?: "") }
    var totalTermMonths by remember { mutableStateOf(mortgage?.totalTermMonths?.let { if (it > 0) it.toString() else "" } ?: "") }
    var dealDescription by remember { mutableStateOf(mortgage?.dealDescription ?: "") }
    var overpaymentAllowance by remember { mutableStateOf(mortgage?.overpaymentAllowancePercent?.toString() ?: "10.0") }
    var notes by remember { mutableStateOf(mortgage?.notes ?: "") }

    // Mortgage type dropdown
    var mortgageType by remember { mutableStateOf(mortgage?.mortgageType ?: MortgageType.REPAYMENT) }
    var typeExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (mortgage == null) "Add Property" else "Edit Property") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text("Property", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                OutlinedTextField(
                    value = propertyValue, onValueChange = { propertyValue = it },
                    label = { Text("Current Property Value") }, prefix = { Text("£") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = purchasePrice, onValueChange = { purchasePrice = it },
                    label = { Text("Purchase Price") }, prefix = { Text("£") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text("Mortgage", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)

                OutlinedTextField(
                    value = provider, onValueChange = { provider = it },
                    label = { Text("Lender") }, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = remainingBalance, onValueChange = { remainingBalance = it },
                    label = { Text("Remaining Balance") }, prefix = { Text("£") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = originalMortgageAmount, onValueChange = { originalMortgageAmount = it },
                    label = { Text("Original Mortgage Amount") }, prefix = { Text("£") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = monthlyPayment, onValueChange = { monthlyPayment = it },
                    label = { Text("Monthly Payment") }, prefix = { Text("£") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = interestRate, onValueChange = { interestRate = it },
                    label = { Text("Interest Rate") }, suffix = { Text("%") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                // Mortgage type
                ExposedDropdownMenuBox(expanded = typeExpanded, onExpandedChange = { typeExpanded = it }) {
                    OutlinedTextField(
                        value = mortgageType.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Mortgage Type") },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                        MortgageType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }) },
                                onClick = { mortgageType = type; typeExpanded = false }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = termRemainingMonths, onValueChange = { termRemainingMonths = it },
                    label = { Text("Term Remaining (months)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = totalTermMonths, onValueChange = { totalTermMonths = it },
                    label = { Text("Original Total Term (months)") },
                    supportingText = { Text("e.g. 300 for 25 years") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text("Deal", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)

                OutlinedTextField(
                    value = dealDescription, onValueChange = { dealDescription = it },
                    label = { Text("Deal Type") },
                    supportingText = { Text("e.g. 2-year fixed, 5-year fixed, Tracker") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = overpaymentAllowance, onValueChange = { overpaymentAllowance = it },
                    label = { Text("Overpayment Allowance") }, suffix = { Text("%") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                OutlinedTextField(
                    value = notes, onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                if (provider.isNotBlank() || (propertyValue.toDoubleOrNull() ?: 0.0) > 0) {
                    onSave(
                        MortgageInfo(
                            provider = provider,
                            propertyValue = propertyValue.toDoubleOrNull() ?: 0.0,
                            purchasePrice = purchasePrice.toDoubleOrNull() ?: 0.0,
                            originalMortgageAmount = originalMortgageAmount.toDoubleOrNull() ?: 0.0,
                            remainingBalance = remainingBalance.toDoubleOrNull() ?: 0.0,
                            monthlyPayment = monthlyPayment.toDoubleOrNull() ?: 0.0,
                            interestRate = interestRate.toDoubleOrNull() ?: 0.0,
                            mortgageType = mortgageType,
                            dealDescription = dealDescription,
                            fixedUntil = mortgage?.fixedUntil, // Preserve existing value
                            termRemainingMonths = termRemainingMonths.toIntOrNull() ?: 0,
                            totalTermMonths = totalTermMonths.toIntOrNull() ?: 0,
                            overpaymentAllowancePercent = overpaymentAllowance.toDoubleOrNull() ?: 10.0,
                            owners = mortgage?.owners ?: emptyList(),
                            notes = notes
                        )
                    )
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
