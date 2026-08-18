package com.bn.mavenpicker.service

object MavenCommandBuilder {

    fun buildPreview(
        selectors: List<String>,
        alsoMake: Boolean,
        goals: List<String> = listOf("clean", "package")
    ): String {
        if (selectors.isEmpty()) {
            return "（未选择模块）"
        }
        return buildString {
            append("mvn ")
            append(goals.joinToString(" "))
            append(" -pl ")
            append(selectors.joinToString(","))
            if (alsoMake) {
                append(" -am")
            }
        }
    }

    fun buildDualPreview(
        selectors: List<String>,
        alsoMake: Boolean,
        packageGoals: List<String>
    ): String {
        if (selectors.isEmpty()) {
            return "（未选择模块）"
        }
        return "Clean: ${buildPreview(selectors, alsoMake, listOf("clean"))}\n" +
            "Package: ${buildPreview(selectors, alsoMake, packageGoals)}"
    }
}
