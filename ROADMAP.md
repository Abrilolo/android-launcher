# Roadmap

Project: Minimal Launcher  
Aspect: Personal  
Default mode: New Project  
Execution mode: New Android project  
Status: Phases 4-8 implemented; S24+ validation pending
Last updated: 2026-10-20

## Current Phase

Current phase: Device validation for Phases 5-8

Current objective:

- Validar en Samsung Galaxy S24+ la fricción, persistencia, recordatorios y configuración.

## Phase List

| Phase | Goal | Deliverables | Exit Criteria | Status |
| --- | --- | --- | --- | --- |
| Phase 0 - Clarify | Fijar problema y principios | IDEA.md | Objetivo, usuario y límites definidos | Complete |
| Phase 1 - Define MVP | Definir comportamiento verificable | REQUIREMENTS.md, DECISIONS.md | Criterios de aceptación definidos | Complete |
| Phase 2 - Build Shell | Crear launcher Compose mínimo | Proyecto Android, Manifest, UI base | Compila y puede seleccionarse como Home | Complete |
| Phase 3 - Visual Polish and Device Validation | Estética blanco y negro y uso real | Tema B/N, reloj y alarma, apartados desplegables, pruebas S24+ | Diseño usable y estable en el dispositivo principal | Complete |
| Phase 4 - Espacios por horario | Mostrar contenido según la hora | Modelo de espacios, franjas, disponibilidad de apps por espacio | Home muestra el espacio correcto y cambia de franja solo | Complete |
| Phase 5 - Fricción | Hacer incómodo abrir apps restringidas | Sección Restringidas, pantalla de pausa, motivo de apertura | Abrir una app restringida requiere pausa y motivo | Implemented; validate |
| Phase 6 - Notas y recordatorios | Capturar información del día | Notas etiquetadas, recordatorios programados, persistencia local | Notas y recordatorios sobreviven a un reinicio | Implemented; validate |
| Phase 7 - Personalize | Configurar sin editar código | Edición de franjas, apps por espacio, apartados | El usuario adapta el launcher desde ajustes | Implemented; validate |
| Phase 8 - Módulos propios | Finanzas y dieta nativas | Registro de gastos y comidas, resúmenes diarios | Cada módulo sustituye a las notas correspondientes | Implemented; validate |

## Phase 2 - Build Shell

Completion notes:

- Validado manualmente en emulador y Samsung Galaxy S24+.
- Swipes horizontales funcionan.
- Las aplicaciones se abren correctamente.
- Se corrigió la barra de acción automática que ocultaba/cortaba el título.
- Se añadió manejo del área segura del sistema para evitar solapamientos con status/navigation bars.

## Phase 3 - Visual Polish and Device Validation

Completion notes:

- Validado en Samsung Galaxy S24+ (2026-10-19): tema B/N, reloj y alarma, apartados, lista de texto y widgets.
- Se quitaron los indicadores de página (puntos) bajo el widget.
- APK actualizada e instalada en el Samsung Galaxy S24+ (2026-10-20).

Priority order:

1. Tema blanco y negro: fondo negro, texto blanco, gris para secundario. Sin iconos.
2. Reloj grande y próxima alarma en la parte superior.
3. Apartados desplegables (▸ / ▾) en lugar de tarjetas o botones.
4. Lista de apps en texto grande, estilo lista vertical.
5. Espaciado, tamaños y áreas táctiles cómodos con una mano en el S24+.
6. Probar reinicio del launcher, retorno desde una app y rotación.
7. Registrar fricciones antes de implementar nuevas funciones.

Exit criteria:

- El contenido no se corta ni se solapa con las barras del sistema.
- No hay iconos ni colores fuera de blanco, negro y grises.
- Los controles son cómodos de tocar con una mano.
- La pantalla principal se entiende sin explicación.

## Phase 4 - Espacios por horario

Implementation notes:

- Añadido modelo `Space` con selección automática por hora y grupos de apps disponibles.
- Mañana (06-12) muestra apps esenciales; Trabajo (12-18), esenciales y otras; Noche (18-06), todas.
- El espacio activo aparece sobre el reloj y se actualiza automáticamente cada minuto.
- La pantalla separada de Entretenimiento se sustituyó por el espacio horario activo.
- Validado por Adrián en Samsung Galaxy S24+ (2026-10-20).

Deliverables:

- Modelo `Space` (nombre, franja horaria, apps disponibles, apartados).
- Espacios iniciales: Mañana (06-12), Trabajo (12-18), Noche (18-06, cruza medianoche).
- Selección del espacio según la hora y actualización al cambiar de franja.
- Sustituir la zona Entretenimiento por disponibilidad de apps por espacio.

Exit criteria:

- Home muestra el espacio correcto según la hora.
- Las apps no disponibles no aparecen en la lista principal.
- El cambio de franja ocurre sin reiniciar el launcher.

## Phase 5 - Fricción

Implementation notes:

- Apps restringidas aparecen atenuadas en una sección plegable.
- Al seleccionar una app se exige una pausa de 30 segundos y escribir un motivo antes de abrirla.
- La duración inicial es fija; se hará configurable en la Fase 7.
- La duración de la pausa y los apartados son configurables desde Configurar.

Deliverables:

- Sección "Restringidas" en texto atenuado.
- Pantalla de pausa con cuenta atrás y hora en que la app vuelve a estar disponible.
- Campo para escribir el motivo antes de abrir.

Exit criteria:

- No es posible abrir una app restringida desde el launcher sin la pausa y el motivo.
- Siempre existe una vía para abrirla (no es bloqueo absoluto).

Future option:

- `AccessibilityService` para interceptar aperturas desde notificaciones o recientes. Evaluar solo si la fricción dentro del launcher resulta insuficiente.

## Phase 6 - Notas y recordatorios

Implementation notes:

- Notas por etiqueta, gastos y comidas persisten localmente en DataStore.
- Los recordatorios diarios se programan por hora y espacio y se restauran después de reiniciar el dispositivo.
- Android puede retrasar alarmas inexactas por ahorro de batería; validar puntualidad en el S24+.

## Phase 7 - Personalize

Implementation notes:

- La pantalla Configurar permite editar los horarios y grupos de apps de cada espacio.
- Los horarios se guardan solo si cubren el día completo sin huecos ni solapamientos.
- Permite ajustar la duración de pausa y la visibilidad de los apartados.

## Phase 8 - Módulos propios

Implementation notes:

- Finanzas guarda gastos con monto y concepto y muestra el total del día.
- Dieta guarda comidas y muestra el conteo diario.
- Los registros persisten localmente y se pueden borrar desde sus apartados.

Deliverables:

- Notas locales con etiqueta (Finanzas, Dieta, General).
- Recordatorios por espacio (p. ej. "Desayunar" en Mañana).
- Persistencia local (DataStore o Room).
- Notificaciones de recordatorio.

Exit criteria:

- Las notas y recordatorios persisten tras reiniciar el teléfono.
- Los recordatorios llegan a tiempo en el S24+.

## Validation Questions

- ¿Los espacios por horario reducen aperturas impulsivas?
- ¿La fricción disuade sin generar ganas de desinstalar el launcher?
- ¿La lista de texto permite encontrar apps con rapidez?
- ¿Qué información se repite en las notas de finanzas y dieta?

## Future Ideas

- Módulo propio de finanzas: registro rápido de gastos y resumen diario o semanal.
- Módulo propio de dieta: registro de comidas y recordatorios de horarios.
- Agenda del día en un apartado (calendario de Android o propia).
- Espacios distintos para fin de semana.
- Ocultar apps poco frecuentes sin eliminarlas.
- Estadísticas locales de aperturas evitadas.

## Next Action

Instalar la APK en el S24+ y validar apps restringidas, notas tras reinicio, entrega de recordatorios y personalización de horarios.
