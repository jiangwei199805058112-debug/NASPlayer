package com.example.nasonly.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "nas_prefs")

object NasPrefsKeys {
    val HOST = stringPreferencesKey("host")
    val SHARE = stringPreferencesKey("share")
    val USER = stringPreferencesKey("user")
    val PASS = stringPreferencesKey("pass")
    val DOMAIN = stringPreferencesKey("domain")
    val SMB_VER = stringPreferencesKey("smb_ver")
}

data class NasConfig(
    val host: String = "",
    val share: String = "",
    val username: String = "",
    val password: String = "",
    val domain: String = "",
    val smbVersion: String = "",
)

@Singleton
class NasPrefs @Inject constructor(
    private val context: Context,
) {
    suspend fun save(
        host: String,
        share: String,
        username: String,
        password: String,
        domain: String,
        smbVersion: String,
    ) {
        context.dataStore.edit { prefs ->
            prefs[NasPrefsKeys.HOST] = host
            prefs[NasPrefsKeys.SHARE] = share
            prefs[NasPrefsKeys.USER] = username
            prefs[NasPrefsKeys.PASS] = password
            prefs[NasPrefsKeys.DOMAIN] = domain
            prefs[NasPrefsKeys.SMB_VER] = smbVersion
        }
    }

    fun flow(): Flow<NasConfig> = context.dataStore.data.map { prefs ->
        NasConfig(
            host = prefs[NasPrefsKeys.HOST] ?: "",
            share = prefs[NasPrefsKeys.SHARE] ?: "",
            username = prefs[NasPrefsKeys.USER] ?: "",
            password = prefs[NasPrefsKeys.PASS] ?: "",
            domain = prefs[NasPrefsKeys.DOMAIN] ?: "",
            smbVersion = prefs[NasPrefsKeys.SMB_VER] ?: "",
        )
    }
}
