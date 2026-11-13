# Sonora

Reproductor de música local para Android, con una experiencia tipo app de streaming.
Basado en [OuterTune](https://github.com/OuterTune/OuterTune) (fork de InnerTune). Licencia **GPL-3.0**.

## Qué cambió respecto a OuterTune-lite

| Área | Cambio |
|---|---|
| Identidad | Nombre "Sonora", paquete `com.sonora.music`, ícono propio (disco coral con ecualizador), versión 1.0.0 |
| Tema | Oscuro por defecto (#121212), acento coral #FF8686, tipografía de títulos más gruesa (`ui/theme/SonoraTheme.kt`) |
| Navegación | Barra inferior Inicio / Buscar / Biblioteca, con degradado y sin "píldora" de selección |
| Inicio | Saludo según la hora, grilla de acceso rápido, carruseles (recientes, vuelve a escuchar, playlists, descubre) |
| Buscar | Nueva pestaña con tarjetas de categorías y géneros de tu biblioteca (`ui/screens/ExploreScreen.kt`) |
| Mini-player | Tarjeta flotante redondeada teñida con el color de la portada |
| Reproductor | Fondo degradado con el color dominante de la portada |
| Mezclas | Mezcla del día, Mezcla de favoritas y Redescubre; botón flotante de aleatorio |
| Resumen Sonora | Minutos escuchados, canciones top, artistas top y álbum del año (`ui/screens/RecapScreen.kt`), calculado en el dispositivo |

Todos los textos nuevos están en `res/values/strings-sonora.xml` (inglés) y `res/values-es/strings-sonora.xml` (español).

## Cambiar nombre, paquete o color

- **Nombre:** `app/src/main/res/values/app_name.xml`
- **Paquete:** `applicationId` en `app/build.gradle.kts` **y** `android:targetPackage` en `app/src/main/res/xml/shortcuts.xml`
- **Color de acento:** `SonoraColors.Accent` en `ui/theme/SonoraTheme.kt` y `DefaultThemeColor` en `ui/theme/Theme.kt`
- **Ícono:** `res/drawable/launcher_foreground.xml`, `launcher_monochrome.xml`, `small_icon.xml` y `res/values/ic_launcher_background.xml`

## Compilar con GitHub Actions

1. Crea un repositorio en GitHub (puede ser privado) y sube esta carpeta a la rama `main`.
2. El workflow `.github/workflows/sonora.yml` se ejecuta en cada push a `main` (o manualmente en **Actions > Sonora - compilar APK > Run workflow**).
3. Al terminar, descarga el APK desde **Actions > (ejecución) > Artifacts > Sonora-debug-apk**.

Los workflows originales de OuterTune (`build.yml`, `build_ot_*`) siguen ahí; puedes borrarlos si no los usas.

## Notas legales

- GPL-3.0: si distribuyes la app, debes publicar el código fuente con la misma licencia y mantener los créditos de OuterTune/InnerTune.
- No uses nombres, logos ni íconos de Spotify u otras marcas.
- Respaldo del código original: `_respaldo_original_app.tar.gz`.
