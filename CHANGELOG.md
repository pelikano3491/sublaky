# 1.1.4 — Distribución adaptable

- Revisión visual con Compose en tamaños de teléfono y tablet, en vertical y horizontal.
- Reproductor compacto cuando falta altura, con controles y espectro visibles. Walkman usa columnas en horizontal cuando hay ancho suficiente.
- Búsqueda fija y canciones con desplazamiento independiente. Acciones de carpetas, permiso y escaneo agrupadas en «Fuentes».
- Cabecera y navegación reducidas en horizontal. Pantalla completa dispone cinta e información lado a lado en horizontal.
- Mantiene MP3, aleatorio y reproducción. VersionCode 8. La revisión de capturas no sustituye la prueba de gestos y audio en dispositivo.

# 1.1.3 — Búsqueda fija

- La búsqueda y las acciones de biblioteca quedan fuera del desplazamiento de canciones en Walkman, Deck y Escritorio.
- Walkman y las pantallas estrechas reservan zonas independientes para reproductor y biblioteca. Desplazar canciones no desplaza el reproductor; si este no cabe, su zona admite desplazamiento propio.
- Conserva las columnas laterales en Deck y Escritorio, los modos, el visualizador y Solo MP3. VersionCode 7.

# 1.1.2 — Deck con biblioteca lateral

- Deck muestra el reproductor a la izquierda y la biblioteca a la derecha desde 600 dp de ancho (tablets y teléfonos en horizontal con espacio suficiente).
- Cada columna tiene su propio desplazamiento: recorrer canciones no mueve el reproductor. En pantallas estrechas se mantiene la distribución vertical.
- Conserva Solo MP3, aleatorio, visualizador y modos. VersionCode 6.

# 1.1.1 — Filtro MP3

- Ajustes incluye «Solo MP3», activado por defecto y guardado entre sesiones.
- Se aplica a la biblioteca del dispositivo y a las carpetas añadidas. Cambiarlo vuelve a escanear sin borrar archivos.
- Al desactivarlo se recuperan los otros formatos compatibles. La opción WAV queda deshabilitada mientras Solo MP3 está activo.
- Conserva la estética, el visualizador, los modos y el aleatorio. VersionCode 5 para actualizar la instalación anterior.

# 1.1.0 — Edición nocturna

- Aproximación a las referencias: carcasa negra con grano, marcos metálicos, bobinas en relieve, controles oscuros con luz naranja y carátula en la etiqueta completa.
- Visualizador conectado a audio PCM: 32 bandas FFT, sin micrófono y sin modificar la señal de reproducción. Desactiva el análisis sin vista activa.
- Los cuatro modos quedan en una barra inferior. El modo automático se elige en Ajustes.
- Mantiene aleatorio, búsqueda, permisos y carpetas. Misma identidad de app y versionCode 4.
- Diecisiete pruebas JVM: seis de biblioteca, seis de análisis espectral y cinco de integración PCM/TeeAudioProcessor.

# 1.0.0 — Edición cinta

- Nueva estética verde oscuro, crema, cobre y aluminio cálido. Cassette con ventana, cinta, tornillos, bobinas suaves y etiqueta de la canción.
- Walkman de ancho contenido; Deck con pantalla lateral cuando hay espacio; Escritorio con biblioteca al lado; modo inmersivo sin hueco superior artificial.
- Carátulas embebidas, búsqueda de música, duración por pista y resaltado de la canción activa.
- Controles con símbolos dibujados y etiquetas; búsqueda temporal al soltar la barra.
- Mantiene aleatorio, carpetas y permisos. Mismo identificador de app y versionCode superior para actualizar la instalación existente.

# 0.1.1

Verificación: compilación y Android Lint correctos; seis pruebas existentes del filtro aprobadas. Firma verificada e idéntica a 0.1.0, para actualizar la instalación. El modo aleatorio aún requiere prueba funcional en tablet.

- Botón Aleatorio en todos los modos visuales, con estado seleccionado accesible.
- Media3 gestiona el orden aleatorio de la cola y los controles anterior/siguiente. Activarlo no interrumpe la canción actual ni cambia el orden visible de la biblioteca.
- La preferencia se conserva al reiniciar el servicio. No activa repetición automática.
- Versión 0.1.0: el usuario confirmó funcionamiento en tablet; la estética sigue siendo provisional.

Pendiente en tablet: activar/desactivar mientras suena una pista, avanzar por una cola de varias canciones, comprobar reproducción automática y restauración de la preferencia tras cerrar la app.
