package org.sublaky.player

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.sublaky.player.data.MusicRepository
import org.sublaky.player.domain.*
import org.sublaky.player.playback.PlaybackService

data class PlayerState(
    val tracks: List<Track> = emptyList(), val scanning: Boolean = false,
    val connected: Boolean = false, val playing: Boolean = false, val shuffle: Boolean = false,
    val currentUri: String = "", val album: String = "", val artwork: Bitmap? = null,
    val title: String = "Tu música, a tu ritmo", val artist: String = "Biblioteca local",
    val position: Long = 0, val duration: Long = 0, val error: String? = null,
    val mode: VisualMode = VisualMode.AUTO, val excludeVoice: Boolean = true,
    val includeWav: Boolean = false, val onlyMp3: Boolean = true, val folders: Int = 0
)

class PlayerViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("sublaky", 0)
    private val repository = MusicRepository(app)
    private val mutable = MutableStateFlow(PlayerState(
        mode = VisualMode.entries.firstOrNull { it.name == prefs.getString("mode", "AUTO") } ?: VisualMode.AUTO,
        excludeVoice = prefs.getBoolean("voice", true), includeWav = prefs.getBoolean("wav", false),
        onlyMp3 = prefs.getBoolean("onlyMp3", true),
        folders = trees().size
    ))
    val state = mutable.asStateFlow()
    private var controller: MediaController? = null
    private var scanJob: Job? = null
    private var artworkJob: Job? = null
    private var artworkUri: String? = null
    private val future = MediaController.Builder(app, SessionToken(app, ComponentName(app, PlaybackService::class.java))).buildAsync()
    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) { refresh() }
        override fun onPlayerError(error: PlaybackException) { mutable.update { it.copy(error = "No se pudo reproducir este archivo: ${error.errorCodeName}") } }
    }
    init {
        future.addListener({
            try {
                controller = future.get().also { it.addListener(listener) }
                refresh()
            } catch (_: Exception) { mutable.update { it.copy(error = "No se pudo conectar el reproductor.") } }
        }, ContextCompat.getMainExecutor(app))
        viewModelScope.launch { while (true) { refresh(); delay(500) } }
    }
    private fun trees(): Set<String> = prefs.getStringSet("trees", emptySet()).orEmpty().toSet()
    private fun refresh() {
        val p = controller ?: return
        val uri = p.currentMediaItem?.mediaId.orEmpty()
        mutable.update { it.copy(connected = true, playing = p.isPlaying, shuffle = p.shuffleModeEnabled,
            currentUri = uri, album = p.mediaMetadata.albumTitle?.toString().orEmpty(),
            title = p.mediaMetadata.title?.toString() ?: "Tu música, a tu ritmo",
            artist = p.mediaMetadata.artist?.toString().orEmpty(),
            position = p.currentPosition.coerceAtLeast(0), duration = p.duration.coerceAtLeast(0)) }
        if (artworkUri != uri) {
            artworkUri = uri
            artworkJob?.cancel()
            mutable.update { it.copy(artwork = null) }
            if (uri.isNotEmpty()) artworkJob = viewModelScope.launch {
                val bitmap = withContext(Dispatchers.IO) {
                    val reader = MediaMetadataRetriever()
                    try {
                        reader.setDataSource(getApplication<Application>(), Uri.parse(uri))
                        reader.embeddedPicture?.let { bytes ->
                            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                            val options = BitmapFactory.Options().apply {
                                inSampleSize = 1
                                while (bounds.outWidth / inSampleSize > 600 || bounds.outHeight / inSampleSize > 600) inSampleSize *= 2
                            }
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                        }
                    } catch (_: Exception) { null } finally { reader.release() }
                }
                if (artworkUri == uri) mutable.update { it.copy(artwork = bitmap) }
            }
        }
    }
    fun scan(granted: Boolean) {
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            mutable.update { it.copy(scanning = true, error = null) }
            try {
                val s = state.value
                val policy = ScanPolicy(excludeVoice = s.excludeVoice).withFormats(s.onlyMp3, s.includeWav)
                val tracks = repository.scan(policy, granted, trees())
                mutable.update { it.copy(tracks = tracks, scanning = false) }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { mutable.update { it.copy(scanning = false, error = "No se pudo leer la biblioteca. Revisa los permisos o vuelve a elegir la carpeta.") } }
        }
    }
    fun addFolder(uri: Uri, granted: Boolean) {
        try {
            getApplication<Application>().contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val updated = trees() + uri.toString()
            prefs.edit().putStringSet("trees", updated).apply()
            mutable.update { it.copy(folders = updated.size) }
            scan(granted)
        } catch (_: SecurityException) { mutable.update { it.copy(error = "Esta carpeta no permite conservar el acceso.") } }
    }
    fun clearFolders(granted: Boolean) {
        trees().forEach { uri ->
            try { getApplication<Application>().contentResolver.releasePersistableUriPermission(Uri.parse(uri), Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            catch (_: SecurityException) { }
        }
        prefs.edit().remove("trees").apply()
        mutable.update { it.copy(folders = 0) }
        scan(granted)
    }
    fun settings(voice: Boolean, wav: Boolean, granted: Boolean) {
        prefs.edit().putBoolean("voice", voice).putBoolean("wav", wav).apply()
        mutable.update { it.copy(excludeVoice = voice, includeWav = wav) }
        scan(granted)
    }

    fun onlyMp3(enabled: Boolean, granted: Boolean) {
        prefs.edit().putBoolean("onlyMp3", enabled).apply()
        mutable.update { it.copy(onlyMp3 = enabled) }
        scan(granted)
    }
    fun mode(mode: VisualMode) { prefs.edit().putString("mode", mode.name).apply(); mutable.update { it.copy(mode = mode) } }
    fun play(track: Track) {
        val p = controller ?: return
        val tracks = state.value.tracks
        val index = tracks.indexOfFirst { it.uri == track.uri }
        if (index < 0) return
        mutable.update { it.copy(error = null) }
        p.setMediaItems(tracks.map { MediaItem.Builder().setMediaId(it.uri).setUri(it.uri)
            .setMediaMetadata(MediaMetadata.Builder().setTitle(it.title).setArtist(it.artist).setAlbumTitle(it.album).build()).build() }, index, 0)
        p.prepare(); p.play()
    }
    fun toggle() { controller?.let { if (it.isPlaying) it.pause() else it.play() } }
    fun toggleShuffle() { controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled } }
    fun previous() { controller?.seekToPrevious() }
    fun next() { controller?.seekToNext() }
    fun seek(value: Long) { controller?.seekTo(value) }
    override fun onCleared() {
        controller?.removeListener(listener)
        MediaController.releaseFuture(future)
        super.onCleared()
    }
}
