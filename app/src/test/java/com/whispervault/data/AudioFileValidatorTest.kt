package com.whispervault.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioFileValidatorTest {
    @Test
    fun acceptsKnownExtensions() {
        listOf("mp3", "wav", "m4a", "flac", "aac", "ogg", "MP3", "WAV").forEach {
            assertTrue(it, AudioFileValidator.isSupportedExtension(it))
        }
    }

    @Test
    fun rejectsUnknownExtensions() {
        assertFalse(AudioFileValidator.isSupportedExtension("txt"))
        assertFalse(AudioFileValidator.isSupportedExtension("exe"))
    }

    @Test
    fun acceptsAudioMimeTypes() {
        assertTrue(AudioFileValidator.isSupportedMime("audio/mpeg"))
        assertTrue(AudioFileValidator.isSupportedMime("audio/ogg"))
        assertTrue(AudioFileValidator.isSupported("track.bin", "audio/flac"))
    }

    @Test
    fun rejectsNonAudioWithoutExtension() {
        assertFalse(AudioFileValidator.isSupported("notes.txt", "text/plain"))
    }
}
