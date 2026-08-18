package com.bnightning.mavenpicker.service

import com.bnightning.mavenpicker.model.MavenRunOptions
import kotlin.test.Test
import kotlin.test.assertEquals

class MavenCommandBuilderTest {

    @Test
    fun previewIncludesRelativePathSelectorsAndAlsoMake() {
        val command = MavenCommandBuilder.buildPreview(
            listOf("dq-core/dq-auth-service-v2", "dq-services/dq-gateway-service"),
            MavenRunOptions(alsoMake = true)
        )
        assertEquals(
            "mvn clean package -pl dq-core/dq-auth-service-v2,dq-services/dq-gateway-service -am",
            command
        )
    }

    @Test
    fun previewWithoutAlsoMakeOmitsAm() {
        val command = MavenCommandBuilder.buildPreview(
            listOf("dq-services/a"),
            MavenRunOptions(alsoMake = false)
        )
        assertEquals("mvn clean package -pl dq-services/a", command)
    }

    @Test
    fun previewCleanOnly() {
        val command = MavenCommandBuilder.buildPreview(
            listOf("dq-services/a"),
            MavenRunOptions(alsoMake = true),
            goals = listOf("clean")
        )
        assertEquals("mvn clean -pl dq-services/a -am", command)
    }

    @Test
    fun emptySelectionHasPlaceholder() {
        assertEquals("（未选择模块）", MavenCommandBuilder.buildPreview(emptyList(), MavenRunOptions()))
    }

    @Test
    fun dualPreviewShowsCleanAndPackage() {
        val command = MavenCommandBuilder.buildDualPreview(
            listOf("dq-services/a"),
            MavenRunOptions(alsoMake = true),
            packageGoals = listOf("clean", "package")
        )
        assertEquals(
            "Clean: mvn clean -pl dq-services/a -am\nPackage: mvn clean package -pl dq-services/a -am",
            command
        )
    }

    @Test
    fun previewIncludesStructuredRunOptionsAndExtraArgs() {
        val command = MavenCommandBuilder.buildPreview(
            listOf("dq-services/a"),
            MavenRunOptions(
                alsoMake = true,
                alsoMakeDependents = true,
                skipTests = true,
                offline = true,
                profiles = listOf("dev", "native", "dev"),
                extraArgs = "-T 2 -nsu"
            ),
            goals = listOf("verify")
        )

        assertEquals(
            "mvn verify -pl dq-services/a -am -amd -DskipTests -o -Pdev,native -T 2 -nsu",
            command
        )
    }
}
