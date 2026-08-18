package com.bnightning.mavenpicker.service

import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import org.jetbrains.idea.maven.execution.MavenRunConfigurationType
import org.jetbrains.idea.maven.execution.MavenRunnerParameters

/**
 * 通过 IntelliJ 内置 Maven Runner 在 Run 工具窗口执行构建。
 */
object MavenRunnerExecutor {

    fun run(
        project: Project,
        workingDir: String,
        selectors: List<String>,
        alsoMake: Boolean,
        goals: List<String> = listOf("clean", "package")
    ) {
        FileDocumentManager.getInstance().saveAllDocuments()

        val pomFileName: String? = null
        val params = MavenRunnerParameters(
            true,
            workingDir,
            pomFileName,
            goals,
            emptyList()
        )
        params.setProjectsCmdOptionValues(selectors)
        if (alsoMake) {
            params.setCmdOptions("-am")
        }

        val goalText = goals.joinToString(" ")
        MavenRunConfigurationType.runConfiguration(project, params) { descriptor ->
            descriptor.processHandler?.addProcessListener(object : ProcessAdapter() {
                override fun processTerminated(event: ProcessEvent) {
                    if (event.exitCode != 0) {
                        notify(
                            project,
                            "Maven $goalText 失败（exitCode=${event.exitCode}）。请查看 Run 窗口日志。",
                            NotificationType.ERROR
                        )
                    }
                }
            })
        }
    }

    private fun notify(project: Project, content: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Maven Picker")
            .createNotification(content, type)
            .notify(project)
    }
}
