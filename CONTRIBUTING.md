# Cómo contribuir

¡Gracias por tu interés en mejorar esta calculadora!

## Antes de empezar

- Revisa [`ROADMAP.md`](../ROADMAP.md) y las *issues* abiertas para evitar duplicados.
- Para cambios grandes, abre primero una *issue* explicando la propuesta.

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
4. Ejecuta las pruebas:
   ```bash
   cd android && gradle test assembleDebug
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
