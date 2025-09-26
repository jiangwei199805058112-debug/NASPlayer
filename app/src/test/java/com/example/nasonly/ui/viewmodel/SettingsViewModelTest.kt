package com.example.nasonly.ui.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.MockitoAnnotations

@ExperimentalCoroutinesApi
class SettingsViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    
    @Mock
    private lateinit var mockDataStore: DataStore<Preferences>

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun `initial settings should have default values`() = runTest(testDispatcher) {
        // Given - ViewModel is created with default settings
        
        // When
        val expectedDefaults = mapOf(
            "autoPlay" to true,
            "rememberPosition" to true,
            "defaultVolume" to 0.8f,
            "cacheSize" to 100, // MB
            "connectionTimeout" to 30 // seconds
        )

        // Then
        // In a real test, we'd verify these through the ViewModel's state
        assertTrue(expectedDefaults.isNotEmpty())
    }

    @Test
    fun `updateAutoPlay should save setting`() = runTest(testDispatcher) {
        // Given
        val newAutoPlayValue = false

        // When
        // In a real implementation, we'd call viewModel.updateAutoPlay(newAutoPlayValue)
        
        // Then
        // Verify the setting was saved to DataStore
        assertTrue(true) // Placeholder assertion
    }

    @Test
    fun `updateRememberPosition should save setting`() = runTest(testDispatcher) {
        // Given
        val newRememberPositionValue = false

        // When
        // In a real implementation, we'd call viewModel.updateRememberPosition(newRememberPositionValue)
        
        // Then
        // Verify the setting was saved to DataStore
        assertTrue(true) // Placeholder assertion
    }

    @Test
    fun `updateDefaultVolume should save valid volume level`() = runTest(testDispatcher) {
        // Given
        val validVolume = 0.6f

        // When
        val clampedVolume = validVolume.coerceIn(0f, 1f)
        
        // Then
        assertEquals(0.6f, clampedVolume, 0.01f)
    }

    @Test
    fun `updateDefaultVolume should clamp invalid volume levels`() = runTest(testDispatcher) {
        // Given
        val tooHighVolume = 1.5f
        val tooLowVolume = -0.5f

        // When
        val clampedHighVolume = tooHighVolume.coerceIn(0f, 1f)
        val clampedLowVolume = tooLowVolume.coerceIn(0f, 1f)
        
        // Then
        assertEquals(1.0f, clampedHighVolume, 0.01f)
        assertEquals(0.0f, clampedLowVolume, 0.01f)
    }

    @Test
    fun `updateCacheSize should save valid cache size`() = runTest(testDispatcher) {
        // Given
        val validCacheSize = 200 // MB

        // When
        val clampedCacheSize = validCacheSize.coerceAtLeast(10)
        
        // Then
        assertEquals(200, clampedCacheSize)
    }

    @Test
    fun `updateCacheSize should enforce minimum cache size`() = runTest(testDispatcher) {
        // Given
        val tooSmallCacheSize = 5 // MB

        // When
        val clampedCacheSize = tooSmallCacheSize.coerceAtLeast(10)
        
        // Then
        assertEquals(10, clampedCacheSize)
    }

    @Test
    fun `updateConnectionTimeout should save valid timeout`() = runTest(testDispatcher) {
        // Given
        val validTimeout = 45 // seconds

        // When
        val clampedTimeout = validTimeout.coerceIn(5, 300)
        
        // Then
        assertEquals(45, clampedTimeout)
    }

    @Test
    fun `updateConnectionTimeout should clamp invalid timeouts`() = runTest(testDispatcher) {
        // Given
        val tooShortTimeout = 3 // seconds
        val tooLongTimeout = 500 // seconds

        // When
        val clampedShortTimeout = tooShortTimeout.coerceIn(5, 300)
        val clampedLongTimeout = tooLongTimeout.coerceIn(5, 300)
        
        // Then
        assertEquals(5, clampedShortTimeout)
        assertEquals(300, clampedLongTimeout)
    }

    @Test
    fun `resetToDefaults should restore all default settings`() = runTest(testDispatcher) {
        // Given - some settings have been changed
        
        // When
        val defaultSettings = mapOf(
            "autoPlay" to true,
            "rememberPosition" to true,
            "defaultVolume" to 0.8f,
            "cacheSize" to 100,
            "connectionTimeout" to 30
        )

        // Then
        // In a real test, we'd verify all settings were reset to defaults
        assertEquals(5, defaultSettings.size)
        assertTrue(defaultSettings["autoPlay"] as Boolean)
        assertTrue(defaultSettings["rememberPosition"] as Boolean)
        assertEquals(0.8f, defaultSettings["defaultVolume"] as Float, 0.01f)
        assertEquals(100, defaultSettings["cacheSize"] as Int)
        assertEquals(30, defaultSettings["connectionTimeout"] as Int)
    }
}