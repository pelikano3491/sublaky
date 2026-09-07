package org.sublaky.player.domain

data class Track(
    val uri: String,
    val title: String,
    val artist: String = "",
    val album: String = "",
    val durationMs: Long = 0,
    val fileName: String = "",
    val folder: String = ""
)

data class ScanPolicy(
    val extensions: Set<String> = setOf("mp3", "m4a", "aac", "flac", "ogg", "opus"),
    val excludeVoice: Boolean = true
) {
    fun withFormats(onlyMp3: Boolean, includeWav: Boolean): ScanPolicy = copy(
        extensions = if (onlyMp3) setOf("mp3") else if (includeWav) extensions + "wav" else extensions
    )

    fun accepts(track: Track): Boolean {
        if (track.fileName.substringAfterLast('.', "").lowercase() !in extensions) return false
        val location = track.folder.lowercase()
        val name = track.fileName.lowercase()
        val voiceLocation = listOf("voice", "voice notes", "notas de voz").any(location::contains)
        val voiceName = name.startsWith("ptt-") || name.startsWith("audio_") || name.startsWith("voice")
        val missingTags = track.artist.isBlank() && track.album.isBlank()
        return !(excludeVoice && missingTags && (voiceLocation || voiceName))
    }
}

enum class VisualMode(val label: String) {
    AUTO("Auto"), WALKMAN("Walkman"), DECK("Deck"), DESKTOP("Escritorio"), FULLSCREEN("Pantalla completa")
}
