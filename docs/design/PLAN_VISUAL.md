# Plan · Más visual, menos texto

Fecha: 2026-10-22. Estado: **aprobado** (decisiones del 2026-10-22 abajo).

## Qué queremos

1. **Menos gris difícil de leer.** El rojo marca lo importante; el gris solo para lo vacío o secundario.
2. **Iconos en lugar de palabras.** Iconos de puntos y de línea al estilo Nothing: Agua es un vaso, Bloqueado es un candado.
3. **Que el dibujo diga el dato.** Como la interfaz Glyph de Nothing: se ve cuánto falta, qué sigue o si algo se pasó, sin leer.
4. **Reacciones.** Cada acción importante (guardar, marcar, borrar, cambiar) responde con una vibración corta y una animación pequeña.

Se conserva el estilo actual: fondo negro, puntos, un solo acento rojo (#D71921) y texto en mayúsculas espaciadas.

## Reglas de color

| Uso | Color | Ejemplo |
|---|---|---|
| Lo que importa ahora: lo siguiente, lo pendiente que urge, lo que se pasó, el título de cada bloque | Rojo #D71921 | "GASTO HOY", la próxima comida, gasto sobre presupuesto |
| Dato principal, lo hecho | Blanco | El total, una comida marcada |
| Dato secundario (horas, fechas) | Gris claro, sube de #8A8A8A a #B0B0B0 | "14:54" bajo un gasto |
| Vacío, pendiente que no urge | Gris medio #4A4A4A (el de las bolitas de Finanzas) | Vaso sin llenar |
| Líneas y tarjetas | #222222 / #0B0B0B (sin cambio) | Bordes |

Regla: si hay que leerlo para entender algo, no va en gris oscuro.

## Bases (se hacen una vez y las usan todas las vistas)

### B1 · Juego de iconos

- **Iconos de puntos** (rejilla de 9×9) dibujados con el mismo motor que el reloj de puntos. Sirven para estados y para iconos grandes. Pueden **llenarse por partes**: un vaso lleno a la mitad son medio vaso de puntos.
- **Iconos de línea** (trazo de 2 dp, puntas redondas) para controles chicos: engrane, papelera, casa, más, palomita, candado, pila, buscar, flecha. La casa y la papelera ya existen así.
- Cada icono lleva su descripción para lectores de pantalla, para no perder accesibilidad al quitar texto.

Primer juego (unos 30): vaso, plato, sol, luna, taza, hoja, pesa, corazón, cuerpo/báscula, cinta métrica, moneda, cartera, candado cerrado y abierto, huella, alfiler (fijar), maletín, foco, nota, campana, reloj, globo, avión, fuego (racha), palomita, más, menos, papelera, engrane, pila, buscar, reloj de arena.

### B2 · Medidores de puntos

Piezas que se repiten:

- **Fila de puntos**: progreso por partes (4 comidas, 8 vasos, 5 sesiones).
- **Anillo de puntos**: tiempo que falta (alarma, descanso entre series, bloqueo de Bancos).
- **Barra de puntos**: cantidad contra un límite (gasto contra presupuesto). Se pone roja al pasarse.

### B3 · Reacciones

- **Vibraciones**: una corta al marcar o guardar, una doble al terminar algo (día completo, última serie), una "de error" cuando no se puede (monto vacío).
- **Números que ruedan**: cuando cambia un total, los dígitos de puntos cuentan hasta el valor nuevo.
- **Punto que respira**: lo siguiente (la próxima comida, la siguiente serie) late despacio en rojo.
- **Llenado**: al marcar, el punto o icono se llena con un pequeño rebote.
- **Barrido**: al completar un día, todos los puntos se encienden en secuencia.
- **Borrar con deshacer**: la fila sale hacia un lado y aparece "DESHACER" unos segundos.
- Todo se apaga si el teléfono tiene las animaciones desactivadas.

## Puntos que se prenden y se apagan con significado (2026-10-22)

Idea del usuario: cada punto puede estar **blanco, apagado (negro) o rojo**, y la animación no es adorno: dice algo. Iconos de 15×15 puntos (los de comida y snack los diseñó el usuario; boceto en `concept/DietaVisual.html`).

Reglas para todas las vistas (corregidas el 2026-10-22: **nada parpadea en bucle**, se veía como un LED descompuesto):

| Estado | Puntos |
|---|---|
| Sin marcar o inactivo | Ícono completo en **gris visible** (#5C5C5C) |
| Al tocarlo | **Una sola animación corta**: los puntos se prenden de abajo hacia arriba y lo rojo sale al final (vapor de la cena, agua que llena el vaso) |
| Marcado o activo | **Blanco** con sus detalles en **rojo**, quieto |
| Cambia un número | Se redibuja punto por punto **una vez** |
| Lo siguiente | Se distingue por color (su hora en rojo), sin animación |

Cómo se aplica en el resto:

- **Finanzas:** al guardar, el total se redibuja punto por punto; la barra de presupuesto se llena en blanco y los puntos que pasan del límite se prenden en rojo uno a uno.
- **Alarma:** el anillo pierde un punto por cada tramo de tiempo; en la última hora los que quedan son rojos y al sonar laten.
- **Ejercicio:** cada serie hecha prende sus puntos; el descanso es un anillo que se apaga punto por punto y vibra al terminar.
- **Bancos:** el candado se abre prendiendo sus puntos al poner la huella; el anillo de "se vuelve a bloquear" se apaga hasta cerrarse.
- **Agua y hábitos:** el vaso se llena fila por fila; el hábito se prende al marcarlo.
- **Reloj mundial:** el sol o la luna de cada ciudad se prende o se apaga según amanezca o anochezca allá.
- **Semana:** al abrirla, los días se prenden en orden; los que faltan quedan apagados.

## Engrane en cada vista (2026-10-22)

Toda vista de módulo lleva arriba a la derecha el engrane (`SettingsGear` en `feature/ModuleUi.kt`). Abre directo los ajustes de ese módulo y atrás regresa a la vista. Ya está en Dieta y Finanzas; las demás lo reciben cuando se pulan.

## Vista por vista

Orden propuesto: lo que más se ve primero. Cada vista sigue el proceso del backlog: boceto en el concepto (`docs/design/concept/`), lo apruebas, se programa, se prueba en el teléfono y lo validas.

### 1. Dieta (página y widget)
- **Agua**: 8 vasos de puntos que se llenan; tocar uno lo llena con rebote. El siguiente vaso por llenar tiene borde rojo.
- **Comidas**: 4 iconos (sol = desayuno, plato = comida, taza = merienda, luna = cena) en lugar de los nombres. La siguiente late en rojo con su hora debajo; las hechas en blanco.
- **Hábitos**: icono por hábito en lugar de la palabra.
- **Día completo**: barrido de puntos y vibración doble.
- Fuera: "COMIDAS HOY", "SIGUE: … A LAS …" (lo dice el punto rojo con su hora).

### 2. Finanzas (página y widget)
- **Barra de presupuesto**: puntos que se llenan contra el presupuesto diario y se ponen rojos al pasarse; el total también se pone rojo y late una vez.
- El total **rueda** al guardar un gasto; la fila nueva entra deslizando.
- Borrar un gasto con **deshacer**.
- "2 REGISTROS · SEMANA $188.44" pasa a icono de cartera + total de la semana.

### 3. Inicio (widgets y página de Apps)
- Encabezado de cada widget con **icono** en lugar del nombre (vaso, moneda, campana, globo, candado, alfiler, pesa).
- **Alarma**: anillo de puntos con lo que falta; rojo en la última hora.
- **Reloj mundial**: sol o luna por ciudad según sea de día o de noche allá, y diferencia de horas en puntos.
- **Bancos**: candado cerrado o abierto y anillo con el tiempo para volver a bloquearse.
- **Apps**: el aviso "OCIO Y JUEGOS VUELVEN A LAS 18:00" pasa a reloj de arena + **18:00** en rojo; las apps fuera de horario llevan un candado chico.
- **Modo edición**: barra con iconos (cuadrícula = Widgets, engrane = Ajustes, palomita = Listo); en cada widget, ✕ y pila con icono. Al levantar un widget, vibración y sombra; al soltar en una pila, los dos se "juntan" con una animación.

### 4. Alarmas
- Cada alarma con anillo de tiempo restante; la próxima en rojo.
- Recordatorios con icono de campana; al agregar uno, vibración y entrada deslizando.

### 5. Reloj mundial
- Ciudades con sol o luna, la hora en puntos y una regla de 24 puntos que marca la hora local de cada una.
- "AQUÍ" pasa a un punto rojo junto a tu ciudad.

### 6. Notas
- Categorías con icono (General = nota, Trabajo = maletín, Ideas = foco, Finanzas = moneda, Dieta = hoja) en lugar de la palabra.
- "FIJAR"/"FIJADA" pasa a un alfiler (rojo cuando está fijada). "BORRAR" pasa a papelera, con deshacer.

### 7. Ejercicio
- Series como fila de puntos por ejercicio; la siguiente late en rojo.
- Descanso como anillo de puntos que se vacía, con vibración al terminar.
- Semana: racha con icono de fuego; sesiones hechas encendidas.
- Peso y cintura con báscula y cinta métrica, y una flecha roja o blanca según suba o baje.

### 8. Bancos (bóveda)
- Candado que se abre con animación al poner la huella; anillo con el tiempo para volver a bloquearse.
- "BLOQUEADO · 12 APPS" pasa a candado + 12.

### 9. Semana
- Cada bloque con su icono (pesa, moneda, vaso/plato, báscula) en lugar del título en texto.
- Días como puntos; hoy en rojo.

### 10. Módulos, ajustes y pausa
- Lista de módulos con icono; los textos de ayuda largos se acortan o pasan a una "i".
- Pantalla de pausa (apps fuera de horario): anillo de respiración que se cierra en lugar de texto con cuenta regresiva.

## Decisiones (2026-10-22)

1. **Iconos:** de puntos para lo grande y los estados; de línea fina para los controles chicos.
2. **Texto:** icono + número u hora. Se quita la palabra donde el icono es obvio; los títulos largos y las ayudas se acortan.
3. **Vibración:** activa por defecto, con un interruptor en Ajustes para apagarla.
4. **Orden:** después de las bases, primero **Dieta**; luego Finanzas, Inicio y las demás.

## Primer paso propuesto

Hacer las bases B1–B3 con un boceto de **Dieta** en el concepto, para ver iconos, colores y reacciones juntos antes de tocar las demás vistas.
