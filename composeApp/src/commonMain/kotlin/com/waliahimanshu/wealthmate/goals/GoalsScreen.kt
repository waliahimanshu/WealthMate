package com.waliahimanshu.wealthmate.goals

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.waliahimanshu.wealthmate.*
import com.waliahimanshu.wealthmate.components.displayCurrency
import com.waliahimanshu.wealthmate.savings.DeleteConfirmationDialog
import kotlin.math.roundToInt

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
    var deletingGoal by remember { mutableStateOf<SharedGoal?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Goals", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Track progress towards your financial goals", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
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
                    onDelete = { deletingGoal = goal }
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

    deletingGoal?.let { goal ->
        DeleteConfirmationDialog(
            itemName = goal.name,
            onDismiss = { deletingGoal = null },
            onConfirm = {
                onUpdateGoals(data.sharedGoals.filter { it.id != goal.id })
                deletingGoal = null
            }
        )
    }
}

@Composable
fun GoalCard(goal: SharedGoal, members: List<HouseholdMember>, onContribute: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
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
                Text("${displayCurrency(goal.currentAmount)} saved", color = MaterialTheme.colorScheme.primary)
                Text("${displayCurrency(goal.remainingAmount)} to go", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("Target: ${displayCurrency(goal.targetAmount)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            if (goal.contributions.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("Recent contributions:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                goal.contributions.takeLast(3).reversed().forEach { contribution ->
                    Text("${contribution.memberName}: ${displayCurrency(contribution.amount)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    onSave(goal.copy(name = name, targetAmount = target.toDoubleOrNull() ?: goal.targetAmount, currentAmount = currentAmount.toDoubleOrNull() ?: goal.currentAmount, category = category, customCategory = customCategory))
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
                        OutlinedTextField(value = selectedMember?.name ?: "Select member", onValueChange = {}, readOnly = true, label = { Text("Who is contributing?") }, modifier = Modifier.fillMaxWidth().menuAnchor())
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            members.forEach { member ->
                                DropdownMenuItem(text = { Text(member.name) }, onClick = { selectedMember = member; expanded = false })
                            }
                        }
                    }
                }
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount") }, prefix = { Text("£") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
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
