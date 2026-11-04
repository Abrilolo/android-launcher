package com.adrian.minimallauncher.data

enum class AppSection { ESSENTIAL, ENTERTAINMENT, GAMES, OTHER, VAULT }

/**
 * A user-editable app category in the swipe-up App Drawer (e.g. Esenciales, Escuela, Entretenimiento, Juegos, Todas).
 * When [isDistraction] is true, apps in this category require the pause when the current phase/space blocks leisure.
 * When [visible] is true, the category tab is shown in the App Drawer.
 */
data class AppCategory(
    val id: String,
    val name: String,
    val isDistraction: Boolean = false,
    val visible: Boolean = true,
)

val DEFAULT_APP_CATEGORIES = listOf(
    AppCategory(id = "apps", name = "Esenciales", isDistraction = false, visible = true),
    AppCategory(id = "school", name = "Escuela", isDistraction = false, visible = true),
    AppCategory(id = "entertainment", name = "Entretenimiento", isDistraction = true, visible = true),
    AppCategory(id = "games", name = "Juegos", isDistraction = true, visible = true),
    AppCategory(id = "all", name = "Todas", isDistraction = false, visible = true),
)

data class LauncherApp(
    val packageName: String,
    val label: String,
    val section: AppSection,
    val isEntertainment: Boolean,
    val isVault: Boolean = false,
    val categoryId: String = when (section) {
        AppSection.ESSENTIAL -> "apps"
        AppSection.ENTERTAINMENT -> "entertainment"
        AppSection.GAMES -> "games"
        AppSection.OTHER -> "all"
        AppSection.VAULT -> "vault"
    },
) {
    val isEssential: Boolean get() = section == AppSection.ESSENTIAL
}

