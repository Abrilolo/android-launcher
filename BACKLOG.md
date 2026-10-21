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

Vista en curso: **Inicio y páginas**

| Vista | Archivo principal | Estado | Notas |
| --- | --- | --- | --- |
| Inicio y páginas | `ui/HomeGrid.kt`, `ui/LauncherScreen.kt` | En curso | Ver "Inicio y páginas" abajo |
| Página de Apps (antes cajón) | `ui/AppsPage.kt` | Por validar | Ahora es la página principal |
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

Detalles por vista: se agregan debajo de esta línea, con un encabezado por vista, cuando se empiece a trabajar en ella.

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

## Por validar en el teléfono

- [ ] Bancos: desbloquear con huella y revisar que las 12 apps sean las correctas (se cambió la librería `fragment`, que también muestra el diálogo de huella).
- [ ] Ejercicio: una sesión completa a las 20:35 y que la semana la marque hecha.
- [ ] Respaldo: exportar a Drive y guardarlo fuera del teléfono.

## Siguiente (recomendado)

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
