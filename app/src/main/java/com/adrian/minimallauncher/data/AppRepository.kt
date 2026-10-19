package com.adrian.minimallauncher.data
import android.content.Context
import android.content.Intent
class AppRepository(private val context: Context) {
    private val entertainmentPackages = setOf("com.instagram.android", "com.twitter.android", "com.facebook.katana", "com.google.android.youtube")
    // Shown on the main screen in this order.
    private val essentialPackages = listOf(
        "com.whatsapp",
        "com.samsung.android.app.notes",
        "com.sec.android.gallery3d",
        "com.sec.android.app.camera",
        "com.android.chrome",
        "com.samsung.android.calendar",
        "com.samsung.android.dialer",
        "com.android.settings",
    )
    fun loadApps(): List<LauncherApp> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        return pm.queryIntentActivities(intent, 0).mapNotNull { info ->
            val packageName = info.activityInfo.packageName
            if (packageName == context.packageName) return@mapNotNull null
            LauncherApp(packageName, info.loadLabel(pm).toString(), packageName in entertainmentPackages, packageName in essentialPackages)
        }.distinctBy { it.packageName }
            .sortedWith(compareBy({ essentialPackages.indexOf(it.packageName).let { i -> if (i < 0) Int.MAX_VALUE else i } }, { it.label.lowercase() }))
    }
    fun openApp(packageName: String) {
        context.packageManager.getLaunchIntentForPackage(packageName)?.let { intent ->
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}
