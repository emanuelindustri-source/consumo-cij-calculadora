# Notas de la versión 2.5.0

**Producto:** **Consumo CIJ** — calculadora de consumo de tinta y solvente para impresoras de
inyección continua (CIJ) de Videojet, en web (archivo único) y app Android nativa.

**Fecha:** 2026-11-05 — **Compilación:** 11 — **Versión anterior:** 2.4.1 (compilación 10)

Este documento deja constancia de **cómo se encontró el proyecto** (antes), **qué se planificó y
cómo se hizo** (durante), **qué se consiguió** (después) y **qué queda pendiente**. Las notas de las
versiones anteriores ([2.4.1](docs/NOTAS-2.4.1.md), [2.4.0](docs/NOTAS-2.4.0.md),
[2.3.0](docs/NOTAS-2.3.0.md), [2.2.0](docs/NOTAS-2.2.0.md), [2.1.1](docs/NOTAS-2.1.1.md) y
[2.1.0](docs/NOTAS-2.1.0.md)) se conservan intactas y congeladas.

---

## 1 · Antes — cómo se encontró el proyecto (estado a la entrada de la 2.5.0)

La 2.4.1 cerró el catálogo de tintas con **139 tintas Videojet** validado contra 493 fichas de
seguridad y el TCO v35. La calculadora era técnicamente correcta, pero **hablaba en mililitros**:

| Lo que había | Lo que faltaba para ventas |
|---|---|
| Consumo en ml/hora, ml/mes y cartuchos/mes. | **Precio** de cada cartucho y **moneda**. |
| Coste de mantenimiento en horas de núcleo. | **Costo por hora, día, mes y año** en dinero. |
| Comparación de modelos dentro de la app. | **Comparador** con la competencia u otros equipos Videojet. |
| Informes TXT/CSV con consumo. | Costos dentro del informe y **exportación** de la comparación. |

Un comercial podía decir «este equipo gasta 1,8 cartuchos de solvente al mes», pero no «eso son
**X** al mes y **Y** por cada 1.000 códigos». Tampoco podía sentar en la misma mesa el cálculo
actual y una oferta de la competencia sin abrir una hoja de cálculo aparte.

Además, el catálogo tenía **479 cadenas** y las traducciones de la interfaz seguían siendo solo
técnicas: no existía vocabulario de precios, moneda ni comparación en en / pt-BR / pt-PT / it / zh.

---

## 2 · Durante — plan de la 2.5.0

1. **Calculadora de costos para ventas** en la web y en la app:
   - moneda (USD, EUR, MXN, COP, BRL, ARS, CLP, PEN, GBP, CNY);
   - precio del cartucho de tinta y del cartucho de solvente;
   - costo por hora, día, mes y año, costo por 1.000 códigos y precio por litro;
   - reparto tinta / solvente y costo total de propiedad (equipo + consumibles).
2. **Comparador de equipos y competencia**: cálculo actual + hasta 4 escenarios guardados, con
   diferencias mensuales y anuales, mejor costo resaltado y exportación CSV.
3. **Informes** TXT y CSV con el bloque de costos en las dos plataformas.
4. **Traducciones** de todas las cadenas nuevas a los seis idiomas y revisión de gramática.
5. **Actualizar** repositorio, GitHub Pages, web offline, APK y documentación, sin tocar las notas
   antiguas.

Criterios de diseño acordados:

- Los precios **nunca salen del dispositivo**: no se envían a ningún servidor.
- El comparador admite **datos manuales** de la competencia (ml/mes, precio/litro, impresiones/mes)
  porque no se dispone de su motor de consumo.
- La comparación es honesta: se avisa si los escenarios usan **monedas distintas** y se recuerda
  usar la misma producción.
- El TCO v35 sigue siendo la **fuente de verdad** de tamaños de cartucho; no se inventan envases.

---

## 3 · Después — qué se consiguió (la 2.5.0)

### 3.1 Calculadora de costos (web y Android)

- **Moneda** con formato local en los seis idiomas (`Intl.NumberFormat` en web,
  `NumberFormat` + `Currency` en Android).
- **Precio por cartucho** de tinta y de solvente; el precio por litro se deduce del tamaño real
  del cartucho del modelo elegido (TCO v35).
- **Resultados**: costo/hora, costo/día, costo/mes, costo/año, costo por 1.000 códigos, reparto
  porcentual tinta/solvente y **TCO** = consumibles a *N* años + precio del equipo.
- Campos nuevos en `CalcInput`: `currency`, `inkPrice`, `muPrice`, `equipPrice`, `years`;
  se guardan con el resto de la configuración y entran en la copia de seguridad.

### 3.2 Comparador de equipos y competencia

- **Primera columna fija**: el cálculo actual, siempre visible.
- **Escenarios Videojet**: una foto del cálculo actual con su modelo y su tinta.
- **Escenarios de competencia**: nombre, tinta y solvente consumidos al mes, precio por litro y
  impresiones al mes (opcional).
- **Comparación**: costo mensual, costo anual, costo por 1.000 códigos, consumo y **diferencia**
  frente al cálculo actual; se resalta el **mejor costo mensual**.
- **Persistencia**: `localStorage` (`vj_calc_scenarios_v1`) en la web y `SharedPreferences` en
  Android; máximo **4 escenarios**.
- **Exportación CSV** del comparador en la web y borrado individual o total.

### 3.3 Informes

- **Web**: `buildReport` (TXT) y `buildCsv` incluyen moneda, precios, costo de tinta, costo de
  solvente, costo total, costo por 1.000 códigos y TCO cuando hay equipo.
- **Android**: `Report.csv` y `Report.txt` añaden el bloque **COSTOS** con las mismas cifras.

### 3.4 Idiomas y gramática

- Catálogo: **554 cadenas × 5 idiomas** (75 nuevas).
- Vocabulario nuevo revisado en español, inglés, portugués de Brasil, portugués de Portugal,
  italiano y chino: *Moneda / Currency / Moeda / Valuta / 货币*, *Costo total de propiedad*,
  *Comparador*, *Competencia*, *Diferencia mensual*, etc.
- Se añadió la cadena `año` que faltaba para los informes y se corrigieron textos de la interfaz
  que mezclaban español con traducciones.

### 3.5 Novedades en la app

- La tarjeta **«Novedades de esta versión»** se actualizó: antes seguía mostrando los avisos de la
  2.4.x (solvente validado, cartuchos «750 mL Only», copia de seguridad) y ahora presenta la
  calculadora de costos, el comparador, el costo total de propiedad y los informes con costos.
- Se retiraron del catálogo las cuatro cadenas de noticias antiguas, que quedaron sin uso, y se
  añadieron las cuatro nuevas con su traducción a los cinco idiomas.

### 3.6 Pruebas

- **Web**: 64 pruebas del motor (13 nuevas de costos: precio/litro, costo/hora, costo por 1.000
  códigos, TCO y reparto) + 12.800 valores de solvente.
- **Android**: 36 pruebas (2 nuevas de costos, 1 de ida y vuelta de precios y comparador en la
  copia de seguridad y 1 del CSV del comparador).
- `check_i18n.py`: catálogo, párrafos, claves JS, paridad web/docs y sintaxis, todo correcto.

### 3.7 Versión

- `versionName 2.5.0` — `versionCode 11`.
- Web `WEB_VERSION = "2.5.0"` y pie de página actualizado.
- APK firmada con la misma clave que la 2.4.1.

---

## 4 · Pendiente — qué queda a futuro

- **Actualizar el TCO**: las 27 tintas de las SDS que no están en el TCO v35 siguen sin matriz
  modelo/contenedor; no se añaden a ciegas.
- **Verificador de velocidad**: 535 fichas comparables y 4 diferencias con `CIJ Speeds.xlsx`.
- **Tintas V42xx en la Serie 1000VJ**: revisar la lista completa con la hoja oficial.
- **V457 con V725** (740 ml): confirmar con Videojet.
- **Revisión nativa** de chino simplificado y portugués de Portugal.
- **Informe PDF**, **widget** de consumo mensual y copia de seguridad en la web.
- **Copia segura del keystore** y revocación de tokens antiguos.

---

## 5 · Cómo comprobar esta entrega

```bash
python tools/validar.py
node tools/test_engine.js
node tools/test_solvent.js
cd android && gradle testDebugUnitTest
python tools/check_update.py
```

Comprobaciones realizadas el 2026-11-05:

- `test_engine.js`: **64/64**.
- `test_solvent.js`: **12.800/12.800**.
- `gradle testDebugUnitTest`: **35/35**.
- `check_i18n.py`: **554 cadenas × 5 idiomas**, sin fallos.
- `check_notas.py`: notas anteriores intactas; esta nota registrada y congelada al publicar.
- APK 2.5.0 compilada, firmada y probada en el emulador Pixel 7 / API 34.

---

**Consumo CIJ 2.5.0** — compilación **11** · 2026-11-05 · calculadora de costos para ventas,
comparador de equipos y competencia, informes con costos y 554 cadenas en seis idiomas.
