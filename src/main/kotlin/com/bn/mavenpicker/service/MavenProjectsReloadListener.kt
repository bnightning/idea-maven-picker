package com.bnightning.mavenpicker.service

import com.bnightning.mavenpicker.ui.MavenPickerPanelRegistry
import com.intellij.openapi.project.Project
import com.intellij.util.Alarm
import org.jetbrains.idea.maven.project.MavenProjectsManager

class MavenProjectsReloadListener(private val project: Project) : MavenProjectsManager.Listener {

    private val alarm = Alarm(Alarm.ThreadToUse.SWING_THREAD, project)

    override fun activated() = scheduleReload()

    fun scheduleReload() {
        alarm.cancelAllRequests()
        alarm.addRequest({
            MavenPickerPanelRegistry.getInstance(project).panel?.reload()
        }, 500)
    }
}
