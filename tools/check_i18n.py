#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Valida el repositorio sin dependencias externas.

  1. i18n/catalog.json: mismos idiomas, sin cadenas vacias, marcadores {n}
     coherentes y marcadores de negrita **...** equilibrados.
  2. web/calculadora-offline.html y docs/calculadora.html: copias identicas.
  3. La web es un archivo unico: sin recursos ni URL externas.
  4. El bloque de traducciones y los assets de Android coinciden con el catalogo
     (artefactos generados por tools/gen_i18n.py al dia).
  5. Todo parrafo traducible de la web tiene su clave en el catalogo, incluidos
     los que conservan la negrita (clave con marcadores **...**).
  6. Claves usadas en el JavaScript presentes en el catalogo.
  7. Codigo JavaScript sin errores de sintaxis (si hay node disponible).
  8. Etiquetas HTML balanceadas.

Uso:  python tools/check_i18n.py                (comprueba todo)
      python tools/check_i18n.py --list-unused   (ademas lista las claves sin uso)
Sale con codigo 1 si algo falla.
"""
import json
import os
import re
import subprocess
import sys
import tempfile
from html.parser import HTMLParser

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
WEB = os.path.join(ROOT, "web", "calculadora-offline.html")
DOCS = os.path.join(ROOT, "docs", "calculadora.html")
CAT = os.path.join(ROOT, "i18n", "catalog.json")
ASSETS = os.path.join(ROOT, "android", "app", "src", "main", "assets", "i18n")

errors = []
warnings = []


def fail(msg):
    errors.append(msg)


def ok(msg):
    print("  OK  " + msg)


def read(path):
    with open(path, encoding="utf-8") as f:
        return f.read()


# ---------------------------------------------------------------- catalogo
cat = json.loads(read(CAT))
strings = cat.get("strings", {})
langs = cat.get("languages", [])
if not strings:
    fail("catalog.json sin cadenas")
placeholder = re.compile(r"\{[a-zA-Z0-9]+\}")
for es, tr in strings.items():
    for lang in langs:
        value = tr.get(lang, "")
        if not str(value).strip():
            fail("cadena vacia [%s]: %s" % (lang, es[:70]))
        elif set(placeholder.findall(es)) != set(placeholder.findall(str(value))):
            fail("marcadores {n} distintos [%s]: %s" % (lang, es[:70]))
        if str(value).count("**") != es.count("**"):
            fail("marcadores de negrita **...** distintos [%s]: %s" % (lang, es[:70]))
        elif str(value).count("**") % 2:
            fail("marcadores de negrita sin cerrar [%s]: %s" % (lang, es[:70]))
ok("catalogo: %d cadenas x %d idiomas" % (len(strings), len(langs)))

# ---------------------------------------------------------------- web
if not os.path.isfile(WEB):
    fail("no existe web/calculadora-offline.html")
    sys.exit(1)
html = read(WEB)
if os.path.isfile(DOCS):
    if read(DOCS) != html:
        fail("docs/calculadora.html no coincide con web/calculadora-offline.html")
    else:
        ok("web y docs son identicas")
else:
    fail("no existe docs/calculadora.html")

external = re.findall(r'(?:src|href)\s*=\s*["\'](https?:)?//', html, re.I)
if external:
    fail("la web referencia recursos externos (%d)" % len(external))
else:
    ok("web 100 %% offline (sin recursos externos)")

m = re.search(r'<script id="i18n-data" type="application/json">(.*?)</script>', html, re.S)
if not m:
    fail("falta el bloque i18n-data en la web")
else:
    data = json.loads(m.group(1))
    for lang in langs:
        if lang not in data:
            fail("i18n-data sin idioma " + lang)
        else:
            missing = [s for s in strings if s not in data[lang]]
            if missing:
                fail("i18n-data[%s] incompleto (%d)" % (lang, len(missing)))
            elif len(data[lang]) != len(strings):
                fail("i18n-data[%s] con claves de mas (%d)" % (lang, len(data[lang]) - len(strings)))
    ok("bloque i18n-data completo")

# ---------------------------------------------------------------- android assets
for lang in langs:
    p = os.path.join(ASSETS, lang + ".json")
    if not os.path.isfile(p):
        fail("falta assets i18n " + lang)
        continue
    d = json.loads(read(p))
    missing = [s for s in strings if s not in d]
    extra = [s for s in d if s not in strings]
    if missing:
        fail("assets/%s.json incompleto (%d)" % (lang, len(missing)))
    if extra:
        fail("assets/%s.json con claves de mas (%d)" % (lang, len(extra)))
if not errors:
    ok("assets de Android completos")

# ------------------------------------------------- artefactos generados al dia
sys.path.insert(0, os.path.join(ROOT, "tools"))
try:
    import gen_i18n  # noqa: E402
    generated = gen_i18n.build(strings, langs)
    stale = []
    for rel, content in generated.items():
        if rel == "web:i18n-data":
            continue
        path = os.path.join(ROOT, rel.replace("/", os.sep))
        if not os.path.isfile(path) or read(path) != content:
            stale.append(rel)
    if gen_i18n.rewrite_web(generated["web:i18n-data"]) != html:
        stale.append("bloque i18n-data")
    if stale:
        fail("artefactos generados desactualizados: " + ", ".join(stale))
        fail("ejecute:  python tools/gen_i18n.py")
    else:
        ok("artefactos generados al dia (gen_i18n.py)")
except Exception as e:  # pragma: no cover
    warnings.append("no se pudo comprobar gen_i18n.py: %s" % e)


# ---------------------------------------------------------------- claves del DOM
class Node(object):
    def __init__(self, tag, attrs):
        self.tag = tag
        self.attrs = dict(attrs)
        self.children = []


class Build(HTMLParser):
    VOID = {"area", "base", "br", "col", "embed", "hr", "img", "input", "link",
            "meta", "param", "source", "track", "wbr"}

    def __init__(self):
        HTMLParser.__init__(self, convert_charrefs=True)
        self.root = Node("#root", [])
        self.stack = [self.root]

    def handle_starttag(self, tag, attrs):
        n = Node(tag.lower(), attrs)
        self.stack[-1].children.append(n)
        if tag.lower() not in self.VOID:
            self.stack.append(n)

    def handle_startendtag(self, tag, attrs):
        self.stack[-1].children.append(Node(tag.lower(), attrs))

    def handle_endtag(self, tag):
        for i in range(len(self.stack) - 1, 0, -1):
            if self.stack[i].tag == tag.lower():
                del self.stack[i:]
                return

    def handle_data(self, data):
        self.stack[-1].children.append(data)


SKIP = {"script", "style", "noscript", "textarea", "code", "pre"}
INLINE = {"b", "strong", "em", "i", "code"}
BLOCKED = {"a", "button", "select", "input", "textarea"}
TARGETS = {"p", "li", "summary", "h3"}


def node_key(el):
    """Clave de traduccion: texto plano con **...** en los tramos con formato."""
    out, tags = [], []

    def walk(n):
        for c in n.children:
            if isinstance(c, str):
                out.append(c)
            elif c.tag in SKIP:
                continue
            elif c.tag in INLINE:
                out.append("**")
                tags.append(c.tag)
                walk(c)
                out.append("**")
            else:
                walk(c)

    walk(el)
    return re.sub(r"\s+", " ", "".join(out)).strip(), tags


def texts(node, acc):
    for c in node.children:
        if isinstance(c, str):
            acc.append(re.sub(r"\s+", " ", c).strip())
        else:
            texts(c, acc)
    return acc


def descendants(node, acc):
    for c in node.children:
        if not isinstance(c, str):
            acc.append(c)
            descendants(c, acc)
    return acc


tree = Build()
body_start = html.index("<body>")
body_end = html.index('<script id="i18n-data"')
tree.feed(html[body_start:body_end])
tree.close()

reachable = set()
missing_dom = []
for el in descendants(tree.root, []):
    for a in ("title", "aria-label", "placeholder", "alt"):
        v = el.attrs.get(a)
        if v:
            reachable.add(v.strip())
    for txt in texts(el, []):
        if len(txt) > 1:
            reachable.add(txt)
    classes = (el.attrs.get("class") or "").split()
    if el.tag not in TARGETS and not ({"sub", "hint", "note"} & set(classes)):
        continue
    if any(d.tag in BLOCKED for d in descendants(el, [])):
        continue
    key, tags = node_key(el)
    if len(key) <= 1:
        continue
    reachable.add(key)
    if key not in strings:
        plain = key.replace("*", "")
        if plain in strings:
            missing_dom.append(key[:90] + "  (solo existe sin **...**)")
        else:
            missing_dom.append(key[:90])
if missing_dom:
    for k in missing_dom[:12]:
        fail("parrafo de la web sin traducir: " + k)
    if len(missing_dom) > 12:
        fail("... y %d parrafos mas sin traducir" % (len(missing_dom) - 12))
else:
    ok("todos los parrafos traducibles tienen clave en el catalogo")

# ---------------------------------------------------------------- JavaScript
scripts = re.findall(r'<script(?![^>]*type="application/json")[^>]*>(.*?)</script>', html, re.S)
js = "\n".join(scripts)
if re.search(r"\b(toast|t|tf)\(", js) and 'const LANGS' not in js:
    fail("la web no incluye el motor de idiomas")
used = set(re.findall(r'\btf?\("((?:[^"\\]|\\.)*)"', js))
missing = sorted(k for k in used if k not in strings and len(k) > 3)
if missing:
    fail("claves usadas en JS sin traducir: " + ", ".join(missing[:5]))
else:
    ok("todas las claves JS estan en el catalogo")
reachable |= used

if any(x in js for x in ("mailto:",)) and "emanuel.industri@gmail.com" not in html:
    fail("el reporte de errores no apunta al correo configurado")

try:
    with tempfile.NamedTemporaryFile("w", suffix=".js", delete=False, encoding="utf-8") as f:
        f.write(js)
        tmp = f.name
    r = subprocess.run(["node", "--check", tmp], capture_output=True, text=True)
    os.unlink(tmp)
    if r.returncode != 0:
        fail("JavaScript con errores de sintaxis: " + (r.stderr or "")[:400])
    else:
        ok("JavaScript sin errores de sintaxis")
except FileNotFoundError:
    warnings.append("node no disponible: no se pudo comprobar el JavaScript")

# ---------------------------------------------------------------- claves sin uso
# Una clave se considera en uso si aparece en el JavaScript de la web o como
# literal entrecomillada en el codigo Kotlin de la app (extension .tr() / I18n).
kt = []
for dirpath, _dirnames, filenames in os.walk(os.path.join(ROOT, "android", "app", "src")):
    for name in filenames:
        if name.endswith(".kt"):
            kt.append(read(os.path.join(dirpath, name)))
kt_text = "\n".join(kt)
# Los literales de Kotlin pueden partirse en varias líneas con «" + "»:
# se unen antes de buscar, para no dar por huérfana una clave que sí se usa.
kt_flat = re.sub(r'"\s*\+\s*"', "", kt_text)


def used_in_app(key):
    return ('"%s"' % key) in kt_flat


unused = sorted(k for k in strings if k not in reachable and not used_in_app(k))
if "--list-unused" in sys.argv:
    print()
    print("Claves sin uso (%d):" % len(unused))
    for k in unused:
        print("  |" + k)
if unused:
    warnings.append("%d claves del catalogo sin uso en la web ni en la app (%s%s)"
                    % (len(unused), ", ".join(k[:38] for k in unused[:4]),
                       ", …" if len(unused) > 4 else ""))

# ---------------------------------------------------------------- HTML
VOID = {"area", "base", "br", "col", "embed", "hr", "img", "input", "link",
        "meta", "param", "source", "track", "wbr"}


class Balance(HTMLParser):
    def __init__(self):
        super().__init__(convert_charrefs=False)
        self.stack = []
        self.problems = []

    def handle_starttag(self, tag, attrs):
        if tag not in VOID:
            self.stack.append((tag, self.getpos()))

    def handle_endtag(self, tag):
        if tag in VOID:
            return
        if not self.stack:
            self.problems.append(("cierre sin apertura", tag, self.getpos()))
            return
        if self.stack[-1][0] == tag:
            self.stack.pop()
            return
        idx = next((i for i in range(len(self.stack) - 1, -1, -1)
                    if self.stack[i][0] == tag), None)
        if idx is None:
            self.problems.append(("cierre huerfano", tag, self.getpos()))
        else:
            for t, pos in self.stack[idx + 1:]:
                self.problems.append(("sin cerrar", t, pos))
            del self.stack[idx:]


b = Balance()
b.feed(html)
for p in b.problems:
    fail("HTML %s <%s> linea %d" % (p[0], p[1], p[2][0]))
if b.stack:
    fail("HTML sin cerrar al final: " + ", ".join(t for t, _ in b.stack[:5]))
if not b.problems and not b.stack:
    ok("HTML balanceado")

# ---------------------------------------------------------------- resumen
print()
for w in warnings:
    print("AVISO: " + w)
if errors:
    print("FALLOS (%d):" % len(errors))
    for e in errors:
        print("  - " + e)
    sys.exit(1)
print("Todo correcto.")
