package com.issasoda.issaspeaker

import com.issasoda.issaspeaker.data.account.IdTokenClaims
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class IdTokenClaimsTest {

    private fun token(payload: String): String {
        fun b64(text: String) = Base64.getUrlEncoder().withoutPadding().encodeToString(text.toByteArray())
        return b64("""{"alg":"RS256"}""") + "." + b64(payload) + ".signature"
    }

    @Test
    fun readsVerifiedEmailAndNonce() {
        val claims = IdTokenClaims.parse(token("""{"email":"a@b.c","email_verified":true,"nonce":"abc-123"}"""))!!
        assertTrue(claims.emailVerified)
        assertEquals("abc-123", claims.nonce)
    }

    @Test
    fun verifiedAsStringIsAccepted() {
        assertTrue(IdTokenClaims.parse(token("""{"email_verified":"true"}"""))!!.emailVerified)
    }

    @Test
    fun missingOrFalseVerificationIsNotVerified() {
        assertFalse(IdTokenClaims.parse(token("""{"email":"a@b.c"}"""))!!.emailVerified)
        assertFalse(IdTokenClaims.parse(token("""{"email_verified":false}"""))!!.emailVerified)
    }

    @Test
    fun malformedTokensAreRejected() {
        assertNull(IdTokenClaims.parse("not-a-jwt"))
        assertNull(IdTokenClaims.parse("a.b"))
        assertNull(IdTokenClaims.parse("a.%%%.c"))
        assertNull(IdTokenClaims.parse(token("plain text")))
    }

    @Test
    fun missingNonceIsNull() {
        assertNull(IdTokenClaims.parse(token("""{"email_verified":true}"""))!!.nonce)
    }
}
