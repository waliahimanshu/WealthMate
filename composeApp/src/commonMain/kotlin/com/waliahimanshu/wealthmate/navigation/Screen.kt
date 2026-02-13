package com.waliahimanshu.wealthmate.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.ui.graphics.vector.ImageVector

enum class Screen(
    val displayName: String,
    val icon: ImageVector
) {
    DASHBOARD("Dashboard", Icons.Outlined.Home),
    INCOME("Income", Icons.Outlined.TrendingUp),
    EXPENSES("Expenses", Icons.Outlined.Receipt),
    SAVINGS_INVESTMENTS("Savings & Investments", Icons.Outlined.AccountBalance),
    GOALS("Goals", Icons.Outlined.Flag),
    SETTINGS("Settings", Icons.Outlined.Settings)
}
