# Historial de versiones

Registro detallado de versiones anteriores, cambios de la versión actual y estado de los artefactos.

## Resumen

| Versión | Fecha | Web | APK | Cambio principal |
|---|---|---|---|---|
| **2.1.1** | 2026-10-21 | ✅ | ✅ v2.1.1 (código 6) | Portada y notas públicas al día · versión y compilación visibles · seguimiento |
| 2.1.0 | 2026-10-21 | ✅ | ✅ v2.1.0 (código 5) | Idiomas (ES/EN/PT-BR/PT-PT/IT/ZH) + reporte de errores + HTML corregido |
| 2.0.1 | 2026-10-21 | ✅ | ✅ v2.0.1 | Correcciones de datos: mantenimiento, 70 µm, QR y cartuchos |
| 2.0.0 | 2026-10-21 | ✅ | ✅ v2.0 | Limpieza total: sin minería, sin costes, **100 % offline** |
| 1.2.0 | 2026-10-20 | ✅ | ✅ v1.2 | Cartuchos reales por tinta/modelo + mantenimiento con P/N |
| 1.1.0 | 2026-10-20 | ✅ | ✅ v1.1 | Familias correctas + boquillas por modelo |
| 1.0.0 | 2026-10-19 | ✅ | ✅ v1.0 | Versión inicial funcional (web + Android) |

---

## Versión 2.1.1 — 2026-10-21 (actual)

**Objetivo:** poner al día la web pública y dejar un seguimiento claro de versiones y compilaciones,
sin romper el modo offline ni cambiar el motor de cálculo.

### Cambios
- Portada `docs/index.html` actualizada: seis idiomas, reporte de errores, «Novedades 2.1.1»,
  tabla de versiones completa y enlaces a las notas.
- Nueva página pública `docs/notas.html` (antes / hecho / pendiente + tabla de compilación).
- Corrección de las filas de versión que estaban por error en la tabla de modelos.
- `version.json` en la raíz y en `docs/`: versión, `versionCode`, fecha, artefactos e idiomas.
- Web: pie `v2.1.1 · 21/10/2026`; el informe técnico indica `web v2.1.1`.
- Android: tarjeta «Novedades de esta versión», texto legal `2.1.1 · compilación 6` y el informe
  de error incluye el `versionCode`.
- Catálogo: **364 cadenas** × 6 idiomas (antes 345).

### Estado de los artefactos
| Artefacto | Estado | Notas |
|---|---|---|
| `web/calculadora-offline.html` | ✅ | 0 dependencias · 0 peticiones de red |
| `docs/calculadora.html` | ✅ | Copia idéntica publicada en GitHub Pages |
| `docs/index.html` · `docs/notas.html` | ✅ | Portada y notas públicas |
| `android/` (proyecto) | ✅ | Kotlin + Compose · compila con Gradle 8.7 |
| APK v2.1.1 | ✅ | `versionCode 6` · firmada · sin permisos de red |

### Verificación
- `python tools/check_i18n.py`: catálogo, copias web, assets, JavaScript y HTML correctos.
- APK: `aapt2 dump badging` sin permisos de red y `apksigner verify` con la misma firma que 2.0.
- Emulador Pixel 7 / API 34: instalación y arranque en español e inglés sin fallos.

---

## Versión 2.1.0 — 2026-10-21

**Objetivo:** interfaz multilingüe y reporte de errores sin conexión.

### Cambios
- Seis idiomas con detección automática; catálogo `i18n/catalog.json` con 345 cadenas.
- Reporte de errores por correo o portapapeles, sin permiso `INTERNET`.
- Corrección de dos tarjetas HTML («Exportar resultado» y «Mantenimiento y vida útil»).
- `versionName` 2.1.0 · `versionCode` 5.

---

## Versión 2.0.1 — 2026-10-21

**Objetivo:** corregir los datos detectados al contrastar la app con las fuentes oficiales.

### Cambios
- Mantenimiento: VJ 1620 HR, VJ 1650 HR y VJ 1650 UHS → **14 000 h** (TCO v35).
- Velocidad de VJ 1580 y VJ 1860 con boquilla de 70 µm (clave `1580+`).
- V4238 y V4251 se marcan como **sin datos oficiales** (ficha `#N/D`); el resultado se etiqueta
  como estimación con base MEK.
- Fuentes QR reales (21/25/29/33) de una línea; 220 gotas/carácter es una estimación documentada.
- Android: cartuchos según la tinta al cambiar equipo/boquilla/tinta (1710 y 1580C usan 750 ml de
  solvente).
- Cifras reales de validación: 139 tintas, 734 fichas de solvente y 535 de velocidad.
- `versionName` 2.0.1 · `versionCode` 4.

### Estado de los artefactos
| Artefacto | Estado | Notas |
|---|---|---|
| `web/calculadora-offline.html` | ✅ | 0 dependencias · 0 peticiones de red |
| `docs/calculadora.html` | ✅ | Copia publicada en GitHub Pages |
| `android/` (proyecto) | ✅ | Kotlin + Compose · compila con Gradle 8.7 |
| APK v2.0.1 | ✅ | firmado · sin permisos de red |

### Verificación
- 734 fichas de solvente y 535 de velocidad contrastadas (máx. 0,05 ml/h y ±1 en cps/nominal,
  dentro del redondeo de las fichas).
- APK: `aapt2 dump badging` sin permisos de red y `apksigner verify` correcto.
- Web: VJ 1880+ / V4201 a 30 °C = 5,21 ml/h (documento oficial: 5,2).

---

## Versión 2.0.0 — 2026-10-21

**Objetivo:** dejar la herramienta como calculadora técnica pura, sin funciones económicas ni módulos
experimentales, y garantizar su funcionamiento sin internet.

### Cambios
- **Eliminado** el módulo experimental de apoyo/minería (código, interfaz, consentimiento y textos).
- **Eliminados** los permisos `INTERNET` y `ACCESS_NETWORK_STATE`.
- **Eliminado** el apartado de precios y costes (por cartucho, por período y por 1 000 impresiones).
- **Eliminados** los avisos legales extensos; se sustituyen por notas técnicas de fuentes de datos.
- **Renombradas** las secciones: «Mantenimiento y ajustes avanzados» y «Fuentes de datos y notas técnicas».
- `versionName` 2.0 · `versionCode` 3.

### Estado de los artefactos
| Artefacto | Estado | Notas |
|---|---|---|
| `web/calculadora-offline.html` | ✅ | 170 KB · 0 dependencias · 0 peticiones de red |
| `docs/calculadora.html` | ✅ | Copia publicada en GitHub Pages |
| `android/` (proyecto) | ✅ | Kotlin + Compose · compila con Gradle 8.7 |
| APK v2.0 | ✅ | 1,77 MB · firmado · sin permisos de red |

### Verificación
- Motor: pruebas contra documentos oficiales (la cifra «28/28» no era reproducible: el repositorio
  no incluye esa batería de pruebas).
- APK: `aapt2 dump badging` sin permisos de red; pantallas `small…xlarge`; densidades 160…65534.
- Emulador Pixel 7 (API 34): instalación correcta, arranque 1,7–2,2 s, sin errores en logcat.
- Web: cálculo VJ 1880+ / V4201 a 30 °C = **5,21 ml/h** (documento oficial: 5,2).

---

## Versión 1.2.0 — 2026-10-20

- Cartuchos reales por tinta y modelo (142 tintas) con solvente asociado.
- Mantenimiento con P/N e intervalo oficial por modelo.
- Notación compacta de cifras grandes (M/B).
- Corrección del historial (las entradas antiguas se sobrescribían).
- Incluía un módulo opcional de apoyo con consentimiento (retirado en 2.0 por decisión del proyecto).

---

## Versión 1.1.0 — 2026-10-20

- Familias corregidas: **Simplicity** (1240, 1280, 1580, 1580+, 1580C, 1880, 1880+, 1880 UHS, 1880 HR)
  y **Serie 1000VJ** (1040, 1210, 1220, 1510…1710, 1860, UHS y HR).
- Modelos unificados con **boquillas según el modelo** (40/50/60/70 µm).
- En 1880/1880+ el cambio de boquilla modifica el consumo de solvente (286 h/L vs 222 h/L a 20 °C).
- Interfaz adaptativa de 1 y 2 columnas y temas Claro/Oscuro/AMOLED.

---

## Versión 1.0.0 — 2026-10-19

- Primera versión completa: calculadora web offline y app Android nativa.
- Motor de solvente, tinta y velocidad con gráficos, exportación e historial.
- Validación inicial contra 150 PDFs de solvente y 40 de velocidad.
