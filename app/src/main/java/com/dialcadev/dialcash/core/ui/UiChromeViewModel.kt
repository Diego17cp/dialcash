package com.dialcadev.dialcash.core.ui

import androidx.lifecycle.ViewModel
import com.dialcadev.dialcash.core.ui.components.AppBarAction
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UiChromeViewModel : ViewModel() {
    val hazeState: HazeState = HazeState()
    private val _topBarHeightPx = MutableStateFlow(0)
    val topBarHeightPx: StateFlow<Int> = _topBarHeightPx.asStateFlow()

    private val _bottomBarHeightPx = MutableStateFlow(0)
    val bottomBarHeightPx: StateFlow<Int> = _bottomBarHeightPx.asStateFlow()

    private val _extraActions = MutableStateFlow<List<AppBarAction>>(emptyList())
    val extraActions: StateFlow<List<AppBarAction>> = _extraActions.asStateFlow()

    fun reportTopBarHeight(px: Int) { _topBarHeightPx.value = px }
    fun reportBottomBarHeight(px: Int) { _bottomBarHeightPx.value = px }

    fun setExtraActions(owner: String, actions: List<AppBarAction>) {
        currentOwner = owner
        _extraActions.value = actions
    }

    fun clearExtraActions(owner: String) {
        if (currentOwner == owner) {
            currentOwner = null
            _extraActions.value = emptyList()
        }
    }

    private var currentOwner: String? = null
}