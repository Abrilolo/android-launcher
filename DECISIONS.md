# Decisions

Project: Minimal Launcher  
Aspect: Personal  
Last updated: 2026-10-21

## 2026-10-21 - Respaldo como archivo que elige el usuario

Type: Technical / Data
Status: Accepted

### Context

Todos los datos viven solo en el teléfono; desinstalar o cambiar de teléfono los perdería. La nube (Fase 16) es un paso más grande.

### Decision

- Exportar a un JSON con el selector de archivos del sistema: el usuario decide si va a Descargas, Drive u otro proveedor. No se piden permisos de almacenamiento ni cuentas.
- Importar fusiona: conserva lo que ya hay en el teléfono y agrega lo que falte (las series se comparan por día, ejercicio y número de serie).
- Incluye los ajustes de módulos; no incluye espacios, pausa ni apartados.

### Impact

- El formato (`"app": "minimal-launcher"`, `"format": 1`) servirá de base para la sincronización de la Fase 16.


## 2026-10-21 - Inicio en cuadrícula de widgets y cajón de apps

Type: Product / UX
Status: Accepted
Supersedes: inicio con apartados a la izquierda y carrusel de widgets a la derecha; sección "Restringidas"

### Context

El objetivo es ver el estado del día sin abrir nada, con estilo Nothing (negro, matriz de puntos, un solo rojo). El concepto de referencia está en `docs/design/`.

### Decision

- El inicio es un reloj en matriz de puntos y una cuadrícula de tarjetas; cada tarjeta abre la página de su módulo.
- Las apps viven en un cajón que se abre con un ícono de 3×3 puntos abajo al centro (no con deslizar).
- Lo que el horario no permite no se muestra: los apartados sin apps permitidas se ocultan y lo restringido solo se encuentra buscando en "Todas", con pausa.
- El launcher se queda en vertical.

### Impact

- Recordatorios, Notas, Finanzas y Dieta dejan de ser apartados de la lista y se abren desde sus tarjetas.
- La posición editable del widget deja de usarse.

## 2026-10-21 - El launcher es la app; cada aspecto es un módulo

Type: Architecture
Status: Accepted

### Context

Se quiere a futuro "una app propia" que junte finanzas, dieta, ejercicio, notas y bancos en un mismo ambiente con sus datos.

### Decision

- No crear una app aparte por ahora: el launcher ya es el ambiente de todo el día.
- Cada aspecto es un módulo con tres piezas: widget, página y ajustes (`feature/exercise`, `feature/vault`).
- Se separarán en módulos de Gradle cuando exista una segunda app que los reutilice.

### Alternatives Considered

- App separada que comparta datos con el launcher (descartado por ahora: obliga a sincronizar entre dos apps).

## 2026-10-21 - Room para los registros, DataStore para preferencias

Type: Technical
Status: Accepted
Supersedes: registros guardados como texto en DataStore

### Decision

- Todos los registros (notas, recordatorios, gastos, comidas, medidas, hábitos, series de ejercicio) se guardan en Room (`launcher.db`).
- Los datos que existían en DataStore se copian una sola vez y la copia antigua se conserva.

### Impact

- Permite historial, consultas por fecha y exportar datos más adelante.

## 2026-10-21 - Bancos protegido con la huella o el PIN del teléfono

Type: Product / Security
Status: Accepted

### Decision

- Las apps de banco solo se abren desde el widget Bancos tras pasar la huella o el PIN del sistema (`BiometricPrompt`).
- Sin bloqueo de pantalla configurado, Bancos no se abre.
- Se vuelve a bloquear por tiempo, al abrir una app o al salir del launcher, y nunca aparece en el buscador.


## 2026-10-19 - Launcher contextual por espacios horarios

Type: Product / Scope
Status: Accepted
Review date: After Phase 4
Supersedes: "MVP orientado a dos zonas" (parcialmente)

### Context

El objetivo evolucionó: el launcher debe acompañar el día mostrando lo relevante según la hora (agenda, recordatorios, hábitos), no solo separar productividad de entretenimiento.

### Decision

La pantalla principal mostrará un **espacio** según la franja horaria:

| Espacio | Franja inicial | Enfoque |
| --- | --- | --- |
| Mañana | 06:00 - 12:00 | Rutina matinal, desayuno, agenda del día; entretenimiento restringido |
| Trabajo | 12:00 - 18:00 | Apps de productividad, tareas, agenda |
| Noche | 18:00 - 24:00 | Cierre del día, notas, entretenimiento permitido |

Las franjas se definirán primero en código y serán editables en una fase posterior.

### Alternatives Considered

- Mantener dos zonas fijas (Mi día / Entretenimiento).
- Espacios por ubicación o movimiento (descartado: requiere permisos y servicios extra).

### Impact

- La zona "Entretenimiento" deja de existir como página fija.
- Cada app tiene horarios en los que está disponible.
- El MVP actual de dos zonas se mantiene como base técnica.

## 2026-10-19 - Entretenimiento como apps disponibles por horario

Type: Product
Status: Accepted
Review date: After Phase 5

### Decision

Las apps de entretenimiento no tendrán una página propia. Cada espacio define qué apps están disponibles; fuera de su horario, una app queda restringida.

### Reason

Elimina el "rincón de distracción" siempre accesible con un swipe y vincula el entretenimiento al momento del día en que es aceptable.

## 2026-10-19 - Bloqueo por fricción, no absoluto

Type: Product / UX
Status: Accepted
Review date: After Phase 5

### Decision

Las apps restringidas no serán imposibles de abrir, pero abrirlas será deliberadamente incómodo. Capas de fricción previstas, de menor a mayor:

1. La app no aparece en la lista del espacio actual.
2. Solo se encuentra en una sección "Restringidas", en texto atenuado.
3. Al tocarla se muestra una pantalla de pausa con cuenta atrás (p. ej. 15 s) y la hora a la que vuelve a estar disponible.
4. Para continuar hay que escribir una frase o motivo de apertura.
5. (Futuro, opcional) Un `AccessibilityService` intercepta aperturas desde fuera del launcher (notificaciones, recientes) y muestra la misma pausa.

### Alternatives Considered

- Bloqueo absoluto: descartado; genera frustración y tiende a desinstalarse.
- Sin bloqueo: insuficiente para reducir aperturas impulsivas.

### Risks

- Las capas 1-4 solo actúan dentro del launcher; la app sigue accesible desde notificaciones o recientes hasta implementar la capa 5.
- Samsung (One UI) puede detener servicios de accesibilidad en segundo plano.

## 2026-10-19 - Estética solo texto en blanco y negro

Type: UI
Status: Accepted
Review date: After Phase 3

### Decision

La interfaz no tendrá iconos de aplicaciones. Todo será texto, en blanco y negro (fondo negro, texto blanco, atenuados en gris para elementos secundarios o restringidos).

### Reason

Reduce el atractivo visual de las apps y refuerza un uso intencional.

### Impact

- Jerarquía visual basada en tamaño, peso y opacidad del texto.
- Secciones desplegables (▸ / ▾) en lugar de tarjetas o iconos.

## 2026-10-19 - Pantalla principal en dos columnas

Type: UI
Status: Accepted
Review date: After Phase 3

### Decision

Debajo del reloj, la pantalla principal se divide en dos columnas:

- Izquierda (~55%): apartados desplegables (Recordatorios, Apps, Todas).
- Derecha (~45%): espacio reservado para una visualización propia, aún por definir.

### Impact

- La columna izquierda hace scroll de forma independiente.
- Los nombres de apps y apartados deben caber en media pantalla.

## 2026-10-19 - Finanzas y dieta: notas ahora, módulos propios en el futuro

Type: Scope
Status: Accepted
Review date: After Phase 6

### Decision

Por ahora, finanzas y dieta se registrarán como **notas** locales dentro del launcher. En el futuro se construirán módulos propios (registro de gastos, comidas, resúmenes) en lugar de depender de apps externas.

### Reason

Permite empezar a capturar información sin diseñar todavía modelos de datos complejos.

### Impact

- Se necesita persistencia local para notas y recordatorios.
- Las notas deben poder etiquetarse (Finanzas, Dieta, General) para migrarlas a los módulos futuros.

## 2026-10-16 - MVP orientado a dos zonas

Type: Product / Scope  
Status: Superseded by "Launcher contextual por espacios horarios" (2026-10-19)  
Review date: After Phase 3

### Context

El objetivo principal es reducir distracciones y organizar el acceso diario desde Home.

### Decision

El MVP tendrá dos zonas independientes:

1. Main Zone: Mi día, hábitos, productividad y apps esenciales.
2. Entertainment Zone: redes sociales, juegos y otras apps no esenciales.

### Alternatives Considered

- Una única pantalla con todas las apps.
- Un drawer tradicional con iconos.
- Bloquear completamente las apps de entretenimiento.

### Reason

La separación reduce la exposición inicial a entretenimiento sin impedir el acceso deliberado.

### Impact

- Home debe abrir siempre la zona principal.
- La navegación secundaria requiere swipe o una acción explícita.

### Risks

- El swipe puede ser poco visible.
- Una clasificación incorrecta puede aumentar la fricción.

### Evidence Or Source

- Requisitos expresados por el propietario del proyecto.

## 2026-10-19 - Samsung Galaxy S24+ como dispositivo principal

Type: Product / Validation
Status: Accepted
Review date: After Phase 3

### Decision

El Samsung Galaxy S24+ será el dispositivo principal para validar el diseño, el tamaño de los controles, los insets del sistema y el uso cotidiano.

### Reason

Es el teléfono real del propietario y la experiencia debe priorizar su pantalla, densidad y navegación.

### Impact

- Las decisiones visuales se probarán primero en el S24+.
- El diseño seguirá siendo responsive para no quedar limitado exclusivamente a ese dispositivo.
- Las pruebas en emulador servirán como apoyo, no como validación final.

## 2026-10-19 - Layout edge-to-edge con área segura

Type: Technology / UI
Status: Accepted
Review date: After Phase 3

### Decision

La aplicación utilizará un tema sin ActionBar, layout edge-to-edge y padding de área segura para evitar que el contenido quede debajo del status bar o navigation bar.

### Reason

La barra de acción automática estaba ocultando y cortando el título de la pantalla principal en el dispositivo físico.

### Impact

- El tema Android debe permanecer sin ActionBar.
- Las pantallas Compose deben respetar `safeDrawingPadding`.
- Los cambios visuales deben verificarse en el teléfono físico.

## 2026-10-16 - Jetpack Compose como UI del MVP

Type: Technology  
Status: Accepted  
Review date: After Phase 2

### Decision

Toda la interfaz inicial se implementará con Jetpack Compose.

### Reason

Compose simplifica el pager, el estado visual y la evolución de las dos zonas.

### Impact

- La UI dependerá de Material 3 y Foundation.
- El estado de apps se expondrá desde un ViewModel.

## 2026-10-16 - Clasificación inicial por package name

Type: Architecture / Scope  
Status: Accepted  
Review date: After Phase 3

### Decision

La primera clasificación se definirá mediante una colección de package names en código, aislada en el repositorio de apps.

### Alternatives Considered

- Clasificación automática por categorías.
- Clasificación basada en tiempo de uso.
- Configuración editable desde el primer build.

### Reason

Es la solución más pequeña para validar la experiencia principal.

### Impact

- La lista será modificable sin cambiar las pantallas.
- La configuración editable queda para una fase posterior.

### Risks

- Los package names pueden variar entre aplicaciones o dispositivos.

## 2026-10-16 - Acceso a aplicaciones mediante Intent

Type: Architecture  
Status: Accepted  
Review date: After Phase 2

### Decision

El repositorio obtendrá el launch intent asociado a cada package name y la UI invocará una única función openApp.

### Reason

Mantiene la UI independiente de las aplicaciones concretas.

### Impact

- La Activity del launcher no debe aparecer en su propia lista.
- Apps sin launch intent válido se omitirán o no podrán abrirse.
