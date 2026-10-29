package com.adrian.minimallauncher.feature.exercise

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Handler
import android.os.Looper
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.time.LocalDate

private const val SESSION_URL = "https://exercise.local/index.html"

/** Local-only renderer for the reviewed SVG motion engine; Room owns all durable state. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun GuidedExerciseScreen(date: LocalDate, settings: ModuleSettings, onExit: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val exit by rememberUpdatedState(onExit)
    val repository = remember(context) { GuidedExerciseRepository(LauncherDatabase.get(context)) }
    val loaded by produceState<Result<String>?>(null, date) {
        value = withContext(Dispatchers.IO) { runCatching { repository.load(date, settings) } }
    }
    var view by remember { mutableStateOf<WebView?>(null) }
    BackHandler { view?.evaluateJavascript("window.sessionBack && window.sessionBack()", null) ?: exit() }
    DisposableEffect(lifecycle, view) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> view?.let { web ->
                    web.evaluateJavascript("window.sessionCheckpoint && window.sessionCheckpoint()") { web.onPause() }
                }
                Lifecycle.Event.ON_RESUME -> view?.let { web ->
                    web.onResume()
                    web.evaluateJavascript("window.sessionResume && window.sessionResume()", null)
                }
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val result = loaded
    if (result == null || result.isFailure) {
        Column(modifier.fillMaxSize()) {
            Text(if (result == null) "Preparando tu sesión…" else "No se pudo abrir la sesión. Tus registros siguen guardados.")
            Button(onClick = { exit() }) { Text("Volver") }
        }
        return
    }
    val bridge = remember(date, result) {
        GuidedExerciseBridge(result.getOrThrow(), repository, date) { exit() }
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
    private val onExit: () -> Unit,
) {
    @JavascriptInterface fun load(): String = initial
    @JavascriptInterface fun save(snapshot: String): Boolean = runBlocking(Dispatchers.IO) { repository.save(date, snapshot) }
    @JavascriptInterface fun exit() { Handler(Looper.getMainLooper()).post(onExit) }
}
