package de.protech.termine.look

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/** Helligkeit der App, unabhängig vom System einstellbar. */
enum class ThemeMode(val label: String) {
    SYSTEM("Wie das System"),
    LIGHT("Immer hell"),
    DARK("Immer dunkel")
}

/** Kachelform: Beschriftung und Eckenradius in dp. */
enum class CardShape(val label: String, val cornerDp: Int) {
    ANGULAR("Kantig", 8),
    SOFT("Weich", 20),
    ROUND("Rund", 34)
}

data class Appearance(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accent: Color = Accents.DEFAULT,
    val useSystemColors: Boolean = false,
    val shape: CardShape = CardShape.SOFT,
    /** Deckkraft der Kacheln in Prozent, 55 bis 100. */
    val opacityPercent: Int = 100,
    val pattern: Pattern = Pattern.NONE,
    /** Stärke des Musters in Prozent, 20 bis 100. */
    val patternStrength: Int = 60
) {
    val cardAlpha: Float get() = opacityPercent.coerceIn(55, 100) / 100f
    val translucent: Boolean get() = opacityPercent < 100
}

/** Auswahl der Akzentfarben. Namen wie bei „Unsere Tage“. */
object Accents {
    val DEFAULT = Color(0xFF0A6B5B)

    val ALL: List<Pair<String, Color>> = listOf(
        "Petrol" to Color(0xFF0A6B5B),
        "Salbei" to Color(0xFF4F8B63),
        "Enzian" to Color(0xFF3D6DB5),
        "Himmel" to Color(0xFF2E7D8A),
        "Flieder" to Color(0xFF7B52AB),
        "Rosé" to Color(0xFFC14D6B),
        "Mohn" to Color(0xFFC0392B),
        "Kupfer" to Color(0xFFC2662C),
        "Gold" to Color(0xFFBE912F),
        "Graphit" to Color(0xFF4A5A62)
    )

    fun nameOf(color: Color): String =
        ALL.firstOrNull { it.second.value == color.value }?.first ?: "Eigene"
}

/**
 * Hält die Darstellung und schreibt jede Änderung sofort mit, damit sie einen
 * Neustart übersteht. Bewusst mit SharedPreferences – für fünf Werte braucht
 * es keine Datenbank.
 */
class AppearanceStore(context: Context) {

    private val prefs = context.getSharedPreferences("darstellung", Context.MODE_PRIVATE)

    var value: Appearance by mutableStateOf(load())
        private set

    private fun load(): Appearance {
        val accentArgb = prefs.getInt(KEY_ACCENT, Accents.DEFAULT.toArgb())
        return Appearance(
            themeMode = runCatching {
                ThemeMode.valueOf(prefs.getString(KEY_THEME, ThemeMode.SYSTEM.name)!!)
            }.getOrDefault(ThemeMode.SYSTEM),
            accent = Color(accentArgb),
            useSystemColors = prefs.getBoolean(KEY_SYSTEM_COLORS, false),
            shape = runCatching {
                CardShape.valueOf(prefs.getString(KEY_SHAPE, CardShape.SOFT.name)!!)
            }.getOrDefault(CardShape.SOFT),
            opacityPercent = prefs.getInt(KEY_OPACITY, 100),
            pattern = runCatching {
                Pattern.valueOf(prefs.getString(KEY_PATTERN, Pattern.NONE.name)!!)
            }.getOrDefault(Pattern.NONE),
            patternStrength = prefs.getInt(KEY_PATTERN_STRENGTH, 60)
        )
    }

    private fun update(block: (Appearance) -> Appearance) {
        val next = block(value)
        value = next
        prefs.edit()
            .putString(KEY_THEME, next.themeMode.name)
            .putInt(KEY_ACCENT, next.accent.toArgb())
            .putBoolean(KEY_SYSTEM_COLORS, next.useSystemColors)
            .putString(KEY_SHAPE, next.shape.name)
            .putInt(KEY_OPACITY, next.opacityPercent)
            .putString(KEY_PATTERN, next.pattern.name)
            .putInt(KEY_PATTERN_STRENGTH, next.patternStrength)
            .apply()
    }

    fun setThemeMode(mode: ThemeMode) = update { it.copy(themeMode = mode) }
    fun setAccent(color: Color) = update { it.copy(accent = color, useSystemColors = false) }
    fun setUseSystemColors(enabled: Boolean) = update { it.copy(useSystemColors = enabled) }
    fun setShape(shape: CardShape) = update { it.copy(shape = shape) }
    fun setOpacity(percent: Int) = update { it.copy(opacityPercent = percent.coerceIn(55, 100)) }
    fun setPattern(pattern: Pattern) = update { it.copy(pattern = pattern) }
    fun setPatternStrength(percent: Int) =
        update { it.copy(patternStrength = percent.coerceIn(20, 100)) }

    fun reset() = update {
        Appearance()
    }

    private companion object {
        const val KEY_THEME = "helligkeit"
        const val KEY_ACCENT = "akzent"
        const val KEY_SYSTEM_COLORS = "systemfarben"
        const val KEY_SHAPE = "kachelform"
        const val KEY_OPACITY = "deckkraft"
        const val KEY_PATTERN = "muster"
        const val KEY_PATTERN_STRENGTH = "musterstaerke"
    }
}

val LocalAppearance = staticCompositionLocalOf { Appearance() }
