package com.issasoda.issaspeaker

import com.issasoda.issaspeaker.ai.AiSpeech
import com.issasoda.issaspeaker.ai.AiSpeechApi
import com.issasoda.issaspeaker.ai.AiVoices
import com.issasoda.issaspeaker.speaker.VoiceChoice
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class VoicesTest {

    @Test
    fun `the speech model is picked from the key's own list`() {
        val openAi = listOf("gpt-4o", "tts-1", "tts-1-hd", "gpt-4o-mini-tts", "gpt-realtime-2.1", "whisper-1")
        assertEquals("gpt-4o-mini-tts", AiVoices.preferredModel(AiSpeechApi.OPENAI_SPEECH, openAi))
        assertEquals("tts-1-hd", AiVoices.preferredModel(AiSpeechApi.OPENAI_SPEECH, listOf("tts-1", "tts-1-hd")))
        val gemini = listOf("gemini-2.5-pro-preview-tts", "gemini-2.5-flash-preview-tts", "gemini-3.1-flash-tts-preview", "gemini-2.5-flash")
        assertEquals("gemini-3.1-flash-tts-preview", AiVoices.preferredModel(AiSpeechApi.GEMINI, gemini))
        assertNull(AiVoices.preferredModel(AiSpeechApi.GEMINI, listOf("gemini-2.5-flash")))
        assertNull(AiVoices.apiFor("deepseek"))
    }

    @Test
    fun `tts-1 offers only its nine voices`() {
        assertEquals(9, AiVoices.voices(AiSpeechApi.OPENAI_SPEECH, "tts-1").size)
        assertEquals(13, AiVoices.voices(AiSpeechApi.OPENAI_SPEECH, "gpt-4o-mini-tts").size)
        assertEquals(30, AiVoices.voices(AiSpeechApi.GEMINI, "gemini-3.1-flash-tts-preview").size)
    }

    @Test
    fun `PCM is wrapped in a valid WAV header`() {
        val pcm = ByteArray(480) { it.toByte() }
        val wav = AiSpeech.pcmToWav(pcm, 24_000)
        val header = ByteBuffer.wrap(wav).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals("RIFF", String(wav, 0, 4))
        assertEquals(36 + pcm.size, header.getInt(4))
        assertEquals("WAVE", String(wav, 8, 4))
        assertEquals(24_000, header.getInt(24))
        assertEquals(48_000, header.getInt(28))
        assertEquals(pcm.size, header.getInt(40))
        assertArrayEquals(pcm, wav.copyOfRange(44, wav.size))
    }

    @Test
    fun `voice choices survive being stored`() {
        listOf(VoiceChoice.Automatic, VoiceChoice.Phone("es-us-x-esc-local"), VoiceChoice.Ai("google", "gemini-3.1-flash-tts-preview", "Kore"))
            .forEach { assertEquals(it, VoiceChoice.decode(it.encode())) }
        assertEquals(VoiceChoice.Phone("es-us-x-esc-local"), VoiceChoice.decode("es-us-x-esc-local"))
        assertEquals(VoiceChoice.Automatic, VoiceChoice.decode("ai:broken"))
    }
}
