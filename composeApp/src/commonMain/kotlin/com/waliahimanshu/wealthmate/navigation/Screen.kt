package com.waliahimanshu.wealthmate.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Cottage
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.ui.graphics.vector.ImageVector

enum class Screen(
    val displayName: String,
    val icon: ImageVector,
    val showInMobileBottomBar: Boolean = true,
    val mobileDisplayName: String = displayName
) {
    DASHBOARD("Dashboard", Icons.Outlined.Home),
    INCOME("Income", Icons.Outlined.TrendingUp),
    EXPENSES("Expenses", Icons.Outlined.Receipt),
    SAVINGS_INVESTMENTS("Savings & Investments", Icons.Outlined.AccountBalance, mobileDisplayName = "Savings"),
    PROPERTY("Property", Icons.Outlined.Cottage, showInMobileBottomBar = false),
    GOALS("Goals", Icons.Outlined.Flag, showInMobileBottomBar = false),
    SETTINGS("Settings", Icons.Outlined.Settings, showInMobileBottomBar = false);

    companion object {
        val mobileBottomBarScreens = entries.filter { it.showInMobileBottomBar }
        val mobileDrawerScreens = entries.filter { !it.showInMobileBottomBar }
    }
}
