# Changelog

Formato basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y versionado semántico.

## [0.2.0]
### Añadido
- Aviso de actualización dentro de la app (lee los Releases de GitHub): "Actualizar" descarga e instala el APK, "Más tarde" lo pospone 24 h. También hay "Buscar actualizaciones" en Ajustes.
- Colores personalizables con paleta (principal, secundario, fondo, tarjetas) y color de texto automático / claro / oscuro.
- Reproductor rediseñado: carátula con sombra y resplandor, animación de ondas cuando la emisora no tiene logo, desliza la carátula para cambiar de emisora, botones anterior/siguiente (también en la notificación y en el mini-reproductor).
### Corregido
- Texto negro sobre fondos oscuros: ahora todos los textos usan un color de alto contraste con el tema.
- Logos de radios que no cargaban o se veían con pocos píxeles: se usa el favicon del sitio web de la emisora como respaldo, se aceptan logos SVG y se envía User-Agent.

## [0.1.0]
### Añadido
- Descubrir emisoras de todo el mundo (radio-browser.info) con búsqueda y filtros: país, "Mi país", género, idioma, orden y calidad.
- Favoritas y Recientes (guardadas en el dispositivo).
- Reproductor con notificación, controles de bloqueo de pantalla, volumen, temporizador de apagado, título del tema en curso y compartir.
- 9 temas estéticos (incluye Material You en Android 12+) y fondo de pantalla desde la galería con velo ajustable.
- Ícono de la app (adaptativo, con versión monocromática para íconos temáticos de Android 13+).
