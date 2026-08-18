package com.bnightning.mavenpicker.service

import com.bnightning.mavenpicker.model.MavenRunOptions

object MavenCommandBuilder {

    fun buildPreview(
        selectors: List<String>,
        options: MavenRunOptions,
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
            options.commandOptions().forEach { append(" ").append(it) }
        }
    }

    fun buildDualPreview(
        selectors: List<String>,
        options: MavenRunOptions,
        packageGoals: List<String>
    ): String {
        if (selectors.isEmpty()) {
            return "（未选择模块）"
        }
        return "Clean: ${buildPreview(selectors, options, listOf("clean"))}\n" +
            "Package: ${buildPreview(selectors, options, packageGoals)}"
    }

    fun buildExecutionPreview(
        selectors: List<String>,
        options: MavenRunOptions,
        goals: List<String>
    ): String {
        if (selectors.isEmpty()) {
            return "（未选择模块）"
        }
        return buildPreview(selectors, options, goals)
    }
}
