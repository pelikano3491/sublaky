package org.sublaky.player

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import org.sublaky.player.playback.AudioSpectrum
import org.sublaky.player.domain.SpectrumAnalyzer
import org.sublaky.player.domain.VisualMode
import kotlin.math.*
import kotlin.random.Random

private val grain by lazy {
    val random = Random(61)
    Bitmap.createBitmap(IntArray(128 * 128) {
        val alpha = random.nextInt(3, 23)
        (alpha shl 24) or if (random.nextBoolean()) 0xFFFFFF else 0
    }, 128, 128, Bitmap.Config.ARGB_8888).asImageBitmap()
}

internal fun Modifier.metalPanel(radius: Dp = 10.dp): Modifier = clip(RoundedCornerShape(radius)).drawWithCache {
    val texture = ShaderBrush(ImageShader(grain, TileMode.Repeated, TileMode.Repeated))
    val fill = Brush.linearGradient(listOf(Color(0xFF303233), Color(0xFF141617), Color(0xFF232425)))
    onDrawBehind {
        drawRect(fill)
        drawRect(texture)
        drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF787975), Color(0xFF0B0C0C), Color(0xFF454641))), cornerRadius = CornerRadius(radius.toPx()), style = Stroke(2.dp.toPx()))
        drawRoundRect(Color.Black.copy(alpha = .7f), topLeft = Offset(3.dp.toPx(), 3.dp.toPx()), size = Size(size.width - 6.dp.toPx(), size.height - 6.dp.toPx()), cornerRadius = CornerRadius(radius.toPx()), style = Stroke(1.dp.toPx()))
    }
}

@Composable
internal fun Cassette(state: PlayerState) {
    var angle by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(state.playing) {
        if (state.playing) {
            var before = withFrameNanos { it }
            while (true) withFrameNanos { now -> angle = (angle + (now - before) / 1e9f * 1.8f) % (2 * PI.toFloat()); before = now }
        }
    }
    BoxWithConstraints(Modifier.fillMaxWidth().aspectRatio(1.5f).metalPanel(10.dp)) {
        val scale = (maxWidth.value / 340f).coerceIn(.25f, 2f)
        val miniature = maxWidth < 160.dp
        val large = LocalDensity.current.fontScale > 1.25f
        Box(Modifier.padding(horizontal = maxWidth * .047f, vertical = maxHeight * .055f).fillMaxWidth().fillMaxHeight(.80f)
            .clip(RoundedCornerShape(9.dp)).border(1.dp, Color(0xFF8C775E), RoundedCornerShape(9.dp))) {
            if (state.artwork != null) Image(state.artwork.asImageBitmap(), "Carátula sobre la cinta", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else SunsetLabel(Modifier.fillMaxSize())
            Box(Modifier.fillMaxWidth().fillMaxHeight(.42f).background(Brush.verticalGradient(listOf(Color(0xFFF0D5B2).copy(alpha = .9f), Color(0xFFE5C5A2).copy(alpha = .65f), Color.Transparent))))
            if (!miniature) Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = (10 * scale).dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (state.currentUri.isBlank()) "Tu mundo en una cinta" else state.title, color = Color(0xFF201C1A), fontSize = (20 * scale).sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (!large) Text(state.artist.ifBlank { "Música siempre contigo" }, color = Color(0xFF30251F), fontSize = (11 * scale).sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text("A", color = Paper, fontFamily = FontFamily.Serif, fontSize = (23 * scale).sp, modifier = Modifier.align(Alignment.CenterEnd).padding(end = (9 * scale).dp, top = (20 * scale).dp))
        }
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            val y = h * .56f; val rotation = angle
            val rim = Brush.verticalGradient(listOf(Color(0xFFB3A18C), Color(0xFF2C2925), Color(0xFF887963)))
            drawRoundRect(Color.Black.copy(alpha = .75f), Offset(w * .18f, h * .35f), Size(w * .64f, h * .39f), CornerRadius(h * .18f))
            drawRoundRect(rim, Offset(w * .185f, h * .355f), Size(w * .63f, h * .37f), CornerRadius(h * .17f), style = Stroke(h * .013f))
            drawRoundRect(Color(0xFF060708), Offset(w * .20f, h * .37f), Size(w * .60f, h * .34f), CornerRadius(h * .16f))
            val center = Rect(w * .43f, h * .405f, w * .57f, h * .675f)
            drawRect(Brush.horizontalGradient(listOf(Color(0xFF1A1411), Color(0xFF806752), Color(0xFF211916)), startX = center.left, endX = center.right), center.topLeft, center.size)
            repeat(15) { i -> drawLine(Color(0xFFCEAA7F).copy(alpha = .14f), Offset(center.left + center.width * i / 15, center.top), Offset(center.left + center.width * i / 15, center.bottom), 1f) }
            listOf(.315f, .685f).forEach { xFraction ->
                val p = Offset(w * xFraction, y)
                val r = h * .142f
                drawCircle(Color.Black, r * 1.08f, p)
                drawCircle(Brush.linearGradient(listOf(Color(0xFFE0D1B6), Color(0xFF72634E), Color(0xFFB9B3A2), Color(0xFF3E352E)), p - Offset(r, r), p + Offset(r, r)), r, p)
                drawCircle(Color(0xFFDFD1B7).copy(alpha = .8f), r * .90f, p, style = Stroke(r * .035f))
                drawCircle(Color(0xFF111112), r * .72f, p)
                repeat(6) { n ->
                    val a = rotation + n * PI.toFloat() / 3
                    drawLine(Color(0xFFB8AE98), p + Offset(cos(a), sin(a)) * r * .47f, p + Offset(cos(a), sin(a)) * r * .64f, r * .15f, StrokeCap.Round)
                }
                drawCircle(Brush.radialGradient(listOf(Color(0xFF484847), Color(0xFF070708)), p - Offset(r * .12f, r * .15f), r * .56f), r * .41f, p)
                drawCircle(Color.White.copy(alpha = .22f), r * .08f, p - Offset(r * .12f, r * .15f))
            }
            val bottom = Path().apply { moveTo(w * .13f, h * .97f); lineTo(w * .17f, h * .81f); lineTo(w * .83f, h * .81f); lineTo(w * .87f, h * .97f); close() }
            drawPath(bottom, Brush.verticalGradient(listOf(Color(0xFF313331), Color(0xFF101213)), startY = h * .80f, endY = h))
            drawPath(bottom, Color(0xFF53564F), style = Stroke(1.4f))
            drawCircle(Color(0xFF060809), h * .02f, Offset(w * .5f, h * .86f))
            listOf(Offset(w * .025f, h * .034f), Offset(w * .975f, h * .034f), Offset(w * .025f, h * .966f), Offset(w * .975f, h * .966f)).forEach { p ->
                val r = h * .018f
                drawCircle(Color.Black, r * 1.4f, p)
                drawCircle(Brush.linearGradient(listOf(Color(0xFFB8B9AF), Color(0xFF272A29)), p - Offset(r, r), p + Offset(r, r)), r, p)
                drawLine(Color.Black, p - Offset(r * .6f, 0f), p + Offset(r * .6f, 0f), r * .3f)
                drawLine(Color.Black, p - Offset(0f, r * .6f), p + Offset(0f, r * .6f), r * .3f)
            }
        }
        if (!miniature) Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = (20 * scale).dp, vertical = (8 * scale).dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Eyebrow("SBL–60", Muted); Eyebrow(if (large) "TYPE I" else "LOW NOISE · HIGH FIDELITY", Muted)
        }
    }
}

/** Original procedural label, used only when the music has no embedded cover. */
@Composable
private fun SunsetLabel(modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width; val h = size.height
        drawRect(Brush.verticalGradient(listOf(Color(0xFFCF8A79), Color(0xFF8E5666), Color(0xFF271C34))))
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFFD49F), Color(0xFFFFB47F).copy(alpha = .1f)), Offset(w * .66f, h * .34f), h * .23f), h * .23f, Offset(w * .66f, h * .34f))
        drawCircle(Color(0xFFF7BC8B), h * .085f, Offset(w * .66f, h * .34f))
        val random = Random(24)
        repeat(100) { drawCircle(Color(0xFFFFE2C1).copy(alpha = random.nextFloat() * .6f), random.nextFloat() * 1.5f + .4f, Offset(random.nextFloat() * w, random.nextFloat() * h * .65f)) }
        repeat(3) { layer ->
            val mountain = Path().apply {
                moveTo(0f, h)
                for (i in 0..20) lineTo(w * i / 20, h * (.50f + layer * .13f - random.nextFloat() * .23f))
                lineTo(w, h); close()
            }
            drawPath(mountain, listOf(Color(0xFF6D4355), Color(0xFF332B3D), Color(0xFF131A25))[layer])
        }
    }
}

@Composable
internal fun SpectrumView(playing: Boolean, modifier: Modifier = Modifier, compact: Boolean = false, dense: Boolean = false) {
    val preview = LocalInspectionMode.current
    val owner = LocalLifecycleOwner.current
    // Preview effects are not executed by every renderer, so compute its initial frame eagerly.
    val initial = remember(preview) {
        if (preview) {
            val tones = doubleArrayOf(110.0, 260.0, 590.0, 1200.0, 2500.0, 5200.0, 10000.0)
            SpectrumAnalyzer().analyze(FloatArray(2048) { i -> tones.sumOf { sin(2 * PI * it * i / 48000) * .04 }.toFloat() }, 48000)
        } else FloatArray(32)
    }
    val frame by produceState(initial, playing, owner, preview) {
        if (preview) value = initial
        else if (playing) owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            AudioSpectrum.observers.incrementAndGet()
            try {
                while (true) {
                    val next = AudioSpectrum.levels
                    value = FloatArray(32) { max(next[it], value[it] * .77f) }
                    delay(40)
                }
            } finally { AudioSpectrum.observers.decrementAndGet(); value = FloatArray(32) }
        } else value = FloatArray(32)
    }
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(5.dp)).background(Color(0xFF08090A)).border(1.dp, Color(0xFF30312E), RoundedCornerShape(5.dp)).padding(10.dp)) {
        Canvas(Modifier.fillMaxWidth().height(if (dense) 16.dp else if (compact) 36.dp else 84.dp)) {
            val bars = frame
            val columns = 32; val rows = 16
            val dx = size.width / columns; val dy = size.height / rows
            repeat(columns) { x ->
                val hue = lerp(Color(0xFFFFBE53), Color(0xFFFF562C), x / 31f)
                val lit = (bars[x] * rows).roundToInt()
                if (lit > 0) drawRect(hue.copy(alpha = .09f), Offset(x * dx - dx * .3f, size.height - lit * dy), Size(dx * 1.5f, lit * dy))
                repeat(rows) { y ->
                    drawRect(if (y < lit) hue else Color(0xFF251C13), Offset(x * dx + dx * .16f, size.height - (y + 1) * dy), Size(dx * .66f, dy * .62f))
                }
            }
        }
        val enlarged = LocalDensity.current.fontScale > 1.25f
        if (!dense) Row(Modifier.fillMaxWidth().padding(top = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            if (!enlarged) Eyebrow("60 Hz", Muted)
            Eyebrow("ESPECTRO DE AUDIO", Muted)
            if (!enlarged) Eyebrow("16 kHz", Muted)
        }
    }
}

@Composable
internal fun ModeBar(mode: VisualMode, select: (VisualMode) -> Unit) {
    Row(Modifier.fillMaxWidth().metalPanel(4.dp).padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        VisualMode.entries.filter { it != VisualMode.AUTO }.forEach { item ->
            val active = item == mode
            TextButton(onClick = { select(item) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.textButtonColors(contentColor = if (active) Amber else Muted), contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (item == VisualMode.AUTO) Text("A", fontFamily = FontFamily.Monospace, fontSize = 18.sp)
                    else Canvas(Modifier.size(22.dp)) {
                        val c = if (active) Amber else Muted
                        val w = size.width; val h = size.height
                        when (item) {
                            VisualMode.DESKTOP -> repeat(4) { i -> drawRect(c, Offset(w * (.10f + i * .22f), h * (1 - (i + 1) * .20f)), Size(w * .12f, h * (i + 1) * .20f)) }
                            VisualMode.FULLSCREEN -> {
                                listOf(Offset(0f, 0f), Offset(w, 0f), Offset(0f, h), Offset(w, h)).forEach { p ->
                                    val sx = if (p.x == 0f) 1 else -1; val sy = if (p.y == 0f) 1 else -1
                                    drawLine(c, p, p + Offset(w * .3f * sx, 0f), 2.dp.toPx())
                                    drawLine(c, p, p + Offset(0f, h * .3f * sy), 2.dp.toPx())
                                }
                            }
                            else -> {
                                drawRoundRect(c, Offset(0f, h * .2f), Size(w, h * .6f), CornerRadius(2.dp.toPx()), style = Stroke(1.dp.toPx()))
                                listOf(.28f, .72f).forEach { drawCircle(c, w * .13f, Offset(w * it, h * .48f), style = Stroke(1.dp.toPx())) }
                            }
                        }
                    }
                    Box(Modifier.heightIn(min = 32.dp), contentAlignment = Alignment.Center) {
                        Text(if (item == VisualMode.FULLSCREEN) "Pantalla\ncompleta" else item.label, fontSize = 10.sp, lineHeight = 12.sp, maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                    Box(Modifier.width(42.dp).height(2.dp).background(if (active) Amber else Color.Transparent))
                }
            }
        }
    }
}
