# Notas de la versión 2.3.0

**Producto:** **Consumo CIJ** — calculadora de consumo de tinta y solvente para impresoras de
inyección continua (CIJ) de Videojet, en web (archivo único) y app Android nativa.

**Fecha:** 2026-11-04 — **Ámbito:** app Android, web publicada, sistema de actualizaciones,
herramientas de publicación, documentación y repositorio.

Este documento deja constancia de **cómo se encontró el proyecto**, **qué se iba a hacer**,
**qué se consiguió** y **qué queda pendiente a futuro**. Las entregas anteriores están en
[`NOTAS-2.2.0.md`](docs/NOTAS-2.2.0.md), [`NOTAS-2.1.1.md`](docs/NOTAS-2.1.1.md) y
[`NOTAS-2.1.0.md`](docs/NOTAS-2.1.0.md).

---

## 1 — Cómo se encontró el proyecto (estado a la entrada)

La versión 2.2.0 estaba cerrada y validada: motor de cálculo con los tres modelos oficiales,
seis idiomas, 60 pruebas automáticas y una APK firmada. Al revisar el proyecto con la mirada
puesta en el uso real (una herramienta que se instala en un taller y se queda ahí durante meses)
aparecieron cuatro problemas de fondo:

| Problema | Consecuencia |
|---|---|
| **No había forma de avisar de una versión nueva.** La app era 100 % offline por diseño y el manifiesto no declaraba `INTERNET`; la única vía era que el usuario entrara a GitHub a mirar. | Los equipos se quedaban en la versión instalada el primer día, con fallos ya corregidos y sin saberlo. |
| **La versión estaba escrita a mano en cinco sitios** (`build.gradle.kts`, `strings.xml`, dos textos de Compose, `ReportIssue`, la web y el `version.json`). | Ya había quedado desfasada: `strings.xml` seguía diciendo «Versión 2.0.1» y la tarjeta de novedades decía 2.2.0 con el código 7. |
| **Restaurar una entrada del historial no funcionaba en la misma sesión.** El identificador se generaba con la marca de tiempo al guardar, pero al leerlo se recalculaba con un hash del prefijo y la fecha. | El botón «cargar» de un cálculo recién guardado no hacía nada hasta reiniciar la app. |
| **La publicación dependía de pasos manuales** (compilar, renombrar, calcular el tamaño, subir la APK, escribir la release y actualizar la documentación). | Un error de transcripción en el `versionCode` o en la huella dejaba el aviso de actualización inservible. |

Además, la web traducía la interfaz pero **cinco mensajes emergentes seguían en español**
(«Informe TXT descargado», «Resumen copiado…», etc.) y la fecha del historial usaba siempre
`es-ES`.

### Lo que ya estaba bien y no se tocó

- Motor de cálculo, datos de tintas, cartuchos y mantenimiento.
- Los seis idiomas y el catálogo único `i18n/catalog.json`.
- Las 43 pruebas de la web y las 17 de Kotlin.
- El diseño adaptable, los temas (claro, oscuro, AMOLED) y la exportación CSV/TXT.

---

## 2 — Qué se iba a hacer (plan de la 2.3.0)

1. **Aviso de actualización de la APK**: comprobación al abrir la app y una vez al día en segundo
   plano, aviso dentro de la interfaz, notificación opcional, descarga del APK con verificación
   de huella e instalación con el instalador del sistema.
2. **Una sola fuente de verdad para la versión**: `BuildConfig` generado desde
   `build.gradle.kts`, y `version.json` validado contra él.
3. **Corregir el historial** y las fechas en el idioma activo.
4. **Cerrar los textos sin traducir** de la web y añadir el aviso de versión nueva en la copia
   publicada (nunca en el archivo offline abierto con doble clic).
5. **Automatizar la publicación**: comprobar la coherencia antes de publicar y un comando que
   compile, calcule la huella, actualice `version.json` y cree la release con la APK.
6. **Mantener la privacidad**: la app sigue calculando sin internet; la comprobación de versión es
   la única conexión, es anónima y se puede desactivar.

---

## 3 — Qué se consiguió (la 2.3.0)

### 3.1 Sistema de actualizaciones (Android)

- **`UpdateChecker`**: descarga `version.json` por HTTPS desde el repositorio y, si falla, desde
  GitHub Pages. Compara `versionCode` con el instalado. Sin librerías externas
  (`HttpURLConnection`), con tiempos de espera y sin enviar ningún dato.
- **`UpdateScheduler` + `UpdateJobService`**: comprobación diaria con `JobScheduler` (API del
  sistema, sin WorkManager), persistente tras reiniciar y condicionada a que el usuario active
  los avisos.
- **`UpdateNotifier`**: canal «Actualizaciones», notificación de versión nueva, de descarga lista
  y de error. Si el permiso `POST_NOTIFICATIONS` no está concedido, el aviso sigue apareciendo
  dentro de la app.
- **`UpdateDownloader` + `UpdateDownloadReceiver`**: `DownloadManager` descarga el APK en el
  almacenamiento privado de la app; al terminar se comprueba el **SHA-256** publicado. Si no
  coincide, el archivo se borra y no se ofrece instalar.
- **`UpdateInstaller`**: instala con `FileProvider` y el instalador de Android. La actualización se
  aplica encima de la versión actual: los datos, el historial y el tema se conservan.
- **Interfaz**: aviso bajo la cabecera, diálogo con versión, fecha, tamaño y estado de la descarga,
  botones «Descargar e instalar», «Instalar», «Más tarde» y «Omitir esta versión», y tarjeta
  «Actualizaciones» con la versión instalada, la comprobación automática, el interruptor de avisos
  y «Buscar actualizaciones».
- **Primer arranque**: se pregunta una sola vez si se quieren recibir notificaciones. Si se
  rechazan, la app no insiste y el aviso interno sigue funcionando.
- **Instalación**: Android pide una vez permiso para instalar apps desconocidas y puede mostrar el
  aviso de Play Protect (normal fuera de la tienda). La actualización se aplica encima y conserva
  datos, historial y tema.

Permisos nuevos: `INTERNET`, `ACCESS_NETWORK_STATE`, `POST_NOTIFICATIONS`,
`REQUEST_INSTALL_PACKAGES` y `RECEIVE_BOOT_COMPLETED`. Ya no se puede decir «sin permisos de red»:
la documentación ahora explica que **los cálculos son locales y la única red es la comprobación de
versión**, que el usuario puede desactivar.

### 3.2 Versión y publicación

- `versionCode 7 → 8`, `versionName 2.2.0 → 2.3.0`.
- `BuildConfig.BUILD_DATE`, `UPDATE_URL`, `UPDATE_URL_FALLBACK`, `NOTES_URL` y `RELEASES_URL`
  generados desde Gradle. La app ya no repite la versión en el código.
- **`tools/check_update.py`**: comprueba que `version.json` (raíz y `docs/`) coincide con
  `versionCode`, `versionName`, `BUILD_DATE`, el nombre del APK, la URL de la release, las notas,
  los idiomas y el número de cadenas del catálogo; además, que la app solo use HTTPS de los
  endpoints del proyecto.
- **`tools/release.py`**: compila la APK firmada, copia el archivo a `dist/`, calcula tamaño y
  SHA-256, actualiza los dos `version.json`, crea la release de GitHub y sube la APK. Si hay `git`,
  confirma y sube el manifiesto; si no, lo hace por la API.
- `tools/validar.py` pasa a cinco pasos (notas, catálogo/web, motor de la web, motor de Kotlin y
  versión publicada).
- La CI deja de exigir «sin INTERNET» y pasa a validar el sistema de actualizaciones y sus
  endpoints.

### 3.3 Correcciones

- **Historial**: cada entrada guarda su identificador en las preferencias; restaurar funciona en la
  misma sesión y después de reiniciar. El guardado limpia las claves sobrantes al borrar entradas.
- **Fechas**: el historial usa el idioma activo de la app, no el del sistema.
- **Textos sin traducir en la web**: los cinco mensajes emergentes que quedaban en español ya usan
  el catálogo; la fecha del historial también respeta el idioma.
- **Versión visible**: la tarjeta de novedades y las notas técnicas leen `BuildConfig`; ya no hay
  cifras escritas a mano que puedan quedar desfasadas.
- **`strings.xml`**: el texto legal decía «Versión 2.0.1»; ahora describe el comportamiento real
  (cálculos sin internet y comprobación de versión desactivable).

### 3.4 Web publicada

- Constante `WEB_VERSION` y aviso flotante cuando `version.json` publica una versión más nueva:
  «Recargar» y «Descargar APK».
- El aviso **solo se activa en la copia publicada** (`http`/`https`); el archivo offline abierto con
  doble clic sigue sin hacer ninguna petición de red.

### 3.5 Idiomas

- Catálogo: **466 cadenas × 5 idiomas** (antes 413). Se añadieron las 50 cadenas del sistema de
  actualizaciones, los avisos de la web y los mensajes emergentes que faltaban.
- Se retiraron las cadenas de la versión 2.2.0 que quedaron obsoletas.
- `tools/check_i18n.py` comprueba ahora que **todo texto visible de la app** (`.tr()`, `I18n.t`,
  `I18n.tf`) exista en el catálogo: un texto nuevo sin traducir rompe la validación.

### 3.6 Documentación

- README, CHANGELOG, ROADMAP, VERSIONES, portada y notas públicas actualizados a 2.3.0.
- Se documenta el flujo de publicación (`tools/release.py`) y la política de red.
- `version.json` incorpora `apkUrl`, `apkSizeBytes`, `apkSha256`, `notesUrl`, `mandatory` y
  `updateCheck`.

---

## 4 — Qué queda pendiente a futuro

- **Firma de distribución**: el almacén `android/keystore/consumocij.jks` está en el `.gitignore`
  (correcto), pero no hay copia de seguridad documentada. Si se pierde, ninguna actualización
  podrá instalarse encima y habrá que desinstalar y reinstalar. Conviene guardar una copia segura
  y anotar la contraseña fuera del repositorio.
- **Rotación de la clave**: si en algún momento el almacén se ha compartido, hay que asumir que la
  clave es pública y planificar una migración (nueva aplicación o instalación limpia).
- **Notas por idioma en la release**: `version.json` no incluye los puntos destacados traducidos;
  el diálogo muestra versión, fecha y tamaño, y enlaza a las notas públicas.
- **Play Store**: la instalación directa del APK exige permitir «origen desconocido». Si se publica
  en una tienda, el sistema de actualización debe sustituirse por el de la tienda.
- **Actualización incremental**: hoy se descarga la APK completa (~2 MB). Para versiones futuras
  grandes (p. ej. con más datos) convendría valorar APK splits o un delta.
- **Copia de seguridad del historial**: exportar e importar el historial y la configuración desde la
  app (la web ya exporta JSON).
- **Revisión de datos con Videojet**: quedan dudas documentadas (V457 con solvente V725 a 740 ml) y
  la tabla de 70 µm de algunos modelos.

---

## 5 — Cómo comprobar esta entrega

```bash
python tools/validar.py          # notas, catálogo/web, motor web, motor Kotlin y version.json
cd android && gradle testDebugUnitTest assembleRelease
python tools/release.py --build   # APK + huella + version.json (sin publicar)
```

Comprobaciones realizadas el 2026-11-04:

- `tools/validar.py`: **correcta** (43 pruebas de la web y 22 de Kotlin incluidas).
- `tools/check_update.py`: versión 2.3.0 (compilación 8) coherente con la app, las notas y el
  catálogo; solo HTTPS de los endpoints del proyecto.
- APK firmada compilada con JDK 17 y Android SDK 34; instalada y probada en el emulador
  Pixel 7 / API 34.
- Comprobación manual del aviso: con `version.json` en 2.3.0 y una app 2.2.0 (compilación 7) se
  muestra el diálogo, se descarga la APK, se verifica el SHA-256 y se abre el instalador.
- Web: el aviso de versión nueva aparece en GitHub Pages y no aparece al abrir
  `web/calculadora-offline.html` con doble clic.

---

**Consumo CIJ 2.3.0** — compilación **8** · 2026-11-04 · cálculos sin internet · aviso de
actualización opcional y verificable.
