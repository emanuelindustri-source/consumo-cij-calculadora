# Notas de la versión 2.4.0

**Producto:** **Consumo CIJ** — calculadora de consumo de tinta y solvente para impresoras de
inyección continua (CIJ) de Videojet, en web (archivo único) y app Android nativa.

**Fecha:** 2026-11-04 — **Compilación:** 9 — **Versión anterior:** 2.3.0 (compilación 8)

Este documento deja constancia de **cómo se encontró el proyecto**, **qué se iba a hacer**,
**qué se consiguió** y **qué queda pendiente**. Las notas de las versiones anteriores
([2.3.0](docs/NOTAS-2.3.0.md), [2.2.0](docs/NOTAS-2.2.0.md), [2.1.1](docs/NOTAS-2.1.1.md) y
[2.1.0](docs/NOTAS-2.1.0.md)) se conservan intactas y congeladas: no se borran ni se reescriben.

---

## 1 — Cómo se encontró el proyecto (estado a la entrada de la 2.4.0)

La 2.3.0 quedó publicada con el sistema de actualizaciones verificado, el historial corregido y la
publicación automatizada. Al revisar los datos con las hojas oficiales aparecieron tres problemas:

| Problema | Consecuencia |
|---|---|
| **Cartuchos de 5000 ml en equipos que no los usan.** El TCO v35 marca seis modelos como «750mL Only» (1210, 1220, 1240, 1280, 1510 y 1580), pero la app mostraba los tamaños del catálogo de tintas sin filtrarlos por modelo. | Un VJ 1510 con V401 ofrecía 5000 ml; un 1580 ofrecía 1000 ml; y las configuraciones guardadas conservaban esos tamaños. |
| **La validación de solvente no estaba en el repositorio.** La documentación citaba 734 fichas y 11.744 valores, pero el verificador era externo y no se podía repetir. | Cualquier cambio en el motor podía romper los datos sin que la CI lo detectara. |
| **El aviso de actualización prometía «ver las novedades» y no las mostraba.** Solo enseñaba versión, fecha y tamaño; además, «Omitir esta versión» era para siempre. | El usuario actualizaba a ciegas y quien omitía una versión no volvía a recibir el aviso. |

La 2.3.0 también dejó dos deudas: la tabla de velocidad seguía sin un verificador en el repositorio
y la lista de tintas de la Serie 1000VJ no incluía algunas tintas V42xx que el TCO asigna a esos
modelos.

---

## 2 — Qué se iba a hacer (plan de la 2.4.0)

1. **Corregir los cartuchos**: respetar la lista «750mL Only» del TCO v35 y no ofrecer 1 L ni 5 L
   donde no corresponden; corregir también las configuraciones guardadas.
2. **Validar el solvente**: extraer las fichas oficiales por equipo/tinta, guardar un fixture en el
   repositorio y comprobar **todos** los valores en la CI (web y Android).
3. **Mejorar el aviso**: mostrar las novedades por idioma, recordar a los 14 días la versión omitida
   y soportar actualizaciones obligatorias.
4. **Copia de seguridad**: exportar e importar configuración, historial, tema e idioma en JSON.
5. **Documentar** el estado de la 2.3.0, la 2.4.0 y el plan de la 2.5.0, sin tocar las notas antiguas.

---

## 3 — Qué se consiguió (la 2.4.0)

### 3.1 Cartuchos correctos: «750 mL Only»

- Se añadió la lista oficial del TCO v35 con los **seis modelos que solo admiten 750 ml**:
  **VJ 1210, 1220, 1240, 1280, 1510 y 1580**.
- La app y la web filtran ahora los tamaños por **modelo + tinta**:
  - VJ 1510 + V401 → **750 ml** (antes 750/5000).
  - VJ 1580 + V4201 → **750 ml** (antes 750/1000).
  - VJ 1520 + V401 → **750/5000** (correcto: el TCO lo permite).
  - VJ 1880+ + V4201 → **750/1000** (sin cambios).
  - VJ 1620 UHS + V505 → **750/5000** (correcto).
  - VJ 1710 + V480 → **600 ml de tinta / 750 ml de solvente**.
- Si la tinta guardada no pertenece al grupo del modelo, se usan los tamaños del modelo, nunca los
  de otra familia.
- **Configuraciones antiguas corregidas al arrancar**: un 5000 ml guardado en un equipo 750-only se
  ajusta a 750 ml sin que el usuario tenga que tocar nada.
- La app muestra un aviso: «Este modelo usa exclusivamente cartuchos de 750 ml (CIJ TCO Calculator
  v35)».
- Pruebas nuevas en web (8) y Android (5) que fijan estos casos.

### 3.2 Validación del solvente: 800 fichas y 12.800 valores

- Se extrajeron las **800 fichas** de solvente (PDF por equipo/tinta, 16 temperaturas de 10 a 40 °C)
  a `tools/fixtures/solvent_cards.json`.
- **`tools/test_solvent.js`** ejecuta el motor real de la web contra las 800 fichas:
  **12.800/12.800 valores** dentro de 0,06 ml/h.
- **`SolventCardsTest.kt`** hace la misma comprobación contra el motor de Android, leyendo el mismo
  fixture: **12.800/12.800**.
- La validación se ejecuta en `tools/validar.py` y en la CI, así que ya no depende de scripts
  externos ni de los PDF.
- Las dos fichas que el modelo original devuelve `#N/D` (V4238 y V4251) se excluyen y la app sigue
  avisando de que son estimaciones.

### 3.3 Aviso de actualización con novedades y recordatorio

- `version.json` incluye **`highlights`** en los seis idiomas; el diálogo y la notificación muestran
  los puntos destacados antes de actualizar.
- **Recordatorio a los 14 días**: una versión omitida vuelve a avisarse pasado ese plazo.
- **`mandatory`**: si una versión se marca como obligatoria, no se puede omitir y el diálogo lo
  indica; nunca se instala sin confirmación del usuario.
- El aviso sigue siendo opcional y desactivable; los cálculos no usan red.

### 3.4 Copia de seguridad

- **Exportar** configuración, historial (hasta 20 cálculos), tema e idioma a un JSON con el selector
  de archivos del sistema (SAF, sin permisos de almacenamiento).
- **Importar** el archivo en otro dispositivo o tras un borrado; se validan modelo, tinta, base,
  fuente y todos los rangos numéricos, y se corrigen los tamaños de cartucho según el modelo.
- Errores controlados: archivo que no es de Consumo CIJ, JSON roto o valores fuera de rango.

### 3.5 Versión, pruebas y documentación

- `versionName 2.4.0` · `versionCode 9`.
- Catálogo: **479 cadenas** × 5 idiomas (antes 466).
- Pruebas: **51 web + 32 Android = 83** (además de los 12.800 valores de solvente).
- `tools/check_update.py` valida también `highlights` (los seis idiomas, 2–6 puntos por idioma).
- `tools/validar.py` incorpora la validación de solvente.
- README, CHANGELOG, ROADMAP, VERSIONES, DATOS, portada y notas públicas actualizados.

### 3.6 Auditoría de velocidad (trabajo iniciado)

Se comparó la tabla de velocidad con `CIJ Speeds.xlsx`. El archivo usa claves de modelo distintas
(`1580/1860`, `1880`) y se detectaron **4 diferencias** en `7x9 Single` y `5x5 Twin` (1240/1280) y en
`9x12 Twin`/`5x5 Four` (1710). **No se cambiaron los datos** porque la fuente principal de la app es
el `CIJ Line Speed Calculator v9.7` y hay que decidir cuál es la hoja autoritativa. Queda como
pendiente para la 2.4.1/2.5.0, junto con el verificador de las 535 fichas de velocidad.

---

## 4 — Qué queda pendiente a futuro (2.5.0)

- **Verificador de velocidad**: extraer las 535 fichas comparables y resolver las 4 diferencias
  detectadas antes de fijar la tabla.
- **Tintas V42xx en la Serie 1000VJ**: el TCO asigna V4214…V4264 a modelos como 1210/1510; la app
  solo lista V4xx en ese grupo. Revisar la lista completa con la hoja oficial.
- **V457 con V725**: el TCO indica 740 ml frente a 750 ml del resto de V725; confirmar con Videojet.
- **Revisión nativa** de chino simplificado y portugués de Portugal.
- **Informe PDF** y **widget** de consumo mensual.
- **Copia de seguridad también en la web** (la web ya exporta configuración; falta historial).
- **Actualización incremental** si la APK crece.
- **Copia segura del keystore** y revocación de tokens antiguos.

---

## 5 — Cómo comprobar esta entrega

```bash
python tools/validar.py        # notas, catálogo, web, solvente, motores y versión
node tools/test_solvent.js     # 800 fichas · 12.800 valores
node tools/test_engine.js      # 51 pruebas de la web
cd android && gradle testDebugUnitTest   # 32 pruebas (incluye 12.800 valores)
python tools/check_update.py   # version.json, highlights y red permitida
```

Comprobaciones realizadas el 2026-11-04:

- `tools/test_solvent.js`: **12.800/12.800** valores correctos.
- `tools/test_engine.js`: **51/51**.
- `gradle testDebugUnitTest`: **32/32**, incluida `SolventCardsTest` (800 fichas).
- `tools/validar.py`: validación completa correcta.
- APK 2.4.0 compilada, firmada con la misma clave que la 2.3.0 y probada en el emulador
  Pixel 7 / API 34.
- Aviso de actualización probado entre la 2.3.0 y la 2.4.0 con descarga, SHA-256 e instalación.

---

**Consumo CIJ 2.4.0** — compilación **9** · 2026-11-04 · datos verificados, cartuchos correctos y
actualizaciones que explican qué cambian.
