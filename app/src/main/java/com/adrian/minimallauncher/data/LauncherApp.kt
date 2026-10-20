package com.adrian.minimallauncher.data

enum class AppSection { ESSENTIAL, ENTERTAINMENT, GAMES, OTHER, VAULT }

data class LauncherApp(
    val packageName: String,
    val label: String,
    val section: AppSection,
    val isEntertainment: Boolean,
    val isVault: Boolean = false,
) {
    val isEssential: Boolean get() = section == AppSection.ESSENTIAL
}
