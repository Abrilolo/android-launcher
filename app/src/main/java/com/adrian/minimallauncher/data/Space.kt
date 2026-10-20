package com.adrian.minimallauncher.data

import java.time.LocalDateTime
import java.time.LocalTime

enum class AppGroup {
    ESSENTIAL,
    OTHER,
    ENTERTAINMENT,
}

data class Space(
    val name: String,
    val startsAt: LocalTime,
    val endsAt: LocalTime,
    val availableGroups: Set<AppGroup>,
) {
    fun contains(time: LocalTime): Boolean =
        if (startsAt < endsAt) time >= startsAt && time < endsAt
        else time >= startsAt || time < endsAt

    fun allows(app: LauncherApp): Boolean = when {
        app.isVault -> true
        app.isEssential -> AppGroup.ESSENTIAL in availableGroups
        app.isEntertainment -> AppGroup.ENTERTAINMENT in availableGroups
        else -> AppGroup.OTHER in availableGroups
    }

    companion object {
        val all: List<Space> = listOf(
            Space("Mañana", LocalTime.of(6, 0), LocalTime.NOON, setOf(AppGroup.ESSENTIAL)),
            Space("Trabajo", LocalTime.NOON, LocalTime.of(18, 0), setOf(AppGroup.ESSENTIAL, AppGroup.OTHER)),
            Space("Noche", LocalTime.of(18, 0), LocalTime.of(6, 0), AppGroup.entries.toSet()),
        )

        fun at(now: LocalDateTime, spaces: List<Space> = all): Space = spaces.first { it.contains(now.toLocalTime()) }
    }
}
