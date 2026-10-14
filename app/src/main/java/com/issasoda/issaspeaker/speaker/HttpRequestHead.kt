package com.issasoda.issaspeaker.speaker

import java.io.InputStream

/** The parts of an HTTP request the audio server looks at. Everything else in the request is ignored. */
data class HttpRequestHead(val method: String, val path: String, val range: String?) {

    companion object {
        private const val MAX_LINE = 2048
        private const val MAX_HEADERS = 64

        /** Reads the request line and headers; null when the request is malformed or oversized. */
        fun read(input: InputStream): HttpRequestHead? {
            val requestLine = readLine(input) ?: return null
            val parts = requestLine.split(' ')
            if (parts.size != 3 || !parts[2].startsWith("HTTP/")) return null
            var range: String? = null
            repeat(MAX_HEADERS) {
                val line = readLine(input) ?: return null
                if (line.isEmpty()) return HttpRequestHead(parts[0], parts[1], range)
                val name = line.substringBefore(':').trim()
                if (name.equals("Range", ignoreCase = true)) range = line.substringAfter(':').trim()
            }
            return null
        }

        /**
         * "bytes=a-b", "bytes=a-" or "bytes=-n" → the byte range within [size]; null for no range or one the server
         * does not support (several ranges, out of bounds), in which case the whole file is sent.
         */
        fun parseRange(header: String?, size: Long): LongRange? {
            val spec = header?.removePrefix("bytes=")?.takeIf { header.startsWith("bytes=") && ',' !in it } ?: return null
            val start = spec.substringBefore('-').trim()
            val end = spec.substringAfter('-').trim()
            val range = when {
                start.isEmpty() -> end.toLongOrNull()?.let { (size - it).coerceAtLeast(0)..(size - 1) }
                else -> start.toLongOrNull()?.let { from -> from..(end.toLongOrNull()?.coerceAtMost(size - 1) ?: (size - 1)) }
            }
            return range?.takeIf { size > 0 && it.first in 0..it.last && it.last < size }
        }

        /** One CRLF- or LF-terminated line in ISO-8859-1, at most [MAX_LINE] bytes. */
        private fun readLine(input: InputStream): String? {
            val bytes = StringBuilder()
            while (bytes.length <= MAX_LINE) {
                when (val b = input.read()) {
                    -1 -> return null
                    '\n'.code -> return bytes.toString().trimEnd('\r')
                    else -> bytes.append(b.toChar())
                }
            }
            return null
        }
    }
}
