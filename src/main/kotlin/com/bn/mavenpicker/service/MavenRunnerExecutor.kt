package com.bnightning.mavenpicker.service

import com.bnightning.mavenpicker.model.MavenRunOptions
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.notification.Notification
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import org.jetbrains.idea.maven.execution.MavenRunConfigurationType
import org.jetbrains.idea.maven.execution.MavenRunnerParameters

data class MavenRunBatch(
    val workingDir: String,
    val selectors: List<String>
)

/**
 * 通过 IntelliJ 内置 Maven Runner 在 Run 工具窗口执行构建。
 */
object MavenRunnerExecutor {

    fun runBatches(
        project: Project,
        batches: List<MavenRunBatch>,
        options: MavenRunOptions,
        goals: List<String>,
        onAllComplete: ((success: Boolean) -> Unit)? = null
    ) {
        runOnEdt(project) {
            if (batches.isEmpty() || project.isDisposed) return@runOnEdt
            FileDocumentManager.getInstance().saveAllDocuments()
            runBatchAt(project, batches, 0, options, goals, onAllComplete)
        }
    }

    private fun runBatchAt(
        project: Project,
        batches: List<MavenRunBatch>,
        index: Int,
        options: MavenRunOptions,
        goals: List<String>,
        onAllComplete: ((success: Boolean) -> Unit)?
    ) {
        runOnEdt(project) {
            if (project.isDisposed) return@runOnEdt
            if (index >= batches.size) {
                onAllComplete?.invoke(true)
                return@runOnEdt
            }

            val batch = batches[index]
            val goalText = goals.joinToString(" ")
            val params = MavenRunnerParameters(
                true,
                batch.workingDir,
                null as String?,
                goals,
                emptyList<String>()
            )
            params.setProjectsCmdOptionValues(batch.selectors)
            val applied = MavenRunnerParametersApplier.apply(project, params, goals, options)

            MavenRunConfigurationType.runConfiguration(
                project,
                applied.parameters,
                applied.generalSettings,
                applied.runnerSettings,
            ) { descriptor ->
                runOnEdt(project) {
                    if (project.isDisposed) return@runOnEdt
                    focusRunWindow(project)
                    descriptor.processHandler?.addProcessListener(object : ProcessAdapter() {
                        override fun processTerminated(event: ProcessEvent) {
                            runOnEdt(project) {
                                if (project.isDisposed) return@runOnEdt
                                handleBatchTerminated(
                                    project, batches, index, batch, goalText, options, goals, event, onAllComplete
                                )
                            }
                        }
                    })
                }
            }
        }
    }

    private fun handleBatchTerminated(
        project: Project,
        batches: List<MavenRunBatch>,
        index: Int,
        batch: MavenRunBatch,
        goalText: String,
        options: MavenRunOptions,
        goals: List<String>,
        event: ProcessEvent,
        onAllComplete: ((success: Boolean) -> Unit)?
    ) {
        if (event.exitCode != 0) {
            notify(
                project,
                "Maven $goalText 失败（Reactor ${index + 1}/${batches.size}，exitCode=${event.exitCode}）。请查看 Run 窗口日志。",
                NotificationType.ERROR
            )
            onAllComplete?.invoke(false)
            return
        }
        if (index == batches.lastIndex) {
            notify(
                project,
                "Maven $goalText 成功（${batches.size} 个 Reactor，${batch.selectors.size} 个模块）。",
                NotificationType.INFORMATION
            )
            onAllComplete?.invoke(true)
        } else {
            runBatchAt(project, batches, index + 1, options, goals, onAllComplete)
        }
    }

    private fun focusRunWindow(project: Project) {
        ToolWindowManager.getInstance(project).getToolWindow("Run")?.activate(null)
    }

    private fun notify(project: Project, content: String, type: NotificationType): Notification =
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Maven Picker")
            .createNotification(content, type)
            .also { it.notify(project) }

    private fun runOnEdt(project: Project, action: () -> Unit) {
        val app = ApplicationManager.getApplication()
        if (app.isDispatchThread) {
            action()
        } else {
            app.invokeLater {
                if (!project.isDisposed) {
                    action()
                }
            }
        }
    }
}
