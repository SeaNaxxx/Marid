#!/usr/bin/env node
// Packs the Termux .deb set into ONE consolidated Bionic usr archive:
//   android/app/src/main/assets/runtime/qodex-usr.tgz (+ .sha256 sidecar)
//
// Why: provisioning used to run 53 individual ar → data.tar.xz → tar
// extraction passes on device (one per deb), each with its own asset copy and
// sha256 round trip — a long black-screen phase on first boot. This script
// assembles the same usr/ tree ONCE at build time:
//   1. ar-extract every deb, unxz data.tar.xz, untar into a staging tree
//   2. re-root ./data/data/com.termux/files/usr under <stage>/usr
//   3. strip man/doc/info pages (never read on device)
//   4. tar -czf the usr/ tree with plain `usr/...` root paths — the layout
//      Provisioner.requireUsrBinaries gates on after a single TarExtractor pass
//
// The .deb inputs stay in the repo (runtime source of truth) but are expected
// OUTSIDE assets/runtime after packing, so the APK ships the consolidated
// archive instead of the raw deb set.
import { spawnSync } from "node:child_process";
import { createHash } from "node:crypto";
import { cp, mkdir, mkdtemp, readFile, readdir, rm, stat, writeFile } from "node:fs/promises";
import os from "node:os";
import { join, resolve } from "node:path";

function argValue(argv, flag, index) {
  if (argv[index] === flag) {
    const value = argv[index + 1];
    if (!value) throw new Error(`Missing value for ${flag}`);
    return { value, nextIndex: index + 1 };
  }
  if (argv[index]?.startsWith(`${flag}=`)) {
    return { value: argv[index].slice(flag.length + 1), nextIndex: index };
  }
  return null;
}

const argv = process.argv.slice(2);
let debDir = null;
let outFile = null;
for (let i = 0; i < argv.length; i += 1) {
  const d = argValue(argv, "--debs", i);
  if (d) { debDir = resolve(d.value); i = d.nextIndex; continue; }
  const o = argValue(argv, "--out", i);
  if (o) { outFile = resolve(o.value); i = o.nextIndex; continue; }
  throw new Error(`Unknown argument "${argv[i]}"`);
}
if (!debDir || !outFile) {
  throw new Error("Usage: pack-runtime-usr.mjs --debs <dir-with-.deb> --out <file.tgz>");
}

function run(command, args, options = {}) {
  const result = spawnSync(command, args, { stdio: "inherit", ...options });
  if (result.error) throw new Error(`${command} failed to start: ${result.error.message}`);
  if (result.status !== 0) throw new Error(`${command} ${args.join(" ")} exited ${result.status}`);
}

const STRIP_DIRS = ["share/man", "share/doc", "share/info"];

async function exists(path) {
  try {
    await stat(path);
    return true;
  } catch {
    return false;
  }
}

const stage = await mkdtemp(join(os.tmpdir(), "qodex-usr-"));
const debNames = (await readdir(debDir)).filter((name) => name.endsWith(".deb")).sort();
if (debNames.length === 0) throw new Error(`no .deb files under ${debDir}`);
console.log(`[pack-usr] consolidating ${debNames.length} deb(s) from ${debDir}`);

// Pass 1: every deb's data.tar lands under <stage>/deb/<name>/data/data/com.termux/files/usr.
for (const name of debNames) {
  const debStage = join(stage, "deb", name.replace(/\.deb$/, ""));
  await mkdir(debStage, { recursive: true });
  run("ar", ["x", resolve(debDir, name), "data.tar.xz"], { cwd: debStage });
  const dataTar = join(debStage, "data.tar.xz");
  if (!(await exists(dataTar))) {
    throw new Error(`${name}: no data.tar.xz member (only xz-compressed debs are supported)`);
  }
  run("tar", ["-xJf", dataTar, "-C", debStage]);
  await rm(dataTar, { force: true });
}

// Pass 2: re-root each deb's usr tree under <stage>/usr (first deb wins on
// conflicts — the same ordering the per-deb device pipeline had).
const usrRoot = join(stage, "usr");
let files = 0;
for (const name of debNames) {
  const debUsr = join(stage, "deb", name.replace(/\.deb$/, ""), "data", "data", "com.termux", "files", "usr");
  if (!(await exists(debUsr))) {
    throw new Error(`${name}: unpacked tree missing usr/ — unexpected deb layout`);
  }
  // cp -aT semantics via tar keeps modes/symlinks; overwrite-merge with existing.
  run("cp", ["-a", `${debUsr}/.`, usrRoot]);
  await rm(join(stage, "deb", name.replace(/\.deb$/, "")), { recursive: true, force: true });
}

// Pass 3: strip docs that can never be read on device.
for (const sub of STRIP_DIRS) {
  await rm(join(usrRoot, sub), { recursive: true, force: true });
}

// Structural gate before writing the archive: the exact files provisioning
// gates on must exist, otherwise the APK would ship an archive that is
// guaranteed to fail on device.
for (const bin of ["bin/node", "bin/bash"]) {
  const path = join(usrRoot, bin);
  if (!(await exists(path))) throw new Error(`usr/${bin} missing after consolidation`);
}

run("du", ["-sh", usrRoot]);

await rm(outFile, { force: true });
// Root-relative `usr/...` entries — matches the Provisioner single-pass layout.
// gzip -9 over a tar pipe: same decompression speed on device, smaller APK.
run("sh", ["-c", `tar -C '${stage}' -cf - usr | gzip -9 > '${outFile}'`]);
const shaFile = resolve(outFile.replace(/\.tgz$/, ".sha256"));
const hash = createHash("sha256").update(await readFile(outFile)).digest("hex");
await writeFile(shaFile, `${hash}\n`);
console.log(`[pack-usr] wrote ${outFile}`);
console.log(`[pack-usr] wrote ${shaFile}`);
console.log(`[pack-usr] sha256: ${hash}`);

await rm(stage, { recursive: true, force: true });
