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
| Ejercicio · hoy / semana / cuerpo / ajustes | `feature/exercise/ExerciseScreen.kt` | Por validar | Boceto: `docs/design/concept/EjercicioVisual.html` |
| Bancos | `feature/vault/VaultModule.kt` | Por validar | Desbloqueo con huella sin probar |
| Finanzas | `feature/finance/FinanceScreen.kt` | Por validar | Boceto: `docs/design/concept/FinanzasVisual.html` (modelo nuevo) |
| Comida (antes Dieta) | `ui/DietPage.kt`, `feature/food/` | Por validar | Boceto: `docs/design/concept/ComidaVisual.html` |
| Notas | `feature/notes/NotesScreen.kt` | Pendiente | |
| Alarmas y recordatorios | `ui/ModulePages.kt` (`AlarmsPage`) | Pendiente | Los recordatorios usan el formulario anterior |
| Reloj mundial | `ui/ModulePages.kt` (`WorldClockPage`) | Pendiente | |
| Módulos y respaldo | `ui/ModulesScreen.kt` | Pendiente | |
| Apps y espacios (configuración) | `ui/SettingsScreen.kt` | Pendiente | Aún con el estilo anterior (campos y botones de Material) |
| Pausa de apps restringidas | `ui/FrictionScreen.kt` | Pendiente | Aún con el estilo anterior |
| Rutinas (nueva) | `ui/RoutinesScreen.kt` | Hecho | Boceto: `docs/design/concept/RutinasVisual.html` |

Detalles por vista: se agregan debajo de esta línea, con un encabezado por vista, cuando se empiece a trabajar en ella.

### Rutinas (2026-10-28)

Boceto aprobado (2026-10-28). Programado en `ui/RoutinesScreen.kt` con íconos de trazo (`ui/components/StrokeIcon.kt`); instalado en el S24+, validado por el usuario (2026-10-28):

- [x] Arriba, las fases como íconos de trazo: hechas en blanco, la actual blanca con su detalle rojo y las que siguen en gris. Debajo va la hora (la siguiente en rojo) o un alfiler si empieza por lugar.
- [x] El día como una línea: gris = hay rutina, blanco = ya pasó, marca roja = ahora.
- [x] Una tarjeta con la fase tocada, sin texto: su ícono y su hora; las acciones como íconos (con hora si la tienen). Llegar a casa es una cadena en orden con un punto rojo sobre la que sigue, y el descanso es un anillo que se acorta.
- [x] Engrane arriba para los ajustes de Rutinas (lugares, apps por fase, contacto de WhatsApp).
- [x] Pedido (2026-10-28): "no tienen que ser fijos ni los nombres ni las acciones". El lápiz muestra los nombres y permite cambiar el nombre, elegir si la fase empieza por hora o por lugar (llegar o salir), quitar acciones (✕) y agregarlas (+). También permite agregar fases (+ al final) y quitarlas (papelera). La lista de acciones deja lugar para las nuevas.
- [x] Iniciar el día se define una vez y se queda así hasta que se edite; en la vista solo se ven las acciones encendidas.
- [x] Sin contadores "6/6": quitados también de Dieta, Semana y Ejercicio (regla en `docs/design/PLAN_VISUAL.md`).
- [x] Sin texto en la vista y con íconos de trazo continuo (reglas en `docs/design/PLAN_VISUAL.md`).

#### Widget de Rutinas (2026-10-28)

Boceto aprobado (2026-10-28). Programado en `ui/components/RoutineWidget.kt` (widget "Rutinas" en el catálogo: 2×2, 4×2, 4×1); instalado en el S24+. validado por el usuario (2026-10-28):

- [x] Muestra primero lo más urgente: pago sin registrar (tarjeta y monto en rojo), comida cerca o entrenar cerca (ícono con un anillo que se cierra en los 30 min previos y la hora en rojo) y, si no hay nada de eso, lo que sigue en la fase.
- [x] 2×2: solo lo más urgente. 4×2: a la derecha, los recordatorios de la fase actual por hora (acciones con hora y recordatorios ligados a la fase, con campana), hasta 4; el siguiente con su hora en rojo. En una fase en orden, la cadena. 4×1: las fases del día y lo más urgente a la derecha.
- [x] Pedido (2026-10-28): "recordatorios que se muestran dependiendo en qué rutina o fase estemos". Cada recordatorio pertenece a una fase y solo aparece y suena mientras esa fase está activa (backend pedido a la sesión de backend).
- [x] Al tocarlo: pago → registro; comida → Dieta; entreno → Ejercicio; paso → se marca; el resto abre Rutinas.
- [x] Sin texto ni contadores; el ícono nuevo se dibuja una vez cuando cambia lo que muestra.

### Ajustes de Rutinas

Boceto aprobado (aprobación general, 2026-10-29): `docs/design/concept/RutinasAjustesVisual.html`. Programado en `ui/RoutineSettingsScreen.kt` (lo abre el engrane de Rutinas); el panel de prueba `ui/RoutinePlaceholderScreen.kt` se borró.

Funcionalidad: falta mucho trabajo (aprobación general del boceto, 2026-10-29).

- [ ] Lugares: pin blanco si tiene ubicación y gris si no; tocar el nombre para renombrar, "Fijar aquí" guarda la ubicación actual, ✕ quita y "Nuevo lugar" agrega. Aviso rojo con PERMITIR si falta la ubicación "todo el tiempo". **Por validar.**
- [ ] Fases en orden con ↑↓. Al tocar una: modo (elegir / en orden), apps permitidas (esenciales, otras, entretenimiento, con cuántas abren y cuántas se bloquean) y sus acciones. **Por validar.**
- [ ] Cada acción con solo lo que usa: hora (reloj del sistema, ✕ la quita), minutos del descanso (− +), app de música o podcast (lista de apps) y teléfono y mensaje de WhatsApp (vacío = el general). **Por validar.**
- [ ] Pruebas, cerrado y abajo: simular llegar/salir por lugar, el WhatsApp general (teléfono y mensaje por defecto), pago de prueba, probar WhatsApp y abrir accesibilidad. **Por validar.**
- [ ] Falta: la lista de apps de audio muestra todas las apps (no filtra las de audio); el WhatsApp general quedó en Pruebas y quizá deba ir en su propio lugar; el permiso no se vuelve a revisar al regresar de Ajustes del sistema; no se ve en mapa dónde quedó fijado un lugar ni su radio; la papelera de lugar no pide confirmar; el resumen de registros del panel viejo ya no existe.

### Ejercicio (stack y kcal)

Boceto aprobado (aprobación general, 2026-10-29): `docs/design/concept/EjercicioVisual.html`. Programado en `feature/exercise/ExerciseScreen.kt`, `ExerciseBodyTab.kt` y `ExerciseSettings.kt`, con 15 íconos de trazo nuevos en `ui/components/StrokeIcon.kt`.

Funcionalidad: falta mucho trabajo (aprobación general del boceto, 2026-10-29).

- [ ] Arriba, tres íconos en lugar de pestañas: hoy (pesa), semana (calendario) y cuerpo (báscula). El engrane abre los ajustes. **Por validar.**
- [ ] Hoy: los grupos musculares del día, la hora de inicio (en rojo si no has empezado) y los minutos. Cada ejercicio muestra el ícono de su grupo, kg × reps y un punto por serie; el siguiente punto tiene borde rojo. El ícono abre los pasos de kg y reps, y la "i" muestra el nombre y cómo se hace. **Por validar.**
- [ ] Descanso como anillo que se vacía (rojo en los últimos 10 s, vibra al terminar), con 60, 90 o 120. Palomita de sesión hecha. **Por validar.**
- [ ] Exceso de kcal: si la comida del día pasa la meta, una tarjeta con la llama y el excedente propone un ejercicio del stack del mismo grupo de hoy, con minutos y kcal; ⟳ cambia de opción. **Por validar.**
- [ ] Semana: los 7 días como íconos (hecho en blanco, hoy con detalle rojo), la llama con los días entrenados y el plan del día tocado en números. **Por validar.**
- [ ] Cuerpo: peso con línea de inicio a meta y tres puntos de mañanas, IMC en una regla (rango sano, tú y tu meta), cintura (meta: la mitad de la altura), pecho, brazo y muslo con flecha de tendencia. Tocar abre la captura. **Por validar.**
- [ ] Ajustes: el stack por grupo (nombre, intensidad suave/media/fuerte, minutos, ✕ y +) y "Borrar ejemplos" mientras haya ejemplos; altura y peso meta (los mismos de Comida); hora de inicio y descanso. **Por validar.**
- [ ] Falta: guardar el ejercicio extra como hecho (hoy se olvida al cerrar la app) y restar sus kcal; elegir qué medidas seguir; teclado de números propio en lugar del del sistema; racha real entre semanas; widget de Ejercicio con íconos de trazo; el calentamiento tampoco se guarda.

### Registro obligatorio

Boceto aprobado (aprobación general, 2026-10-29): `docs/design/concept/RegistroVisual.html`. Programado en `ui/ForcedEntryScreen.kt`, con 5 íconos de trazo nuevos (bolsa, transferencia, regalo, servicio, borrar). Se prueba con el mosaico "Pago de prueba".

Funcionalidad: falta mucho trabajo (aprobación general del boceto, 2026-10-29).

- [ ] Arriba, un punto por pago pendiente (el actual en rojo) y el candado; atrás no hace nada y no hay engrane. **Por validar.**
- [ ] Toque 1: el monto grande y, en la misma pantalla, comida (presupuesto de comida o bolsa de extras, con lo que queda hoy) o no comida (transferencia, regalo, servicio). Si el pago pasa lo que queda de comida, la bolsa sale en rojo. No comida se guarda con ese toque. **Por validar.**
- [ ] Tocar el monto abre un teclado numérico propio para corregirlo; ✓ confirma. **Por validar.**
- [ ] Toque 2 (comida): recetas en círculos con foto (inicial si no tiene), las más usadas primero; la hoja abre ingredientes (buscar, tocar, los gramos suben de 50 en 50); ✓ la marca como comida sin detalle. **Por validar.**
- [ ] Salida: las comidas que ya pasaron con su hora (la siguiente en rojo); tocarlas las marca y ✓ termina. **Por validar.**
- [ ] Falta: la lectura real de notificaciones del banco (hoy solo el mosaico); transferencia, regalo y servicio se guardan como gasto extra con su categoría (la app los cuenta como ingreso cuando son entradas); la bolsa usa la categoría "antojo"; receta siempre 1 porción; los gramos solo de 50 a 300; sin cuenta compartida ni "paga otro"; sin deshacer después de guardar.

### Comida (recetas y metas)

Boceto aprobado (aprobación general, 2026-10-29): `docs/design/concept/ComidaVisual.html`. Programado en `ui/DietPage.kt` y `feature/food/` (`FoodUi.kt`, `FoodLogSheet.kt`, `RecipeScreen.kt`, `FoodWeek.kt`), con íconos de trazo nuevos en `ui/components/StrokeIcon.kt` (snack, cena, termo, huevo, vitamina, sinazucar, hoy, tazon, camara).

Funcionalidad: falta mucho trabajo (aprobación general del boceto, 2026-10-29).

- [ ] Arriba, dos íconos (hoy y semana) en lugar de pestañas; a la derecha, el libro abre las recetas y el engrane los ajustes. **Por validar.**
- [ ] Hoy: anillo con las kcal que quedan y la meta en gris; se llena en blanco y, si te pasas, lo que sobra sale en rojo junto con el número (+). Debajo van proteína (huevo) y fibra (hoja) con su barra. **Por validar.**
- [ ] Comidas del día: cada fila lleva la hora (la siguiente en rojo), el ícono de trazo, la foto de la receta registrada y sus kcal. Tocarla abre el registro de esa comida; mantenerla presionada la marca o desmarca sin registrar. **Por validar.**
- [ ] Agua y hábitos con íconos de trazo: los vasos se llenan uno tras otro y solo se ven los ml tomados, sin contador. **Por validar.**
- [ ] +: registrar una receta (foto con sus kcal) o armar la comida con ingredientes. Se busca en la base USDA o se agrega uno propio; la porción usual se cambia con − / +, los gramos se editan, el lápiz corrige el ingrediente y el total se suma en vivo. **Por validar.**
- [ ] Receta: foto (cámara para elegirla), minutos, kcal, proteína, fibra, vitaminas, ingredientes con gramos y pasos. El lápiz la edita, la papelera la borra y ✓ registra una porción. **Por validar.**
- [ ] Semana: peso con la meta y lo que falta en rojo, línea de los últimos registros y + para anotar el peso; IMC en una regla de rangos; kcal por día contra la meta (en rojo lo que se pasa) y puntos de proteína y fibra cumplidas. **Por validar.**
- [ ] Falta: la receta planeada de cada comida (pedida al backend: `MealSlot.recipeId`); mientras tanto, cada comida registrada se asigna a la comida más cercana en hora. También faltan las vitaminas del día (no hay meta ni se guardan con la comida), las porciones distintas de 1 al registrar una receta y el registro rápido por texto ("¿Qué comiste?"). El widget de Comida sigue con íconos de puntos, y las metas de kcal y proteína se editan solo en ajustes.

### Notas y recordatorios

Boceto aprobado (aprobación general, 2026-10-29): `docs/design/concept/NotasRecordatoriosVisual.html`. Programado en `feature/notes/NotesScreen.kt` y `AlarmsPage` en `ui/ModulePages.kt`, con 2 íconos de trazo nuevos (circulo, nota_hecha).

Funcionalidad: falta mucho trabajo (aprobación general del boceto, 2026-10-29).

- [ ] Notas: los 5 aspectos (Trabajo, Escuela, Ejercicio, Comida, Dieta) con un punto por nota pendiente sobre cada ícono; el aspecto de la fase actual en rojo; sin pendientes, gris. Las notas de General, Ideas y Finanzas van en un sexto ícono solo si hay alguna. **Por validar.**
- [ ] Notas: al abrir, los puntos salen uno a uno; al marcar una nota, el círculo dibuja la palomita, la nota pasa a gris y su punto se encoge. Campo "Nueva nota" guarda en el aspecto elegido (Enter o al salir). **Por validar.**
- [ ] Notas · lápiz: nombres bajo los íconos, fijar arriba (solo una) o borrar cada nota. **Por validar.**
- [ ] Recordatorios: siguiente alarma del reloj arriba (tocar abre el reloj); fases con una marca por recordatorio (blanca hecho, roja el siguiente, gris el resto); en la tarjeta, campana con su hora. Solo se marcan durante su fase. **Por validar.**
- [ ] Recordatorios · lápiz: título, hora (selector del sistema), mover a otra fase tocando su ícono, borrar, + agrega uno a la fase elegida. **Por validar.**
- [ ] Falta: el engrane de Notas abre Módulos sin ajustes propios (el aspecto por fase está fijo en `WidgetSelector.focusOf`: Trabajo→Trabajo, Llegar a casa→Ejercicio, Iniciar el día→Dieta); las notas hechas se quedan en gris sin limpiarse; no se puede mover una nota a otro aspecto ni editar su texto; un recordatorio nuevo nace sin título a la siguiente hora en punto; los recordatorios de las acciones de la fase siguen en Rutinas, no aquí; la página Notas ya no muestra la fecha de cada nota.

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

### Finanzas (modelo nuevo)

Boceto `docs/design/concept/FinanzasVisual.html`; código en `feature/finance/` (`FinanceScreen.kt`, `EntrySheet.kt`, `FinanceSettings.kt`). Reemplaza la vista de arriba.

Funcionalidad: falta mucho trabajo (aprobación general del boceto, 2026-10-29).

- [ ] Arriba, en grande, lo que queda para gastar hoy; 20 puntos (blancos lo gastado, rojos al pasarte). **Por validar.**
- [ ] Lo que queda del mes (bolsa), de la semana, y el tamaño de la bolsa, con íconos mes / semana / bolsa. **Por validar.**
- [ ] Semana en columnas de puntos: cada columna es lo que el día podía gastar; gastar de más baja las columnas de los días que faltan. La letra de mañana en rojo. **Por validar.**
- [ ] Barra del ingreso fijo: fijos (gris), ahorro (rayado) y bolsa (blanco lo gastado); transferencias, regalos y servicios aparte. **Por validar.**
- [ ] Presupuestos por tema (comida al día, etc.) con sus puntos y lo que queda hoy. **Por validar.**
- [ ] Por cobrar: total en rojo; tocar una cuenta la marca como cobrada. **Por validar.**
- [ ] + abre el registro: extra, compartido (con "te deben"), fijo o ingreso; el ingreso pregunta si es para tu estabilidad (sí → presupuesto, no → transferencia, regalo o servicio). Aviso rojo si el gasto no cabe en lo de hoy. **Por validar.**
- [ ] Engrane: ingreso y fijos del mes, % de ahorro, límite diario (vacío = sale de la bolsa), presupuestos por tema y movimientos del mes con borrar. **Por validar.**
- [ ] Pendiente: los gastos fijos e ingresos fijos se registran cada mes a mano (no hay recurrentes); cobro parcial de una cuenta compartida; editar un movimiento; categorías propias más allá de comida / transporte / otro; conectar con Comida (antojo fuera del presupuesto); el widget aún usa el modelo anterior.

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
| 2b | Recordatorios por fase: cada recordatorio pertenece a una fase y solo suena en ella; marca de hecho por día; lista de la fase para el widget Rutinas | Por validar |
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
