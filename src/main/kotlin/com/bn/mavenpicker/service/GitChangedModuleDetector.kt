package com.bnightning.mavenpicker.service

import com.bnightning.mavenpicker.model.MavenModuleInfo
import com.intellij.openapi.project.Project
import java.util.concurrent.TimeUnit

object GitChangedModuleDetector {

    fun detectChangedSelectors(project: Project, modules: List<MavenModuleInfo>): Set<String> {
        val basePath = project.basePath ?: return emptySet()
        val changedPaths = runGitDiffNameOnly(basePath)
        if (changedPaths.isEmpty()) return emptySet()

        return modules.asSequence()
            .filter { module -> changedPaths.any { changedPath -> moduleContainsChangedFile(module, changedPath) } }
            .map(MavenModuleInfo::selector)
            .toSet()
    }

    private fun moduleContainsChangedFile(module: MavenModuleInfo, changedPath: String): Boolean {
        val normalizedChanged = changedPath.replace('\\', '/').trimStart('/')
        val modulePath = module.relativePath.replace('\\', '/').trim('.').trim('/')
        if (modulePath.isBlank()) {
            return normalizedChanged == "pom.xml" || !normalizedChanged.contains('/')
        }
        return normalizedChanged == modulePath ||
            normalizedChanged.startsWith("$modulePath/") ||
            normalizedChanged.endsWith("/${module.artifactId}/pom.xml")
    }

    private fun runGitDiffNameOnly(basePath: String): Set<String> {
        return runCatching {
            val process = ProcessBuilder("git", "diff", "--name-only", "HEAD")
                .directory(java.io.File(basePath))
                .redirectErrorStream(true)
                .start()
            if (!process.waitFor(5, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                return emptySet()
            }
            if (process.exitValue() != 0) {
                return runGitDiffCached(basePath)
            }
            process.inputStream.bufferedReader().readLines()
                .map(String::trim)
                .filter(String::isNotBlank)
                .toSet()
        }.getOrDefault(emptySet())
    }

    private fun runGitDiffCached(basePath: String): Set<String> =
        runCatching {
            val process = ProcessBuilder("git", "diff", "--cached", "--name-only")
                .directory(java.io.File(basePath))
                .redirectErrorStream(true)
                .start()
            if (!process.waitFor(5, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                return emptySet()
            }
            process.inputStream.bufferedReader().readLines()
                .map(String::trim)
                .filter(String::isNotBlank)
                .toSet()
        }.getOrDefault(emptySet())

    fun longestPrefixModule(changedPath: String, modules: List<MavenModuleInfo>): MavenModuleInfo? {
        val normalized = changedPath.replace('\\', '/')
        return modules.maxByOrNull { module ->
            val moduleDir = module.relativePath.replace('\\', '/').trim('.').trim('/')
            if (moduleDir.isBlank()) {
                if (normalized == "pom.xml") 1 else 0
            } else if (normalized == moduleDir || normalized.startsWith("$moduleDir/")) {
                moduleDir.length
            } else {
                0
            }
        }?.takeIf { module ->
            val moduleDir = module.relativePath.replace('\\', '/').trim('.').trim('/')
            moduleDir.isBlank() || normalized == moduleDir || normalized.startsWith("$moduleDir/")
        }
    }
}
