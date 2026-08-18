package com.bnightning.mavenpicker.service

import com.bnightning.mavenpicker.model.ModulePreset
import com.bnightning.mavenpicker.model.PresetMatchKind

object PresetJsonIO {

    fun export(presets: List<ModulePreset>): String = buildString {
        append("[\n")
        presets.forEachIndexed { index, preset ->
            append("  {\n")
            append("    \"name\": ${jsonString(preset.name)},\n")
            append("    \"matchKind\": ${jsonString(preset.matchKind.name)},\n")
            append("    \"pattern\": ${jsonString(preset.pattern)},\n")
            append("    \"leafOnly\": ${preset.leafOnly}\n")
            append("  }")
            if (index < presets.lastIndex) append(",")
            append("\n")
        }
        append("]\n")
    }

    fun import(json: String): List<ModulePreset> {
        val objects = splitTopLevelObjects(json)
        return objects.mapNotNull(::parseObject)
    }

    private fun splitTopLevelObjects(json: String): List<String> {
        val trimmed = json.trim()
        if (!trimmed.startsWith("[")) return emptyList()
        val result = mutableListOf<String>()
        var depth = 0
        var start = -1
        for (index in trimmed.indices) {
            when (trimmed[index]) {
                '{' -> {
                    if (depth == 1) start = index
                    depth++
                }
                '}' -> {
                    depth--
                    if (depth == 1 && start >= 0) {
                        result += trimmed.substring(start, index + 1)
                        start = -1
                    }
                }
                '[' -> if (index == 0) depth = 1
            }
        }
        return result
    }

    private fun parseObject(block: String): ModulePreset? {
        val name = readStringField(block, "name") ?: return null
        val matchKind = readStringField(block, "matchKind")
            ?.let { runCatching { PresetMatchKind.valueOf(it) }.getOrNull() }
            ?: PresetMatchKind.ALL_LEAVES
        val pattern = readStringField(block, "pattern").orEmpty()
        val leafOnly = readBooleanField(block, "leafOnly") ?: true
        return ModulePreset(name = name, matchKind = matchKind, pattern = pattern, leafOnly = leafOnly)
    }

    private fun readStringField(block: String, field: String): String? {
        val regex = Regex("""\"$field\"\s*:\s*\"((?:\\.|[^\"\\])*)\"""")
        val match = regex.find(block) ?: return null
        return match.groupValues[1]
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
    }

    private fun readBooleanField(block: String, field: String): Boolean? {
        val regex = Regex("""\"$field\"\s*:\s*(true|false)""")
        return regex.find(block)?.groupValues?.get(1)?.toBooleanStrictOrNull()
    }

    private fun jsonString(value: String): String =
        "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}
