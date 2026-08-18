package com.bnightning.mavenpicker.ui

import com.bnightning.mavenpicker.model.MavenGoal
import com.bnightning.mavenpicker.model.MavenModuleInfo
import com.bnightning.mavenpicker.model.MavenRunOptions
import com.bnightning.mavenpicker.model.ModulePreset
import com.bnightning.mavenpicker.model.RecentRunRecord
import com.bnightning.mavenpicker.service.GitChangedModuleDetector
import com.bnightning.mavenpicker.service.MavenCommandBuilder
import com.bnightning.mavenpicker.service.MavenDependencyAnalyzer
import com.bnightning.mavenpicker.service.MavenModuleResolver
import com.bnightning.mavenpicker.service.MavenRunBatch
import com.bnightning.mavenpicker.service.MavenRunnerExecutor
import com.bnightning.mavenpicker.service.PresetMatcher
import com.bnightning.mavenpicker.settings.MavenPickerSettings
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.Messages
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.ui.SearchTextField
import com.intellij.ui.CheckboxTree
import com.intellij.ui.CheckedTreeNode
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.tree.TreeUtil
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.GridLayout
import java.awt.datatransfer.StringSelection
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.DefaultComboBoxModel
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JSeparator
import javax.swing.JTree
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeSelectionModel
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import javax.swing.AbstractAction
import javax.swing.KeyStroke
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class SelectivePackagePanel(private val project: Project) : JPanel(BorderLayout()) {

    private companion object {
        private const val FORM_LABEL_WIDTH = 56
        private const val SECTION_GAP = 8
        private const val ROW_GAP = 6
    }

    private val resolver = MavenModuleResolver()
    private val settings = MavenPickerSettings.getInstance(project)

    private var allModules: List<MavenModuleInfo> = emptyList()
    private lateinit var tree: CheckboxTree
    private lateinit var countLabel: JBLabel
    private lateinit var commandPreview: JBTextArea
    private lateinit var includeAggregatorsCheck: JBCheckBox
    private lateinit var alsoMakeCheck: JBCheckBox
    private lateinit var alsoMakeDependentsCheck: JBCheckBox
    private lateinit var skipTestsCheck: JBCheckBox
    private lateinit var offlineCheck: JBCheckBox
    private lateinit var cleanBeforePackageCheck: JBCheckBox
    private lateinit var profilesField: JBTextField
    private lateinit var extraArgsField: JBTextField
    private lateinit var searchField: SearchTextField
    private lateinit var presetCombo: ComboBox<String>
    private lateinit var advancedOptionsPanel: JPanel
    private lateinit var advancedOptionsToggle: JButton
    private lateinit var goalCombo: ComboBox<String>
    private lateinit var customGoalField: JBTextField
    private lateinit var recentCombo: ComboBox<String>
    private lateinit var dependencyHintLabel: JBLabel
    private val selectedSelectors = linkedSetOf<String>()

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
            selectedSelectors.clear()
            selectedSelectors += settings.selectedSelectors
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
        val options = settings.runOptions
        includeAggregatorsCheck = JBCheckBox("显示聚合模块（pom）", settings.includeAggregators)
        alsoMakeCheck = JBCheckBox("-am 同时构建依赖/父 POM", options.alsoMake)
        alsoMakeDependentsCheck = JBCheckBox("-amd 同时构建依赖方", options.alsoMakeDependents)
        skipTestsCheck = JBCheckBox("跳过测试（-DskipTests）", options.skipTests)
        offlineCheck = JBCheckBox("离线模式（-o）", options.offline)
        cleanBeforePackageCheck = JBCheckBox("Package 前先 clean", settings.cleanBeforePackage)
        profilesField = JBTextField(options.normalizedProfiles().joinToString(","))
        extraArgsField = JBTextField(options.extraArgs)
        searchField = SearchTextField()
        goalCombo = ComboBox((MavenGoal.PREDEFINED + MavenGoal.CUSTOM).toTypedArray())
        goalCombo.selectedItem = settings.selectedGoal
        customGoalField = JBTextField(settings.customGoal)
        customGoalField.isVisible = goalCombo.selectedItem == MavenGoal.CUSTOM
        recentCombo = ComboBox<String>()
        dependencyHintLabel = JBLabel(" ")
        countLabel = JBLabel()
        commandPreview = JBTextArea(4, 20).apply {
            isEditable = false
            lineWrap = true
            wrapStyleWord = true
            border = JBUI.Borders.empty(6)
        }

        tree = object : CheckboxTree(ModuleTreeRenderer(), buildRoot(selectedSelectors)) {
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
        listOf(alsoMakeCheck, alsoMakeDependentsCheck, skipTestsCheck, offlineCheck).forEach { checkBox ->
            checkBox.addActionListener {
                persistCurrentState()
                refreshPreview()
            }
        }
        cleanBeforePackageCheck.addActionListener {
            persistCurrentState()
            refreshPreview()
        }
        listOf(profilesField, extraArgsField).forEach { field ->
            field.document.addDocumentListener(object : DocumentListener {
                override fun insertUpdate(event: DocumentEvent) = onOptionsChanged()
                override fun removeUpdate(event: DocumentEvent) = onOptionsChanged()
                override fun changedUpdate(event: DocumentEvent) = onOptionsChanged()
            })
        }
        searchField.textEditor.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(event: DocumentEvent) = rebuildTree(keepSelection = true)
            override fun removeUpdate(event: DocumentEvent) = rebuildTree(keepSelection = true)
            override fun changedUpdate(event: DocumentEvent) = rebuildTree(keepSelection = true)
        })
        goalCombo.addActionListener {
            customGoalField.isVisible = goalCombo.selectedItem == MavenGoal.CUSTOM
            cleanBeforePackageCheck.isEnabled = selectedGoal() == MavenGoal.PACKAGE
            persistCurrentState()
            refreshPreview()
            revalidate()
            repaint()
        }
        customGoalField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(event: DocumentEvent) = onOptionsChanged()
            override fun removeUpdate(event: DocumentEvent) = onOptionsChanged()
            override fun changedUpdate(event: DocumentEvent) = onOptionsChanged()
        })
        cleanBeforePackageCheck.isEnabled = selectedGoal() == MavenGoal.PACKAGE
        refreshRecentCombo()

        val north = verticalSection(
            buildSelectionToolbar(),
            buildPresetRow(),
            buildRecentRow(),
            buildSearchRow(),
            checkboxRow(includeAggregatorsCheck)
        )

        val south = verticalSection(
            sectionSeparator(),
            statusRow(countLabel),
            dependencyHintRow(dependencyHintLabel),
            buildRunOptionsPanel(),
            buildGoalRow(),
            buildRunActions(),
            buildPreviewSection()
        )

        val panel = JPanel(BorderLayout(0, SECTION_GAP))
        panel.border = JBUI.Borders.empty(8)
        panel.add(north, BorderLayout.NORTH)
        panel.add(JBScrollPane(tree), BorderLayout.CENTER)
        panel.add(south, BorderLayout.SOUTH)
        registerKeyboardShortcuts(panel)
        refreshPreview()
        return panel
    }

    private fun buildSelectionToolbar(): JPanel =
        buttonRow(
            "全选叶子" to { checkLeavesOnly() },
            "Git 变更" to { selectGitChangedModules() },
            "清空" to { setAllChecked(false) },
            "反选" to { invertLeaves() }
        )

    private fun buildGoalRow(): JPanel {
        val goalPanel = JPanel(BorderLayout(JBUI.scale(6), 0))
        goalPanel.add(goalCombo, BorderLayout.CENTER)
        goalPanel.add(customGoalField, BorderLayout.SOUTH)
        return labeledRow("Goal", goalPanel)
    }

    private fun buildRunActions(): JPanel = buttonRow(
        "执行" to { runSelectedGoal() },
        "复制命令" to { copyPreview() },
        "刷新模块" to { reload() }
    )

    private fun buildRecentRow(): JPanel {
        recentCombo.addActionListener {
            val index = recentCombo.selectedIndex - 1
            settings.recentRuns.getOrNull(index)?.let { applyRecentRun(it) }
            if (recentCombo.selectedIndex > 0) {
                recentCombo.selectedIndex = 0
            }
        }
        return labeledRow("最近", recentCombo)
    }

    private fun refreshRecentCombo() {
        if (!this::recentCombo.isInitialized) return
        recentCombo.model = DefaultComboBoxModel(
            (listOf("最近使用…") + settings.recentRuns.map { it.displayName() }).toTypedArray()
        )
    }

    private fun buildPresetRow(): JPanel {
        presetCombo = ComboBox()
        refreshPresetCombo()
        presetCombo.addActionListener {
            val presetIndex = presetCombo.selectedIndex - 1
            settings.presets.getOrNull(presetIndex)?.let { applyPreset(it) }
            if (presetCombo.selectedIndex > 0) {
                presetCombo.selectedIndex = 0
            }
        }
        val manageButton = JButton("管理").apply {
            addActionListener {
                PresetEditorDialog(project, settings.presets) { presets ->
                    settings.presets = presets
                    refreshPresetCombo()
                }.show()
            }
        }
        return labeledRow("预设", presetCombo, manageButton)
    }

    private fun refreshPresetCombo() {
        if (!this::presetCombo.isInitialized) return
        presetCombo.model = DefaultComboBoxModel(
            (listOf("选择预设…") + settings.presets.map(ModulePreset::name)).toTypedArray()
        )
    }

    private fun buildSearchRow(): JPanel = labeledRow("搜索", searchField)

    private fun buildRunOptionsPanel(): JPanel {
        advancedOptionsPanel = verticalSection(
            checkboxRow(alsoMakeDependentsCheck),
            checkboxRow(skipTestsCheck),
            checkboxRow(offlineCheck),
            labeledRow("Profiles", profilesField),
            labeledRow("额外参数", extraArgsField)
        ).apply {
            isVisible = optionsRequireAdvancedSection()
        }
        advancedOptionsToggle = JButton().apply {
            horizontalAlignment = JButton.LEFT
            isBorderPainted = false
            isContentAreaFilled = false
            addActionListener {
                advancedOptionsPanel.isVisible = !advancedOptionsPanel.isVisible
                updateAdvancedOptionsToggle()
                revalidate()
                repaint()
            }
        }
        updateAdvancedOptionsToggle()

        return verticalSection(
            checkboxRow(cleanBeforePackageCheck),
            checkboxRow(alsoMakeCheck),
            fullWidthRow(advancedOptionsToggle),
            advancedOptionsPanel
        )
    }

    private fun buildPreviewSection(): JPanel {
        val header = JPanel(BorderLayout()).apply {
            add(JBLabel("命令预览"), BorderLayout.WEST)
            add(JButton("复制命令").apply { addActionListener { copyPreview() } }, BorderLayout.EAST)
        }
        val previewScroll = JBScrollPane(commandPreview).apply {
            preferredSize = Dimension(0, JBUI.scale(72))
        }
        return verticalSection(
            fullWidthRow(header),
            fullWidthRow(previewScroll)
        )
    }

    private fun optionsRequireAdvancedSection(): Boolean =
        alsoMakeDependentsCheck.isSelected ||
            skipTestsCheck.isSelected ||
            offlineCheck.isSelected ||
            profilesField.text.isNotBlank() ||
            extraArgsField.text.isNotBlank()

    private fun updateAdvancedOptionsToggle() {
        advancedOptionsToggle.text = if (advancedOptionsPanel.isVisible) {
            "隐藏高级 Maven 参数"
        } else {
            "高级 Maven 参数…"
        }
    }

    private fun verticalSection(vararg rows: JComponent): JPanel {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        rows.forEachIndexed { index, row ->
            if (index > 0) {
                panel.add(Box.createVerticalStrut(ROW_GAP))
            }
            panel.add(row)
        }
        panel.alignmentX = Component.LEFT_ALIGNMENT
        val height = rows.sumOf { it.maximumSize.height } + ROW_GAP * (rows.size - 1).coerceAtLeast(0)
        panel.maximumSize = Dimension(Integer.MAX_VALUE, height)
        return panel
    }

    private fun fullWidthRow(content: JComponent): JPanel = JPanel(BorderLayout()).apply {
        add(content, BorderLayout.CENTER)
        constrainFullWidth(this)
    }

    private fun labeledRow(label: String, field: JComponent, trailing: JComponent? = null): JPanel {
        val labelComponent = fixedLabel(label)
        return JPanel(BorderLayout(JBUI.scale(6), 0)).apply {
            add(labelComponent, BorderLayout.WEST)
            add(field, BorderLayout.CENTER)
            trailing?.let { add(it, BorderLayout.EAST) }
            constrainFullWidth(this)
        }
    }

    private fun checkboxRow(checkBox: JBCheckBox): JPanel = fullWidthRow(
        JPanel(BorderLayout()).apply {
            add(Box.createHorizontalStrut(JBUI.scale(FORM_LABEL_WIDTH)), BorderLayout.WEST)
            add(checkBox, BorderLayout.CENTER)
        }
    )

    private fun statusRow(label: JBLabel): JPanel = fullWidthRow(
        JPanel(BorderLayout()).apply {
            add(fixedLabel("状态"), BorderLayout.WEST)
            add(label, BorderLayout.CENTER)
        }
    )

    private fun buttonRow(vararg items: Pair<String, () -> Unit>): JPanel {
        val panel = JPanel(GridLayout(1, items.size, JBUI.scale(6), 0))
        items.forEach { (text, action) ->
            panel.add(JButton(text).apply { addActionListener { action() } })
        }
        return fullWidthRow(panel)
    }

    private fun sectionSeparator(): JSeparator = JSeparator().apply {
        maximumSize = Dimension(Integer.MAX_VALUE, preferredSize.height)
    }

    private fun fixedLabel(text: String): JBLabel = JBLabel(text).apply {
        val size = Dimension(JBUI.scale(FORM_LABEL_WIDTH), preferredSize.height)
        preferredSize = size
        minimumSize = size
    }

    private fun constrainFullWidth(row: JComponent) {
        row.alignmentX = Component.LEFT_ALIGNMENT
        row.maximumSize = Dimension(Integer.MAX_VALUE, preferredSize(row))
    }

    private fun preferredSize(component: JComponent): Int =
        component.preferredSize?.height?.plus(JBUI.scale(2)) ?: JBUI.scale(28)

    private fun dependencyHintRow(label: JBLabel): JPanel = fullWidthRow(
        JPanel(BorderLayout()).apply {
            add(fixedLabel("依赖"), BorderLayout.WEST)
            add(label, BorderLayout.CENTER)
        }
    )

    private fun registerKeyboardShortcuts(root: JComponent) {
        val inputMap = root.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
        val actionMap = root.actionMap
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK), "runGoal")
        actionMap.put("runGoal", object : AbstractAction() {
            override fun actionPerformed(e: java.awt.event.ActionEvent) = runSelectedGoal()
        })
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK or InputEvent.SHIFT_DOWN_MASK), "copyCommand")
        actionMap.put("copyCommand", object : AbstractAction() {
            override fun actionPerformed(e: java.awt.event.ActionEvent) = copyPreview()
        })
    }

    fun runSelectedGoal() {
        val goal = selectedGoal()
        if (MavenGoal.requiresConfirmation(goal)) {
            val confirmed = Messages.showYesNoDialog(
                project,
                "deploy 会将构件发布到远程仓库，确认继续？",
                "Maven Picker",
                Messages.getWarningIcon()
            )
            if (confirmed != Messages.YES) return
        }
        runMaven(MavenGoal.goalsForExecution(goal, cleanBeforePackageCheck.isSelected))
    }

    fun applySelectors(selectors: Collection<String>) {
        selectedSelectors.clear()
        selectedSelectors += selectors
        if (this::tree.isInitialized) {
            rebuildTree(keepSelection = true, syncTreeSelection = false)
        }
        persistCurrentState()
        refreshPreview()
    }

    fun addSelector(selector: String) {
        selectedSelectors += selector
        if (this::tree.isInitialized) {
            rebuildTree(keepSelection = true, syncTreeSelection = false)
        }
        persistCurrentState()
        refreshPreview()
    }

    private fun selectGitChangedModules() {
        val changed = GitChangedModuleDetector.detectChangedSelectors(project, allModules)
        if (changed.isEmpty()) {
            Messages.showInfoMessage(project, "未检测到 Git 变更文件，或变更不在当前 Reactor 模块内。", "Maven Picker")
            return
        }
        applySelectors(changed)
    }

    private fun applyRecentRun(record: RecentRunRecord) {
        selectedSelectors.clear()
        selectedSelectors += record.selectors
        goalCombo.selectedItem = record.goal
        customGoalField.isVisible = record.goal == MavenGoal.CUSTOM
        cleanBeforePackageCheck.isSelected = record.cleanBeforePackage
        alsoMakeCheck.isSelected = record.alsoMake
        rebuildTree(keepSelection = true, syncTreeSelection = false)
        persistCurrentState()
        refreshPreview()
    }

    private fun selectedGoal(): String =
        (goalCombo.selectedItem as? String)?.let { goal ->
            if (goal == MavenGoal.CUSTOM) customGoalField.text.trim().ifBlank { MavenGoal.PACKAGE } else goal
        } ?: MavenGoal.PACKAGE

    private fun executionGoals(): List<String> =
        MavenGoal.goalsForExecution(selectedGoal(), cleanBeforePackageCheck.isSelected)

    private fun runMaven(goals: List<String>) {
        val selected = collectCheckedModules()
        if (selected.isEmpty()) {
            Messages.showErrorDialog(project, "未选择任何模块。", "Maven Picker")
            return
        }
        val batches = selected.groupBy { it.reactorRootDir }
            .map { (root, modules) -> MavenRunBatch(root, modules.map { it.selector }) }
        persistCurrentState()
        settings.selectedGoal = selectedGoal()
        settings.customGoal = customGoalField.text
        val options = currentRunOptions()
        val record = RecentRunRecord(
            selectors = selected.map { it.selector },
            goal = selectedGoal(),
            cleanBeforePackage = cleanBeforePackageCheck.isSelected,
            alsoMake = alsoMakeCheck.isSelected
        )
        MavenRunnerExecutor.runBatches(
            project = project,
            batches = batches,
            options = options,
            goals = goals
        ) { success ->
            if (success) {
                settings.rememberRecentRun(record)
                refreshRecentCombo()
            }
        }
    }

    private fun persistCurrentState() {
        if (!this::tree.isInitialized) return
        syncSelectionFromVisibleTree()
        settings.selectedSelectors = selectedSelectors.toList()
        settings.includeAggregators = includeAggregatorsCheck.isSelected
        settings.runOptions = currentRunOptions()
        settings.cleanBeforePackage = cleanBeforePackageCheck.isSelected
        settings.selectedGoal = selectedGoal()
        settings.customGoal = customGoalField.text
    }

    private fun onOptionsChanged() {
        if (!this::tree.isInitialized) return
        persistCurrentState()
        refreshPreview()
    }

    private fun currentRunOptions(): MavenRunOptions = MavenRunOptions(
        alsoMake = alsoMakeCheck.isSelected,
        alsoMakeDependents = alsoMakeDependentsCheck.isSelected,
        skipTests = skipTestsCheck.isSelected,
        offline = offlineCheck.isSelected,
        profiles = profilesField.text.split(','),
        extraArgs = extraArgsField.text
    )

    fun copyPreview() {
        CopyPasteManager.getInstance().setContents(StringSelection(commandPreview.text))
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Maven Picker")
            .createNotification("已复制 Maven 命令。", NotificationType.INFORMATION)
            .notify(project)
    }

    private fun visibleModules(): List<MavenModuleInfo> {
        val query = searchField.text.trim()
        return allModules
            .asSequence()
            .filter { includeAggregatorsCheck.isSelected || !it.isAggregator }
            .filter { module ->
                query.isBlank() ||
                    listOf(module.artifactId, module.selector, module.groupId, module.groupName)
                        .any { it.contains(query, ignoreCase = true) }
            }
            .toList()
    }

    private fun rebuildTree(keepSelection: Boolean, syncTreeSelection: Boolean = true) {
        if (keepSelection && syncTreeSelection) syncSelectionFromVisibleTree()
        if (!keepSelection) selectedSelectors.clear()
        tree.model = DefaultTreeModel(buildRoot(selectedSelectors))
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
        selectedSelectors.removeAll(visibleModules().map { it.selector }.toSet())
        selectedSelectors += visibleModules().filter { !it.isAggregator }.map { it.selector }
        rebuildTree(keepSelection = true, syncTreeSelection = false)
        persistCurrentState()
        refreshPreview()
    }

    private fun setAllChecked(checked: Boolean) {
        val visibleSelectors = visibleModules().map { it.selector }.toSet()
        if (checked) selectedSelectors += visibleSelectors else selectedSelectors.removeAll(visibleSelectors)
        rebuildTree(keepSelection = true, syncTreeSelection = false)
        persistCurrentState()
        refreshPreview()
    }

    private fun invertLeaves() {
        val selectors = visibleModules().filter { !it.isAggregator }.map { it.selector }
        selectors.forEach { selector ->
            if (!selectedSelectors.add(selector)) selectedSelectors.remove(selector)
        }
        rebuildTree(keepSelection = true, syncTreeSelection = false)
        persistCurrentState()
        refreshPreview()
    }

    private fun applyPreset(preset: ModulePreset) {
        selectedSelectors.clear()
        selectedSelectors += PresetMatcher.matchingSelectors(preset, allModules)
        rebuildTree(keepSelection = true, syncTreeSelection = false)
        persistCurrentState()
        refreshPreview()
    }

    private fun collectCheckedModules(): List<MavenModuleInfo> {
        if (!this::tree.isInitialized) {
            return allModules.filter { settings.selectedSelectors.contains(it.selector) }
        }
        return allModules.filter { it.selector in selectedSelectors }.sortedBy { it.selector }
    }

    private fun syncSelectionFromVisibleTree() {
        if (!this::tree.isInitialized) return
        val renderedSelectors = mutableSetOf<String>()
        val checkedRenderedSelectors = mutableSetOf<String>()
        val root = tree.model.root as? CheckedTreeNode ?: return
        walk(root) { node ->
            val module = node.userObject as? MavenModuleInfo
            if (module != null) {
                renderedSelectors += module.selector
                if (node.isChecked) {
                    checkedRenderedSelectors += module.selector
                }
            }
        }
        selectedSelectors.removeAll(renderedSelectors)
        selectedSelectors += checkedRenderedSelectors
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
        commandPreview.text = MavenCommandBuilder.buildExecutionPreview(
            selected.map { it.selector },
            currentRunOptions(),
            executionGoals()
        )
        if (alsoMakeCheck.isSelected && selected.isNotEmpty()) {
            val upstream = MavenDependencyAnalyzer.upstreamSelectors(
                project,
                selected.map { it.selector },
                allModules
            )
            dependencyHintLabel.text = if (upstream.isEmpty()) {
                "-am 无额外上游模块"
            } else {
                "-am 将额外构建：${upstream.joinToString(", ")}"
            }
        } else {
            dependencyHintLabel.text = " "
        }
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
