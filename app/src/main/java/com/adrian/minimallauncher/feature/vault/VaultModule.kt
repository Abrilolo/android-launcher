package com.adrian.minimallauncher.feature.vault

import com.adrian.minimallauncher.ui.components.DotIcons
import com.adrian.minimallauncher.ui.components.DotIcon
import com.adrian.minimallauncher.ui.components.DotActivation
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.adrian.minimallauncher.data.LauncherApp
import com.adrian.minimallauncher.feature.CardFill
import com.adrian.minimallauncher.feature.CardLine
import com.adrian.minimallauncher.feature.LauncherRed
import com.adrian.minimallauncher.feature.ModuleHeader
import com.adrian.minimallauncher.feature.SmallLabel
import com.adrian.minimallauncher.ui.BiometricHelper
import com.adrian.minimallauncher.ui.components.DotMatrixText
import com.adrian.minimallauncher.ui.components.FingerprintIcon
import kotlinx.coroutines.delay

private val lockChoices = listOf(30, 45, 60, 120)

/** What kind of money app each known package is; shown as the small label on its card. */
private val kinds = mapOf(
    "com.nu.production" to "BANCO", "com.bancomer.mbanking" to "BANCO", "com.revolut.revolut" to "BANCO",
    "com.storicard.stori" to "BANCO", "card.plata.app" to "BANCO",
    "com.mercadopago.wallet" to "PAGOS", "com.google.android.apps.walletnfcrel" to "TARJETAS",
    "com.dolarapp" to "DIVISAS", "com.gbm.app" to "INVERSIÓN", "com.gbm.homebroker" to "INVERSIÓN",
    "com.bitso.exchange" to "CRIPTO", "com.bitso.wallet" to "CRIPTO", "com.binance.dev" to "CRIPTO",
)

/**
 * Asks for fingerprint or PIN before [onUnlocked]. With no screen lock set up there is nothing to
 * check against, so the vault stays closed instead of opening unprotected.
 */
fun unlockVault(activity: FragmentActivity, onUnlocked: () -> Unit) {
    if (!BiometricHelper.canAuthenticate(activity)) {
        Toast.makeText(activity, "Activa huella o PIN en los ajustes del teléfono para usar Bancos", Toast.LENGTH_LONG).show()
        return
    }
    BiometricHelper.prompt(activity, title = "Bancos", subtitle = "Confirma con tu huella o PIN", onSuccess = onUnlocked)
}

/**
 * Home widget: never shows which apps or balances are inside, or how many; only the padlock, big and centered.
 * Each change of [opening] plays the padlock opening (the caller opens Bancos once it has played).
 */
@Composable
fun VaultWidget(opening: Int = 0) {
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        DotIcon(
            DotIcons.candadoGrande, done = true, contentDescription = "Bancos bloqueado",
            size = minOf(96.dp, minOf(maxWidth, maxHeight) * 0.75f),
            activation = DotActivation.Unlock,
            replay = opening,
        )
    }
}

/** The unlocked list. Locks itself after [lockSeconds], when an app is opened or when the launcher leaves the screen. */
@Composable
fun VaultScreen(
    apps: List<LauncherApp>,
    lockSeconds: Int,
    onOpenApp: (String) -> Unit,
    onLockSecondsChange: (Int) -> Unit,
    onLock: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var left by remember(lockSeconds) { mutableIntStateOf(lockSeconds) }
    LaunchedEffect(left) {
        if (left <= 0) onLock() else {
            delay(1_000L)
            left--
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { onLock() }

    Column(modifier.fillMaxSize().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        ModuleHeader("BANCOS", onLock, Modifier.padding(top = 8.dp)) {
            SmallLabel("ABIERTO", color = MaterialTheme.colorScheme.onSurface)
        }
        Row(
            Modifier.fillMaxWidth().padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            SmallLabel("SE BLOQUEA EN")
            DotMatrixText("%d:%02d".format(left / 60, left % 60), cell = 3.5.dp, color = LauncherRed)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(CardLine))
        LazyVerticalGrid(GridCells.Fixed(2), Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(apps, key = { it.packageName }) { app ->
                Column(
                    Modifier
                        .height(80.dp)
                        .background(CardFill, RoundedCornerShape(22.dp))
                        .border(1.dp, CardLine, RoundedCornerShape(22.dp))
                        .clickable(role = Role.Button) { onOpenApp(app.packageName); onLock() }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    SmallLabel(kinds[app.packageName] ?: "FINANZAS")
                    Text(app.label, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SmallLabel("VOLVER A BLOQUEAR A LOS")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                lockChoices.forEach { seconds ->
                    val selected = seconds == lockSeconds
                    Text(
                        "$seconds S",
                        Modifier
                            .background(if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent, RoundedCornerShape(18.dp))
                            .border(1.dp, if (selected) MaterialTheme.colorScheme.onSurface else Color(0xFF333333), RoundedCornerShape(18.dp))
                            .clickable(role = Role.RadioButton) { onLockSecondsChange(seconds) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) Color.Black else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
        Text(
            "BLOQUEAR AHORA",
            Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .border(1.dp, Color(0xFF333333), RoundedCornerShape(26.dp))
                .clickable(role = Role.Button, onClick = onLock)
                .padding(vertical = 16.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
    }
}
