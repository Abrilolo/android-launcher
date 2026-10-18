# Minimal Launcher

Project: Minimal Launcher  
Aspect: Personal  
Default mode: New Project  
Status: Active planning  
Last updated: 2026-10-16

## Idea Summary

Raw idea:

- Construir un launcher Android minimalista para reducir distracciones y organizar el día desde la pantalla Home.

Current interpretation:

- La pantalla inicial prioriza hábitos, productividad y aplicaciones esenciales.
- Las aplicaciones de entretenimiento están separadas y se presentan de forma neutral, sin iconos llamativos.
- La primera versión es local, personal y enfocada en dos zonas.

## Problem, Desire, Or Opportunity

- Problem: El launcher convencional mezcla aplicaciones productivas y distractoras.
- Desire: Tener una entrada al teléfono deliberada, limpia y orientada al día actual.
- Opportunity: Validar si una separación visual entre productividad y entretenimiento reduce aperturas impulsivas.
- Evidence or source: Requisito expresado por el propietario del proyecto.

## Target User Or Beneficiary

- Primary beneficiary: Adrián, para uso personal.
- Secondary beneficiary: Ninguno en el MVP.
- User need: Acceder rápidamente a actividades diarias y aplicaciones esenciales.
- Context of use: Al presionar Home y durante el uso cotidiano del teléfono.

## Expected Outcome

- Desired result: Un launcher donde Home abre “Mi día”, las apps se clasifican en dos zonas y cualquier app se puede abrir mediante tap.
- Success signal: El usuario puede iniciar sus acciones principales y abrir apps esenciales sin pasar por una cuadrícula de iconos.
- What should be different after this exists: El teléfono presenta primero una superficie de intención y no una colección visual de aplicaciones.

## Possible Versions

| Version | Description | Use When | Risk |
| --- | --- | --- | --- |
| Small version | Dos zonas, lista de apps, clasificación fija y apertura mediante Intent. | Validar la interacción base. | Clasificación poco flexible. |
| Useful version | Acciones rápidas y clasificación editable. | Convertir el launcher en herramienta diaria. | Mayor alcance. |
| Ambitious version | Estadísticas, rutinas, bloqueos y sugerencias. | Solo después de validar el hábito. | Puede convertirse en otra distracción. |

## MVP Candidate

- Aplicación Jetpack Compose registrada como launcher Home.
- Zona principal “Mi día” como primera pantalla.
- Zona secundaria “Entretenimiento” accesible mediante swipe horizontal.
- Consulta de aplicaciones instaladas mediante PackageManager.
- Clasificación inicial mediante package names configurados en código.
- Apertura de aplicaciones mediante Intent.
- Acciones rápidas visuales para Alimentación, Gastos, Ejercicio y Tareas.

## Out-Of-Scope Ideas

- Sincronización en la nube.
- Cuentas de usuario.
- Bloqueo estricto de aplicaciones.
- Analítica de uso o seguimiento remoto.
- Reemplazo completo de apps de notas, gastos, ejercicio o tareas.
- Personalización visual avanzada.

## Constraints

- Time: Construir primero una versión funcional pequeña.
- Budget: Sin servicios externos ni backend en el MVP.
- Skill or learning constraint: Usar Kotlin y Jetpack Compose.
- Platform constraint: Android; debe poder seleccionarse como launcher Home.
- Privacy or security constraint: Los datos deben permanecer localmente.
- Maintenance constraint: Mantener una arquitectura pequeña.

## Assumptions

| Assumption | Why It Matters | How To Validate |
| --- | --- | --- |
| Una interfaz de texto reduce aperturas impulsivas. | Es la hipótesis central. | Usar el launcher durante varios días. |
| Dos zonas son suficientes. | Define la navegación principal. | Revisar si las apps se encuentran sin frustración. |
| La clasificación fija es suficiente inicialmente. | Evita construir ajustes antes de validar. | Registrar cambios necesarios durante el uso. |

## Open Questions

- ¿Las acciones rápidas abrirán aplicaciones externas o tendrán pantallas propias?
- ¿La clasificación deberá editarse desde la interfaz?
- ¿Debe existir una tercera zona para aplicaciones neutras?

## Next Action

Crear el esqueleto Android Compose y validar la instalación como launcher, manteniendo el alcance limitado al MVP.
