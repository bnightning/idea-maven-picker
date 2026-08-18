package com.bnightning.mavenpicker.model

/**
 * Maven Picker 可控制的运行参数。预览与实际 Maven Runner 均以此对象为唯一输入。
 */
data class MavenRunOptions(
    val alsoMake: Boolean = true,
    val alsoMakeDependents: Boolean = false,
    val skipTests: Boolean = false,
    val offline: Boolean = false,
    val profiles: List<String> = emptyList(),
    val extraArgs: String = ""
) {
    fun commandOptions(): List<String> = buildList {
        addAll(runnerGoalPrefixFlags())
        if (skipTests) add("-DskipTests")
        if (offline) add("-o")
        normalizedProfiles().takeIf { it.isNotEmpty() }?.let { add("-P${it.joinToString(",")}") }
        addAll(extraArgs.trim().split(Regex("\\s+")).filter { it.isNotBlank() })
    }

    /** IntelliJ Runner 需单独传参的 CLI 开关（-am / -amd），不可拼进 setCmdOptions。 */
    fun runnerGoalPrefixFlags(): List<String> = buildList {
        if (alsoMake) add("-am")
        if (alsoMakeDependents) add("-amd")
    }

    fun normalizedProfiles(): List<String> =
        profiles.map(String::trim).filter(String::isNotBlank).distinct()
}
