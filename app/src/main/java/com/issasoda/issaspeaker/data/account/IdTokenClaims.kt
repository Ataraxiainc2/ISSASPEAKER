package com.issasoda.issaspeaker.data.account

import java.util.Base64

/**
 * The few claims the app reads from a Google ID token's payload. The token arrives straight from Google
 * Play services over Credential Manager, so the app does not re-verify the signature here; with no backend,
 * these claims only drive the interface (the admin gate is documented as not being a security boundary).
 *
 * Pure Kotlin on purpose (no org.json), so it runs in JVM unit tests.
 */
data class IdTokenClaims(val emailVerified: Boolean, val nonce: String?) {

    companion object {
        /** Null when the token is not a three-part JWT with a readable JSON payload. */
        fun parse(idToken: String): IdTokenClaims? {
            val parts = idToken.split('.')
            if (parts.size != JWT_PARTS) return null
            val payload = runCatching { String(Base64.getUrlDecoder().decode(parts[1].trimEnd('=')), Charsets.UTF_8) }
                .getOrNull() ?: return null
            if (!payload.trimStart().startsWith("{")) return null
            val verified = EMAIL_VERIFIED.find(payload)?.groupValues?.get(1) == "true"
            val nonce = NONCE.find(payload)?.groupValues?.get(1)
            return IdTokenClaims(verified, nonce)
        }

        private const val JWT_PARTS = 3
        private val EMAIL_VERIFIED = Regex("\"email_verified\"\\s*:\\s*\"?(true|false)\"?")
        private val NONCE = Regex("\"nonce\"\\s*:\\s*\"([^\"]*)\"")
    }
}
