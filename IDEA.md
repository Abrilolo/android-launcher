# Minimal Launcher

Project: Minimal Launcher  
Aspect: Personal  
Default mode: New Project  
Status: Active planning  
Last updated: 2026-10-19

## Idea Summary

Raw idea:

- Construir un launcher Android minimalista para reducir distracciones y organizar el día desde la pantalla Home.
- (2026-10-19) Un launcher que durante el día vaya mostrando cosas a favor de la agenda, las finanzas y la dieta; por ejemplo, que en la mañana restrinja ciertas apps para no perder el tiempo en ellas. Sin iconos: pura letra, blanco y negro.

Current interpretation:

- La pantalla inicial cambia según la hora del día mediante **espacios** (Mañana, Trabajo, Noche).
- Cada espacio muestra reloj, alarma, apartados desplegables (agenda, recordatorios, notas) y las apps disponibles en esa franja.
- El entretenimiento no tiene zona propia: sus apps solo están disponibles en ciertos horarios.
- Fuera de su horario, abrir una app es posible pero deliberadamente incómodo (fricción progresiva).
- Finanzas y dieta empiezan como notas; en el futuro serán módulos propios.
- Interfaz solo texto, blanco y negro, sin iconos.

## Reference Sketch

Boceto del propietario (2026-10-19):

    ┌───────────────────────────┐
    │          RELOJ            │
    │         ALARMA            │
    │                           │
    │  Apartado 1          ▸    │
    │  Apartado 2          ▸    │   <- desplegables
    │  Apartado 3          ▸    │
    │                           │
    │  ┌─────────────────────┐  │
    │  │ Nota / recordatorio │  │   <- p. ej. "Desayunar"
    │  └─────────────────────┘  │
    └───────────────────────────┘

Referencias visuales:

- Espacios por contexto al estilo Aviate (Mañana, Trabajo…), sin sus iconos ni colores.
- Lista de apps en texto blanco sobre fondo negro al estilo Olauncher.

## Problem, Desire, Or Opportunity

- Problem: El launcher convencional mezcla aplicaciones productivas y distractoras, y es igual a cualquier hora.
- Desire: Una entrada al teléfono deliberada, limpia y orientada al momento del día.
- Opportunity: Validar si mostrar contenido según la hora y añadir fricción a las apps distractoras reduce aperturas impulsivas.
- Evidence or source: Requisito expresado por el propietario del proyecto.

## Target User Or Beneficiary

- Primary beneficiary: Adrián, para uso personal.
- Secondary beneficiary: Ninguno por ahora.
- User need: Ver lo relevante de cada momento del día y acceder rápido a lo esencial.
- Context of use: Al presionar Home y durante el uso cotidiano del Samsung Galaxy S24+.

## Expected Outcome

- Desired result: Un launcher donde Home abre el espacio del momento, con agenda, recordatorios y las apps permitidas en esa franja.
- Success signal: Menos aperturas impulsivas de apps de entretenimiento por la mañana y en horario de trabajo.
- What should be different after this exists: El teléfono presenta primero lo que toca hacer ahora, no una colección de aplicaciones.

## Possible Versions

| Version | Description | Use When | Risk |
| --- | --- | --- | --- |
| Small version | Dos zonas, lista de apps, clasificación fija y apertura mediante Intent. | Base técnica (ya construida). | Clasificación poco flexible. |
| Useful version | Espacios por horario, apps disponibles por franja, fricción para apps restringidas, notas y recordatorios. | Convertir el launcher en herramienta diaria. | Mayor alcance; necesita persistencia. |
| Ambitious version | Módulos propios de finanzas y dieta, agenda integrada, estadísticas e interceptación de aperturas externas. | Solo después de validar el hábito. | Puede convertirse en otra distracción o en demasiado mantenimiento. |

## Core Concepts

- **Espacio:** configuración activa según la franja horaria (Mañana 06-12, Trabajo 12-18, Noche 18-24).
- **App disponible / restringida:** cada app tiene horarios permitidos; fuera de ellos queda restringida.
- **Fricción:** capas que hacen incómodo abrir una app restringida (oculta de la lista → sección atenuada → pausa con cuenta atrás → escribir un motivo).
- **Apartado:** sección desplegable de texto en el espacio (agenda, recordatorios, notas).
- **Nota:** texto local etiquetado (Finanzas, Dieta, General).
- **Recordatorio:** aviso programado, p. ej. "Desayunar" en el espacio Mañana.

## Out-Of-Scope Ideas

- Sincronización en la nube.
- Cuentas de usuario.
- Bloqueo absoluto e imposible de saltar.
- Analítica remota o seguimiento fuera del dispositivo.
- Iconos, colores o personalización visual avanzada.
- Módulos completos de finanzas y dieta (futuro, ver ROADMAP).

## Constraints

- Time: Construir por fases pequeñas, validando cada una en el teléfono.
- Budget: Sin servicios externos ni backend.
- Skill or learning constraint: Usar Kotlin y Jetpack Compose.
- Platform constraint: Android; debe poder seleccionarse como launcher Home. Dispositivo principal: Samsung Galaxy S24+.
- Privacy or security constraint: Los datos deben permanecer localmente.
- Maintenance constraint: Mantener una arquitectura pequeña.

## Assumptions

| Assumption | Why It Matters | How To Validate |
| --- | --- | --- |
| Una interfaz de texto reduce aperturas impulsivas. | Es la hipótesis central. | Usar el launcher durante varios días. |
| Tres franjas horarias son suficientes. | Define la estructura de espacios. | Revisar si el contenido de cada espacio encaja con el día real. |
| La fricción sin bloqueo absoluto basta para disuadir. | Evita frustración y desinstalación. | Contar cuántas veces se completa la pausa y se abre la app. |
| Las notas bastan inicialmente para finanzas y dieta. | Evita construir módulos antes de validar. | Revisar qué información se repite en las notas. |

## Open Questions

- ¿Qué apps exactas estarán restringidas en cada espacio?
- ¿Qué duración de pausa resulta disuasoria sin ser frustrante?
- ¿Qué apartados aparecen en cada espacio y en qué orden?
- ¿La agenda se leerá del calendario de Android o se gestionará dentro del launcher?
- ¿Cómo se comporta el espacio en fines de semana?

## Next Action

Terminar el pulido visual en blanco y negro (Phase 3) y después implementar los espacios por horario.
