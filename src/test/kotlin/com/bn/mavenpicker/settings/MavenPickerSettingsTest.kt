package com.bnightning.mavenpicker.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MavenPickerSettingsTest {

    @Test
    fun loadingLegacyStateAddsDefaultPresets() {
        val settings = MavenPickerSettings()
        settings.loadState(MavenPickerSettings.State())

        assertEquals(
            listOf("叶子服务", "core 组", "services 组", "modules 组"),
            settings.presets.map { it.name }
        )
    }

    @Test
    fun savedPresetInitializationIsNotOverwritten() {
        val state = MavenPickerSettings.State(presetsInitialized = true)
        val settings = MavenPickerSettings()
        settings.loadState(state)

        assertTrue(settings.presets.isEmpty())
    }
}
