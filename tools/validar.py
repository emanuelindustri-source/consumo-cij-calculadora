#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Validacion completa del proyecto con un solo comando.

    python tools/validar.py

Ejecuta:
  1. tools/check_notas.py  las notas de version existen, no se borraron ni se
                           reescribieron y conservan sus bloques (antes / plan /
                           hecho / pendiente).
  2. tools/check_i18n.py   catalogo, copias web, assets, parrafos traducibles,
                           JavaScript y HTML.
  3. tools/test_engine.js  pruebas del motor de calculo contra los valores de
                           referencia de los modelos oficiales de Videojet.
  4. Pruebas del motor en Kotlin (solo si GRADLE_HOME apunta a una instalacion
     de Gradle; en CI las ejecuta .github/workflows/android.yml).

Sale con codigo 1 si algo falla. No necesita dependencias externas.
"""
import os
import shutil
import subprocess
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))


def run(title, cmd):
    print(flush=True)
    print("=" * 72, flush=True)
    print(title, flush=True)
    print("=" * 72, flush=True)
    try:
        r = subprocess.run(cmd, cwd=ROOT)
    except FileNotFoundError:
        print("AVISO: no se pudo ejecutar %s (herramienta no encontrada)" % cmd[0])
        return 0
    return r.returncode


def main():
    code = 0
    code |= run("1/4 · Notas de version (permanencia e historial)",
                [sys.executable, os.path.join("tools", "check_notas.py")])
    code |= run("2/4 · Catalogo, web, assets e idiomas",
                [sys.executable, os.path.join("tools", "check_i18n.py")])
    if shutil.which("node"):
        code |= run("3/4 · Motor de calculo de la web",
                    ["node", os.path.join("tools", "test_engine.js")])
    else:
        print("\nAVISO: node no disponible; se omiten las pruebas del motor de la web.")

    gradle = os.environ.get("GRADLE_HOME")
    gradle_bin = None
    if gradle:
        cand = os.path.join(gradle, "bin", "gradle.bat" if os.name == "nt" else "gradle")
        if os.path.isfile(cand):
            gradle_bin = cand
    if gradle_bin:
        code |= run("4/4 · Motor de calculo de la app (Kotlin)",
                    [gradle_bin, "-p", "android", "testDebugUnitTest", "--console=plain"])
    else:
        print("\nAVISO: defina GRADLE_HOME para ejecutar tambien las pruebas de Kotlin.")
        print("       (el workflow .github/workflows/android.yml las ejecuta en CI)")

    print()
    print("=" * 72)
    if code:
        print("VALIDACION FALLIDA")
    else:
        print("VALIDACION CORRECTA")
    print("=" * 72)
    return 1 if code else 0


if __name__ == "__main__":
    sys.exit(main())
