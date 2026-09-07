package org.sublaky.player

import androidx.compose.runtime.Composable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import org.sublaky.player.domain.VisualMode

private val demo = PlayerState(connected = true, playing = true, currentUri = "preview", title = "Luz de tarde", artist = "Sublaky · Sesiones en cinta", duration = 192000, position = 67000, shuffle = true)

@Composable
private fun DesignPreview(mode: VisualMode, longTitle: Boolean = false) {
    MaterialTheme(colorScheme = darkColorScheme(primary = Amber, onPrimary = Ink, onSurface = Paper, onSurfaceVariant = Muted)) {
        Column(Modifier.fillMaxSize().background(Ink).padding(18.dp)) {
            PlayerPanel(if (longTitle) demo.copy(title = "Una canción con un título extraordinariamente largo para comprobar la etiqueta") else demo, mode)
        }
    }
}

@PreviewTest @Preview(widthDp = 360, heightDp = 900) @Composable
fun WalkmanPhone() { DesignPreview(VisualMode.WALKMAN) }

@PreviewTest @Preview(widthDp = 800, heightDp = 650) @Composable
fun DeckTablet() { DesignPreview(VisualMode.DECK) }

@PreviewTest @Preview(widthDp = 800, heightDp = 1150) @Composable
fun ImmersiveTablet() { DesignPreview(VisualMode.FULLSCREEN) }

@PreviewTest @Preview(widthDp = 360, heightDp = 1100, fontScale = 1.5f) @Composable
fun LargeText() { DesignPreview(VisualMode.WALKMAN, true) }

@PreviewTest @Preview(widthDp = 360, heightDp = 120) @Composable
fun ModeNavigation() {
    MaterialTheme(colorScheme = darkColorScheme(primary = Amber)) {
        Column(Modifier.fillMaxSize().background(Ink).padding(12.dp)) { ModeBar(VisualMode.WALKMAN) {} }
    }
}

@Composable
private fun CollectionPreview(mode: VisualMode, landscape: Boolean = false) {
    val songs = (1..30).map { org.sublaky.player.domain.Track("song$it", "Canción $it · Luz de tarde", "Sublaky", durationMs = 192000, fileName = "$it.mp3") }
    MaterialTheme(colorScheme = darkColorScheme(primary = Amber, onPrimary = Ink, onSurface = Paper, onSurfaceVariant = Muted)) {
        Column(Modifier.fillMaxSize().background(Ink).padding(16.dp)) {
            AppHeader(landscape) {}
            CollectionLayout(demo.copy(tracks = songs), mode, songs, "", {}, null, {}, {}, {}, Modifier.weight(1f))
            AdaptiveModeBar(mode, landscape) {}
        }
    }
}
@PreviewTest @Preview(widthDp = 600, heightDp = 960) @Composable
fun CollectionPortrait() { CollectionPreview(VisualMode.DECK) }
@PreviewTest @Preview(widthDp = 960, heightDp = 600) @Composable
fun CollectionLandscape() { CollectionPreview(VisualMode.DECK, true) }
@PreviewTest @Preview(widthDp = 360, heightDp = 800) @Composable
fun CollectionPhone() { CollectionPreview(VisualMode.WALKMAN) }
@PreviewTest @Preview(widthDp = 800, heightDp = 360) @Composable
fun CollectionPhoneLandscape() { CollectionPreview(VisualMode.WALKMAN, true) }

@PreviewTest @Preview(widthDp = 960, heightDp = 600) @Composable
fun DesktopLandscape() { CollectionPreview(VisualMode.DESKTOP, true) }
@PreviewTest @Preview(widthDp = 600, heightDp = 960) @Composable
fun WalkmanTabletPortrait() { CollectionPreview(VisualMode.WALKMAN) }
@PreviewTest @Preview(widthDp = 960, heightDp = 600) @Composable
fun FullscreenLandscape() { CollectionPreview(VisualMode.FULLSCREEN, true) }
