# Changelog

Todos los cambios relevantes de este proyecto.
El formato sigue [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y el versionado
[SemVer](https://semver.org/lang/es/).

## [2.0.1] — 2026-10-21

### Corregido
- **Mantenimiento**: el cartucho Core de VJ 1620 HR, VJ 1650 HR y VJ 1650 UHS pasa a **14 000 h**
  (antes 12 000 h), según la hoja *Maintenance Setup* del CIJ TCO Calculator v35.
- **Velocidad**: VJ 1580 y VJ 1860 con boquilla de 70 µm ya muestran caracteres/s y velocidad nominal.
  La tabla de 70 µm pasa a usar la clave `1580+` (la hoja original la llama `1580/1580+` y no se encontraba).
- **Tintas sin datos oficiales**: V4238 (MEK / Methanol) y V4251 (Methanol / Water) no tienen factor en el
  Makeup Usage Model v14 (la ficha original da `#N/D`). Ahora se avisa y el resultado se etiqueta como
  estimación con base MEK, en lugar de calcularlo en silencio.
- **QR**: se sustituye la fuente genérica «QR Code» por las cuatro versiones reales (21, 25, 29 y 33) de
  una línea, y el cálculo de gotas reconoce el nombre nuevo.
- **Cartuchos en Android**: al cambiar de equipo, boquilla o tinta, el tamaño se toma de la tinta
  (TCO v35). Antes el 1710 usaba 600 ml y el 1580C 700 ml también para el solvente, cuyo contenedor es 750 ml.

### Cambiado
- Cifras de validación corregidas en la app, la web y la documentación: **139 tintas** (antes «142», que
  incluía tres entradas que no son tintas), **734 fichas de solvente** y **535 de velocidad** contrastadas.
- Se retira la mención a «28/28 pruebas»: el repositorio no incluye una batería de pruebas automatizada.
- Versión: `versionName 2.0.1` · `versionCode 4`.

### Notas
- Pendiente de confirmar con Videojet: V457 con solvente V725 figura con **740 ml** en el TCO
  (el resto de V725 usa 750 ml).

## [2.0.0] — 2026-10-21

### Eliminado
- Módulo experimental de «apoyo voluntario» y todo su código, consentimiento, textos y avisos.
- Permisos `INTERNET` y `ACCESS_NETWORK_STATE`: la aplicación vuelve a ser **100 % offline**.
- Todo el apartado económico: precios por cartucho, coste de fluidos y coste por 1 000 impresiones
  (campos, cálculos, tablas e informes).
- Bloque de avisos legales extenso del pie de página; se sustituye por notas técnicas de fuentes de datos.

### Cambiado
- La tarjeta de ajustes pasa a llamarse **«Mantenimiento y ajustes avanzados»**.
- La sección final de la app pasa a **«Fuentes de datos y notas técnicas»**.
- Versión de la app: `versionName 2.0` (`versionCode 3`).

### Verificado
- APK sin permisos de red (`aapt2 dump badging`) y con soporte `small…xlarge` y todas las densidades.
- Pruebas del motor: comprobaciones manuales contra los documentos oficiales.
- Prueba en dispositivo (Pixel 7 / API 34): instalación, arranque y navegación sin errores.

## [1.2.0] — 2026-10-20

### Añadido
- **Cartuchos reales por tinta y modelo** (139 tintas) con su solvente asociado
  (V7201, V7257, V7220…): p. ej. V4225 = 700 ml, V480 = 600 ml, V401 = 750/5000 ml.
- Seguimiento de mantenimiento con **P/N e intervalo oficial** por modelo.
- Notación compacta para cifras grandes (M/B) y mejoras de legibilidad.
- Módulo opcional de apoyo con consentimiento explícito (retirado en 2.0).

### Corregido
- Selección de boquilla por modelo y coherencia del consumo de solvente en 1880/1880+ (60 vs 70 µm).
- Historial: las entradas antiguas dejaban de sobrescribirse con la configuración actual.

## [1.1.0] — 2026-10-20

### Añadido
- **Familias corregidas**: Simplicity (1240, 1280, 1580, 1580+, 1580C, 1880, 1880+, 1880 UHS, 1880 HR)
  y Serie 1000VJ (resto de modelos 1XX0).
- Modelos unificados con **boquillas dependientes del modelo** (40/50/60/70 µm).
- Validación de la hoja *Maintenance Setup* del TCO para núcleos y módulos de servicio.

### Cambiado
- Interfaz adaptativa (1 columna &lt; 840 dp · 2 columnas ≥ 840 dp) y temas Claro/Oscuro/AMOLED.

## [1.0.0] — 2026-10-19

### Añadido
- Primera versión: calculadora web offline y app Android nativa (Kotlin + Jetpack Compose).
- Motor de solvente (CIJ Makeup Usage Model v14), tinta (CIJ TCO v35) y velocidad (CIJ Line Speed v9.7).
- Gráficos, exportación CSV/TXT, historial y temas de pantalla.
- Verificación de datos con las fichas oficiales de solvente y velocidad.
