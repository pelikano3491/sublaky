package org.sublaky.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun StatusLamp(playing: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Canvas(Modifier.size(8.dp)) { drawCircle(if (playing) Amber else Color(0xFF55534E)) }
        Eyebrow(if (playing) "PLAY" else "READY", if (playing) Amber else Muted)
    }
}

@Composable
internal fun TransportKey(label: String, kind: String, main: Boolean, enabled: Boolean, modifier: Modifier, click: () -> Unit) {
    Button(onClick = click, enabled = enabled, modifier = modifier.heightIn(min = 64.dp).metalPanel(4.dp),
        shape = RoundedCornerShape(4.dp), contentPadding = PaddingValues(horizontal = 4.dp, vertical = 9.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (main && enabled) Amber.copy(alpha = .5f) else Color(0xFF5C5D58)),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = if (main) Amber else Paper,
            disabledContainerColor = Color.Transparent, disabledContentColor = Color(0xFF777670))) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(contentAlignment = Alignment.Center) {
                if (main && enabled) Canvas(Modifier.size(26.dp)) { drawCircle(Brush.radialGradient(listOf(Amber.copy(alpha = .45f), Color.Transparent))) }
                TransportIcon(kind, if (!enabled) Color(0xFF777670) else if (main) Amber else Paper, Modifier.size(23.dp))
            }
            Text(label, fontSize = 11.sp, maxLines = 1)
        }
    }
}

@Composable
internal fun TransportIcon(kind: String, color: Color, modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width; val h = size.height
        fun triangle(left: Float, right: Float) { drawPath(Path().apply { moveTo(w * left, h * .18f); lineTo(w * right, h * .5f); lineTo(w * left, h * .82f); close() }, color) }
        when (kind) {
            "pause" -> { drawRect(color, Offset(w * .20f, h * .18f), Size(w * .20f, h * .64f)); drawRect(color, Offset(w * .60f, h * .18f), Size(w * .20f, h * .64f)) }
            "next" -> { triangle(.15f, .72f); drawRect(color, Offset(w * .77f, h * .18f), Size(w * .12f, h * .64f)) }
            "previous" -> { triangle(.85f, .28f); drawRect(color, Offset(w * .11f, h * .18f), Size(w * .12f, h * .64f)) }
            "shuffle" -> {
                drawPath(Path().apply { moveTo(w * .08f, h * .23f); lineTo(w * .30f, h * .23f); lineTo(w * .69f, h * .77f); lineTo(w * .94f, h * .77f) }, color, style = Stroke(w * .09f))
                drawPath(Path().apply { moveTo(w * .08f, h * .77f); lineTo(w * .30f, h * .77f); lineTo(w * .69f, h * .23f); lineTo(w * .94f, h * .23f) }, color, style = Stroke(w * .09f))
                listOf(.23f, .77f).forEach { y -> drawPath(Path().apply { moveTo(w * .78f, h * (y - .13f)); lineTo(w * .94f, h * y); lineTo(w * .78f, h * (y + .13f)) }, color, style = Stroke(w * .09f)) }
            }
            else -> triangle(.25f, .85f)
        }
    }
}

@Composable
internal fun TapeMark(modifier: Modifier) {
    Canvas(modifier) {
        drawRoundRect(Amber, Offset(0f, size.height * .18f), Size(size.width, size.height * .64f), CornerRadius(5f))
        drawRoundRect(Ink, Offset(size.width * .12f, size.height * .33f), Size(size.width * .76f, size.height * .29f), CornerRadius(5f))
        listOf(.30f, .70f).forEach { drawCircle(Amber, size.width * .09f, Offset(size.width * it, size.height * .475f)) }
        drawLine(Ink, Offset(size.width * .3f, size.height * .71f), Offset(size.width * .7f, size.height * .71f), 2f)
    }
}

@Composable
internal fun SpeakerGrille(modifier: Modifier) {
    Canvas(modifier) { repeat(4) { row -> repeat(12) { col -> drawCircle(Color(0xFF0D1912), 1.5.dp.toPx(), Offset(size.width * (col + .5f) / 12, size.height * (row + .5f) / 4)) } } }
}
