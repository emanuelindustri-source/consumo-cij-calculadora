# Notas de la versión 2.1.1

**Fecha:** 2026-10-21 · **Ámbito:** portada del proyecto, notas públicas, web (archivo único),
APK Android y repositorio.

Este documento deja constancia de **cómo estaba el proyecto antes**, **qué se cambió en la 2.1.1**
y **qué queda pendiente**. Se complementa con [`NOTAS-2.1.0.md`](NOTAS-2.1.0.md), que documenta
la entrega anterior (idiomas y reporte de errores).

---

## 1. Lo que había antes (2.1.0)

### Portada del proyecto (`docs/index.html`)
- Seguía anunciando la **2.0.0** y un APK de **1,8 MB**, aunque la última entrega era la 2.1.0.
- No mencionaba los **seis idiomas**, el **reporte de errores** ni la validación automática.
- La tabla de versiones solo llegaba hasta la 2.0.0.
- Las filas de la 2.1.0 y la 2.0.1 se habían insertado **por error en la tabla de modelos**
  (una tabla de dos columnas con filas de tres celdas), lo que rompía esa sección.
- No había una página pública de **notas de versión** ni forma de seguir «antes / hecho / pendiente».

### Web y app
- La web y la app ya estaban en **seis idiomas** y con reporte de errores, pero **no mostraban
  la compilación**: el pie decía «v2.1.0» sin número de compilación ni fecha.
- La app no tenía una tarjeta de **novedades** y el texto legal solo indicaba `2.1.0`.
- El informe de error de Android enviaba la versión, pero **no el `versionCode`**.

### Repositorio
- No existía un archivo **`version.json`** legible por máquina con versión, compilación, fecha
  y artefactos.
- La documentación de la 2.1.0 estaba repartida, pero la **portada y las notas públicas** no
  reflejaban el estado real.

---

## 2. Lo que se hizo en la 2.1.1

### 2.1. Portada al día (`docs/index.html`)
- Nueva banda superior: **«Versión 2.1.1 · 6 idiomas · 100 % offline»**.
- Texto de presentación con **seis idiomas**, detección automática y **reporte de errores**.
- Sección nueva **«Novedades 2.1.1»** con cuatro tarjetas: idiomas, reporte, correcciones y
  seguimiento.
- Tabla de versiones ampliada: **2.1.1, 2.1.0, 2.0.1 y 2.0.0**, con enlaces a las notas.
- **Corregidas** las dos filas que estaban en la tabla de modelos; ahora viven en `#versiones`.
- Navegación con **«Notas»**, pie con la versión y enlace a las notas.

### 2.2. Página pública de notas (`docs/notas.html`)
- Página HTML autocontenida, con el mismo estilo del sitio y sin recursos externos.
- Tres bloques: **Antes / Hecho / Pendiente**.
- Tabla de **versiones de compilación** (web, APK, catálogo y repositorio).
- Enlaces al Markdown completo, al historial, al CHANGELOG, al roadmap y a la última release.
- Accesible desde la portada, desde el pie y desde la tabla de versiones.

### 2.3. Versión y compilación visibles
- **Web:** pie con `v2.1.1 · 21/10/2026`, y el informe técnico indica `Consumo CIJ · web v2.1.1`.
- **Android:** texto legal `Versión 2.1.1 · compilación 6`, tarjeta **«Novedades de esta versión»**
  y el informe de error incluye `Versión: 2.1.1 (compilación 6)`.
- **`version.json`** en la raíz y en `docs/`: producto, versión, `versionCode`, fecha, artefactos,
  idiomas, modo offline y número de cadenas del catálogo.

### 2.4. Catálogo de idiomas
- **364 cadenas** × 6 idiomas (eran 345): tarjeta de novedades, título completo y etiquetas
  dinámicas de los paneles de equipo, tinta y mantenimiento.
- Regenerado el bloque `i18n-data` de la web y los `assets/i18n/*.json` de Android desde
  `i18n/catalog.json` (sin editar copias a mano).
- Validación de marcadores `{n}` y de cadenas vacías: **0 problemas**.

### 2.5. Compilación y seguimiento
- APK **2.1.1 / versionCode 6** (antes 2.1.0 / 5), firmada, sin permiso `INTERNET`.
- `docs/calculadora.html` sigue siendo **idéntica byte a byte** a `web/calculadora-offline.html`.
- Validación con `python tools/check_i18n.py` (catálogo, copias, assets, JavaScript y HTML).

---

## 3. Versiones de compilación

| Artefacto | Versión | Compilación | Fecha | Notas |
|---|---|---|---|---|
| Web · `docs/calculadora.html` | 2.1.1 | — | 2026-10-21 | Archivo único, sin recursos externos |
| Android · `ConsumoCIJ-2.1.1.apk` | 2.1.1 | 6 | 2026-10-21 | minSdk 34 · sin `INTERNET` · misma firma que 2.0 |
| Catálogo · `i18n/catalog.json` | 2.1.1 | — | 2026-10-21 | 364 cadenas × 6 idiomas |
| Repositorio · etiqueta `v2.1.1` | 2.1.1 | — | 2026-10-21 | Release con el APK adjunto |

> La versión **web** no usa `versionCode`: se identifica por `2.1.1` y la fecha de compilación.
> La app usa `versionName` + `versionCode`, que Android exige para actualizar.

---

## 4. Verificación realizada

- `python tools/check_i18n.py` → catálogo, copias web idénticas, assets completos, JavaScript
  sin errores de sintaxis y HTML balanceado.
- APK: `aapt2 dump badging` (versión, `minSdk 34`, sin permisos de red) y
  `apksigner verify --print-certs` (misma firma que la 2.0).
- Instalación y arranque en el emulador **Pixel 7 / API 34**, en español e inglés, sin fallos.
- Web: apertura local, cambio de idioma y reporte de error (correo prellenado y copia).
- Portada y notas publicadas en GitHub Pages, con la calculadora enlazada.

---

## 5. Lo que queda pendiente

- Traducir las **cabeceras del informe TXT** (el CSV ya está traducido).
- Revisión por **hablantes nativos** de chino simplificado y portugués de Portugal.
- Conservar la **negrita** en los párrafos traducidos de la web (hoy el texto plano la pierde).
- Valorar un **selector de idioma dentro de la app** (hoy sigue el idioma del sistema).
- Conservar el **keystore** de firma en un lugar seguro para futuras actualizaciones.
- Revocar los **tokens de publicación** que ya no se usen.
- Añadir una **prueba automática del motor de cálculo** con los valores de referencia.

---

## 6. Cómo reproducir esta entrega

```powershell
# 1. Validar catálogo, web, assets y HTML
python tools/check_i18n.py

# 2. Compilar la APK release (Gradle 8.7 y JDK 17)
$env:JAVA_HOME = "C:\Users\Sr.Hidalgo\AppData\Local\Programs\jdk-17"
& "C:\Users\Sr.Hidalgo\.gradle\wrapper\dists\gradle-8.7-bin\bhs2wmbdwecv87pi65oeuq5iu\gradle-8.7\bin\gradle.bat" `
  -p android assembleRelease --offline

# 3. Comprobar la APK
& "$env:ANDROID_HOME\build-tools\34.0.0\aapt2.exe" dump badging `
  android\app\build\outputs\apk\release\app-release.apk
& "$env:ANDROID_HOME\build-tools\34.0.0\apksigner.bat" verify --print-certs `
  android\app\build\outputs\apk\release\app-release.apk
```

---

## 7. Enlaces

- [Notas de la versión en la web](https://emanuelindustri-source.github.io/consumo-cij-calculadora/notas.html)
- [Calculadora web](https://emanuelindustri-source.github.io/consumo-cij-calculadora/calculadora.html)
- [Historial de versiones](VERSIONES.md) · [CHANGELOG](../CHANGELOG.md) · [Roadmap](../ROADMAP.md)
- [Última release](https://github.com/emanuelindustri-source/consumo-cij-calculadora/releases/latest)
