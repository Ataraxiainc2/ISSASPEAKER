package com.issasoda.issaspeaker.speaker

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.io.RandomAccessFile
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.security.SecureRandom

/**
 * Hands one audio file to the speaker (catalog F022, threat model 5.4). A Cast speaker can only play audio it fetches
 * from a URL, so for the length of one announcement the phone listens:
 *
 * - on the Wi-Fi address only (never every interface), on a random port chosen by the system;
 * - for exactly one path with 128 random bits in it; any other request is dropped without an answer;
 * - for GET and HEAD only, with byte ranges (the speaker's player asks for them); nothing can be uploaded;
 * - until [close], which the caller does as soon as the announcement ends (and at most a couple of minutes later).
 */
class AudioServer private constructor(private val socket: ServerSocket, private val file: File, private val path: String) : Closeable {

    /** What the speaker is told to fetch. */
    val url: String = "http://${socket.inetAddress.hostAddress}:${socket.localPort}$path"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private fun start() {
        scope.launch {
            while (!socket.isClosed) {
                val client = try {
                    socket.accept()
                } catch (_: SocketException) {
                    break // closed
                }
                launch { client.use { serve(it) } }
            }
        }
    }

    private fun serve(client: Socket) {
        client.soTimeout = CLIENT_TIMEOUT_MS
        val request = try {
            HttpRequestHead.read(client.getInputStream())
        } catch (_: IOException) {
            null
        } ?: return
        if (request.path != path || request.method !in ALLOWED_METHODS) return
        try {
            respond(client.getOutputStream(), request)
        } catch (_: IOException) {
            // The speaker hung up mid-transfer (it often re-requests a range); nothing to do.
        }
    }

    private fun respond(out: OutputStream, request: HttpRequestHead) {
        val size = file.length()
        val range = HttpRequestHead.parseRange(request.range, size)
        val (status, first, last) = if (range != null) Triple("206 Partial Content", range.first, range.last) else Triple("200 OK", 0L, size - 1)
        val headers = buildString {
            append("HTTP/1.1 $status\r\n")
            append("Content-Type: $CONTENT_TYPE\r\n")
            append("Content-Length: ${last - first + 1}\r\n")
            append("Accept-Ranges: bytes\r\n")
            if (range != null) append("Content-Range: bytes $first-$last/$size\r\n")
            append("Cache-Control: no-store\r\n")
            append("Connection: close\r\n\r\n")
        }
        out.write(headers.toByteArray(Charsets.ISO_8859_1))
        if (request.method == "GET") copy(out, first, last - first + 1)
        out.flush()
    }

    private fun copy(out: OutputStream, offset: Long, length: Long) {
        RandomAccessFile(file, "r").use { input ->
            input.seek(offset)
            val buffer = ByteArray(BUFFER_BYTES)
            var left = length
            while (left > 0) {
                val read = input.read(buffer, 0, minOf(buffer.size.toLong(), left).toInt())
                if (read < 0) break
                out.write(buffer, 0, read)
                left -= read
            }
        }
    }

    override fun close() {
        socket.close()
        scope.cancel()
    }

    companion object {
        const val CONTENT_TYPE = "audio/wav"
        private val ALLOWED_METHODS = setOf("GET", "HEAD")
        private const val BACKLOG = 4
        private const val TOKEN_BYTES = 16
        private const val CLIENT_TIMEOUT_MS = 10_000
        private const val BUFFER_BYTES = 16 * 1024

        /** Starts serving [file] on [address] (the phone's Wi-Fi address). */
        fun open(file: File, address: InetAddress): AudioServer {
            val token = ByteArray(TOKEN_BYTES).also { SecureRandom().nextBytes(it) }.joinToString("") { "%02x".format(it) }
            return AudioServer(ServerSocket(0, BACKLOG, address), file, "/a/$token.wav").also { it.start() }
        }
    }
}
