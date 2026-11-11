# Backlog

Lo que falta por hacer y cómo lo trabajamos. `ROADMAP.md` dice qué fases ya se hicieron; este archivo es para lo que sigue.

Last updated: 2026-11-02

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
| Bancos | `feature/vault/VaultModule.kt` | Hecho | Funciona bien |
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
- [ ] Ver tu rutina: letra más grande con brillo rojo en Hoy, Semana y el menú del Dock. Reiniciar rutina al terminar: píldora gris sin marco que se llena de rojo oscuro al mantener. **Por validar.**
- [ ] Metas: ahora en la escena. Series de los últimos 20 días o 12 semanas, apiladas por grupo en grises; en rojo bajo la línea, los récords. Tocar una barra muestra su detalle. Abajo, el mejor registro de cada ejercicio. Peso y medidas sigue nativo y se abre desde aquí. Swipe a la izquierda durante el ejercicio: la misma gráfica, por sesión, con su lista de registros. **Por validar.**
- [ ] Barra de abajo: ahora es un Dock. Transparente, marco negro con brillo rojo muy sutil en las esquinas. Un toque abre con un rebote corto. Punto rojo bajo la vista abierta. Hoy con punto si hay sesión en curso; Semana con los días que faltan. Mantener presionado abre un menú (Hoy: rutina y hora; Semana: Hoy y Mañana; Ajustes: reiniciar, también mantenido). **Por validar.**
- [ ] Semana: ahora en la misma escena que Hoy. Siete columnas con la personita: festeja lo hecho, espera hoy, duerme lo que falta, se sienta en descanso y mira al piso si no se hizo. Tocar un día (o swipe sobre el detalle) lo abre abajo con sus ejercicios; los puntos son las series. De hoy en adelante, Ver rutina lleva a editar ese día; Guardar desde ahí no cambia la rutina de hoy. **Por validar.**
- [ ] Cuerpo: peso con línea de inicio a meta y tres puntos de mañanas, IMC en una regla (rango sano, tú y tu meta), cintura (meta: la mitad de la altura), pecho, brazo y muslo con flecha de tendencia. Tocar abre la captura. **Por validar.**
- [ ] Ajustes: el stack por grupo (nombre, intensidad suave/media/fuerte, minutos, ✕ y +) y "Borrar ejemplos" mientras haya ejemplos; altura y peso meta (los mismos de Comida); hora de inicio y descanso. **Por validar.**
- [ ] Falta: guardar el ejercicio extra como hecho (hoy se olvida al cerrar la app) y restar sus kcal; elegir qué medidas seguir; teclado de números propio en lugar del del sistema; racha real entre semanas; widget de Ejercicio con íconos de trazo; el calentamiento tampoco se guarda.

#### Ejercicio · Hoy primero y rutina sin guía (2026-11-02)

Pedido: "no me gustó mucho que el ejercicio guiado sea como la función principal... que sin estar pasando de pantalla en pantalla pueda ver mi progreso y que en cualquier momento pueda pasar a esta vista guiada". Reemplaza lo anterior que abría en el inicio a la hora de entrenar y el botón que solo salía a esa hora. Sin boceto: lo describió el usuario. Recorrido completo en el navegador con datos de prueba, sin errores. **Por validar en el S24+.**

- [ ] Hoy: siempre abre aquí, con Ver tu rutina, Iniciar tu rutina y Rutina guiada (Seguir… si hay sesión a medias). Abajo, una marca por ejercicio con sus series en puntos: blanco hecho, rojo el actual, gris lo que sigue. **Por validar.**
- [ ] Rutina sin guía: en una sola pantalla, marcas arriba, la personita y un cuadro con el ejercicio y sus series (tocar reps o kg los ajusta). El botón dice Descanso, Continuar o Terminar según lo que sigue; solo cambia el cuadro. El descanso va en el mismo cuadro, con Seguir. Tocar una marca salta a ese ejercicio. Las series por tiempo usan el mismo cronómetro. Al final, la vista de Terminaste. **Por validar.**
- [ ] Rutina guiada: "Rutina guiada" abajo del cuadro pasa a la guía en el mismo ejercicio o descanso; desde Hoy se regresa a cualquiera de las dos. Sigue con el inicio de mantener y arrastrar y con calentamiento. **Por validar.**
- [ ] Plan editado: el widget, la página Semana y la sugerencia por exceso de kcal ya leen Tu rutina, no la guía Tokio. **Por validar.**
- [ ] Falta: la rutina sin guía no tiene calentamiento (sigue solo en la guiada).

#### Cajón de apps que sigue al dedo (2026-11-04)

Pedido: "cuando no se alcanzan ver en la pantalla e intentas el scroll, se muestran todas las apps, no quiero eso... con el swipe hacia arriba el mismo efecto que tenemos en ejercicio como lo del fondo se pone como blur y con el deslizamiento de mi dedo se va subiendo o bajando". **Por validar en el S24+.**

- [ ] Desplazar la página de inicio ya no abre el cajón. Solo se abre si la página ya está hasta abajo (o no se desplaza) cuando empieza el gesto. **Por validar.**
- [ ] El cajón sube y baja con el dedo; al soltar queda abierto o cerrado según cuánto y qué tan rápido se movió. El inicio de atrás se desenfoca y oscurece poco a poco, como en Ejercicio. Se cierra jalando hacia abajo desde arriba de la lista, o con atrás. **Por validar.**

#### Barra de abajo de Ejercicio en las demás vistas (2026-11-04)

Pedido: "el estilo de la barra que tenemos para los menus en el apartado de ejercicio, hazla tambien para las demás vistas". **Por validar en el S24+.**

- [ ] Dieta y Finanzas: la misma barra de Ejercicio, una sola de lado a lado con línea arriba, ícono y nombre; la activa en rojo. Se quitó la raya antes de Ajustes. Sigue el rebote al tocar, los números rojos y los menús al mantener. **Por validar.**

#### Ejercicio · Hoy y barra de abajo acomodados (2026-11-02)

Pedido: "mira el acomodo de la foto, pero conserva ya nuestros diseños... la barra de abajo sí hazla así, pero conservando nuestros menús". **Por validar en el S24+.**

- [ ] Hoy: fecha y frase; tarjetas con el formato de la foto. La hora de entrenar con lo que falta; marco gris y fondo transparente; los cuadros del reloj y de la personita con marco negro y sin fondo. La hora va en ámbar y lo hecho en verde (el rojo queda para actuar). La tarjeta de la rutina: el nombre con Ver rutina en rojo, tiempo, ejercicios y las marcas con un play redondo rojo al lado para iniciar; Rutina guiada con la personita, toda la tarjeta entra a la guía. **Por validar.**
- [ ] Ningún texto de la app lleva brillo: la sesión de ejercicio, Dieta y Finanzas. **Por validar.**
- [ ] Barra de abajo: una sola, de lado a lado, con ícono y nombre (Hoy, Semana, Metas, Ajustes); la activa en rojo. Mantener apretado sigue abriendo sus menús. **Por validar.**

#### Ejercicio · rutina sin guía con los gestos de la guiada (2026-11-02)

Pedido: "estirar la tarjeta donde se ven las repeticiones... mejor algo como de cambiar ejercicio... que los swipes también estén en esta forma... respeta ya también ese diseño". Recorrido en el navegador con datos de prueba, sin errores. **Por validar en el S24+.**

- [ ] Tarjeta: la misma de la guiada, abajo; tiene manija y el botón de seguir es el deslizador (Descanso, Continuar, Terminar, Saltar descanso). **Por validar.**
- [ ] Estirar la tarjeta hacia arriba abre postura y tips bajo las series, que siguen a la vista; la personita sube con el borde. Reemplaza el botón de i. **Por validar.**
- [ ] Cambiar ejercicio: el botón de flechas muestra otro del mismo grupo; se pasa con swipe o con ›, y la última tarjeta (Otros) deja cambiar de grupo y de ejercicio. **Por validar.**
- [ ] Descanso: la misma tarjeta para elegir lo que sigue, con el reloj arriba. **Por validar.**
- [ ] Swipes: a la derecha, tiempo y rutina (con Reiniciar y Terminar); a la izquierda, metas del ejercicio. Sobre la personita o el nombre del ejercicio. **Por validar.**
- [ ] Series: agregar con + y borrar deslizando la fila, como en la guiada. **Por validar.**

#### Ejercicio · flujo completo (2026-10-31)

Pedido: "pule todo los detalles de flujo... que paso cuando te sales y regresas, prepara como se retoma la rutina". Íconos aún sin tocar (pendiente aparte). **Por validar en el teléfono.**

- [ ] **Salir y regresar.** Bloquear, cambiar de app o cerrar el módulo: al volver abre exacto donde estaba. Los cronómetros siguen en tiempo real; lo pausado sigue pausado. Más de 20 min fuera cuenta como pausa y no suma a tu tiempo.
- [ ] **Retomar.** Home y Atrás en la sesión van a Hoy. Hoy muestra lo que quedó con su reloj en vivo y un toque en el botón retoma (empezar sigue siendo mantener apretado). Atrás en Hoy sale del módulo.
- [ ] **Gestos.** Pausar un calentamiento sube la píldora Seguir para cortarlo. Swipe a la derecha también en el descanso abre la rutina. Terminar antes: deslizar Terminar en ese panel. Sin series en un ejercicio, se salta. El siguiente es el primero con series pendientes.
- [ ] **Reiniciar** borra el día: se mantiene apretado (0.9 s) y se llena de rojo; un toque ya no borra nada.
- [ ] **Vibraciones.** Toque, presión, confirmar, serie completa y alerta. Fin de descanso o de serie por tiempo vibra también con la pantalla apagada (puede llegar unos segundos tarde; es alarma inexacta, sin permiso extra).
- [ ] **Latencia.** Semana, Metas y Ajustes van encima de la escena viva, sin recargar. WebView precalentado. La pantalla no se apaga durante la sesión.
- [ ] **Barra inferior** a todo el ancho. Quitados avisos y textos obvios.

#### Ejercicio · sesión guiada (bocetos 2026-10-29)

Android implementation (2026-10-29): the approved motion and gesture prototype is bundled as an offline WebView scene reached from Today's exercise screen, with the planned session and previous set logs supplied by Room. Choices, custom warm-ups/exercises, editable rotating phrases, timers, completed sets, and resumable session state are persisted locally; guided records are included in backup format 5. The generated scene removes the demo sidebar, sample exercises, and remote fonts. `:app:assembleDebug` and `:app:testDebugUnitTest` pass; device interaction and visual fit remain to be checked after installation.

Bocetos del usuario en `docs/design/sketches/ejercicio/` (1 a 6, en orden). Pedido: "solo tenemos como el flujo, pero nos falta mucha funcionalidad de como se ven las cosas". Dentro de la sesión sí va texto (nombre del ejercicio o calentamiento, tips): es la app en uso, no un widget. Estado: **Boceto por aprobar**: `docs/design/concept/EjercicioSesionVisual.html` (2026-10-29, interactivo con gestos; el dashboard va marcado "por diseñar").

- [ ] **Entrada.** Si es hora de entrenar abre directo en el inicio de la rutina; si no, en la vista general. Arriba, los grupos musculares de hoy en una barra flotante y un botón home a la vista general.
- [ ] **Personita.** Personaje dibujado que se mueve todo el tiempo (excepción pedida a la regla de "nada en bucle"). Reacciona al tocarla. Historial de pedidos (2026-10-29):
  - "Más animaciones... movimientos más naturales como que camina hacia atrás y adelante", con el estilo de las referencias (torso de color como frijol, manos y pies ovalados).
  - "Se ve muy rígido... su diseño era como más dibujado": trazo que tiembla como lápiz, extremidades curvas y sueltas, color del torso desfasado del contorno, cabeza flotante, resortes en cada articulación (las manos llegan tarde, todo rebota antes de quedarse), casi siempre de frente y de lado solo para caminar, boxear y piso.
  - "Que se mueva a lo largo de la pantalla... con una función random": comportamientos al azar con pesos según el momento, sin secuencia fija y sin repetir el último; cada uno dura más y hay más variedad. Entrada: saltitos antes de pelear, sombra de boxeo larga con combinaciones al azar, calentamiento de futbolista (tobillos, cuádriceps, isquios, balanceo, zancadas, trote) y, si tardas, se impacienta (reloj, señala, hombros, brazos cruzados). Vista general: camina, saluda, se sienta, rueda y se queda acostada de lado. Fiesta: baile tipo Fortnite, funk brasileño, salsa. Espera: camina, saca una silla, se sienta, se duerme con "z" o se acuesta en el piso y se duerme. Descanso: descansa → se estira → calienta. Durante: motiva (rebota, aplaude, puño, señala, saltitos).
  - "Que desplace más y en diferentes direcciones... con el punto de fuga": perspectiva (al alejarse más chica, más arriba, hacia el centro y más tenue); entre acciones casi siempre camina a otro punto, de lado, al fondo, hacia ti o en diagonal; voltea de frente antes de cambiar de dirección.
  - "Junta dogplank y jacks": un solo calentamiento "Saltos y plancha".
  - **Prototype refinement (2026-10-29):** three-action memory, exertion-weighted recovery, shorter travel between actions, foreground staging for expressive gestures, short-arc joint interpolation, faster boxing response, grounded feet, and standing-up transitions when interrupting floor/chair poses. Added hand-held bottle/towel, wristwatch, floor mat, seated cross-legged sleep, eyes and blinking. Existing football warm-ups, dances, rolling and combined “Saltos y plancha” are retained. Sidebar buttons expose specific actions for review. This is HTML prototype work, not Android integration.
- [ ] **Iniciar.** Botón ▷: se mantiene apretado (se llena como agua) y luego se arrastra y suelta en un punto. Las dos cosas juntas, para no iniciar por error.
- [ ] **Calentamiento.** Abajo, una tarjeta con dibujo vectorial y el nombre del calentamiento, con 3 opciones para la rutina. Swipe horizontal cambia de opción; al final, si sigues jalando, aparece "otros" con la cuadrícula completa de calentamientos.
- [~] **Detalle.** Swipe hacia arriba en la tarjeta: la personita "espía" desde arriba y abajo van insights del calentamiento, tu progreso y tips. **HTML prototype implemented:** hands arrive before the head; the character looks down, glances, blinks and follows the pointer. The main figure pauses while details are open. Close button, Escape and downward swipe restore it; repeated opening replaces gesture listeners. Reduced-motion support included. Android integration and visual acceptance remain pending.
- [ ] **Tarjeta inferior.** Selección y descanso usan la misma tarjeta a todo el ancho, con todos sus controles adentro. Jalar arriba abre el detalle con el fondo difuminado y el botón siempre dice Seguir. **Por validar.**
- [ ] **Elegir.** Swipe a la derecha sobre la animación (o junto a ella) inicia ese calentamiento o ejercicio.
- [ ] **Durante el ejercicio.** Arriba, la personita chica junto al tiempo. Te motiva todo el tiempo (pedido: "durante el ejercicio animaciones como motivando") y reacciona al completar una serie (salta), al mover el peso (saca músculo) y al terminar las series (festeja).
- [ ] **Músculo.** Dibujo minimalista del grupo que se trabaja; su zona se pone más roja conforme avanzan las series. Mantener apretado muestra la info del ejercicio: calorías, tips y postura.
- [ ] **Series.** Una fila por serie con su número, reps y kg propios y un círculo para marcarla terminada; "+" agrega otra serie y swipe a la izquierda la borra (fondo rojo con papelera). El número de serie es una etiqueta de fila, no un contador N/M.
- [ ] **Swipes laterales.** Izquierda → derecha: el siguiente ejercicio (elegirlo o verlo) con el tiempo de la rutina. Derecha → izquierda: dashboard de ese ejercicio (aún sin diseñar: registros anteriores de reps, peso y tiempo, mejor marca y consulta de registros previos).
- [ ] **Cronómetro.** Calentamiento y series por tiempo en min:seg, con regresar (vuelve al tiempo completo) y pausar debajo. **Por validar.**
- [ ] **Terminar ejercicio.** Regresa el semicírculo Continuar: se jala hacia arriba, con su sombreado rojo a negro. Seguir, Saltar descanso y Terminar sin marco: fondo gris oscuro que se llena de rojo oscuro al seleccionar. **Por validar.**
- [ ] **Descanso.** Contador (5:00) con la personita descansando; abajo, la siguiente rutina y un botón para saltar. Ejercicio y saltar comparten una tarjeta abajo con asa: swipe arriba da la información y a los lados cambia el ejercicio ahí mismo, sin abrir otra vista; al final está "Otros". La información sube y baja al ritmo del dedo. Saltar se desliza, con flechas, de blanco a rojo, y entra directo al que quedó elegido. **Por validar.** Mientras menos tiempo queda, más se prepara la personita. Si llega a 0 y no avanzas, el contador cambia de color y cuenta hacia arriba lo que tardes, con la animación de "te estoy esperando". Así hasta terminar la rutina.

Prototype verification: `node --test docs/design/tests/exercise-session.test.cjs` checks mood exclusions, weighted memory, all behavior lifecycles, finite joints/SVG transforms, floor/chair interruptions, depth ordering, small stages, reduced motion, gesture rebinding and peek animation state. Browser visual review is pending: the browser URL policy rejected the local `file://` preview. These offline checks do not establish visual quality or Android behavior.

**Selection flow refinement (HTML prototype, 2026-10-29):** every rest exit now opens an exercise picker, including the first exercise after warming up. Swipe through the predefined alternatives, then use Others for the full catalog. Selecting an item from Others previews it; Continue or the explicit stage swipe commits it. Navigation from the routine list also enters the picker. Browsing/canceling does not mutate recorded sets; switching back restores sets by routine slot and exercise ID. Canceling preserves the rest countdown/overtime. Start and finish now have introductory/closing copy; the final screen freezes elapsed time and reports recorded sets instead of assuming calories for custom exercises.

Others includes a small custom-item form for warm-ups and exercises. Entries currently live only in memory until the prototype reloads; persistent catalog storage and Android integration remain pending. Animation metadata is optional: missing/unknown animations use a generic greeting/coach, and missing artwork uses a neutral pose. No technique animation, calorie estimate or previous history is invented for a new exercise. Offline regression tests cover selection/confirmation, Others, cancellation, set preservation and custom items without animations; visual acceptance remains pending.

**Final entry-screen annotations (HTML prototype, 2026-10-29):** keep “Un momento para ti.”; remove the paragraph beneath it and the visible hold/drag instruction at the bottom. Retain the accessible button label. The start control now has continuously visible liquid, touch/fill ripples, drag-dependent tilt and deformation, destination proximity feedback, and a docking ripple with a character reaction. Starting still requires holding until full and releasing at the destination. Cancellation resets the control; leaving the screen disposes animation frames and pending navigation. Reduced-motion preferences suppress decorative motion. Offline tests cover premature release, canceled gestures, scaled target alignment, single confirmation, valid reactions and navigation cleanup. Visual acceptance and Android integration remain pending.

- [ ] **Editable session phrases:** allow editing the opening and closing messages and keeping several alternatives. Choose a different phrase across sessions without immediately repeating the last one. Keep the currently approved “Un momento para ti.” as an available opening phrase. This request is recorded for implementation; no phrase editor or rotation is present yet.

**Start-control sketch revision (HTML prototype, 2026-10-29):** supersedes the circular liquid-container design above. Use an irregular red-outlined ink blot with a readable play glyph and a small destination dot, without a dashed track or visible instructions. At rest the blot gently changes its silhouette; pressing squashes it, holding extends a pulsing lobe toward the destination, and the glyph nudges right once armed. Drag velocity stretches a trailing tail. Releasing over the destination gathers the blot with a settling bounce and confirms; an early release returns it. The hold/release gate, cancellation cleanup and reduced-motion support remain. Offline checks now also verify forward extension and drag-dependent outline changes. Browser visual acceptance and Android integration remain pending.

#### Ejercicio · Hoy, Tu rutina y personita (2026-10-31)

Boceto: `docs/design/concept/EjercicioHoyRutinaVisual.html`. Programado en la misma escena web de la sesión (`app/src/main/session/home.js` y `home.css`), para que todas las pantallas compartan a la personita. Peso y medidas (Cuerpo) y Ajustes siguen nativos. Compilado; sin instalar porque el teléfono no estaba conectado. **Por validar en el S24+.**

- [ ] Hoy: frase por fase (mañana, en camino, trabajo, en casa, hora de entrenar, noche; sale de la fase de Rutinas), Ver tu rutina, Empezar o Seguir y barra inferior. El exceso de kcal pasó aquí; tocarlo cambia de opción. **Por validar.**
- [ ] Tu rutina: calendario, galería con tarjetas más chicas, + y − transparentes que solo toman color al tocarlos, lista con swipe, editar, cambiar, agregar y crear. El plan se guarda en `guided_exercise_records` (`plan:week`, `plan:today:<fecha>`), sin migración y dentro del respaldo. **Por validar.**
- [ ] Terminaste: minutos, series y metas en un solo apartado. **Por validar.**
- [ ] Personita: perfil con un ojo; de espaldas sin cara ni pelo; zancada calculada con el piso recorrido (no resbala); punto de fuga real con horizonte y piso tenue; se levanta apoyando la rodilla. **Por validar.**
- [ ] Fases nuevas: café y bostezo; camión con agarradera y frenón; mochila, celular y llamada; escritorio con laptop; lavarse los dientes, leer y cama con cobija. Entrenamiento: dominadas en barra, dominadas de balón, cuerda, flexiones, sentadillas, curl, press, parada de manos, rueda y mortal. La toalla seca cuello, cara, cabeza y brazos. **Por validar.**
- [ ] Interfaz: señala botones, se sube a ellos, se sienta con las piernas colgando, se acuesta, zapatea, se cuelga y hace dominadas de una tarjeta, escala el borde hasta el engrane, a veces resbala y se soba. Se puede agarrar con el dedo y soltarla. **Por validar.**
- [ ] Reacciones: se emociona al mantener Iniciar y sale volando con el brillo; sigue el dedo con la mirada; responde a series, peso, agregar, borrar, días y tarjetas. **Por validar.**
- [ ] Continuar: solo aparece al acabar el tiempo o las series, con la meta más abajo; la personita sube por el semicírculo y se monta en él. **Por validar.**
- [ ] Espía: la misma personita sale en cada hoja de abajo; también se sube, se sienta, se acuesta o camina sobre la orilla de la tarjeta. **Por validar.**
- [ ] Ajustes pedidos (2026-10-31): sin líneas de punto de fuga; sin textos de cómo usarlo; hora de entrenar junto a la frase de Hoy, editable al tocarla; sin botón Seguir (el botón de inicio solo sale a la hora de entrenar o con una sesión a medias, con borde blanco y rojo al presionar); Otros con la lista de Tu rutina; nombre del grupo solo en la tarjeta centrada; una sola tarjeta para cambiar ejercicio, flecha y Continuar chico; una sola personita en pantalla, nunca sobre información (bordes y esquinas); al tocar Continuar se lanza y el semicírculo la atrapa según su posición; espía arreglado. **Por validar.**
- [ ] Falta: el calentamiento ya no se puede saltar, solo salir con casa; en la app nativa de Semana y Cuerpo no se aplicó el estilo nuevo.

### Registro obligatorio

Boceto aprobado (aprobación general, 2026-10-29): `docs/design/concept/RegistroVisual.html`. Programado en `ui/ForcedEntryScreen.kt`, con 5 íconos de trazo nuevos (bolsa, transferencia, regalo, servicio, borrar). Se prueba con el mosaico "Pago de prueba".

Funcionalidad: falta mucho trabajo (aprobación general del boceto, 2026-10-29).

- [ ] Arriba, un punto por pago pendiente (el actual en rojo) y el candado; atrás no hace nada y no hay engrane. **Por validar.**
- [ ] Toque 1: el monto grande y, en la misma pantalla, comida (presupuesto de comida o bolsa de extras, con lo que queda hoy) o no comida (transferencia, regalo, servicio). Si el pago pasa lo que queda de comida, la bolsa sale en rojo. No comida se guarda con ese toque. **Por validar.**
- [ ] Tocar el monto abre un teclado numérico propio para corregirlo; ✓ confirma. **Por validar.**
- [ ] Toque 2 (comida): recetas en círculos con foto (inicial si no tiene), las más usadas primero; la hoja abre ingredientes (buscar, tocar, los gramos suben de 50 en 50); ✓ la marca como comida sin detalle. **Por validar.**
- [ ] Salida: las comidas que ya pasaron con su hora (la siguiente en rojo); tocarlas las marca y ✓ termina. **Por validar.**
- [ ] Avisos del banco (2026-11-04): el launcher lee las notificaciones de Nu, BBVA, Mercado Pago, Google Wallet, Stori, Plata y Revolut (`BankNotificationService.kt`, reglas en `data/BankParser.kt`). Un pago abre el registro; un ingreso abre su propio paso: el monto y un toque (estabilidad arriba → ingreso fijo; transferencia, regalo o servicio abajo). Si Wallet y el banco avisan el mismo monto en 3 min, cuenta una vez; dos pagos iguales en la misma app cuentan como dos. Se activa en el engrane de Finanzas → AVISOS DEL BANCO → PERMITIR. En Pruebas de Rutinas hay "Ingreso de prueba". **Por validar.**
- [ ] Reglas probadas con 24 avisos de ejemplo de Nu, Revolut, Mercado Pago, BBVA, Stori y Plata (2026-11-04, `BankParserTest`); todos caben en la regla general, sin reglas por banco. Avisos reales del teléfono (Nu "Transferencia exitosa", Revolut "Has recibido MXN…" y "Pagado MXN… en …") también. Un traspaso entre tus cuentas (sale de un banco y entra el mismo monto a otro, sin importar cuánto tarde, mientras el primero siga pendiente) no cuenta como pago ni como ingreso: el dinero sigue siendo tuyo. Falta: ejemplos de Google Wallet; avisos con otro texto se afinan cuando aparezcan; transferencia, regalo y servicio (en un pago) se guardan como gasto extra con su categoría; la bolsa usa la categoría "antojo"; receta siempre 1 porción; los gramos solo de 50 a 300; sin cuenta compartida ni "paga otro"; sin deshacer después de guardar.

### Comida y Finanzas · estilo de Ejercicio (2026-10-31)

Componentes nativos en `feature/ModuleStyle.kt`: Dock, título con brillo, enlace rojo, filas de registro, selector de rango y gráfica apilada.

- [ ] Comida: Dock (Hoy con las comidas que faltan, Semana, Recetas, Ajustes); mantener Recetas: Nueva receta. Registrar comida reemplaza al botón +. Etiquetas cortas en kcal, macros, comidas, agua y hábitos. Semana: kcal por día o semana (rojo: lo que pasó la meta), peso, IMC y registros. **Por validar.**
- [ ] Finanzas: Dock con Hoy, Resumen (semana o mes), Presupuesto y Ajustes. Hoy con las cuentas por cobrar. Registrar es un círculo rojo con + arriba del Dock, a la derecha, en Hoy y Resumen. **Por validar.**
- [ ] Gastos fijos al registrar (2026-11-04): en FIJO salen los conceptos del plan de Ajustes como botones; elegir uno llena el monto planeado y guarda el pago con ese concepto. Debajo se ve lo que queda (lo de hoy para diarios y semanales, el resto del mes para mensuales), en rojo si te pasas, y "Próximos días" con lo que toca cada día que falta: pagar de más hoy baja los días que siguen de ese mismo gasto. En Hoy, cada gasto fijo con sus puntos y lo que queda; un mensual pagado sale con ✓. **Por validar.**
- [ ] Registro obligatorio con nombres (2026-11-04): bajo cada ícono va su nombre (Comida, Antojo, Transferencia, Regalo, Servicio; en un ingreso: Ingreso fijo, Transferencia, Regalo, Servicio). Montos con comas. **Por validar.**
- [ ] Ingreso que cuenta (2026-11-04): lo detectado o registrado como ingreso fijo este mes es el ingreso de Ajustes; el que fijas con el lápiz es un mínimo (para un sueldo que aún no llega o que el banco no avisa). Presupuesto es aparte: empieza vacío y nada detectado entra ahí. **Por validar.**
- [ ] Gasto con presupuesto propio (2026-11-04): lo que tiene su tema (comida…) se descuenta solo de ese tema; "Queda hoy" y la semana solo cuentan lo que no tiene tema, y la semana libre ya descuenta lo que apartan los temas. La bolsa del mes sigue contando todo. **Por validar.**
- [ ] Hoy más claro (2026-11-04): "Queda hoy" grande y "Gastado" a la derecha; debajo, "+$X de días anteriores" (o en rojo con − si se gastó de más). **Por validar.**
- [ ] Meta de ahorro (2026-11-05): apartado dentro de Ajustes ("Meta de ahorro ›", con el monto a la derecha). Eliges cuánto y en cuánto tiempo (1, 3, 6 meses, 1 o 2 años); muestra cuánto toca por mes y hasta cuándo. Abonas o sacas dinero y la alcancía se llena: rojo en camino, ámbar si vas atrás del tiempo, verde al llegar. **Por validar.**
- [ ] Pastel del presupuesto (2026-11-05): en Hoy y en Resumen, el pastel ya viene partido por apartado (Libre en rojo, cada tema en tono cálido, cada gasto fijo en tono frío, Ahorro en verde en el mes), cada uno del tamaño de su % del presupuesto y apagado; lo que gastas en un apartado lo va prendiendo del centro hacia afuera, con borde rojo si te pasas. Tocar una rebanada o su fila lleva a su anillo; deslizando se pasa por el anillo de cada apartado (Queda, Gastado, Presupuesto). Hoy usa lo del día, Resumen la semana, el mes o el día elegido; los gastos fijos mensuales solo salen en el mes. Debajo, los últimos 5 movimientos de esa página con "Ver más". Números en tipografía normal. **Por validar.**
- [ ] Colores de estado del dinero (2026-11-05): en todo Finanzas el dinero se pinta por cómo va: verde (te queda bastante, menos de la mitad gastado), amarillo (vas, hasta 80%), naranja (te queda poco) y rojo solo cuando te pasas. Aplica a los anillos, Queda, Mes y Semana, puntos de gastos fijos, puntos de la tira de días y la gráfica. El pastel ya no usa rojo: Libre en verde azulado, temas en violeta/rosa, fijos en azules, ahorro en verde. Meta: amarillo en camino, naranja si vas atrás, verde al llegar. **Por validar.**
- [ ] Día elegido como Hoy (2026-11-05): en Resumen, tocar un día muestra la misma vista que Hoy con los datos de ese día (pastel, anillos y sus movimientos), sin la gráfica de barras. **Por validar.**
- [ ] Cada apartado en su periodo (2026-11-05): un tema o gasto fijo contado por semana ya no sale en el día; el día solo muestra los diarios, la semana los diarios y semanales (primero los semanales) y el mes todos (primero los mensuales). **Por validar.**
- [ ] Abajo de Hoy: Ahorro, Gastado y Te queda (2026-11-05): en lugar de Mes, Semana y Bolsa. Te queda = ingreso − ahorro − fijos − extras del mes. **Por validar.**
- [ ] Insights del pastel (2026-11-05): tocar una rebanada la marca (las demás se apagan) y en lugar de los totales sale una tarjeta con su nombre y frases: cómo vas, si gastas más rápido que el tiempo, cuánto te toca por día lo que resta, lo más grande, cuántos movimientos y su promedio, y su % del presupuesto. Tocarla otra vez o el centro la suelta; tocar la fila lleva a su anillo. **Por validar.**
- [ ] Ajustes se queda guardado (2026-11-05): al guardar, el plan y el % de ahorro se pisaban entre sí y se perdía el plan; ahora cada cambio se escribe sobre el último. **Por validar.**
- [ ] Ajustes de Finanzas (2026-11-04): el mismo tablero que Presupuesto, pero de verdad: ingreso fijo al mes (en $0 hasta editarlo con el lápiz), tabla de gastos fijos, ahorro y la barra fijos / ahorro / libre. Solo se guarda con el botón Guardar de abajo. Ese plan manda sobre lo registrado para calcular ahorro y bolsa (sin plan, cuenta lo registrado). Avisos del banco, bolsa y aparte, límite diario, presupuestos por tema y movimientos van en "Avanzado". **Por validar.**
- [ ] Resumen (2026-11-04): Semana y Mes en una sola pestaña con selector. Semana: tira de días como en Ejercicio (inicial y número; hoy con anillo rojo). Mes: los mismos días en calendario. Un punto marca si gastaste (rojo si pasaste lo del día); tocar un día lo rellena de rojo y filtra los movimientos a ese día. Abajo, la gráfica de barras por día y, en Mes, la barra del ingreso. **Por validar.**
- [ ] Presupuesto (2026-11-04): pestaña del Dock, tablero de simulación que no se guarda (dura mientras Finanzas está abierta). 1) Ingreso al mes; 2) gastos fijos en una sola tabla: por fila concepto y monto, abajo Día · Semana · Mes y lo que suma al mes; al pie el total al mes y qué % del ingreso se lleva (rojo si lo pasa); 3) ahorro %. Resultado: barra fijos / ahorro / libre y lo libre al mes, semana y día. **Por validar.**
- [ ] Montos con formato de moneda (2026-11-04): al escribir se ve "$12,345.50" en Presupuesto, Ajustes y Registrar; todas las cifras de Finanzas (y los números de puntos) llevan comas de miles. **Por validar.**

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

### Finanzas · paleta del diseño (2026-11-09)

Pedido: "Sí, aplicar" a la paleta del diseño `Rediseño de datos, iconos y títulos` (Artifact). Paleta: fondo `#000000`, texto `#f8f9f7`, acción/acento `#1a77b5`; alertas `#4dd402` (éxito), `#ffcf7d` (aviso), `#ff2962` (peligro); acentos para fechas, etiquetas y configuración: `#f2f0c4`, `#e21f25`, `#c2d806`. Se aplicó solo a Finanzas, por ahora.

- [x] Estados de dinero (`MoneyColors.kt`): sobra = verde `#4dd402`; en uso = azul `#1a77b5`; poco = ámbar `#ffcf7d`; pasado = rosa `#ff2962`. **Por validar en el S24+.**
- [x] Blanco y gris de Finanzas (`FinanceUi.kt`): texto `#f8f9f7`; el gris secundario pasa a `#E8E8E8`. **Por validar.**
- [x] Acción principal en toda la app (2026-11-09, pedido "Sí, en toda la app"): `LauncherRed` pasó a `LauncherAccent` = `#1a77b5` en todas las pantallas. Pasado, error y fuera de rango usan `LauncherDanger` = `#ff2962` (comida de más e IMC fuera de rango, respaldo con error, día con exceso, widget mal colocado). Los logos de IA conservan su rojo `#D71921`. Compila (`:app:compileDebugKotlin`). **Por validar en el S24+.**
- [ ] Pendiente: colores de categorías (`FinanceChartColors`) siguen con su paleta anterior.
- [x] Escala de texto en el tema (`ui/theme/LauncherTheme.kt`, 2026-11-09): título de vista 40 (headlineLarge), sección 24 (headlineMedium), elemento 17 (titleMedium y bodyLarge), dato de apoyo 14, etiquetas 12. Blanco `#F8F9F7` y secundario `#E8E8E8` en toda la app. **Por validar en el S24+:** los títulos cambian de tamaño en todas las vistas.
- [x] Gráficas (`ui/components/DataCharts.kt`): dona, columnas verticales, barras horizontales y línea continua suave. En Finanzas → Resumen, la vista "Gráficas" en cuadrícula 2×2 como el diseño (`feature/finance/FinanceCharts.kt`): distribución con leyenda, gasto semanal en columnas (por día si el período es corto, por semana si es largo), gasto por categoría y saldo del mes con línea suave. **Por validar.** Falta decidir si también va en Hoy o en una pestaña propia.

### Finanzas · Hoy como el diseño y vectores (2026-11-10)

Pedido: "no veo el main como lo diseñamos", y que las gráficas y el calendario sigan la paleta, los vectores estándar y la vista de calendario.

- [x] Iconos en lugar de texto: el botón de registrar es un círculo azul con + (sin "Registrar"); "Ver más" y "Leer más" son chevrones; la fila de pagos vinculados muestra el monto o un chevron. **Por validar.**
- [x] Calendario de Resumen abierto por defecto. **Por validar.**
- [x] Gráficas: azul en tonos; rosa solo en las columnas que superan el gasto del día (o de la semana). **Por validar.**
- [x] Hoy como el Main del tablero (2026-11-10, `feature/finance/FinanceHoy.kt`): saldo del mes grande, gasto semanal en columnas (rosa solo al pasarte del día), pagos de hoy con ojo (detalle) y lápiz (editar), y pagos vinculados con chevron. Quitados de Hoy: el resumen de Día, la tarjeta de meta de ahorro y la fecha de cabecera; siguen en Presupuesto y Ajustes. **Por validar en el S24+.**
- [x] Acciones sin palabras en Finanzas (2026-11-10, `ui/components/IconAction.kt`): editar, eliminar, registrar cargo, guardar, cancelar y permitir son íconos de trazo; el texto queda solo para accesibilidad. Pendiente: "Guardar meta" (botón ancho), "Ver/Editar" del Banco, "Ocultar movimientos". **Por validar.**
- [x] Escala tipográfica en Finanzas: se quitó el ajuste local que redefinía los estilos; ahora usa la escala global del tema (headline 40/24, title 17, body 17/15/14, caption 12). **Por validar.**
- [ ] Pendiente en el resto de la app: 106 textos de acción (Dieta, Notas, Rutinas, Apps, Ajustes, Pausa de apps) y 33 tamaños de letra fijos fuera del tema.
- [x] Gestos en movimientos (2026-11-10): deslizar a la derecha elimina (fondo rosa muy tenue, con confirmación); a la izquierda edita o categoriza (fondo azul muy tenue). Hoy ya no muestra ojo ni lápiz; tocar la fila despliega categoría y cuenta. **Por validar.**
- [x] Banco: tarjetas en cuadrícula como las cuentas; el día de corte va en amarillo pálido. **Por validar.**
- [x] Resumen: la gráfica de gasto por día es tocable (un toque selecciona el día y muestra su monto); el rosa marca los días que pasan el gasto diario. El selector Semana / Mes / Rango se mantiene. **Por validar.**
- [x] Escala tipográfica estricta en el tema (2026-11-10): vista 40 (headlineLarge, también el título de cada módulo), sección 24 (headlineMedium), cifra destacada 32 (headlineSmall), subsección 20 (titleLarge), elemento 17/15 (titleMedium/titleSmall), cuerpo 17/15/14 y caption 15/13/12. Volver es solo "‹". **Por validar.**
- [x] Resumen en orden de importancia (2026-11-10): selector Semana / Mes / Rango (igual que antes), luego lo gastado en grande con el color de cómo va y lo que queda, los días (fila o calendario, siempre a la vista), las gráficas una a la vez que se cambian deslizando (gasto por día, por categoría, por concepto, acumulado; puntos abajo; columnas y categorías se tocan) y los movimientos con gestos. "Gastos por tipo" queda cerrado al final. **Por validar.**
- [x] Resumen reordenado (2026-11-10, pedido "no tiene sentido que me separes la fecha de los días"): arriba el selector, luego el nombre del período junto con los días (deslizar a los lados cambia de semana o mes; tocar un día lo abre), después lo gastado. **Por validar.**
- [x] Gráficas con paleta variada (`ChartPalette`: azul, crema, lima, ámbar, azul claro, verde; el rosa solo al pasarte) y datos al tocar: el día (pagos, promedio, el más grande, lo que quedó o te pasaste), la categoría (% del gasto, pagos, promedio, el más grande), el concepto (% del gasto, tipo, pagos) y en Acumulado el promedio, el día más alto y los días sin gasto. **Por validar.**
- [x] Líneas de separación más visibles (`FinanceRule` #3B4C5C). **Por validar.**
- [x] Vista individual de cada banco (2026-11-10, `feature/bank/BankAccountScreen.kt`): al tocar un cuadro de la cuadrícula de Banco se abre su vista, en orden: nombre y tipo (lápiz para editar, "‹" o atrás para volver), saldo en grande (azul disponible; ámbar lo que se debe en tarjeta, con lo que te deben), en tarjetas las fechas de corte y pago con los días que faltan (rosa si faltan 3 o menos), "Este mes" con entradas contra salidas, "Últimas semanas" en columnas tocables, "En qué se fue" por categoría (tocar muestra su %) y sus movimientos con gestos. **Por validar.**
- [x] Hoy arriba (2026-11-10, pedido "que se vea el gasto ese día y abajito la diferencia entre ingreso y el gasto"): "Gastado hoy" en grande (color según lo que tocaba al día) y debajo solo la cifra de ingreso menos gasto del mes, sin etiqueta (verde con + si sobra, rosa si falta). No descuenta ni muestra lo ahorrado (pendiente: módulo de ahorro). Reemplaza "Saldo del mes". **Por validar.**
- [x] Hoy: "Pagos de hoy" pasa a "Movimientos recientes" (2026-11-10): los 5 más recientes con la misma lista y gestos de Resumen; el chevron muestra el resto. **Por validar.**
- [x] Pulido de contención (2026-11-10, /ui-taste: "definir qué tarjetas son necesarias"): tarjeta solo para lo que se toca como bloque (gráficas de Resumen y gasto semanal de Hoy) y lo editable (Plan de Presupuesto). Sin tarjeta, con espacio entre secciones (32) y encabezado: cifras principales (con su barra de color), listas (líneas entre filas) y las cuadrículas de Banco (sus celdas ya son tarjetas). **Por validar.**
- [x] Recuadro de datos al tocar (2026-11-10): cada gráfica abre un recuadro flotante justo donde se tocó y otro toque lo cierra; ya no hay espacio reservado debajo. Columnas: pagos, promedio, el más grande y lo que quedó o te pasaste. Dona (se toca el anillo o la leyenda): % del gasto, pagos, promedio y el más grande. Conceptos: % del gasto, tipo y pagos. Acumulado (se toca la línea): ese día, promedio al día y si vas bajo o arriba del ritmo. **Por validar.**
- [x] Tarjetas por sección (2026-11-10, pedido "usa tarjetas para separar bien las cosas"; reemplaza el "no todo en tarjetas" anterior): `FinanceCard` (fondo #0D141B, borde #22313F, radio 20). Resumen: días, lo gastado, gráficas, movimientos y gastos por tipo. Hoy: saldo, gasto semanal y pagos de hoy. Presupuesto: ingreso con la barra, gastos fijos, ahorro, libre y el plan. Banco: cuentas, tarjetas y por cobrar. **Por validar.**
- [x] Presupuesto como el diseño (2026-11-10): arriba, solo lectura, el ingreso grande, la barra fijos / ahorro / libre que se toca, y las secciones Gastos fijos, Ahorro y Libre. Abajo "Plan": lo editable cerrado (ingreso, fijos y ahorro; categorías; meta; avanzado; simular) que se abre al tocar y se cierra al guardar. Sin textos explicativos. **Por validar.**
- [x] Banco: títulos de sección a 24 con su total en color; sin subtítulos explicativos. **Por validar.**
- [x] Diseño "Resumen, Banco y Presupuesto" (Artifact, 2026-11-10), aplicado en parte: Hoy centra la semana en hoy (3 días antes, hoy y 3 que vienen); Banco muestra las cuentas de débito y efectivo en cuadrícula de 2 columnas, y la cuenta abierta muestra su detalle debajo. **Por validar.** Pendiente: gráficas tocables y "Resumen" con secciones nuevas; "Presupuesto" con secciones; gestos de deslizar (derecha archivar o eliminar, izquierda categorizar o duplicar) en movimientos.
- [x] Jerarquía en Hoy (2026-11-10): el saldo del mes es lo más grande y va en azul (rojo si está negativo); el ingreso va en verde; el gasto semanal en 24 en azul, rosa si pasa el presupuesto de la semana; los montos de cada pago en azul; nombres y títulos en blanco; etiquetas y datos secundarios en gris. **Por validar.**
- [ ] Pendiente: puntos rojos de fechas importantes y hoy en amarillo en el calendario.

### Finanzas · planeado vs. real (2026-11-11)

Boceto aprobado: lienzo "Finanzas: planeado vs. real" (Artifact). Pedido: "Implementalo… si le picas a algo que sea interactivo… swipes horizontales… tarjetas transparentes… vectores para acciones… (i) para contexto… secuencial y con jerarquía". Modelo en `feature/finance/FinanceMoney.kt`; piezas en `FinanceKit.kt`.

- [ ] Tres capas: real (cuentas, movimientos, deuda), planeado (pagos de tarjeta, pagos programados, ingreso con día) y topes (guía por tema, no se restan). Dinero disponible = en cuentas − por pagar este mes. **Por validar.**
- [ ] Hoy: Dinero disponible (toque = la cuenta), Por pagar y Gastado hoy (toque = lista), Viene en tarjetas que se deslizan (toque = fecha y ✓ / banco), Topes y Semana en páginas que se deslizan, movimientos. **Por validar.**
- [ ] Presupuesto (pestaña): Planeado (ingreso, pagos programados, tarjetas) y Topes, cada fila abre su cuadro con íconos (✓, lápiz, papelera); se editan en una hoja. Aviso si los topes pasan lo disponible. El engrane sigue abriendo los ajustes avanzados. **Por validar.**
- [ ] Banco: arriba En cuentas y Por pagar (toque = desglose); cuadrícula translúcida con punto por tipo, en tarjetas lo que toca y la barra del límite; "+" con ícono. **Por validar.**
- [ ] Registro: si el cobro se parece a un pago programado (monto ±10 % y ±7 días), lo propone primero para marcarlo hecho. **Por validar.**
- [ ] Resumen: Pasó / Viene arriba; se quitó "de $X · quedan" de la bolsa vieja. Widgets: "Disponible" y Pagos con estados. **Por validar.**
- [x] Tarjetas de Finanzas translúcidas (gris sobre negro), sin azul oscuro. Revisado en el teléfono con capturas.
- Falta: la Bolsa y la Semana del widget siguen con la cuenta vieja; el ingreso se marca hecho solo al registrar un ingreso fijo.
- [ ] Pulido (2026-11-11, /ui-taste): colores por jerarquía (título blanco grande, subtítulos en crema en mayúsculas, contenido blanco); tarjetas teñidas por lo que muestran (por pagar ámbar, gastado azul, ingreso verde, planeado punteado); íconos por acción (agregar azul, hecho verde, borrar rosa); tarjetas de crédito teñidas por cómo van contra su límite y "Por pagar" con la barra del límite total; nombre del banco en el color de su tipo; gráficas vacías con barras tenues y "Sin gastos"; desenfoque al deslizar páginas y tarjetas; se quitó el "Movimientos" repetido. **Por validar.**
- [ ] Presupuesto como reparto (pedido: "debería de ver mi dinero disponible"): En cuentas − Planeado (+ Por entrar) = Disponible − Topes por gastar = Para ahorro (o "Te pasas" en rosa). Tocar una fila abre su hoja para editar; borrar y marcar hecho están en la hoja. **Por validar.**
- [ ] Presupuesto con dos páginas para deslizar (2026-11-11): "Este mes" (el reparto, con "Ya apartado") y "Siguiente mes" (ingreso planeado, lo que ya toca pagar de tarjetas y pagos fijos, topes y "Te quedaría"/"Te faltarían"). **Por validar.**
- [ ] Ahorro en el momento: tocar "Para ahorro" abre "Apartar para ahorro" (cuánto y de qué cuenta sale); baja esa cuenta y suma a la meta. **Por validar.**
- [ ] Engrane de Finanzas simplificado: solo Avisos del banco y Meta de ahorro, cada uno con su (i). El plan viejo, categorías, límite diario y simulador salieron (viven en Presupuesto). **Por validar.**
- [ ] Vista de una cuenta: sin flecha de regreso (atrás del teléfono); el lápiz (y el + de Banco) abren el editor en un cuadro encima, no al final de la página. **Por validar.**

### Finanzas · bancos unificados y limpieza (2026-11-11)

Pedido: "elimines todos los movimientos y todos los datos de gastos" y que cada crédito y débito tenga los últimos 4 dígitos de sus tarjetas para reconocer las notificaciones. Base de datos versión 11; copia previa en `.local-backups/2026-11-11-antes-de-limpiar/` (fuera de git).

- [x] Limpieza: movimientos, pendientes por registrar, presupuestos por tema, plan de Ajustes (ingreso, fijos, pagos programados, % de ahorro), límite diario y meta. Se quedan Por cobrar y lo que otros deben de Nu ($6,530.72). Revisado en el teléfono.
- [x] Cuentas cargadas: Revolut (crédito $14,140.67 de $15,000, corte 3, pago 23: $9,888.19 el 23 nov y $3,822.09 el 23 dic; débito $23,366.25), Nu (crédito $9,877.41 de $37,400, corte 14, pago 24; débito $0.88), Stori (crédito $2,110.96 de $8,000, corte 12, pago 2 dic; débito $0), Plata (crédito $1,344.79 de $6,000, corte 27, pago 26: $262.86 el 26 nov y $1,081.93 después), Mercado Pago (crédito $0 de $3,300, corte 13, pago 3; débito $3.72), efectivo $0.
- [ ] Cada cuenta (crédito y débito) tiene "Tarjetas · últimos 4"; la tarjeta además Límite, A pagar y Siguiente. La vista de la cuenta muestra las tarjetas, lo que toca pagar, lo siguiente y lo disponible. **Por validar.**
- [ ] Notificaciones por banco (Revolut, Nu, Stori, Plata, Mercado Pago): compra y transferencia recibida piden registro con la cuenta ya elegida; retiro de efectivo se registra solo (débito → efectivo, o la tarjeta si es disposición); "Recibimos tu pago" baja la deuda de la tarjeta; el aviso de pago actualiza lo que toca pagar (el pago mínimo se ignora). Si la notificación trae los 4 dígitos, mandan; si no, compra = crédito del banco (Revolut con "Balance actual" = débito), ingreso y retiro = débito. Probado con los 20 textos de ejemplo. **Por validar con notificaciones reales.**
- [ ] Una compra con tarjeta cae en el pago que le toca según corte y pago; al pasar el día de pago, "Siguiente" pasa a "A pagar". **Por validar.**
- [ ] Widget Pagos: además de los pagos programados, lo que toca pagar de cada tarjeta. **Por validar.**
- [ ] Pagar una tarjeta desde el débito no cuenta como gasto (pedido 2026-11-11: "Sí, arréglalo"). Si la tarjeta avisa "Recibimos tu pago", se empareja sola con la salida del mismo monto en otro banco (antes o después, hasta 3 días): baja el débito y la deuda, sin registro. Si no avisa, en la pantalla de registro está "Tarjeta": eliges cuál pagaste (viene elegida la que tiene ese monto a pagar) y baja el débito y lo que debes. **Por validar.**

### Finanzas · widgets (2026-11-11)

Boceto: lienzo "Widgets de finanzas" (Artifact). Pedido: "todos". Programado en `feature/finance/FinanceWidgets.kt`; cada uno en 4×2, 2×2 y 4×1, sin texto ni contadores, pintado por cómo va el dinero. Instalado en el S24+.

- [ ] Hoy (reemplaza el widget de Finanzas con texto): anillo con lo que queda hoy; en 4×2 la semana por día, el último gasto y el + que abre Finanzas con el registro. **Por validar.**
- [ ] Semana y mes: lo que queda de la semana con una barra por día; en 4×2 la línea del saldo del mes (punteada hasta fin de mes al mismo ritmo); en 4×1 el mes como barra con marca de hoy. **Por validar.**
- [ ] Presupuesto: Libre y temas del mes; dona con los colores de cada tema, filas con barra y lo que queda, o íconos con barra. **Por validar.**
- [ ] Pagos programados: el siguiente pago con su día; rosa si toca hoy o ya pasó. En 4×1 el mes con un punto por pago. **Por validar.**
- [ ] Meta de ahorro: arco de avance, barra con marca de dónde deberías ir y un punto por mes. Colores de la meta como en la app. **Por validar.**
- [ ] Bolsa de extras: frasco que se vacía con lo gastado; en 4×2 los últimos extras, en 4×1 la línea del mes. **Por validar.**
- [ ] Por cobrar: cuentas por cobrar del Banco y cuentas compartidas; puntos por semana esperando (ámbar desde 3). **Por validar.**
- [ ] Pedido (2026-11-11): "una etiqueta para que sepa qué estoy viendo, lo más corta y simple". Cada widget lleva una etiqueta chica en gris: HOY, SEMANA, PRESUPUESTO, PAGOS, META, BOLSA, POR COBRAR (arriba a la izquierda; en 4×1 al inicio de la fila). **Por validar.**
- Cambio frente al boceto: el + usa el acento azul de la app (no rojo); la Bolsa se pinta por lo que queda y muestra los últimos extras, no entradas.

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

### Rutinas, Notas y Comida · más claro (2026-11-04)

Pedido: "hacer todo más claro, no solo con iconos, texto que señalen (no explicar)… no tarjetas y más tablas… los gestos y botones que ya tenemos, más claros". Sin boceto nuevo; sigue el criterio de Ejercicio y Finanzas. Compila. **Por validar en el S24+.**

- [ ] Rutinas: sin tarjeta; el título RUTINAS, REINICIAR y EDITAR en texto. Las fases llevan su nombre bajo el ícono (la abierta, subrayada) y su hora o LUGAR. Las acciones son una tabla: ícono, nombre, hora y HECHO o SIGUE. Editar: ACTIVA/APAGADA, QUITAR, AGREGAR ACCIÓN, y el catálogo también en tabla. **Por validar.**
- [ ] Notas: sin tarjeta; el título NOTAS. Cada aspecto con su nombre bajo el ícono. La lista es una tabla; EDITAR → FIJAR / QUITAR FIJA y BORRAR en texto; FIJA en rojo cuando está fijada. **Por validar.**
- [ ] Comida: metas en tabla (KCAL · META, PROTEÍNA, FIBRA) con lo que queda o lo que sobra en texto; el anillo se quitó. REGISTRAR COMIDA es un enlace rojo. La lista de comidas tiene encabezado HORA · COMIDA · KCAL. **Por validar.**
- [ ] Falta: la vista Semana de Comida y el catálogo de recetas siguen con el estilo anterior.

## Por validar en el teléfono

- [x] Bancos: desbloquear con huella y revisar que las 12 apps sean las correctas (se cambió la librería `fragment`, que también muestra el diálogo de huella).
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
