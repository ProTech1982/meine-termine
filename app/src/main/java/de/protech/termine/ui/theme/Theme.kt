package de.protech.termine.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/*
 * Ruhiges Teal-Grün als Grundton: klar, ohne Krankenhaus-Blau.
 * Ab Android 12 uebernimmt die App auf Wunsch die Systemfarben.
 */

private val LightColors = lightColorScheme(
    primary = Color(0xFF0A6B5B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB4F1E1),
    onPrimaryContainer = Color(0xFF00201A),
    secondary = Color(0xFF4B635C),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCDE8DF),
    onSecondaryContainer = Color(0xFF07201A),
    tertiary = Color(0xFF42617A),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFC6E7FF),
    onTertiaryContainer = Color(0xFF001E31),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF5FBF7),
    onBackground = Color(0xFF171D1B),
    surface = Color(0xFFF5FBF7),
    onSurface = Color(0xFF171D1B),
    surfaceVariant = Color(0xFFDBE5E0),
    onSurfaceVariant = Color(0xFF3F4945),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEFF5F1),
    surfaceContainer = Color(0xFFE9EFEB),
    surfaceContainerHigh = Color(0xFFE3EAE6),
    surfaceContainerHighest = Color(0xFFDDE4E0),
    outline = Color(0xFF6F7975),
    outlineVariant = Color(0xFFBFC9C4)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8FD5C5),
    onPrimary = Color(0xFF00382F),
    primaryContainer = Color(0xFF005144),
    onPrimaryContainer = Color(0xFFB4F1E1),
    secondary = Color(0xFFB1CCC3),
    onSecondary = Color(0xFF1C352F),
    secondaryContainer = Color(0xFF334B45),
    onSecondaryContainer = Color(0xFFCDE8DF),
    tertiary = Color(0xFFAACBE6),
    onTertiary = Color(0xFF0C3448),
    tertiaryContainer = Color(0xFF284A61),
    onTertiaryContainer = Color(0xFFC6E7FF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0E1513),
    onBackground = Color(0xFFDDE4E0),
    surface = Color(0xFF0E1513),
    onSurface = Color(0xFFDDE4E0),
    surfaceVariant = Color(0xFF3F4945),
    onSurfaceVariant = Color(0xFFBFC9C4),
    surfaceContainerLowest = Color(0xFF090F0E),
    surfaceContainerLow = Color(0xFF161D1B),
    surfaceContainer = Color(0xFF1A2220),
    surfaceContainerHigh = Color(0xFF242C2A),
    surfaceContainerHighest = Color(0xFF2F3734),
    outline = Color(0xFF899390),
    outlineVariant = Color(0xFF3F4945)
)

/**
 * Etwas groessere Schriftgrade als der Standard – die Liste soll sich mit
 * einem Blick lesen lassen, auch im Vorbeigehen.
 */
private val AppTypography = Typography().let { base ->
    base.copy(
        displaySmall = base.displaySmall.copy(fontWeight = FontWeight.SemiBold),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(
            fontSize = 17.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.SemiBold
        ),
        bodyLarge = base.bodyLarge.copy(fontSize = 17.sp, lineHeight = 25.sp),
        bodyMedium = base.bodyMedium.copy(fontSize = 15.sp, lineHeight = 21.sp),
        labelLarge = base.labelLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    )
}

@Composable
fun MeineTermineTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
