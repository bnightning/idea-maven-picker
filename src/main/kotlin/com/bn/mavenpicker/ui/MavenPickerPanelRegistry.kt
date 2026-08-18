package com.bnightning.mavenpicker.ui

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class MavenPickerPanelRegistry {
    var panel: SelectivePackagePanel? = null

    companion object {
        fun getInstance(project: Project): MavenPickerPanelRegistry =
            project.getService(MavenPickerPanelRegistry::class.java)
    }
}
