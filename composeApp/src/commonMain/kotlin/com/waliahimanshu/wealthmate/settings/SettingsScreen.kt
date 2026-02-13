package com.waliahimanshu.wealthmate.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.waliahimanshu.wealthmate.HouseholdFinances
import com.waliahimanshu.wealthmate.HouseholdMember
import com.waliahimanshu.wealthmate.components.parseColor
import com.waliahimanshu.wealthmate.storage.SyncStatus

@Composable
fun SettingsScreen(
    data: HouseholdFinances,
    syncStatus: SyncStatus,
    currentToken: String?,
    onSaveToken: (String) -> Unit,
    onClearToken: () -> Unit,
    onSync: () -> Unit,
    onForceRefresh: () -> Unit,
    onAddMember: (HouseholdMember) -> Unit,
    onUpdateMember: (HouseholdMember) -> Unit,
    onDeleteMember: (String) -> Unit
) {
    var tokenInput by remember { mutableStateOf("") }
    var showToken by remember { mutableStateOf(false) }
    val hasToken = !currentToken.isNullOrBlank()
    var showAddMemberDialog by remember { mutableStateOf(false) }
    var editingMember by remember { mutableStateOf<HouseholdMember?>(null) }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Manage your household and sync settings", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Household Members Section
        item {
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
                        Text("Household Members", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Button(onClick = { showAddMemberDialog = true }) {
                            Text("Add Member")
                        }
                    }
                    Spacer(Modifier.height(12.dp))

                    if (data.members.isEmpty()) {
                        Text(
                            "No members yet. Add yourself and your partner to get started.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        data.members.forEach { member ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(36.dp).clip(CircleShape).background(parseColor(member.color)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(member.name.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(member.name, fontWeight = FontWeight.SemiBold)
                                    Text("£${member.salary.toLong()}/mo salary", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { editingMember = member }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { onDeleteMember(member.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                                }
                            }
                            if (member != data.members.last()) {
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
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

        // About Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("About WealthMate", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("Version 3.0", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Your data is stored locally and synced to a private GitHub Gist.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    if (showAddMemberDialog) {
        AddMemberDialog(
            onDismiss = { showAddMemberDialog = false },
            onAdd = {
                onAddMember(it)
                showAddMemberDialog = false
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
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = salary, onValueChange = { salary = it }, label = { Text("Monthly Salary (Net)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), prefix = { Text("£") }, modifier = Modifier.fillMaxWidth())
                Text("Color", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    colors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(parseColor(color))
                                .clickable { selectedColor = color }
                                .then(if (selectedColor == color) Modifier.background(Color.White.copy(alpha = 0.3f)) else Modifier)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isNotBlank()) {
                    onAdd(HouseholdMember(name = name, salary = salary.toDoubleOrNull() ?: 0.0, color = selectedColor))
                }
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
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
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = salary, onValueChange = { salary = it }, label = { Text("Monthly Salary (Net)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), prefix = { Text("£") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(member.copy(name = name, salary = salary.toDoubleOrNull() ?: 0.0))
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
