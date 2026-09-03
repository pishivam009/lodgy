package com.lodgy.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Red/Amber/Green, the convention wardens already read off any dashboard outside this app.
 *  NEUTRAL is the deliberate fourth: a state that is neither good nor bad (a vacated tenant). */
enum class StatusLevel { GOOD, WARN, BAD, NEUTRAL }

@Immutable
data class StatusPalette(
    /** For text/icons drawn straight onto a surface. */
    val accent: Color,
    val container: Color,
    val onContainer: Color,
)

@Immutable
data class StatusColors(
    val good: StatusPalette,
    val warn: StatusPalette,
    val bad: StatusPalette,
    val neutral: StatusPalette,
) {
    operator fun get(level: StatusLevel): StatusPalette = when (level) {
        StatusLevel.GOOD -> good
        StatusLevel.WARN -> warn
        StatusLevel.BAD -> bad
        StatusLevel.NEUTRAL -> neutral
    }
}

/** Green is pulled towards teal and amber towards orange so the pair stays separable under
 *  red-green colour-vision deficiency; colour is never the only cue anyway (see LODGY-62). */
val LightStatusColors = StatusColors(
    good = StatusPalette(accent = Color(0xFF15704A), container = Color(0xFFD3EFE0), onContainer = Color(0xFF06331F)),
    warn = StatusPalette(accent = Color(0xFF8A5200), container = Color(0xFFFBE7C6), onContainer = Color(0xFF442700)),
    bad = StatusPalette(accent = Color(0xFFB3261E), container = Color(0xFFF9DEDC), onContainer = Color(0xFF410E0B)),
    neutral = StatusPalette(accent = Color(0xFF5A5F66), container = Color(0xFFE3E5E8), onContainer = Color(0xFF2A2D31)),
)

val DarkStatusColors = StatusColors(
    good = StatusPalette(accent = Color(0xFF6FD7A6), container = Color(0xFF0E4230), onContainer = Color(0xFFB8EFD4)),
    warn = StatusPalette(accent = Color(0xFFF0BE63), container = Color(0xFF4A3206), onContainer = Color(0xFFFBE0AE)),
    bad = StatusPalette(accent = Color(0xFFF2B8B5), container = Color(0xFF601410), onContainer = Color(0xFFF9DEDC)),
    neutral = StatusPalette(accent = Color(0xFFB6BAC0), container = Color(0xFF34383D), onContainer = Color(0xFFE0E2E6)),
)

val LocalStatusColors = staticCompositionLocalOf { LightStatusColors }

/** Companion-style accessor so call sites read `LodgyTheme.statusColors[level]`. */
object LodgyStatus {
    val colors: StatusColors
        @Composable @ReadOnlyComposable get() = LocalStatusColors.current
}
