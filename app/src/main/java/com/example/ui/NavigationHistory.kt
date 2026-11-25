package com.example.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Pure Back/Forward navigation history controller synced with Jetpack Navigation routes.
 */
class RouteHistoryState(initialRoute: String) {
    private val _entries = mutableStateListOf(initialRoute)
    var index by mutableIntStateOf(0)
        private set

    val entries: List<String> get() = _entries
    val currentRoute: String get() = _entries.getOrElse(index) { _entries.first() }
    val canGoBack: Boolean get() = index > 0
    val canGoForward: Boolean get() = index < _entries.lastIndex

    fun push(route: String): Boolean {
        if (currentRoute == route) return false
        while (_entries.size > index + 1) {
            _entries.removeAt(_entries.lastIndex)
        }
        _entries.add(route)
        index = _entries.lastIndex
        return true
    }

    fun goBack(): String? {
        if (!canGoBack) return null
        index -= 1
        return currentRoute
    }

    fun goForward(): String? {
        if (!canGoForward) return null
        index += 1
        return currentRoute
    }
}

@Composable
fun rememberRouteHistoryState(initialRoute: String): RouteHistoryState =
    remember(initialRoute) { RouteHistoryState(initialRoute) }
