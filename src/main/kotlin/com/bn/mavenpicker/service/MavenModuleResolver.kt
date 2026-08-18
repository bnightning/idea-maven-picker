package com.bn.mavenpicker.service

import com.bn.mavenpicker.model.MavenModuleInfo
import com.bn.mavenpicker.model.MavenScanResult
import com.intellij.openapi.project.Project
import org.jetbrains.idea.maven.project.MavenProject
import org.jetbrains.idea.maven.project.MavenProjectsManager
import java.nio.file.Path
import java.nio.file.Paths

/**
 * 从 IDEA 已导入的 Maven 模型中解析模块，并用相对 Reactor 根目录的路径作为 `-pl` 选择器。
 */
class MavenModuleResolver {

    fun scan(project: Project): MavenScanResult {
        val manager = MavenProjectsManager.getInstance(project)
        if (!manager.isMavenizedProject) {
            return MavenScanResult(
                reactorRootDir = project.basePath.orEmpty(),
                modules = emptyList(),
                errorMessage = "当前项目尚未导入为 Maven 工程。请先通过 Maven 工具窗口导入后再试。"
            )
        }

        val mavenProjects = manager.projects
        if (mavenProjects.isEmpty()) {
            return MavenScanResult(
                reactorRootDir = project.basePath.orEmpty(),
                modules = emptyList(),
                errorMessage = "未能读取到任何 Maven 模块。请确认项目已完成 Maven 导入。"
            )
        }

        val reactorRoot = resolveReactorRoot(project, manager)
            ?: return MavenScanResult(
                reactorRootDir = project.basePath.orEmpty(),
                modules = emptyList(),
                errorMessage = "无法确定 Maven Reactor 根目录。"
            )

        val reactorRootPath = Paths.get(reactorRoot)
        val modules = mavenProjects.mapNotNull { mavenProject ->
            toModuleInfo(mavenProject, reactorRoot, reactorRootPath)
        }.distinctBy { it.selector }
            .sortedWith(compareBy({ it.groupName }, { it.relativePath }, { it.artifactId }))

        if (modules.none { !it.isAggregator }) {
            return MavenScanResult(
                reactorRootDir = reactorRoot,
                modules = modules,
                errorMessage = "未找到可执行的叶子模块（packaging != pom）。"
            )
        }

        return MavenScanResult(reactorRootDir = reactorRoot, modules = modules)
    }

    private fun resolveReactorRoot(project: Project, manager: MavenProjectsManager): String? {
        val roots = manager.rootProjects
        val basePath = project.basePath
        if (roots.isEmpty()) {
            return basePath
        }
        return roots.firstOrNull { it.directory == basePath }?.directory
            ?: roots.minByOrNull { it.directory.length }?.directory
            ?: basePath
    }

    private fun toModuleInfo(
        mavenProject: MavenProject,
        reactorRoot: String,
        reactorRootPath: Path
    ): MavenModuleInfo? {
        val mavenId = mavenProject.mavenId
        val artifactId = mavenId.artifactId ?: return null
        val groupId = mavenId.groupId.orEmpty()
        val packaging = mavenProject.packaging
        val relativePath = toRelativePath(reactorRootPath, mavenProject.directory) ?: return null
        val selector = relativePath.ifBlank {
            // 根模块本身没有相对路径时，退回坐标选择器，避免生成空的 -pl
            if (groupId.isNotBlank()) "$groupId:$artifactId" else ":$artifactId"
        }
        return MavenModuleInfo(
            selector = selector,
            groupId = groupId,
            artifactId = artifactId,
            packaging = packaging,
            relativePath = relativePath.ifBlank { "." },
            reactorRootDir = reactorRoot,
            groupName = groupName(relativePath),
            isAggregator = packaging.equals("pom", ignoreCase = true)
        )
    }

    private fun toRelativePath(reactorRootPath: Path, moduleDir: String): String? {
        return runCatching {
            val rel = reactorRootPath.normalize().toAbsolutePath()
                .relativize(Paths.get(moduleDir).normalize().toAbsolutePath())
                .toString()
                .replace('\\', '/')
            if (rel.startsWith("..")) null else rel
        }.getOrNull()
    }

    private fun groupName(relativePath: String): String {
        if (relativePath.isBlank()) return "root"
        return relativePath.substringBefore('/')
    }
}
