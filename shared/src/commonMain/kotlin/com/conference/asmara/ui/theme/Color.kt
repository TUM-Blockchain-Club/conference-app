package com.conference.asmara.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Only the roles that actually show on these two screens are overridden; the
// rest are filled in by lightColorScheme()/darkColorScheme() defaults.
private val Indigo = Color(0xFF4F46E5)
private val IndigoLight = Color(0xFFA5B4FC)
private val Teal = Color(0xFF0D9488)
private val TealLight = Color(0xFF5EEAD4)

internal val LightColors = lightColorScheme(
    primary = Indigo,
    onPrimary = Color.White,
    secondary = Teal,
    onSecondary = Color.White,
    background = Color(0xFFFBFBFE),
    onBackground = Color(0xFF1A1B22),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1B22),
    surfaceVariant = Color(0xFFECECF3),
    onSurfaceVariant = Color(0xFF494A54),
)

internal val DarkColors = darkColorScheme(
    primary = IndigoLight,
    onPrimary = Color(0xFF1E1B4B),
    secondary = TealLight,
    onSecondary = Color(0xFF00312B),
    background = Color(0xFF121319),
    onBackground = Color(0xFFE5E5EC),
    surface = Color(0xFF1A1B22),
    onSurface = Color(0xFFE5E5EC),
    surfaceVariant = Color(0xFF2C2D36),
    onSurfaceVariant = Color(0xFFC6C6D0),
)
