package com.codigomoo.calendariomedico.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PinManager @Inject constructor(@ApplicationContext private val context: Context) {

    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "caregiver_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun hasPin(): Boolean = prefs.contains(KEY_PIN_HASH)

    fun setPin(raw: String) = prefs.edit().putString(KEY_PIN_HASH, hash(raw)).apply()

    fun verifyPin(raw: String): Boolean = prefs.getString(KEY_PIN_HASH, null) == hash(raw)

    fun clearPin() = prefs.edit().remove(KEY_PIN_HASH).apply()

    private fun hash(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val KEY_PIN_HASH = "pin_hash"
    }
}
