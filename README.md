<p align="center"><img src="assets/icon/ondas-icon-rounded-512.png" width="140" alt="Ícono de Ondas"></p>

<h1 align="center">Ondas</h1>

App Android de radios online de todo el mundo. Kotlin + Jetpack Compose + Media3.

- Directorio de emisoras: [radio-browser.info](https://www.radio-browser.info/)
- Descubrir por país, género, idioma, calidad · Favoritas · Recientes
- Temas estéticos y fondo propio desde la galería
- Reproducción en segundo plano con notificación
- Aviso de nuevas versiones (desde GitHub Releases) con "Actualizar" / "Más tarde"

## Compilar
1. Abrir la carpeta en Android Studio (Ladybug o superior) y esperar la sincronización de Gradle.
2. Ejecutar en un dispositivo/emulador con Android 8.0+ (API 26).

APK por línea de comandos: `./gradlew assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`.

El APK "debug" usa `app/debug.keystore` (incluido a propósito) para que todas las versiones tengan la misma firma y se puedan instalar unas sobre otras. Para Play Store hay que crear una clave privada aparte.

## Publicar una versión
1. Subir `versionCode` y `versionName` en `app/build.gradle.kts`.
2. Anotar los cambios en `CHANGELOG.md`.
3. `git tag vX.Y.Z && git push --tags` → GitHub Actions compila y adjunta el APK al Release.

## Notas para desarrollar
- Si cambias columnas de la tabla `stations` (Room), sube la versión de la base y escribe una `Migration`.
- Para sumar un tema: agregar una línea en `Themes.all` (`ui/theme/Theme.kt`).
