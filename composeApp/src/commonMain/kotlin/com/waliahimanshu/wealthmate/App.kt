package com.waliahimanshu.wealthmate

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

enum class Screen {
    DASHBOARD,
    SALARY,
    OUTGOINGS,
    SAVINGS,
    MORTGAGE
}

@Composable
fun WealthMateApp(
    initialData: FinanceData,
    onDataChanged: (FinanceData) -> Unit
) {
    var currentScreen by remember { mutableStateOf(Screen.DASHBOARD) }
    var financeData by remember { mutableStateOf(initialData) }

    LaunchedEffect(financeData) {
        onDataChanged(financeData)
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF4CAF50),
            secondary = Color(0xFF81C784),
            surface = Color(0xFF1E1E1E),
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
            Column(modifier = Modifier.fillMaxSize()) {
                // Navigation Tabs
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Screen.entries.forEach { screen ->
                        NavigationBarItem(
                            selected = currentScreen == screen,
                            onClick = { currentScreen = screen },
                            icon = {},
                            label = { Text(screen.name.replace("_", " ")) }
                        )
                    }
                }

                // Content
                Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    when (currentScreen) {
                        Screen.DASHBOARD -> DashboardScreen(financeData)
                        Screen.SALARY -> SalaryScreen(
                            salary = financeData.salary,
                            onSalaryChanged = { financeData = financeData.copy(salary = it) }
                        )
                        Screen.OUTGOINGS -> OutgoingsScreen(
                            outgoings = financeData.outgoings,
                            onOutgoingsChanged = { financeData = financeData.copy(outgoings = it) }
                        )
                        Screen.SAVINGS -> SavingsScreen(
                            savings = financeData.savings,
                            onSavingsChanged = { financeData = financeData.copy(savings = it) }
                        )
                        Screen.MORTGAGE -> MortgageScreen(
                            mortgage = financeData.mortgage,
                            onMortgageChanged = { financeData = financeData.copy(mortgage = it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardScreen(data: FinanceData) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(
                "WealthMate Dashboard",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
        }

        item {
            SummaryCard(
                title = "Monthly Salary",
                value = formatCurrency(data.salary),
                color = Color(0xFF4CAF50)
            )
        }

        item {
            SummaryCard(
                title = "Total Outgoings",
                value = formatCurrency(data.totalOutgoings),
                color = Color(0xFFF44336)
            )
        }

        item {
            SummaryCard(
                title = "Mortgage Payment",
                value = formatCurrency(data.mortgage?.monthlyPayment ?: 0.0),
                color = Color(0xFFFF9800)
            )
        }

        item {
            SummaryCard(
                title = "Net Monthly",
                value = formatCurrency(data.netMonthly),
                color = if (data.netMonthly >= 0) Color(0xFF2196F3) else Color(0xFFF44336)
            )
        }

        item {
            Spacer(Modifier.height(8.dp))
            Text(
                "Savings Overview",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            SummaryCard(
                title = "Total Savings",
                value = formatCurrency(data.totalSavings),
                color = Color(0xFF9C27B0)
            )
        }

        if (data.savings.isNotEmpty()) {
            items(data.savings) { account ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(account.name, fontWeight = FontWeight.Bold)
                            Text(
                                "${account.provider} - ${account.accountType.name.replace("_", " ")}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                formatCurrency(account.balance),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "${account.interestRate}% AER",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
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
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun SalaryScreen(salary: Double, onSalaryChanged: (Double) -> Unit) {
    var salaryText by remember(salary) { mutableStateOf(if (salary == 0.0) "" else salary.toString()) }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Monthly Salary",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = salaryText,
            onValueChange = {
                salaryText = it
                it.toDoubleOrNull()?.let { value -> onSalaryChanged(value) }
            },
            label = { Text("Net Monthly Salary (GBP)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            prefix = { Text("£") }
        )

        Text(
            "Enter your take-home pay after tax and deductions",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}

@Composable
fun OutgoingsScreen(outgoings: List<Outgoing>, onOutgoingsChanged: (List<Outgoing>) -> Unit) {
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Monthly Outgoings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Button(onClick = { showAddDialog = true }) {
                Text("Add")
            }
        }

        Text(
            "Total: ${formatCurrency(outgoings.sumOf { it.amount })}",
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFFF44336)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(outgoings) { outgoing ->
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
                            Text(
                                outgoing.category.name.replace("_", " "),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                formatCurrency(outgoing.amount),
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF44336)
                            )
                            TextButton(onClick = {
                                onOutgoingsChanged(outgoings.filter { it.id != outgoing.id })
                            }) {
                                Text("X", color = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddOutgoingDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { outgoing ->
                onOutgoingsChanged(outgoings + outgoing)
                showAddDialog = false
            }
        )
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
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    prefix = { Text("£") },
                    modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = category.name.replace("_", " "),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        OutgoingCategory.entries.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name.replace("_", " ")) },
                                onClick = {
                                    category = cat
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountValue = amount.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank() && amountValue > 0) {
                        onAdd(Outgoing(
                            id = kotlin.random.Random.nextLong().toString(),
                            name = name,
                            amount = amountValue,
                            category = category
                        ))
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun SavingsScreen(savings: List<SavingsAccount>, onSavingsChanged: (List<SavingsAccount>) -> Unit) {
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "UK Savings Accounts",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Button(onClick = { showAddDialog = true }) {
                Text("Add")
            }
        }

        Text(
            "Total: ${formatCurrency(savings.sumOf { it.balance })}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(savings) { account ->
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
                            Text(
                                "${account.provider} - ${account.accountType.name.replace("_", " ")}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                            Text(
                                "${account.interestRate}% AER",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF4CAF50)
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                formatCurrency(account.balance),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            TextButton(onClick = {
                                onSavingsChanged(savings.filter { it.id != account.id })
                            }) {
                                Text("X", color = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddSavingsDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { account ->
                onSavingsChanged(savings + account)
                showAddDialog = false
            }
        )
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
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Account Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = provider,
                    onValueChange = { provider = it },
                    label = { Text("Provider (e.g., Monzo, Chase)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = balance,
                    onValueChange = { balance = it },
                    label = { Text("Balance") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    prefix = { Text("£") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = interestRate,
                    onValueChange = { interestRate = it },
                    label = { Text("Interest Rate (AER %)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    suffix = { Text("%") },
                    modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = accountType.name.replace("_", " "),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Account Type") },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        UKAccountType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.name.replace("_", " ")) },
                                onClick = {
                                    accountType = type
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val balanceValue = balance.toDoubleOrNull() ?: 0.0
                    val rateValue = interestRate.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank() && provider.isNotBlank()) {
                        onAdd(SavingsAccount(
                            id = kotlin.random.Random.nextLong().toString(),
                            name = name,
                            provider = provider,
                            balance = balanceValue,
                            interestRate = rateValue,
                            accountType = accountType
                        ))
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun MortgageScreen(mortgage: MortgageInfo?, onMortgageChanged: (MortgageInfo?) -> Unit) {
    var provider by remember(mortgage) { mutableStateOf(mortgage?.provider ?: "") }
    var balance by remember(mortgage) { mutableStateOf(mortgage?.remainingBalance?.toString() ?: "") }
    var payment by remember(mortgage) { mutableStateOf(mortgage?.monthlyPayment?.toString() ?: "") }
    var rate by remember(mortgage) { mutableStateOf(mortgage?.interestRate?.toString() ?: "") }
    var term by remember(mortgage) { mutableStateOf(mortgage?.termRemainingMonths?.toString() ?: "") }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Mortgage Details",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = provider,
            onValueChange = { provider = it },
            label = { Text("Lender") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = balance,
            onValueChange = { balance = it },
            label = { Text("Remaining Balance") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            prefix = { Text("£") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = payment,
            onValueChange = { payment = it },
            label = { Text("Monthly Payment") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            prefix = { Text("£") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = rate,
            onValueChange = { rate = it },
            label = { Text("Interest Rate") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            suffix = { Text("%") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = term,
            onValueChange = { term = it },
            label = { Text("Remaining Term (months)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    val balanceValue = balance.toDoubleOrNull() ?: 0.0
                    val paymentValue = payment.toDoubleOrNull() ?: 0.0
                    val rateValue = rate.toDoubleOrNull() ?: 0.0
                    val termValue = term.toIntOrNull() ?: 0
                    if (provider.isNotBlank()) {
                        onMortgageChanged(MortgageInfo(
                            provider = provider,
                            remainingBalance = balanceValue,
                            monthlyPayment = paymentValue,
                            interestRate = rateValue,
                            termRemainingMonths = termValue
                        ))
                    }
                }
            ) {
                Text("Save")
            }

            if (mortgage != null) {
                TextButton(onClick = { onMortgageChanged(null) }) {
                    Text("Clear", color = Color.Red)
                }
            }
        }
    }
}

fun formatCurrency(amount: Double): String {
    val wholePart = amount.toLong()
    val decimalPart = ((amount - wholePart) * 100).roundToInt()
    val decimalStr = decimalPart.toString().padStart(2, '0')
    return "£$wholePart.$decimalStr"
}
