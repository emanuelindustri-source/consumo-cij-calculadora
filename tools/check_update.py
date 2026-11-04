#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Comprueba que version.json (raiz y docs/) coincide con la app y con el catalogo.

Es la salvaguarda del sistema de actualizaciones: si la version publicada en
`version.json` no coincide con `versionCode`/`versionName` de la APK, la app
nunca detectaria la actualizacion (o lo haria en bucle).

    python tools/check_update.py

Sale con codigo 1 si algo no cuadra. No necesita dependencias externas.
"""
import json
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
VERSION = os.path.join(ROOT, "version.json")
VERSION_DOCS = os.path.join(ROOT, "docs", "version.json")
GRADLE = os.path.join(ROOT, "android", "app", "build.gradle.kts")
CATALOG = os.path.join(ROOT, "i18n", "catalog.json")

errors = []


def fail(msg):
    errors.append(msg)


def read_json(path):
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def read(path):
    with open(path, encoding="utf-8") as f:
        return f.read()


def main():
    print("Version publicada")
    if not os.path.isfile(VERSION):
        fail("falta version.json")
        return resumen()
    if not os.path.isfile(VERSION_DOCS):
        fail("falta docs/version.json (GitHub Pages lo necesita)")
        return resumen()

    v = read_json(VERSION)
    d = read_json(VERSION_DOCS)
    if v != d:
        fail("version.json y docs/version.json no son identicos")
    else:
        print("  OK  raiz y docs publican el mismo version.json")

    version = str(v.get("version", ""))
    code = v.get("versionCode")
    if not re.match(r"^\d+\.\d+\.\d+$", version):
        fail("version no tiene formato X.Y.Z: %r" % version)
    if not isinstance(code, int) or code <= 0:
        fail("versionCode debe ser un entero positivo")

    gradle = open(GRADLE, encoding="utf-8").read()
    m = re.search(r"versionCode\s*=\s*(\d+)", gradle)
    n = re.search(r'versionName\s*=\s*"([^"]+)"', gradle)
    if not m or int(m.group(1)) != code:
        fail("versionCode de version.json (%s) no coincide con build.gradle.kts (%s)"
             % (code, m.group(1) if m else "?"))
    if not n or n.group(1) != version:
        fail("version de version.json (%s) no coincide con versionName (%s)"
             % (version, n.group(1) if n else "?"))

    bd = re.search(r'BUILD_DATE",\s*"\\"([^"\\]+)\\"', gradle)
    if not bd or bd.group(1) != v.get("date"):
        fail("date de version.json (%s) no coincide con BUILD_DATE (%s)"
             % (v.get("date"), bd.group(1) if bd else "?"))

    apk = "ConsumoCIJ-%s.apk" % version
    if v.get("apk") != apk:
        fail("apk debe llamarse %s (ahora: %s)" % (apk, v.get("apk")))
    url = str(v.get("apkUrl", ""))
    if not url.startswith("https://"):
        fail("apkUrl debe ser una URL https")
    elif not url.endswith("/v%s/%s" % (version, apk)):
        fail("apkUrl debe apuntar a la release v%s y al archivo %s" % (version, apk))

    notes = str(v.get("notes", ""))
    if notes != "docs/NOTAS-%s.md" % version:
        fail("notes debe ser docs/NOTAS-%s.md" % version)
    for rel in ("NOTAS-%s.md" % version, notes):
        if not os.path.isfile(os.path.join(ROOT, rel)):
            fail("no existe %s (las notas son obligatorias)" % rel)

    if not str(v.get("notesUrl", "")).startswith("https://"):
        fail("notesUrl debe ser una URL https")

    cat = read_json(CATALOG)
    langs = ["es"] + list(cat.get("languages", []))
    if v.get("languages") != langs:
        fail("languages de version.json no coincide con el catalogo (%s)" % ", ".join(langs))
    if v.get("catalogStrings") != len(cat.get("strings", {})):
        fail("catalogStrings (%s) no coincide con el catalogo (%d)"
             % (v.get("catalogStrings"), len(cat.get("strings", {}))))

    size = v.get("apkSizeBytes", 0)
    sha = str(v.get("apkSha256", ""))
    if not isinstance(size, int) or size < 0:
        fail("apkSizeBytes debe ser un entero >= 0")
    if size > 0:
        if not re.match(r"^[0-9a-f]{64}$", sha):
            fail("apkSha256 debe ser un SHA-256 en minusculas de 64 caracteres")
        elif not 300_000 < size < 60_000_000:
            fail("apkSizeBytes parece fuera de rango: %d" % size)
    else:
        print("  AVISO  apkSizeBytes = 0: complete la publicacion con tools/release.py")

    if v.get("offline") is not True:
        fail("offline debe ser true: los calculos no usan red")

    # ---------------------------------------------------------- novedades
    hl = v.get("highlights")
    if not isinstance(hl, dict):
        fail("version.json no incluye highlights (novedades por idioma)")
    else:
        for lang in langs:
            items = hl.get(lang)
            if not isinstance(items, list) or not (2 <= len(items) <= 6):
                fail("highlights[%s] debe tener entre 2 y 6 puntos" % lang)
            elif any(not str(x).strip() for x in items):
                fail("highlights[%s] tiene un punto vacio" % lang)
        extra = [k for k in hl if k not in langs]
        if extra:
            fail("highlights con idiomas que no estan en el catalogo: " + ", ".join(extra))
        if not errors:
            print("  OK  novedades en los %d idiomas (%s)" % (len(langs), ", ".join(langs)))

    # ---------------------------------------------------------- red permitida
    manifest = read(os.path.join(ROOT, "android", "app", "src", "main", "AndroidManifest.xml"))
    for perm in ("android.permission.INTERNET", "android.permission.POST_NOTIFICATIONS",
                 "android.permission.REQUEST_INSTALL_PACKAGES"):
        if perm not in manifest:
            fail("AndroidManifest.xml no declara %s (lo necesita la actualizacion)" % perm)

    allowed = (
        "https://raw.githubusercontent.com/emanuelindustri-source/consumo-cij-calculadora/",
        "https://emanuelindustri-source.github.io/consumo-cij-calculadora/",
        "https://cdn.jsdelivr.net/gh/emanuelindustri-source/consumo-cij-calculadora@main/",
        "https://github.com/emanuelindustri-source/consumo-cij-calculadora/",
    )
    kt_urls = set()
    for dirpath, _dirs, files in os.walk(os.path.join(ROOT, "android", "app", "src", "main", "java")):
        for name in files:
            if not name.endswith(".kt"):
                continue
            text = open(os.path.join(dirpath, name), encoding="utf-8").read()
            kt_urls.update(re.findall(r'https?://[^"\s]+', text))
    for url in sorted(kt_urls):
        if url.startswith("http://"):
            fail("URL sin cifrar en la app: " + url)
        elif not url.startswith(allowed):
            fail("URL de red no permitida en la app: " + url)
    if any(u.startswith("http://") for u in kt_urls):
        pass
    else:
        print("  OK  la app solo usa HTTPS de los endpoints del proyecto")

    if not errors:
        print("  OK  version %s (compilacion %s) coherente con la app y el catalogo" % (version, code))
    return resumen()


def resumen():
    print()
    if errors:
        print("FALLOS (%d):" % len(errors))
        for e in errors:
            print("  - " + e)
        print()
        print("El aviso de actualizacion de la app depende de version.json: corrijalo antes de publicar.")
        return 1
    print("Version publicada correcta.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
