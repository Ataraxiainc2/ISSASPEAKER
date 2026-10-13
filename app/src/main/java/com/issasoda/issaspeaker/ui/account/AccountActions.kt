package com.issasoda.issaspeaker.ui.account

/** What the account controls report back to the screen: a short status line, or nothing. */
data class AccountActions(
    val busy: Boolean,
    val message: String?,
    val onSignIn: () -> Unit,
    val onSignOut: () -> Unit,
    val onEraseEverything: () -> Unit,
)
