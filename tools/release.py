#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Publica una version nueva: compila la APK firmada, calcula su huella, actualiza
version.json (raiz y docs/) y crea la release de GitHub con el APK adjunto.

    python tools/release.py --build --publish

Requisitos:
  · JDK 17 + Android SDK 34 (los mismos que para compilar a mano).
  · android/keystore/keystore.properties con la firma de distribucion.
  · Para --publish: variable de entorno GITHUB_TOKEN con permiso de escritura.
  · Opcional: git en el PATH para confirmar y subir version.json; si no esta,
    se actualiza por la API de GitHub (solo esos dos archivos).

La app lee version.json para avisar de la actualizacion, asi que la huella y el
tamano deben corresponder exactamente al APK subido. Este script lo garantiza.
"""
import base64
import hashlib
import json
import os
import re
import shutil
import subprocess
import sys
import urllib.error
import urllib.parse
import urllib.request

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ANDROID = os.path.join(ROOT, "android")
GRADLE = os.path.join(ANDROID, "app", "build.gradle.kts")
VERSION = os.path.join(ROOT, "version.json")
VERSION_DOCS = os.path.join(ROOT, "docs", "version.json")
DIST = os.path.join(ROOT, "dist")
OWNER = "emanuelindustri-source"
REPO = "consumo-cij-calculadora"
API = "https://api.github.com/repos/%s/%s" % (OWNER, REPO)


def read(path):
    with open(path, encoding="utf-8") as f:
        return f.read()


def sha256(path):
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for block in iter(lambda: f.read(65536), b""):
            h.update(block)
    return h.hexdigest()


def gradle_value(pattern):
    m = re.search(pattern, read(GRADLE))
    return m.group(1) if m else None


def find_apk():
    base = os.path.join(ANDROID, "app", "build", "outputs", "apk", "release")
    if not os.path.isdir(base):
        return None
    apks = [os.path.join(base, n) for n in os.listdir(base) if n.endswith(".apk")]
    return max(apks, key=os.path.getmtime) if apks else None


def build():
    gradle = shutil.which("gradle")
    if not gradle:
        home = os.environ.get("GRADLE_HOME")
        if home:
            cand = os.path.join(home, "bin", "gradle.bat" if os.name == "nt" else "gradle")
            if os.path.isfile(cand):
                gradle = cand
    if not gradle:
        print("ERROR: no se encontro gradle. Defina GRADLE_HOME o instale Gradle 8.7.")
        return None
    print("Compilando APK de distribucion…")
    r = subprocess.run([gradle, "assembleRelease", "--no-daemon", "--console=plain"], cwd=ANDROID)
    if r.returncode != 0:
        print("ERROR: la compilacion fallo.")
        return None
    apk = find_apk()
    if not apk:
        print("ERROR: no se genero la APK.")
    return apk


def update_manifest(apk):
    v = json.loads(read(VERSION))
    version = v["version"]
    name = "ConsumoCIJ-%s.apk" % version
    os.makedirs(DIST, exist_ok=True)
    target = os.path.join(DIST, name)
    shutil.copy2(apk, target)
    v["apk"] = name
    v["apkSizeBytes"] = os.path.getsize(target)
    v["apkSha256"] = sha256(target)
    v["apkUrl"] = ("https://github.com/%s/%s/releases/download/v%s/%s"
                   % (OWNER, REPO, version, name))
    text = json.dumps(v, ensure_ascii=False, indent=2) + "\n"
    for path in (VERSION, VERSION_DOCS):
        with open(path, "w", encoding="utf-8", newline="\n") as f:
            f.write(text)
    print("  APK:    %s (%d bytes)" % (target, v["apkSizeBytes"]))
    print("  SHA256: %s" % v["apkSha256"])
    return v, target


def api(method, url, token, data=None, raw=False, ctype="application/json"):
    body = data
    if data is not None and not raw:
        body = json.dumps(data).encode("utf-8")
    req = urllib.request.Request(url, data=body, method=method)
    req.add_header("Authorization", "Bearer " + token)
    req.add_header("Accept", "application/vnd.github+json")
    req.add_header("User-Agent", "consumo-cij-release")
    req.add_header("X-GitHub-Api-Version", "2022-11-28")
    if body is not None:
        req.add_header("Content-Type", ctype)
    with urllib.request.urlopen(req, timeout=120) as r:
        text = r.read().decode("utf-8")
        return json.loads(text) if text.strip() else {}


def commit_manifests_api(token, version):
    """Confirma version.json y docs/version.json en main usando la API."""
    head = api("GET", API + "/git/ref/heads/main", token)["object"]["sha"]
    commit = api("GET", API + "/git/commits/" + head, token)
    tree = []
    for rel in ("version.json", "docs/version.json"):
        content = base64.b64encode(read(os.path.join(ROOT, rel)).encode("utf-8")).decode("ascii")
        blob = api("POST", API + "/git/blobs", token,
                   {"content": content, "encoding": "base64"})
        tree.append({"path": rel, "mode": "100644", "type": "blob", "sha": blob["sha"]})
    new_tree = api("POST", API + "/git/trees", token,
                   {"base_tree": commit["tree"]["sha"], "tree": tree})
    new_commit = api("POST", API + "/git/commits", token, {
        "message": "release: publicar la version %s" % version,
        "tree": new_tree["sha"],
        "parents": [head],
    })
    api("PATCH", API + "/git/refs/heads/main", token,
        {"sha": new_commit["sha"], "force": False})
    print("  version.json confirmado en main (%s)" % new_commit["sha"][:8])


def git_commit(token):
    git = shutil.which("git")
    if not git:
        return False
    try:
        subprocess.run([git, "-C", ROOT, "add", "version.json", "docs/version.json"], check=True)
        r = subprocess.run([git, "-C", ROOT, "commit", "-m", "release: version.json con la huella del APK"],
                           capture_output=True, text=True)
        if r.returncode != 0:
            return False
        if token:
            remotes = subprocess.run([git, "-C", ROOT, "remote", "get-url", "origin"],
                                     capture_output=True, text=True).stdout.strip()
            if remotes:
                url = re.sub(r"https://[^@]+@", "https://", remotes)
                ascii_url = url.replace("https://", "https://x-access-token:%s@" % token, 1)
                subprocess.run([git, "-C", ROOT, "push", ascii_url, "HEAD:main"], check=True)
        return True
    except Exception:
        return False


def publish(token, v, apk_path):
    version = v["version"]
    tag = "v" + version
    notes_path = os.path.join(ROOT, "NOTAS-%s.md" % version)
    body = read(notes_path) if os.path.isfile(notes_path) else "Consumo CIJ %s" % version

    try:
        release = api("GET", API + "/releases/tags/" + tag, token)
        print("  release %s ya existe" % tag)
    except urllib.error.HTTPError as e:
        if e.code != 404:
            raise
        release = api("POST", API + "/releases", token, {
            "tag_name": tag,
            "target_commitish": "main",
            "name": "Consumo CIJ %s" % version,
            "body": body,
            "draft": False,
            "prerelease": False,
        })
        print("  release %s creada" % tag)

    name = os.path.basename(apk_path)
    for asset in release.get("assets", []):
        if asset["name"] == name:
            api("DELETE", API + "/releases/assets/%d" % asset["id"], token)
    upload_url = ("https://uploads.github.com/repos/%s/%s/releases/%d/assets?name=%s"
                  % (OWNER, REPO, release["id"], urllib.parse.quote(name)))
    with open(apk_path, "rb") as f:
        data = f.read()
    asset = api("POST", upload_url, token, data, raw=True,
                ctype="application/vnd.android.package-archive")
    print("  APK subida: %s" % asset.get("browser_download_url", name))
    return release


def main():
    do_build = "--build" in sys.argv
    do_publish = "--publish" in sys.argv
    token = os.environ.get("GITHUB_TOKEN", "").strip()

    v = json.loads(read(VERSION))
    version = v["version"]
    vn = gradle_value(r'versionName\s*=\s*"([^"]+)"')
    vc = gradle_value(r"versionCode\s*=\s*(\d+)")
    if vn != version or str(vc) != str(v.get("versionCode")):
        print("ERROR: version.json (%s / %s) no coincide con build.gradle.kts (%s / %s)."
              % (version, v.get("versionCode"), vn, vc))
        return 1

    apk = build() if do_build else None
    if not apk:
        existing = os.path.join(DIST, "ConsumoCIJ-%s.apk" % version)
        apk = existing if os.path.isfile(existing) else None
    if not apk:
        print("ERROR: no hay APK que publicar. Use --build.")
        return 1

    v, dist_apk = update_manifest(apk)
    print()
    print("Siguiente paso: confirme version.json en el repositorio (git) para que")
    print("la app pueda ver la version nueva; despues publique la release.")
    print("  git add version.json docs/version.json && git commit -m \"release %s\" && git push" % version)

    if not do_publish:
        print()
        print("Para crear la release y subir la APK:  python tools/release.py --publish")
        return 0

    if not token:
        print("ERROR: defina GITHUB_TOKEN para publicar.")
        return 1
    try:
        if not git_commit(token):
            commit_manifests_api(token, version)
        publish(token, v, dist_apk)
    except urllib.error.HTTPError as e:
        print("ERROR de la API (%s): %s" % (e.code, e.read().decode("utf-8", "replace")[:400]))
        return 1
    except Exception as e:
        print("ERROR: %s" % e)
        return 1

    print()
    print("Publicado. Compruebe:")
    print("  https://github.com/%s/%s/releases/tag/v%s" % (OWNER, REPO, version))
    print("  https://%s.github.io/%s/" % (OWNER, REPO))
    print("  https://raw.githubusercontent.com/%s/%s/main/version.json" % (OWNER, REPO))
    return 0


if __name__ == "__main__":
    sys.exit(main())
