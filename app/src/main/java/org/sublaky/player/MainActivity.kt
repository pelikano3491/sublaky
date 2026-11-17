package org.sublaky.player

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
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
import androidx.compose.ui.res.stringResource
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
import java.util.Locale

internal val Ink = Color(0xFF0B0C0D)
internal val Paper = Color(0xFFE5DDCF)
internal val Amber = Color(0xFFFF903F)
internal val Muted = Color(0xFFA4A29C)
internal val Orange = Color(0xFFCF653B)

private const val PREF_LANGUAGE = "preferred_language"

private fun getSavedLanguage(context: Context): String? =
    context.getSharedPreferences("sublaky", Context.MODE_PRIVATE).getString(PREF_LANGUAGE, null)

internal fun systemLocale(): Locale = if (Build.VERSION.SDK_INT >= 24) {
    Resources.getSystem().configuration.locales.get(0) ?: Locale.ENGLISH
} else {
    Resources.getSystem().configuration.locale ?: Locale.ENGLISH
}

internal fun detectedSystemLanguageLabel(): String = systemLocale().toLanguageTag()

internal fun localeForLanguageTag(languageTag: String?, fallbackLocale: Locale? = null): Locale = when (languageTag?.takeIf { it.isNotBlank() }) {
    "en" -> Locale.ENGLISH
    "es", "es-ES" -> Locale("es")
    else -> fallbackLocale ?: systemLocale()
}

private fun applyAppLanguage(context: Context, languageTag: String?) {
    val prefs = context.getSharedPreferences("sublaky", Context.MODE_PRIVATE)
    val normalized = languageTag?.takeIf { it.isNotBlank() }
    val locale = if (normalized == null) systemLocale() else localeForLanguageTag(normalized)

    if (normalized == null) {
        prefs.edit().remove(PREF_LANGUAGE).apply()
    } else {
        prefs.edit().putString(PREF_LANGUAGE, normalized).apply()
    }

    Locale.setDefault(locale)
    val config = context.resources.configuration
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        config.setLocale(locale)
        config.setLocales(LocaleList(locale))
    } else {
        config.setLocale(locale)
    }
    context.resources.updateConfiguration(config, context.resources.displayMetrics)
}

private fun languageTagLabel(languageTag: String?): String = when (languageTag) {
    "en" -> "English"
    "es", "es-ES" -> "Español"
    else -> "System default"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyAppLanguage(this, getSavedLanguage(this))
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
    var settings by remember { mutableStateOf(false) }
    var languageMenu by remember { mutableStateOf(false) }
    var selectedLanguage by rememberSaveable { mutableStateOf(getSavedLanguage(context)) }
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
            if (denied) Text(stringResource(R.string.permission_denied_message), color = Amber, style = MaterialTheme.typography.bodySmall)
            CollectionLayout(state, mode, tracks, query, { query = it }, vm,
                { request.launch(permission) }, { folder.launch(null) }, { vm.scan(granted()) }, Modifier.weight(1f))
            AdaptiveModeBar(mode, window.width > window.height, vm::mode)
        }
    }
    if (settings) AlertDialog(onDismissRequest = { settings = false }, title = { Text(stringResource(R.string.settings_title)) }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Eyebrow(stringResource(R.string.library_section_label), Amber)
            Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(state.mode == VisualMode.AUTO, { vm.mode(if (it) VisualMode.AUTO else mode) }); Text(stringResource(R.string.mode_auto_screen)) }
            Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(state.onlyMp3, { vm.onlyMp3(it, granted()) }); Text(stringResource(R.string.only_mp3)) }
            Text(if (state.onlyMp3) stringResource(R.string.only_mp3_notice) else stringResource(R.string.mp3_formats))
            Text(stringResource(R.string.language))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box {
                    TextButton(onClick = { languageMenu = true }) {
                        Text(
                            when (selectedLanguage) {
                                "en" -> stringResource(R.string.language_english)
                                "es", "es-ES" -> stringResource(R.string.language_spanish)
                                else -> stringResource(R.string.language_system)
                            }
                        )
                    }
                    DropdownMenu(expanded = languageMenu, onDismissRequest = { languageMenu = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.language_system)) }, onClick = {
                            languageMenu = false
                            selectedLanguage = null
                        })
                        DropdownMenuItem(text = { Text(stringResource(R.string.language_english)) }, onClick = {
                            languageMenu = false
                            selectedLanguage = "en"
                        })
                        DropdownMenuItem(text = { Text(stringResource(R.string.language_spanish)) }, onClick = {
                            languageMenu = false
                            selectedLanguage = "es"
                        })
                    }
                }
                Text(
                    "[debug: ${detectedSystemLanguageLabel()}]",
                    style = MaterialTheme.typography.labelSmall,
                    color = Muted
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(state.excludeVoice, { vm.settings(it, state.includeWav, granted()) }); Text(stringResource(R.string.exclude_voice_notes)) }
            Text(stringResource(R.string.voice_filter_hint), style = MaterialTheme.typography.bodySmall)
            Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(state.includeWav, { vm.settings(state.excludeVoice, it, granted()) }, enabled = !state.onlyMp3); Text(stringResource(R.string.include_wav)) }
            HorizontalDivider()
            Text(stringResource(R.string.authorized_folders, state.folders))
            TextButton(onClick = { folder.launch(null) }) { Text(stringResource(R.string.add_folder)) }
            TextButton(onClick = { vm.clearFolders(granted()) }) { Text(stringResource(R.string.forget_folders)) }
            HorizontalDivider()
            Text(stringResource(R.string.app_about), style = MaterialTheme.typography.bodySmall)
        }
    }, confirmButton = {
        TextButton(onClick = {
            applyAppLanguage(context, selectedLanguage)
            settings = false
            (context as? ComponentActivity)?.recreate()
        }) { Text(stringResource(R.string.done)) }
    })
}

@Composable
internal fun modeLabel(mode: VisualMode): String = when (mode) {
    VisualMode.AUTO -> stringResource(R.string.visual_mode_auto)
    VisualMode.WALKMAN -> stringResource(R.string.visual_mode_walkman)
    VisualMode.DECK -> stringResource(R.string.visual_mode_deck)
    VisualMode.DESKTOP -> stringResource(R.string.visual_mode_desktop)
    VisualMode.FULLSCREEN -> stringResource(R.string.visual_mode_fullscreen)
}

@Composable
private fun ListeningHeading(mode: VisualMode) {
    Column(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 10.dp)) {
        Eyebrow(stringResource(R.string.listening_heading, modeLabel(mode).uppercase()), Muted)
    }
}

@Composable
internal fun AppHeader(compact: Boolean, settings: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = if (compact) 0.dp else 8.dp), verticalAlignment = Alignment.CenterVertically) {
        TapeMark(Modifier.size(28.dp))
        Text(stringResource(R.string.app_name), color = Paper, fontWeight = FontWeight.Black, fontSize = if (compact) 23.sp else 32.sp, modifier = Modifier.weight(1f).padding(start = 10.dp))
        TextButton(onClick = settings) { Text(stringResource(R.string.settings)) }
    }
}

@Composable
internal fun AdaptiveModeBar(mode: VisualMode, compact: Boolean, change: (VisualMode) -> Unit) {
    if (!compact) ModeBar(mode, change)
    else Row(Modifier.fillMaxWidth().metalPanel(4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        VisualMode.entries.filter { it != VisualMode.AUTO }.forEach { item ->
            TextButton(onClick = { change(item) }) { Text(modeLabel(item), color = if (mode == item) Amber else Muted, fontSize = 11.sp) }
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
                Column(Modifier.weight(1f).padding(end = 8.dp)) {
                    val deviceLabel = if (portable) stringResource(R.string.device_portable) else stringResource(R.string.device_stereo_deck)
                    Eyebrow(stringResource(R.string.player_header_tag, deviceLabel), Paper)
                    Eyebrow(stringResource(R.string.player_spec_tag, stringResource(R.string.device_high_fidelity)), Muted)
                }
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
                TransportKey(stringResource(R.string.previous), "previous", false, state.currentUri.isNotEmpty(), Modifier.weight(1f)) { vm?.previous() }
                TransportKey(if (state.playing) stringResource(R.string.pause) else stringResource(R.string.play), if (state.playing) "pause" else "play", true, state.currentUri.isNotEmpty(), Modifier.weight(1.2f)) { vm?.toggle() }
                TransportKey(stringResource(R.string.next), "next", false, state.currentUri.isNotEmpty(), Modifier.weight(1f)) { vm?.next() }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                FilterChip(selected = state.shuffle, onClick = { vm?.toggleShuffle() }, enabled = state.connected,
                    label = { Text(if (state.shuffle) stringResource(R.string.shuffle_on) else stringResource(R.string.shuffle_off), fontSize = 12.sp) },
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
        Eyebrow(if (state.currentUri.isEmpty()) stringResource(R.string.ready_for_first_tape) else if (state.playing) stringResource(R.string.now_playing_label) else stringResource(R.string.paused_label), Color(0xFFABA79C))
        Text(if (state.currentUri.isEmpty()) stringResource(R.string.choose_song) else state.title.ifBlank { stringResource(R.string.default_title) }, color = Color(0xFFEAE0CB), fontSize = 22.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(if (state.currentUri.isEmpty()) stringResource(R.string.collection_waiting) else state.artist.ifBlank { stringResource(R.string.unknown_artist) }, color = Color(0xFFABA79C), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun CompactPlayer(state: PlayerState, vm: PlayerViewModel?, dense: Boolean, showTape: Boolean = true) {
    val previousLabel = stringResource(R.string.previous)
    val nextLabel = stringResource(R.string.next)
    val shuffleLabel = stringResource(R.string.shuffle)
    val playLabel = stringResource(R.string.play)
    val pauseLabel = stringResource(R.string.pause)
    Column(Modifier.fillMaxWidth().metalPanel(8.dp).padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(state.title.ifBlank { stringResource(R.string.default_title) }, color = Paper, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (!dense) Text(state.artist.ifBlank { stringResource(R.string.unknown_artist) }, color = Muted, fontSize = 11.sp, maxLines = 1)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!dense && showTape) Box(Modifier.width(90.dp)) { Cassette(state) }
            SpectrumView(state.playing, Modifier.weight(1f), compact = true, dense = dense)
        }
        Progress(state, vm)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            IconButton(modifier = Modifier.semantics { contentDescription = previousLabel }, onClick = { vm?.previous() }, enabled = state.currentUri.isNotEmpty()) { TransportIcon("previous", Paper, Modifier.size(20.dp)) }
            TextButton(onClick = { vm?.toggle() }, enabled = state.currentUri.isNotEmpty()) { Text(if (state.playing) pauseLabel else playLabel, color = Amber) }
            IconButton(modifier = Modifier.semantics { contentDescription = nextLabel }, onClick = { vm?.next() }, enabled = state.currentUri.isNotEmpty()) { TransportIcon("next", Paper, Modifier.size(20.dp)) }
            IconToggleButton(modifier = Modifier.semantics { contentDescription = shuffleLabel }, checked = state.shuffle, onCheckedChange = { vm?.toggleShuffle() }, enabled = state.connected) {
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
            Text(stringResource(R.string.collection_label, state.tracks.size), color = Paper, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Box {
                TextButton(onClick = { sources = true }) { Text(stringResource(R.string.sources)) }
                DropdownMenu(expanded = sources, onDismissRequest = { sources = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.add_folder)) }, onClick = { sources = false; folder() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.update_library)) }, enabled = !state.scanning, onClick = { sources = false; scan() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.grant_access)) }, onClick = { sources = false; permission() })
                }
            }
        }
        OutlinedTextField(value = query, onValueChange = change, modifier = Modifier.fillMaxWidth(), singleLine = true,
            placeholder = { Text(stringResource(R.string.search_placeholder), fontSize = 13.sp) }, shape = RoundedCornerShape(12.dp),
            trailingIcon = { if (query.isNotEmpty()) TextButton(onClick = { change("") }) { Text(stringResource(R.string.clear_query)) } })
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
            Text(track.artist.ifBlank { stringResource(R.string.unknown_artist) }, color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
        Text(if (scanning) stringResource(R.string.searching_more) else if (searching) stringResource(R.string.search_no_results) else stringResource(R.string.empty_collection), color = Paper, fontSize = 18.sp)
        Text(if (searching) stringResource(R.string.try_other_query) else stringResource(R.string.empty_collection_hint), color = Muted, fontSize = 13.sp)
    }
}

@Composable
internal fun Eyebrow(text: String, color: Color = Muted) { Text(text, color = color, fontSize = 9.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
private fun time(ms: Long): String = "%d:%02d".format(ms.coerceAtLeast(0) / 60000, (ms.coerceAtLeast(0) / 1000) % 60)





