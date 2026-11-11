package com.sintelis.timemark.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sintelis.timemark.R
import com.sintelis.timemark.domain.LocationState
import com.sintelis.timemark.domain.ScheduleCalculator
import com.sintelis.timemark.domain.WatermarkConfig
import java.time.LocalDateTime

private val RobotoCondensedFamily = FontFamily(
    Font(R.font.roboto_condensed_regular, FontWeight.Normal),
    Font(R.font.roboto_condensed_medium, FontWeight.Medium),
    Font(R.font.roboto_condensed_bold, FontWeight.Bold)
)

@Composable
fun WatermarkPreviewOverlay(
    presetDateTime: LocalDateTime,
    location: LocationState,
    config: WatermarkConfig,
    slotBadgeText: String? = null,
    includeTime: Boolean = true,
    onClickDate: (() -> Unit)? = null,
    onClickLocation: (() -> Unit)? = null,
    onClickBrand: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val dropShadow = Shadow(
        color = Color.Black.copy(alpha = 0.85f),
        offset = Offset(0f, 2f),
        blurRadius = 6f
    )

    Box(modifier = modifier.fillMaxSize().padding(12.dp)) {
        // Top-left GPS pill
        if (config.showGpsPill) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .then(if (onClickLocation != null) Modifier.clickable(onClick = onClickLocation) else Modifier)
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = location.formattedCoordinates(),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontFamily = RobotoCondensedFamily
                )
            }
        }

        // Top-right slot badge (preview-only indicator, not stamped into final JPEG)
        if (!slotBadgeText.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(6.dp))
                    .background(TimemarkOrange.copy(alpha = 0.9f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = slotBadgeText,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Bottom-left bar + date pill + 2-line address
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(end = 96.dp)
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(TimemarkOrange)
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .then(if (onClickDate != null) Modifier.clickable(onClick = onClickDate) else Modifier)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (includeTime) {
                            ScheduleCalculator.formatWatermarkDateTime(
                                presetDateTime,
                                config.dateFormatStyle
                            )
                        } else {
                            ScheduleCalculator.formatWatermarkDateOnly(
                                presetDateTime,
                                config.dateFormatStyle
                            )
                        },
                        color = Color.White,
                        fontSize = 12.sp,
                        fontFamily = RobotoCondensedFamily,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(
                    modifier = Modifier
                        .then(if (onClickLocation != null) Modifier.clickable(onClick = onClickLocation) else Modifier)
                ) {
                    if (location.addressLine1.isNotBlank()) {
                        Text(
                            text = location.addressLine1,
                            style = TextStyle(
                                color = Color.White,
                                fontSize = 11.sp,
                                fontFamily = RobotoCondensedFamily,
                                shadow = dropShadow
                            )
                        )
                    }
                    if (location.addressLine2.isNotBlank()) {
                        Text(
                            text = location.addressLine2,
                            style = TextStyle(
                                color = Color.White,
                                fontSize = 11.sp,
                                fontFamily = RobotoCondensedFamily,
                                shadow = dropShadow
                            )
                        )
                    }
                }
            }
        }

        // Bottom-right branding
        if (config.brandTopText.isNotBlank() || config.brandSubText.isNotBlank() || onClickBrand != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .then(if (onClickBrand != null) Modifier.clickable(onClick = onClickBrand) else Modifier),
                horizontalAlignment = Alignment.End
            ) {
                if (config.brandTopText.isBlank() && config.brandSubText.isBlank()) {
                    Text(
                        text = "+ Brand",
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 11.sp,
                        fontFamily = RobotoCondensedFamily,
                        style = TextStyle(shadow = dropShadow)
                    )
                }
                if (config.brandTopText.isNotBlank()) {
                    if (config.brandTopText.equals("Timemark", ignoreCase = true)) {
                        Text(
                            text = buildAnnotatedString {
                                withStyle(SpanStyle(color = TimemarkGold, fontWeight = FontWeight.Bold)) {
                                    append("Time")
                                }
                                withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.Bold)) {
                                    append("mark")
                                }
                            },
                            fontSize = 13.sp,
                            fontFamily = RobotoCondensedFamily,
                            style = TextStyle(shadow = dropShadow)
                        )
                    } else {
                        Text(
                            text = config.brandTopText,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontFamily = RobotoCondensedFamily,
                            fontSize = 13.sp,
                            style = TextStyle(shadow = dropShadow)
                        )
                    }
                }
                if (config.brandSubText.isNotBlank()) {
                    Text(
                        text = config.brandSubText,
                        color = Color.White.copy(alpha = 0.92f),
                        fontWeight = FontWeight.Medium,
                        fontFamily = RobotoCondensedFamily,
                        fontSize = 11.sp,
                        style = TextStyle(shadow = dropShadow)
                    )
                }
            }
        }
    }
}
