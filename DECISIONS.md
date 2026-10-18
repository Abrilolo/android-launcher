# Decisions

Project: Minimal Launcher  
Aspect: Personal  
Last updated: 2026-10-16

## 2026-10-16 - MVP orientado a dos zonas

Type: Product / Scope  
Status: Accepted  
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
