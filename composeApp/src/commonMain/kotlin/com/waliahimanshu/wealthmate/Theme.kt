package com.waliahimanshu.wealthmate

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.materialkolor.DynamicMaterialTheme
import com.materialkolor.PaletteStyle

val SeedColor = Color(0xFF006C4C)

@Composable
fun WealthMateTheme(
    isDark: Boolean,
    content: @Composable () -> Unit
) {
    DynamicMaterialTheme(
        seedColor = SeedColor,
        useDarkTheme = isDark,
        style = PaletteStyle.Expressive,
        animate = true,
        content = content
    )
}
