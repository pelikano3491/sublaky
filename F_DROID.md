# Candidatura de Sublaky a F-Droid

## Estado de la preparación

Versión candidata: 1.1.4, versionCode 8, identificador de producción `org.sublaky.player`.

- Compilación local de producción, pruebas JVM y lintRelease completados en Windows con JDK 17, Gradle 8.13 y Android SDK 36.
- APK de producción sin firmar en app/build/outputs/apk/release/app-release-unsigned.apk. No es instalable sin firmar.
- Ficha Fastlane preparada en inglés y español; política de privacidad y procedencia de los gráficos incluidas.
- Árbol de dependencias de producción e inventario de licencias declaradas en POM disponibles en fdroid/. Las declaraciones POM no constituyen una auditoría completa de código y dependencias de compilación.
- El manifiesto final no solicita INTERNET ni RECORD_AUDIO. ACCESS_NETWORK_STATE procede de las dependencias multimedia.
- Plantilla de metadatos incluida con marcadores explícitos. NO está lista para enviarse sin completar y validar.

## Pendiente antes del envío

1. Publicar y revisar el repositorio público https://github.com/pelikano3491/sublaky con este código, la licencia y los recursos. Excluir claves, cachés, APK y archivos locales.
2. Confirmar el nombre público del autor. Crear y verificar la etiqueta `v1.1.4` sobre el commit publicado.
3. Validar fdroid/org.sublaky.player.yml.template y usarla como metadata/org.sublaky.player.yml dentro de fdroiddata.
4. Añadir un icono PNG de catálogo y capturas actuales tomadas en dispositivo. Las imágenes de screenshotTest son vistas de desarrollo y no prueban gestos ni audio en hardware.
5. Revisar todas las dependencias y ejecutar fdroid lint, el escáner y fdroid build en Linux o la CI de F-Droid. No se ha realizado esa validación en este PC.
6. Decidir la firma pública antes de distribuir APK de producción. No se ha creado una clave privada ni demostrado compilación reproducible. El envío puede utilizar firma de F-Droid; conservar una firma propia requiere configurar y validar compilaciones reproducibles.
7. Enviar la solicitud o merge request y atender la revisión. Aún no hay solicitud enviada ni aprobación.

## Instalaciones existentes

Las APK de prueba usan `org.sublaky.player.debug`. La versión de producción usa `org.sublaky.player`; se instala como otra aplicación y no hereda automáticamente las preferencias ni los permisos de carpetas de la versión de prueba. Los archivos musicales permanecen en el dispositivo.

## Compilación

En un entorno con JDK 17 y Android SDK 36:

```sh
sh gradlew assembleRelease testReleaseUnitTest lintRelease
```

No se requiere Android Studio ni una clave para compilar la APK sin firmar. La comprobación local con caché no sustituye una compilación independiente en la infraestructura de F-Droid.

Referencias oficiales:
- https://f-droid.org/docs/Submitting_to_F-Droid_Quick_Start_Guide/
- https://f-droid.org/docs/Inclusion_Policy/
- https://f-droid.org/docs/Reproducible_Builds/
