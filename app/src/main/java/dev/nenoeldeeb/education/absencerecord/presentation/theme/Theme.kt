package dev.nenoeldeeb.education.absencerecord.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
    darkColorScheme(
        // Primary Colors
        primary = blue50,
        onPrimary = blue95,
        primaryContainer = blue30,
        onPrimaryContainer = blue90,
        inversePrimary = blue80,
        // Secondary Colors
        secondary = purple50,
        onSecondary = purple95,
        secondaryContainer = purple30,
        onSecondaryContainer = purple90,
        // Tertiary Colors
        tertiary = green30,
        onTertiary = green90,
        tertiaryContainer = green30,
        onTertiaryContainer = green90,
        // Background/Surface Colors
        background = black,
        onBackground = gray90,
        surface = gray10,
        onSurface = gray90,
        surfaceVariant = gray20,
        onSurfaceVariant = gray80,
        // Elevation Tonal Surfaces
        surfaceBright = gray30,
        surfaceDim = gray5,
        surfaceContainer = gray15,
        surfaceContainerHigh = gray20,
        surfaceContainerHighest = gray25,
        surfaceContainerLow = gray10,
        surfaceContainerLowest = gray5,
        // Error Colors
        error = red50,
        onError = red95,
        errorContainer = red30,
        onErrorContainer = red90,
        // Outline/Scrim
        outline = gray60,
        outlineVariant = gray30,
        scrim = black,
        // Dynamic Tints
        surfaceTint = blue50,
        inverseSurface = gray80,
        inverseOnSurface = gray10
    )

private val LightColorScheme: ColorScheme
    get() =
        lightColorScheme(
            // Primary Colors
            primary = blue50,
            onPrimary = blue10,
            primaryContainer = blue70,
            onPrimaryContainer = blue10,
            inversePrimary = blue20,
            // Secondary Colors
            secondary = purple50,
            onSecondary = purple95,
            secondaryContainer = purple70,
            onSecondaryContainer = purple10,
            // Tertiary Colors
            tertiary = green50,
            onTertiary = green95,
            tertiaryContainer = green70,
            onTertiaryContainer = green10,
            // Background/Surface Colors
            background = white,
            onBackground = gray10,
            surface = gray95,
            onSurface = gray10,
            surfaceVariant = gray90,
            onSurfaceVariant = gray10,
            // Elevation Tonal Surfaces
            surfaceBright = white,
            surfaceDim = gray85,
            surfaceContainer = gray90,
            surfaceContainerHigh = white,
            surfaceContainerHighest = white,
            surfaceContainerLow = gray85,
            surfaceContainerLowest = gray80,
            // Error Colors
            error = red50,
            onError = red95,
            errorContainer = red70,
            onErrorContainer = red10,
            // Outline/Scrim
            outline = gray40,
            outlineVariant = gray80,
            scrim = black,
            // Dynamic Tints
            surfaceTint = blue50,
            inverseSurface = gray20,
            inverseOnSurface = gray95
        )

@Composable
fun AbsenceRecordTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val useDynamicColor = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val colorScheme =
        when {
            useDynamicColor && darkTheme -> dynamicDarkColorScheme(context)
            useDynamicColor && !darkTheme -> dynamicLightColorScheme(context)
            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = shapes,
        typography = Typography,
        content = content
    )
}