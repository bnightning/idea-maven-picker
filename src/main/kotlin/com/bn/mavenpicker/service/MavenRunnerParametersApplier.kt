package com.bnightning.mavenpicker.service

import com.bnightning.mavenpicker.model.MavenRunOptions
import com.intellij.openapi.project.Project
import org.jetbrains.idea.maven.execution.MavenRunner
import org.jetbrains.idea.maven.execution.MavenRunnerParameters
import org.jetbrains.idea.maven.execution.MavenRunnerSettings
import org.jetbrains.idea.maven.project.MavenGeneralSettings
import org.jetbrains.idea.maven.project.MavenProjectsManager

/**
 * 将 [MavenRunOptions] 映射到 IntelliJ Maven Runner API。
 *
 * IntelliJ 的 [MavenRunnerParameters.setCmdOptions] 只能存一个字符串，且会作为单个 CLI 参数传给 Maven，
 * 不能把 `-am -amd -DskipTests` 拼在一起。因此：
 * - `-DskipTests`、自定义 `-D` 属性 → [MavenRunnerSettings]
 * - `-o` → [MavenGeneralSettings.setWorkOffline]
 * - `-P` → [MavenRunnerParameters.profilesMap]
 * - `-am` / `-amd` 及其余 CLI 开关 → 前置到 goals 列表（Maven 3+ 可正确解析）
 */
object MavenRunnerParametersApplier {

    data class AppliedSettings(
        val parameters: MavenRunnerParameters,
        val generalSettings: MavenGeneralSettings,
        val runnerSettings: MavenRunnerSettings,
    )

    fun apply(
        project: Project,
        parameters: MavenRunnerParameters,
        goals: List<String>,
        options: MavenRunOptions,
    ): AppliedSettings {
        val generalSettings = MavenProjectsManager.getInstance(project).generalSettings.clone()
        val runnerSettings = MavenRunner.getInstance(project).settings.clone()

        if (options.offline) {
            generalSettings.isWorkOffline = true
        }
        if (options.skipTests) {
            runnerSettings.isSkipTests = true
        }

        options.normalizedProfiles().forEach { profile ->
            parameters.profilesMap[profile] = true
        }

        val goalPrefixes = options.runnerGoalPrefixFlags().toMutableList()
        parseExtraArgs(options.extraArgs, goalPrefixes, runnerSettings, parameters.profilesMap)

        parameters.setGoals(goalPrefixes + goals)
        parameters.setCmdOptions(null)

        return AppliedSettings(parameters, generalSettings, runnerSettings)
    }

    private fun parseExtraArgs(
        extraArgs: String,
        goalPrefixes: MutableList<String>,
        runnerSettings: MavenRunnerSettings,
        profilesMap: MutableMap<String, Boolean>,
    ) {
        if (extraArgs.isBlank()) return

        val tokens = extraArgs.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        var index = 0
        while (index < tokens.size) {
            when (val token = tokens[index]) {
                "-o", "--offline" -> Unit
                "-am", "--also-make", "-amd", "--also-make-dependents" -> {
                    if (token !in goalPrefixes) {
                        goalPrefixes.add(token)
                    }
                }
                "-D", "--define" -> {
                    val property = tokens.getOrNull(++index) ?: break
                    applyMavenProperty(property, runnerSettings)
                }
                "-P", "--activate-profiles" -> {
                    val profiles = tokens.getOrNull(++index) ?: break
                    profiles.split(",").map(String::trim).filter(String::isNotBlank).forEach { profile ->
                        profilesMap[profile] = true
                    }
                }
                else -> when {
                    token.startsWith("-D") -> applyMavenProperty(token.removePrefix("-D"), runnerSettings)
                    token.startsWith("-P") -> {
                        token.removePrefix("-P").split(",").map(String::trim).filter(String::isNotBlank)
                            .forEach { profile -> profilesMap[profile] = true }
                    }
                    else -> goalPrefixes.add(token)
                }
            }
            index++
        }
    }

    private fun applyMavenProperty(property: String, runnerSettings: MavenRunnerSettings) {
        if (property.isBlank()) return
        val separator = property.indexOf('=')
        if (separator < 0) {
            runnerSettings.mavenProperties[property] = ""
        } else {
            runnerSettings.mavenProperties[property.substring(0, separator)] = property.substring(separator + 1)
        }
    }
}
