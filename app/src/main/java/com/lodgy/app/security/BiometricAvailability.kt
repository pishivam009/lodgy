package com.lodgy.app.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class BiometricAvailability @Inject constructor(@ApplicationContext private val context: Context) {
    fun isAvailable(): Boolean {
        val result = BiometricManager.from(context).canAuthenticate(BIOMETRIC_STRONG or BIOMETRIC_WEAK)
        return result == BiometricManager.BIOMETRIC_SUCCESS
    }
}
