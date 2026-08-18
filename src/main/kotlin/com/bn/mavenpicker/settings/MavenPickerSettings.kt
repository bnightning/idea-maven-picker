package com.bnightning.mavenpicker.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
@State(name = "MavenPickerSettings", storages = [Storage("mavenPicker.xml")])
class MavenPickerSettings : PersistentStateComponent<MavenPickerSettings.State> {

    private var state = State()

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

    var alsoMake: Boolean
        get() = state.alsoMake
        set(value) {
            state.alsoMake = value
        }

    var cleanBeforePackage: Boolean
        get() = state.cleanBeforePackage
        set(value) {
            state.cleanBeforePackage = value
        }

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    data class State(
        var selectedSelectors: MutableList<String> = mutableListOf(),
        var includeAggregators: Boolean = false,
        var alsoMake: Boolean = true,
        var cleanBeforePackage: Boolean = true
    )

    companion object {
        fun getInstance(project: Project): MavenPickerSettings {
            return project.getService(MavenPickerSettings::class.java)
        }
    }
}
