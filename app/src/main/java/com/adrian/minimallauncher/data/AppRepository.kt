package com.adrian.minimallauncher.data
import android.content.Context
import android.content.Intent
/** An app the user picked by name; matched by known package or by its launcher label. */
private class CuratedApp(val names: List<String>, val packages: List<String> = emptyList(), val prefix: Boolean = false) {
    fun matches(packageName: String, label: String): Boolean {
        if (packageName in packages) return true
        val normalized = label.normalized()
        return names.any { name -> normalized == name || (prefix && normalized.startsWith(name)) }
    }
}
private fun String.normalized() = lowercase().filter(Char::isLetterOrDigit)
class AppRepository(private val context: Context) {
    // Not shown in a section of their own, but restricted like entertainment.
    private val distractingPackages = setOf("com.instagram.android", "com.twitter.android", "com.facebook.katana")
    // Each list is shown on the main screen in this order.
    private val essentialApps = listOf(
        CuratedApp(listOf("whatsapp"), listOf("com.whatsapp")),
        CuratedApp(emptyList(), listOf("com.samsung.android.app.notes")),
        CuratedApp(emptyList(), listOf("com.sec.android.gallery3d")),
        CuratedApp(emptyList(), listOf("com.sec.android.app.camera")),
        CuratedApp(emptyList(), listOf("com.android.chrome")),
        CuratedApp(emptyList(), listOf("com.samsung.android.calendar")),
        CuratedApp(emptyList(), listOf("com.samsung.android.dialer")),
        CuratedApp(listOf("spotify"), listOf("com.spotify.music")),
        CuratedApp(listOf("outlook"), listOf("com.microsoft.office.outlook")),
        CuratedApp(emptyList(), listOf("com.android.settings")),
    )
    private val entertainmentApps = listOf(
        CuratedApp(listOf("netflix"), listOf("com.netflix.mediaclient")),
        CuratedApp(listOf("primevideo", "amazonprimevideo"), listOf("com.amazon.avod.thirdpartyclient")),
        CuratedApp(listOf("max", "hbomax"), listOf("com.wbd.stream", "com.hbo.hbonow")),
        CuratedApp(listOf("izzigo"), listOf("tv.mirada.iris.android.inspire.izzigo"), prefix = true),
        CuratedApp(listOf("loselegidos", "thechosen"), listOf("net.comeandsee.thechosen"), prefix = true),
        CuratedApp(listOf("appletv"), listOf("com.apple.atve.androidtv.appletv")),
        CuratedApp(listOf("disney", "disneyplus"), listOf("com.disney.disneyplus")),
        CuratedApp(listOf("vix"), listOf("com.univision.prendetv")),
        CuratedApp(listOf("crunchyroll"), listOf("com.crunchyroll.crunchyroid")),
        CuratedApp(listOf("foxone"), listOf("com.foxappy.astericmx"), prefix = true),
        CuratedApp(listOf("youtube"), listOf("com.google.android.youtube")),
    )
    private val gameApps = listOf(
        CuratedApp(listOf("ruleta"), listOf("com.kamagames.roulettist"), prefix = true),
        CuratedApp(listOf("pocketchess"), listOf("com.dkxqzbfkjt.pocketchess"), prefix = true),
        CuratedApp(listOf("amongus"), listOf("com.innersloth.spacemafia"), prefix = true),
        CuratedApp(listOf("blackjack"), listOf("com.tripledot.blackjack"), prefix = true),
        CuratedApp(listOf("nowifigames"), listOf("com.nebula.offlinegames"), prefix = true),
        CuratedApp(listOf("dartsclub"), listOf("com.boombitgames.Dartsy"), prefix = true),
        CuratedApp(listOf("nonogram", "nonogramcom"), listOf("com.easybrain.nonogram"), prefix = true),
        CuratedApp(listOf("brawlstar"), listOf("com.supercell.brawlstars"), prefix = true),
    )
    fun loadApps(): List<LauncherApp> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        return pm.queryIntentActivities(intent, 0).mapNotNull { info ->
            val packageName = info.activityInfo.packageName
            if (packageName == context.packageName) return@mapNotNull null
            val label = info.loadLabel(pm).toString()
            val (section, order) = classify(packageName, label)
            val app = LauncherApp(
                packageName, label, section,
                isEntertainment = section == AppSection.ENTERTAINMENT || section == AppSection.GAMES || packageName in distractingPackages,
            )
            app to order
        }.distinctBy { it.first.packageName }
            .sortedWith(compareBy({ it.second }, { it.first.label.lowercase() }))
            .map { it.first }
    }
    private fun classify(packageName: String, label: String): Pair<AppSection, Int> {
        listOf(AppSection.ESSENTIAL to essentialApps, AppSection.ENTERTAINMENT to entertainmentApps, AppSection.GAMES to gameApps)
            .forEach { (section, curated) ->
                val index = curated.indexOfFirst { it.matches(packageName, label) }
                if (index >= 0) return section to index
            }
        return AppSection.OTHER to Int.MAX_VALUE
    }
    fun openApp(packageName: String) {
        context.packageManager.getLaunchIntentForPackage(packageName)?.let { intent ->
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}
