package com.issasoda.issaspeaker.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.issasoda.issaspeaker.R
import com.issasoda.issaspeaker.data.account.Account
import com.issasoda.issaspeaker.ui.components.AccountPhoto
import com.issasoda.issaspeaker.ui.theme.Sizes
import com.issasoda.issaspeaker.ui.theme.Spacing

/** The account sheet opened from the top-bar photo: who is signed in, and the account actions. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSheet(account: Account?, actions: AccountActions, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        AccountPanel(
            account = account,
            actions = actions,
            modifier = Modifier.padding(start = Spacing.xl, end = Spacing.xl, bottom = Spacing.xxl),
        )
    }
}

/** Shared by the sheet and the Settings screen, so both show exactly the same account controls. */
@Composable
fun AccountPanel(account: Account?, actions: AccountActions, modifier: Modifier = Modifier) {
    var confirmErase by remember { mutableStateOf(false) }
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        AccountPhoto(account, Sizes.profilePhoto)
        if (account == null) {
            Text(stringResource(R.string.account_signed_out_title), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(R.string.account_signed_out_body),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            Button(onClick = actions.onSignIn, enabled = !actions.busy, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(if (actions.busy) R.string.account_signing_in else R.string.account_sign_in))
            }
        } else {
            Text(account.displayName ?: account.email, style = MaterialTheme.typography.titleLarge)
            Text(account.email, style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(onClick = actions.onSignOut, enabled = !actions.busy, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.account_sign_out))
            }
        }
        actions.message?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        TextButton(
            onClick = { confirmErase = true },
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) { Text(stringResource(R.string.account_erase)) }
    }
    if (confirmErase) {
        AlertDialog(
            onDismissRequest = { confirmErase = false },
            title = { Text(stringResource(R.string.account_erase_title)) },
            text = { Text(stringResource(R.string.account_erase_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmErase = false
                        actions.onEraseEverything()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.account_erase_confirm)) }
            },
            dismissButton = { TextButton(onClick = { confirmErase = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}
