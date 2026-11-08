package org.exxlexxlee.svitlo.ui.dial

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.exxlexxlee.domain.model.HourState
import org.exxlexxlee.domain.model.PowerState
import org.exxlexxlee.svitlo.ui.theme.SvitloTheme
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun OutageDial(
    hours: List<HourState>,
    hourOffset: Int,
    modifier: Modifier = Modifier,
    isToday: Boolean = false,
    currentHour: Int = -1,
) {
    val onColor = MaterialTheme.colorScheme.primaryContainer
    val offColor = MaterialTheme.colorScheme.onSurface
    val maybeFill = MaterialTheme.colorScheme.primaryContainer
    val maybeStroke = MaterialTheme.colorScheme.onSurface
    val unknownColor = MaterialTheme.colorScheme.outline
    val labelColor = MaterialTheme.colorScheme.onSurface
    val currentStroke = MaterialTheme.colorScheme.tertiary
    val faceColor = MaterialTheme.colorScheme.surface
    val description = buildDialDescription(hours, hourOffset)
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(
        color = labelColor,
        fontSize = 12.sp,
    )

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .semantics(mergeDescendants = true) { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            val diameter = min(size.width, size.height)
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)
            hours.forEachIndexed { hourIndex, hour ->
                val hourOfDay = hourOffset + hourIndex
                val past = isToday && currentHour >= 0 && hourOfDay < currentHour
                val current = isToday && hourOfDay == currentHour
                val dim = if (past) 0.45f else 1f
                listOf(hour.first, hour.second).forEachIndexed { half, state ->
                    val startAngle = -90f + hourIndex * 30f + half * 15f
                    val fill = when (state) {
                        PowerState.ON -> onColor
                        PowerState.OFF -> offColor
                        PowerState.MAYBE -> maybeFill
                        PowerState.UNKNOWN -> unknownColor
                    }.copy(alpha = dim)
                    drawArc(
                        color = fill,
                        startAngle = startAngle,
                        sweepAngle = 15f,
                        useCenter = true,
                        topLeft = topLeft,
                        size = arcSize,
                    )
                    if (state == PowerState.MAYBE) {
                        drawArc(
                            color = maybeStroke.copy(alpha = dim),
                            startAngle = startAngle,
                            sweepAngle = 15f,
                            useCenter = true,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = 3.dp.toPx()),
                        )
                    }
                }
                if (current) {
                    drawArc(
                        color = currentStroke,
                        startAngle = -90f + hourIndex * 30f,
                        sweepAngle = 30f,
                        useCenter = true,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = 5.dp.toPx()),
                    )
                }
            }
            drawCircle(
                color = faceColor,
                radius = diameter * 0.32f,
                center = center,
            )
            drawCircle(
                color = labelColor.copy(alpha = 0.35f),
                radius = diameter / 2f,
                center = center,
                style = Stroke(width = 1.dp.toPx()),
            )
            val labels = listOf("12", "3", "6", "9")
            val labelRadius = diameter / 2f - 14.dp.toPx()
            labels.forEachIndexed { index, label ->
                val angle = Math.toRadians((-90f + index * 90f).toDouble())
                val layout = measurer.measure(label, labelStyle)
                val x = center.x + (cos(angle) * labelRadius).toFloat() - layout.size.width / 2f
                val y = center.y + (sin(angle) * labelRadius).toFloat() - layout.size.height / 2f
                drawText(layout, topLeft = Offset(x, y))
            }
        }
    }
}

@Preview(showBackground = true, name = "All on")
@Composable
private fun OutageDialAllOnPreview() {
    SvitloTheme {
        OutageDial(hours = List(12) { HourState(PowerState.ON, PowerState.ON) }, hourOffset = 0)
    }
}

@Preview(showBackground = true, name = "All off")
@Composable
private fun OutageDialAllOffPreview() {
    SvitloTheme {
        OutageDial(hours = List(12) { HourState(PowerState.OFF, PowerState.OFF) }, hourOffset = 12)
    }
}

@Preview(showBackground = true, name = "Mixed half hours")
@Composable
private fun OutageDialMixedPreview() {
    SvitloTheme {
        OutageDial(
            hours = listOf(
                HourState(PowerState.ON, PowerState.ON),
                HourState(PowerState.OFF, PowerState.ON),
                HourState(PowerState.OFF, PowerState.OFF),
                HourState(PowerState.ON, PowerState.OFF),
                HourState(PowerState.MAYBE, PowerState.MAYBE),
                HourState(PowerState.ON, PowerState.ON),
                HourState(PowerState.ON, PowerState.ON),
                HourState(PowerState.OFF, PowerState.OFF),
                HourState(PowerState.ON, PowerState.ON),
                HourState(PowerState.ON, PowerState.ON),
                HourState(PowerState.MAYBE, PowerState.ON),
                HourState(PowerState.OFF, PowerState.OFF),
            ),
            hourOffset = 0,
            isToday = true,
            currentHour = 2,
        )
    }
}
