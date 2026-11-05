# Notas de la versión 2.4.1

**Producto:** **Consumo CIJ** — calculadora de consumo de tinta y solvente para impresoras de
inyección continua (CIJ) de Videojet, en web (archivo único) y app Android nativa.

**Fecha:** 2026-11-05 — **Compilación:** 10 — **Versión anterior:** 2.4.0 (compilación 9)

Este documento deja constancia de **cómo se encontró el proyecto**, **qué se iba a hacer**,
**qué se consiguió** y **qué queda pendiente**. Las notas de las versiones anteriores
([2.4.0](docs/NOTAS-2.4.0.md), [2.3.0](docs/NOTAS-2.3.0.md), [2.2.0](docs/NOTAS-2.2.0.md),
[2.1.1](docs/NOTAS-2.1.1.md) y [2.1.0](docs/NOTAS-2.1.0.md)) se conservan intactas y congeladas.

---

## 1 — Cómo se encontró el proyecto (estado a la entrada de la 2.4.1)

La 2.4.0 dejó el catálogo con 139 tintas, los cartuchos «750 mL Only» corregidos y el solvente
validado con 800 fichas. Faltaba contrastar el catálogo de tintas con las **hojas de datos de
seguridad (SDS)** que Videojet publica por producto.

La carpeta `ficha de tintas` contenía **493 PDF**: 492 SDS y un catálogo comercial. Cada SDS
identifica un producto (`V4201-D`, `V401-K`, `V480-C`…) y su variante de envase. Al contrastarla con
el TCO v35 apareció:

| Hallazgo | Detalle |
|---|---|
| **Una tinta del TCO parecía faltar** | **V4287** (700 ml, base MPK, V7250): al revisar la fila completa se vio que pertenece a **MI 9029**, no a un equipo Videojet. |
| **27 tintas de las SDS no están en el TCO v35** | Productos nuevos o variantes sin matriz modelo/contenedor; no se pueden calcular con garantía. |
| **El resto del catálogo coincide** | Las tintas comunes tienen la misma base y los mismos tamaños que el TCO. |
| **Erratas en español e italiano** | «El consumo de make-up **lo influyen**», «**medimiento**», «ciclo di **misura**», «1 L **o** 5 L». |

---

## 2 — Qué se iba a hacer (plan de la 2.4.1)

1. **Validar** la carpeta de fichas contra el catálogo y el TCO v35.
2. **Añadir** la tinta que faltaba y documentar las que quedan fuera del TCO.
3. **Corregir la gramática** en español e italiano y revisar las traducciones nuevas.
4. **Documentar** la validación en `docs/TINTAS.md` y con una herramienta repetible.
5. **Actualizar** notas, páginas, APK y repositorio sin tocar las notas antiguas.

---

## 3 — Qué se consiguió (la 2.4.1)

### 3.1 Validación de las fichas de tintas

- **493 PDF revisados**: 492 SDS + 1 catálogo comercial.
- **188 productos `V*`**: 152 de la serie de tintas (143 confirmadas como *Ink* + 9 sin tipo
  legible) y **36 make-up**.
- **139 tintas Videojet en el TCO v35** y **139 en la app**; V4287 pertenece a MI 9029 y no se añade.
- **0 diferencias** de base y tamaño en las tintas comunes.
- **27 tintas de las SDS fuera del TCO** documentadas y **no calculadas a ciegas**.
- **15 tintas del TCO sin ficha** en esta carpeta: se mantienen.
- **3 variantes de make-up** (`V705A`, `V7201M`, `V7207M`) documentadas, no añadidas.
- Envases confirmados: `-D` 750 ml, `-L` 1 000 ml, `-K` 5 000 ml, `-C` 600 ml, `-E` 700 ml,
  `-R` 750 ml.

### 3.2 V4287: identificada, no añadida

El TCO tiene una fila con **V4287** (700 ml, MPK, V7250 750 ml) que parecía la tinta que faltaba. Al
revisar la fila completa se comprobó que pertenece a la impresora **MI 9029** (`A: MI 9029`, `B: MI`),
no a un equipo Videojet. Además, la base **MPK** no tiene factor en el Makeup Usage Model v14, así que
el motor la calculaba como estimación con MEK.

**Decisión:** no se añade. El catálogo Videojet queda con **139 tintas**, las que el TCO asigna a las
familias Simplicity y Serie 1000VJ.

### 3.3 Gramática y traducciones

- **Español**: «El consumo de make-up **lo influyen**» → «**está influido por**»; «ciclo de
  **medimiento**» → «ciclo de **medición**».
- **Italiano**: «ciclo di **misura**» → «ciclo di **misurazione**»; «1 L **o** 5 L» → «1 L **né**
  5 L».
- **Chino**: «condensador activo/pasivo» → «主动/被动冷凝器» (más preciso).
- Revisadas las cadenas nuevas de 2.4.0/2.4.1 en los seis idiomas.

### 3.4 Herramientas y documentación

- **`tools/check_fichas.py`**: compara los nombres de las SDS con el catálogo de la web, sin
  dependencias. No se ejecuta en la CI porque la carpeta de fichas no está en el repositorio.
- **`docs/TINTAS.md`**: informe completo de la validación, con la lista de productos fuera del TCO y
  la decisión tomada para cada uno.
- `README`, `CHANGELOG`, `ROADMAP`, `VERSIONES`, `DATOS`, portada y notas públicas actualizados.
- Pruebas: **52 web + 33 Android = 85** (además de los 12.800 valores de solvente).

### 3.5 Versión

- `versionName 2.4.1` · `versionCode 10`.
- Catálogo de idiomas: **479 cadenas** × 5 idiomas.
- APK firmada con la misma clave que la 2.4.0.

---

## 4 — Qué queda pendiente a futuro (2.5.0)

- **Actualizar el TCO**: las 27 tintas nuevas de las SDS necesitan la matriz modelo/contenedor para
  poder calcularse. No se añaden a ciegas.
- **Verificador de velocidad**: las 535 fichas comparables y las **4 diferencias** detectadas con
  `CIJ Speeds.xlsx`.
- **Tintas V42xx en la Serie 1000VJ**: revisar la lista completa con la hoja oficial.
- **V457 con V725** (740 ml): confirmar con Videojet.
- **Revisión nativa** de chino simplificado y portugués de Portugal.
- **Informe PDF**, **widget** de consumo mensual y copia de seguridad en la web.
- **Copia segura del keystore** y revocación de tokens antiguos.

---

## 5 — Cómo comprobar esta entrega

```bash
python tools/validar.py
node tools/test_solvent.js
python tools/check_fichas.py --dir "C:\Users\Sr.Hidalgo\Documents\OneDrive_39_17-8-2026\ficha de tintas"
cd android && gradle testDebugUnitTest
python tools/check_update.py
```

Comprobaciones realizadas el 2026-11-05:

- `check_fichas.py`: 492 SDS, 188 productos `V*`, V4287 identificada como MI 9029 y 27 tintas fuera del TCO.
- `test_engine.js`: **52/52**.
- `test_solvent.js`: **12.800/12.800**.
- `gradle testDebugUnitTest`: **33/33**.
- `validar.py`: validación completa correcta.
- APK 2.4.1 compilada, firmada y probada en el emulador Pixel 7 / API 34.

---

**Consumo CIJ 2.4.1** — compilación **10** · 2026-11-05 · catálogo de tintas validado, V4287
descartada por pertenecer a MI 9029 y textos revisados en los seis idiomas.
