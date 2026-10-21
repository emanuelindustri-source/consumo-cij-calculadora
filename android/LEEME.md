# Consumo CIJ · Aplicación Android

Calculadora de consumo de **tinta y solvente** para impresoras CIJ de las familias **Simplicity** y
**Serie 1000VJ**, con los modelos oficiales incrustados.

| Modelo | Uso |
|---|---|
| `CIJ Makeup Usage Model v14` | Consumo de solvente por temperatura |
| `CIJ TCO Calculator v35` | Gotas/carácter, gotas/litro, contenedores y mantenimiento |
| `CIJ Line Speed Calculator v9.7` | Velocidad de línea, cpi y dpi |

Contrastado con **734 fichas de solvente** y **535 de velocidad** de línea (coincidencia total).

---

## Especificaciones

| Característica | Valor |
|---|---|
| Tipo | Aplicación **nativa** (Kotlin + Jetpack Compose, Material 3) |
| Versión mínima | **Android 14 (API 34)** — compatible con 15 y superiores |
| Compilación | compileSdk 34 · targetSdk 34 · minSdk 34 · **versionName 2.1.1** · versionCode 6 |
| Paquete | `com.calculadora.consumocij` |
| Nombre visible | **Consumo CIJ · Calculadora** |
| Internet | **No usado** — el manifiesto no declara `INTERNET` |
| Pantallas | `small · normal · large · xlarge` · densidades 160…65534 |
| Persistencia | `SharedPreferences` (local) |
| Exportación | CSV / TXT con el selector de archivos del sistema (SAF) |
| Tamaño | **≈ 1,77 MB** (release, R8 + recursos reducidos) |

### Adaptación de pantalla

- Teléfonos, phablets, tabletas y TV (1 columna < 840 dp · 2 columnas ≥ 840 dp).
- Cualquier resolución y densidad; gráficos vectoriales con Canvas.
- 16:9, 18:9, 20:9, 21:9, 4:3, apaisado, *notch* y barras del sistema (edge-to-edge).
- Escala de fuente del sistema y multi-ventana.
- Temas: Automático, Claro, Oscuro y **AMOLED (negro puro)**.

---

## Compilar

### Android Studio (recomendado)
1. **Open** → seleccionar esta carpeta.
2. Esperar la sincronización de Gradle.
3. **Build → Build App Bundle(s) / APK(s) → Build APK(s)**.
4. APK en `app/build/outputs/apk/debug/app-debug.apk`.

### Línea de comandos
```bat
cd android
set JAVA_HOME=C:\Users\Sr.Hidalgo\AppData\Local\Programs\jdk-17
set ANDROID_HOME=C:\Users\Sr.Hidalgo\AppData\Local\Android\Sdk
gradle assembleRelease
```
Requisitos: **JDK 17**, **Android SDK 34** y **Gradle 8.7+**.

### Firma
Cree su propio *keystore* (no se incluye en el repositorio por seguridad):

```bat
keytool -genkeypair -alias consumocij -keyalg RSA -keysize 2048 -validity 10000 ^
        -keystore keystore\consumocij.jks
```

Cree `keystore/keystore.properties`:
```properties
storeFile=keystore/consumocij.jks
storePassword=********
keyAlias=consumocij
keyPassword=********
```
El *build* usará esa firma si el archivo existe; si no, usará la firma de depuración.

### Instalar
```bat
%ANDROID_HOME%\platform-tools\adb.exe install -r app-release.apk
```

---

## Estructura

```
android/
├── build.gradle.kts · settings.gradle.kts · gradle.properties
└── app/
    ├── build.gradle.kts              minSdk 34 · Compose · Material 3
    └── src/main/
        ├── AndroidManifest.xml       SIN permisos de red
        └── java/com/calculadora/consumocij/
            ├── MainActivity.kt
            ├── CalculatorViewModel.kt
            ├── data/VideojetData.kt  datos oficiales (generados)
            ├── data/CalcEngine.kt    motor de cálculo
            ├── ui/CalculatorScreen.kt
            ├── ui/Charts.kt
            ├── ui/theme/Theme.kt
            └── util/                 formato e informes
```

## Notas

- La app funciona **sin conexión**; no hay telemetría, publicidad ni recogida de datos.
- La sección **«Fuentes de datos y notas técnicas»** dentro de la app documenta el origen de los valores.
- Licencia del proyecto: **MIT**.

---

## Idiomas, reporte de errores y seguimiento (2.1.0 – 2.1.1)

- La interfaz sigue el **idioma del sistema** (o el idioma por aplicación de Android 13+, en
  Ajustes > Aplicaciones > Consumo CIJ > Idioma). Idiomas: español, inglés, portugués de Brasil,
  portugués de Portugal, italiano y chino simplificado; si no hay traducción se muestra el español.
- Las traducciones están en `app/src/main/assets/i18n/<idioma>.json` y las carga
  `i18n/I18n.kt`; los literales de la interfaz usan la extensión `String.tr()`.
- `res/xml/locales_config.xml` declara los idiomas y el manifiesto lo enlaza con
  `android:localeConfig`.
- **Reportar un error**: tarjeta al final de la pantalla. Genera un informe con versión,
  dispositivo, idioma y los datos del cálculo, y lo entrega al correo del usuario
  (`emanuel.industri@gmail.com`). Si la app falla al arrancar, guarda `last_crash.txt` y ofrece
  enviarlo la próxima vez.
- La app **no declara el permiso `INTERNET`**: el envío lo hace la aplicación de correo.
- **Versión y compilación**: la tarjeta «Novedades de esta versión» y las notas técnicas muestran
  `2.1.1 · compilación 6`; el informe de error añade el `versionCode`.
- Notas completas: [`docs/NOTAS-2.1.1.md`](../docs/NOTAS-2.1.1.md) y
  [`version.json`](../version.json).
