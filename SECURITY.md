# Política de seguridad

## Alcance

Este proyecto es una herramienta de cálculo técnico. **Los cálculos, el historial y los informes
funcionan sin conexión**; la única conexión a internet es la comprobación de actualizaciones:

- La aplicación Android consulta `version.json` por **HTTPS** para saber si hay una versión nueva.
  La consulta es **anónima** (sin identificadores, cookies ni analítica) y se puede **desactivar**
  en «Actualizaciones».
- La web publicada consulta `version.json` para avisar de una versión nueva. El archivo
  `web/calculadora-offline.html` abierto con doble clic **no hace ninguna petición**.
- No hay telemetría, publicidad, cuentas de usuario ni recogida de datos.

### Permisos de la app

| Permiso | Para qué |
|---|---|
| `INTERNET`, `ACCESS_NETWORK_STATE` | Consultar `version.json` y descargar el APK cuando el usuario lo pide |
| `POST_NOTIFICATIONS` | Avisar de una versión nueva (solo si el usuario lo activa) |
| `REQUEST_INSTALL_PACKAGES` | Abrir el instalador del APK descargado (con confirmación del sistema) |
| `RECEIVE_BOOT_COMPLETED` | Mantener la comprobación diaria tras reiniciar |

No se solicita acceso a almacenamiento, ubicación, contactos, cámara ni micrófono.

### Integridad de las actualizaciones

- `version.json` publica el **SHA-256** y el tamaño del APK.
- Tras la descarga, la app calcula el SHA-256 y, si no coincide, **borra el archivo** y no ofrece
  instalarlo.
- El APK está firmado; Android solo permite instalar la actualización si la firma coincide con la
  versión instalada.

## Versiones soportadas

| Versión | Soporte |
|---|---|
| 2.3.x | ✅ Activa |
| 2.2.x y anteriores | ⚠️ Sin aviso automático de actualizaciones (se recomienda instalar la 2.3.0 una vez) |
| 1.x | ⚠️ Solo referencia histórica (incluía un módulo retirado en 2.0) |

## Reportar una vulnerabilidad

Si encuentras un problema de seguridad:

1. **No** abras una *issue* pública con detalles explotables.
2. Describe el problema (versión, plataforma, pasos) y envíalo de forma privada al mantenedor
   (`emanuel.industri@gmail.com`).
3. Se confirmará la recepción y se trabajará en una corrección lo antes posible.

## Buenas prácticas para quien compila

- No subas el *keystore* ni `keystore.properties` al repositorio (están en `.gitignore`). Guarda una
  copia segura: sin la misma clave, Android no permite actualizar encima.
- Firma los APK de distribución con una clave propia y no la compartas.
- Verifica el APK antes de distribuirlo:
  ```bash
  aapt2 dump badging app-release.apk
  apksigner verify --print-certs app-release.apk
  python tools/check_update.py
  ```
  `check_update.py` comprueba que el SHA-256 publicado corresponde al APK y que la app solo usa
  HTTPS de los endpoints del proyecto.

## Dependencias

La app usa únicamente bibliotecas oficiales de AndroidX/Compose. Mantén Gradle y AGP actualizados y
revisa las alertas de dependencias antes de publicar.
