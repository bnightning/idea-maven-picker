package com.bnightning.mavenpicker.action

import com.bnightning.mavenpicker.ui.SelectivePackagePanel
import com.intellij.openapi.actionSystem.AnActionEvent

class CopyMavenCommandAction : MavenPickerPanelAction() {

    override fun perform(panel: SelectivePackagePanel, e: AnActionEvent) {
        panel.copyPreview()
    }
}
