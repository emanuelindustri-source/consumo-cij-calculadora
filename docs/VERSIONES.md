# Historial de versiones

Registro detallado de versiones anteriores, cambios de la versión actual y estado de los artefactos.

## Resumen

| Versión | Fecha | Web | APK | Cambio principal |
|---|---|---|---|---|
| **2.0.0** | 2026-10-21 | ✅ | ✅ v2.0 | Limpieza total: sin minería, sin costes, **100 % offline** |
| 1.2.0 | 2026-10-20 | ✅ | ✅ v1.2 | Cartuchos reales por tinta/modelo + mantenimiento con P/N |
| 1.1.0 | 2026-10-20 | ✅ | ✅ v1.1 | Familias correctas + boquillas por modelo |
| 1.0.0 | 2026-10-19 | ✅ | ✅ v1.0 | Versión inicial funcional (web + Android) |

---

## Versión 2.0.0 — 2026-10-21 (actual)

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
- Motor: **28/28** pruebas contra documentos oficiales.
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
