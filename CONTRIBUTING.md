# Cómo contribuir

¡Gracias por tu interés en mejorar esta calculadora!

## Antes de empezar

- Revisa [`ROADMAP.md`](../ROADMAP.md) y las *issues* abiertas para evitar duplicados.
- Para cambios grandes, abre primero una *issue* explicando la propuesta.

## Notas de versión: no se borran ni se reescriben

Las notas de versión (`NOTAS-X.Y.Z.md` en la raíz y en `docs/`) son el **historial del proyecto**:
explican qué es la aplicación, qué cambió en cada versión y cuáles son las metas a futuro. Sirven
también para que otra persona —o una IA— retome el proyecto sin contexto previo.

- **Nunca** se elimina una nota, ni se reescribe la de una versión ya publicada.
- Cada versión añade la suya con los bloques **antes / plan / hecho / pendiente**, y la raíz y
  `docs/` deben ser **idénticas**.
- Al publicar, `python tools/check_notas.py --freeze` guarda la huella SHA-256 en
  `docs/notas-indice.json`. Desde ese momento, cualquier cambio o borrado **hace fallar la CI**.
- Para empezar una versión nueva:

  ```bash
  python tools/check_notas.py --new 2.3.0    # crea raíz + docs con la plantilla
  python tools/validar.py                    # valida todo, incluidas las notas
  python tools/check_notas.py --freeze       # al publicar: congela las anteriores
  ```

- Si añades contenido a una versión ya publicada, **no la edites**: crea la nota de la versión
  siguiente y explica allí el cambio.

La comprobación del historial de Git es opcional: si `git` no está en el `PATH`, defina la variable
de entorno `GIT` con la ruta al ejecutable y la validación revisará también las eliminaciones.

## Entorno

| Herramienta | Versión |
|---|---|
| JDK | 17 |
| Android SDK | 34 (platform-tools, build-tools 34.0.0) |
| Gradle | 8.7+ |
| Node.js (opcional, pruebas de la web) | 18+ |

## Flujo de trabajo

1. Haz un *fork* y crea una rama: `feat/mi-mejora` o `fix/mi-correccion`.
2. Implementa el cambio respetando el estilo existente.
3. **Valida los datos**: si tocas cálculos o tablas, comprueba los valores contra los documentos oficiales
   y documenta la fuente en el *pull request*.
4. Ejecuta la validación completa (notas de versión, catálogo, web y motor):
   ```bash
   python tools/validar.py
   ```
   y, si tocas Android, también las pruebas y la compilación:
   ```bash
   cd android && gradle testDebugUnitTest assembleDebug
   ```
5. Abre el *pull request* completando la plantilla.

## Reglas de datos

- No se aceptan valores «aproximados» sin fuente: cada cifra debe poder rastrearse a un documento.
- Los tamaños de cartucho dependen de la tinta y del equipo; no los generalices.
- Si un dato no está disponible, la interfaz debe indicarlo («n/d») en lugar de inventarlo.

## Estilo

- **Kotlin**: 4 espacios, nombres en inglés para código y español para textos de interfaz.
- **Web**: JavaScript moderno, sin dependencias externas; CSS con variables.
- **Commits**: `tipo(ámbito): descripción` (p. ej. `fix(web): corrige redondeo de cpi`).

## Código de conducta

Al participar aceptas el [`CODE_OF_CONDUCT.md`](../CODE_OF_CONDUCT.md).
