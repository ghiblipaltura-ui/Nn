package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
  primary = NavyPrimary,
  onPrimary = PureWhite,
  primaryContainer = NavyLightContainer,
  onPrimaryContainer = NavyPrimary,
  secondary = EmeraldAccent,
  onSecondary = PureWhite,
  secondaryContainer = EmeraldContainer,
  onSecondaryContainer = EmeraldDarkContainer,
  tertiary = GoldAccent,
  onTertiary = NavyDeep,
  tertiaryContainer = GoldContainer,
  onTertiaryContainer = NavyDeep,
  background = SoftCanvas,
  onBackground = NavyDeep,
  surface = CardSurface,
  onSurface = NavyDeep,
  surfaceVariant = NavyLightContainer,
  onSurfaceVariant = SlateText,
  outline = SlateBorder,
  error = ErrorRed,
  onError = PureWhite,
  errorContainer = ErrorContainerLight,
  onErrorContainer = ErrorRed,
)

private val DarkColors = darkColorScheme(
  primary = EmeraldAccent,
  onPrimary = NavyDeep,
  primaryContainer = NavyContainer,
  onPrimaryContainer = PureWhite,
  secondary = EmeraldAccent,
  onSecondary = NavyDeep,
  secondaryContainer = EmeraldDarkContainer,
  onSecondaryContainer = EmeraldContainer,
  tertiary = GoldAccent,
  onTertiary = NavyDeep,
  background = DarkCanvas,
  onBackground = PureWhite,
  surface = DarkSurface,
  onSurface = PureWhite,
  surfaceVariant = DarkCard,
  onSurfaceVariant = SlateBorder,
  outline = DarkBorder,
  error = ErrorRed,
  onError = PureWhite,
)

val EarnMoreShapes = Shapes(
  small = RoundedCornerShape(10.dp),
  medium = RoundedCornerShape(16.dp),
  large = RoundedCornerShape(24.dp),
)

@Composable
fun EarnMoreTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColors else LightColors
  MaterialTheme(
    colorScheme = colorScheme,
    typography = EarnMoreTypography,
    shapes = EarnMoreShapes,
    content = content,
  )
}
