package com.issasoda.issaspeaker.admin

import com.issasoda.issaspeaker.BuildConfig

/**
 * Who sees the admin tools (catalog F026).
 *
 * This is a **user-interface gate, not a security boundary**, and the threat model says so: the app has
 * no server and the admin screens only show what the public repository already shows (the feature catalog
 * and the security audit log). Anyone holding the phone with the admin account signed in is the admin.
 * Nothing secret may ever be placed behind this check alone.
 *
 * Sign-in with Google arrives in M1 (F019). Until then only debug builds show the admin tools.
 */
object AdminPolicy {

    const val ADMIN_EMAIL = "sat.tecnologias@gmail.com"

    /** True only for the admin address, and only once Google has verified it. */
    fun isAdmin(email: String?, emailVerified: Boolean): Boolean =
        emailVerified && email?.trim()?.equals(ADMIN_EMAIL, ignoreCase = true) == true

    fun canOpenAdminTools(signedInEmail: String?, emailVerified: Boolean): Boolean =
        BuildConfig.DEBUG || isAdmin(signedInEmail, emailVerified)
}
