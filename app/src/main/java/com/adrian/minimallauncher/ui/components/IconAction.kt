package com.adrian.minimallauncher.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Acción sin palabras: un ícono de trazo en un botón de 44 dp. El texto va solo en la
 * descripción de accesibilidad, para que el lector de pantalla siga diciendo qué hace.
 */
@Composable
fun IconAction(name: String, description: String, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        StrokeIcon(name, if (enabled) StrokeState.DONE else StrokeState.OFF, 22.dp)
    }
}
