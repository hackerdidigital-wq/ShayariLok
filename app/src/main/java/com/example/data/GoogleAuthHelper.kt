package com.example.data

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.MutableContextWrapper
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object GoogleAuthHelper {

  private const val TAG = "Auth"
  private val interactiveSignInActive = AtomicBoolean(false)

  fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth -> trySend(auth.currentUser) }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
  }

  internal tailrec fun Context.findActivity(): Activity? =
    when (this) {
      is Activity -> this
      is ContextWrapper -> baseContext?.findActivity()
      else -> null
    }

  internal fun resolveWebClientId(context: Context): String? =
    runCatching { context.getString(R.string.default_web_client_id).trim() }
      .getOrNull()
      ?.takeIf { it.isNotEmpty() }

  fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope,
  ) {
    if (Firebase.auth.currentUser != null) {
      onAuthSuccess()
      return
    }
    val clientId =
      resolveWebClientId(context)
        ?: run {
          onUnauthenticated()
          return
        }

    val activity = context.findActivity()
    val credentialContext: Context =
      if (activity != null) MutableContextWrapper(activity) else context
    val executionScope = (activity as? LifecycleOwner)?.lifecycleScope ?: scope

    val googleIdOption =
      GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()

    val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

    executionScope.launch {
      try {
        val result = credentialManager.getCredential(credentialContext, request)
        val credential = result.credential
        if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
          val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
          val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
          Firebase.auth.signInWithCredential(authCredential).await()
          onAuthSuccess()
        } else {
          onUnauthenticated()
        }
      } catch (e: CancellationException) {
        throw e
      } catch (_: Exception) {
        onUnauthenticated()
      }
    }
  }

  fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {},
  ) {
    val clientId =
      resolveWebClientId(context)
        ?: run {
          onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
          return
        }

    val activity = context.findActivity()
    if (activity == null || activity.isFinishing || activity.isDestroyed) {
      onAuthError("Google Sign-In requires an active Activity window")
      return
    }

    if (!interactiveSignInActive.compareAndSet(false, true)) {
      return
    }

    val mutableActivityContext = MutableContextWrapper(activity)
    val executionScope = (activity as? LifecycleOwner)?.lifecycleScope ?: scope

    val primarySignInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val primaryRequest = GetCredentialRequest.Builder().addCredentialOption(primarySignInOption).build()

    executionScope.launch {
      try {
        val result =
          executeInteractiveCredentialRequest(
            credentialManager = credentialManager,
            context = mutableActivityContext,
            primaryRequest = primaryRequest,
            clientId = clientId,
          )
        val credential = result.credential
        if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
          val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
          val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
          Firebase.auth.signInWithCredential(authCredential).await()
          onAuthSuccess()
        } else {
          onAuthError("Unexpected credential type received")
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: GetCredentialCancellationException) {
        Log.w(TAG, "Google Sign-In cancelled or dismissed: ${e.errorMessage}", e)
        val diagnostic = classifyCancellationDiagnostic(e)
        if (diagnostic != null) {
          onAuthError(diagnostic)
        } else {
          onAuthCancelled()
        }
      } catch (e: NoCredentialException) {
        Log.w(TAG, "No Google credential available on device: ${e.errorMessage}", e)
        onAuthError(
          "No Google account found on this device. Please add a Google account in device Settings and try again."
        )
      } catch (e: GetCredentialProviderConfigurationException) {
        Log.e(TAG, "Credential provider configuration missing", e)
        onAuthError("Google Play Services Credential Provider is unavailable on this device.")
      } catch (e: GetCredentialInterruptedException) {
        Log.w(TAG, "Google Sign-In interrupted: ${e.errorMessage}", e)
        onAuthError("Google Sign-In was interrupted. Please tap Sign in with Google again.")
      } catch (e: Exception) {
        Log.e(TAG, "Google Sign-In failed", e)
        onAuthError(e.localizedMessage ?: "Google Sign-In failed. Please try again.")
      } finally {
        interactiveSignInActive.set(false)
      }
    }
  }

  private suspend fun executeInteractiveCredentialRequest(
    credentialManager: CredentialManager,
    context: Context,
    primaryRequest: GetCredentialRequest,
    clientId: String,
  ): GetCredentialResponse {
    return try {
      credentialManager.getCredential(context, primaryRequest)
    } catch (e: GetCredentialInterruptedException) {
      Log.w(TAG, "Google Sign-In interrupted on first attempt; retrying once: ${e.errorMessage}", e)
      delay(250L)
      credentialManager.getCredential(context, primaryRequest)
    } catch (e: NoCredentialException) {
      Log.w(TAG, "GetSignInWithGoogleOption returned NoCredentialException; trying GetGoogleIdOption fallback", e)
      val fallbackOption =
        GetGoogleIdOption.Builder()
          .setFilterByAuthorizedAccounts(false)
          .setServerClientId(clientId)
          .setAutoSelectEnabled(false)
          .build()
      val fallbackRequest = GetCredentialRequest.Builder().addCredentialOption(fallbackOption).build()
      credentialManager.getCredential(context, fallbackRequest)
    }
  }

  internal fun classifyCancellationDiagnostic(e: GetCredentialException): String? {
    val msg = e.errorMessage?.toString()?.trim().orEmpty()
    if (msg.isEmpty()) return null
    val lower = msg.lowercase()
    return when {
      lower.contains("28444") || lower.contains("developer console") || lower.contains("10:") ->
        "Google Sign-In setup mismatch (SHA-1 fingerprint or Web Client ID). Add this build's SHA-1 in Firebase Console ($msg)."
      lower.contains("28436") || lower.contains("temporarily blocked") ->
        "Too many failed/cancelled Google prompts temporarily paused sign-in. Wait a minute or clear Google Play Services cache and try again."
      lower.contains("cannot find a matching credential") || lower.contains("16:") ->
        "No matching Google credential found ($msg). Ensure a Google account is logged in on this device."
      else -> null
    }
  }

  fun signOut(
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onSignOutComplete: () -> Unit = {},
  ) {
    scope.launch {
      withContext(NonCancellable) {
        try {
          credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
          Log.w(TAG, "Failed to clear credential state: ${e.message}")
        } finally {
          Firebase.auth.signOut()
          onSignOutComplete()
        }
      }
    }
  }
}
