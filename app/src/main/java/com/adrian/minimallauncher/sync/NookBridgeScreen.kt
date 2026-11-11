package com.adrian.minimallauncher.sync

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.adrian.minimallauncher.feature.ModuleCard
import com.adrian.minimallauncher.feature.ModuleHeader
import com.adrian.minimallauncher.feature.SmallLabel
import com.adrian.minimallauncher.feature.StatusDot
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFmt = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun NookBridgeScreen(
    status: BridgeStatus,
    messages: List<BridgeMessage>,
    onRefreshIps: () -> Unit,
    onRunSelfTest: () -> Unit,
    onSendMessageToPc: (String) -> Unit,
    onSetPcTargetUrl: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val connected = status.isConnected

    Column(
        modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 24.dp),
    ) {
        ModuleHeader("PC", onBack, Modifier.padding(top = 8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatusDot(filled = connected, next = false, size = 8.dp)
                SmallLabel(
                    if (connected) "CONECTADO" else "ESPERANDO",
                    color = if (connected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (messages.isEmpty()) {
            Box(
                Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                SmallLabel("SIN MENSAJES RECIBIDOS")
            }
        } else {
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 12.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                messages.forEach { item ->
                    ModuleCard {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SmallLabel(item.from, color = Color.White)
                            SmallLabel(formatTime(item.timestamp))
                        }
                        Text(
                            item.text,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

private fun formatTime(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(timeFmt)
