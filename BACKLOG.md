# Backlog

Lo que falta por hacer y cómo lo trabajamos. `ROADMAP.md` dice qué fases ya se hicieron; este archivo es para lo que sigue.

Last updated: 2026-10-21

## Cómo trabajamos una mejora

Cada vez que llegue algo por mejorar (un detalle visual, algo que estorba, una idea), pasa por estos pasos:

1. **Registrar.** Se anota en la vista que corresponda (sección "Pulido por vista") o en "Ideas", con la petición en palabras del usuario.
2. **Aclarar.** Si hay dudas (qué se ve mal, cómo debería verse) se resuelven antes de tocar código. La referencia visual es `docs/design/`.
3. **Planear.** Se escribe en una línea qué se va a cambiar y en qué archivos. Si cambia el diseño, primero se ajusta el concepto.
4. **Ejecutar.** Un cambio a la vez, con la app compilando al final.
5. **Validar.** Se instala en el Samsung Galaxy S24+ y se revisa con captura. Lo que solo el usuario puede probar (huella, una sesión real) queda marcado como "por validar".
6. **Cerrar.** Se marca hecho aquí, se actualiza la captura en `docs/design/app/` y, si fue una decisión de producto, se anota en `DECISIONS.md`.

Estados: `Pendiente` · `En curso` · `Por validar` · `Hecho`.

## Pulido por vista

Después de la primera vista general se pulirá **una vista a la vez**: se juntan todos los detalles de esa vista, se corrigen en bloque y se valida antes de pasar a la siguiente.

Vista en curso: **Alarmas y recordatorios (lenguaje visual)**

| Vista | Archivo principal | Estado | Notas |
| --- | --- | --- | --- |
| Inicio y páginas | `ui/HomeGrid.kt`, `ui/LauncherScreen.kt` | Hecho | Validado en el teléfono |
| Página de Apps (antes cajón) | `ui/AppsPage.kt` | Hecho | Página principal |
| Semana (izquierda del inicio) | `ui/WeekSummaryScreen.kt` | Pendiente | |
| Ejercicio · hoy / semana / ajustes | `feature/exercise/ExerciseScreen.kt` | Pendiente | |
| Bancos | `feature/vault/VaultModule.kt` | Por validar | Desbloqueo con huella sin probar |
| Finanzas | `feature/finance/FinanceScreen.kt` | Pendiente | |
| Dieta | `ui/ModulePages.kt` (`DietPage`) | Pendiente | |
| Notas | `feature/notes/NotesScreen.kt` | Pendiente | |
| Alarmas y recordatorios | `ui/ModulePages.kt` (`AlarmsPage`) | Pendiente | Los recordatorios usan el formulario anterior |
| Reloj mundial | `ui/ModulePages.kt` (`WorldClockPage`) | Pendiente | |
| Módulos y respaldo | `ui/ModulesScreen.kt` | Pendiente | |
| Apps y espacios (configuración) | `ui/SettingsScreen.kt` | Pendiente | Aún con el estilo anterior (campos y botones de Material) |
| Pausa de apps restringidas | `ui/FrictionScreen.kt` | Pendiente | Aún con el estilo anterior |
| Rutinas (nueva) | `ui/RoutinesScreen.kt` | Por validar | Boceto: `docs/design/concept/RutinasVisual.html` |

Detalles por vista: se agregan debajo de esta línea, con un encabezado por vista, cuando se empiece a trabajar en ella.

### Rutinas (2026-10-28)

Boceto aprobado (2026-10-28). Programado en `ui/RoutinesScreen.kt` con íconos de trazo (`ui/components/StrokeIcon.kt`); instalado en el S24+, por validar ([~]):

- [~] Arriba, las fases como íconos de trazo: hechas en blanco, la actual blanca con su detalle rojo y las que siguen en gris. Debajo va la hora (la siguiente en rojo) o un alfiler si empieza por lugar.
- [~] El día como una línea: gris = hay rutina, blanco = ya pasó, marca roja = ahora.
- [~] Una tarjeta con la fase tocada, sin texto: su ícono y su hora; las acciones como íconos (con hora si la tienen). Llegar a casa es una cadena en orden con un punto rojo sobre la que sigue, y el descanso es un anillo que se acorta.
- [~] Engrane arriba para los ajustes de Rutinas (lugares, apps por fase, contacto de WhatsApp).
- [~] Pedido (2026-10-28): "no tienen que ser fijos ni los nombres ni las acciones". El lápiz muestra los nombres y permite cambiar el nombre, elegir si la fase empieza por hora o por lugar (llegar o salir), quitar acciones (✕) y agregarlas (+). También permite agregar fases (+ al final) y quitarlas (papelera). La lista de acciones deja lugar para las nuevas.
- [~] Iniciar el día se define una vez y se queda así hasta que se edite; en la vista solo se ven las acciones encendidas.
- [x] Sin contadores "6/6": quitados también de Dieta, Semana y Ejercicio (regla en `docs/design/PLAN_VISUAL.md`).
- [x] Sin texto en la vista y con íconos de trazo continuo (reglas en `docs/design/PLAN_VISUAL.md`).

#### Widget de Rutinas (2026-10-28)

Boceto aprobado (2026-10-28). Programado en `ui/components/RoutineWidget.kt` (widget "Rutinas" en el catálogo: 2×2, 4×2, 4×1); compila, falta instalar (el teléfono no estaba conectado). Por validar ([~]):

- [~] Muestra primero lo más urgente: pago sin registrar (tarjeta y monto en rojo), comida cerca o entrenar cerca (ícono con un anillo que se cierra en los 30 min previos y la hora en rojo) y, si no hay nada de eso, lo que sigue en la fase.
- [~] 2×2: solo lo más urgente. 4×2: a la derecha, los recordatorios de la fase actual por hora (acciones con hora y recordatorios ligados a la fase, con campana), hasta 4; el siguiente con su hora en rojo. En una fase en orden, la cadena. 4×1: las fases del día y lo más urgente a la derecha.
- [~] Pedido (2026-10-28): "recordatorios que se muestran dependiendo en qué rutina o fase estemos". Cada recordatorio pertenece a una fase y solo aparece y suena mientras esa fase está activa (backend pedido a la sesión de backend).
- [~] Al tocarlo: pago → registro; comida → Dieta; entreno → Ejercicio; paso → se marca; el resto abre Rutinas.
- [~] Sin texto ni contadores; el ícono nuevo se dibuja una vez cuando cambia lo que muestra.

### Inicio y páginas

Petición (2026-10-21): "Apps debe ser la vista principal y los widgets son como la otra vista. Quiero que primero se vean las apps." Además, organizar pantallas como en Samsung (One UI) al mantener presionado el inicio.

Base existente: modo EDITAR del inicio con pilas de widgets, ancho de 1 o 2 columnas, mover y agregar a pila (`WidgetStackConfig`, `ui/components/WidgetStackTile.kt`, sin commit al registrar esto).

Plan por partes (cada una se valida en el teléfono antes de la siguiente):

1. **Páginas.** El inicio pasa a ser una fila de páginas: la de Apps (la lista de apps que hoy está en el cajón) es la principal y se ve primero; los widgets viven en otras páginas. Se guarda cuál es la principal, y el botón o gesto de inicio siempre regresa a ella.
2. **Modo edición (mantener presionado).** Vista reducida de todas las páginas, con:
   - un "+" en los extremos para agregar una página a la izquierda o a la derecha;
   - una papelera en cada página para borrarla (se borran sus widgets, nunca las apps);
   - una casa en cada página para elegir la principal;
   - una barra inferior con **Widgets** y **Ajustes**.
3. **Cuadrícula.** Cada página de widgets es una cuadrícula de 4 columnas. Los widgets tienen tamaños (por ejemplo 2×2, 4×1, 4×2), se mueven arrastrando y se redimensionan con asas que solo permiten los tamaños válidos de cada widget. Al soltar un widget sobre otro del mismo tamaño se forma una pila, que se cambia deslizando.
4. **Catálogo de widgets.** Buscador "Buscar widgets", lista alfabética por módulo con cuántos diseños tiene cada uno y acordeón con vista previa real y su tamaño (2×2, 4×1…). Para colocar: botón **Añadir** (primer hueco libre de la página actual) o mantener presionado y arrastrar a la página y posición exactas. Los widgets que lo necesiten piden su ajuste al colocarse (por ejemplo, Mundo pide las ciudades y Notas qué nota mostrar).
5. **Widgets de otras apps (opcional).** Spotify, WhatsApp, Reloj, etc., con el sistema de widgets de Android.

Decisiones (2026-10-21):

- **Página de Apps:** la lista de texto (apartados, buscador, índice alfabético, respetando horarios) con el diseño del concepto `docs/design/concept/Apps.dc.html`: buscador arriba, pestañas, índice en "Todas" y aviso de cuándo vuelven ocio y juegos.
- **Páginas que se agregan con +:** solo widgets. Las apps viven únicamente en la página de Apps.
- **Catálogo:** solo widgets del launcher (Ejercicio, Finanzas, Dieta, Notas, Mundo, Alarma, Bancos). Los de otras apps quedan fuera por ahora.
- **Semana:** sigue siendo una página especial que existe aparte de las agregadas, pero se puede borrar y restablecer.

Avance (2026-10-21):

- [x] 1. Páginas: Apps es la principal y se ve primero (`ui/AppsPage.kt`, diseño del concepto); el botón de inicio regresa a ella (`launchMode="singleTask"`). El cajón se retiró. Las páginas y el acomodo de widgets se guardan por página. **Por validar.**
- [x] 2. Modo edición al mantener presionado (`ui/HomePagesHost.kt`): páginas reducidas, "+" en ambos extremos, casa y papelera por página (Apps no se puede borrar), barra con WIDGETS, AJUSTES, "+ SEMANA" cuando Semana fue borrada y LISTO. **Por validar:** borrar y restablecer Semana no se probó en el teléfono.
- [x] 4. Catálogo: buscador, lista alfabética con número de diseños, acordeón con vista previa real y tamaño, botón AÑADIR (en la página actual o en la pila elegida con "+ PILA"). Mantener presionada una vista previa la levanta: se arrastra a la celda exacta (si está ocupada, cae en la libre más cercana; sobre un widget del mismo tamaño hace pila), cambia de página al dejarla en el borde y, si se suelta en Apps o Semana, vuelve el catálogo. Al colocar Reloj Mundial se piden las ciudades y al colocar Nota Fijada qué nota mostrar. Probado en el teléfono; por validar contigo.
- [x] Arreglos vistos al probar: al sostener un widget (de la página o del catálogo) cerca del borde de arriba o de abajo, la página se desplaza sola para llegar a las filas que no se ven; en una pila, la ✕ quita el widget que se está viendo y el título muestra cuál es ("FINANZAS 1/2"); tocarlo pasa al siguiente. Probado en el teléfono; por validar contigo.

Ajustes pedidos (2026-10-21), probados en el teléfono; por validar contigo:

- [x] El reloj grande de puntos deja de ser fijo y pasa a ser un widget del launcher ("Reloj", 4×2 o 2×2) que se agrega, mueve o quita como los demás.
- [x] La página de Apps también acepta widgets: arriba el reloj grande (viene puesto) y debajo la lista de apps como ya estaba. En modo edición la lista se atenúa y tocarla sale de edición. Si se quita el reloj, la hora vuelve al encabezado.
- [x] El buscador de apps solo aparece en la pestaña TODAS.
- [x] 3. Cuadrícula libre de 4 columnas: arrastrar a cualquier celda libre, asa de la esquina para cambiar de tamaño (solo tamaños válidos), soltar sobre otro widget del mismo tamaño para hacer pila. Probado en el teléfono; por validar contigo.

### Finanzas

Pedido (2026-10-21), probado en el teléfono; por validar contigo:

- [x] Quitar el encabezado "‹ FINANZAS": regresar ya se hace con el gesto de Samsung.
- [x] "GASTO HOY" y "ÚLTIMOS 7 DÍAS" en el rojo del launcher.
- [x] El punto decimal del total en rojo.
- [x] Los montos de la lista en letra normal, no en puntos.
- [x] Abajo solo el botón "+"; al tocarlo aparecen Monto y Concepto.
- [x] Arreglo visto al probar: al abrir el teclado la pantalla se movía de más y los campos quedaban arriba, encima de la lista. Ahora quedan justo sobre el teclado (`windowSoftInputMode="adjustResize"`; afecta a todos los campos de texto del launcher).

Lenguaje visual (2026-10-22), probado en el S24+; por validar contigo:

- [~] Engrane arriba que abre los ajustes de Finanzas (presupuesto diario).
- [~] Con presupuesto: fila de 20 puntos bajo el total que se prenden conforme gastas, uno tras otro al guardar. Al pasarte, los puntos, el total y "DE $X" se ponen rojos, el total rebota una vez y vibra con un "no".
- [~] El total se redibuja de izquierda a derecha al cambiar; el gasto nuevo entra deslizando.
- [~] Borrar con deshacer: el × lo oculta y abajo sale "BORRADO · … DESHACER" por 4 segundos (en lugar del +).
- [~] "N REGISTROS · SEMANA $X" pasa a ícono de cartera + "SEMANA $X".
- [~] Widget: "GASTO HOY" en rojo, total en rojo al pasarte del presupuesto, y la semana en columnas de puntos como en la página (antes eran barras).

### Inicio · lenguaje visual (2026-10-22)

Probado en el S24+ (salvo Alarma y Ejercicio, que no están en ninguna página); por validar contigo:

- [~] Encabezados de widget con ícono de puntos en lugar del nombre: campana (Alarma), globo (Mundo), candado (Bancos), alfiler (Nota fijada), pesa (Ejercicio). "GASTO HOY", "COMIDAS" y "AGUA" se quedan con su texto rojo.
- [~] Alarma: anillo de 24 puntos con las horas que faltan; en la última hora pasa a 12 puntos de 5 minutos, en rojo, y la hora también en rojo.
- [~] Mundo: sol o luna por ciudad (de día o de noche allá) y la diferencia de horas ("−15 H").
- [~] Bancos: candado arriba, huella para abrir y "12 APPS" con el número en puntos.
- [~] Apps: "OCIO Y JUEGOS VUELVEN A LAS 18:00" pasa a reloj de arena + "OCIO" + **18:00** en rojo; en la búsqueda, las apps fuera de horario llevan un candado gris chico.
- [~] Transparencia por widget (2026-10-22): en modo edición cada widget lleva un engrane que abre sus ajustes con TRANSPARENCIA de 0% (sin tarjeta) a 100% (tarjeta sólida), en pasos de 5%, vista previa y CANCELAR / GUARDAR. Vale para todos los widgets nativos; el reloj empieza en 0%. El fondo del launcher sigue negro y no hay opción de color (decidido así). Probado en el S24+; por validar contigo.
- [~] Barra del modo edición con íconos de línea (Widgets, Ajustes, + Semana, Listo), chips de edición con ícono (engrane, ✕, + Pila), sombra y vibración al levantar un widget, y animación de encaje al juntar widgets en una pila.
- [~] Reloj en pila o con tarjeta: margen interno de 16 dp para que la hora y la fecha no queden pegadas al borde izquierdo; si una pila del reloj recibe otro widget, se enciende su tarjeta.
- [~] Gesto de regresar en Inicio: bloqueado en las páginas del inicio (`systemGestureExclusion` + `BackHandler`) para que deslizar desde los bordes no dispare el regresar de Samsung (sigue activo dentro de cualquier módulo, ajustes o modo edición).
- [~] Semana: se quitó el texto "INICIO ›" de arriba a la derecha.
- [~] Edición de widgets estilo One UI: en modo edición siempre se ve la vista previa real del widget (sin cuadro gris encima); mantener presionado un widget en la pantalla normal lo enmarca en el mismo lugar con las 4 esquinas blancas para redimensionar/mover y abre su burbuja con 3 opciones (**Editar pila**, **Eliminar / Eliminar todo**, **Ajustes**); mantener presionado un espacio vacío abre la edición completa de páginas; **Editar pila** abre el carrusel con las vistas previas de la pila, botón `−` para quitar cada uno y tarjeta `+` al final para agregar otro.
- [ ] Pendiente para más adelante: miniatura (Picture-in-Picture) al salir de videollamadas de WhatsApp.


### Widgets de asistentes IA (2026-10-23)

Petición: "quiero que agreguemos los widgets para abrir aplicaciones. En este caso de Chat GPT, Claude, Gemini". Diseños del usuario (Claude en puntos rojos; ChatGPT y Gemini en vector) pasados a puntos de 17×17. Concepto aprobado: lienzo [Widgets IA](https://claude.ai/artifact/F5eBnXo3kF8hUF8CsbWkzh) (privado).

Decisiones:

- Una entrada "Asistentes IA" en el catálogo con 3 diseños: **4×1** las tres apps; **2×2** una app; **4×2** una app con hasta 4 accesos que la propia app publica (nuevo chat, voz…), como iconos de línea en una matriz de 2×2. Al colocar el 2×2 o el 4×2 se pregunta qué app. Los 2×2 se apilan entre sí.
- Solo logos, sin el nombre de la IA. Tocar abre la app al instante. La animación propia de cada figura sale cuando el widget aparece: al llegar a su página, al deslizar la pila hasta él o al volver al launcher desde otra app o del sistema: **Claude saluda** (parpadea con párpados grises, agita la mano en blanco con estela gris y un brillo blanco le cruza el cuerpo); **ChatGPT se dibuja** girando con la punta en rojo; **Gemini brilla** (la estrella crece desde el centro y chispea en sus puntas).
- Sin instalar: icono gris e "INSTALAR" (abre la tienda). Fuera de horario: icono gris (Claude dormido, Gemini encogido), la hora en que vuelve en rojo y la pausa de siempre antes de abrir.
- Engrane: transparencia como los demás; en el 4×1 además qué apps se ven y en qué orden.
- Accesos del 4×2 como iconos en un círculo ajustado, sin tarjeta. Claude: cámara (toma la foto y la manda a un chat), código, galería (elige una imagen y la manda) y voz. ChatGPT: los suyos. Gemini: solo voz y Lens, centrados (a través de la app de Google, porque Gemini no publica accesos).

Avance:

- [~] Iconos y animaciones (`feature/ai/AiAssistants.kt`).
- [~] Widget en el catálogo, elección de app al colocar, accesos de la app en el 4×2 y "APPS Y ORDEN" en el engrane del 4×1.
- [ ] Por validar contigo en el teléfono (instalado).

### Lenguaje visual (todas las vistas)

Pedido (2026-10-22): menos gris difícil de ver, más rojo para lo importante, iconos y vectores estilo Nothing en lugar de texto, que el dibujo diga el dato, y reacciones al guardar, marcar, borrar o editar.

- [x] Plan escrito en `docs/design/PLAN_VISUAL.md` y aprobado: iconos de puntos + línea, icono + número, vibración activa con opción a apagar, empezar por Dieta.
- [~] Bases. Hecho: iconos de puntos que se llenan (`ui/components/DotIcon.kt`, primeros 9: plato, pesa, manzana, luna, vaso, pasos, sin azúcar, huevo, cama); reacciones (`ui/components/Reactions.kt`: vibración corta, doble y de error; latido; rebote), interruptor de Vibración en Módulos (también va en el respaldo); gris secundario más claro (#B0B0B0). Falta: iconos de línea y medidores (anillo y barra), que se harán con las vistas que los usan.
- [~] Dieta programada según el boceto aprobado (`ui/DietPage.kt`, widget en `ui/HomeGrid.kt`): sin encabezado, conteo en puntos con "/" rojo, comidas con tus íconos (comida, snack, cena) y manzana, la hora de la siguiente en rojo; gris visible sin marcar; al marcar una sola animación (cena: vapor en ondas; vasos: agua que se mueve; lo demás se prende de abajo hacia arriba); hábitos con ícono; "qué comiste" tras el botón +. Compila; **falta probar en el teléfono** (no estaba conectado).
- [x] Regla para todo lo que sigue: nada parpadea en bucle; gris visible, una animación al tocar y quieto (memoria del proyecto y `docs/design/PLAN_VISUAL.md`).
- [~] Dieta personalizable: comidas que se agregan, quitan y editan desde el engrane (nombre, hora, FUERTE o SNACK, ícono); las fuertes se ven más grandes y los snacks más chicos, en filas de 4. Agua: cuántos al día, VASO o TERMO y cuánto le cabe (250–1000 ml); abajo de los vasos se ve la medida y lo tomado del total. Snack nuevo: barra de chocolate que se muerde al marcar. Probado en el S24+; por validar contigo. Detalle visto: en ajustes los nombres largos se cortan en el campo.
- [~] Widget "Agua" aparte (los vasos ya no van en el widget de Dieta). Tocarlo abre Dieta.
- [~] Engrane arriba en Dieta y Finanzas que abre los ajustes de ese módulo (atrás regresa al módulo). **Regla: toda vista lleva su engrane.**
- [ ] Vistas, una por una: Dieta, Finanzas, Inicio, Alarmas, Reloj mundial, Notas, Ejercicio, Bancos, Semana, Módulos/ajustes/pausa.

## Por validar en el teléfono

- [ ] Bancos: desbloquear con huella y revisar que las 12 apps sean las correctas (se cambió la librería `fragment`, que también muestra el diálogo de huella).
- [ ] Ejercicio: una sesión completa a las 20:35 y que la semana la marque hecha.
- [ ] Respaldo: exportar a Drive y guardarlo fuera del teléfono.

## Siguiente (recomendado)

### Rutinas y registros conectados (plan 2026-10-28)

Pedido: notas del cuaderno y `app-plan.md`. Diagrama: `docs/diagrams/launcher-rutinas.html`. Plan con decisiones: `docs/diagrams/plan-rutinas.html`.

Decidido: vista web que lee los mismos registros; casa y oficina por ubicación; WhatsApp abre el chat con el texto listo; sin mensaje bíblico por ahora.

Decidido (2026-10-28, plan v2): empezar por Rutinas; modo forzoso en todo el teléfono sin permiso de superposición (abre el registro y no deja salir); ingredientes de USDA con porciones editables e ingredientes propios; al pasar la meta de kcal, Ejercicio elige del stack del mismo grupo muscular (el contenido del stack lo arma el usuario); la vista web lee el respaldo JSON, sin servidor; la lectura de avisos del banco se revisa después.

Reparto: el backend (datos, lógica, servicios) lo hace una sesión aparte; las vistas y los widgets se diseñan con el usuario.

| Paso | Qué | Estado |
| --- | --- | --- |
| 1 | Base de datos versión 3 y respaldo con las tablas nuevas | Por validar |
| 2 | Rutinas: 5 fases por hora, apps, recordatorios por punto, audio | Por validar |
| 3 | Finanzas: tipos de ingreso, ahorro %, bolsa de extras, cuentas compartidas | Por validar |
| 4 | Comida: recetas, base de ingredientes, metas; ajuste en Ejercicio | Por validar |
| 5 | Pago detectado y modo forzoso | Por validar |
| 6 | Ubicación: geocercas, pendientes al salir, WhatsApp | Por validar |
| 7 | Notas por aspecto y lógica de widgets | Por validar |
| 2b | Recordatorios por fase: cada recordatorio pertenece a una fase y solo suena en ella; marca de hecho por día; lista de la fase para el widget Rutinas | En curso · compila, falta instalar |
| 8 | Vista web | En curso · el respaldo (formato 2) ya trae todo; falta la página |

Avance (2026-10-28): solo lógica y datos; las vistas las diseña la otra sesión. Compila y pasan las pruebas de lógica (`app/src/test`). Instalado en el S24+ el 2026-10-28 (la base migró a la versión 3 sin cierres). En la versión de prueba hay dos mosaicos de ajustes rápidos: **Rutina** (panel de prueba) y **Pago de prueba**. Qué probar en cada paso al instalar:

1. Base v3: abrir el launcher sin perder notas, gastos, comidas ni series; exportar el respaldo y ver `"format": 2` con las listas nuevas (recetas, ingredientes, presupuestos, pagos pendientes, stack, rutina).
2. Rutinas: fases y acciones editables (nombre, orden, disparador por hora o lugar; agregar, quitar y ordenar acciones de un catálogo que crece sin cambiar la base). Las 5 del plan son solo el inicio. Cada fase es "elegir" (las acciones se prenden o apagan una vez y así se quedan hasta editarlas, como Iniciar el día) o "en orden" (una tras otra, como llegar a casa). Cualquier fase empieza por hora o al llegar o salir de un lugar; los lugares son una lista editable (casa, oficina, gimnasio…). Las apps se abren a las mismas horas que con los 3 espacios. En el panel Rutina se agregan, borran y mueven fases y acciones, y el avance se ve en puntos.
3. Finanzas: en el panel Rutina, "Hoy puedes gastar": si un día gastas de más, baja lo de los días que quedan de la semana.
4. Comida: base de 67 ingredientes de USDA cargada; con meta de kcal y una comida que la pase, el panel sugiere un ejercicio del stack de ejemplo.
5. Modo forzoso: encender "Registro obligatorio" en Accesibilidad, tocar Pago de prueba, intentar abrir otra app (regresa) y terminar ¿comida? → gasto → comida.
6. Ubicación: dar ubicación "todo el tiempo", fijar casa y oficina, y probar "Simular salida" (pide lo pendiente y luego abre WhatsApp con el texto).
2b. Recordatorios por fase (base versión 4): los recordatorios que ya tenías pasan a la fase que salió de su espacio (Mañana → inicio, Trabajo → trabajo, Noche → casa). Un recordatorio de otra fase no suena; al marcarlo hecho tampoco suena ese día.
7. Notas y widgets: datos listos en el ViewModel (`pendingNotes`, `widgetPicks`); se ven cuando existan los widgets.

### Respaldo automático

- Copia semanal sin acordarse: el usuario elige una vez la carpeta (por ejemplo, en Drive) y el launcher guarda ahí el JSON cada semana.
- Activar el respaldo de Google de Android (`allowBackup` y reglas de extracción) para que los datos vuelvan al instalar en un teléfono nuevo.

## Más adelante

### Fase 16 · App propia y nube

- Sincronizar los datos para verlos en otro dispositivo o en la web; el formato del respaldo (`"format": 1`) es la base.
- Separar `feature/*` en módulos de Gradle cuando exista la segunda app.

### Alarmas propias

- Lista de alarmas con interruptores como en el concepto. Implica un despertador completo (sonido, posponer, sonar con el teléfono bloqueado). Solo si se quiere dejar el Reloj de Samsung.

### Ideas

- Categorías en Finanzas (comida, transporte, café…) y gasto por categoría en la semana.
- Rachas en Ejercicio (semanas seguidas cumplidas).
- Aviso del próximo recordatorio en el inicio.
- Espacios distintos para fin de semana (viene de `ROADMAP.md`).
- Estadísticas locales de aperturas evitadas (viene de `ROADMAP.md`).
