#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Guarda permanente de las notas de version.

Las notas de version son el historial del proyecto: explican que es la aplicacion,
que cambio en cada version y cuales son las metas a futuro. NO SE BORRAN NI SE
REESCRIBEN. Este script lo comprueba en cada validacion.

  1. Cada version tiene su nota en la raiz y en docs/, con contenido identico.
  2. Toda nota congelada en docs/notas-indice.json conserva su SHA-256 exacto
     (si alguien la edita o la borra, la validacion falla).
  3. Cada nota contiene los bloques que necesita una IA para entender el proyecto:
     como estaba antes, que se hizo, que queda pendiente. Las notas nuevas deben
     incluir ademas el plan.
  4. La version de version.json tiene nota, y toda version publicada en
     docs/VERSIONES.md esta registrada.
  5. Si hay git disponible, ninguna nota que existio en el historial puede faltar
     hoy (las eliminaciones se detectan y se denuncian).

Uso:
    python tools/check_notas.py                 comprueba todo
    python tools/check_notas.py --list          lista las notas registradas
    python tools/check_notas.py --new 2.3.0     crea la nota de una version nueva
    python tools/check_notas.py --freeze        congela las notas al publicar

Sale con codigo 1 si algo falla. No necesita dependencias externas.
"""
import datetime
import hashlib
import json
import os
import re
import shutil
import subprocess
import sys
import unicodedata

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DOCS = os.path.join(ROOT, "docs")
MANIFEST = os.path.join(DOCS, "notas-indice.json")
VERSION_JSON = os.path.join(ROOT, "version.json")
VERSIONES = os.path.join(DOCS, "VERSIONES.md")
NAME_RE = re.compile(r"^NOTAS-(\d+\.\d+\.\d+)\.md$")
SEMVER_RE = re.compile(r"^(\d+)\.(\d+)\.(\d+)$")

# Bloques minimos que hacen util una nota para una persona o una IA.
# Cada bloque se detecta por palabras clave en los titulos (sin acentos).
SECCIONES = {
    "antes": ("antes", "como se encontro", "estado a la entrada", "situacion inicial",
              "lo que habia", "punto de partida"),
    "plan": ("plan", "se iba a hacer", "que se iba a hacer", "objetivo de la version",
             "propuesta", "alcance"),
    "hecho": ("se hizo", "se consiguio", "que se consiguio", "hecho", "cambios de la version",
              "que se logro"),
    "futuro": ("pendiente", "a futuro", "proximos pasos", "metas", "hoja de ruta", "roadmap"),
}
ORDEN_SECCIONES = ("antes", "plan", "hecho", "futuro")
MIN_CARACTERES = 400

errors = []
warnings = []


def fail(msg):
    errors.append(msg)


def ok(msg):
    print("  OK  " + msg)


def warn(msg):
    warnings.append(msg)


def read(path):
    with open(path, encoding="utf-8") as f:
        return f.read()


def write(path, content):
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write(content)


def sha256(path):
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for block in iter(lambda: f.read(65536), b""):
            h.update(block)
    return h.hexdigest()


def sin_acentos(text):
    """Minusculas y sin acentos, para comparar titulos sin depender del idioma."""
    text = unicodedata.normalize("NFD", text.lower())
    return "".join(c for c in text if unicodedata.category(c) != "Mn")


def key(version):
    return tuple(int(x) for x in version.split("."))


def versiones_en_disco():
    """Versiones con nota en la raiz y/o en docs/."""
    found = {}
    for base, prefix in ((ROOT, ""), (DOCS, "docs/")):
        for name in os.listdir(base):
            m = NAME_RE.match(name)
            if not m:
                continue
            path = os.path.join(base, name)
            if not os.path.isfile(path):
                continue
            encontrados = found.setdefault(m.group(1), {})
            if "root" in encontrados or "docs" in encontrados:
                # Misma version repetida en el mismo sitio: no deberia ocurrir.
                pass
            encontrados["root" if prefix == "" else "docs"] = prefix + name
    return found


def version_actual():
    if not os.path.isfile(VERSION_JSON):
        return None
    try:
        return json.loads(read(VERSION_JSON)).get("version")
    except ValueError:
        return None


def cargar_manifiesto():
    if not os.path.isfile(MANIFEST):
        return None
    try:
        data = json.loads(read(MANIFEST))
    except ValueError as e:
        fail("docs/notas-indice.json no es JSON valido: %s" % e)
        return None
    if not isinstance(data.get("notas"), list):
        fail("docs/notas-indice.json no tiene la lista \"notas\"")
        return None
    return data


def titulos(text):
    return [m.group(1).strip() for m in re.finditer(r"^#{1,6}\s+(.+?)\s*$", text, re.M)]


def secciones_presentes(text):
    titulos_norm = [sin_acentos(t) for t in titulos(text)]
    presentes = set()
    for nombre, claves in SECCIONES.items():
        if any(k in t for t in titulos_norm for k in claves):
            presentes.add(nombre)
    return presentes


def comprobar_contenido(version, texto, declaradas, archivo):
    faltan = [s for s in declaradas if s not in secciones_presentes(texto)]
    if faltan:
        fail("%s: faltan los bloques %s (necesarios para entender la version)"
             % (archivo, ", ".join(faltan)))
    if len(texto.strip()) < MIN_CARACTERES:
        fail("%s: la nota es demasiado corta (%d caracteres); una nota vacia no sirve "
             "de historial" % (archivo, len(texto.strip())))
    if version not in texto:
        fail("%s: la nota no menciona su version (%s)" % (archivo, version))
    if "consumo cij" not in sin_acentos(texto):
        warn("%s: no menciona el producto (\"Consumo CIJ\"); conviene recordar que es la app"
             % archivo)


# --------------------------------------------------------------------- git
def git_bin():
    for cand in (os.environ.get("GIT"), shutil.which("git"),
                 r"C:\Program Files\Git\cmd\git.exe",
                 r"C:\Program Files (x86)\Git\cmd\git.exe",
                 os.path.join(os.environ.get("LOCALAPPDATA", ""), "Programs", "Git", "cmd", "git.exe")):
        if cand and os.path.isfile(cand):
            return cand
    return None


def git(*args):
    g = git_bin()
    if not g:
        return None
    try:
        r = subprocess.run([g, "-C", ROOT] + list(args), capture_output=True, text=True,
                           encoding="utf-8", errors="replace")
    except OSError:
        return None
    if r.returncode != 0:
        return None
    return r.stdout


def comprobar_historial(registradas):
    if not git_bin() or not os.path.isdir(os.path.join(ROOT, ".git")):
        warn("sin git disponible: no se pudo revisar el historial (las demas comprobaciones "
             "si se aplicaron)")
        return
    # 1) Notas que alguna vez se anadieron al repositorio.
    anadidas = git("log", "--all", "--diff-filter=A", "--name-only", "--pretty=format:@%h|%ad|%s",
                   "--date=short", "--", "*NOTAS-*.md")
    if anadidas is None:
        warn("no se pudo leer el historial de git (clon superficial?); las notas se "
             "comprobaron igualmente por contenido y huella")
        return
    actuales = set()
    for base, rel in ((ROOT, ""), (DOCS, "docs/")):
        for name in os.listdir(base):
            if NAME_RE.match(name):
                actuales.add((rel + name).replace("\\", "/"))

    huerfanas = []
    for linea in anadidas.splitlines():
        linea = linea.strip()
        if not linea:
            continue
        if linea.startswith("@"):
            commit = linea
            continue
        ruta = linea.replace("\\", "/")
        nombre = os.path.basename(ruta)
        if not NAME_RE.match(nombre):
            continue
        if ruta not in actuales and ruta.startswith(("docs/", "NOTAS-")):
            huerfanas.append(ruta)
    if huerfanas:
        for ruta in sorted(set(huerfanas)):
            fail("la nota %s existio en el historial y hoy no esta: las notas de version no "
                 "se borran (recuperela con: git checkout <commit> -- %s)" % (ruta, ruta))

    # 2) Eliminaciones: se avisa siempre, aunque despues se hayan restaurado.
    borradas = git("log", "--all", "--diff-filter=D", "--name-only", "--pretty=format:@%h|%ad|%s",
                   "--date=short", "--", "*NOTAS-*.md")
    if borradas:
        for linea in borradas.splitlines():
            linea = linea.strip()
            if linea.startswith("@"):
                continue
            if NAME_RE.match(os.path.basename(linea)):
                warn("hubo un borrado de %s en el historial (%s); las notas no deben borrarse"
                     % (linea.replace("\\", "/"), "ver git log"))

    # 3) Las notas registradas deben estar versionadas (no solo en el disco).
    for version, entrada in sorted(registradas.items(), key=lambda kv: key(kv[0])):
        ruta = entrada.get("root")
        if ruta and git("ls-files", "--error-unmatch", ruta) is None:
            warn("%s todavia no esta versionada en git (haga commit)" % ruta)


# --------------------------------------------------------------------- modo --new
PLANTILLA = """# Notas de la version {version}

> **Producto:** Consumo CIJ - calculadora de consumo de tinta y solvente para impresoras
> de inyeccion continua (CIJ) de Videojet. Web 100 % offline y app Android nativa.
> **Fecha:** {fecha} - **Compilacion (versionCode):** {code} - **Version anterior:** {anterior}

## 1 - Como se encontro el proyecto

<!-- Estado real al empezar: que funcionaba, que estaba roto y que faltaba. -->

## 2 - Que se iba a hacer (plan de la {version})

<!-- Lista de cambios previstos y por que. -->

## 3 - Que se consiguio (la {version})

<!-- Cambios hechos, con el detalle tecnico necesario para reproducirlos. -->

## 4 - Que queda pendiente a futuro

<!-- Metas, deudas tecnicas y tareas que necesitan personas. -->

## 5 - Como comprobar esta entrega

<!-- Comandos y pruebas ejecutadas, incluidos los resultados. -->
"""


def modo_new(version):
    if not SEMVER_RE.match(version):
        print("Version no valida: %s (formato X.Y.Z)" % version)
        return 1
    anterior = "?"
    if os.path.isfile(VERSION_JSON):
        anterior = json.loads(read(VERSION_JSON)).get("version", "?")
    code = "?"
    gradle = os.path.join(ROOT, "android", "app", "build.gradle.kts")
    if os.path.isfile(gradle):
        m = re.search(r"versionCode\s*=\s*(\d+)", read(gradle))
        if m:
            code = str(int(m.group(1)) + 1)
    texto = PLANTILLA.format(version=version, fecha=datetime.date.today().isoformat(),
                             code=code, anterior=anterior)
    raiz = os.path.join(ROOT, "NOTAS-%s.md" % version)
    copia = os.path.join(DOCS, "NOTAS-%s.md" % version)
    for destino in (raiz, copia):
        if os.path.isfile(destino):
            print("Ya existe %s: no se sobrescribe (las notas no se reescriben)."
                  % os.path.relpath(destino, ROOT))
            return 1
    write(raiz, texto)
    write(copia, texto)
    print("Creadas %s y %s" % (os.path.relpath(raiz, ROOT), os.path.relpath(copia, ROOT)))
    print("Complete los cinco bloques y ejecute:  python tools/check_notas.py --freeze")
    return 0


# --------------------------------------------------------------------- modo --freeze
def modo_freeze(force=False):
    if os.path.isfile(MANIFEST) and cargar_manifiesto() is None:
        print("FALLO: docs/notas-indice.json existe pero no es valido; no se sobrescribe.")
        return 1
    data = cargar_manifiesto() or {"politica": POLITICA, "notas": []}
    actual = version_actual()
    previas = {n["version"]: n for n in data["notas"]}
    disco = versiones_en_disco()
    hoy = datetime.date.today().isoformat()
    cambios = 0

    for version in sorted(disco, key=key):
        entrada = previas.get(version) or {"version": version, "fecha": hoy,
                                           "secciones": list(ORDEN_SECCIONES),
                                           "congelada": False}
        rutas = {}
        for campo, ruta in (("root", "NOTAS-%s.md" % version),
                            ("docs", "docs/NOTAS-%s.md" % version)):
            if ruta in disco[version].values():
                rutas[campo] = ruta

        # Primero se comprueba la huella antigua; solo despues se recalcula.
        if entrada.get("congelada"):
            for ruta, huella in (entrada.get("sha256") or {}).items():
                p = os.path.join(ROOT, ruta)
                if not os.path.isfile(p):
                    print("FALLO: la nota congelada %s no existe" % ruta)
                    return 1
                if sha256(p) != huella and not force:
                    print("FALLO: %s cambio desde que se congelo. Las notas publicadas no se "
                          "reescriben: cree NOTAS-%s.md con los cambios nuevos." % (ruta, actual))
                    return 1

        for campo, ruta in rutas.items():
            entrada[campo] = ruta
            entrada.setdefault("sha256", {})[ruta] = sha256(os.path.join(ROOT, ruta))
        # La version en curso sigue abierta; el resto queda congelado para siempre.
        entrada["congelada"] = (version != actual)
        if version not in previas:
            cambios += 1
            print("Registrada %s%s" % (version, " (congelada)" if entrada["congelada"] else " (abierta)"))
        previas[version] = entrada

    data["politica"] = POLITICA
    data["actual"] = actual
    data["notas"] = [previas[v] for v in sorted(previas, key=key)]
    write(MANIFEST, json.dumps(data, ensure_ascii=False, indent=2) + "\n")
    print("Actualizado %s (%d notas, %d nuevas)"
          % (os.path.relpath(MANIFEST, ROOT), len(data["notas"]), cambios))
    return 0


POLITICA = ("Las notas de version son permanentes: no se borran ni se reescriben. Cada version "
            "anade su propio archivo NOTAS-X.Y.Z.md (raiz y docs/, identicos) con los bloques "
            "antes / plan / hecho / pendiente. Al publicar, tools/check_notas.py --freeze guarda "
            "su huella SHA-256 y la validacion falla si alguien la elimina o la modifica.")


# --------------------------------------------------------------------- comprobacion
def main():
    actual = version_actual()
    disco = versiones_en_disco()
    data = cargar_manifiesto()

    print("Notas de version")
    if not disco:
        fail("no hay ninguna nota NOTAS-X.Y.Z.md")
        return resumen()

    if data is None:
        fail("falta docs/notas-indice.json: registre las notas con "
             "python tools/check_notas.py --freeze")
        return resumen()

    registradas = {n["version"]: n for n in data["notas"] if n.get("version")}

    # 1. Registro <-> disco
    for version in sorted(set(registradas) | set(disco), key=key):
        entrada = registradas.get(version)
        sitios = disco.get(version, {})
        if entrada is None:
            fail("la nota %s no esta registrada en docs/notas-indice.json "
                 "(ejecute --freeze)" % version)
            continue
        if not sitios:
            fail("la nota de la version %s esta registrada pero no existe como archivo: "
                 "las notas de version no se borran" % version)
            continue
        if "root" not in sitios or "docs" not in sitios:
            fail("la version %s necesita la nota en la raiz y en docs/ (falta %s)"
                 % (version, "docs/" if "root" in sitios else "NOTAS-%s.md" % version))
            continue

        raiz = os.path.join(ROOT, sitios["root"])
        copia = os.path.join(ROOT, sitios["docs"])
        a, b = read(raiz), read(copia)
        if a != b:
            fail("%s y %s no son identicas" % (sitios["root"], sitios["docs"]))

        declaradas = entrada.get("secciones") or list(ORDEN_SECCIONES)
        comprobar_contenido(version, a, declaradas, sitios["root"])

        if entrada.get("congelada"):
            huellas = entrada.get("sha256") or {}
            for ruta, archivo in ((sitios["root"], raiz), (sitios["docs"], copia)):
                esperada = huellas.get(ruta)
                if not esperada:
                    fail("%s esta congelada pero no tiene huella registrada (ejecute --freeze)"
                         % ruta)
                elif sha256(archivo) != esperada:
                    fail("%s se modifico despues de publicarse. Las notas no se reescriben: "
                         "anada NOTAS-<nueva>.md con los cambios" % ruta)

    if not errors:
        ok("%d notas registradas y completas (%s)"
           % (len(registradas), ", ".join(sorted(registradas, key=key))))

    # 2. La version en curso tiene su nota
    if actual:
        if actual not in registradas:
            fail("version.json dice %s pero no hay NOTAS-%s.md" % (actual, actual))
        else:
            ok("la version en curso (%s) tiene nota" % actual)

    # 3. Toda version publicada en VERSIONES.md tiene nota
    if os.path.isfile(VERSIONES):
        minimo = min(registradas, key=key) if registradas else actual
        texto = read(VERSIONES)
        for version in sorted(set(re.findall(r"\|\s*\**(\d+\.\d+\.\d+)\**\s*\|", texto)), key=key):
            if minimo and key(version) >= key(minimo) and version not in registradas:
                fail("docs/VERSIONES.md publica la %s y no tiene nota propia (NOTAS-%s.md)"
                     % (version, version))
        ok("todas las versiones publicadas desde %s tienen nota" % minimo)

    # 4. Historial de git
    comprobar_historial(registradas)

    return resumen()


def resumen():
    print()
    for w in warnings:
        print("AVISO: " + w)
    if errors:
        print("FALLOS (%d):" % len(errors))
        for e in errors:
            print("  - " + e)
        print()
        print("Las notas de version son el historial del proyecto y no se borran.")
        print("Para una version nueva:  python tools/check_notas.py --new X.Y.Z")
        return 1
    print("Notas de version correctas.")
    return 0


if __name__ == "__main__":
    args = sys.argv[1:]
    if "--list" in args:
        d = cargar_manifiesto()
        if not d:
            sys.exit(1)
        for n in d["notas"]:
            print("%-8s %s %s" % (n["version"],
                                 "congelada" if n.get("congelada") else "abierta ",
                                 ", ".join(n.get("sha256", {}).keys()) or "(sin archivo)"))
        sys.exit(0)
    if "--new" in args:
        i = args.index("--new")
        if i + 1 >= len(args):
            print("Uso: python tools/check_notas.py --new X.Y.Z")
            sys.exit(2)
        sys.exit(modo_new(args[i + 1]))
    if "--freeze" in args:
        sys.exit(modo_freeze(force="--force" in args))
    sys.exit(main())
