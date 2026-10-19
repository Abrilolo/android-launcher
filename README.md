# Minimal Launcher

Aspect: Personal  
Default mode: New Project  
Execution mode: New Android project  
Current status: MVP shell functional; visual polish in progress
Repository or project path: C:\Workspace\20-Personal\Labs\android-launcher

## Project Goal

Crear un launcher Android minimalista, solo texto en blanco y negro, que muestre lo relevante según la hora del día (espacios Mañana, Trabajo y Noche) y que haga incómodo abrir apps distractoras fuera de su horario.

## Why This Project Matters

- Personal reason: Convertir la pantalla Home en un punto de inicio intencional.
- Problem, desire, or opportunity: Evitar que las apps distractoras aparezcan junto a las herramientas esenciales.
- Expected benefit: Menos aperturas impulsivas y acceso más rápido a las actividades importantes.
- Success signal: Uso diario del launcher sin volver inmediatamente al launcher convencional.

## Project Type

- Type: Personal Android application.
- Primary output: Launcher funcional construido con Kotlin y Jetpack Compose.

## Current Phase

Current phase: Phase 3 - Visual Polish and Device Validation

The Android Compose shell is functional on the Samsung Galaxy S24+. Swipes work, apps open correctly, the launcher can be selected as Home, and the system action bar/inset issue that clipped the title has been corrected.

## Product Summary

Built (MVP shell):

- Zona principal “Mi día” y zona de entretenimiento mediante swipe.
- Descubrimiento de apps con PackageManager.
- Clasificación inicial por package name.
- Apertura con Intent.

Target (see ROADMAP.md):

- Estética solo texto, blanco y negro, sin iconos; reloj y alarma arriba; apartados desplegables.
- Espacios por horario: Mañana (06-12), Trabajo (12-18), Noche (18-24).
- Entretenimiento como apps disponibles por horario, no como zona propia.
- Fricción progresiva para apps restringidas: oculta, atenuada, pausa con cuenta atrás y motivo. No es bloqueo absoluto.
- Notas etiquetadas (Finanzas, Dieta, General) y recordatorios. Módulos propios de finanzas y dieta en el futuro.

## Internal Documents

- IDEA.md: intención, usuario, hipótesis y límites.
- RESEARCH.md: notas técnicas y preguntas pendientes.
- REQUIREMENTS.md: alcance y criterios verificables del MVP.
- ROADMAP.md: fases de construcción y validación.
- DECISIONS.md: decisiones aceptadas de producto y arquitectura.

## Planned Architecture

    MainActivity
        -> LauncherScreen
            -> MainZone
            -> EntertainmentZone
        -> LauncherViewModel
            -> AppRepository
                -> PackageManager / Intent

Planned evolution: `LauncherScreen` renderiza el `Space` activo; un `SpaceRepository` resuelve el espacio por hora y la disponibilidad de apps; una pantalla de fricción intercepta las apps restringidas; un almacén local (DataStore o Room) guarda notas y recordatorios.

## Out Of Scope

- Backend y sincronización.
- Bloqueo absoluto de aplicaciones.
- Analítica remota.
- Iconos y personalización visual avanzada.

## Validation

La primera validación será manual en un dispositivo Android o emulador:

1. Compilar e instalar.
2. Seleccionar Minimal Launcher como aplicación Home.
3. Verificar que “Mi día” aparezca primero y que el título no quede cortado.
4. Confirmar swipes, clasificación y apertura de apps.
5. Repetir las pruebas en el Samsung Galaxy S24+.
6. Usarlo durante varios días y registrar fricciones de diseño y navegación.

## Next Action

Aplicar el tema blanco y negro solo texto en la pantalla principal (reloj, alarma, apartados desplegables, lista de apps) y validarlo en el Galaxy S24+. Después, implementar los espacios por horario (Phase 4).
