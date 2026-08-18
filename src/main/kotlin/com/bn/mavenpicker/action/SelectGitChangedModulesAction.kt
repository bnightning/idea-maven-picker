package com.bnightning.mavenpicker.action

import com.bnightning.mavenpicker.service.GitChangedModuleDetector
import com.bnightning.mavenpicker.service.MavenModuleResolver
import com.bnightning.mavenpicker.ui.MavenPickerPanelRegistry
import com.bnightning.mavenpicker.ui.SelectivePackagePanel
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware

class SelectGitChangedModulesAction : AnAction("选择 Git 变更模块", "勾选 Git 变更涉及的 Maven 模块", null), DumbAware {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val project = e.project
        val panel = project?.let { MavenPickerPanelRegistry.getInstance(it).panel }
        e.presentation.isEnabledAndVisible = project != null && panel != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val panel = MavenPickerPanelRegistry.getInstance(project).panel ?: return
        val modules = MavenModuleResolver().scan(project).modules
        val changed = GitChangedModuleDetector.detectChangedSelectors(project, modules)
        panel.applySelectors(changed)
    }
}
