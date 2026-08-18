package com.bnightning.mavenpicker.action

import com.bnightning.mavenpicker.ui.MavenPickerPanelRegistry
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware

abstract class MavenPickerPanelAction : AnAction(), DumbAware {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    final override fun update(e: AnActionEvent) {
        val project = e.project
        e.presentation.isEnabledAndVisible = project != null && MavenPickerPanelRegistry.getInstance(project).panel != null
    }

    final override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val panel = MavenPickerPanelRegistry.getInstance(project).panel ?: return
        perform(panel, e)
    }

    protected abstract fun perform(panel: com.bnightning.mavenpicker.ui.SelectivePackagePanel, e: AnActionEvent)
}
