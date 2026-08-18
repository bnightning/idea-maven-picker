package com.bnightning.mavenpicker.ui

import com.bnightning.mavenpicker.service.MavenProjectsReloadListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import org.jetbrains.idea.maven.project.MavenProjectsManager

class MavenPickerToolWindowFactory : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = SelectivePackagePanel(project)
        MavenPickerPanelRegistry.getInstance(project).panel = panel
        val content = ContentFactory.getInstance().createContent(panel, "", false)
        content.isCloseable = false
        toolWindow.contentManager.addContent(content)

        val reloadListener = MavenProjectsReloadListener(project)
        MavenProjectsManager.getInstance(project).addManagerListener(reloadListener)

        val connection = project.messageBus.connect(toolWindow.disposable)
        connection.subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
            override fun after(events: List<VFileEvent>) {
                if (events.any { it.file?.name.equals("pom.xml", ignoreCase = true) }) {
                    reloadListener.scheduleReload()
                }
            }
        })

        Disposer.register(toolWindow.disposable, Disposable {
            MavenPickerPanelRegistry.getInstance(project).panel = null
        })
    }
}
