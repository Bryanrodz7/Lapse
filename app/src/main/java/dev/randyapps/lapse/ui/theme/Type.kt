package dev.randyapps.lapse.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import dev.randyapps.lapse.R

/**
 * Instrument Serif, bundled locally (SIL OFL, see assets/licenses). Used only for item names
 * and the days-remaining number — the two things the eye should land on first.
 */
val InstrumentSerif = FontFamily(
    Font(R.font.instrument_serif_regular, FontWeight.Normal),
    Font(R.font.instrument_serif_italic, FontWeight.Normal, FontStyle.Italic),
)

/**
 * The system sans carries everything else. The spec allows Inter or the system default; the
 * system font is the better choice at minSdk 24 because Inter now ships only as a variable
 * font, which Android can't render below API 26.
 */
val LapseSans = FontFamily.SansSerif

/**
 * Trimming the first/last line gap keeps the big serif numerals optically centred against the
 * sans metadata beside them. Without it the number floats high in its own box.
 */
private val TrimmedLineHeight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.Both,
)

/** The hero: large enough to read at arm's length without looking at anything else. */
val DaysNumberStyle = TextStyle(
    fontFamily = InstrumentSerif,
    fontWeight = FontWeight.Normal,
    fontSize = 46.sp,
    lineHeight = 48.sp,
    letterSpacing = (-0.5).sp,
    lineHeightStyle = TrimmedLineHeight,
)

/**
 * Expired items get a smaller number as well as less contrast, so they sink below active rows
 * by weight and not only by colour.
 */
val ExpiredDaysNumberStyle = DaysNumberStyle.copy(
    fontSize = 32.sp,
    lineHeight = 34.sp,
)

val ItemNameStyle = TextStyle(
    fontFamily = InstrumentSerif,
    fontWeight = FontWeight.Normal,
    fontSize = 23.sp,
    lineHeight = 28.sp,
    letterSpacing = 0.sp,
)

/** Small, uppercase, wide-tracked, quiet — labels should recede, not announce. */
val OverlineStyle = TextStyle(
    fontFamily = LapseSans,
    fontWeight = FontWeight.Medium,
    fontSize = 11.sp,
    lineHeight = 14.sp,
    letterSpacing = 1.4.sp,
)

val MetaStyle = TextStyle(
    fontFamily = LapseSans,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
    lineHeight = 18.sp,
    letterSpacing = 0.1.sp,
)

/** The empty state's single serif line. */
val QuietDisplayStyle = TextStyle(
    fontFamily = InstrumentSerif,
    fontWeight = FontWeight.Normal,
    fontSize = 30.sp,
    lineHeight = 40.sp,
    letterSpacing = 0.sp,
)

val LapseTypography = Typography(
    displayLarge = DaysNumberStyle,
    displayMedium = QuietDisplayStyle,
    headlineMedium = ItemNameStyle,
    titleLarge = TextStyle(
        fontFamily = InstrumentSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 26.sp,
        lineHeight = 32.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = LapseSans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = MetaStyle,
    labelSmall = OverlineStyle,
    labelMedium = TextStyle(
        fontFamily = LapseSans,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.2.sp,
    ),
)
