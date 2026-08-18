package com.bn.mavenpicker.ui

import com.bn.mavenpicker.model.MavenModuleInfo
import com.bn.mavenpicker.service.MavenCommandBuilder
import com.bn.mavenpicker.service.MavenModuleResolver
import com.bn.mavenpicker.service.MavenRunnerExecutor
import com.bn.mavenpicker.settings.MavenPickerSettings
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.Messages
import com.intellij.ui.CheckboxTree
import com.intellij.ui.CheckedTreeNode
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.tree.TreeUtil
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridLayout
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTree
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeSelectionModel

class SelectivePackagePanel(private val project: Project) : JPanel(BorderLayout()) {

    private val resolver = MavenModuleResolver()
    private val settings = MavenPickerSettings.getInstance(project)

    private var allModules: List<MavenModuleInfo> = emptyList()
    private lateinit var tree: CheckboxTree
    private lateinit var countLabel: JBLabel
    private lateinit var commandPreview: JBTextArea
    private lateinit var includeAggregatorsCheck: JBCheckBox
    private lateinit var alsoMakeCheck: JBCheckBox
    private lateinit var cleanBeforePackageCheck: JBCheckBox

    init {
        reload()
    }

    fun reload() {
        val scan = resolver.scan(project)
        removeAll()
        if (scan.hasError) {
            add(buildErrorPanel(scan.errorMessage.orEmpty()), BorderLayout.CENTER)
        } else {
            allModules = scan.modules
            add(buildMainPanel(), BorderLayout.CENTER)
        }
        revalidate()
        repaint()
    }

    private fun buildErrorPanel(message: String): JComponent {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.border = JBUI.Borders.empty(12)
        panel.add(JBLabel("<html>$message</html>"))
        panel.add(Box.createVerticalStrut(12))
        panel.add(JButton("刷新").apply { addActionListener { reload() } })
        return panel
    }

    private fun buildMainPanel(): JComponent {
        includeAggregatorsCheck = JBCheckBox("显示聚合模块（pom）", settings.includeAggregators)
        alsoMakeCheck = JBCheckBox("-am 同时构建依赖/父 POM", settings.alsoMake)
        cleanBeforePackageCheck = JBCheckBox("Package 前先 clean", settings.cleanBeforePackage)
        countLabel = JBLabel()
        commandPreview = JBTextArea(4, 20).apply {
            isEditable = false
            lineWrap = true
            wrapStyleWord = true
            border = JBUI.Borders.empty(6)
        }

        tree = object : CheckboxTree(ModuleTreeRenderer(), buildRoot(settings.selectedSelectors.toSet())) {
            override fun onNodeStateChanged(node: CheckedTreeNode) {
                persistCurrentState()
                refreshPreview()
            }
        }.apply {
            isRootVisible = false
            showsRootHandles = true
            selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
        }
        TreeUtil.expandAll(tree)

        includeAggregatorsCheck.addActionListener {
            rebuildTree(keepSelection = true)
            persistCurrentState()
        }
        alsoMakeCheck.addActionListener {
            persistCurrentState()
            refreshPreview()
        }
        cleanBeforePackageCheck.addActionListener {
            persistCurrentState()
            refreshPreview()
        }

        val north = JPanel()
        north.layout = BoxLayout(north, BoxLayout.Y_AXIS)
        north.alignmentX = LEFT_ALIGNMENT
        north.add(buildToolbar())
        north.add(Box.createVerticalStrut(4))
        north.add(alignLeft(cleanBeforePackageCheck))
        north.add(alignLeft(includeAggregatorsCheck))
        north.add(alignLeft(alsoMakeCheck))

        val south = JPanel()
        south.layout = BoxLayout(south, BoxLayout.Y_AXIS)
        south.add(alignLeft(countLabel))
        south.add(Box.createVerticalStrut(4))
        south.add(JBScrollPane(commandPreview))

        val panel = JPanel(BorderLayout(0, 8))
        panel.border = JBUI.Borders.empty(8)
        panel.add(north, BorderLayout.NORTH)
        panel.add(JBScrollPane(tree), BorderLayout.CENTER)
        panel.add(south, BorderLayout.SOUTH)
        refreshPreview()
        return panel
    }

    private fun buildToolbar(): JPanel {
        val col = JPanel()
        col.layout = BoxLayout(col, BoxLayout.Y_AXIS)
        col.alignmentX = LEFT_ALIGNMENT
        col.add(equalButtons("刷新" to { reload() }, "Clean" to { runMaven(listOf("clean")) }, "Package" to { runMaven(packageGoals()) }))
        col.add(Box.createVerticalStrut(4))
        col.add(equalButtons("全选叶子" to { checkLeavesOnly() }, "清空" to { setAllChecked(false) }, "反选" to { invertLeaves() }))
        col.add(Box.createVerticalStrut(4))
        col.add(buildPresetRow())
        return col
    }

    private fun buildPresetRow(): JPanel {
        val combo = ComboBox(arrayOf("选择预设…", "叶子服务", "core 组", "services 组", "modules 组"))
        combo.addActionListener {
            when (combo.selectedIndex) {
                1 -> checkLeavesOnly()
                2 -> checkGroup { it.contains("core", ignoreCase = true) }
                3 -> checkGroup { it.contains("service", ignoreCase = true) }
                4 -> checkGroup { it.contains("module", ignoreCase = true) }
            }
        }
        val row = JPanel(BorderLayout(6, 0))
        row.add(JBLabel("预设"), BorderLayout.WEST)
        row.add(combo, BorderLayout.CENTER)
        row.alignmentX = LEFT_ALIGNMENT
        row.maximumSize = Dimension(Integer.MAX_VALUE, combo.preferredSize.height + 4)
        return row
    }

    private fun packageGoals(): List<String> {
        return if (cleanBeforePackageCheck.isSelected) listOf("clean", "package") else listOf("package")
    }

    private fun runMaven(goals: List<String>) {
        val selected = collectCheckedModules()
        if (selected.isEmpty()) {
            Messages.showErrorDialog(project, "未选择任何模块。", "Maven Picker")
            return
        }
        val roots = selected.map { it.reactorRootDir }.distinct()
        if (roots.size > 1) {
            Messages.showErrorDialog(project, "选中的模块属于不同的 Maven 根工程，无法一次执行。", "Maven Picker")
            return
        }
        persistCurrentState()
        MavenRunnerExecutor.run(
            project = project,
            workingDir = selected.first().reactorRootDir,
            selectors = selected.map { it.selector },
            alsoMake = alsoMakeCheck.isSelected,
            goals = goals
        )
    }

    private fun persistCurrentState() {
        if (!this::tree.isInitialized) return
        settings.selectedSelectors = collectCheckedModules().map { it.selector }
        settings.includeAggregators = includeAggregatorsCheck.isSelected
        settings.alsoMake = alsoMakeCheck.isSelected
        settings.cleanBeforePackage = cleanBeforePackageCheck.isSelected
    }

    private fun equalButtons(vararg items: Pair<String, () -> Unit>): JPanel {
        val panel = JPanel(GridLayout(1, items.size, 4, 0))
        items.forEach { (text, action) ->
            panel.add(JButton(text).apply { addActionListener { action() } })
        }
        panel.alignmentX = LEFT_ALIGNMENT
        panel.maximumSize = Dimension(Integer.MAX_VALUE, 32)
        return panel
    }

    private fun alignLeft(component: JComponent): JPanel {
        val wrapper = JPanel()
        wrapper.layout = BoxLayout(wrapper, BoxLayout.X_AXIS)
        wrapper.add(component)
        wrapper.add(Box.createHorizontalGlue())
        wrapper.alignmentX = LEFT_ALIGNMENT
        return wrapper
    }

    private fun visibleModules(): List<MavenModuleInfo> {
        return if (includeAggregatorsCheck.isSelected) allModules else allModules.filter { !it.isAggregator }
    }

    private fun rebuildTree(keepSelection: Boolean) {
        val selected = if (keepSelection) collectCheckedModules().map { it.selector }.toSet() else emptySet()
        tree.model = DefaultTreeModel(buildRoot(selected))
        TreeUtil.expandAll(tree)
        refreshPreview()
    }

    private fun buildRoot(selectedSelectors: Set<String>): CheckedTreeNode {
        val root = CheckedTreeNode("root")
        visibleModules()
            .groupBy { it.groupName }
            .toSortedMap(compareBy<String> { groupRank(it) }.thenBy { it })
            .forEach { (groupName, modules) ->
                val groupNode = CheckedTreeNode(groupName)
                modules.forEach { module ->
                    val node = CheckedTreeNode(module)
                    node.isChecked = selectedSelectors.contains(module.selector)
                    groupNode.add(node)
                }
                root.add(groupNode)
            }
        return root
    }

    private fun groupRank(name: String): Int = when {
        name.contains("core", ignoreCase = true) -> 0
        name.contains("service", ignoreCase = true) -> 1
        name.contains("module", ignoreCase = true) -> 2
        name == "root" -> 3
        else -> 4
    }

    private fun checkLeavesOnly() {
        forEachModuleNode { node, module ->
            tree.setNodeState(node, !module.isAggregator)
        }
        persistCurrentState()
        refreshPreview()
    }

    private fun setAllChecked(checked: Boolean) {
        forEachModuleNode { node, _ -> tree.setNodeState(node, checked) }
        persistCurrentState()
        refreshPreview()
    }

    private fun invertLeaves() {
        val desired = mutableListOf<Pair<CheckedTreeNode, Boolean>>()
        forEachModuleNode { node, _ -> desired += node to !node.isChecked }
        desired.forEach { (node, checked) -> tree.setNodeState(node, checked) }
        persistCurrentState()
        refreshPreview()
    }

    private fun checkGroup(predicate: (String) -> Boolean) {
        forEachModuleNode { node, module ->
            tree.setNodeState(node, !module.isAggregator && predicate(module.groupName))
        }
        persistCurrentState()
        refreshPreview()
    }

    private fun forEachModuleNode(visitor: (CheckedTreeNode, MavenModuleInfo) -> Unit) {
        val root = tree.model.root as? CheckedTreeNode ?: return
        walk(root) { node ->
            val module = node.userObject as? MavenModuleInfo
            if (module != null) {
                visitor(node, module)
            }
        }
    }

    private fun collectCheckedModules(): List<MavenModuleInfo> {
        if (!this::tree.isInitialized) {
            return allModules.filter { settings.selectedSelectors.contains(it.selector) }
        }
        val result = mutableListOf<MavenModuleInfo>()
        val root = tree.model.root as? CheckedTreeNode ?: return result
        walk(root) { node ->
            val module = node.userObject as? MavenModuleInfo
            if (module != null && node.isChecked) {
                result += module
            }
        }
        return result.sortedBy { it.selector }
    }

    private fun walk(node: CheckedTreeNode, visitor: (CheckedTreeNode) -> Unit) {
        visitor(node)
        for (i in 0 until node.childCount) {
            walk(node.getChildAt(i) as CheckedTreeNode, visitor)
        }
    }

    private fun refreshPreview() {
        val selected = collectCheckedModules()
        countLabel.text = "已选择 ${selected.size} / 可见 ${visibleModules().size}"
        commandPreview.text = MavenCommandBuilder.buildDualPreview(
            selected.map { it.selector },
            alsoMakeCheck.isSelected,
            packageGoals()
        )
    }

    private class ModuleTreeRenderer : CheckboxTree.CheckboxTreeCellRenderer() {
        override fun customizeRenderer(
            tree: JTree,
            value: Any?,
            selected: Boolean,
            expanded: Boolean,
            leaf: Boolean,
            row: Int,
            hasFocus: Boolean
        ) {
            val node = value as? CheckedTreeNode ?: return
            when (val userObject = node.userObject) {
                is MavenModuleInfo -> {
                    textRenderer.append(userObject.artifactId, SimpleTextAttributes.REGULAR_ATTRIBUTES)
                    if (userObject.isAggregator) {
                        textRenderer.append("  [pom]", SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES)
                    }
                }
                is String -> textRenderer.append(userObject, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
            }
        }
    }
}
