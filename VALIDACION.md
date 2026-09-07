# Validación de Sublaky 1.1.0 — Edición nocturna

## Revisión 1.1

Resultado: BUILD SUCCESSFUL, 17 pruebas aprobadas (0 fallos/errores) y Lint con 0 errores, 17 avisos. Firma APK v2 verificada e idéntica a la edición 1.0.0. VersionCode 4, versión 1.1.0-debug. El manifiesto final no añade INTERNET ni RECORD_AUDIO.

- Compilación Android, pruebas JVM, Lint y generación de vistas Compose.
- 17 pruebas: biblioteca (6), FFT (6), SpectrumSink y TeeAudioProcessor (5). Incluyen audio en contrafase, PCM float, silencio, tono y preservación exacta de bytes a la salida del procesador.
- Vistas Compose del reproductor en teléfono, Deck y pantalla completa de tablet, texto al 150 % y barra de modos en teléfono. Las barras de las vistas usan el análisis de una señal sintética de prueba; no se presentan como capturas de reproducción real.
- El emulador sigue sin arrancar en este PC; no se repitieron cambios de drivers ni de BIOS. La integración de audio y UI en Android físico queda pendiente, aunque el procesador PCM se verificó en JVM.
- Probar en tablet: actualización sin desinstalar, barras durante música y silencio, pausa/reanudación, avance de pista, ocultar/abrir la app, rotación, auriculares, carátula y navegación por los cuatro modos.
- Se conserva la firma de desarrollo y el identificador `org.sublaky.player.debug`; APK de distribución personal, no publicación en F-Droid ni una tienda.

## Historial 1.0

## Resultado de esta edición

- `assembleDebug testDebugUnitTest lintDebug updateDebugScreenshotTest`: BUILD SUCCESSFUL.
- Seis pruebas del filtro de biblioteca: 0 fallos, 0 errores y 0 omitidas.
- Android Lint: 0 errores y 17 avisos; no se ocultan ni desactivan verificaciones.
- Cuatro vistas generadas desde los componentes Compose: Walkman en teléfono, Deck en tablet, pantalla completa en tablet y texto ampliado al 150 %. Se inspeccionaron las imágenes y se corrigieron la distribución del indicador de estado y el tamaño de la etiqueta.
- Estas vistas son renders del código de interfaz con datos de ejemplo, no capturas de una app ejecutándose en Android. No prueban permisos, audio, navegación ni extracción de carátulas en un dispositivo.
- Se preparó un emulador Android 30 AOSP. El PC no dispone del controlador de aceleración Android y el emulador terminó con error de acceso a memoria al arrancar sin aceleración, en dos modos gráficos. No se cambiaron BIOS, hipervisor ni controladores del sistema.
- APK verificado con apksigner: firma válida e idéntica a 0.1.1. Identificador `org.sublaky.player.debug`, versionCode 3, versión 1.0.0-debug, minSdk 26 y targetSdk 36. Es una edición de instalación personal con firma de desarrollo; no una publicación en una tienda.
- El APK no solicita INTERNET. Media3 declara ACCESS_NETWORK_STATE.
- El usuario confirmó el funcionamiento de 0.1.0 en tablet y la presencia de aleatorio en 0.1.1. Falta confirmar esta edición 1.0 en la tablet, especialmente carátulas, búsqueda, rotación y controles.

## Historial de configuración y pruebas iniciales

## Entorno encontrado

Windows x64. Git y 7-Zip presentes. Java, Gradle y ADB no estaban disponibles en PATH. No se encontraron Android Studio, JDK ni SDK en las ubicaciones habituales revisadas ni variables JAVA_HOME/ANDROID_HOME configuradas.

Se preparan herramientas portátiles en el área de trabajo: Temurin JDK 17, Android command-line tools, plataforma Android 36, Build Tools 35.0.0, Platform Tools y Gradle 8.13. Descargas de Java y Gradle contrastadas con SHA-256 del proveedor; herramientas Android contrastadas con el hash de la distribución. No se instala Android Studio ni un emulador con esta configuración.

## Resultado automático — 6 de septiembre de 2026

- `assembleDebug testDebugUnitTest lintDebug`: BUILD SUCCESSFUL.
- 6 pruebas JVM ejecutadas, 0 fallos, 0 errores, 0 omitidas.
- Android Lint: 0 errores, 15 avisos. Incluyen sugerencias de actualizar versiones y usar extensiones Kotlin, y servicio exportado: este último es intencional para controles multimedia del sistema, con restricción a la propia app y controladores de confianza en onGetSession.
- APK: org.sublaky.player.debug, 0.1.0-debug, minSdk 26, targetSdk 36.
- Firma de desarrollo APK v2 verificada con apksigner. No es una firma de distribución estable.
- Manifiesto empaquetado sin permiso INTERNET. Media3 añade ACCESS_NETWORK_STATE, que no concede acceso a Internet.
- No se ejecutó la app en un teléfono ni emulador; no se ha validado visualmente ni probado audio real.

## Pruebas manuales pendientes en Android

1. Instalar el APK en Android 8–12 y Android 13 o posterior. Denegar permiso y comprobar que se puede seleccionar una carpeta.
2. Autorizar audio, escanear MP3/FLAC/M4A/OGG, probar el filtro WAV y notas de voz.
3. Seleccionar una carpeta, cerrar/abrir, comprobar acceso persistente y olvidar la carpeta.
4. Reproducir, pausar, buscar, cambiar de pista; comprobar archivo corrupto y archivo eliminado.
5. Apagar pantalla, comprobar controles de notificación, auriculares y pérdida de foco ante otra app.
6. Rotar teléfono/tablet, cambiar todos los modos y probar fuentes grandes.
7. Revocar permisos desde Android y comprobar el error visible al escanear.

La compilación y las pruebas JVM no sustituyen estas pruebas de reproducción y permisos en un dispositivo.
