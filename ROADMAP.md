# Roadmap

Hoja de ruta del proyecto. Las prioridades pueden cambiar según el uso real en planta.

> **Regla permanente:** las notas de versión (`NOTAS-X.Y.Z.md`) **no se borran ni se reescriben**.
> Son el historial del proyecto y la memoria que permite a cualquier persona o IA entender qué es la
> aplicación, qué cambió y qué falta. Se crean con `python tools/check_notas.py --new X.Y.Z` y se
> congelan al publicar con `--freeze`; la validación falla si desaparecen o se modifican.

## v2.4.1 — Catálogo de tintas validado (hecho)
- [x] **493 PDF de fichas de tintas revisados** (492 SDS + catálogo).
- [x] **V4287 identificada**: pertenece a MI 9029 y no se añade al catálogo Videojet.
- [x] **27 tintas fuera del TCO** documentadas y no calculadas a ciegas.
- [x] **Gramática** española e italiana corregida y traducciones revisadas.
- [x] **`tools/check_fichas.py`** y **`docs/TINTAS.md`**.

## v2.4.0 — Datos y copia de seguridad (hecho)
- [x] **Cartuchos «750 mL Only»**: seis modelos corregidos; nunca 1 L ni 5 L donde el TCO no lo permite.
- [x] **Solvente validado**: 800 fichas oficiales y 12.800 valores en la web y en Android.
- [x] **Aviso con novedades**: `highlights` por idioma y recordatorio a los 14 días.
- [x] **Actualizaciones obligatorias** (`mandatory`) sin instalación automática.
- [x] **Copia de seguridad JSON**: configuración, historial, tema e idioma.
- [x] **Corrección de configuraciones guardadas** con tamaños de cartucho no válidos.

## v2.4.2 / 2.5.0 — Precisión de velocidad
- [ ] **Verificador de las 535 fichas de velocidad** contra la hoja oficial.
- [ ] Resolver las **4 diferencias** detectadas con `CIJ Speeds.xlsx`.
- [ ] Revisar las tintas **V42xx** asignadas a la Serie 1000VJ.
- [ ] Confirmar **V457 con V725** (740 ml) con Videojet.

## v2.3.0 — Actualizaciones verificadas (hecho)
- [x] **Aviso de versión nueva** dentro de la app y notificación local opcional.
- [x] **Comprobación diaria** en segundo plano con `JobScheduler` (desactivable).
- [x] **Descarga del APK con SHA-256** y instalación con el instalador del sistema.
- [x] **Una sola fuente de versión**: `BuildConfig` + `version.json` validado en CI.
- [x] **Publicación automatizada** con `tools/release.py` (APK + huella + release).
- [x] **Aviso de versión nueva en la web publicada** (sin afectar al archivo offline).
- [x] **Correcciones**: historial restaurado en la misma sesión, fechas en el idioma activo y
  mensajes emergentes traducidos.

## v2.2.0 — Pendientes resueltos y calidad (hecho)
- [x] **Cabeceras del informe TXT traducidas** en los seis idiomas (web y app).
- [x] **Negrita y formato conservados** en los textos traducidos de la web.
- [x] **Selector de idioma dentro de la app**, con memoria de la elección.
- [x] **Prueba automática del motor** con valores de referencia (43 web + 17 Android).
- [x] **Un solo comando de validación** (`tools/validar.py`) ejecutado también en la CI.
- [x] **Generador de traducciones** (`tools/gen_i18n.py`): una única fuente de verdad.

## v2.1.1 — Seguimiento y puesta al día (hecho)
- [x] **Portada del proyecto y notas públicas al día (antes / hecho / pendiente).**
- [x] **Versión y compilación visibles** en la web y en la app (`2.1.1` · `6`).
- [x] **`version.json`** con versión, fecha y artefactos para seguimiento.
- [x] **Catálogo de 364 cadenas** validado en CI.

## v2.1 — Mejoras de calculadora
- [ ] Comparador de escenarios A/B (dos configuraciones lado a lado).
- [ ] Planificador de mantenimiento con fechas de cambio y calendario exportable (.ics).
- [x] **Multi-idioma (ES/EN/PT-BR/PT-PT/IT/ZH) en la web y en la app — 2.1.0.**
- [ ] Presets de mensajes habituales (fecha, lote, EAN-13, QR, DataMatrix).

## v2.2 — Datos y trazabilidad
- [ ] Importación de la tabla de tintas desde CSV para actualizarla sin recompilar.
- [ ] Registro de auditoría de cálculos (exportable a CSV firmado).
- [ ] Verificación automática de los PDFs de origen en cada release (script de validación en CI).

## v2.4 — Android
- [ ] Widget de pantalla de inicio con el consumo mensual.
- [ ] Modo tableta con panel lateral fijo y ventana dividida.
- [ ] Accesibilidad: descripciones ampliadas, navegación por teclado/TV y contraste reforzado.
- [ ] Integración con impresión del informe en PDF (SAF).
- [ ] Exportar e importar el historial y la configuración (copia de seguridad).

## v3.0 — Ecosistema
- [ ] Sincronización opcional (solo si el usuario la activa) entre dispositivos.
- [ ] Versión de escritorio (Windows/macOS) con el mismo motor.
- [ ] API local (sin nube) para integrar el cálculo en otras herramientas internas.

## Pendiente inmediato (2.4.2 / 2.5.0)
- [ ] **Verificador de velocidad** (535 fichas) y revisión de las 4 diferencias con `CIJ Speeds.xlsx`.
- [ ] Revisión por hablantes nativos de chino simplificado y portugués de Portugal.
- [x] **Solvente validado** con 800 fichas y 12.800 valores en la CI.
- [x] **Copia de seguridad** de configuración e historial en JSON (Android).
- [ ] Copia de seguridad del historial también en la web.
- [ ] Capturas nuevas de la app con el aviso de novedades y la copia de seguridad.
- [ ] Copia de seguridad documentada del keystore y revocación de tokens antiguos.
- [ ] Valorar actualización incremental (APK splits o delta) si la app crece.

## Ideas en evaluación
- Detección automática del modelo por fotografía de la placa de características.
- Estimación de consumo por sustrato (absorbencia, porosidad) y por tipo de código.
- Alertas de caducidad de consumibles con notificaciones locales.
