# Arquitectura del proyecto

## Visión general

```
┌─────────────────────────────┐     ┌──────────────────────────────┐
│  web/calculadora-offline.html│     │  android/ (Kotlin + Compose) │
│  HTML+CSS+JS en un archivo   │     │  App nativa Android 14+      │
│  0 dependencias · 0 red      │     │  0 permisos de red           │
└──────────────┬──────────────┘     └──────────────┬───────────────┘
               │                                    │
               └──────────────┬─────────────────────┘
                              ▼
                 Motor de cálculo (idéntico)
     solvente · tinta · velocidad · mantenimiento
                              ▲
                              │
        Datos generados y verificados (modelos oficiales)
```

El **motor es el mismo** en ambas plataformas: mismas fórmulas, mismos datos y mismos resultados.

## Estructura de carpetas

```
.
├── docs/                        Sitio GitHub Pages
│   ├── index.html               Portada del proyecto (landing)
│   ├── calculadora.html         Calculadora completa (autónoma)
│   ├── assets/                  Capturas de la app
│   ├── DATOS.md                 Fuentes, fórmulas y validación
│   ├── VERSIONES.md             Historial de versiones
│   └── ARQUITECTURA.md          Este documento
├── web/
│   └── calculadora-offline.html Calculadora para usar sin conexión
├── android/                     Proyecto Android Studio
│   ├── app/src/main/java/com/calculadora/consumocij/
│   │   ├── MainActivity.kt
│   │   ├── CalculatorViewModel.kt
│   │   ├── data/CalcEngine.kt        Motor de cálculo
│   │   ├── data/VideojetData.kt      Datos oficiales (generado)
│   │   ├── ui/CalculatorScreen.kt    Interfaz adaptable
│   │   ├── ui/Charts.kt              Gráficos con Canvas
│   │   ├── ui/theme/Theme.kt         Temas Claro/Oscuro/AMOLED
│   │   └── util/                     Formato e informes CSV/TXT
│   ├── app/src/main/res/             Recursos (dpi, sw600dp, sw720dp, land)
│   └── LEEME.md                      Guía de compilación y firma
├── .github/workflows/android.yml     CI: compila el APK
├── CHANGELOG.md · ROADMAP.md · VERSIONES
└── LICENSE (MIT)
```

## Web

- **Un único archivo** con CSS y JavaScript embebidos; sin fuentes, CDN ni librerías.
- Gráficos dibujados con `Canvas` 2D y escalado por `devicePixelRatio`.
- Persistencia en `localStorage` (configuración e historial).
- Exportación con `Blob` + descarga local.
- Diseño adaptable con *media queries* para móvil, tableta, escritorio y TV.

## Android

- **Kotlin + Jetpack Compose** (Material 3), `minSdk 34`, `targetSdk 34`.
- Estado con `ViewModel` + `mutableStateOf`; persistencia con `SharedPreferences`.
- Gráficos con `Canvas` de Compose (independientes de la resolución).
- Exportación mediante **SAF** (`CreateDocument`), sin permisos de almacenamiento.
- Temas: Automático, Claro, Oscuro y AMOLED (negro puro).
- Adaptación por tamaño: `values/`, `values-sw600dp/`, `values-sw720dp/`, `values-land/`.
- **Sin permisos de red**: el manifiesto no declara `INTERNET`.

## Generación de datos

Los datos embebidos en la app (`VideojetData.kt`) y en la web (objeto `DATA`) se generan a partir de los
libros de cálculo oficiales y se verifican contra los PDFs publicados. El proceso fue:

1. Lectura de `CIJ Makeup Usage Model v14.xlsx` (equipos, horas por litro, ecuaciones y constantes).
2. Lectura de `CIJ TCO Calculator_v35.xlsm` (gotas/litro, gotas/carácter, contenedores por tinta y
   hoja *Maintenance Setup*).
3. Lectura de `CIJ Line Speed Calculator v9.7.xlsx` (dos juegos de velocidad según boquilla).
4. Contraste con 734 fichas de solvente y 535 de velocidad de línea.
5. Generación de los archivos de datos para web (JSON) y Android (Kotlin).

## Idiomas e informes

- **Fuente única de verdad**: [`i18n/catalog.json`](../i18n/catalog.json). De ahí se generan el
  bloque `<script id="i18n-data">` de la web, los `assets/i18n/*.json` de Android y la copia
  `docs/calculadora.html` con `python tools/gen_i18n.py`. **Ninguna copia se edita a mano.**
- **Negrita y formato**: los tramos resaltados se encierran entre `**…**` en la clave y en cada
  traducción; la web reconstruye la etiqueta HTML original (`<b>`, `<em>`, `<code>`) y escapa el
  resto del texto. Así la traducción conserva el formato sin editar el HTML.
- **Informes**: las cabeceras y etiquetas del informe TXT/CSV y del informe técnico pasan por
  `t()` / `tf()`, con las etiquetas alineadas por idioma (`rptRow` en web, `padRow` en Android).

## Herramientas y pruebas

| Herramienta | Qué hace |
|---|---|
| `tools/gen_i18n.py` | Genera todos los artefactos de traducción desde el catálogo (`--check` para comprobar) |
| `tools/check_i18n.py` | Catálogo, copias web, assets, párrafos traducibles, JavaScript y HTML |
| `tools/test_engine.js` | 43 pruebas del motor de la web contra los valores de referencia oficiales |
| `tools/validar.py` | Ejecuta todo lo anterior y las pruebas de Kotlin de una sola vez |
| `android/app/src/test/` | 17 pruebas del motor de Android (JVM, sin dispositivo) |

Las pruebas no reimplementan las fórmulas: **extraen el motor real** del HTML (web) o compilan
`CalcEngine.kt` (Android) y lo comparan con los valores publicados en los modelos oficiales de
Videojet. La CI las ejecuta en cada push.

## Compilación

**Web:** no requiere compilación; se abre el HTML o se publica en cualquier servidor estático.

**Android:**
```bash
cd android
gradle testDebugUnitTest  # 17 pruebas del motor
gradle assembleDebug      # desarrollo
gradle assembleRelease    # producción (firmado, R8 + shrinkResources)
```

La integración continua (`.github/workflows/`) valida web e idiomas (`validar.yml`) y ejecuta las
pruebas de Kotlin y la compilación de la APK (`android.yml`) en cada push.

## Decisiones de diseño

- **Sin red**: la herramienta debe funcionar en planta sin cobertura; por eso no hay permisos de internet.
- **Datos locales**: los modelos se embeben en el binario para garantizar reproducibilidad.
- **Un solo archivo web**: facilita su distribución por USB o correo.
- **Fórmulas explícitas**: se documentan en `docs/DATOS.md` para poder auditarlas.
