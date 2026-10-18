# Requirements

Project: Minimal Launcher  
Aspect: Personal  
Default mode: New Project  
Status: Defined for MVP  
Last updated: 2026-10-16

## Project Objective

- Crear un launcher Android personal que presente primero hábitos, productividad y apps esenciales, manteniendo el entretenimiento separado.

Target state:

- Al presionar Home aparece “Mi día”.
- El usuario puede navegar a “Entretenimiento” mediante swipe horizontal.
- Las apps instaladas se leen, clasifican y abren mediante tap.

## MVP Scope

Included:

- Jetpack Compose.
- Activity con categorías HOME y DEFAULT.
- Permiso QUERY_ALL_PACKAGES.
- Consulta de apps con PackageManager.
- Estado Compose/ViewModel para separar apps principales y de entretenimiento.
- Lista de texto o botones para apps esenciales.
- Acciones rápidas: Alimentación, Gastos, Ejercicio y Tareas.
- Zona secundaria sin iconos llamativos.

## Excluded Scope

- Persistencia de hábitos y gastos.
- Backend, cuentas o sincronización.
- Analítica de comportamiento.
- Bloqueo de apps.
- Clasificación automática basada en uso.
- Personalización avanzada.

## User Stories Or Use Cases

| ID | User Or Actor | Need | Outcome | Priority |
| --- | --- | --- | --- | --- |
| UC-001 | Adrián | Presionar Home | Ver “Mi día” como pantalla inicial | Must |
| UC-002 | Adrián | Registrar una intención diaria | Tocar una de las cuatro acciones rápidas | Must |
| UC-003 | Adrián | Abrir una app esencial | Encontrarla en una lista de texto y abrirla | Must |
| UC-004 | Adrián | Acceder al entretenimiento deliberadamente | Hacer swipe a la zona secundaria | Must |
| UC-005 | Adrián | Mantener baja la tentación visual | Ver entretenimiento sin iconos llamativos | Must |

## Functional Requirements

| ID | Requirement | Acceptance Signal | Status |
| --- | --- | --- | --- |
| FR-001 | La app debe declarar la categoría Android HOME. | Puede seleccionarse como launcher. | Planned |
| FR-002 | Debe consultar las apps lanzables instaladas. | La lista muestra apps disponibles. | Planned |
| FR-003 | Debe excluirse de su propia lista. | Minimal Launcher no aparece. | Planned |
| FR-004 | El estado debe clasificar apps principales y de entretenimiento. | Cada app aparece en una sola zona. | Planned |
| FR-005 | Un tap debe abrir la app mediante Intent. | La app destino se inicia. | Planned |
| FR-006 | “Mi día” debe ser la página inicial. | El pager comienza en la página principal. | Planned |
| FR-007 | La zona secundaria debe alcanzarse mediante swipe. | El gesto muestra la segunda zona. | Planned |
| FR-008 | Las acciones rápidas deben aparecer en la zona principal. | Se muestran las cuatro acciones. | Planned |

## Non-Functional Requirements

| ID | Requirement | Acceptance Signal | Status |
| --- | --- | --- | --- |
| NFR-001 | La interfaz debe ser minimalista. | No depende de iconos grandes ni tarjetas intensas. | Planned |
| NFR-002 | El MVP debe funcionar sin red. | Las pantallas base funcionan offline. | Planned |
| NFR-003 | La clasificación debe ser fácil de modificar. | Los package names están aislados. | Planned |
| NFR-004 | No debe requerir backend ni servicios externos. | La app funciona localmente. | Planned |

## Acceptance Criteria

- El launcher puede seleccionarse como aplicación Home.
- Al presionar Home, la primera pantalla visible es “Mi día”.
- “Mi día” contiene Alimentación, Gastos, Ejercicio y Tareas.
- Las apps esenciales se muestran como texto o botones limpios.
- La zona secundaria se alcanza mediante swipe horizontal.
- Las apps de entretenimiento se muestran sin iconos llamativos.
- Tocar una app abre su actividad principal.
- La app se excluye de su propia lista.

## Dependencies

| Dependency | Purpose | Status | Risk |
| --- | --- | --- | --- |
| Android SDK | Ejecutar la app y consultar paquetes | Required | Bajo |
| Kotlin | Lenguaje principal | Required | Bajo |
| Jetpack Compose | UI completa | Required | Bajo |
| QUERY_ALL_PACKAGES | Consultar apps instaladas | Planned | Restricciones de distribución |

## Risks

- Google Play puede restringir QUERY_ALL_PACKAGES.
- Algunas apps pueden no proporcionar un launch intent válido.
- Los package names pueden cambiar entre dispositivos.
- Las acciones rápidas podrían quedar como botones sin comportamiento en la primera iteración.

## Validation Method

- Compilación del módulo Android.
- Selección como launcher en un dispositivo o emulador.
- Prueba manual de clasificación y apertura de varias apps.
- Uso cotidiano durante varios días.

## Open Questions

- Definir la acción concreta de cada widget.
- Decidir si la edición de categorías entra en la siguiente fase.
