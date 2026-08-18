package com.bnightning.mavenpicker.service

import com.bnightning.mavenpicker.model.MavenModuleInfo
import com.bnightning.mavenpicker.model.ModulePreset
import com.bnightning.mavenpicker.model.PresetMatchKind
import kotlin.test.Test
import kotlin.test.assertEquals

class PresetMatcherTest {

    private val modules = listOf(
        module("core/auth", "auth-service", "core"),
        module("services/gateway", "gateway-service", "services"),
        module("modules/bom", "platform-bom", "modules", isAggregator = true)
    )

    @Test
    fun allLeavesExcludesAggregators() {
        val selectors = PresetMatcher.matchingSelectors(
            ModulePreset(name = "叶子", matchKind = PresetMatchKind.ALL_LEAVES),
            modules
        )

        assertEquals(setOf("core/auth", "services/gateway"), selectors)
    }

    @Test
    fun groupAndArtifactMatchersAreCaseInsensitive() {
        val groupSelectors = PresetMatcher.matchingSelectors(
            ModulePreset(name = "服务", matchKind = PresetMatchKind.GROUP_NAME_CONTAINS, pattern = "SERV"),
            modules
        )
        val artifactSelectors = PresetMatcher.matchingSelectors(
            ModulePreset(name = "网关", matchKind = PresetMatchKind.ARTIFACT_ID_CONTAINS, pattern = "GATEWAY"),
            modules
        )

        assertEquals(setOf("services/gateway"), groupSelectors)
        assertEquals(setOf("services/gateway"), artifactSelectors)
    }

    @Test
    fun selectorMatcherSupportsCommaSeparatedExactValues() {
        val selectors = PresetMatcher.matchingSelectors(
            ModulePreset(
                name = "指定模块",
                matchKind = PresetMatchKind.SELECTOR_EQUALS,
                pattern = "core/auth, modules/bom",
                leafOnly = false
            ),
            modules
        )

        assertEquals(setOf("core/auth", "modules/bom"), selectors)
    }

    private fun module(
        selector: String,
        artifactId: String,
        groupName: String,
        isAggregator: Boolean = false
    ) = MavenModuleInfo(
        selector = selector,
        groupId = "com.example",
        artifactId = artifactId,
        packaging = if (isAggregator) "pom" else "jar",
        relativePath = selector,
        reactorRootDir = "/workspace",
        groupName = groupName,
        isAggregator = isAggregator
    )
}
