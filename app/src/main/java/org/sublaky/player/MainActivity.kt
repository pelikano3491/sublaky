package org.sublaky.player

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.sublaky.player.domain.Track
import org.sublaky.player.domain.VisualMode

internal val Ink = Color(0xFF0B0C0D)
internal val Paper = Color(0xFFE5DDCF)
internal val Amber = Color(0xFFFF903F)
internal val Muted = Color(0xFFA4A29C)
internal val Orange = Color(0xFFCF653B)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Amber, onPrimary = Ink,
                background = Ink, surface = Color(0xFF191A1B), onSurface = Paper,
                onBackground = Paper, onSurfaceVariant = Muted, secondary = Orange,
                surfaceVariant = Color(0xFF242526))) { Sublaky() }
        }
    }
}

@Composable
private fun Sublaky(vm: PlayerViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
    fun granted() = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    var denied by rememberSaveable { mutableStateOf(false) }
    val request = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { denied = !it; vm.scan(it) }
    val folder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { it?.let { uri -> vm.addFolder(uri, granted()) } }
    var settings by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    val window = LocalWindowInfo.current.containerSize
    val wide = with(LocalDensity.current) { window.width.toDp() >= 840.dp }
    val deckColumns = with(LocalDensity.current) { window.width.toDp() >= 600.dp }
    val mode = if (state.mode != VisualMode.AUTO) state.mode else if (wide) VisualMode.DESKTOP else if (window.width > window.height) VisualMode.DECK else VisualMode.WALKMAN
    val tracks = remember(state.tracks, query) { state.tracks.filter { query.isBlank() || "${it.title} ${it.artist} ${it.album}".contains(query.trim(), ignoreCase = true) } }
    LaunchedEffect(Unit) { vm.scan(granted()) }
    DisposableEffect(mode) {
        val activityWindow = (context as? ComponentActivity)?.window
        val bars = activityWindow?.let { WindowCompat.getInsetsController(it, it.decorView) }
        if (mode == VisualMode.FULLSCREEN) {
            bars?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            bars?.hide(WindowInsetsCompat.Type.systemBars())
        } else bars?.show(WindowInsetsCompat.Type.systemBars())
        onDispose { bars?.show(WindowInsetsCompat.Type.systemBars()) }
    }
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF1E2021), Ink, Color(0xFF090A0B))))) {
        Column(Modifier.safeDrawingPadding().fillMaxSize().padding(horizontal = if (wide) 28.dp else 18.dp)) {
            AppHeader(window.width > window.height, { settings = true })
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp)) }
            if (denied) Text("Puedes elegir una carpeta o dar permiso de música desde los ajustes de Android.", color = Amber, style = MaterialTheme.typography.bodySmall)
            CollectionLayout(state, mode, tracks, query, { query = it }, vm,
                { request.launch(permission) }, { folder.launch(null) }, { vm.scan(granted()) }, Modifier.weight(1f))
            AdaptiveModeBar(mode, window.width > window.height, vm::mode)
        }
    }
    if (settings) AlertDialog(onDismissRequest = { settings = false }, title = { Text("A tu manera") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Eyebrow("TU BIBLIOTECA", Amber)
            Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(state.mode == VisualMode.AUTO, { vm.mode(if (it) VisualMode.AUTO else mode) }); Text("Elegir modo según la pantalla") }
            Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(state.onlyMp3, { vm.onlyMp3(it, granted()) }); Text("Solo MP3") }
            Text(if (state.onlyMp3) "Solo se mostrarán archivos .mp3." else "MP3 · M4A · AAC · FLAC · OGG · OPUS")
            Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(state.excludeVoice, { vm.settings(it, state.includeWav, granted()) }); Text("Excluir notas de voz") }
            Text("Se orienta por nombre, carpeta y etiquetas. Desactívalo si falta algún audio.", style = MaterialTheme.typography.bodySmall)
            Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(state.includeWav, { vm.settings(state.excludeVoice, it, granted()) }, enabled = !state.onlyMp3); Text("Incluir WAV") }
            HorizontalDivider()
            Text("${state.folders} carpetas autorizadas")
            TextButton(onClick = { folder.launch(null) }) { Text("Añadir una carpeta") }
            TextButton(onClick = { vm.clearFolders(granted()) }) { Text("Olvidar las carpetas") }
            HorizontalDivider()
            Text("Sublaky 1.1 · Edición nocturna\nMúsica local, sin cuentas ni anuncios.\nEl espectro responde al audio reproducido. No usa el micrófono. Las bobinas son decorativas.", style = MaterialTheme.typography.bodySmall)
        }
    }, confirmButton = { TextButton(onClick = { settings = false }) { Text("Listo") } })
}

@Composable
private fun ListeningHeading(mode: VisualMode) {
    Column(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 10.dp)) {
        Eyebrow("${mode.label.uppercase()}  /  LA MÚSICA TAMBIÉN NOS SALVA", Muted)
    }
}

@Composable
internal fun AppHeader(compact: Boolean, settings: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = if (compact) 0.dp else 8.dp), verticalAlignment = Alignment.CenterVertically) {
        TapeMark(Modifier.size(28.dp))
        Text("Sublaky", color = Paper, fontWeight = FontWeight.Black, fontSize = if (compact) 23.sp else 32.sp, modifier = Modifier.weight(1f).padding(start = 10.dp))
        TextButton(onClick = settings) { Text("Ajustes") }
    }
}

@Composable
internal fun AdaptiveModeBar(mode: VisualMode, compact: Boolean, change: (VisualMode) -> Unit) {
    if (!compact) ModeBar(mode, change)
    else Row(Modifier.fillMaxWidth().metalPanel(4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        VisualMode.entries.filter { it != VisualMode.AUTO }.forEach { item ->
            TextButton(onClick = { change(item) }) { Text(item.label, color = if (mode == item) Amber else Muted, fontSize = 11.sp) }
        }
    }
}

@Composable
internal fun CollectionLayout(state: PlayerState, mode: VisualMode, tracks: List<Track>, query: String,
    change: (String) -> Unit, vm: PlayerViewModel?, permission: () -> Unit, folder: () -> Unit,
    scan: () -> Unit, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val availableHeight = maxHeight
        val horizontal = maxWidth > maxHeight
        val columns = maxWidth >= 560.dp && (mode != VisualMode.WALKMAN || maxWidth > maxHeight)
        if (mode == VisualMode.FULLSCREEN) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                ListeningHeading(mode)
                if (horizontal) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) { Cassette(state) }
                    Box(Modifier.weight(1f)) { CompactPlayer(state, vm, dense = availableHeight < 350.dp, showTape = false) }
                } else PlayerPanel(state, mode, vm)
            }
        } else if (columns) {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())) {
                    if (availableHeight >= 350.dp) ListeningHeading(mode)
                    PlayerPanel(state, mode, vm, compact = availableHeight < 650.dp, dense = availableHeight < 350.dp)
                }
                Library(state, tracks, query, change, vm, permission, folder, scan, Modifier.weight(1f).fillMaxHeight())
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                // Reserve enough room for the pinned search and visible song rows.
                Column(Modifier.heightIn(max = availableHeight * 0.58f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                    PlayerPanel(state, mode, vm, compact = true)
                }
                Library(state, tracks, query, change, vm, permission, folder, scan, Modifier.weight(1f).fillMaxWidth())
            }
        }
    }
}

@Composable
internal fun PlayerPanel(state: PlayerState, mode: VisualMode, vm: PlayerViewModel? = null, compact: Boolean = false, dense: Boolean = false) {
    if (compact) { CompactPlayer(state, vm, dense); return }
    val portable = mode == VisualMode.WALKMAN
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = if (portable) 440.dp else 780.dp).fillMaxWidth()
            .metalPanel(10.dp).padding(if (portable) 12.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).padding(end = 8.dp)) { Eyebrow("SUBLAKY  /  ${if (portable) "PORTABLE" else "STEREO DECK"}", Paper); Eyebrow("SL—01     •     HIGH FIDELITY", Muted) }
                StatusLamp(state.playing)
            }
            BoxWithConstraints {
                if (!portable && mode != VisualMode.FULLSCREEN && maxWidth >= 550.dp) {
                    Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1.2f)) { Cassette(state) }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) { NowPlaying(state); SpectrumView(state.playing); Progress(state, vm) }
                    }
                } else Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { Cassette(state); NowPlaying(state); SpectrumView(state.playing); Progress(state, vm) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TransportKey("Anterior", "previous", false, state.currentUri.isNotEmpty(), Modifier.weight(1f)) { vm?.previous() }
                TransportKey(if (state.playing) "Pausa" else "Escuchar", if (state.playing) "pause" else "play", true, state.currentUri.isNotEmpty(), Modifier.weight(1.2f)) { vm?.toggle() }
                TransportKey("Siguiente", "next", false, state.currentUri.isNotEmpty(), Modifier.weight(1f)) { vm?.next() }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                FilterChip(selected = state.shuffle, onClick = { vm?.toggleShuffle() }, enabled = state.connected,
                    label = { Text(if (state.shuffle) "Aleatorio · Sí" else "Aleatorio · No", fontSize = 12.sp) },
                    leadingIcon = { TransportIcon("shuffle", Amber, Modifier.size(17.dp)) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF38271B), selectedLabelColor = Amber))
                SpeakerGrille(Modifier.width(64.dp).height(26.dp))
            }
        }
    }
}

@Composable
private fun NowPlaying(state: PlayerState) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(Color(0xFF0B0C0D)).border(1.dp, Color(0xFF353633), RoundedCornerShape(4.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Eyebrow(if (state.currentUri.isEmpty()) "LISTO PARA TU PRIMERA CINTA" else if (state.playing) "AHORA SUENA" else "EN PAUSA", Color(0xFFABA79C))
        Text(if (state.currentUri.isEmpty()) "Elige una canción" else state.title, color = Color(0xFFEAE0CB), fontSize = 22.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(if (state.currentUri.isEmpty()) "Tu colección te espera abajo" else state.artist.ifBlank { "Artista desconocido" }, color = Color(0xFFABA79C), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun CompactPlayer(state: PlayerState, vm: PlayerViewModel?, dense: Boolean, showTape: Boolean = true) {
    Column(Modifier.fillMaxWidth().metalPanel(8.dp).padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(state.title, color = Paper, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (!dense) Text(state.artist.ifBlank { "Artista desconocido" }, color = Muted, fontSize = 11.sp, maxLines = 1)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!dense && showTape) Box(Modifier.width(90.dp)) { Cassette(state) }
            SpectrumView(state.playing, Modifier.weight(1f), compact = true, dense = dense)
        }
        Progress(state, vm)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            IconButton(modifier = Modifier.semantics { contentDescription = "Anterior" }, onClick = { vm?.previous() }, enabled = state.currentUri.isNotEmpty()) { TransportIcon("previous", Paper, Modifier.size(20.dp)) }
            TextButton(onClick = { vm?.toggle() }, enabled = state.currentUri.isNotEmpty()) { Text(if (state.playing) "Pausa" else "Escuchar", color = Amber) }
            IconButton(modifier = Modifier.semantics { contentDescription = "Siguiente" }, onClick = { vm?.next() }, enabled = state.currentUri.isNotEmpty()) { TransportIcon("next", Paper, Modifier.size(20.dp)) }
            IconToggleButton(modifier = Modifier.semantics { contentDescription = "Aleatorio" }, checked = state.shuffle, onCheckedChange = { vm?.toggleShuffle() }, enabled = state.connected) {
                TransportIcon("shuffle", if (state.shuffle) Amber else Muted, Modifier.size(20.dp))
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun Progress(state: PlayerState, vm: PlayerViewModel?) {
    var scrub by remember(state.currentUri) { mutableStateOf<Float?>(null) }
    Column {
        Slider(value = scrub ?: state.position.coerceAtMost(state.duration).toFloat(),
            onValueChange = { scrub = it }, onValueChangeFinished = { scrub?.let { vm?.seek(it.toLong()) }; scrub = null },
            valueRange = 0f..state.duration.coerceAtLeast(1).toFloat(), enabled = state.duration > 0,
            modifier = Modifier.height(32.dp),
            thumb = { _ -> Canvas(Modifier.size(20.dp)) {
                drawCircle(Color.Black)
                drawCircle(Brush.linearGradient(listOf(Color(0xFFE3DAC6), Color(0xFF756D61))), radius = size.minDimension * .44f)
                drawCircle(Color(0xFFE9D9BE), radius = size.minDimension * .44f, style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
            } },
            track = { _ -> Canvas(Modifier.fillMaxWidth().height(5.dp)) {
                drawRoundRect(Color(0xFF41413D), cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()))
                val fraction = ((scrub ?: state.position.toFloat()) / state.duration.coerceAtLeast(1)).coerceIn(0f, 1f)
                drawRoundRect(Brush.verticalGradient(listOf(Color(0xFFFFD2A2), Amber, Color(0xFFAC4A24))), size = androidx.compose.ui.geometry.Size(size.width * fraction, size.height), cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()))
            } })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Eyebrow(time(scrub?.toLong() ?: state.position), Paper); Eyebrow(time(state.duration), Muted) }
    }
}

@Composable
private fun LibraryHeader(state: PlayerState, query: String, change: (String) -> Unit, permission: () -> Unit, folder: () -> Unit, scan: () -> Unit) {
    var sources by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Colección · ${state.tracks.size}", color = Paper, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Box {
                TextButton(onClick = { sources = true }) { Text("Fuentes") }
                DropdownMenu(expanded = sources, onDismissRequest = { sources = false }) {
                    DropdownMenuItem(text = { Text("Añadir carpeta") }, onClick = { sources = false; folder() })
                    DropdownMenuItem(text = { Text("Actualizar biblioteca") }, enabled = !state.scanning, onClick = { sources = false; scan() })
                    DropdownMenuItem(text = { Text("Dar acceso") }, onClick = { sources = false; permission() })
                }
            }
        }
        OutlinedTextField(value = query, onValueChange = change, modifier = Modifier.fillMaxWidth(), singleLine = true,
            placeholder = { Text("Buscar canción, artista o álbum", fontSize = 13.sp) }, shape = RoundedCornerShape(12.dp),
            trailingIcon = { if (query.isNotEmpty()) TextButton(onClick = { change("") }) { Text("Borrar") } })
        if (state.scanning) LinearProgressIndicator(Modifier.fillMaxWidth())
    }
}

@Composable
private fun TrackRow(track: Track, index: Int, state: PlayerState, vm: PlayerViewModel?) {
    val selected = track.uri == state.currentUri
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (selected) Color(0xFF34271E) else Color.White.copy(alpha = .025f))
        .clickable(enabled = state.connected) { vm?.play(track) }.padding(horizontal = 12.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(38.dp).clip(RoundedCornerShape(7.dp)).background(if (selected) Amber else Color(0xFF292B2B)), contentAlignment = Alignment.Center) {
            if (selected) TransportIcon(if (state.playing) "play" else "pause", Ink, Modifier.size(18.dp))
            else Text("%02d".format(index + 1), color = Muted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(track.title, color = if (selected) Amber else Paper, fontSize = 14.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.artist.ifBlank { "Artista desconocido" }, color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(time(track.durationMs), color = Muted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun Library(state: PlayerState, tracks: List<Track>, query: String, change: (String) -> Unit, vm: PlayerViewModel?, permission: () -> Unit, folder: () -> Unit, scan: () -> Unit, modifier: Modifier) {
    Column(modifier) {
        LibraryHeader(state, query, change, permission, folder, scan)
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            if (tracks.isEmpty()) item { EmptyLibrary(state.scanning, query.isNotBlank()) }
            itemsIndexed(tracks, key = { _, track -> track.uri }) { index, track -> TrackRow(track, index, state, vm) }
        }
    }
}

@Composable
private fun EmptyLibrary(scanning: Boolean, searching: Boolean) {
    Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(if (scanning) "Buscando tus próximas canciones…" else if (searching) "No encontramos esa canción." else "Toda colección empieza con una cinta.", color = Paper, fontSize = 18.sp)
        Text(if (searching) "Prueba con otro título o artista." else "Añade una carpeta o da acceso a la música del dispositivo.", color = Muted, fontSize = 13.sp)
    }
}

@Composable
internal fun Eyebrow(text: String, color: Color = Muted) { Text(text, color = color, fontSize = 9.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
private fun time(ms: Long): String = "%d:%02d".format(ms.coerceAtLeast(0) / 60000, (ms.coerceAtLeast(0) / 1000) % 60)





