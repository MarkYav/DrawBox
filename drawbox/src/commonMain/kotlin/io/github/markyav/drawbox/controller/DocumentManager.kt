package io.github.markyav.drawbox.controller

import io.github.markyav.drawbox.model.Action
import io.github.markyav.drawbox.model.ActionHistory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal class DocumentManager {
    private val _history = MutableStateFlow(ActionHistory())
    val history: StateFlow<ActionHistory> = _history.asStateFlow()

    private val _redoStack = MutableStateFlow<List<Action>>(emptyList())
    val redoStack: StateFlow<List<Action>> = _redoStack.asStateFlow()

    fun undo(): Boolean {
        val currentHistory = _history.value.actions
        if (currentHistory.isEmpty()) return false

        val lastAction = currentHistory.last()

        _history.update { it.copy(actions = currentHistory.dropLast(1)) }
        _redoStack.update { it + lastAction }
        return true
    }

    fun redo(): Boolean {
        val currentRedo = _redoStack.value
        if (currentRedo.isEmpty()) return false

        val actionToRestore = currentRedo.last()

        _redoStack.update { it.dropLast(1) }
        _history.update { it.copy(actions = it.actions + actionToRestore) }
        return true
    }

    fun clear() {
        _history.value = ActionHistory()
        _redoStack.value = emptyList()
    }

    fun commitAction(action: Action) {
        _history.update { it.copy(actions = it.actions + action) }
        _redoStack.value = emptyList()
    }
}
