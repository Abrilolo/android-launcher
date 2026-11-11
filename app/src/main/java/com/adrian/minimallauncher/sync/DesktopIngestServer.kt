package com.adrian.minimallauncher.sync

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import androidx.room.withTransaction
import com.adrian.minimallauncher.data.DayCheck
import com.adrian.minimallauncher.data.FinanceRecord
import com.adrian.minimallauncher.data.LauncherDatabase
import com.adrian.minimallauncher.data.MealRecord
import com.adrian.minimallauncher.data.MoneyKind
import com.adrian.minimallauncher.data.NoteCategory
import com.adrian.minimallauncher.data.NoteRecord
import com.adrian.minimallauncher.data.ReminderRecord
import com.adrian.minimallauncher.data.ReminderScheduler
import com.adrian.minimallauncher.data.routine.RoutineDefaults
import com.adrian.minimallauncher.feature.exercise.SetLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStreamWriter
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URL
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class BridgeMessage(
    val id: String = UUID.randomUUID().toString(),
    val from: String, // "PC" or "S24+"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val delivered: Boolean = true,
)

data class BridgeStatus(
    val running: Boolean = false,
    val port: Int = DesktopIngestServer.DEFAULT_PORT,
    val localIps: List<String> = emptyList(),
    val lastClientIp: String? = null,
    val lastSeenAt: Long? = null,
    val totalIngested: Int = 0,
    val pcTargetUrl: String = "",
    val lastActionStatus: String? = null,
) {
    val isConnected: Boolean
        get() = lastClientIp != null && lastClientIp != "127.0.0.1"
}

/**
 * Local HTTP (`TCP :8765`) + Auto-Discovery (`UDP :8765`) server for Nook.
 * When PC and phone are on the same Wi-Fi, the PC broadcasts `NOOK_DISCOVER` over UDP and connects automatically.
 */
class DesktopIngestServer(
    private val db: LauncherDatabase,
    private val reminderScheduler: ReminderScheduler,
    private val currentPhaseIdProvider: () -> String = { RoutineDefaults.TRABAJO },
    private val port: Int = DEFAULT_PORT,
    private val context: Context? = null,
) {
    private var serverJob: Job? = null
    private var udpJob: Job? = null
    private var serverSocket: ServerSocket? = null
    private var udpSocket: DatagramSocket? = null
    private var multicastLock: WifiManager.MulticastLock? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _status = MutableStateFlow(BridgeStatus(port = port, localIps = detectLocalIpv4Addresses()))
    val status: StateFlow<BridgeStatus> = _status.asStateFlow()

    private val _messages = MutableStateFlow<List<BridgeMessage>>(emptyList())
    val messages: StateFlow<List<BridgeMessage>> = _messages.asStateFlow()

    fun refreshNetworkInterfaces() {
        _status.value = _status.value.copy(localIps = detectLocalIpv4Addresses())
    }

    fun setPcTargetUrl(url: String) {
        _status.value = _status.value.copy(pcTargetUrl = url.trim())
    }

    private fun acquireMulticastLock() {
        val appCtx = context?.applicationContext ?: return
        runCatching {
            val wifi = appCtx.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return
            val lock = multicastLock ?: wifi.createMulticastLock("nook-bridge-udp").apply {
                setReferenceCounted(false)
                multicastLock = this
            }
            if (!lock.isHeld) lock.acquire()
        }
    }

    private fun releaseMulticastLock() {
        runCatching {
            if (multicastLock?.isHeld == true) multicastLock?.release()
        }
    }

    fun start() {
        if (serverJob?.isActive == true) return
        refreshNetworkInterfaces()
        acquireMulticastLock()
        serverJob = scope.launch {
            try {
                var socket: ServerSocket? = null
                for (attempt in 1..4) {
                    try {
                        val candidate = ServerSocket()
                        candidate.reuseAddress = true
                        candidate.bind(InetSocketAddress("0.0.0.0", port))
                        socket = candidate
                        break
                    } catch (e: Exception) {
                        if (attempt == 4) throw e
                        delay(250L)
                    }
                }
                val bound = socket ?: return@launch
                serverSocket = bound
                _status.value = _status.value.copy(running = true, localIps = detectLocalIpv4Addresses())
                while (isActive) {
                    val client = bound.accept() ?: break
                    launch { handleClient(client) }
                }
            } catch (e: Exception) {
                _status.value = _status.value.copy(running = false)
                if (isActive) Log.e(TAG, "Error en servidor HTTP", e)
            }
        }
        udpJob = scope.launch {
            try {
                var dgram: DatagramSocket? = null
                for (attempt in 1..4) {
                    try {
                        val candidate = DatagramSocket(null)
                        candidate.reuseAddress = true
                        candidate.broadcast = true
                        candidate.bind(InetSocketAddress("0.0.0.0", port))
                        dgram = candidate
                        break
                    } catch (e: Exception) {
                        if (attempt == 4) throw e
                        delay(250L)
                    }
                }
                val bound = dgram ?: return@launch
                udpSocket = bound
                val buf = ByteArray(256)
                while (isActive) {
                    val packet = DatagramPacket(buf, buf.size)
                    bound.receive(packet)
                    val msg = String(packet.data, 0, packet.length, StandardCharsets.UTF_8).trim()
                    if (msg.startsWith("NOOK")) {
                        val remoteIp = packet.address?.hostAddress
                        if (remoteIp != null) markClientSeen(remoteIp)
                        val reply = """{"status":"ok","app":"minimal-launcher","port":$port}""".toByteArray(StandardCharsets.UTF_8)
                        bound.send(DatagramPacket(reply, reply.size, packet.address, packet.port))
                    }
                }
            } catch (e: Exception) {
                if (isActive) Log.e(TAG, "Error en UDP discovery", e)
            }
        }
    }

    fun stop() {
        serverJob?.cancel()
        udpJob?.cancel()
        runCatching { serverSocket?.close() }
        runCatching { udpSocket?.close() }
        releaseMulticastLock()
        serverSocket = null
        udpSocket = null
        _status.value = _status.value.copy(running = false)
    }

    fun sendMessageToPc(text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        scope.launch {
            val targetUrl = _status.value.pcTargetUrl.ifBlank {
                _status.value.lastClientIp?.takeIf { it != "127.0.0.1" }?.let { "http://$it:8766/api/v1/message" }.orEmpty()
            }
            if (targetUrl.startsWith("http")) {
                runCatching {
                    val conn = (URL(targetUrl).openConnection() as HttpURLConnection).apply {
                        requestMethod = "POST"
                        connectTimeout = 2_000
                        readTimeout = 2_000
                        doOutput = true
                        setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    }
                    val payload = JSONObject()
                        .put("from", "S24+")
                        .put("text", clean)
                        .put("timestamp", System.currentTimeMillis())
                        .toString()
                    conn.outputStream.use { it.write(payload.toByteArray(StandardCharsets.UTF_8)) }
                    conn.responseCode
                }
            }
            appendMessage(BridgeMessage(from = "S24+", text = clean))
        }
    }

    fun runSelfTest() {
        scope.launch {
            runCatching {
                val conn = (URL("http://127.0.0.1:$port/api/v1/message").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 2_000
                    readTimeout = 2_000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                }
                val testJson = JSONObject().put("text", "Prueba local OK").toString()
                conn.outputStream.use { it.write(testJson.toByteArray(StandardCharsets.UTF_8)) }
                conn.responseCode
            }
        }
    }

    private fun appendMessage(msg: BridgeMessage) {
        _messages.value = (listOf(msg) + _messages.value).take(50)
    }

    private suspend fun handleClient(socket: Socket) = withContext(Dispatchers.IO) {
        socket.use { client ->
            client.soTimeout = 5_000
            val remoteIp = client.inetAddress?.hostAddress ?: "PC"
            val input = client.getInputStream()
            val requestLine = readAsciiLine(input) ?: return@withContext

            val parts = requestLine.split(" ")
            val method = parts.getOrNull(0).orEmpty()
            val path = parts.getOrNull(1).orEmpty().substringBefore('?')

            var contentLength = 0
            var userAgent = ""
            var forwardedIp = ""
            while (true) {
                val line = readAsciiLine(input) ?: break
                if (line.isEmpty()) break
                when {
                    line.startsWith("Content-Length:", ignoreCase = true) -> {
                        contentLength = line.substringAfter(":").trim().toIntOrNull() ?: 0
                    }
                    line.startsWith("User-Agent:", ignoreCase = true) -> {
                        userAgent = line.substringAfter(":").trim()
                    }
                    line.startsWith("X-Nook-Client-Ip:", ignoreCase = true) -> {
                        forwardedIp = line.substringAfter(":").trim()
                    }
                }
            }

            val effectiveIp = when {
                forwardedIp.isNotEmpty() && forwardedIp != "127.0.0.1" -> forwardedIp
                remoteIp == "127.0.0.1" && (
                    userAgent.contains("nook-bridge", ignoreCase = true) ||
                        path == "/api/v1/health" ||
                        path == "/api/v1/ingest"
                    ) -> "PC"
                else -> remoteIp
            }
            markClientSeen(effectiveIp)

            if (method == "OPTIONS") {
                sendResponse(client, 200, """{"status":"ok"}""")
                return@withContext
            }

            if (method == "GET" && (path == "/" || path == "/api/v1/health" || path == "/api/v1/ingest")) {
                val health = JSONObject()
                    .put("status", "ok")
                    .put("app", "minimal-launcher")
                    .put("port", port)
                sendResponse(client, 200, health.toString())
                return@withContext
            }

            if (method == "GET" && path == "/api/v1/messages") {
                val arr = JSONArray(
                    _messages.value.map {
                        JSONObject()
                            .put("id", it.id)
                            .put("from", it.from)
                            .put("text", it.text)
                            .put("timestamp", it.timestamp)
                    }
                )
                sendResponse(client, 200, JSONObject().put("status", "ok").put("messages", arr).toString())
                return@withContext
            }

            if (method != "POST" || (path != "/api/v1/ingest" && path != "/api/v1/message")) {
                sendResponse(client, 404, """{"error":"Not Found"}""")
                return@withContext
            }

            if (contentLength <= 0 || contentLength > MAX_BODY_BYTES) {
                sendResponse(client, 400, """{"error":"Invalid Content-Length"}""")
                return@withContext
            }

            val bodyBytes = ByteArray(contentLength)
            var totalRead = 0
            while (totalRead < contentLength) {
                val read = input.read(bodyBytes, totalRead, contentLength - totalRead)
                if (read == -1) break
                totalRead += read
            }
            val rawJson = String(bodyBytes, 0, totalRead, StandardCharsets.UTF_8)

            try {
                val json = JSONObject(rawJson)
                if (path == "/api/v1/message") {
                    val text = json.optString("text", json.optString("message", "")).trim()
                    if (text.isNotEmpty()) {
                        appendMessage(BridgeMessage(from = "PC", text = text))
                    }
                    sendResponse(client, 200, """{"status":"ok","received":true}""")
                    return@withContext
                }

                val count = ingestPayload(json)
                sendResponse(client, 200, """{"status":"ok","ingested":$count}""")
            } catch (e: Exception) {
                Log.e(TAG, "Error insertando en Room", e)
                val safeMsg = e.message.orEmpty().replace("\"", "'")
                sendResponse(client, 400, """{"error":"$safeMsg"}""")
            }
        }
    }

    private fun markClientSeen(remoteIp: String) {
        val current = _status.value
        val updatedIp = if (remoteIp != "127.0.0.1" || current.lastClientIp == null) remoteIp else current.lastClientIp
        _status.value = current.copy(
            lastClientIp = updatedIp,
            lastSeenAt = System.currentTimeMillis(),
        )
    }

    private fun readAsciiLine(input: InputStream): String? {
        val buf = ByteArrayOutputStream(128)
        while (true) {
            val b = input.read()
            if (b == -1) return if (buf.size() == 0) null else buf.toString(StandardCharsets.UTF_8.name())
            if (b == '\n'.code) break
            if (b != '\r'.code) buf.write(b)
        }
        return buf.toString(StandardCharsets.UTF_8.name())
    }

    private suspend fun ingestPayload(root: JSONObject): Int {
        val personal = db.personal()
        val exerciseDao = db.exercise()
        val now = System.currentTimeMillis()
        val today = LocalDate.now().toString()
        val scheduledReminders = mutableListOf<ReminderRecord>()
        val summaryParts = mutableListOf<String>()
        var inserted = 0

        val directMessage = root.optString("message", root.optString("text", "")).trim()

        db.withTransaction {
            root.optJSONArray("reminders")?.let { arr ->
                val rows = mutableListOf<ReminderRecord>()
                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i) ?: continue
                    val title = item.optString("title").trim()
                    if (title.isEmpty()) continue
                    val (hour, minute) = parseHourMinute(item)
                    val rawPhase = item.optString("phaseId", item.optString("phase_id", "")).trim()
                    val phaseId = normalizePhaseId(rawPhase)
                    val record = ReminderRecord(
                        id = item.optString("id").ifBlank { UUID.randomUUID().toString() },
                        title = title,
                        hour = hour.coerceIn(0, 23),
                        minute = minute.coerceIn(0, 59),
                        phaseId = phaseId,
                    )
                    rows.add(record)
                    scheduledReminders.add(record)
                    summaryParts.add("$title · %02d:%02d".format(record.hour, record.minute))
                    inserted++
                }
                if (rows.isNotEmpty()) personal.insertReminders(rows)
            }

            root.optJSONArray("finances")?.let { arr ->
                val rows = mutableListOf<FinanceRecord>()
                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i) ?: continue
                    val amountCents = when {
                        item.has("amountCents") -> item.optLong("amountCents", 0L)
                        item.has("amount_cents") -> item.optLong("amount_cents", 0L)
                        item.has("amount") -> (item.optDouble("amount", 0.0) * 100.0).toLong()
                        else -> 0L
                    }
                    val description = item.optString("description", item.optString("concept", "")).trim()
                    if (amountCents <= 0L && description.isEmpty()) continue
                    val rawKind = item.optString("kind", item.optString("type", "EXTRA")).uppercase()
                    val kind = when (rawKind) {
                        "INCOME", "INCOME_FIXED" -> MoneyKind.INCOME_FIXED
                        else -> runCatching { MoneyKind.valueOf(rawKind) }.getOrDefault(MoneyKind.EXTRA)
                    }
                    val desc = description.ifEmpty { "Movimiento" }
                    rows.add(
                        FinanceRecord(
                            id = item.optString("id").ifBlank { UUID.randomUUID().toString() },
                            amountCents = amountCents,
                            description = desc,
                            createdAt = item.optLong("createdAt", now + i),
                            kind = kind,
                            category = item.optString("category", "").trim().lowercase(),
                            pendingFromOthersCents = item.optLong("pendingFromOthersCents", 0L).coerceIn(0L, amountCents.coerceAtLeast(0L)),
                            mealId = item.optString("mealId", "").ifBlank { null },
                        )
                    )
                    summaryParts.add("$desc · $${amountCents / 100}")
                    inserted++
                }
                if (rows.isNotEmpty()) personal.insertFinances(rows)
            }

            root.optJSONArray("meals")?.let { arr ->
                val rows = mutableListOf<MealRecord>()
                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i) ?: continue
                    val foodName = item.optString("description", item.optString("food", "")).trim()
                    val portion = item.optString("portion", "").trim()
                    val description = if (portion.isNotEmpty() && !foodName.contains(portion, ignoreCase = true)) {
                        "$foodName ($portion)"
                    } else {
                        foodName
                    }
                    if (description.isEmpty()) continue
                    val slotId = item.optString("slotId", item.optString("meal_time", "")).trim().lowercase().ifBlank { null }
                    rows.add(
                        MealRecord(
                            id = item.optString("id").ifBlank { UUID.randomUUID().toString() },
                            description = description,
                            createdAt = item.optLong("createdAt", now + i),
                            recipeId = null,
                            servings = item.optDouble("servings", 1.0).toFloat(),
                            kcal = item.optDouble("kcal", 0.0).toFloat(),
                            proteinG = item.optDouble("proteinG", 0.0).toFloat(),
                            carbsG = item.optDouble("carbsG", 0.0).toFloat(),
                            fatG = item.optDouble("fatG", 0.0).toFloat(),
                            fiberG = item.optDouble("fiberG", 0.0).toFloat(),
                            slotId = slotId,
                        )
                    )
                    summaryParts.add(description)
                    inserted++
                }
                if (rows.isNotEmpty()) personal.insertMeals(rows)
            }

            root.optJSONArray("workouts")?.let { arr ->
                var anyWorkout = false
                val workoutNotes = mutableListOf<NoteRecord>()
                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i) ?: continue
                    val exerciseName = item.optString("exercise", item.optString("exerciseId", "")).trim()
                    if (exerciseName.isEmpty()) continue
                    val sets = item.optInt("sets", 1).coerceIn(1, 20)
                    val reps = item.optInt("reps", 0).coerceAtLeast(0)
                    val weightKg = item.optDouble("weightKg", item.optDouble("weight_kg", 0.0)).toFloat()
                    val exerciseId = exerciseName.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_').ifBlank { "workout" }
                    for (setIdx in 0 until sets) {
                        if (exerciseDao.countSet(today, exerciseId, setIdx) == 0) {
                            exerciseDao.insert(
                                SetLog(
                                    date = today,
                                    exerciseId = exerciseId,
                                    setIndex = setIdx,
                                    weightKg = weightKg,
                                    reps = reps,
                                    createdAt = now + i * 100 + setIdx,
                                )
                            )
                        }
                    }
                    val notes = item.optString("notes", "").trim()
                    if (notes.isNotEmpty()) {
                        workoutNotes.add(
                            NoteRecord(
                                id = UUID.randomUUID().toString(),
                                category = NoteCategory.EXERCISE,
                                text = "$exerciseName: $notes",
                                createdAt = now + i,
                            )
                        )
                    }
                    summaryParts.add("$exerciseName ${sets}×$reps")
                    anyWorkout = true
                    inserted++
                }
                if (anyWorkout) {
                    personal.insertChecks(listOf(DayCheck(today, DayCheck.WORKOUT)))
                }
                if (workoutNotes.isNotEmpty()) {
                    personal.insertNotes(workoutNotes)
                }
            }

            root.optJSONArray("notes")?.let { arr ->
                val rows = mutableListOf<NoteRecord>()
                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i) ?: continue
                    val text = item.optString("text", "").trim()
                    if (text.isEmpty()) continue
                    val rawCat = item.optString("category", "GENERAL").trim().uppercase()
                    val category = runCatching { NoteCategory.valueOf(rawCat) }.getOrDefault(NoteCategory.GENERAL)
                    rows.add(
                        NoteRecord(
                            id = item.optString("id").ifBlank { UUID.randomUUID().toString() },
                            category = category,
                            text = text,
                            createdAt = item.optLong("createdAt", now + i),
                        )
                    )
                    summaryParts.add(text)
                    inserted++
                }
                if (rows.isNotEmpty()) personal.insertNotes(rows)
            }
        }

        scheduledReminders.forEach(reminderScheduler::schedule)

        val feedText = when {
            directMessage.isNotEmpty() && summaryParts.isNotEmpty() -> "$directMessage\n${summaryParts.joinToString(" · ")}"
            directMessage.isNotEmpty() -> directMessage
            summaryParts.isNotEmpty() -> summaryParts.joinToString(" · ")
            else -> "Recibido"
        }
        appendMessage(BridgeMessage(from = "PC", text = feedText))
        _status.value = _status.value.copy(totalIngested = _status.value.totalIngested + inserted)
        return inserted
    }

    private fun parseHourMinute(item: JSONObject): Pair<Int, Int> {
        if (item.has("hour")) {
            return item.optInt("hour", 9) to item.optInt("minute", 0)
        }
        val iso = item.optString("dueDateIso", item.optString("due_date", "")).trim()
        if (iso.isNotEmpty()) {
            runCatching {
                val dt = LocalDateTime.parse(iso.removeSuffix("Z"))
                return dt.hour to dt.minute
            }
        }
        return 9 to 0
    }

    private fun normalizePhaseId(raw: String): String = when (raw.lowercase()) {
        "morning", "manana", "mañana", "inicio" -> RoutineDefaults.INICIO
        "commute", "traslado" -> RoutineDefaults.TRASLADO
        "work", "trabajo" -> RoutineDefaults.TRABAJO
        "leave", "salida" -> RoutineDefaults.SALIDA
        "night", "noche", "home", "casa" -> RoutineDefaults.CASA
        "" -> currentPhaseIdProvider()
        else -> raw
    }

    private fun sendResponse(socket: Socket, statusCode: Int, jsonBody: String) {
        val statusText = when (statusCode) {
            200 -> "OK"
            404 -> "Not Found"
            else -> "Bad Request"
        }
        val bodyBytes = jsonBody.toByteArray(StandardCharsets.UTF_8)
        val writer = OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)
        writer.write("HTTP/1.1 $statusCode $statusText\r\n")
        writer.write("Content-Type: application/json; charset=utf-8\r\n")
        writer.write("Access-Control-Allow-Origin: *\r\n")
        writer.write("Content-Length: ${bodyBytes.size}\r\n")
        writer.write("Connection: close\r\n\r\n")
        writer.write(jsonBody)
        writer.flush()
    }

    companion object {
        private const val TAG = "NookBridge"
        const val DEFAULT_PORT = 8765
        private const val MAX_BODY_BYTES = 1_048_576

        fun detectLocalIpv4Addresses(): List<String> = runCatching {
            val result = mutableListOf<String>()
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return emptyList()
            for (nif in interfaces) {
                if (!nif.isUp || nif.isLoopback) continue
                for (addr in nif.inetAddresses) {
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        val ip = addr.hostAddress ?: continue
                        if (!ip.startsWith("169.254.")) result.add(ip)
                    }
                }
            }
            result.distinct()
        }.getOrDefault(emptyList())
    }
}
