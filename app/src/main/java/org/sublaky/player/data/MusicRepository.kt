package org.sublaky.player.data

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.sublaky.player.domain.ScanPolicy
import org.sublaky.player.domain.Track
import kotlin.coroutines.coroutineContext

class MusicRepository(private val context: Context) {
    suspend fun scan(policy: ScanPolicy, mediaPermission: Boolean, trees: Set<String>): List<Track> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<Track>()
        if (mediaPermission) {
            val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            val path = if (Build.VERSION.SDK_INT >= 29) MediaStore.Audio.Media.RELATIVE_PATH else MediaStore.Audio.Media.DATA
            val columns = arrayOf("_id", "title", "artist", "album", "duration", "_display_name", path)
            context.contentResolver.query(collection, columns, null, null, "title ASC")?.use { cursor ->
                while (cursor.moveToNext()) {
                    coroutineContext.ensureActive()
                    fun str(i: Int) = cursor.getString(i)?.takeUnless { it == "<unknown>" }.orEmpty()
                    tracks += Track(ContentUris.withAppendedId(collection, cursor.getLong(0)).toString(), str(1), str(2), str(3), cursor.getLong(4), str(5), str(6))
                }
            }
        }
        for (tree in trees) {
            val root = DocumentFile.fromTreeUri(context, Uri.parse(tree)) ?: continue
            val pending = ArrayDeque<Pair<DocumentFile, String>>()
            pending.add(root to (root.name ?: "Carpeta"))
            while (pending.isNotEmpty()) {
                coroutineContext.ensureActive()
                val (directory, parent) = pending.removeFirst()
                for (file in directory.listFiles()) {
                    val name = file.name ?: continue
                    if (file.isDirectory) pending.add(file to "$parent/$name")
                    else if (name.substringAfterLast('.', "").lowercase() in policy.extensions) {
                        val reader = MediaMetadataRetriever()
                        try {
                            reader.setDataSource(context, file.uri)
                            fun tag(key: Int) = reader.extractMetadata(key).orEmpty()
                            tracks += Track(file.uri.toString(), tag(7).ifBlank { name.substringBeforeLast('.') }, tag(2), tag(1), tag(9).toLongOrNull() ?: 0, name, parent)
                        } catch (_: IllegalArgumentException) {
                            // Unreadable or unsupported documents do not prevent scanning the rest.
                        } catch (_: RuntimeException) {
                            // Some document providers advertise files whose data is unavailable.
                        } finally { reader.release() }
                    }
                }
            }
        }
        tracks.filter(policy::accepts).distinctBy { it.uri }.sortedBy { it.title.lowercase() }
    }
}
