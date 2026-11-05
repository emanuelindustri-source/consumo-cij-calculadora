# Validación de las fichas de tintas y fluidos

Documento de la campaña de revisión de la carpeta **`ficha de tintas`** (hojas de datos de
seguridad, SDS) y su contraste con el catálogo de la calculadora.

## 1. Qué contiene la carpeta

| Tipo | Cantidad |
|---|---|
| Hojas de datos de seguridad (SDS) en PDF | **492** |
| Catálogo comercial de tintas y fluidos (`br-cij-inks-and-fluids-es.pdf`) | 1 |
| **Total de PDF** | **493** |

Cada SDS identifica un producto (Videojet o de otra marca incluida en el TCO) con un código y una
variante de envase: `V4201-D`, `V401-K`, `V480-C`, `V705-D`, etc. La primera página indica si es
**Ink** (tinta) o **Make-Up Fluid** (solvente de reposición).

## 2. Resultado del contraste

| Comprobación | Resultado |
|---|---|
| Productos `V*` distintos en las SDS | **188**: 152 de la serie de tintas (143 confirmadas como *Ink* + 9 sin tipo legible) y 36 make-up |
| Tintas del catálogo de la app | **139** |
| Tintas del CIJ TCO Calculator v35 para equipos Videojet | **139** |
| Bases y tamaños de las 139 tintas | **0 diferencias** con el TCO |
| Tinta del TCO que no aplica a Videojet | **V4287** (MI 9029) → **no añadida** |
| Tintas de las SDS fuera del TCO v35 | **27** → documentadas, no añadidas al cálculo |

### 2.1 V4287: identificada, pero no es una tinta Videojet

El TCO incluye una fila con **V4287** (700 ml, base MPK, solvente V7250 750 ml). Al revisar la fila
completa se comprobó que pertenece a la impresora **MI 9029** (`A: MI 9029`, `B: MI`), no a un
equipo Videojet. Además, su base **MPK** no tiene factor en el Makeup Usage Model v14, por lo que el
motor caía en una estimación con MEK.

**Decisión:** no se añade a la calculadora. El catálogo Videojet queda con **139 tintas**, que son
las que el TCO asigna a las familias Simplicity y Serie 1000VJ.

### 2.2 Tintas de las SDS que no están en el TCO v35

Son productos nuevos o variantes que el TCO v35 todavía no modela. **No se añadieron a la
calculadora** porque no hay datos oficiales de modelo, base y contenedor para calcularlos con
garantía:

`V4221M`, `V4244`, `V425`, `V4254`, `V4259M`, `V4266`, `V4277`, `V428`, `V4280`, `V4281`, `V4282`,
`V4284`, `V4285`, `V4286`, `V4288`, `V4290`, `V4293`, `V4294`, `V430`, `V483`, `V483A`, `V483B`,
`V505A`, `V651`, `V652`, `V653`, `V654`.

> El catálogo comercial las agrupa por aplicación (bebidas retornables, alimentos, etc.), pero no
> publica la matriz modelo/contenedor que necesita el motor. Quedan como pendiente para cuando se
> actualice el TCO.
>
> En el otro sentido, **15 tintas de la app no tienen ficha en esta carpeta** (V404, V413, V4238,
> V4263, V4276, V438, V463, V467, V468, V483AB, V491, V509, V523, V572, V573): son códigos del TCO
> que no aparecen en el lote de SDS recibido; no se retiran.

### 2.3 Variantes de make-up

Las SDS incluyen `V705A`, `V7201M` y `V7207M`, que no están en la app. Son variantes de
`V705`/`V7201`/`V7207`; no se añadieron porque los modelos actuales usan el código base.

### 2.4 Envases: lo que confirma y lo que no se cambió

La variante del nombre se corresponde con el envase:

| Sufijo | Tamaño | Ejemplo |
|---|---|---|
| `-D` | 750 ml | V4201-D |
| `-L` | 1 000 ml | V4201-L |
| `-K` | 5 000 ml | V401-K |
| `-C` | 600 ml | V480-C |
| `-E` | 700 ml | V4225-E |
| `-R` | 750 ml | V571-R |

Esta correspondencia confirma los tamaños de la app para V4201 (750/1000), V401 (750/5000), V480
(600), V4225 (700) y V571 (750).

**No se retiraron tamaños** cuando la carpeta solo tenía una variante: la ausencia de un PDF `-L` o
`-K` no demuestra que el envase no exista; el **TCO v35 sigue siendo la fuente de verdad** para los
tamaños. Por el mismo motivo no se añadieron los 5 L que aparecen en algunas SDS (V435, V704, V708,
V710, V715, V720) sin confirmación del TCO.

## 3. Revisión gramatical

- **Español**: corregido «El consumo de make-up **lo influyen**…» → «**está influido por**…» y la
  errata «ciclo de **medimiento**» → «ciclo de **medición**».
- **Italiano**: «ciclo di **misura**» → «ciclo di **misurazione**»; negación «1 L **o** 5 L» →
  «1 L **né** 5 L».
- **Chino**: «condensador activo/pasivo» → «主动/被动冷凝器» (más preciso que «启用/停用»).
- Se revisaron las cadenas nuevas de la 2.4.0/2.4.1 en los seis idiomas (actualizaciones, copia de
  seguridad, cartuchos 750-only y fichas).

## 4. Cómo repetir la comprobación

```bash
python tools/check_fichas.py --dir "C:\Users\Sr.Hidalgo\Documents\OneDrive_39_17-8-2026\ficha de tintas"
```

El verificador no necesita dependencias: lee los nombres de los PDF, deduce el código y la variante,
y compara con `web/calculadora-offline.html`. V4287 se excluye del recuento porque el TCO la asigna a
MI 9029. El análisis de tipos (Ink/Make-Up) de este documento se hizo con `pdfplumber` sobre la
primera página de cada SDS.

## 5. Conclusión

- El catálogo Videojet de la app **coincide con el TCO v35** en las **139 tintas** (base y tamaños).
- **V4287 no se añade**: es una tinta de MI 9029 con base MPK sin factor oficial.
- Las **27 tintas nuevas** de las SDS quedan identificadas y **no se calculan a ciegas**.
- La carpeta confirma los envases y deja claro qué falta por actualizar cuando Videojet publique un
  TCO nuevo.
