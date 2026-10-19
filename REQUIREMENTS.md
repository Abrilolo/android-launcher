# Requirements

Project: Minimal Launcher  
Aspect: Personal  
Default mode: New Project  
Status: MVP shell built; contextual scope defined  
Last updated: 2026-10-19

## Project Objective

- Crear un launcher Android personal que muestre, según la hora del día, lo relevante (agenda, recordatorios, notas y apps permitidas) y que haga incómodo abrir apps distractoras fuera de su horario.

Target state:

- Al presionar Home aparece el espacio de la franja actual (Mañana, Trabajo o Noche).
- Cada espacio muestra reloj, alarma, apartados desplegables y las apps disponibles.
- Las apps restringidas se pueden abrir solo tras superar capas de fricción.
- La interfaz es solo texto, blanco y negro, sin iconos.

## Scope By Stage

### Stage A - MVP shell (built)

- Jetpack Compose.
- Activity con categorías HOME y DEFAULT.
- Permiso QUERY_ALL_PACKAGES.
- Consulta de apps con PackageManager.
- ViewModel con clasificación de apps por package name.
- Apertura de apps mediante Intent.

### Stage B - Visual polish (current)

- Estética solo texto, fondo negro, texto blanco, gris para secundario.
- Reloj y alarma en la parte superior.
- Apartados desplegables.
- Área segura y edge-to-edge en el Galaxy S24+.

### Stage C - Espacios por horario

- Definición de espacios y franjas en código.
- Selección automática del espacio según la hora.
- Disponibilidad de apps por espacio (sustituye la zona Entretenimiento).

### Stage D - Fricción para apps restringidas

- Ocultar apps restringidas de la lista principal.
- Sección "Restringidas" con texto atenuado.
- Pantalla de pausa con cuenta atrás y hora de disponibilidad.
- Confirmación escribiendo un motivo.

### Stage E - Notas y recordatorios

- Notas locales etiquetadas (Finanzas, Dieta, General).
- Recordatorios programados por espacio (p. ej. "Desayunar").
- Persistencia local.

## Excluded Scope

- Backend, cuentas o sincronización.
- Analítica remota.
- Bloqueo absoluto.
- Clasificación automática basada en uso.
- Iconos o personalización visual avanzada.
- Módulos completos de finanzas y dieta (futuro).

## User Stories Or Use Cases

| ID | User Or Actor | Need | Outcome | Priority | Stage |
| --- | --- | --- | --- | --- | --- |
| UC-001 | Adrián | Presionar Home | Ver el espacio de la hora actual | Must | C |
| UC-002 | Adrián | Ver la hora y la próxima alarma | Reloj y alarma visibles arriba | Must | B |
| UC-003 | Adrián | Abrir una app esencial | Encontrarla en una lista de texto y abrirla | Must | A |
| UC-004 | Adrián | Evitar entretenimiento por la mañana | Las apps restringidas no aparecen en la lista | Must | C |
| UC-005 | Adrián | Abrir una app restringida si es necesario | Superar una pausa y escribir un motivo | Must | D |
| UC-006 | Adrián | Recordar desayunar | Recibir un recordatorio en el espacio Mañana | Should | E |
| UC-007 | Adrián | Apuntar un gasto o una comida | Crear una nota etiquetada | Should | E |
| UC-008 | Adrián | Ver agenda del día | Apartado desplegable con eventos | Could | Future |

## Functional Requirements

| ID | Requirement | Acceptance Signal | Stage | Status |
| --- | --- | --- | --- | --- |
| FR-001 | La app debe declarar la categoría Android HOME. | Puede seleccionarse como launcher. | A | Done |
| FR-002 | Debe consultar las apps lanzables instaladas. | La lista muestra apps disponibles. | A | Done |
| FR-003 | Debe excluirse de su propia lista. | Minimal Launcher no aparece. | A | Done |
| FR-004 | El estado debe clasificar las apps por package name. | Cada app tiene una categoría. | A | Done |
| FR-005 | Un tap debe abrir la app mediante Intent. | La app destino se inicia. | A | Done |
| FR-006 | La pantalla debe mostrar reloj y próxima alarma. | Hora y alarma visibles arriba. | B | Planned |
| FR-007 | Los apartados deben poder desplegarse y plegarse. | Tocar ▸ muestra el contenido. | B | Planned |
| FR-008 | El espacio activo debe elegirse según la hora. | A las 07:00 se muestra Mañana; a las 13:00, Trabajo. | C | Planned |
| FR-009 | Cada espacio debe definir sus apps disponibles. | Una app restringida no aparece en la lista principal. | C | Planned |
| FR-010 | El espacio debe actualizarse al cambiar de franja. | Al pasar las 12:00 cambia sin reiniciar. | C | Planned |
| FR-011 | Las apps restringidas deben aparecer en una sección atenuada. | Texto gris en "Restringidas". | D | Planned |
| FR-012 | Abrir una app restringida debe mostrar una pausa con cuenta atrás. | No se puede continuar antes de que acabe. | D | Planned |
| FR-013 | Tras la pausa debe escribirse un motivo para continuar. | Sin texto, el botón de abrir está desactivado. | D | Planned |
| FR-014 | Deben poder crearse notas etiquetadas. | La nota persiste tras reiniciar. | E | Planned |
| FR-015 | Deben poder programarse recordatorios. | Llega una notificación a la hora indicada. | E | Planned |

## Non-Functional Requirements

| ID | Requirement | Acceptance Signal | Status |
| --- | --- | --- | --- |
| NFR-001 | La interfaz debe ser solo texto en blanco y negro. | Sin iconos de apps ni colores de acento. | Planned |
| NFR-002 | Debe funcionar sin red. | Todas las pantallas funcionan offline. | Planned |
| NFR-003 | Espacios, franjas y clasificación deben ser fáciles de modificar. | Están aislados en una configuración. | Planned |
| NFR-004 | No debe requerir backend ni servicios externos. | La app funciona localmente. | Planned |
| NFR-005 | Los datos personales deben quedarse en el dispositivo. | Sin permisos de red usados para datos. | Planned |
| NFR-006 | La fricción no debe ser absoluta. | Siempre existe una vía para abrir la app. | Planned |

## Acceptance Criteria

- El launcher puede seleccionarse como aplicación Home.
- Al presionar Home se muestra el espacio de la franja actual.
- La interfaz no muestra iconos y usa solo blanco, negro y grises.
- Reloj y alarma aparecen en la parte superior.
- Las apps restringidas no aparecen en la lista principal del espacio.
- Abrir una app restringida exige esperar la cuenta atrás y escribir un motivo.
- Tocar una app disponible la abre directamente.
- Las notas y recordatorios persisten tras reiniciar el teléfono.

## Dependencies

| Dependency | Purpose | Status | Risk |
| --- | --- | --- | --- |
| Android SDK | Ejecutar la app y consultar paquetes | Required | Bajo |
| Kotlin | Lenguaje principal | Required | Bajo |
| Jetpack Compose | UI completa | Required | Bajo |
| QUERY_ALL_PACKAGES | Consultar apps instaladas | In use | Restricciones de distribución en Play |
| DataStore o Room | Persistir notas, recordatorios y configuración | Planned (Stage E) | Bajo |
| AlarmManager / WorkManager | Recordatorios programados | Planned (Stage E) | Optimización de batería de Samsung |
| POST_NOTIFICATIONS | Mostrar recordatorios | Planned (Stage E) | Bajo |
| AlarmManager.getNextAlarmClock | Mostrar la próxima alarma | Planned (Stage B) | Bajo |

## Risks

- Google Play puede restringir QUERY_ALL_PACKAGES.
- La fricción solo actúa dentro del launcher; una app restringida sigue abriéndose desde notificaciones o recientes.
- One UI puede retrasar recordatorios por optimización de batería.
- Los package names pueden cambiar entre dispositivos.

## Validation Method

- Compilación del módulo Android.
- Prueba manual en el Samsung Galaxy S24+ cambiando la hora del sistema para simular franjas.
- Uso cotidiano durante varios días registrando aperturas evitadas y fricciones molestas.

## Open Questions

- Lista concreta de apps restringidas por espacio.
- Duración de la pausa.
- Comportamiento en fines de semana.
- Fuente de la agenda (calendario de Android o propia).
