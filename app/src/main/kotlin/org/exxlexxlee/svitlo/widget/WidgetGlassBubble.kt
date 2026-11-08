package org.exxlexxlee.svitlo.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.highlight.Highlight

/**
 * Creates a frosted glass bubble ("матовая бульбашка") modifier for Compose UI.
 * When a [backdrop] is provided, it applies real-time backdrop blur using `io.github.kyant0:backdrop`.
 */
fun Modifier.widgetFrostedBubble(
    shape: Shape = RoundedCornerShape(28.dp),
    backdrop: LayerBackdrop? = null,
    tintColor: Color = Color(0x59282C34),
    borderColor: Color = Color(0x4DFFFFFF),
    borderWidth: Dp = 1.2.dp,
    blurRadius: Float = 32f,
): Modifier {
    val base = this
        .clip(shape)
        .border(borderWidth, borderColor, shape)

    return if (backdrop != null) {
        base.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = { blur(blurRadius) },
            highlight = { Highlight.Default },
            onDrawSurface = { drawRect(tintColor) },
        )
    } else {
        base.background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0x7A484C55),
                    Color(0x542E3138),
                    Color(0x6622242A),
                ),
            ),
            shape = shape,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF141722)
@Composable
fun WidgetGlassBubblePreview() {
    val backdrop = rememberLayerBackdrop()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        // Sampled background layer (backdrop source)
        Box(
            modifier = Modifier
                .matchParentSize()
                .layerBackdrop(backdrop)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF5B6CFF), Color(0xFF141722)),
                    ),
                ),
        )

        // Frosted bubble surface
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widgetFrostedBubble(backdrop = backdrop)
                .padding(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Київ",
                    color = Color(0xFFF4F6FC),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .background(Color(0x338EA2FF), RoundedCornerShape(14.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "Група 1.1",
                        color = Color(0xFFD4DEFF),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Матова бульбашка віджета",
                color = Color(0xFFBAC0D0),
                fontSize = 13.sp,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WidgetGlassBubbleGradientPreview() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFFFFB6C1), Color(0xFFC782B8), Color(0xFF6B4D82)),
                ),
            )
            .padding(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widgetFrostedBubble()
                .padding(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Київська область — 2.2",
                    color = Color(0xFFF4F6FC),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .background(Color(0x338EA2FF), RoundedCornerShape(14.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "Черга 2.2",
                        color = Color(0xFFD4DEFF),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Скляний матовий фон віджета",
                color = Color(0xFFBAC0D0),
                fontSize = 13.sp,
            )
        }
    }
}
