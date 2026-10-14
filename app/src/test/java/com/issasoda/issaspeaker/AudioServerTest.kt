package com.issasoda.issaspeaker

import com.issasoda.issaspeaker.speaker.HttpRequestHead
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AudioServerTest {

    @Test
    fun `byte ranges the speaker asks for`() {
        assertEquals(0L..999L, HttpRequestHead.parseRange("bytes=0-", 1000))
        assertEquals(100L..199L, HttpRequestHead.parseRange("bytes=100-199", 1000))
        assertEquals(900L..999L, HttpRequestHead.parseRange("bytes=-100", 1000))
        assertEquals(500L..999L, HttpRequestHead.parseRange("bytes=500-5000", 1000))
    }

    @Test
    fun `unsupported or impossible ranges send the whole file`() {
        assertNull(HttpRequestHead.parseRange(null, 1000))
        assertNull(HttpRequestHead.parseRange("bytes=0-1,5-9", 1000))
        assertNull(HttpRequestHead.parseRange("bytes=2000-", 1000))
        assertNull(HttpRequestHead.parseRange("bytes=9-3", 1000))
        assertNull(HttpRequestHead.parseRange("items=0-1", 1000))
        assertNull(HttpRequestHead.parseRange("bytes=0-", 0))
    }

    @Test
    fun `request head is read, the Range header found in any case`() {
        val request = "GET /a/abc.wav HTTP/1.1\r\nHost: 192.168.1.5:4000\r\nrange: bytes=0-\r\n\r\n"
        assertEquals(HttpRequestHead("GET", "/a/abc.wav", "bytes=0-"), HttpRequestHead.read(request.byteInputStream()))
    }

    @Test
    fun `malformed, truncated or oversized requests are refused`() {
        assertNull(HttpRequestHead.read("NOT-HTTP\r\n\r\n".byteInputStream()))
        assertNull(HttpRequestHead.read("GET /a HTTP/1.1\r\nHost: x\r\n".byteInputStream()))
        assertNull(HttpRequestHead.read(("GET /" + "a".repeat(5000) + " HTTP/1.1\r\n\r\n").byteInputStream()))
        val manyHeaders = "GET /a HTTP/1.1\r\n" + "X: y\r\n".repeat(100) + "\r\n"
        assertNull(HttpRequestHead.read(manyHeaders.byteInputStream()))
    }
}
