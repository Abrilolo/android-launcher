package com.adrian.minimallauncher.data

import android.content.ClipData
import android.content.Context
import androidx.core.content.FileProvider
import java.io.File
import android.content.Intent
import android.content.pm.LauncherApps
import android.net.Uri
import android.os.Process
import com.adrian.minimallauncher.feature.ai.AppShortcut

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
    // Protected bank and financial vault apps
    private val vaultApps = listOf(
        CuratedApp(listOf("nu", "nubank"), listOf("com.nu.production")),
        CuratedApp(listOf("bbva", "bancomer"), listOf("com.bancomer.mbanking")),
        CuratedApp(listOf("mercadopago"), listOf("com.mercadopago.wallet")),
        CuratedApp(listOf("wallet", "googlewallet"), listOf("com.google.android.apps.walletnfcrel")),
        CuratedApp(listOf("stori"), listOf("com.storicard.stori")),
        CuratedApp(listOf("plata", "platacard"), listOf("card.plata.app")),
        CuratedApp(listOf("revolut"), listOf("com.revolut.revolut")),
        CuratedApp(listOf("bitso"), listOf("com.bitso.exchange", "com.bitso.wallet")),
        CuratedApp(listOf("dolarapp"), listOf("com.dolarapp")),
        CuratedApp(listOf("gbm", "gmb", "gbmhomebroker"), listOf("com.gbm.homebroker", "com.gbm.app")),
        CuratedApp(listOf("binance"), listOf("com.binance.dev")),
    )

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
                isVault = section == AppSection.VAULT,
            )
            app to order
        }.distinctBy { it.first.packageName }
            .sortedWith(compareBy({ it.second }, { it.first.label.lowercase() }))
            .map { it.first }
    }

    private fun classify(packageName: String, label: String): Pair<AppSection, Int> {
        listOf(
            AppSection.VAULT to vaultApps,
            AppSection.ESSENTIAL to essentialApps,
            AppSection.ENTERTAINMENT to entertainmentApps,
            AppSection.GAMES to gameApps,
        ).forEach { (section, curated) ->
            val index = curated.indexOfFirst { it.matches(packageName, label) }
            if (index >= 0) return section to index
        }
        return AppSection.OTHER to Int.MAX_VALUE
    }

    fun openApp(packageName: String) {
        val launcherApps = context.getSystemService(LauncherApps::class.java)
        val activity = runCatching { launcherApps?.getActivityList(packageName, Process.myUserHandle())?.firstOrNull() }.getOrNull()
        if (launcherApps != null && activity != null && runCatching { launcherApps.startMainActivity(activity.componentName, Process.myUserHandle(), null, null) }.isSuccess) return
        context.packageManager.getLaunchIntentForPackage(packageName)?.let { intent ->
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            context.startActivity(intent)
        }
    }

    /**
     * Up to four shortcuts each app publishes (new chat, voice…), the ones in its manifest first. Only the default
     * launcher may read them, so this is empty until the user picks this launcher as home.
     */
    fun loadShortcuts(packages: Collection<String>): Map<String, List<AppShortcut>> {
        val launcherApps = context.getSystemService(LauncherApps::class.java) ?: return packages.associateWith(::builtInShortcuts)
        if (runCatching { launcherApps.hasShortcutHostPermission() }.getOrDefault(false).not()) return packages.associateWith(::builtInShortcuts)
        return packages.associateWith { pkg ->
            val query = LauncherApps.ShortcutQuery()
                .setPackage(pkg)
                .setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC)
            runCatching { launcherApps.getShortcuts(query, Process.myUserHandle()) }.getOrNull().orEmpty()
                .filter { it.isEnabled }
                .sortedWith(compareBy({ !it.isDeclaredInManifest }, { it.rank }))
                .mapNotNull { info -> (info.shortLabel ?: info.longLabel)?.toString()?.takeIf(String::isNotBlank)?.let { AppShortcut(pkg, info.id, it) } }
                .ifEmpty { builtInShortcuts(pkg) }
        }
    }

    /**
     * Gemini publishes no shortcuts: its assistant lives in the Google app. These open it there instead, each only
     * when the phone can handle it: talk to it, or point the camera with Lens.
     */
    private fun builtInShortcuts(packageName: String): List<AppShortcut> {
        if (packageName != GEMINI_PACKAGE) return emptyList()
        return listOf("voice" to "HABLAR", "lens" to "LENS")
            .filter { (id, _) -> context.packageManager.resolveActivity(builtInIntent(id), 0) != null }
            .map { (id, label) -> AppShortcut(packageName, BUILT_IN + id, label) }
    }

    private fun builtInIntent(id: String): Intent = when (id) {
        "voice" -> Intent(Intent.ACTION_VOICE_COMMAND).setPackage(GOOGLE_APP)
        else -> Intent().setClassName(GOOGLE_APP, "com.google.android.apps.search.lens.LensExportedActivity")
    }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun startShortcut(packageName: String, id: String) {
        if (id.startsWith(BUILT_IN)) {
            runCatching { context.startActivity(builtInIntent(id.removePrefix(BUILT_IN))) }.onFailure { openApp(packageName) }
            return
        }
        val launcherApps = context.getSystemService(LauncherApps::class.java) ?: return
        runCatching { launcherApps.startShortcut(packageName, id, null, null, Process.myUserHandle()) }
            .onFailure { openApp(packageName) }
    }

    /** A new file for the camera to save a photo into, readable by the app it is shared with. */
    fun newPhotoUri(): Uri {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, "photo_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    /** Hands an image to [packageName] as a share, so it lands in a new chat there. */
    fun shareImage(packageName: String, uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND)
            .setType("image/*")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .setPackage(packageName)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.clipData = ClipData.newRawUri("", uri)
        runCatching { context.startActivity(intent) }.onFailure { openApp(packageName) }
    }

    /** The app's page in the Play Store, or on the web if there is no store. */
    fun openStore(packageName: String) {
        val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(market) }.onFailure {
            runCatching {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        }
    }

    private companion object {
        const val GEMINI_PACKAGE = "com.google.android.apps.bard"
        const val GOOGLE_APP = "com.google.android.googlequicksearchbox"
        const val BUILT_IN = "launcher:"
    }
}
