# Roadmap

Hoja de ruta del proyecto. Las prioridades pueden cambiar según el uso real en planta.

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

## v2.3 — Android
- [ ] Widget de pantalla de inicio con el consumo mensual.
- [ ] Modo tableta con panel lateral fijo y ventana dividida.
- [ ] Accesibilidad: descripciones ampliadas, navegación por teclado/TV y contraste reforzado.
- [ ] Integración con impresión del informe en PDF (SAF).

## v3.0 — Ecosistema
- [ ] Sincronización opcional (solo si el usuario la activa) entre dispositivos.
- [ ] Versión de escritorio (Windows/macOS) con el mismo motor.
- [ ] API local (sin nube) para integrar el cálculo en otras herramientas internas.

## Pendiente inmediato (2.2.1 / 2.3)
- [ ] Revisión por hablantes nativos de chino simplificado y portugués de Portugal.
- [ ] Ampliar las pruebas con los 2 400 valores contrastados de las fichas oficiales.
- [ ] Capturas nuevas de la app con el selector de idioma visible.
- [ ] Publicar la APK de release como artefacto de la CI.
- [ ] Revocar los tokens de publicación antiguos y custodiar el keystore de firma.

## Ideas en evaluación
- Detección automática del modelo por fotografía de la placa de características.
- Estimación de consumo por sustrato (absorbencia, porosidad) y por tipo de código.
- Alertas de caducidad de consumibles con notificaciones locales.
