#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Genera todos los artefactos de traduccion a partir de i18n/catalog.json.

  · android/app/src/main/assets/i18n/<idioma>.json   (empaquetados en la APK)
  · bloque <script id="i18n-data"> de web/calculadora-offline.html
  · docs/calculadora.html                            (copia byte a byte de la web)

Nunca edite a mano los archivos generados: cambie el catalogo y ejecute

    python tools/gen_i18n.py            -> regenera
    python tools/gen_i18n.py --check    -> comprueba y sale con 1 si hay diferencias
"""
import json
import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CAT = os.path.join(ROOT, "i18n", "catalog.json")
WEB = os.path.join(ROOT, "web", "calculadora-offline.html")
DOCS = os.path.join(ROOT, "docs", "calculadora.html")
ASSETS = os.path.join(ROOT, "android", "app", "src", "main", "assets", "i18n")
TAG_OPEN = '<script id="i18n-data" type="application/json">'
TAG_CLOSE = "</script>"


def dump(obj):
    """Formato estable y reproducible: sin espacios, UTF-8 literal y
    conservando el orden del catalogo (el orden de las claves es el de
    i18n/catalog.json, no el alfabetico)."""
    return json.dumps(obj, ensure_ascii=False, separators=(",", ":"))


def dump_ordered(obj, order):
    """Objeto de nivel superior respetando el orden de idiomas del catalogo."""
    parts = []
    for k in order:
        parts.append(json.dumps(k, ensure_ascii=False) + ":" + dump(obj[k]))
    return "{" + ",".join(parts) + "}"


def load_catalog():
    with open(CAT, encoding="utf-8") as f:
        cat = json.load(f)
    return cat["strings"], list(cat["languages"])


def build(strings, langs):
    """Devuelve {ruta_relativa: contenido} con todos los artefactos generados."""
    out = {}
    for lang in langs:
        data = {es: tr[lang] for es, tr in strings.items()}
        rel = os.path.join("android", "app", "src", "main", "assets", "i18n", lang + ".json")
        out[rel] = dump(data)

    block = {lang: {es: tr[lang] for es, tr in strings.items()} for lang in langs}
    out["web:i18n-data"] = dump_ordered(block, langs)
    return out


def rewrite_web(block_text):
    with open(WEB, encoding="utf-8") as f:
        html = f.read()
    i = html.index(TAG_OPEN) + len(TAG_OPEN)
    j = html.index(TAG_CLOSE, i)
    return html[:i] + block_text + html[j:]


def main():
    check = "--check" in sys.argv
    strings, langs = load_catalog()
    generated = build(strings, langs)
    problems = []

    for rel, content in generated.items():
        if rel == "web:i18n-data":
            continue
        path = os.path.join(ROOT, rel.replace("/", os.sep))
        current = open(path, encoding="utf-8").read() if os.path.isfile(path) else None
        if current != content:
            if check:
                problems.append("desactualizado: " + rel)
            else:
                with open(path, "w", encoding="utf-8", newline="\n") as f:
                    f.write(content)
                print("  OK  regenerado " + rel)

    new_html = rewrite_web(generated["web:i18n-data"])
    with open(WEB, encoding="utf-8") as f:
        current_html = f.read()
    if current_html != new_html:
        if check:
            problems.append("desactualizado: bloque i18n-data de web/calculadora-offline.html")
        else:
            with open(WEB, "w", encoding="utf-8", newline="\n") as f:
                f.write(new_html)
            print("  OK  regenerado bloque i18n-data")

    if not check:
        with open(WEB, encoding="utf-8") as f:
            web_text = f.read()
        if os.path.isfile(DOCS) and open(DOCS, encoding="utf-8").read() == web_text:
            pass
        else:
            with open(DOCS, "w", encoding="utf-8", newline="\n") as f:
                f.write(web_text)
            print("  OK  copiado docs/calculadora.html")
        print("Catalogo: %d cadenas x %d idiomas traducidos." % (len(strings), len(langs)))
        return 0

    if problems:
        print("GENERADOS DESACTUALIZADOS (%d):" % len(problems))
        for p in problems:
            print("  - " + p)
        print("Ejecute:  python tools/gen_i18n.py")
        return 1
    print("  OK  artefactos generados al dia (%d cadenas x %d idiomas)" % (len(strings), len(langs)))
    return 0


if __name__ == "__main__":
    sys.exit(main())
