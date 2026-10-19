# Research

Project: Minimal Launcher  
Aspect: Personal  
Default mode: New Project  
Status: Initial technical notes  
Date of last review: 2026-10-16

## Research Objective

Definir una implementación Android pequeña que permita validar la idea sin backend, cuentas ni funcionalidades que generen nuevas distracciones.

## Research Questions

- ¿Cómo se registra una Activity como launcher Home?
- ¿Cómo se consultan las apps lanzables instaladas?
- ¿Cómo se abre una app desde su package name?
- ¿Qué limitaciones tiene QUERY_ALL_PACKAGES?
- ¿Qué estructura Compose permite separar las dos zonas?

## Sources

| Source | Date | Relevance | Notes |
| --- | --- | --- | --- |
| Requisitos del propietario del proyecto | 2026-10-16 | Product direction | Define las dos zonas, acciones rápidas y uso de Compose. |
| Android SDK APIs | TBD | Technical implementation | Verificar durante Phase 2 contra documentación oficial. |
| Android package visibility documentation | TBD | Platform policy | Confirmar alcance y distribución de QUERY_ALL_PACKAGES. |

## Findings

### Facts

- Una Activity puede declararse como candidata a Home mediante ACTION_MAIN, CATEGORY_HOME y CATEGORY_DEFAULT.
- Las apps lanzables pueden consultarse con ACTION_MAIN y CATEGORY_LAUNCHER.
- Una aplicación puede abrirse usando el launch intent asociado a su package name.
- QUERY_ALL_PACKAGES puede estar sujeto a políticas de distribución en Google Play.

### Inferences

- Para uso personal, una clasificación explícita por package name es suficiente inicialmente.
- Un HorizontalPager encaja con la separación entre “Mi día” y “Entretenimiento”.
- El estado de apps debe vivir fuera de los composables.

### Recommendations

- Empezar con una lista neutral basada en texto.
- Mantener descubrimiento y apertura de apps en un repositorio.
- Usar un ViewModel para exponer el estado a Compose.
- No implementar persistencia ni bloqueo hasta observar el uso real.

## Options Compared

| Option | Summary | Pros | Cons | Evidence |
| --- | --- | --- | --- | --- |
| Jetpack Compose | UI declarativa completa | Rápida de iterar; buen soporte para pager y estado | Requiere controlar versiones | Requisito del proyecto |
| XML Views | UI tradicional | Tecnología madura | Más código para estado y navegación | No elegida |
| Clasificación fija | Package names en código | Pequeña y predecible | No editable por usuario | Decisión aceptada |
| Clasificación editable | Ajustes persistentes | Más flexible | Amplía el alcance | Futura |

## Technical Implications

- Kotlin + Jetpack Compose: stack base del MVP.
- Architecture implications: Activity -> ViewModel -> Repository -> PackageManager.
- Integration implications: integración directa con Android; sin backend.
- Testing implications: compilación, instalación como Home y apertura manual.
- Maintenance implications: mantener package names y dependencias actualizados.

## Risks

- La política de Google Play puede limitar QUERY_ALL_PACKAGES.
- El comportamiento de Home puede variar por fabricante Android.
- Las acciones rápidas no tendrán valor completo hasta definir sus destinos.
- Una UI demasiado minimalista puede dificultar descubrir la segunda zona.

## Recommendations

Recommended direction:

- Construir primero el shell Compose con dos páginas, clasificación fija y apertura de apps.

Confidence:

- Media para el enfoque técnico; la hipótesis de reducción de distracciones debe validarse con uso real.

## Contextual Launcher Notes (2026-10-19)

Technical notes for the time-based spaces, friction and notes scope:

| Need | Candidate API | Notes |
| --- | --- | --- |
| Espacio según la hora | `LocalTime` + recomposición periódica o `ACTION_TIME_TICK` | Recalcular al volver a Home y al cambiar de minuto. |
| Próxima alarma | `AlarmManager.getNextAlarmClock()` | No requiere permisos especiales. |
| Fricción dentro del launcher | Pantalla Compose propia antes de lanzar el Intent | Solo cubre aperturas desde el launcher. |
| Interceptar aperturas externas | `AccessibilityService` o `UsageStatsManager` | Requiere permisos sensibles; One UI puede matar el servicio. Opcional y futuro. |
| Notas y configuración | DataStore (simple) o Room (consultas) | Room si las notas se migrarán a módulos de finanzas y dieta. |
| Recordatorios | `AlarmManager` exacto o `WorkManager` + `POST_NOTIFICATIONS` | Las alarmas exactas requieren `SCHEDULE_EXACT_ALARM` en Android 12+. |
| Agenda | `CalendarContract` + `READ_CALENDAR` | Futuro; decidir si se usa el calendario del sistema. |

## Unresolved Questions

- Confirmar la versión mínima y objetivo de Android.
- Confirmar si la distribución será exclusivamente sideload o también Google Play.
- Definir la acción concreta detrás de cada widget diario.
- Definir cómo persistir las categorías si la configuración fija resulta insuficiente.
