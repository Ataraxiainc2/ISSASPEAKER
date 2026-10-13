package com.issasoda.issaspeaker.data.account

import android.content.Context
import androidx.core.content.edit
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.issasoda.issaspeaker.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.SecureRandom
import java.util.Base64

/** The signed-in Google account, as shown in the app. */
data class Account(val email: String, val displayName: String?, val photoUrl: String?, val emailVerified: Boolean)

sealed interface SignInResult {
    data class Success(val account: Account) : SignInResult
    data object Cancelled : SignInResult
    data object NotConfigured : SignInResult
    data class Failed(val reason: String) : SignInResult
}

/**
 * Sign in with Google (Credential Manager) and the signed-in account (catalog F019, F232, F233, F235).
 *
 * Only the profile (email, name, photo URL, verified flag) is kept, in private app storage excluded from
 * backups. The ID token is checked for its nonce and then dropped: nothing here needs to keep it.
 */
class AccountRepository(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    private val credentialManager = CredentialManager.create(appContext)
    private val current = MutableStateFlow(load())

    val account: StateFlow<Account?> = current.asStateFlow()

    /** [activityContext] must be an Activity: Credential Manager shows its sheet on top of it. */
    @Suppress("SwallowedException") // closing the Google sheet is the user's choice, not an error to report
    suspend fun signIn(activityContext: Context): SignInResult {
        val clientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
        if (clientId.isBlank()) return SignInResult.NotConfigured
        val nonce = newNonce()
        val option = GetSignInWithGoogleOption.Builder(clientId).setNonce(nonce).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        return try {
            val credential = credentialManager.getCredential(activityContext, request).credential
            if (credential !is CustomCredential || credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                return SignInResult.Failed("unexpected credential type")
            }
            val google = GoogleIdTokenCredential.createFrom(credential.data)
            val claims = IdTokenClaims.parse(google.idToken)
            if (claims == null || claims.nonce != nonce) return SignInResult.Failed("ID token did not match this request")
            val email = google.email ?: return SignInResult.Failed("no email in the Google credential")
            val account = Account(
                email = email,
                displayName = google.displayName,
                photoUrl = google.profilePictureUri?.toString(),
                emailVerified = claims.emailVerified,
            )
            save(account)
            SignInResult.Success(account)
        } catch (e: GetCredentialCancellationException) {
            SignInResult.Cancelled
        } catch (e: NoCredentialException) {
            SignInResult.Failed("no Google account on this phone: ${e.type}")
        } catch (e: GetCredentialException) {
            SignInResult.Failed(e.type)
        }
    }

    /** Forgets the account here and tells Credential Manager to stop offering automatic sign-in. */
    suspend fun signOut() {
        runCatching { credentialManager.clearCredentialState(ClearCredentialStateRequest()) }
        prefs.edit { clear() }
        current.value = null
    }

    private fun save(account: Account) {
        prefs.edit {
            putString(KEY_EMAIL, account.email)
            putString(KEY_NAME, account.displayName)
            putString(KEY_PHOTO, account.photoUrl)
            putBoolean(KEY_VERIFIED, account.emailVerified)
        }
        current.value = account
    }

    private fun load(): Account? = prefs.getString(KEY_EMAIL, null)?.let { email ->
        Account(email, prefs.getString(KEY_NAME, null), prefs.getString(KEY_PHOTO, null), prefs.getBoolean(KEY_VERIFIED, false))
    }

    private fun newNonce(): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(NONCE_BYTES).also(SecureRandom()::nextBytes))

    private companion object {
        const val FILE = "account"
        const val KEY_EMAIL = "email"
        const val KEY_NAME = "name"
        const val KEY_PHOTO = "photo"
        const val KEY_VERIFIED = "verified"
        const val NONCE_BYTES = 32
    }
}
