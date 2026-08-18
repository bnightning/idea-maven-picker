package com.bnightning.mavenpicker.action

import com.bnightning.mavenpicker.service.MavenModuleResolver
import com.bnightning.mavenpicker.ui.MavenPickerPanelRegistry
import com.bnightning.mavenpicker.ui.SelectivePackagePanel
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.vfs.VirtualFile
import java.nio.file.Paths

class AddModuleToMavenPickerAction : AnAction("加入 Maven Picker 选择", "将当前模块加入 Maven Picker 勾选列表", null), DumbAware {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val project = e.project
        val panel = project?.let { MavenPickerPanelRegistry.getInstance(it).panel }
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible = project != null && panel != null && file != null && isMavenRelated(file)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val panel = MavenPickerPanelRegistry.getInstance(project).panel ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val scan = MavenModuleResolver().scan(project)
        val modules = scan.modules
        val selector = findSelectorForFile(file, modules, scan.reactorRootDir) ?: return
        panel.addSelector(selector)
    }

    private fun isMavenRelated(file: VirtualFile): Boolean =
        file.isDirectory || file.name.equals("pom.xml", ignoreCase = true)

    private fun findSelectorForFile(
        file: VirtualFile,
        modules: List<com.bnightning.mavenpicker.model.MavenModuleInfo>,
        reactorRoot: String
    ): String? {
        val path = file.path.replace('\\', '/')
        val normalizedRoot = reactorRoot.replace('\\', '/')
        val relative = if (path.startsWith(normalizedRoot)) {
            path.removePrefix(normalizedRoot).trimStart('/')
        } else {
            file.name
        }
        if (relative.endsWith("pom.xml")) {
            val dir = relative.removeSuffix("pom.xml").trimEnd('/')
            modules.firstOrNull {
                it.relativePath.replace('\\', '/').trim('.').trim('/') == dir
            }?.selector?.let { return it }
        }
        return modules.firstOrNull {
            path.endsWith(it.relativePath.replace('\\', '/')) ||
                path.contains("/${it.artifactId}/")
        }?.selector
    }
}
