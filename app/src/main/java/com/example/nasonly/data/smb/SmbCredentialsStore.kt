package com.example.nasonly.data.smb

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmbCredentialsStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val gson = Gson()

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "smb_credentials",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveCredentials(host: String, creds: SmbCredentials) {
        val key = "smb://$host"
        val json = gson.toJson(creds)
        sharedPreferences.edit().putString(key, json).apply()
    }

    fun getCredentials(host: String): SmbCredentials? {
        val key = "smb://$host"
        val json = sharedPreferences.getString(key, null)
        return json?.let { gson.fromJson(it, SmbCredentials::class.java) }
    }

    fun clearCredentials(host: String) {
        val key = "smb://$host"
        sharedPreferences.edit().remove(key).apply()
    }
}