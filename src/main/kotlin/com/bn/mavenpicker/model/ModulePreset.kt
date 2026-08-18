package com.bnightning.mavenpicker.model

import java.util.UUID

enum class PresetMatchKind {
    ALL_LEAVES,
    GROUP_NAME_CONTAINS,
    ARTIFACT_ID_CONTAINS,
    SELECTOR_EQUALS
}

data class ModulePreset(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val matchKind: PresetMatchKind,
    val pattern: String = "",
    val leafOnly: Boolean = true
)
