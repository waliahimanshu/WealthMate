package com.waliahimanshu.wealthmate.savings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.waliahimanshu.wealthmate.HouseholdFinances
import com.waliahimanshu.wealthmate.SavingsAccount
import com.waliahimanshu.wealthmate.UKAccountType
import com.waliahimanshu.wealthmate.components.displayCurrency
import com.waliahimanshu.wealthmate.dashboard.SavingsOverviewCard

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
    var deletingAccount by remember { mutableStateOf<SavingsAccount?>(null) }

    val tabs = listOf("Overview", "Joint") + data.members.map { it.name }

    val currentAccounts = when (selectedTab) {
        0 -> data.allSavings
        1 -> data.sharedAccounts
        else -> data.members.getOrNull(selectedTab - 2)?.savings ?: emptyList()
    }
    val currentMemberId = when (selectedTab) {
        0 -> null
        1 -> null
        else -> data.members.getOrNull(selectedTab - 2)?.id
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        savingsContent(
            data = data,
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            tabs = tabs,
            currentAccounts = currentAccounts,
            currentMemberId = currentMemberId,
            onAddClick = {
                addingForMemberId = currentMemberId
                showAddDialog = true
            },
            onEditClick = { account ->
                editingAccount = account
                editingForMemberId = account.ownerId
            },
            onDeleteClick = { account -> deletingAccount = account }
        )
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

    deletingAccount?.let { account ->
        DeleteConfirmationDialog(
            itemName = account.name,
            onDismiss = { deletingAccount = null },
            onConfirm = {
                val accountOwnerId = account.ownerId
                if (accountOwnerId == null) {
                    onUpdateSharedAccounts(data.sharedAccounts.filter { it.id != account.id })
                } else {
                    val member = data.members.find { it.id == accountOwnerId }
                    if (member != null) {
                        onUpdateMemberSavings(accountOwnerId, member.savings.filter { it.id != account.id })
                    }
                }
                deletingAccount = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
fun LazyListScope.savingsContent(
    data: HouseholdFinances,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    tabs: List<String>,
    currentAccounts: List<SavingsAccount>,
    currentMemberId: String?,
    onAddClick: () -> Unit,
    onEditClick: (SavingsAccount) -> Unit,
    onDeleteClick: (SavingsAccount) -> Unit
) {
    item {
        Text("Savings Accounts", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    }

    item {
        ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 0.dp) {
            tabs.forEachIndexed { index, title ->
                Tab(selected = selectedTab == index, onClick = { onTabSelected(index) }, text = { Text(title) })
            }
        }
    }

    item {
        SavingsOverviewCard(
            accounts = currentAccounts,
            title = when (selectedTab) {
                0 -> "Total Savings"
                1 -> "Joint Savings"
                else -> "${tabs[selectedTab]} Savings"
            }
        )
    }

    if (selectedTab > 0) {
        item {
            Button(onClick = onAddClick) {
                Text("Add Account")
            }
        }
    }

    items(currentAccounts) { account ->
        SavingsAccountCard(
            account = account,
            onEdit = { onEditClick(account) },
            onDelete = { onDeleteClick(account) }
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
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(account.name, fontWeight = FontWeight.Bold)
                Text("${account.provider} - ${account.accountType.name.replace("_", " ")}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${account.interestRate}% AER", style = MaterialTheme.typography.bodySmall, color = Color(0xFF4CAF50))
            }
            Text(displayCurrency(account.balance), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
            }
        }
    }
}

@Composable
fun DeleteConfirmationDialog(
    itemName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete $itemName?") },
        text = { Text("Are you sure you want to delete this? This cannot be undone.") },
        confirmButton = {
            Button(onClick = onConfirm) { Text("Delete") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
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
                OutlinedTextField(value = balance, onValueChange = { balance = it }, label = { Text("Balance") }, prefix = { Text("£") }, keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal
                ), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = interestRate, onValueChange = { interestRate = it }, label = { Text("Interest Rate (AER)") }, suffix = { Text("%") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
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
