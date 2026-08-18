package com.bnightning.mavenpicker.service

import com.bnightning.mavenpicker.model.MavenModuleInfo
import com.intellij.openapi.project.Project
import org.jetbrains.idea.maven.project.MavenProjectsManager

/**
 * 估算启用 -am 时会额外构建的上游 Reactor 模块（不含已选模块本身）。
 */
object MavenDependencyAnalyzer {

    fun upstreamSelectors(
        project: Project,
        selectedSelectors: Collection<String>,
        allModules: List<MavenModuleInfo>
    ): List<String> {
        if (selectedSelectors.isEmpty()) return emptyList()

        val moduleBySelector = allModules.associateBy(MavenModuleInfo::selector)
        val moduleByCoordinate = allModules.associateBy { "${it.groupId}:${it.artifactId}" }
        val selected = selectedSelectors.mapNotNull(moduleBySelector::get).toSet()
        if (selected.isEmpty()) return emptyList()

        val manager = MavenProjectsManager.getInstance(project)
        val mavenProjectByDir = manager.projects.associateBy { normalizePath(it.directory) }

        val upstream = linkedSetOf<String>()
        selected.forEach { module ->
            collectPathParents(module, allModules, upstream)
            val mavenProject = mavenProjectByDir[normalizePath(module.reactorRootDir + "/" + module.relativePath.trim('.'))]
                ?: mavenProjectByDir.values.firstOrNull {
                    normalizePath(it.directory).endsWith("/${module.artifactId}") ||
                        it.mavenId.artifactId == module.artifactId
                }
            mavenProject?.dependencies?.forEach { dependency ->
                val coordinate = "${dependency.groupId.orEmpty()}:${dependency.artifactId.orEmpty()}"
                moduleByCoordinate[coordinate]?.let { upstream += it.selector }
            }
        }

        return upstream.minus(selectedSelectors).sorted()
    }

    private fun collectPathParents(
        module: MavenModuleInfo,
        allModules: List<MavenModuleInfo>,
        upstream: MutableSet<String>
    ) {
        val path = module.relativePath.replace('\\', '/').trim('.').trim('/')
        if (path.isBlank()) return
        val segments = path.split('/')
        for (index in 1 until segments.size) {
            val parentPath = segments.take(index).joinToString("/")
            allModules.firstOrNull {
                it.relativePath.replace('\\', '/').trim('.').trim('/') == parentPath && it.isAggregator
            }?.let { upstream += it.selector }
        }
    }

    private fun normalizePath(path: String): String =
        path.replace('\\', '/').removeSuffix("/")
}
