package org.sublaky.player.domain

import org.junit.Assert.*
import org.junit.Test

class ScanPolicyTest {
    @Test fun mp3OnlyOverridesWavAndOtherFormats() {
        val policy = ScanPolicy().withFormats(onlyMp3 = true, includeWav = true)
        assertTrue(policy.accepts(track("song.MP3")))
        listOf("wav", "flac", "m4a", "aac", "ogg", "opus").forEach {
            assertFalse(policy.accepts(track("song.$it")))
        }
        assertEquals(setOf("mp3"), policy.extensions)
    }
    @Test fun disablingMp3OnlyRestoresSupportedFormats() {
        val policy = ScanPolicy().withFormats(onlyMp3 = false, includeWav = true)
        assertTrue(policy.accepts(track("song.flac")))
        assertTrue(policy.accepts(track("song.wav")))
    }
    private fun track(name: String, folder: String = "Music", artist: String = "") = Track("content://test", "Song", artist, fileName = name, folder = folder)
    @Test fun excludesWavByDefault() { assertFalse(ScanPolicy().accepts(track("song.wav"))) }
    @Test fun acceptsUppercaseMp3() { assertTrue(ScanPolicy().accepts(track("SONG.MP3"))) }
    @Test fun excludesUntaggedVoiceFolder() { assertFalse(ScanPolicy().accepts(track("a.opus", "Telegram/Voice"))) }
    @Test fun preservesTaggedMusicEvenInVoiceFolder() { assertTrue(ScanPolicy().accepts(track("a.opus", "Telegram/Voice", "Artist"))) }
    @Test fun canIncludeVoice() { assertTrue(ScanPolicy(excludeVoice = false).accepts(track("PTT-1.opus"))) }
    @Test fun keepsShortMusic() { assertTrue(ScanPolicy().accepts(track("intro.mp3"))) }
}
