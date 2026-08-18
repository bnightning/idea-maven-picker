package com.bnightning.mavenpicker.model

import org.junit.Assert.assertEquals
import org.junit.Test

class MavenRunOptionsTest {

    @Test
    fun `runnerGoalPrefixFlags includes am and amd separately`() {
        val options = MavenRunOptions(alsoMake = true, alsoMakeDependents = true)
        assertEquals(listOf("-am", "-amd"), options.runnerGoalPrefixFlags())
    }

    @Test
    fun `commandOptions keeps preview compatible with mvn CLI`() {
        val options = MavenRunOptions(
            alsoMake = true,
            alsoMakeDependents = true,
            skipTests = true,
        )
        assertEquals(
            listOf("-am", "-amd", "-DskipTests"),
            options.commandOptions(),
        )
    }
}
