# Changelog

Todos los cambios relevantes de este proyecto.
El formato sigue [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y el versionado
[SemVer](https://semver.org/lang/es/).

## [2.4.1] — 2026-11-05

### Añadido
- **Validación del catálogo con 493 PDF de fichas de tintas** (492 SDS + catálogo): 188 productos
  `V*`, 140 tintas y 36 make-up.
- **V4287 identificada**: el TCO la asigna a la impresora **MI 9029** y su base MPK no tiene factor
  oficial; **no se añade** al catálogo Videojet, que queda confirmado con 139 tintas.
- **`tools/check_fichas.py`** y **`docs/TINTAS.md`** con el informe, la lista de las 27 tintas fuera
  del TCO y la decisión tomada.
- Catálogo de **139 tintas Videojet** confirmado contra el TCO y las SDS.

### Cambiado
- **Gramática en español**: «El consumo de make-up **lo influyen**» → «**está influido por**»;
  «ciclo de **medimiento**» → «ciclo de **medición**».
- **Italiano**: «ciclo di **misura**» → «ciclo di **misurazione**»; «1 L **o** 5 L» → «1 L **né**
  5 L».
- **Chino**: «condensador activo/pasivo» → «主动/被动冷凝器».
- `versionName 2.4.1` · `versionCode 10`.
- Pruebas: **52 web + 33 Android** = 85 (más 12.800 valores de solvente).
- README, CHANGELOG, ROADMAP, VERSIONES, DATOS, portada y notas públicas sincronizados.

### Pendiente
- Las 27 tintas nuevas de las SDS necesitan la matriz modelo/contenedor de un TCO actualizado.
- Verificador de las 535 fichas de velocidad y las 4 diferencias con `CIJ Speeds.xlsx`.
- Revisión nativa de chino y portugués de Portugal; V457 con V725.

## [2.4.0] — 2026-11-04

### Añadido
- **Validación de solvente con 800 fichas oficiales** (12.800 valores de 10 a 40 °C) en la web y en
  Android, con el fixture `tools/fixtures/solvent_cards.json` y `tools/test_solvent.js`.
- **Novedades en el aviso de actualización**: `highlights` en los seis idiomas, visibles en el
  diálogo antes de actualizar.
- **Recordatorio a los 14 días** para una versión omitida y soporte de `mandatory`.
- **Copia de seguridad JSON**: exportar e importar configuración, historial, tema e idioma desde la
  app, con el selector de archivos del sistema.
- Aviso en la app y en la web: «Este modelo usa exclusivamente cartuchos de 750 ml».

### Cambiado
- **Cartuchos filtrados por modelo**: la lista «750mL Only» del TCO v35 (VJ 1210, 1220, 1240, 1280,
  1510 y 1580) ya no ofrece 1 L ni 5 L, aunque la tinta tenga esos tamaños.
- Las configuraciones guardadas con tamaños no válidos se corrigen al arrancar.
- `versionName 2.4.0` · `versionCode 9`.
- Catálogo: **479 cadenas** × 5 idiomas (antes 466).
- Pruebas: **51 web + 32 Android** = 83 (además de los 12.800 valores de solvente).
- `tools/validar.py` incorpora la validación de solvente; `check_update.py` valida `highlights`.

### Corregido
- Equipos que mostraban cartuchos de 5000 ml sin usarlos (VJ 1510, 1220, 1240, 1280, 1580…) y
  equipos 750-only que mostraban 1000 ml.
- El aviso decía «toque para ver las novedades» y no mostraba ninguna: ahora las muestra.
- Una versión omitida ya no queda silenciada para siempre: se recuerda a los 14 días.

### Pendiente
- Verificador de las 535 fichas de velocidad y las 4 diferencias detectadas con `CIJ Speeds.xlsx`.
- Revisar las tintas V42xx asignadas a la Serie 1000VJ en el TCO.
- V457 con V725 (740 ml) y revisión nativa de chino/portugués de Portugal.

## [2.3.0] — 2026-11-04

### Añadido
- **Aviso de actualización de la APK**: comprobación al arrancar y una vez al día en segundo plano
  (`JobScheduler`), aviso dentro de la app y notificación local opcional.
- **Descarga verificada**: `DownloadManager`, comprobación **SHA-256** del APK publicado e
  instalación con `FileProvider` y el instalador del sistema.
- **Pantalla «Actualizaciones»**: versión instalada, comprobación automática, interruptor de avisos,
  «Buscar actualizaciones» y estado de la última comprobación.
- **`tools/check_update.py`**: `version.json` debe coincidir con `versionCode`, `versionName`,
  `BUILD_DATE`, el APK, las notas, los idiomas y el catálogo; además, solo se permite HTTPS de los
  endpoints del proyecto.
- **`tools/release.py`**: compila la APK firmada, calcula tamaño y SHA-256, actualiza `version.json`
  y crea la release con el APK adjunto.
- **Aviso de versión nueva en la web publicada**, con «Recargar» y «Descargar APK»; el archivo
  offline abierto con doble clic sigue sin hacer peticiones.
- Datos de versión en `BuildConfig` (`BUILD_DATE`, `UPDATE_URL`, `UPDATE_URL_FALLBACK`, `NOTES_URL`,
  `RELEASES_URL`).
- `version.json` incorpora `apkUrl`, `apkSizeBytes`, `apkSha256`, `notesUrl`, `mandatory` y
  `updateCheck`.

### Cambiado
- `versionName 2.3.0` · `versionCode 8` (antes 2.2.0 · 7).
- La app ya no dice «100 % offline» sin matices: los cálculos son locales y la única red es la
  comprobación de versión, desactivable.
- Catálogo: **466 cadenas** × 5 idiomas traducidos (antes 413). Nuevas cadenas de actualizaciones,
  del aviso web y de los mensajes emergentes que faltaban.
- `tools/validar.py` pasa a cinco pasos (se añade la comprobación de versión publicada).
- CI: `validar.yml` valida el manifiesto de versión y la red permitida; `android.yml` deja de exigir
  «sin INTERNET» y comprueba el sistema de actualizaciones.

### Corregido
- **Historial**: restaurar una entrada guardada en la misma sesión no hacía nada porque el
  identificador se recalculaba al leerlo. Ahora se guarda en las preferencias y se limpian las
  claves sobrantes al borrar entradas.
- **Fechas del historial** en el idioma activo de la app (antes siempre `es-ES`).
- **Cinco mensajes emergentes de la web** seguían en español («Informe TXT descargado»,
  «Informe CSV descargado», «Resumen copiado…», «No se pudo copiar» y los del tema).
- **`strings.xml`** decía «Versión 2.0.1»; ahora describe el comportamiento real.
- La tarjeta de novedades y las notas técnicas leen `BuildConfig` en lugar de cifras escritas a mano.

## [2.2.0] — 2026-10-22

### Añadido
- **Selector de idioma dentro de la app Android**: seis idiomas + «Idioma del sistema», con memoria
  de la elección (`SharedPreferences`) y recreación de la actividad al cambiar.
- **Pruebas automáticas del motor de cálculo**: 43 sobre el motor de la web
  (`tools/test_engine.js`) y 17 sobre el de Android (`CalcEngineTest.kt`), con los valores de
  referencia de los modelos oficiales de Videojet.
- **`tools/gen_i18n.py`**: genera los `assets/i18n/*.json` de Android, el bloque `i18n-data` de la
  web y `docs/calculadora.html` desde `i18n/catalog.json`. Las copias ya no se editan a mano.
- **`tools/validar.py`**: un solo comando para toda la validación del proyecto.
- Comprobaciones nuevas en `tools/check_i18n.py`: párrafos traducibles sin clave, marcadores de
  negrita `**…**` desequilibrados, artefactos generados desactualizados y claves sin uso.
- Las pruebas unitarias de Kotlin se ejecutan en la CI antes de compilar la APK.
- **Notas de versión permanentes**: `docs/notas-indice.json` registra cada versión con su huella
  SHA-256 y `tools/check_notas.py` (paso 1 de `tools/validar.py` y de la CI) **falla** si una nota
  desaparece, si sus dos copias no son idénticas, si una nota publicada se reescribe o si le faltan
  los bloques antes / plan / hecho / pendiente. Revisa además el historial de Git para denunciar
  eliminaciones. Nuevos modos `--new X.Y.Z` y `--freeze`.
- Copia en la raíz de `NOTAS-2.1.0.md` (antes solo estaba en `docs/`), para que raíz y `docs/`
  contengan siempre las mismas notas.
- `validar.yml` hace *checkout* del historial completo y ejecuta la comprobación de notas.

### Cambiado
- **Cabeceras y etiquetas del informe TXT traducidas** en los seis idiomas, en la web y en la app,
  con las etiquetas alineadas automáticamente por idioma. El CSV traduce también las unidades.
- **Interfaz de Android completamente traducida**: las etiquetas de los campos, las cajas
  informativas, los avisos, los rótulos de los gráficos y el detalle del informe se escribían en
  español literal y no pasaban por `I18n`; ahora usan las mismas plantillas traducidas que la web.
- **Informe técnico y de error traducidos** (`techInfo` / `reportText` en web, `ReportIssue` en
  Android), incluido el `versionCode`.
- **Negrita y formato conservados** en los párrafos traducidos de la web: los tramos resaltados se
  marcan con `**…**` en el catálogo y se reconstruye la etiqueta HTML original (`<b>`, `<em>`, `<code>`).
- Los números y fechas de Android (`Fmt`) usan el idioma activo de la app y no el del sistema.
- Catálogo de idiomas: **413 cadenas** × 6 idiomas (antes 364).
- `versionName 2.2.0` → `versionCode 7` (antes 2.1.1 → 6).
- `tools/validar.py` pasa a tener cuatro pasos: notas de versión, catálogo/web, motor de la web y
  motor de Kotlin.
- Portada, notas públicas, README, VERSIONES y ROADMAP sincronizados con el estado real.
- Workflows de CI (`validar.yml` y `android.yml`) ampliados y limpiados.

### Corregido
- Dos párrafos del pie de la web se quedaban a medias: `Basado en CIJ Makeup Usage Model v14…` y
  `Basado en CIJ TCO Calculator v35:` no estaban en el catálogo y el «Basado en» aparecía en español
  dentro de una frase ya traducida.
- Se perdía la **negrita** de los 13 párrafos con formato inline al traducir la web.
- Literal `${t(…)}` sin procesar visible en la web cuando el historial de cálculos estaba vacío.
- La clave del subtítulo de la portada estaba escrita con `&amp;` y por eso nunca se traducía.
- Errata «golpos/tira» → «golpes/tira» en el informe.
- 27 claves muertas retiradas del catálogo (fragmentos, plantillas antiguas y etiquetas sustituidas).

## [2.1.1] — 2026-10-21

### Añadido
- **Portada del proyecto al día**: seis idiomas, reporte de errores, sección «Novedades 2.1.1»
  y tabla de versiones completa (2.1.1, 2.1.0, 2.0.1 y 2.0.0).
- **Página pública de notas** `docs/notas.html` con los bloques «antes / hecho / pendiente» y la
  tabla de versiones de compilación.
- **`version.json`** (raíz y `docs/`) con producto, versión, `versionCode`, fecha, artefactos,
  idiomas y número de cadenas del catálogo.
- Tarjeta **«Novedades de esta versión»** en Android y compilación visible en el texto legal.
- El informe de error de Android incluye el `versionCode` (`Versión 2.1.1 (compilación 6)`).

### Cambiado
- Catálogo de idiomas: **350 cadenas** (antes 345), con las cadenas de novedades y las líneas de
  versión actualizadas a 2.1.1.
- Web: pie `v2.1.1 · 21/10/2026` y el informe técnico indica `web v2.1.1`.
- `versionName 2.1.1` · `versionCode 6` (antes 2.1.0 · 5).

### Corregido
- Las filas de la 2.1.0 y la 2.0.1 estaban **en la tabla de modelos** de la portada; se movieron a
  la tabla de versiones.
- Documentación sincronizada con el estado real del proyecto (portada, notas, README y roadmap).

## [2.1.0] — 2026-10-21

### Añadido
- **Seis idiomas** (ES, EN, PT-BR, PT-PT, IT y ZH) en la web y en la app Android, con detección
  automática, selector en la web y `localeConfig` en Android. Catálogo único
  `i18n/catalog.json` con 345 cadenas.
- **Reporte de errores sin red**: en la web con `mailto:` y copia al portapapeles; en Android con
  `ACTION_SENDTO`/`ACTION_SEND`, portapapeles y `last_crash.txt`.

### Corregido
- Dos tarjetas HTML de la web: «Exportar resultado» (faltaban etiquetas de apertura) y
  «Mantenimiento y vida útil» (cierre de cuadrícula antes de tiempo).
- Restos sin traducir: «12 meses», «Todos», «Boquilla (µm)» e interpolaciones.

### Cambiado
- `versionName 2.1.0` · `versionCode 5`.
- CI de validación (`.github/workflows/validar.yml`) y `tools/check_i18n.py`.
## [2.0.1] — 2026-10-21

### Corregido
- **Mantenimiento**: el cartucho Core de VJ 1620 HR, VJ 1650 HR y VJ 1650 UHS pasa a **14 000 h**
  (antes 12 000 h), según la hoja *Maintenance Setup* del CIJ TCO Calculator v35.
- **Velocidad**: VJ 1580 y VJ 1860 con boquilla de 70 µm ya muestran caracteres/s y velocidad nominal.
  La tabla de 70 µm pasa a usar la clave `1580+` (la hoja original la llama `1580/1580+` y no se encontraba).
- **Tintas sin datos oficiales**: V4238 (MEK / Methanol) y V4251 (Methanol / Water) no tienen factor en el
  Makeup Usage Model v14 (la ficha original da `#N/D`). Ahora se avisa y el resultado se etiqueta como
  estimación con base MEK, en lugar de calcularlo en silencio.
- **QR**: se sustituye la fuente genérica «QR Code» por las cuatro versiones reales (21, 25, 29 y 33) de
  una línea, y el cálculo de gotas reconoce el nombre nuevo.
- **Cartuchos en Android**: al cambiar de equipo, boquilla o tinta, el tamaño se toma de la tinta
  (TCO v35). Antes el 1710 usaba 600 ml y el 1580C 700 ml también para el solvente, cuyo contenedor es 750 ml.

### Cambiado
- Cifras de validación corregidas en la app, la web y la documentación: **139 tintas** (antes «142», que
  incluía tres entradas que no son tintas), **734 fichas de solvente** y **535 de velocidad** contrastadas.
- Se retira la mención a «28/28 pruebas»: el repositorio no incluye una batería de pruebas automatizada.
- Versión: `versionName 2.0.1` · `versionCode 4`.

### Notas
- Pendiente de confirmar con Videojet: V457 con solvente V725 figura con **740 ml** en el TCO
  (el resto de V725 usa 750 ml).

## [2.0.0] — 2026-10-21

### Eliminado
- Módulo experimental de «apoyo voluntario» y todo su código, consentimiento, textos y avisos.
- Permisos `INTERNET` y `ACCESS_NETWORK_STATE`: la aplicación vuelve a ser **100 % offline**.
- Todo el apartado económico: precios por cartucho, coste de fluidos y coste por 1 000 impresiones
  (campos, cálculos, tablas e informes).
- Bloque de avisos legales extenso del pie de página; se sustituye por notas técnicas de fuentes de datos.

### Cambiado
- La tarjeta de ajustes pasa a llamarse **«Mantenimiento y ajustes avanzados»**.
- La sección final de la app pasa a **«Fuentes de datos y notas técnicas»**.
- Versión de la app: `versionName 2.0` (`versionCode 3`).

### Verificado
- APK sin permisos de red (`aapt2 dump badging`) y con soporte `small…xlarge` y todas las densidades.
- Pruebas del motor: comprobaciones manuales contra los documentos oficiales.
- Prueba en dispositivo (Pixel 7 / API 34): instalación, arranque y navegación sin errores.

## [1.2.0] — 2026-10-20

### Añadido
- **Cartuchos reales por tinta y modelo** (139 tintas) con su solvente asociado
  (V7201, V7257, V7220…): p. ej. V4225 = 700 ml, V480 = 600 ml, V401 = 750/5000 ml.
- Seguimiento de mantenimiento con **P/N e intervalo oficial** por modelo.
- Notación compacta para cifras grandes (M/B) y mejoras de legibilidad.
- Módulo opcional de apoyo con consentimiento explícito (retirado en 2.0).

### Corregido
- Selección de boquilla por modelo y coherencia del consumo de solvente en 1880/1880+ (60 vs 70 µm).
- Historial: las entradas antiguas dejaban de sobrescribirse con la configuración actual.

## [1.1.0] — 2026-10-20

### Añadido
- **Familias corregidas**: Simplicity (1240, 1280, 1580, 1580+, 1580C, 1880, 1880+, 1880 UHS, 1880 HR)
  y Serie 1000VJ (resto de modelos 1XX0).
- Modelos unificados con **boquillas dependientes del modelo** (40/50/60/70 µm).
- Validación de la hoja *Maintenance Setup* del TCO para núcleos y módulos de servicio.

### Cambiado
- Interfaz adaptativa (1 columna &lt; 840 dp · 2 columnas ≥ 840 dp) y temas Claro/Oscuro/AMOLED.

## [1.0.0] — 2026-10-19

### Añadido
- Primera versión: calculadora web offline y app Android nativa (Kotlin + Jetpack Compose).
- Motor de solvente (CIJ Makeup Usage Model v14), tinta (CIJ TCO v35) y velocidad (CIJ Line Speed v9.7).
- Gráficos, exportación CSV/TXT, historial y temas de pantalla.
- Verificación de datos con las fichas oficiales de solvente y velocidad.
