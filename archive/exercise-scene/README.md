# Escena de ejercicio (archivada 2026-11-11)

La vista de Ejercicio con el muñequito animado (WebView con el motor SVG del boceto `docs/design/concept/EjercicioSesionVisual.html`).
Se archivó porque ocupaba mucho espacio; Ejercicio ahora es nativo y sigue el patrón visual de Finanzas.

- `GuidedExerciseScreen.kt`: el WebView y su puente con Android.
- `session/`: runtime y estilos que se inyectaban al boceto.
- `exercise-session.test.cjs`: pruebas del runtime (rutas relativas ya no apuntan aquí).

Para volver a usarla: regresar los archivos a `app/src/main/...` y la tarea `generateExerciseSessionAssets` a `app/build.gradle.kts` (ver historial de git).
Los datos (`guided_exercise_records`, `set_logs`) siguen en uso por la vista nativa.
