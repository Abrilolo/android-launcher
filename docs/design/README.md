# Diseño · referencias visuales

Cómo debe verse el launcher. El concepto se hizo en el lienzo de diseño
[Launcher Nothing concepto](https://claude.ai/artifact/Pf7fkEaVp9AFFNegVU7XkQ) (privado) y aquí queda una copia
para trabajar sin depender de él.

- `concept/png/`: cada pantalla del concepto como imagen (390 px de ancho, a 2×).
- `concept/*.dc.html`: la misma pantalla en HTML. Se abre con doble clic en el navegador, los botones funcionan y los
  enlaces llevan de una pantalla a otra (el inicio es `Main.dc.html`). `concept/support.js` es lo que las hace
  funcionar sin el lienzo.
- `app/`: capturas de la app real en el Samsung Galaxy S24+, para comparar con el concepto.

## Sistema visual

- Fondo negro puro. Texto blanco; secundario en gris `#9A9A9A`.
- Un solo acento, rojo `#D71921`, y solo para "activo", "siguiente" o "atención". Nunca decorativo.
- Números en matriz de puntos. Etiquetas en mayúsculas espaciadas. Texto corrido en sans.
  En la app: `DotMatrixText` para los números y `labelSmall` para las etiquetas.
- Tarjetas `#0B0B0B` con borde `#222` y esquinas de 22–26 dp.
- Cada widget responde una pregunta sin abrirse (¿cuánto gasté?, ¿cuántas comidas llevo?, ¿qué sigue?). Tocarlo abre
  su página; Atrás regresa al inicio.
- Sin emojis ni íconos de colores. Íconos de línea o de puntos.
- Bancos nunca muestra apps ni saldos hasta pasar huella o PIN, y se vuelve a bloquear solo.

## Pantallas

| Pantalla | Concepto | En la app | Estado |
| --- | --- | --- | --- |
| Inicio | [png](concept/png/Main.png) · [html](concept/Main.dc.html) | [captura](app/inicio.png) | Hecho |
| Cajón de apps | [png](concept/png/Apps.png) · [html](concept/Apps.dc.html) | [captura](app/cajon-apps.png) | Hecho, con diferencias (abajo) |
| Ejercicio · hoy | [png](concept/png/Ejercicio.png) · [html](concept/Ejercicio.dc.html) | [captura](app/ejercicio-hoy.png) | Hecho |
| Ejercicio · semana | [png](concept/png/EjercicioSemana.png) · [html](concept/EjercicioSemana.dc.html) | [captura](app/ejercicio-semana.png) | Hecho |
| Bancos | [png](concept/png/Bancos.png) · [html](concept/Bancos.dc.html) | — (el diálogo de huella no se puede capturar) | Hecho |
| Dieta | [png](concept/png/Dieta.png) · [html](concept/Dieta.dc.html) | [captura](app/dieta.png) | Hecho |
| Reloj mundial | [png](concept/png/Reloj.png) · [html](concept/Reloj.dc.html) | [captura](app/mundo.png) | Hecho |
| Finanzas | [png](concept/png/Finanzas.png) · [html](concept/Finanzas.dc.html) | — | Hecho; falta captura en el teléfono |
| Notas | [png](concept/png/Notas.png) · [html](concept/Notas.dc.html) | — | Hecho; falta captura en el teléfono |
| Alarmas | [png](concept/png/Alarmas.png) · [html](concept/Alarmas.dc.html) | — | Parcial: muestra la siguiente alarma y abre el Reloj del teléfono |
| Semana (izquierda del inicio) | — (no está en el concepto) | [captura](app/semana.png) | Hecho |
| Respaldo (en Módulos) | — | [captura](app/respaldo.png) | Hecho |
| Módulos (configuración) | [png](concept/png/Modulos.png) · [html](concept/Modulos.dc.html) | [captura](app/modulos.png) | Hecho |

## Diferencias acordadas con el concepto

- **Apps:** el concepto abre el cajón deslizando. En la app es un ícono de 3×3 puntos abajo al centro, y el cajón se
  cierra tocando la barrita de arriba o con Atrás.
- **Cajón:** en vez de pestañas Apps/Todas usa los apartados desplegables (Apps, Entretenimiento, Juegos, Todas) con
  buscador e índice alfabético. Lo que el horario no permite no aparece; solo se encuentra buscando en Todas.
- **Reloj mundial:** se ocultan las ciudades que tienen la misma hora que el teléfono. La primera es Ciudad de México.
- **Orientación:** el launcher se queda en vertical.

## Volver a generar las imágenes

Con Chrome instalado, desde `docs/design/concept/` (Git Bash), una pantalla por comando; Ejercicio mide 1180 px de
alto, Ejercicio semana 1060 y las demás 844:

```bash
"/c/Program Files/Google/Chrome/Application/chrome.exe" --headless=new --hide-scrollbars --force-device-scale-factor=2 --window-size=390,844 --virtual-time-budget=8000 --screenshot="$(cygpath -w "$PWD/png/Main.png")" "file:///$(cygpath -m "$PWD/Main.dc.html")"
```
