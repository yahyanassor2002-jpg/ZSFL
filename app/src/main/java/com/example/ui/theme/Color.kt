package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Rich Agricultural Dark Green & Dark Blue Operational Palette for UPENJAnet
val DarkGreenBg = Color(0xFF051910) // Deep Rich Dark Green background
val DarkBlueBg = Color(0xFF07162C) // Deep Rich Dark Blue background tone
val DarkGreenSurface = Color(0xFF0B2519) // Rich Dark Green card surface
val DarkBlueSurface = Color(0xFF0D223B) // Rich Dark Blue card surface
val DarkGreenBlueSurfaceVariant = Color(0xFF0E222A) // Deep dark green-blue blend

// Full app dark green to dark blue gradient background
val AppDarkBackgroundGradient = Brush.verticalGradient(
  colors = listOf(
    Color(0xFF051910), // Dark Green top
    Color(0xFF08222B), // Dark Green-Blue mid
    Color(0xFF07162C), // Dark Blue bottom
  )
)

val CaneGreenPrimary = Color(0xFF2E8B57) // Bright Forest Cane Green
val CaneDarkGreenContainer = Color(0xFF133C26) // Deep Dark Green container
val CaneDarkBlueContainer = Color(0xFF0F2744) // Deep Dark Blue container

val CaneBlueSecondary = Color(0xFF2D6DA3) // Vibrant Dark Blue secondary
val CaneTealTertiary = Color(0xFF1B4E54) // Deep Teal Blue-Green

val CaneHarvestGold = Color(0xFFD48B00)
val CaneEarthBrown = Color(0xFF2E3B32)

// Text Colors
val TextOnDarkPrimary = Color(0xFFF2F7F4)
val TextOnDarkSecondary = Color(0xFFB5CCBF)
val TextOnDarkBlue = Color(0xFFD4E7FA)

// Status colors
val StatusDraft = Color(0xFF607D8B)
val StatusDraftBg = Color(0xFF152228)
val StatusPending = Color(0xFFE65100)
val StatusPendingBg = Color(0xFF2E1A08)
val StatusSynced = Color(0xFF2E8B57)
val StatusSyncedBg = Color(0xFF0E2E1D)
val StatusSubmitted = Color(0xFF227B48)
val StatusSubmittedBg = Color(0xFF0E281A)

