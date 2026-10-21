# Política de seguridad

## Alcance

Este proyecto es una herramienta de cálculo **sin conexión a internet**:

- La aplicación Android **no declara permisos de red** (`INTERNET`, `ACCESS_NETWORK_STATE`).
- La web es un archivo autónomo que no realiza peticiones externas.
- No hay telemetría, analítica, publicidad ni recogida de datos.

## Versiones soportadas

| Versión | Soporte |
|---|---|
| 2.x | ✅ Activa |
| 1.x | ⚠️ Solo referencia histórica (incluía un módulo retirado en 2.0) |

## Reportar una vulnerabilidad

Si encuentras un problema de seguridad:

1. **No** abras una *issue* pública con detalles explotables.
2. Describe el problema (versión, plataforma, pasos) y envíalo de forma privada al mantenedor.
3. Se confirmará la recepción y se trabajará en una corrección lo antes posible.

## Buenas prácticas para quien compila

- No subas el *keystore* ni `keystore.properties` al repositorio (están en `.gitignore`).
- Firma los APK de distribución con una clave propia y guárdala de forma segura.
- Verifica el APK antes de distribuirlo:
  ```bash
  aapt2 dump badging app-release.apk
  ```
  Comprueba que **no** aparezcan permisos de red.

## Dependencias

La app usa únicamente bibliotecas oficiales de AndroidX/Compose. Mantén Gradle y AGP actualizados y
revisa las alertas de dependencias antes de publicar.
