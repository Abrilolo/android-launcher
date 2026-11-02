package com.adrian.minimallauncher.feature.exercise

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.adrian.minimallauncher.data.LauncherDatabase
import com.adrian.minimallauncher.data.ModuleSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.time.LocalDate

private const val SESSION_URL = "https://exercise.local/index.html"

/** Local-only renderer for the reviewed SVG motion engine; Room owns all durable state. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun GuidedExerciseScreen(
    day: ExerciseDay,
    settings: ModuleSettings,
    phaseId: String,
    excess: String,
    onExit: () -> Unit,
    onOpenNative: (String) -> Unit,
    onStartTime: (java.time.LocalTime) -> Unit,
    onRestSeconds: (Int) -> Unit,
    /** A native screen is over the scene: it stays alive underneath, only stopped. */
    covered: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val date = day.date
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val exit by rememberUpdatedState(onExit)
    val openNative by rememberUpdatedState(onOpenNative)
    val startTime by rememberUpdatedState(onStartTime)
    val restSeconds by rememberUpdatedState(onRestSeconds)
    val currentSettings by rememberUpdatedState(settings)
    val isCovered by rememberUpdatedState(covered)
    // Read once per scene, and again when Guardar reloads it, so the session never restarts by itself.
    val inputs by rememberUpdatedState { boot: String? ->
        val now = java.time.LocalTime.now()
        val session = day.session
        val training = session.kind != SessionKind.REST && !day.completed && !now.isBefore(settings.exerciseStart) && now.isBefore(settings.exerciseStart.plusHours(2))
        SceneInputs(RoutinePlans.scenePhase(phaseId, now, training), training, day.completed, excess, boot)
    }
    val repository = remember(context) { GuidedExerciseRepository(LauncherDatabase.get(context)) }
    val loaded by produceState<Result<String>?>(null, date) {
        value = withContext(Dispatchers.IO) { runCatching { repository.load(date, settings, inputs(null)) } }
    }
    var view by remember { mutableStateOf<WebView?>(null) }
    val result = loaded
    val bridge = remember(date, result) {
        result?.getOrNull()?.let { initial ->
            GuidedExerciseBridge(
                initial, repository, date, context.applicationContext, context.getSystemService(Vibrator::class.java),
                hapticsOn = { currentSettings.haptics },
                reload = { boot -> repository.load(date, currentSettings, inputs(boot)) },
                onOpenNative = { openNative(it) },
                onStartTime = { startTime(it) },
                onRestSeconds = { restSeconds(it) },
                onKeepAwake = { awake -> view?.keepScreenOn = awake },
            ) { exit() }
        }
    }
    BackHandler { view?.evaluateJavascript("window.sessionBack && window.sessionBack()", null) ?: exit() }
    fun pauseScene(web: WebView) {
        ExerciseTimerAlert.sceneVisible = false
        bridge?.stopVibration()
        web.evaluateJavascript("window.sessionPause && window.sessionPause(); window.sessionCheckpoint && window.sessionCheckpoint()") {
            bridge?.flush()
            web.onPause()
        }
    }
    fun resumeScene(web: WebView) {
        ExerciseTimerAlert.sceneVisible = true
        web.onResume()
        web.evaluateJavascript("window.sessionResume && window.sessionResume()", null)
    }
    DisposableEffect(lifecycle, view, bridge) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> view?.let { if (!isCovered) pauseScene(it) }
                Lifecycle.Event.ON_RESUME -> view?.let { if (!isCovered) resumeScene(it) }
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            ExerciseTimerAlert.sceneVisible = false
            bridge?.stopVibration()
            bridge?.flush()
        }
    }
    LaunchedEffect(covered, view) {
        val web = view ?: return@LaunchedEffect
        if (covered) pauseScene(web) else if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) resumeScene(web)
    }
    // The rest length has one home: a change in the native settings reaches the running scene.
    LaunchedEffect(settings.exerciseRestSeconds, view) {
        view?.evaluateJavascript("window.setRestSeconds && window.setRestSeconds(${settings.exerciseRestSeconds})", null)
    }
    if (result?.isFailure == true) {
        Column(modifier.fillMaxSize()) {
            Text("No se pudo abrir la sesión. Tus registros siguen guardados.")
            Button(onClick = { exit() }) { Text("Volver") }
        }
        return
    }
    if (result == null || bridge == null) {
        // Nothing to read while it opens: black, the color the scene arrives on.
        Box(modifier.fillMaxSize())
        return
    }
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            WebView(ctx).apply {
                // AndroidView defaults to WRAP_CONTENT, which makes WebView resolve 100% and 100vh to 0.
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                setBackgroundColor(Color.BLACK)
                this.settings.javaScriptEnabled = true
                this.settings.allowFileAccess = false
                this.settings.allowContentAccess = false
                this.settings.domStorageEnabled = false
                this.settings.blockNetworkLoads = true
                this.settings.setSupportMultipleWindows(false)
                this.settings.textZoom = 100
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = true
                    override fun onPageFinished(view: WebView?, url: String?) {
                        view?.evaluateJavascript("JSON.stringify({title:document.title,body:document.body?.innerText?.slice(0,300),stage:typeof S==='undefined'?'missing':S.stage,width:innerWidth,height:innerHeight})") {
                            Log.d("GuidedExercise", "Page loaded: $it")
                        }
                    }
                    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse {
                        return if (request?.url.toString() == SESSION_URL) {
                            WebResourceResponse("text/html", "utf-8", ctx.assets.open("exercise-session/index.html"))
                        } else WebResourceResponse("text/plain", "utf-8", 403, "Blocked", emptyMap(), ByteArrayInputStream(byteArrayOf()))
                    }
                }
                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                        if (message.messageLevel() == ConsoleMessage.MessageLevel.ERROR) Log.e("GuidedExercise", "${message.lineNumber()}: ${message.message()}")
                        return true
                    }
                }
                addJavascriptInterface(bridge, "AndroidSession")
                view = this
                loadUrl(SESSION_URL)
            }
        },
        onRelease = { web ->
            bridge.stopVibration()
            bridge.flush()
            view = null
            web.stopLoading()
            web.removeJavascriptInterface("AndroidSession")
            web.destroy()
        },
    )
}

/** Exposed only to a bundled document at a single intercepted origin; no arbitrary URL/file API. */
internal class GuidedExerciseBridge(
    private val initial: String,
    private val repository: GuidedExerciseRepository,
    private val date: LocalDate,
    private val context: Context,
    private val vibrator: Vibrator,
    /** False when vibration is off in Módulos. */
    private val hapticsOn: () -> Boolean,
    private val reload: suspend (String?) -> String,
    private val onOpenNative: (String) -> Unit,
    private val onStartTime: (java.time.LocalTime) -> Unit,
    private val onRestSeconds: (Int) -> Unit,
    private val onKeepAwake: (Boolean) -> Unit,
    private val onExit: () -> Unit,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))
    @Volatile private var lastJob: Job? = null
    @Volatile private var nextBoot: String? = null
    @Volatile private var loads = 0

    /** The first load is what opened the screen; a later one is a scene reload after Guardar. */
    @JavascriptInterface fun load(): String {
        if (loads++ == 0) return initial
        flush()
        return runBlocking { reload(nextBoot) }.also { nextBoot = null }
    }

    /** Asks the next load to open on [stage]; a whitelisted name, since this comes from the page. */
    @JavascriptInterface fun reloadInto(stage: String) {
        if (stage in setOf("general", "routine")) nextBoot = stage
    }

    @JavascriptInterface
    fun savePlans(text: String): Boolean {
        if (text.length > 2_000_000 || !text.startsWith("{")) return false
        lastJob = scope.launch { runCatching { repository.savePlans(date, text) } }
        return true
    }

    @JavascriptInterface fun saveStartTime(text: String) {
        val time = runCatching { java.time.LocalTime.parse(text) }.getOrNull() ?: return
        Handler(Looper.getMainLooper()).post { onStartTime(time) }
    }

    @JavascriptInterface fun saveRestSeconds(seconds: Int) {
        Handler(Looper.getMainLooper()).post { onRestSeconds(seconds.coerceIn(10, 900)) }
    }

    @JavascriptInterface fun keepAwake(on: Boolean) {
        Handler(Looper.getMainLooper()).post { onKeepAwake(on) }
    }

    /** The next timer end, for when the scene is not there to feel it; 0 clears it. */
    @JavascriptInterface fun armAlert(at: Long) {
        ExerciseTimerAlert.arm(context, if (hapticsOn()) at else 0)
    }

    /** Metas and Ajustes are native screens. */
    @JavascriptInterface fun openNative(target: String) {
        if (target !in setOf("goals", "settings")) return
        flush()
        Handler(Looper.getMainLooper()).post { onOpenNative(target) }
    }

    @JavascriptInterface
    fun save(snapshot: String): Boolean {
        if (snapshot.length > 2_000_000 || !snapshot.startsWith("{")) return false
        lastJob = scope.launch { repository.save(date, snapshot) }
        return true
    }

    @JavascriptInterface
    fun reset(): Boolean {
        lastJob = scope.launch { repository.reset(date) }
        return true
    }

    /** A short pulse for the start gesture; strength 1-255 where the motor supports it. */
    @JavascriptInterface
    fun vibrate(ms: Int, amplitude: Int) {
        if (!hapticsOn()) return
        val strength = if (vibrator.hasAmplitudeControl()) amplitude.coerceIn(1, 255) else VibrationEffect.DEFAULT_AMPLITUDE
        vibrator.vibrate(VibrationEffect.createOneShot(ms.coerceIn(1, 200).toLong(), strength))
    }

    /** One of [SessionHaptics]' kinds; anything else from the page is ignored. */
    @JavascriptInterface
    fun haptic(kind: String) {
        if (hapticsOn()) SessionHaptics.effect(kind, vibrator)?.let(vibrator::vibrate)
    }

    @JavascriptInterface
    fun stopVibration() {
        vibrator.cancel()
    }

    fun flush() {
        val job = lastJob ?: return
        if (job.isActive) runBlocking { job.join() }
    }

    @JavascriptInterface
    fun exit() {
        flush()
        Handler(Looper.getMainLooper()).post(onExit)
    }
}
