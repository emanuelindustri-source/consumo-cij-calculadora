# Datos, fórmulas y validación

Documento técnico con el origen de los datos, las fórmulas implementadas y las pruebas de validación.

## 1. Fuentes

| Modelo | Archivo de origen | Uso en la calculadora |
|---|---|---|
| **CIJ Makeup Usage Model v14** | `CIJ Makeup Usage Model v14.xlsx` + fichas PDF por equipo/tinta | Consumo de solvente según temperatura y base química |
| **CIJ TCO Calculator v35** | `CIJ TCO Calculator_v35.xlsm` (hojas *SetupSheet*, *Maintenance Setup*) | Gotas/litro, gotas/carácter, contenedores por tinta, mantenimiento |
| **CIJ Line Speed Calculator v9.7** | `CIJ Line Speed Calculator v9.7.xlsx` (hoja *WkSheet*) | Caracteres/seg, velocidad nominal, golpes por carácter |

## 2. Fórmulas

### 2.1 Solvente (make-up)

```
ml/h = (1000 / horas_por_litro_20 °C) × factor_base × K × e^(k × T) × factor_ajuste
```

- `horas_por_litro_20 °C`: dato del equipo (p. ej. 286 h/L en 1880+ 60 µm; 222 h/L en 70 µm).
- `factor_base` y `k`: según la base química (MEK, MIPK, Methanol, Ethanol, Ethanol/Acetona, …).
- `K = 0,4514285714` (modelo publicado) y `k` = factor exponencial de la base.
- `T`: temperatura ambiente en °C. `factor_ajuste`: humedad, recirculación, condensador, boquilla, etc.

### 2.2 Tinta

```
gotas/mensaje = gotas_por_carácter × caracteres × líneas × stroke
ml/impresión = (gotas/mensaje ÷ gotas_por_litro) × 1000
ml/h          = ml/impresión × impresiones/min × 60 × eficiencia
```

- `gotas_por_carácter` según la matriz: 5×5 = 11 · 4×7 = 12 · 5×7 = 13 · 7×9 = 19 · 9×12 = 22 ·
  10×16 = 61 · 16×24 = 102 · 25×34 = 253 (4×5 estimado = 10).
- `gotas_por_litro` según boquilla (60 µm ≈ 1,232 × 10⁹ gotas/L; 50 µm ≈ 2,002 × 10⁹; 40 µm ≈ 3,656 × 10⁹;
  70 µm ≈ 0,884 × 10⁹).

### 2.3 Velocidad de línea

```
velocidad_máx (m/min) = (caracteres/seg ÷ caracteres/línea) × ancho_mm × 0,06
ancho_mín (mm)        = (velocidad ÷ 0,06) ÷ (caracteres/seg ÷ caracteres/línea)
máx_caracteres        = (caracteres/seg × ancho_mm) ÷ (velocidad ÷ 0,06)
cpi = caracteres ÷ (ancho_mm ÷ 25,4)      dpi = (caracteres × (ancho_matriz + 1)) ÷ (ancho_mm ÷ 25,4)
```

## 3. Validación automática

Se contrastaron las tablas oficiales con la calculadora: se abre cada ficha, se extrae la tabla de 16
temperaturas (10–40 °C) y los campos «horas por litro» y «horas por cartucho», y se comparan con el motor.

| Prueba | Muestras | Resultado |
|---|---|---|
| Tabla de solvente 10–40 °C | 736 fichas (734 legibles) · 11 744 valores | **734/734 · coincidencia total** |
| Velocidad, cpi y dpi | 541 fichas leídas · 535 comparables | **535/535** |
| Modelo de tinta (gotas/mensaje e impresiones/cartucho) | 2 escenarios del TCO | **Coincidencia exacta** |

> Desviación máxima observada: **0,05 ml/h** en solvente y **±1** en caracteres/s y velocidad nominal,
> dentro del redondeo a un decimal de las fichas. Los verificadores todavía no forman parte del repositorio
> (ver `ROADMAP.md`).

Casos de referencia documentados:

| Equipo | Tinta | Temperatura | Valor calculado | Documento oficial |
|---|---|---|---|---|
| VJ 1880+ (60 µm) | V4201 (MEK) | 10 °C | 2,35 ml/h | 2,3 ml/h |
| VJ 1880+ (60 µm) | V4201 (MEK) | 20 °C | 3,50 ml/h | 3,5 ml/h |
| VJ 1880+ (60 µm) | V4201 (MEK) | 30 °C | 5,21 ml/h | 5,2 ml/h |
| VJ 1880 (60 µm) | V4257 (MIPK) | 30 °C | 3,19 ml/h | 3,2 ml/h |
| VJ 1210 | V401 (MEK) | 30 °C | 11,20 ml/h | 11,2 ml/h |
| VJ 1710 | 5×7 · 12 car · 11 mm | — | 73,3 m/min | 73,3 m/min |

## 4. Cartuchos (ejemplos reales)

| Tinta | Equipo típico | Contenedor de tinta | Solvente | Contenedor de solvente |
|---|---|---|---|---|
| V401 | 1210, 1510, 1620… | 750 / 5 000 ml | V701 | 750 / 5 000 ml |
| V4201 | 1880, 1880+ | 750 / 1 000 ml | V7201 | 750 / 1 000 ml |
| V4225 | 1580C | **700 ml** | V7220 | 750 ml |
| V4257 | varios | 750 / 1 000 ml | V7257 | 750 / 1 000 ml |
| V480 | 1710 | **600 ml** | V720 | 750 ml |

La app y la web muestran automáticamente los tamaños disponibles según la tinta elegida.

## 5. Mantenimiento (resumen)

| Equipo | Pieza | Intervalo | P/N |
|---|---|---|---|
| VJ 1240 / 1280 | Service Module 1 | 3 000 h | 383556 |
| VJ 1580 / 1580+ / 1580C | Service Module 1 | 5 000 h | 397550 |
| VJ 1880 / 1880+ / UHS / HR | Service Module 1 | 12 meses (≈ 8 760 h) | 611554 |
| VJ 1210 | Core | 6 000 h | — |
| VJ 1220 / 1510 | Core | 9 000 h | SP392167 / 399070 |
| VJ 1520 / 1550 / 1610 / 1610 DH | Core | 12 000 h | según modelo |
| VJ 1620 / 1650 | Core | 14 000 h | SP392169 / SP392128 |
| VJ 1620/1650 UHS y HR | Core | **14 000 h** | 395965 / 395964 |
| VJ 1530 / 1560 / 1630 / 1660 | Core | 63 meses (≈ 45 360 h) | 397550 / 397552 |
| VJ 1710 | Core (+ PV Module) | 4 000 h (+ 15 000 h) | 395623 / 395624 |
| VJ 1860 | Service Module 1 + 2 | 5 000 h + 12 000 h | 611220 / 611170 |

> En VJ 1880 HR, 1880 UHS, 1880+, 1580+ y 1580C el TCO no publica P/N del módulo de servicio: la app
> lo marca como orientativo (`pnEstimated`).

## 6. Limitaciones

- Los valores son **estimaciones** basadas en modelos internos; el consumo real varía con la humedad,
  la ventilación, el mantenimiento, el condensador, la recirculación y el sustrato.
- Las tablas de velocidad cubren las combinaciones equipo/fuente documentadas; si falta una combinación,
  la app lo indica y mantiene el cálculo de consumos.
- Las matrices 4×5 y QR usan valores estimados (no figuran en el modelo de gotas por carácter).
- V4238 (MEK / Methanol) y V4251 (Methanol / Water) no tienen factor oficial: la ficha del Makeup
  Usage Model v14 devuelve `#N/D`. La app lo avisa y etiqueta el resultado como estimación con base MEK.
- El TCO asigna **740 ml** al solvente V725 de la tinta V457, mientras el resto de V725 usa 750 ml:
  pendiente de confirmar con Videojet.
