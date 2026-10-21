# Notas de la versión 2.1.0

**Fecha:** 2026-10-21 · **Ámbito:** web (archivo único), APK Android y repositorio.

Este documento deja constancia de **cómo estaba el proyecto antes**, **qué se cambió en la 2.1.0**
y **qué queda pendiente**.

---

## 1. Lo que había antes (versión 2.0.1)

### Web (`web/calculadora-offline.html` y copia en `docs/calculadora.html`)
- Calculadora completa en un único archivo HTML, **solo en español**, sin selector de idioma.
- Los textos dinámicos (avisos, mantenimiento, gráficos, informes) estaban escritos directamente
  en español dentro del JavaScript; los números se formateaban siempre con `es-ES`.
- **Dos errores reales de HTML** que afectaban a la maquetación:
  1. La tarjeta **«Exportar resultado»** no tenía las etiquetas de apertura (`<div class="card">` y
     `<h2><span class="ic"><svg…`): la línea empezaba directamente en `/><path …`, por lo que el
     título y la tarjeta quedaban fuera de estructura.
  2. La tarjeta **«Mantenimiento y vida útil»** cerraba la cuadrícula de campos antes de tiempo
     (`</div>` de más): los tres campos (vida en almacén, vida del núcleo por horas y por días)
     quedaban fuera de la cuadrícula y sobraba un cierre al final de la tarjeta.
- No había ninguna forma de **reportar errores** desde la página.

### Android (`android/`)
- Interfaz **solo en español**, con los textos escritos como literales dentro de Kotlin
  (`CalculatorScreen.kt`, ~145 literales de interfaz).
- Sin `android:localeConfig`: no se podía elegir el idioma por aplicación en Android 13+.
- Sin captura de fallos ni forma de enviar un informe de error.
- `versionName 2.0.1` · `versionCode 4` · sin permiso `INTERNET`.

### Repositorio
- CI únicamente para compilar la APK (`.github/workflows/android.yml`).
- Sin validación automática de la web, del catálogo de idiomas ni de las copias publicadas.

---

## 2. Lo que se hizo en la 2.1.0

### 2.1. Idiomas (español como idioma principal) — web y Android

- **Catálogo único** `i18n/catalog.json`: **340 cadenas** con el español como idioma de origen y
  traducciones a **inglés (en), portugués de Brasil (pt-BR), portugués de Portugal (pt-PT),
  italiano (it) y chino simplificado (zh)**.
- **Web**
  - Detección automática por `navigator.languages` (pt-BR/pt-PT, zh, it, en, es) y memoria de la
    elección en `localStorage` (`vj_calc_lang`).
  - **Selector de idioma** en la cabecera (Español, English, Português BR/PT, Italiano, 中文).
  - Motor `t()` / `tf()` para textos dinámicos, traducción de nodos de texto y de los atributos
    `title`, `aria-label`, `placeholder` y `alt`.
  - Números y fechas con el locale del idioma (`es-ES`, `en-GB`, `pt-BR`, `pt-PT`, `it-IT`, `zh-CN`).
  - `document.documentElement.lang` actualizado y `<title>` traducido.
  - Si el idioma no está soportado se usa **español**; si falta una cadena se muestra el original.
- **Android**
  - `i18n/I18n.kt` carga `assets/i18n/<idioma>.json`; el idioma se toma de la configuración del
    sistema, incluido el **idioma por aplicación de Android 13+**.
  - Extensión `String.tr()` aplicada a **145 literales** de la interfaz (títulos, campos, avisos,
    botones, textos legales…).
  - `res/xml/locales_config.xml` + `android:localeConfig` con es, en, pt-BR, pt-PT, it y zh-CN.
  - Traducciones empaquetadas en `assets/i18n/*.json` (sin código generado ni dependencias nuevas).

### 2.2. Reporte de errores sin romper el «100 % offline»

No se añadió ningún servidor ni permiso de red: el informe se entrega al **cliente de correo del
usuario**, que es quien lo envía.

- **Web**
  - Botón **«Reportar un error»** en el pie de página.
  - Formulario con descripción y correo de contacto opcional.
  - Genera un informe con versión, idioma, navegador, pantalla, fecha, parámetros del cálculo y el
    último error de JavaScript; se abre `mailto:emanuel.industri@gmail.com` con todo redactado.
  - Botón **«Copiar informe»** como alternativa si no hay cliente de correo.
  - Captura de `window.onerror` y `unhandledrejection` para incluir el último fallo.
- **Android**
  - Tarjeta **«Reportar un error»** al final de la pantalla (descripción, enviar y copiar).
  - `Intent.ACTION_SENDTO` (`mailto:`) con alternativa `ACTION_SEND` y, si falla, portapapeles.
  - **No requiere el permiso `INTERNET`**: la app sigue siendo 100 % offline.
  - Manejador de excepciones no capturadas que guarda `last_crash.txt` en el almacenamiento privado
    y ofrece enviar el detalle en el siguiente arranque.

### 2.3. Correcciones

- Web: reparadas las dos tarjetas HTML rotas descritas en el apartado 1.
- `docs/calculadora.html` queda **idéntico byte a byte** a `web/calculadora-offline.html`.
- Android: texto legal y versión actualizados a **2.1.0** (`versionCode 5`).

### 2.4. Validación y CI

- Nuevo `tools/check_i18n.py` (sin dependencias) que comprueba:
  catálogo completo y sin cadenas vacías, marcadores `{0}…` coherentes, copias web idénticas,
  ausencia de recursos externos, bloque de traducciones completo, assets de Android completos,
  claves usadas en JavaScript presentes en el catálogo, **sintaxis JavaScript** (`node --check`) y
  **HTML balanceado**.
- Nuevo workflow `.github/workflows/validar.yml` que ejecuta esa validación en cada push/PR.
- Resultado local: **todo correcto** (340 cadenas × 5 idiomas, HTML balanceado, JS sin errores).

### 2.5. Verificación de la APK

- Compilada en **release** (R8 + recursos reducidos) y firmada con la **misma clave que la 2.0**
  (SHA-256 `c413a1fb9cdf13cc4affaa913d18e70e89c1dd16bef067d08df8ee400ded63e4`).
- `versionCode 5` · `versionName 2.1.0` · `minSdk 34` · **sin permiso `INTERNET`**.
- Instalada y arrancada en el emulador **Pixel 7 API 34** (ver captura en la release).

### 2.6. Archivos nuevos o relevantes

| Archivo | Contenido |
|---|---|
| `i18n/catalog.json` | Catálogo único de traducciones (340 cadenas × 6 idiomas) |
| `tools/check_i18n.py` | Validador de catálogo, web, assets y HTML |
| `.github/workflows/validar.yml` | CI de validación web/i18n |
| `android/.../i18n/I18n.kt` | Motor de idiomas y extensión `tr()` |
| `android/.../util/ReportIssue.kt` | Informe de error sin red + registro de fallos |
| `android/.../assets/i18n/*.json` | Traducciones empaquetadas en la APK |
| `android/.../res/xml/locales_config.xml` | Idiomas soportados por la app |
| `docs/NOTAS-2.1.0.md` | Este documento |

---

## 3. Pendiente / próximos pasos

1. **Informes**: las cabeceras del informe **TXT** y algunos textos técnicos largos siguen en
   español; el **CSV** ya usa las etiquetas traducidas. Traducir el resto en la próxima revisión.
2. **Revisión nativa**: conviene que un hablante de **chino** y otro de **portugués de Portugal**
   revisen los términos técnicos (墨水, 溶剂, 喷头, 滤芯; «bico»/«bocal»).
3. **Formato**: al traducir párrafos con negritas en la web se sustituye el texto completo y se
   pierde la negrita en idiomas distintos del español. Se puede conservar el marcado con un mapa
   de fragmentos.
4. **Selector dentro de la app**: hoy la app sigue el idioma del sistema o el idioma por aplicación
   de Android 13+; no hay un desplegable propio. Valorar añadirlo.
5. **Firma**: el keystore no se publica en el repositorio. Conservarlo para futuras actualizaciones
   (la 2.1.0 usa la misma clave que la 2.0).
6. **Correo de destino**: si cambia, actualizar `REPORT_MAIL` en la web y `ReportIssue.MAIL` en
   Android.
7. **CI**: revisar el workflow `android.yml` existente (el repositorio no incluye `gradlew`; el CI
   instala Gradle). El nuevo `validar.yml` no compila la APK, solo valida web e idiomas.
8. **Seguridad**: revocar el token de GitHub utilizado para publicar esta versión.

---

## 4. Cómo comprobarlo

```text
Web
  1. Abrir web/calculadora-offline.html (o docs/calculadora.html) en el navegador.
  2. Cambiar el idioma en la cabecera: toda la interfaz y los avisos cambian de idioma.
  3. Pulsar «Reportar un error»: se abre el correo con el informe redactado (o se copia).

Repositorio
  python tools/check_i18n.py      → debe terminar con «Todo correcto.»

Android
  android/app/build/outputs/apk/release/app-release.apk
  adb install -r app-release.apk
  Ajustes > Aplicaciones > Consumo CIJ > Idioma (Android 13+)
```

---

## 5. Resumen en una línea

La 2.1.0 mantiene la calculadora **100 % offline**, añade **seis idiomas con detección automática**,
incorpora un **reporte de errores por correo** en web y Android, corrige **dos errores de HTML** y
deja **validación automática** para que la web, el catálogo y la APK no se rompan.
