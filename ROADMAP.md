# Roadmap

Project: Minimal Launcher  
Aspect: Personal  
Default mode: New Project  
Execution mode: New Android project  
Status: Planning  
Last updated: 2026-10-16

## Current Phase

Current phase: Phase 1 - Define MVP

Current objective:

- Convertir la idea en una especificación pequeña y verificable antes de crear el módulo Android.

## Phase List

| Phase | Goal | Deliverables | Exit Criteria | Status |
| --- | --- | --- | --- | --- |
| Phase 0 - Clarify | Fijar problema y principios | IDEA.md | Objetivo, usuario y límites definidos | Complete |
| Phase 1 - Define MVP | Definir comportamiento verificable | REQUIREMENTS.md, DECISIONS.md | Criterios de aceptación definidos | Complete |
| Phase 2 - Build Shell | Crear launcher Compose mínimo | Proyecto Android, Manifest, UI base | Compila y puede seleccionarse como Home | Next |
| Phase 3 - Validate Use | Probar uso cotidiano | Registro de observaciones | Decisión sobre widgets y categorías | Planned |
| Phase 4 - Improve | Añadir valor validado | Persistencia y ajustes mínimos | Nueva iteración aprobada | Future |

## Phase 2 - Build Shell

Deliverables:

- Módulo Android Kotlin/Compose.
- AndroidManifest con HOME, DEFAULT y QUERY_ALL_PACKAGES.
- Modelo LauncherApp.
- Repositorio para consultar apps y abrirlas con Intent.
- ViewModel con estado de clasificación.
- MainZone y EntertainmentZone.
- Pager horizontal con “Mi día” como primera página.

Exit criteria:

- Compilación correcta.
- Launcher seleccionable desde los ajustes de Android.
- Apps visibles en la zona correcta.
- Tap funcional para abrir apps.

## Phase 3 - Validate Use

Validation questions:

- ¿La separación entre zonas reduce aperturas impulsivas?
- ¿La lista de texto permite encontrar apps con rapidez?
- ¿Las cuatro acciones rápidas son realmente útiles?
- ¿Qué apps deben cambiar de zona?

## Future Ideas

- Clasificación editable desde ajustes internos.
- Persistencia local de hábitos.
- Pantallas propias para gastos, tareas y ejercicio.
- Indicador discreto de fecha, hora y progreso diario.
- Ocultar apps poco frecuentes sin eliminarlas.
- Configuración de gestos y navegación.

## Next Action

Crear el esqueleto Android Compose y ejecutar únicamente la Phase 2.
