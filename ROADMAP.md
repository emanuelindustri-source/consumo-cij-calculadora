# Roadmap

Hoja de ruta del proyecto. Las prioridades pueden cambiar según el uso real en planta.

## v2.1 — Mejoras de calculadora
- [ ] Comparador de escenarios A/B (dos configuraciones lado a lado).
- [ ] Planificador de mantenimiento con fechas de cambio y calendario exportable (.ics).
- [ ] Multi-idioma (ES/EN/PT) en la app y en la web.
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

## Ideas en evaluación
- Detección automática del modelo por fotografía de la placa de características.
- Estimación de consumo por sustrato (absorbencia, porosidad) y por tipo de código.
- Alertas de caducidad de consumibles con notificaciones locales.
