package com.bnightning.mavenpicker.settings

import com.bnightning.mavenpicker.model.MavenRunOptions
import com.bnightning.mavenpicker.model.ModulePreset
import com.bnightning.mavenpicker.model.PresetMatchKind
import com.bnightning.mavenpicker.model.RecentRunRecord
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
@State(name = "MavenPickerSettings", storages = [Storage("mavenPicker.xml")])
class MavenPickerSettings : PersistentStateComponent<MavenPickerSettings.State> {

    private var state = State()

    init {
        ensureDefaultPresets()
    }

    var selectedSelectors: List<String>
        get() = state.selectedSelectors
        set(value) {
            state.selectedSelectors = value.toMutableList()
        }

    var includeAggregators: Boolean
        get() = state.includeAggregators
        set(value) {
            state.includeAggregators = value
        }

    var cleanBeforePackage: Boolean
        get() = state.cleanBeforePackage
        set(value) {
            state.cleanBeforePackage = value
        }

    var runOptions: MavenRunOptions
        get() = MavenRunOptions(
            alsoMake = state.alsoMake,
            alsoMakeDependents = state.alsoMakeDependents,
            skipTests = state.skipTests,
            offline = state.offline,
            profiles = state.profiles.split(',').map(String::trim).filter(String::isNotBlank),
            extraArgs = state.extraArgs
        )
        set(value) {
            state.alsoMake = value.alsoMake
            state.alsoMakeDependents = value.alsoMakeDependents
            state.skipTests = value.skipTests
            state.offline = value.offline
            state.profiles = value.normalizedProfiles().joinToString(",")
            state.extraArgs = value.extraArgs
        }

    var presets: List<ModulePreset>
        get() = state.presets.map { it.toPreset() }
        set(value) {
            state.presets = value.map { PresetState.from(it) }.toMutableList()
            state.presetsInitialized = true
        }

    var selectedGoal: String
        get() = state.selectedGoal
        set(value) {
            state.selectedGoal = value
        }

    var customGoal: String
        get() = state.customGoal
        set(value) {
            state.customGoal = value
        }

    var recentRuns: List<RecentRunRecord>
        get() = state.recentRuns.map { it.toRecord() }
        set(value) {
            state.recentRuns = value.map(RecentRunState::from).toMutableList()
        }

    fun rememberRecentRun(record: RecentRunRecord) {
        val updated = (listOf(record) + recentRuns.filter { it.selectors != record.selectors || it.goal != record.goal })
            .take(10)
        recentRuns = updated
    }

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
        ensureDefaultPresets()
    }

    data class State(
        var selectedSelectors: MutableList<String> = mutableListOf(),
        var includeAggregators: Boolean = false,
        var alsoMake: Boolean = true,
        var cleanBeforePackage: Boolean = true,
        var alsoMakeDependents: Boolean = false,
        var skipTests: Boolean = false,
        var offline: Boolean = false,
        var profiles: String = "",
        var extraArgs: String = "",
        var presetsInitialized: Boolean = false,
        var presets: MutableList<PresetState> = mutableListOf(),
        var selectedGoal: String = "package",
        var customGoal: String = "",
        var recentRuns: MutableList<RecentRunState> = mutableListOf()
    )

    data class RecentRunState(
        var selectors: MutableList<String> = mutableListOf(),
        var goal: String = "package",
        var cleanBeforePackage: Boolean = true,
        var alsoMake: Boolean = true,
        var timestamp: Long = 0L
    ) {
        fun toRecord() = RecentRunRecord(
            selectors = selectors.toList(),
            goal = goal,
            cleanBeforePackage = cleanBeforePackage,
            alsoMake = alsoMake,
            timestamp = timestamp
        )

        companion object {
            fun from(record: RecentRunRecord) = RecentRunState(
                selectors = record.selectors.toMutableList(),
                goal = record.goal,
                cleanBeforePackage = record.cleanBeforePackage,
                alsoMake = record.alsoMake,
                timestamp = record.timestamp
            )
        }
    }

    data class PresetState(
        var id: String = "",
        var name: String = "",
        var matchKind: String = PresetMatchKind.ALL_LEAVES.name,
        var pattern: String = "",
        var leafOnly: Boolean = true
    ) {
        fun toPreset(): ModulePreset = ModulePreset(
            id = id.ifBlank { java.util.UUID.randomUUID().toString() },
            name = name,
            matchKind = runCatching { PresetMatchKind.valueOf(matchKind) }
                .getOrDefault(PresetMatchKind.ALL_LEAVES),
            pattern = pattern,
            leafOnly = leafOnly
        )

        companion object {
            fun from(preset: ModulePreset) = PresetState(
                id = preset.id,
                name = preset.name,
                matchKind = preset.matchKind.name,
                pattern = preset.pattern,
                leafOnly = preset.leafOnly
            )
        }
    }

    private fun ensureDefaultPresets() {
        if (state.presetsInitialized) return
        state.presets = defaultPresets().map(PresetState::from).toMutableList()
        state.presetsInitialized = true
    }

    private fun defaultPresets(): List<ModulePreset> = listOf(
        ModulePreset(name = "叶子服务", matchKind = PresetMatchKind.ALL_LEAVES),
        ModulePreset(name = "core 组", matchKind = PresetMatchKind.GROUP_NAME_CONTAINS, pattern = "core"),
        ModulePreset(name = "services 组", matchKind = PresetMatchKind.GROUP_NAME_CONTAINS, pattern = "service"),
        ModulePreset(name = "modules 组", matchKind = PresetMatchKind.GROUP_NAME_CONTAINS, pattern = "module")
    )

    companion object {
        fun getInstance(project: Project): MavenPickerSettings {
            return project.getService(MavenPickerSettings::class.java)
        }
    }
}
