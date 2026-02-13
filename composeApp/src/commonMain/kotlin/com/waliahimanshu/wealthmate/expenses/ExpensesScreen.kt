package com.waliahimanshu.wealthmate.expenses

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import com.waliahimanshu.wealthmate.components.formatCurrency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
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
        Text("Expenses", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Manage all monthly expenses", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

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
            Text("Add Expense")
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
        title = { Text("Add Expense") },
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
                        OutlinedTextField(value = newCategoryName, onValueChange = { newCategoryName = it }, label = { Text("New Category") }, modifier = Modifier.weight(1f), singleLine = true)
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
        title = { Text("Edit Expense") },
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
                        OutlinedTextField(value = newCategoryName, onValueChange = { newCategoryName = it }, label = { Text("New Category") }, modifier = Modifier.weight(1f), singleLine = true)
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
