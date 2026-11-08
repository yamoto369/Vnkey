package app.vikey.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** The brand (design/BRAND.md): one accent color, indigo #4F46E5 (#8C87FF in dark mode). */
val BrandLight = Color(0xFF4F46E5)
val BrandDark = Color(0xFF8C87FF)
val brand: Color @Composable get() = if (isSystemInDarkTheme()) BrandDark else BrandLight

@Composable
fun VikeyTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val scheme = if (dark) darkColorScheme(primary = BrandDark, secondary = BrandDark, surface = Color(0xFF1C1C1E), background = Color.Black, surfaceVariant = Color(0xFF2C2C2E))
        else lightColorScheme(primary = BrandLight, secondary = BrandLight, surface = Color.White, background = Color(0xFFF2F2F7), surfaceVariant = Color.White)
    // plain Text (row titles, onboarding titles) takes the theme's ink, not the default black that vanished in dark mode
    MaterialTheme(colorScheme = scheme) {
        androidx.compose.runtime.CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides scheme.onBackground, content = content)
    }
}
