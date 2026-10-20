package com.adrian.minimallauncher.data
enum class AppSection { ESSENTIAL, ENTERTAINMENT, GAMES, OTHER }
data class LauncherApp(val packageName: String, val label: String, val section: AppSection, val isEntertainment: Boolean) {
    val isEssential: Boolean get() = section == AppSection.ESSENTIAL
}
