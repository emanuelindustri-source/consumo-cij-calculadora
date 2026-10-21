#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Valida el repositorio sin dependencias externas:

  1. i18n/catalog.json: mismos idiomas, sin cadenas vacias y mismos marcadores {n}.
  2. web/calculadora-offline.html y docs/calculadora.html: copias identicas.
  3. La web es un archivo unico: sin recursos ni URL externas.
  4. El bloque de traducciones inyectado coincide con el catalogo.
  5. Los assets de Android existen y tienen las mismas claves.
  6. Codigo JavaScript sin errores de sintaxis (si hay node disponible).
  7. Etiquetas HTML balanceadas.

Uso:  python tools/check_i18n.py
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
            fail("marcadores distintos [%s]: %s" % (lang, es[:70]))
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
    ok("bloque i18n-data completo")

# ---------------------------------------------------------------- android assets
for lang in langs:
    p = os.path.join(ASSETS, lang + ".json")
    if not os.path.isfile(p):
        fail("falta assets i18n " + lang)
        continue
    d = json.loads(read(p))
    missing = [s for s in strings if s not in d]
    if missing:
        fail("assets/%s.json incompleto (%d)" % (lang, len(missing)))
if not errors:
    ok("assets de Android completos")

# ---------------------------------------------------------------- JS
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
