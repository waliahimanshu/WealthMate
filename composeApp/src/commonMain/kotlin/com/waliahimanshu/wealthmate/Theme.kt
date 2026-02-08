package com.waliahimanshu.wealthmate

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.materialkolor.DynamicMaterialTheme
import com.materialkolor.PaletteStyle

// ============================================================
// THEME OPTIONS - Uncomment ONE seed color + style combo to try
// ============================================================

// Option 1: Teal/Emerald Expressive (default) - Rich green, wealth & growth
//val SeedColor = Color(0xFF006C4C)
//val ThemeStyle = PaletteStyle.Expressive

// Option 2: Teal/Emerald TonalSpot - Calmer, more subtle green
// val SeedColor = Color(0xFF006C4C)
// val ThemeStyle = PaletteStyle.TonalSpot

// Option 3: Teal/Emerald Fidelity - Keeps the green very close to seed
// val SeedColor = Color(0xFF006C4C)
// val ThemeStyle = PaletteStyle.Fidelity

 //Option 4: Deep Sapphire Expressive - Banking blue, professional trust
 val SeedColor = Color(0xFF1A5FB4)
 val ThemeStyle = PaletteStyle.Expressive

// Option 5: Deep Sapphire TonalSpot - Classic banking app feel
// val SeedColor = Color(0xFF1A5FB4)
// val ThemeStyle = PaletteStyle.TonalSpot

// Option 6: Violet/Purple Expressive - Modern fintech (Nubank/Plum style)
// val SeedColor = Color(0xFF6750A4)
// val ThemeStyle = PaletteStyle.Expressive

// Option 7: Violet/Purple Vibrant - Bold & colorful fintech
// val SeedColor = Color(0xFF6750A4)
// val ThemeStyle = PaletteStyle.Vibrant

// Option 8: Gold/Amber Expressive - Premium wealth, luxury feel
// val SeedColor = Color(0xFF8B6914)
// val ThemeStyle = PaletteStyle.Expressive

// Option 9: Mint Fidelity - Fresh, clean, modern
// val SeedColor = Color(0xFF00BFA5)
// val ThemeStyle = PaletteStyle.Fidelity

// Option 10: Ocean Teal Vibrant - Bold & vibrant sea green
// val SeedColor = Color(0xFF00796B)
// val ThemeStyle = PaletteStyle.Vibrant

@Composable
fun WealthMateTheme(
    isDark: Boolean,
    content: @Composable () -> Unit
) {
    DynamicMaterialTheme(
        seedColor = SeedColor,
        useDarkTheme = isDark,
        style = ThemeStyle,
        animate = true,
        content = content
    )
}
