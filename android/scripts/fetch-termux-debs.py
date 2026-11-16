#!/usr/bin/env python3
"""Fetch the Termux Bionic deb closure for Qodex's embedded Android runtime.

Seeds (owner task list): nodejs-lts, libandroid-support, openssl, libicu,
zlib, libsqlite, ca-certificates, bash.

The seeds alone are NOT a linker closure (Zcode-Android device failure 1:
`CANNOT LINK EXECUTABLE usr/bin/node: library "libz.so.1" not found` — the
Node binary's DT_NEEDED set is wider than the seed list). This script
resolves the FULL Depends closure from the pinned Termux index, downloads
every .deb from the pool, verifies each against the index SHA256 (Zcode-Android
guarantee G4), and stages them for APK embedding.

Usage:
  fetch-termux-debs.py <aarch64|x86_64> <outdir>

Exit codes: 0 ok; 2 resolve/download error; 3 checksum mismatch.
"""
import os
import pathlib
import sys
import urllib.request
import hashlib
from concurrent.futures import ThreadPoolExecutor

ARCHES = {"aarch64": "binary-aarch64", "x86_64": "binary-x86_64"}
INDEX_URL = (
    "https://packages.termux.dev/apt/termux-main/dists/stable/main/"
    "{binary}/Packages"
)
SEEDS = [
    "nodejs-lts",
    "libandroid-support",
    "openssl",
    "libicu",
    "zlib",
    "libsqlite",
    "ca-certificates",
    "bash",
]


def parse_packages(text: str):
    pkgs = {}
    for stanza in text.split("\n\n"):
        fields = {}
        key = None
        for line in stanza.splitlines():
            if line.startswith(" ") and key:
                fields[key] += "\n" + line.strip()
                continue
            if ":" in line:
                key, _, val = line.partition(":")
                fields[key.strip()] = val.strip()
        name = fields.get("Package")
        if name:
            pkgs[name] = fields
    return pkgs


def dep_names(depends_field: str):
    if not depends_field:
        return []
    out = []
    for alt in depends_field.split(","):
        first = alt.split("|")[0].strip()
        name = first.split(":")[0].split(" ")[0].strip()
        if name:
            out.append(name)
    return out


def resolve_closure(pkgs, seeds):
    closure = {}
    queue = [s for s in seeds if s in pkgs]
    missing = [s for s in seeds if s not in pkgs]
    while queue:
        name = queue.pop()
        if name in closure:
            continue
        f = pkgs[name]
        closure[name] = f
        for d in dep_names(f.get("Depends", "")):
            if d in pkgs and d not in closure:
                queue.append(d)
    return closure, missing


def fetch(url, dest):
    req = urllib.request.Request(url, headers={"User-Agent": "qodex-runtime-fetch/0.1"})
    with urllib.request.urlopen(req, timeout=60) as r, open(dest, "wb") as f:
        while True:
            chunk = r.read(1 << 16)
            if not chunk:
                break
            f.write(chunk)


def main():
    if len(sys.argv) != 3:
        print(__doc__)
        return 2
    arch, outdir = sys.argv[1], pathlib.Path(sys.argv[2])
    binary = ARCHES.get(arch)
    if not binary:
        print(f"unknown arch {arch}")
        return 2
    outdir.mkdir(parents=True, exist_ok=True)
    idx_url = INDEX_URL.format(binary=binary)
    print(f"index: {idx_url}")
    idx_path = outdir / "Packages.idx"
    fetch(idx_url, idx_path)
    pkgs = parse_packages(idx_path.read_text(errors="replace"))
    print(f"index packages: {len(pkgs)}")
    closure, missing = resolve_closure(pkgs, SEEDS)
    if missing:
        print(f"MISSING SEEDS (resolve error): {missing}")
        return 2
    print(f"closure size: {len(closure)} packages")
    total = 0
    jobs = []
    for name in sorted(closure):
        f = closure[name]
        fname = f.get("Filename")
        sha = f.get("SHA256", "")
        if not fname or not sha:
            print(f"  !! {name}: no Filename/SHA256 in index")
            return 2
        url = "https://packages.termux.dev/apt/termux-main/" + fname
        dest = outdir / (name + ".deb")
        jobs.append((name, f.get("Version", "?"), sha, url, dest))

    def grab(job):
        name, ver, sha, url, dest = job
        for attempt in (1, 2):
            if dest.exists():
                h = hashlib.sha256(dest.read_bytes()).hexdigest()
                if h == sha:
                    break
                # Stale/partial file (e.g. killed downloader): refetch.
                dest.unlink(missing_ok=True)
            fetch(url, dest)
        else:
            raise SystemExit(f"  !! {name}: SHA256 mismatch after refetch")
        size = dest.stat().st_size
        return name, ver, size

    with ThreadPoolExecutor(max_workers=8) as pool:
        for name, ver, size in pool.map(grab, jobs):
            total += size
            print(f"  {name} ({ver}) {size/1048576:.1f}MB OK")
    print(f"TOTAL {total/1048576:.1f}MB, {len(closure)} debs in {outdir}")
    (outdir / "manifest.txt").write_text(
        "\n".join(
            f"{n}\t{closure[n].get('Version','')}\t{closure[n].get('SHA256','')}"
            for n in sorted(closure)
        )
        + "\n"
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
