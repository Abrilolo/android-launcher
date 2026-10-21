# Roadmap

Project: Minimal Launcher  
Aspect: Personal  
Default mode: New Project  
Execution mode: New Android project  
Status: Phases 9-15 implemented; S24+ validation pending
Last updated: 2026-10-21

## Current Phase

Current phase: Phase 14 - Finanzas y Notas con el diseño del concepto

Current objective:

- Llevar Finanzas y Notas al diseño de `docs/design/` (gráfica de puntos, nota fijada, etiquetas), igual que se hizo con Ejercicio.
- En paralelo, validar en el S24+ los módulos de Ejercicio y Bancos y el nuevo inicio.

Design reference: `docs/design/README.md` (concepto en imagen y HTML, capturas de la app y estado de cada pantalla).

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
| Phase 9 - Concepto Nothing | Diseño premium y minimalista | Lienzo de diseño, `docs/design/` | Cada aspecto tiene su pantalla de referencia | Complete |
| Phase 10 - Datos en Room | Base de datos real para todos los registros | `LauncherDatabase`, migración desde DataStore | Los registros sobreviven y se pueden consultar por fecha | Complete |
| Phase 11 - Módulo Ejercicio | Plan de la guía de Tokio dentro del launcher | Widget, página Hoy/Semana/Ajustes, registro de series | Registrar series con peso y repeticiones y ver la semana | Implemented; validate |
| Phase 12 - Módulo Bancos | Apps de banco protegidas | Widget, huella o PIN, bloqueo automático | Sin huella no se ven ni se abren las apps de banco | Implemented; validate |
| Phase 13 - Inicio en cuadrícula | Ver el estado sin abrir nada | Reloj de puntos, 7 tarjetas, ícono de apps, cajón | El inicio responde las preguntas del día de un vistazo | Implemented; validate |
| Phase 14 - Finanzas y Notas | Páginas propias como en el concepto | Gráfica semanal en puntos, nota fijada, etiquetas | Las dos páginas se ven como `docs/design/` | Implemented; validate |
| Phase 15 - Módulos y Alarmas | Editar cada aspecto sin código | Pantalla Módulos, ajustes por módulo, agua en Dieta | Cada módulo se configura desde el engranaje | Implemented; validate |
| Phase 16 - App propia | Juntar los aspectos en un mismo ambiente | Módulos de Gradle reutilizables, respaldo en la nube opcional | Los datos se pueden ver fuera del launcher | Future |

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

## Phase 9 - Concepto Nothing

Completion notes:

- Concepto en el lienzo de diseño "Launcher Nothing concepto": inicio, cajón de apps, Bancos, Finanzas, Dieta, Notas, Alarmas, Reloj mundial, Ejercicio (hoy y semana) y Módulos.
- Copia local en `docs/design/`: imágenes PNG, HTML que se abre en el navegador y capturas de la app real.
- Ejercicio se diseñó a partir de la guía "Físico de atleta · Tokio · octubre 2026".

## Phase 10 - Datos en Room

Completion notes:

- Notas, recordatorios, gastos, comidas, medidas corporales y hábitos viven en `launcher.db` (Room 2.8.5, KSP).
- Al primer arranque se copian los registros que había en DataStore; la copia antigua se conserva como respaldo.
- DataStore queda solo para preferencias (espacios, apartados, pausa, ajustes de módulos).

## Phase 11 - Módulo Ejercicio

Implementation notes:

- Plan A/B/C, cardio, intervalos y descansos de la guía, con técnica y "Cuida" por ejercicio.
- Cada serie se guarda con peso y repeticiones; la siguiente vez propone la última carga.
- La sesión se marca hecha al completar todas las series; descanso de 60/90/120 s con vibración.
- Semana con estado por día, peso promedio de 3 mañanas, cintura semanal y regla de progresión.

Pending validation:

- Usar una sesión completa en el S24+ y revisar que la semana refleje los días hechos.

## Phase 12 - Módulo Bancos

Implementation notes:

- Bancos pasó de apartado a widget; el widget no muestra nombres ni saldos.
- Huella o PIN del teléfono para abrir; se vuelve a bloquear por tiempo (30/45/60/120 s), al abrir una app o al salir del launcher.
- Las apps de banco no aparecen en el buscador.

Pending validation:

- Desbloquear con huella en el S24+ y confirmar que las 12 apps detectadas son las correctas.

## Phase 13 - Inicio en cuadrícula

Implementation notes:

- Reloj en matriz de puntos y tarjetas: Alarma, Ejercicio, Gasto hoy, Comidas, Mundo, Bancos y Nota reciente.
- Cada tarjeta abre su página; las de Finanzas y Notas usan por ahora los formularios anteriores.
- Ícono de apps (3×3 puntos) abajo al centro; el cajón conserva los apartados, el buscador y el índice.
- Recordatorios se movieron a la página de Alarmas. Pantalla fija en vertical.
- Se retiraron el carrusel de widgets y la sección "Restringidas" (lo restringido solo se encuentra buscando).

## Phase 14 - Finanzas y Notas

Deliverables:

- Finanzas: total del día, gráfica de 7 días en puntos, lista de gastos y captura rápida.
- Notas: nota fijada, filtro por etiqueta y captura que se guarda sola.

Exit criteria:

- Las dos páginas coinciden con `docs/design/concept/png/Finanzas.png` y `Notas.png`.

Implementation notes:

- Finanzas (`feature/finance`): total del día en puntos, 7 días en columnas de 8 puntos (el día con más gasto llena la columna), lista de hoy con borrar y captura de monto + concepto.
- Notas (`feature/notes`): una nota fijada en tarjeta blanca, filtro por etiqueta (General, Trabajo, Ideas, Finanzas, Dieta), tocar una nota muestra FIJAR / BORRAR y la captura se guarda con la tecla de listo o al salir.
- Room versión 2: columna `pinned` en `notes` con migración; las notas existentes quedan sin fijar.
- La tarjeta de nota del inicio muestra la fijada o, si no hay, la más reciente.

## Phase 15 - Módulos y Alarmas

Deliverables:

- Pantalla Módulos: activar o quitar cada widget del inicio y editar sus ajustes (ver `Modulos.png`).
- Ajustes por módulo: horarios de comida, meta de agua, presupuesto diario, ciudades, tiempo de bloqueo de Bancos.
- Agua en Dieta.

Implementation notes:

- El engranaje abre Módulos (`ui/ModulesScreen.kt`): interruptor por tarjeta del inicio y editor por módulo; "Apps y espacios" lleva a la configuración anterior (horarios, pausa, apartados).
- Las tarjetas apagadas no dejan hueco: las demás se recorren en pares.
- Ajustes guardados en DataStore (`ModuleSettings`): hora y descanso de Ejercicio, presupuesto diario, horarios de comida, meta de agua, ciudades del reloj mundial y bloqueo de Bancos.
- Finanzas: con presupuesto, la tarjeta muestra "DE $X" y el gasto de hoy se pone en rojo al pasarlo.
- Dieta: tarjeta de agua con un vaso por barra, guardado como hábito del día (`<fecha>|water_<n>`).
- Alarmas sigue mostrando solo la siguiente alarma del Reloj: Android no permite a otras apps leer ni encender sus alarmas.

## Phase 16 - App propia

Notes:

- Separar los módulos en módulos de Gradle para reutilizarlos en una app aparte.
- Respaldo en la nube opcional para ver los datos en otro dispositivo.

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

Validar en el S24+ el desbloqueo de Bancos con huella y una sesión completa de Ejercicio. Pendiente menor: la página de la izquierda (Insights) sigue con los horarios de comida fijos, no con los de Módulos.
