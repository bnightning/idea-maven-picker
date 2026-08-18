package com.bnightning.mavenpicker.ui

import com.bnightning.mavenpicker.model.ModulePreset
import com.bnightning.mavenpicker.model.PresetMatchKind
import com.bnightning.mavenpicker.service.PresetJsonIO
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBList
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.datatransfer.StringSelection
import javax.swing.DefaultListModel
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JScrollPane

class PresetEditorDialog(
    private val project: Project,
    presets: List<ModulePreset>,
    private val onSave: (List<ModulePreset>) -> Unit
) : DialogWrapper(project) {

    private val model = DefaultListModel<ModulePreset>().apply { presets.forEach(::addElement) }
    private val list = JBList(model).apply {
        cellRenderer = PresetListRenderer()
        selectionMode = javax.swing.ListSelectionModel.SINGLE_SELECTION
    }

    init {
        title = "管理 Maven Picker 预设"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout(8, 8)).apply {
            preferredSize = Dimension(460, 280)
        }
        panel.add(JScrollPane(list), BorderLayout.CENTER)
        panel.add(
            JPanel().apply {
                add(JButton("新增").apply { addActionListener { editPreset(null) } })
                add(JButton("编辑").apply { addActionListener { list.selectedValue?.let(::editPreset) } })
                add(JButton("删除").apply {
                    addActionListener {
                        if (list.selectedIndex >= 0) {
                            this@PresetEditorDialog.model.remove(list.selectedIndex)
                        }
                    }
                })
                add(JButton("导出 JSON").apply { addActionListener { exportPresets() } })
                add(JButton("导入 JSON").apply { addActionListener { importPresets() } })
            },
            BorderLayout.SOUTH
        )
        return panel
    }

    override fun doOKAction() {
        onSave((0 until model.size).map(model::getElementAt))
        super.doOKAction()
    }

    private fun editPreset(existing: ModulePreset?) {
        val name = Messages.showInputDialog(
            project,
            "预设名称",
            if (existing == null) "新增预设" else "编辑预设",
            null,
            existing?.name.orEmpty(),
            null
        )?.trim().orEmpty()
        if (name.isBlank()) return

        val kinds = PresetMatchKind.entries.toTypedArray()
        val labels = kinds.map(::kindLabel).toTypedArray()
        val selectedIndex = Messages.showChooseDialog(
            project,
            "匹配方式",
            "编辑预设",
            null,
            labels,
            kindLabel(existing?.matchKind ?: PresetMatchKind.ALL_LEAVES)
        )
        if (selectedIndex < 0) return
        val kind = kinds[selectedIndex]
        val pattern = if (kind == PresetMatchKind.ALL_LEAVES) {
            ""
        } else {
            Messages.showInputDialog(
                project,
                patternPrompt(kind),
                "编辑预设",
                null,
                existing?.pattern.orEmpty(),
                null
            ) ?: return
        }
        val leafOnly = if (kind == PresetMatchKind.ALL_LEAVES) {
            true
        } else {
            Messages.showYesNoDialog(
                project,
                "是否排除 packaging=pom 的聚合模块？",
                "编辑预设",
                null
            ) == Messages.YES
        }
        val preset = ModulePreset(
            id = existing?.id ?: java.util.UUID.randomUUID().toString(),
            name = name,
            matchKind = kind,
            pattern = pattern.trim(),
            leafOnly = leafOnly
        )
        val index = list.selectedIndex
        if (index >= 0 && existing != null) {
            model.set(index, preset)
            list.selectedIndex = index
        } else {
            model.addElement(preset)
            list.selectedIndex = model.size - 1
        }
    }

    private fun kindLabel(kind: PresetMatchKind): String = when (kind) {
        PresetMatchKind.ALL_LEAVES -> "全部叶子模块"
        PresetMatchKind.GROUP_NAME_CONTAINS -> "分组名称包含"
        PresetMatchKind.ARTIFACT_ID_CONTAINS -> "artifactId 包含"
        PresetMatchKind.SELECTOR_EQUALS -> "选择器精确匹配"
    }

    private fun patternPrompt(kind: PresetMatchKind): String = when (kind) {
        PresetMatchKind.GROUP_NAME_CONTAINS -> "输入分组名称关键词"
        PresetMatchKind.ARTIFACT_ID_CONTAINS -> "输入 artifactId 关键词"
        PresetMatchKind.SELECTOR_EQUALS -> "输入 selector，多个以英文逗号分隔"
        PresetMatchKind.ALL_LEAVES -> ""
    }

    private fun exportPresets() {
        val json = PresetJsonIO.export((0 until model.size).map(model::getElementAt))
        CopyPasteManager.getInstance().setContents(StringSelection(json))
        Messages.showInfoMessage(project, "预设 JSON 已复制到剪贴板。", "Maven Picker")
    }

    private fun importPresets() {
        val json = Messages.showMultilineInputDialog(project, "粘贴预设 JSON", "导入预设", "", null, null)
            ?: return
        if (json.isBlank()) return
        val imported = PresetJsonIO.import(json)
        if (imported.isEmpty()) {
            Messages.showErrorDialog(project, "未能解析任何预设，请检查 JSON 格式。", "Maven Picker")
            return
        }
        model.clear()
        imported.forEach { model.addElement(it) }
    }
}

private class PresetListRenderer : javax.swing.DefaultListCellRenderer() {
    override fun getListCellRendererComponent(
        list: javax.swing.JList<*>?,
        value: Any?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): java.awt.Component {
        val preset = value as? ModulePreset
        return super.getListCellRendererComponent(
            list,
            preset?.let { "${it.name}（${it.matchKind}）" }.orEmpty(),
            index,
            isSelected,
            cellHasFocus
        )
    }
}
