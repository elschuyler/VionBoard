/*
 * VionBoard — BiometricBridgeActivity.kt
 * Transparent Activity that hosts the system biometric prompt.
 * A keyboard (InputMethodService) cannot show BiometricPrompt directly
 * because it is not a FragmentActivity. This bridge solves that.
 * SPDX-License-Identifier: GPL-3.0-only
 */
package helium314.keyboard.latin

import android.os.Bundle
import android.view.WindowManager
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import helium314.keyboard.latin.database.ProtectedEntriesDao

/**
 * Fully transparent Activity that bridges the keyboard service and the
 * Android BiometricPrompt API.
 *
 * Lifecycle:
 *  1. LatinIME.startActivity(Intent → BiometricBridgeActivity)
 *  2. Activity appears (transparent, user sees keyboard behind)
 *  3. BiometricPrompt shows system sheet immediately in onCreate
 *  4. On success → VionProtectedSuggestions.onAuthSuccess(trigger)
 *  5. On error/cancel → VionProtectedSuggestions.onAuthCancelled()
 *  6. Activity finishes itself immediately after delivering result
 *
 * The Activity has android:excludeFromRecents="true" and android:noHistory="true"
 * so it leaves no trace in the recents list.
 */
class BiometricBridgeActivity : FragmentActivity() {

    companion object {
        const val EXTRA_TRIGGER    = "vion_trigger"
        const val EXTRA_AUTH_MODE  = "vion_auth_mode"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make the activity fully transparent
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        window.setBackgroundDrawableResource(android.R.color.transparent)

        val trigger  = intent.getStringExtra(EXTRA_TRIGGER) ?: run { finish(); return }
        val authMode = ProtectedEntriesDao.AuthMode.fromString(
            intent.getStringExtra(EXTRA_AUTH_MODE) ?: ""
        )

        val canUseBiometric = canAuthenticateWithBiometrics()
        val canUsePassword  = true // device credential / master password always available as fallback

        when (authMode) {
            ProtectedEntriesDao.AuthMode.BIO_ONLY -> {
                if (canUseBiometric) showBiometricPrompt(trigger, allowDeviceCredential = false)
                else { VionProtectedSuggestions.onAuthCancelled(); finish() }
            }
            ProtectedEntriesDao.AuthMode.PASS_ONLY -> {
                // Use device credential (PIN/pattern/password) as the authenticator
                showBiometricPrompt(trigger, allowDeviceCredential = true, biometricOnly = false)
            }
            ProtectedEntriesDao.AuthMode.BIO_THEN_PASS -> {
                // Default: try biometric first, fall back to device credential
                showBiometricPrompt(trigger, allowDeviceCredential = true)
            }
        }
    }

    private fun showBiometricPrompt(
        trigger: String,
        allowDeviceCredential: Boolean,
        biometricOnly: Boolean = true
    ) {
        val executor = ContextCompat.getMainExecutor(this)

        val authenticators = when {
            allowDeviceCredential -> BIOMETRIC_STRONG or DEVICE_CREDENTIAL
            else                  -> BIOMETRIC_STRONG
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("VionBoard — Protected Entry")
            .setSubtitle("Authenticate to insert protected text")
            .setAllowedAuthenticators(authenticators)
            .apply {
                // Only show negative button when device credential is NOT allowed
                // (can't set both negative button and device credential)
                if (!allowDeviceCredential) {
                    setNegativeButtonText("Cancel")
                }
            }
            .build()

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                VionProtectedSuggestions.onAuthSuccess(trigger)
                finish()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                VionProtectedSuggestions.onAuthCancelled()
                finish()
            }

            override fun onAuthenticationFailed() {
                // Individual attempt failed — prompt stays open, do not finish yet
                // The system handles retry limits; we only act on error or success
            }
        }

        BiometricPrompt(this, executor, callback).authenticate(promptInfo)
    }

    private fun canAuthenticateWithBiometrics(): Boolean {
        val bm = BiometricManager.from(this)
        return bm.canAuthenticate(BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
    }

    override fun onStop() {
        super.onStop()
        // If the activity stops without a result (e.g. user presses home), cancel
        VionProtectedSuggestions.onAuthCancelled()
        finish()
    }
}
