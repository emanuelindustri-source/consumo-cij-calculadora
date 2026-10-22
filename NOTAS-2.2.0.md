# Notas de la versión 2.2.0

**Producto:** **Consumo CIJ** — calculadora de consumo de tinta y solvente para impresoras de
inyección continua (CIJ) de Videojet, en web 100 % offline y app Android nativa.

**Fecha:** 2026-10-22 · **Ámbito:** web (archivo único), portada y notas públicas, APK Android,
catálogo de idiomas, pruebas automáticas y repositorio.

Este documento deja constancia de **cómo se encontró el proyecto**, **qué se iba a hacer**,
**qué se consiguió** y **qué queda pendiente a futuro**. Se complementa con
[`NOTAS-2.1.1.md`](docs/NOTAS-2.1.1.md) y [`NOTAS-2.1.0.md`](docs/NOTAS-2.1.0.md), que documentan
las entregas anteriores.

---

## 1 · Cómo se encontró el proyecto (estado a la entrada)

El proyecto llegó en una carpeta de trabajo con dos partes bien diferenciadas:

| Dónde | Qué había |
|---|---|
| `repo-consumo-cij/` | El repositorio del proyecto, **sin carpeta `.git`**: el código estaba completo y al día con la versión 2.1.1, pero no había historial local ni forma de publicar sin reconstruirlo. |
| `Calculadora_Consumo_Tinta_Solvente_Videojet_CIJ.html` | Copia suelta de la calculadora web. |
| `Consumo_CIJ_Calculadora_v2.0_Android14.apk` y `…_v2.1_…` | APKs compiladas fuera del repositorio (el `.gitignore` excluye los binarios, como es correcto). |
| `NOTAS-2.1.0.md`, `NOTAS-2.1.1.md`, `version.json` | Notas de las entregas anteriores, también sueltas fuera del repositorio. |
| `C:\Users\Sr.Hidalgo\Documents\miralo\` | Biblioteca de referencia: ~2 000 ficheros técnicos Videojet (manuales, IPB, fichas de solvente, competencia, informes de servicio, simulador 1880 Plus y el software `CIJ 1580 Printer`). **Fuente de verdad para contrastar datos.** |

### Lo que ya estaba bien hecho

- **Motor de cálculo** completo en web y Android, con los tres modelos oficiales
  (CIJ Makeup Usage Model v14 · CIJ TCO Calculator v35 · CIJ Line Speed Calculator v9.7).
- **139 tintas**, 734 fichas de solvente y 535 de velocidad contrastadas.
- **100 % offline** en ambos: sin permisos de red, sin recursos externos, sin analítica.
- **Seis idiomas** (ES, EN, PT-BR, PT-PT, IT, ZH) con catálogo único `i18n/catalog.json`.
- **Reporte de errores** por correo del usuario, sin servidor.
- Documentación seria: `README`, `CHANGELOG`, `ROADMAP`, `DATOS.md`, `ARQUITECTURA.md`,
  licencia MIT, plantillas de issues y CI de compilación.

### Lo que faltaba (lo que se encontró roto o a medias)

1. **Cabeceras del informe TXT en español** en todos los idiomas. El CSV estaba traducido, pero el
   TXT —el informe que se pega en los informes de servicio— no.
2. **La negrita se perdía al traducir.** Los 13 párrafos del pie con `<b>` se sustituían por texto
   plano y el formato desaparecía en los cinco idiomas traducidos.
3. **Dos párrafos quedaban a medias:** `Basado en CIJ Makeup Usage Model v14…` y
   `Basado en CIJ TCO Calculator v35:` no estaban en el catálogo, así que el «Basado en» se quedaba
   en español dentro de una frase ya traducida.
4. **Sin selector de idioma en la app**: dependía del idioma del sistema.
5. **Cero pruebas automáticas del motor**, pese a que la documentación citaba valores de referencia.
6. **Dos fallos pequeños sin detectar:** un literal `${t(…)}` sin procesar visible en el historial
   vacío de la web, y una clave del catálogo escrita con `&amp;` que impedía traducir el subtítulo de
   la portada de la calculadora.
7. **Herramientas de validación incompletas:** `check_i18n.py` comprobaba el catálogo pero no que
   cada párrafo de la web tuviera traducción, ni que las copias generadas estuvieran al día.
8. **El repositorio local no era un repositorio**: no había `.git`, por lo que no se podía versionar
   ni publicar sin antes reconstruir el historial.

---

## 2 · Qué se iba a hacer (plan de la 2.2.0)

Se decidió que la 2.2.0 fuera la versión que **cierra los pendientes inmediatos** recogidos en el
`ROADMAP` de la 2.1.1, en lugar de añadir funciones nuevas:

| # | Objetivo | Fuente |
|---|---|---|
| 1 | Traducir las cabeceras del **informe TXT** y del informe técnico | ROADMAP · pendiente 2.1.2 |
| 2 | **Conservar la negrita** en los párrafos traducidos | ROADMAP · pendiente 2.1.2 |
| 3 | **Selector de idioma dentro de la app** | ROADMAP · pendiente 2.1.2 |
| 4 | **Prueba automática del motor** con valores de referencia | ROADMAP · pendiente 2.1.2 |
| 5 | **Un solo comando de validación** + CI completa | Mejora de proceso |
| 6 | Nueva **portada**, **notas públicas**, documentación y repositorio profesional | Peticiones de la entrega |
| 7 | Nueva **APK funcional** firmada con la misma clave | Peticiones de la entrega |

---

## 3 · Qué se consiguió (la 2.2.0)

### 3.1 · Informes TXT y técnicos traducidos por completo

Antes, `buildReport()` (web) y `Report.txt()` (Android) escribían las cabeceras en español:
`== EQUIPO ==`, `Modelo............:`, `Boquilla..........:`…

Ahora:

- **42 cadenas nuevas** para las secciones, las etiquetas y las unidades del informe.
- Las etiquetas se **alinean solas con puntos** (`rptRow`) calculando el ancho de la etiqueta más
  larga del idioma activo, así que la maquetación se mantiene en inglés, portugués, italiano y chino.
- El **informe técnico** del reporte de errores (`techInfo`) y el propio informe
  (`reportText`) también están traducidos, en web y en Android.
- El CSV, que ya estaba traducido, pasa a traducir también las **unidades** (`ml/día`, `L/año`,
  `meses`, `días`).

### 3.2 · Negrita y formato conservados al traducir

Solución con **marcadores en el propio catálogo**: la clave sigue siendo el texto en español, pero
los tramos resaltados se encierran entre `**…**` y cada traducción lleva los mismos marcadores.

```json
"Las **gotas por carácter** dependen de la matriz de la fuente … y las **gotas por litro** del diámetro de boquilla.": {
  "en": "**Drops per character** depend on the font matrix … and **drops per liter** on the nozzle diameter.",
  "zh": "**每字符的墨滴数**取决于字体点阵 …，**每升的墨滴数**取决于喷嘴孔径。"
}
```

- La web reconstruye la etiqueta HTML original del párrafo (`<b>`, `<em>`, `<code>`…), de modo que se
  conserva **la misma etiqueta**, no solo «negrita».
- El texto restante se **escapa** antes de insertarlo, así que los ejemplos con `<equipo>`,
  `<tinta>` o `&` se siguen viendo correctamente.
- Se resolvieron de paso los **dos párrafos incompletos** (`Basado en …`), que ahora sí se traducen
  enteros.

### 3.3 · Selector de idioma dentro de la app

- Nuevo botón de idioma en la cabecera de Android, junto al de tema.
- **Seis opciones** + «Idioma del sistema» (que sigue siendo el comportamiento por defecto).
- La elección se guarda en `SharedPreferences` y se respeta en los siguientes arranques; la app se
  recrea para aplicar el cambio.
- Los números y fechas de la app (`Fmt`) pasan a usar el **idioma activo** y no el del sistema.

### 3.4 · 60 pruebas automáticas del motor

| Batería | Fichero | Pruebas | Ejecuta |
|---|---|---|---|
| Motor de la web | `tools/test_engine.js` | 43 | Extrae el motor real del HTML y lo compara con los valores de referencia |
| Motor de Android | `android/app/src/test/.../CalcEngineTest.kt` | 17 | Mismos valores de referencia sobre `CalcEngine.kt` |

Valores anclados a los **documentos oficiales**, no a la implementación:

- VJ 1880+ / V4201 / 30 °C = **5,21 ml/h** (documento oficial: 5,2).
- VJ 1880+ a 20 °C: **286 h/L** con boquilla de 60 µm y **222 h/L** con 70 µm.
- Gotas/carácter: 5×7 = **13**, 7×9 = **19**, 10×16 = **61**; QR = **220**.
- Gotas/litro: **1 232 240 697** (60 µm) y **883 500 036** (70 µm).
- `K_PUBLISHED = 2,923 / 6,475 = 0,4514285714285714`.
- VJ 1620 HR, VJ 1650 HR y VJ 1650 UHS → núcleo **14 000 h** (TCO v35).
- V4238 y V4251 → **sin datos oficiales**; V4225 = 700 ml, V480 = 600 ml, V401 = 750/5000 ml.
- Propiedades: el consumo de solvente **crece** con la temperatura, los factores ambientales
  **multiplican**, la tinta **no depende** de la temperatura y, sin datos de velocidad, el motor
  devuelve `null` y no un 0 engañoso.

### 3.5 · Un solo comando de validación

- Nuevo **`tools/gen_i18n.py`**: genera los `assets/i18n/*.json` de Android, el bloque `i18n-data`
  de la web y la copia `docs/calculadora.html` **desde `i18n/catalog.json`**. Nadie edita copias a
  mano; `--check` comprueba que estén al día.
- Nuevo **`tools/validar.py`**: ejecuta todo (catálogo, web, HTML, motor de la web y motor de Kotlin).
- `tools/check_i18n.py` ampliado: además de lo anterior, comprueba que **cada párrafo traducible**
  de la web tenga su clave en el catálogo, que los marcadores `**…**` estén **equilibrados** y sea
  el mismo número en los seis idiomas, que no haya claves de más ni de menos en los assets, y avisa
  de las **claves sin uso** en la web y en la app.
- CI: `validar.yml` ejecuta ahora catálogo + web + pruebas del motor; `android.yml` ejecuta las
  **pruebas unitarias de Kotlin** antes de compilar la APK.

### 3.6 · Correcciones detectadas por las nuevas validaciones

- **Textos de Android sin traducir** (hallazgo importante): las etiquetas de varios campos
  (`Caracteres/línea`, `Días/semana`, `Vida en almacén`…), las **cajas informativas** de la base de
  solvente, los **avisos** de relación solvente:tinta, temperatura, caducidad y velocidad, los
  rótulos de los **gráficos** y el detalle del informe se escribían en español literal y **nunca
  pasaban por `I18n`**, así que salían en español dentro de la interfaz ya traducida. Ahora usan las
  mismas plantillas traducidas que la web (`I18n.tf`), lo que además acaba con las diferencias de
  redacción entre plataformas.
- El desplegable «Modelo de temperatura» no marcaba la opción seleccionada en idiomas distintos del
  español, porque se comparaba el texto traducido con el original.
- Literal `${t("Todavía no hay cálculos guardados…")}` visible en la web cuando el historial estaba
  vacío (resto de una plantilla sin procesar).
- Clave del catálogo con `&amp;` que impedía traducir el subtítulo de la portada de la calculadora.
- Etiqueta «golpos/tira» corregida a «golpes/tira».
- **27 claves muertas del catálogo retiradas**: fragmentos de párrafos que se partieron en versiones
  anteriores (`pedidos de consumibles.`, `oficiales de solvente y 535 de velocidad de línea.`…),
  plantillas con los marcadores antiguos (`{base}`, `{cons}`, `{k}`, `{v}`) y etiquetas ya sustituidas.
  Quedan **24 claves de reserva** (textos de interfaz que pueden volver a usarse); se listan con
  `python tools/check_i18n.py --list-unused`.

### 3.7 · Web, portada y notas públicas

- **Calculadora web** (`web/calculadora-offline.html` = `docs/calculadora.html`): 2.2.0 · 22/10/2026,
  informes traducidos y negrita conservada. Sigue siendo **un solo archivo sin recursos externos**.
- **Portada** (`docs/index.html`): banda 2.2.0, sección «Novedades 2.2.0» con cuatro tarjetas, tabla
  de versiones ampliada y contador de pruebas del motor actualizado.
- **Notas públicas** (`docs/notas.html`): ahora con los cuatro bloques
  **Antes / Plan / Hecho / Pendiente** y la tabla de compilación de la 2.2.0.
- **Capturas nuevas** de la app tomadas en el emulador: `docs/assets/idiomas.png` (selector de idioma
  abierto con los seis idiomas) y `docs/assets/idioma-chino.png` (interfaz completa en chino).

### 3.8 · APK 2.2.0

| Campo | Valor |
|---|---|
| Archivo | `ConsumoCIJ-2.2.0.apk` |
| `versionName` / `versionCode` | **2.2.0** / **7** |
| Tamaño | **1,85 MB** |
| SDK | `minSdk 34` · `targetSdk 34` |
| Permisos | **ninguno de red** (solo el permiso interno de AndroidX) |
| Firma | misma clave que la 2.0 y la 2.1 (SHA-256 `c413a1fb…ded63e4`) → **actualiza encima** |
| Compilación | release con R8 y recursos reducidos |
| Pruebas | 17/17 del motor en JVM · instalación y arranque en el emulador Pixel 7 / API 34 |

### 3.9 · Repositorio

- Recuperado el **historial de Git** y convertida la carpeta de trabajo en un repositorio real.
- Nuevas herramientas documentadas, workflows de CI revisados y documentación sincronizada.
- Estado final: **413 cadenas × 6 idiomas**, todo el contenido en UTF-8 y finales de línea
  normalizados según `.gitattributes`.

### 3.10 · Las notas de versión no se borran (garantía permanente)

Hasta ahora, el historial dependía de que nadie borrase ni reescribiese las notas. Eso ya no es una
recomendación: **la validación falla si ocurre**.

- **`docs/notas-indice.json`** registra cada versión con su fecha, los bloques que debe contener y,
  al publicarse, la **huella SHA-256** de sus dos copias (`NOTAS-X.Y.Z.md` y `docs/NOTAS-X.Y.Z.md`).
- **`tools/check_notas.py`** (integrado en `tools/validar.py` y en la CI) comprueba:
  1. que la nota exista en la **raíz y en `docs/`**, con contenido **idéntico**;
  2. que toda nota **congelada** conserve su huella exacta —editarla o borrarla rompe la validación—;
  3. que cada nota tenga los bloques que necesita una IA o una persona para entender el proyecto:
     **cómo estaba antes**, **qué se hizo** y **qué queda pendiente** (las nuevas, además, el plan);
  4. que la versión de `version.json` tenga su nota y que toda versión publicada en `docs/VERSIONES.md`
     esté registrada;
  5. si hay `git`, que **ninguna nota que existió en el historial falte hoy**.
- La 2.1.0 solo estaba en `docs/`; se añadió su copia en la raíz para que ambas carpetas coincidan.
- Para una versión nueva: `python tools/check_notas.py --new 2.3.0` crea la plantilla y
  `--freeze` congela las anteriores al publicar.

> Esta sección se añadió **después** de publicar la 2.2.0, mientras su nota seguía abierta. A partir de
> la próxima entrega, la nota de la 2.2.0 quedará **congelada** y no admitirá cambios: lo que venga
> después se contará en la nota de la versión siguiente.

---

## 4 · Qué queda pendiente a futuro

### 4.1 · Requiere personas (no se puede resolver solo)

1. **Revisión por hablantes nativos** de chino simplificado y portugués de Portugal, sobre todo la
   terminología técnica (墨水, 溶剂, 喷头, 滤芯; «bico» / «bocal»). Las traducciones actuales son
   correctas pero no están validadas en planta.
2. **Contraste en planta** de los consumos estimados frente a los reales, para afinar los factores
   de ajuste ambiental por instalación.

### 4.2 · Seguridad y mantenimiento

3. **Revocar los tokens de publicación** que ya no se usen. La clave de GitHub empleada para
   publicar esta versión **debe revocarse** después de la entrega.
4. **Custodiar el keystore** fuera del repositorio (hoy está excluido por `.gitignore`, pero conviene
   guardarlo en un lugar seguro: sin esa clave no se podrán publicar actualizaciones encima de la
   app instalada).

### 4.3 · Funciones previstas (ROADMAP)

5. **Comparador de escenarios A/B** (dos configuraciones lado a lado).
6. **Planificador de mantenimiento** con fechas de cambio y calendario exportable (`.ics`).
7. **Presets de mensajes** habituales (fecha, lote, EAN-13, QR, DataMatrix).
8. **Importación de la tabla de tintas desde CSV** para actualizarla sin recompilar.
9. **Registro de auditoría** de cálculos exportable.
10. **Widget** de pantalla de inicio y **modo tableta** con panel lateral en Android.
11. **Accesibilidad** reforzada: descripciones ampliadas, navegación por teclado/TV y contraste.
12. **Versión de escritorio** y **API local** (solo si en planta se necesitan).

### 4.4 · Mejoras técnicas concretas

13. Ampliar las pruebas con los **2 400 valores contrastados** de las fichas oficiales (hoy se fijan
    los valores de referencia, no la tabla completa).
14. Revisar el `android.yml` para que publique la **APK de release** como artefacto de la CI.
15. **Congelar la nota de la 2.2.0** al preparar la entrega siguiente (`check_notas.py --freeze`), para
    que quede inmutable como las de 2.1.0 y 2.1.1.

---

## 5 · Cómo comprobar esta entrega

```powershell
# 1. Validación completa (notas de versión, catálogo, web, HTML y motor de la web)
python tools/validar.py

# 1b. Solo las notas de versión (existencia, identidad de copias, huellas y bloques)
python tools/check_notas.py

# 2. Solo el motor de la web (43 pruebas)
node tools/test_engine.js

# 3. Motor de Android (17 pruebas)
$env:JAVA_HOME = "C:\Users\Sr.Hidalgo\AppData\Local\Programs\jdk-17"
& "C:\Users\Sr.Hidalgo\.gradle\wrapper\dists\gradle-8.7-bin\bhs2wmbdwecv87pi65oeuq5iu\gradle-8.7\bin\gradle.bat" `
  -p android testDebugUnitTest

# 4. Compilar la APK release y comprobarla
& "…\gradle.bat" -p android assembleRelease
& "$env:ANDROID_HOME\build-tools\34.0.0\aapt2.exe" dump badging `
  android\app\build\outputs\apk\release\app-release.apk
& "$env:ANDROID_HOME\build-tools\34.0.0\apksigner.bat" verify --print-certs `
  android\app\build\outputs\apk\release\app-release.apk
```

En la web: abrir `docs/calculadora.html`, cambiar el idioma y descargar un **informe TXT** → debe
salir traducido. En Android: pulsar el **selector de idioma** de la cabecera.

---

## 6 · Resumen en una línea

La 2.2.0 deja los **informes traducidos en los seis idiomas**, conserva la **negrita** al traducir,
añade un **selector de idioma en la app**, fija el motor con **60 pruebas automáticas** y publica un
**repositorio profesional** con validación completa en un solo comando.
