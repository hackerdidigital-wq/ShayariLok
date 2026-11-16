package com.example.data

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialCustomException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import java.security.MessageDigest
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
import kotlinx.coroutines.withTimeoutOrNull

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

  @Suppress("DEPRECATION")
  internal fun getAppSigningSha1(context: Context): String? {
    return runCatching {
      val pm = context.packageManager
      val pkgName = context.packageName
      val sigBytes: ByteArray? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
          val pkgInfo = pm.getPackageInfo(pkgName, PackageManager.GET_SIGNING_CERTIFICATES)
          val signingInfo = pkgInfo.signingInfo
          val signers =
            if (signingInfo?.hasMultipleSigners() == true) {
              signingInfo.apkContentsSigners
            } else {
              signingInfo?.signingCertificateHistory
            }
          signers?.firstOrNull()?.toByteArray()
        } else {
          val pkgInfo = pm.getPackageInfo(pkgName, PackageManager.GET_SIGNATURES)
          pkgInfo.signatures?.firstOrNull()?.toByteArray()
        }
      if (sigBytes == null) return@runCatching null
      val digest = MessageDigest.getInstance("SHA-1").digest(sigBytes)
      digest.joinToString(":") { byte -> "%02X".format(byte) }
    }.getOrNull()
  }

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
    val credentialContext: Context = activity ?: context
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
        val result =
          withTimeoutOrNull(4500L) {
            credentialManager.getCredential(credentialContext, request)
          }
        val credential = result?.credential
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
    onAuthCancelled: (String?) -> Unit = {},
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

    val executionScope = (activity as? LifecycleOwner)?.lifecycleScope ?: scope
    val primarySignInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val primaryRequest = GetCredentialRequest.Builder().addCredentialOption(primarySignInOption).build()
    val appSha1 = getAppSigningSha1(activity)

    executionScope.launch {
      try {
        val result =
          executeInteractiveCredentialRequest(
            credentialManager = credentialManager,
            activity = activity,
            primaryRequest = primaryRequest,
            clientId = clientId,
          )
        val credential = result.credential
        if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
          val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
          if (googleIdToken.isBlank()) {
            onAuthError("Google Sign-In returned an empty ID token. Please try again.")
            return@launch
          }
          val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
          Firebase.auth.signInWithCredential(authCredential).await()
          onAuthSuccess()
        } else {
          onAuthError("Unexpected credential type received: ${credential.type}")
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: GoogleIdTokenParsingException) {
        Log.e(TAG, "Failed to parse Google ID token", e)
        onAuthError("Invalid Google ID token response. Please try again.")
      } catch (e: FirebaseAuthException) {
        Log.e(TAG, "Firebase signInWithCredential failed [${e.errorCode}]", e)
        onAuthError("Firebase Google auth error [${e.errorCode}]: ${e.localizedMessage}")
      } catch (e: GetCredentialCancellationException) {
        Log.w(TAG, "Google Sign-In cancelled or dismissed: type=${e.type}, msg=${e.errorMessage}", e)
        val diagnostic = classifyCancellationDiagnostic(e, appSha1)
        if (diagnostic != null) {
          onAuthError(diagnostic)
        } else {
          val sha1Hint =
            if (!appSha1.isNullOrBlank()) {
              "Sign-in was cancelled or rejected by Google Play Services. If you selected an account and returned here, add this APK's SHA-1 in Firebase Console (${activity.packageName}):\nSHA-1: $appSha1"
            } else {
              "Sign-in was cancelled. Please select a Google account and verify your Firebase SHA-1 configuration."
            }
          onAuthCancelled(sha1Hint)
        }
      } catch (e: NoCredentialException) {
        Log.w(TAG, "No Google credential available on device: ${e.errorMessage}", e)
        onAuthError(
          "No Google account available for Sign-In. Ensure you are signed into a Google account in device Settings and try again."
        )
      } catch (e: GetCredentialProviderConfigurationException) {
        Log.e(TAG, "Credential provider configuration missing", e)
        onAuthError("Google Play Services Credential Provider is unavailable on this device.")
      } catch (e: GetCredentialInterruptedException) {
        Log.w(TAG, "Google Sign-In interrupted: ${e.errorMessage}", e)
        onAuthError("Google Sign-In was interrupted. Please tap Sign in with Google again.")
      } catch (e: GetCredentialCustomException) {
        Log.e(TAG, "Google Sign-In custom error: ${e.type} / ${e.errorMessage}", e)
        onAuthError(buildCredentialDiagnosticMessage(e, appSha1))
      } catch (e: GetCredentialUnknownException) {
        Log.e(TAG, "Google Sign-In unknown error: ${e.errorMessage}", e)
        onAuthError(buildCredentialDiagnosticMessage(e, appSha1))
      } catch (e: GetCredentialException) {
        Log.e(TAG, "Google Sign-In credential error: ${e.type} / ${e.errorMessage}", e)
        onAuthError(buildCredentialDiagnosticMessage(e, appSha1))
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
    activity: Activity,
    primaryRequest: GetCredentialRequest,
    clientId: String,
  ): GetCredentialResponse {
    return try {
      credentialManager.getCredential(activity, primaryRequest)
    } catch (e: GetCredentialInterruptedException) {
      Log.w(TAG, "Google Sign-In interrupted on first attempt; retrying once: ${e.errorMessage}", e)
      delay(250L)
      credentialManager.getCredential(activity, primaryRequest)
    } catch (e: NoCredentialException) {
      Log.w(TAG, "GetSignInWithGoogleOption returned NoCredentialException; trying GetGoogleIdOption fallback", e)
      val fallbackOption =
        GetGoogleIdOption.Builder()
          .setFilterByAuthorizedAccounts(false)
          .setServerClientId(clientId)
          .setAutoSelectEnabled(false)
          .build()
      val fallbackRequest = GetCredentialRequest.Builder().addCredentialOption(fallbackOption).build()
      credentialManager.getCredential(activity, fallbackRequest)
    }
  }

  private fun buildCredentialDiagnosticMessage(e: GetCredentialException, appSha1: String?): String {
    val detail =
      listOfNotNull(e.errorMessage?.toString()?.trim(), e.message?.trim())
        .firstOrNull { it.isNotEmpty() }
        ?: e.type
    val sha1Suffix = if (!appSha1.isNullOrBlank()) "\nApp SHA-1: $appSha1" else ""
    return "Google Sign-In failed: $detail$sha1Suffix"
  }

  internal fun classifyCancellationDiagnostic(
    e: GetCredentialException,
    appSha1: String? = null,
  ): String? {
    val combined =
      listOfNotNull(
          e.errorMessage?.toString()?.trim(),
          e.message?.trim(),
          e.cause?.message?.trim(),
        )
        .filter { it.isNotEmpty() }
        .joinToString(" | ")
    if (combined.isEmpty()) return null
    val lower = combined.lowercase()
    val sha1Line = if (!appSha1.isNullOrBlank()) "\nAPK SHA-1: $appSha1" else ""
    return when {
      lower.contains("28444") ||
        lower.contains("developer console") ||
        lower.contains("10:") ||
        lower.contains("not registered") ||
        lower.contains("sha") ->
        "Google Sign-In configuration mismatch (SHA-1 or Web Client ID). Register this build's SHA-1 in Firebase Console ($combined).$sha1Line"
      lower.contains("28436") || lower.contains("temporarily blocked") ->
        "Too many cancelled Google prompts temporarily paused sign-in. Wait a minute or clear Google Play Services cache and try again."
      lower.contains("cannot find a matching credential") ||
        lower.contains("16:") ||
        lower.contains("reauth") ->
        "Google Sign-In could not verify account credential ($combined).$sha1Line"
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
