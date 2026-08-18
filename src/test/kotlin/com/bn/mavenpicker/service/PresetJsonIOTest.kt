package com.bnightning.mavenpicker.service

import com.bnightning.mavenpicker.model.MavenGoal
import com.bnightning.mavenpicker.model.ModulePreset
import com.bnightning.mavenpicker.model.PresetMatchKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PresetJsonIOTest {

    @Test
    fun exportAndImportRoundTrip() {
        val presets = listOf(
            ModulePreset(name = "叶子", matchKind = PresetMatchKind.ALL_LEAVES),
            ModulePreset(
                name = "core",
                matchKind = PresetMatchKind.GROUP_NAME_CONTAINS,
                pattern = "core"
            )
        )
        val json = PresetJsonIO.export(presets)
        val imported = PresetJsonIO.import(json)
        assertEquals(2, imported.size)
        assertEquals("叶子", imported[0].name)
        assertEquals(PresetMatchKind.GROUP_NAME_CONTAINS, imported[1].matchKind)
        assertEquals("core", imported[1].pattern)
    }
}

class MavenGoalTest {

    @Test
    fun packageWithCleanPrependsCleanGoal() {
        assertEquals(
            listOf("clean", "package"),
            MavenGoal.goalsForExecution(MavenGoal.PACKAGE, cleanBeforePackage = true)
        )
    }

    @Test
    fun deployRequiresConfirmationFlag() {
        assertTrue(MavenGoal.requiresConfirmation(MavenGoal.DEPLOY))
    }
}
