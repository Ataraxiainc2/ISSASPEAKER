package com.issasoda.issaspeaker

import com.issasoda.issaspeaker.admin.AdminPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminPolicyTest {

    @Test
    fun verifiedAdminAddressIsAdmin() {
        assertTrue(AdminPolicy.isAdmin("sat.tecnologias@gmail.com", emailVerified = true))
        assertTrue(AdminPolicy.isAdmin("  SAT.Tecnologias@Gmail.com ", emailVerified = true))
    }

    @Test
    fun unverifiedAddressIsNeverAdmin() {
        assertFalse(AdminPolicy.isAdmin("sat.tecnologias@gmail.com", emailVerified = false))
    }

    @Test
    fun lookAlikesAreNotAdmin() {
        listOf(
            null,
            "",
            "sat.tecnologias@gmail.com.evil.example",
            "xsat.tecnologias@gmail.com",
            "sat.tecnologias@gmail.co",
            "sat.tecnologías@gmail.com",
        ).forEach { assertFalse("$it", AdminPolicy.isAdmin(it, emailVerified = true)) }
    }
}
