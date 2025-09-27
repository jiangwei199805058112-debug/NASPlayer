package com.example.nasonly.data.smb

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SmbCredentialsStoreTest {

    private lateinit var store: SmbCredentialsStore
    private val host = "test-host"

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        store = SmbCredentialsStore(context)
        store.clearCredentials(host) // Clean up
    }

    @Test
    fun `save and retrieve credentials works`() {
        val creds = SmbCredentials("user", "pass", "domain")
        store.saveCredentials(host, creds)
        val retrieved = store.getCredentials(host)
        assertNotNull(retrieved)
        assertEquals(creds.username, retrieved?.username)
        assertEquals(creds.password, retrieved?.password)
        assertEquals(creds.domain, retrieved?.domain)
    }

    @Test
    fun `clear credentials removes them`() {
        val creds = SmbCredentials("user", "pass", "domain")
        store.saveCredentials(host, creds)
        assertNotNull(store.getCredentials(host))
        store.clearCredentials(host)
        assertNull(store.getCredentials(host))
    }
}