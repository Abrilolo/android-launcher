# Minimal Launcher

Aspect: Personal  
Default mode: New Project  
Execution mode: New Android project  
Current status: Planning complete for MVP  
Repository or project path: C:\Workspace\20-Personal\Labs\android-launcher

## Project Goal

Crear un launcher Android minimalista que reduzca distracciones y organice el acceso diario mediante dos zonas separadas: productividad y entretenimiento.

## Why This Project Matters

- Personal reason: Convertir la pantalla Home en un punto de inicio intencional.
- Problem, desire, or opportunity: Evitar que las apps distractoras aparezcan junto a las herramientas esenciales.
- Expected benefit: Menos aperturas impulsivas y acceso más rápido a las actividades importantes.
- Success signal: Uso diario del launcher sin volver inmediatamente al launcher convencional.

## Project Type

- Type: Personal Android application.
- Primary output: Launcher funcional construido con Kotlin y Jetpack Compose.

## Current Phase

Current phase: Planning

El siguiente bloque aprobado será la creación del shell Android Compose y su validación como launcher Home.

## MVP Summary

- Main Zone: “Mi día”, acciones rápidas y apps esenciales.
- Entertainment Zone: apps no esenciales, accesibles mediante swipe.
- Descubrimiento de apps con PackageManager.
- Clasificación inicial por package name.
- Apertura con Intent.
- Interfaz basada en texto y controles simples, sin iconos llamativos.

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

## Out Of Scope For MVP

- Backend y sincronización.
- Persistencia avanzada.
- Bloqueo de aplicaciones.
- Analítica de uso.
- Configuración compleja.

## Validation

La primera validación será manual en un dispositivo Android o emulador:

1. Compilar e instalar.
2. Seleccionar Minimal Launcher como aplicación Home.
3. Verificar que “Mi día” aparezca primero.
4. Confirmar clasificación y apertura de apps.
5. Usarlo durante varios días y registrar fricciones.

## Next Action

Crear el proyecto Android Compose y ejecutar únicamente la Phase 2 de ROADMAP.md.
