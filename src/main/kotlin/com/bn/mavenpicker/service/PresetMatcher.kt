package com.bnightning.mavenpicker.service

import com.bnightning.mavenpicker.model.MavenModuleInfo
import com.bnightning.mavenpicker.model.ModulePreset
import com.bnightning.mavenpicker.model.PresetMatchKind

object PresetMatcher {

    fun matchingSelectors(preset: ModulePreset, modules: List<MavenModuleInfo>): Set<String> {
        val pattern = preset.pattern.trim()
        val exactSelectors = pattern.split(',', '\n', '\r')
            .map(String::trim)
            .filter(String::isNotBlank)
            .toSet()

        return modules.asSequence()
            .filter { module -> !preset.leafOnly || !module.isAggregator }
            .filter { module ->
                when (preset.matchKind) {
                    PresetMatchKind.ALL_LEAVES -> !module.isAggregator
                    PresetMatchKind.GROUP_NAME_CONTAINS ->
                        module.groupName.contains(pattern, ignoreCase = true)
                    PresetMatchKind.ARTIFACT_ID_CONTAINS ->
                        module.artifactId.contains(pattern, ignoreCase = true)
                    PresetMatchKind.SELECTOR_EQUALS -> module.selector in exactSelectors
                }
            }
            .map(MavenModuleInfo::selector)
            .toSet()
    }
}
