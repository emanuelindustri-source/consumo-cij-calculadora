#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Valida el catalogo de tintas y fluidos contra las fichas SDS del fabricante.

    python tools/check_fichas.py --dir "C:\\...\\ficha de tintas"

Lee los nombres de los PDF (codigo + variante de envase), deduce el tipo por el
codigo y compara con el catalogo de la web. No necesita dependencias externas y
no forma parte de la CI porque la carpeta de fichas no esta en el repositorio.

Variantes de envase confirmadas: D=750, L=1000, K=5000, C=600, E=700, R=750.
"""
import argparse
import json
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
WEB = os.path.join(ROOT, "web", "calculadora-offline.html")
SUFFIX_SIZE = {"D": 750, "L": 1000, "K": 5000, "C": 600, "E": 700, "R": 750}
# Series de Videojet: V4xx/V5xx/V6xx son tintas; V7xx/V8xx son make-up.
INK_SERIES = ("V4", "V5", "V6")
MU_SERIES = ("V7", "V8")
# El TCO incluye V4287, pero pertenece a la impresora MI 9029 (no Videojet).
NOT_VIDEOJET = {"V4287"}


def load_data():
    with open(WEB, encoding="utf-8") as f:
        html = f.read()
    m = re.search(r"^const DATA = (.*?);$", html, re.M)
    if not m:
        raise SystemExit("no se encontro const DATA en la web")
    return json.loads(m.group(1))


def parse_folder(path):
    products = {}
    total = 0
    for name in sorted(os.listdir(path)):
        if not name.lower().endswith(".pdf") or name.startswith("br-"):
            continue
        total += 1
        product = name.split(".")[0]
        m = re.match(r"^(.*)-([A-Z0-9]{1,3})$", product)
        code, suffix = (m.group(1), m.group(2)) if m else (product, "")
        if not code.startswith("V"):
            continue
        entry = products.setdefault(code, {"suffixes": set(), "files": 0})
        entry["suffixes"].add(suffix)
        entry["files"] += 1
    return total, products


def kind(code):
    if code.startswith(MU_SERIES):
        return "make-up"
    if code.startswith(INK_SERIES):
        return "tinta"
    return "?"


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--dir", default=r"C:\Users\Sr.Hidalgo\Documents\OneDrive_39_17-8-2026\ficha de tintas")
    args = ap.parse_args()
    if not os.path.isdir(args.dir):
        print("AVISO: no existe la carpeta de fichas: " + args.dir)
        print("Este verificador es opcional; no forma parte de la CI.")
        return 0

    data = load_data()
    app_inks = data["inks"]
    app_cont = data["inkContainers"]
    app_mu = {v.get("muCode") for v in app_cont.values() if v.get("muCode")}

    total, products = parse_folder(args.dir)
    inks = {c: v for c, v in products.items() if kind(c) == "tinta"}
    mus = {c: v for c, v in products.items() if kind(c) == "make-up"}
    unknown = {c: v for c, v in products.items() if kind(c) == "?"}

    print("Fichas PDF: %d | productos V*: %d | tintas: %d | make-up: %d | otros: %d"
          % (total, len(products), len(inks), len(mus), len(unknown)))
    print("App: %d tintas | %d make-up" % (len(app_inks), len(app_mu)))

    missing_inks = sorted(set(inks) - set(app_inks) - NOT_VIDEOJET)
    extra_inks = sorted(set(app_inks) - set(inks))
    missing_mu = sorted(set(mus) - app_mu)
    extra_mu = sorted(app_mu - set(mus))
    print("\nTintas en las fichas que faltan en la app (%d): %s"
          % (len(missing_inks), ", ".join(missing_inks) or "-"))
    print("Tintas de la app sin ficha en la carpeta (%d): %s"
          % (len(extra_inks), ", ".join(extra_inks) or "-"))
    print("Make-up en las fichas que faltan en la app (%d): %s"
          % (len(missing_mu), ", ".join(missing_mu) or "-"))
    print("Make-up de la app sin ficha en la carpeta (%d): %s"
          % (len(extra_mu), ", ".join(extra_mu) or "-"))

    print("\nTamanos que confirman las fichas (no se aplican solos; manda el TCO):")
    for code in sorted(inks):
        expected = sorted({SUFFIX_SIZE[s] for s in inks[code]["suffixes"] if s in SUFFIX_SIZE})
        if not expected:
            continue
        app = sorted(app_cont.get(code, {}).get("inkSizes") or [])
        if app and app != expected:
            print("  %-8s fichas %-14s esperado %-12s app %s"
                  % (code, ",".join(sorted(inks[code]["suffixes"])), expected, app))

    print("\nProductos fuera del TCO (no calculados): %d" % len(missing_inks))
    if "V4287" in inks:
        print("V4287: esta en las fichas y en el TCO, pero pertenece a MI 9029; no es una tinta Videojet.")
    print("Ver docs/TINTAS.md para el detalle y la decision de cada uno.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
