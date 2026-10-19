# Roadmap

Project: Minimal Launcher  
Aspect: Personal  
Default mode: New Project  
Execution mode: New Android project  
Status: Visual polish validated on S24+; next: time-based spaces
Last updated: 2026-10-19

## Current Phase

Current phase: Phase 4 - Espacios por horario

Current objective:

- Mostrar el espacio correspondiente (Mañana, Trabajo, Noche) según la hora.

## Phase List

| Phase | Goal | Deliverables | Exit Criteria | Status |
| --- | --- | --- | --- | --- |
| Phase 0 - Clarify | Fijar problema y principios | IDEA.md | Objetivo, usuario y límites definidos | Complete |
| Phase 1 - Define MVP | Definir comportamiento verificable | REQUIREMENTS.md, DECISIONS.md | Criterios de aceptación definidos | Complete |
| Phase 2 - Build Shell | Crear launcher Compose mínimo | Proyecto Android, Manifest, UI base | Compila y puede seleccionarse como Home | Complete |
| Phase 3 - Visual Polish and Device Validation | Estética blanco y negro y uso real | Tema B/N, reloj y alarma, apartados desplegables, pruebas S24+ | Diseño usable y estable en el dispositivo principal | Complete |
| Phase 4 - Espacios por horario | Mostrar contenido según la hora | Modelo de espacios, franjas, disponibilidad de apps por espacio | Home muestra el espacio correcto y cambia de franja solo | Current |
| Phase 5 - Fricción | Hacer incómodo abrir apps restringidas | Sección Restringidas, pantalla de pausa, motivo de apertura | Abrir una app restringida requiere pausa y motivo | Planned |
| Phase 6 - Notas y recordatorios | Capturar información del día | Notas etiquetadas, recordatorios programados, persistencia local | Notas y recordatorios sobreviven a un reinicio | Planned |
| Phase 7 - Personalize | Configurar sin editar código | Edición de franjas, apps por espacio, apartados | El usuario adapta el launcher desde ajustes | Future |
| Phase 8 - Módulos propios | Finanzas y dieta nativas | Registro de gastos y comidas, resúmenes diarios | Cada módulo sustituye a las notas correspondientes | Future |

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

Deliverables:

- Modelo `Space` (nombre, franja horaria, apps disponibles, apartados).
- Espacios iniciales: Mañana (06-12), Trabajo (12-18), Noche (18-24).
- Selección del espacio según la hora y actualización al cambiar de franja.
- Sustituir la zona Entretenimiento por disponibilidad de apps por espacio.

Exit criteria:

- Home muestra el espacio correcto según la hora.
- Las apps no disponibles no aparecen en la lista principal.
- El cambio de franja ocurre sin reiniciar el launcher.

## Phase 5 - Fricción

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

Crear el modelo `Space` con los espacios iniciales y seleccionar el activo según la hora.
