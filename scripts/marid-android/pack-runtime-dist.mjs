#!/usr/bin/env node
// Pack the @marid/server engine dist into the Android runtime bundle that
// android/app/src/main/assets/runtime/ expects (see runtime/Provisioner.kt):
//   marid-dist.tgz      { package.json ("marid-engine"), dist/, node_modules/ }
//   marid-dist.sha256   bare sha256 hex of the tarball (Provisioner.verifySha256)
//
// node_modules is the RUNTIME DEPENDENCY CLOSURE derived from the literal
// bare imports present in the BUILT dist (the esbuild external list plus any
// dynamic requires that survived bundling — e.g. zod). Workspace packages
// (@marid/*) are bundled INTO dist by tsup (noExternal) and are therefore not
// copied. Packages are resolved Node-style against the hoisted workspace
// root node_modules (package-local node_modules first, then root), so
// nested/conflicting versions are copied at their actual resolution paths.
//
// Usage:
//   node scripts/marid-android/pack-runtime-dist.mjs \
//     --dist packages/server/dist \
//     --node-modules node_modules \
//     --out android/app/src/main/assets/runtime/marid-dist.tgz
import { spawnSync } from "node:child_process";
import { createHash } from "node:crypto";
import { cp, mkdir, mkdtemp, readFile, readdir, rm, stat, writeFile } from "node:fs/promises";
import { isBuiltin } from "node:module";
import os from "node:os";
import { join, relative, resolve } from "node:path";

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
let distDir = null;
let nodeModulesRoot = null;
let outFile = null;
for (let i = 0; i < argv.length; i += 1) {
  const d = argValue(argv, "--dist", i);
  if (d) { distDir = resolve(d.value); i = d.nextIndex; continue; }
  const n = argValue(argv, "--node-modules", i);
  if (n) { nodeModulesRoot = resolve(n.value); i = n.nextIndex; continue; }
  const o = argValue(argv, "--out", i);
  if (o) { outFile = resolve(o.value); i = o.nextIndex; continue; }
  throw new Error(`Unknown argument "${argv[i]}"`);
}
if (!distDir || !nodeModulesRoot || !outFile) {
  throw new Error("Usage: pack-runtime-dist.mjs --dist <dir> --node-modules <dir> --out <file.tgz>");
}

const BARE_SPECIFIER = [
  // require("x") / import("x") with a literal argument (esbuild keeps these
  // verbatim for external/dynamic deps)
  /(?:require|import)\s*\(\s*["']([^"'\n]+)["']\s*\)/g,
  // static import/export source: named imports are MULTILINE in tsup output
  // (import {\n  A,\n  B\n} from "x"), so match the `from "..."` clause itself
  // rather than the (brace-ful) clause head.
  /\bfrom\s*["']([^"'\n]+)["']/g,
  // bare side-effect `import "x";`
  /(?:^|[^\w$.])import\s*["']([^"'\n]+)["']/g,
];

function topLevelPackage(specifier) {
  if (!specifier) return null;
  if (specifier.startsWith(".") || specifier.startsWith("/")) return null;
  if (specifier.startsWith("node:")) return null;
  if (isBuiltin(specifier)) return null;
  const parts = specifier.split("/");
  if (specifier.startsWith("@")) {
    if (parts.length < 2) return null;
    return `${parts[0]}/${parts[1]}`;
  }
  return parts[0] || null;
}

async function listSourceFiles(dir) {
  const out = [];
  async function walk(current) {
    const entries = await readdir(current, { withFileTypes: true });
    for (const entry of entries) {
      const full = join(current, entry.name);
      if (entry.isDirectory()) await walk(full);
      else if (/\.(m|c)?js$/.test(entry.name)) out.push(full);
    }
  }
  await walk(dir);
  return out;
}

async function collectRootImports() {
  const files = await listSourceFiles(distDir);
  const roots = new Set();
  for (const file of files) {
    const text = await readFile(file, "utf8");
    for (const regex of BARE_SPECIFIER) {
      regex.lastIndex = 0;
      let match;
      while ((match = regex.exec(text)) !== null) {
        const pkg = topLevelPackage(match[1]);
        if (pkg) roots.add(pkg);
      }
    }
  }
  return roots;
}

async function isDirectory(path) {
  try {
    return (await stat(path)).isDirectory();
  } catch {
    return false;
  }
}

async function packageDeps(pkgDir) {
  let manifest;
  try {
    manifest = JSON.parse(await readFile(join(pkgDir, "package.json"), "utf8"));
  } catch {
    return [];
  }
  const dependencies = manifest.dependencies ?? {};
  const optionalDependencies = manifest.optionalDependencies ?? {};
  const peerDependencies = manifest.peerDependencies ?? {};
  const lists = [dependencies, optionalDependencies, peerDependencies];
  const deps = [];
  for (const list of lists) {
    for (const [name, spec] of Object.entries(list)) {
      if (typeof spec !== "string") continue;
      if (spec.startsWith("workspace:") || spec.startsWith("catalog:")) continue;
      if (!deps.includes(name)) deps.push(name);
    }
  }
  return deps;
}

async function main() {
  const roots = await collectRootImports();
  if (roots.size === 0) throw new Error(`No bare imports found under ${distDir}`);
  console.log(`[pack-runtime] dist imports ${roots.size} package(s): ${[...roots].sort().join(", ")}`);

  const stage = await mkdtemp(join(os.tmpdir(), "marid-dist-"));
  const stageNodeModules = join(stage, "node_modules");
  await mkdir(stageNodeModules, { recursive: true });
  await cp(distDir, join(stage, "dist"), { recursive: true });
  await writeFile(
    join(stage, "package.json"),
    JSON.stringify({ name: "marid-engine", type: "module" }, null, 2) + "\n",
  );

  const copied = new Map(); // relPath -> srcDir
  const queue = [...roots].map((name) => ({ name, fromDir: nodeModulesRoot }));
  while (queue.length > 0) {
    const { name, fromDir } = queue.pop();
    const candidates = [];
    // nearest node_modules first (package-local), then ancestors up to root
    let current = fromDir;
    for (;;) {
      candidates.push(join(current, "node_modules", name));
      if (current === nodeModulesRoot || !current.startsWith(nodeModulesRoot)) break;
      current = resolve(current, "..");
    }
    candidates.push(join(nodeModulesRoot, name));
    let srcDir = null;
    for (const candidate of candidates) {
      if (await isDirectory(candidate)) { srcDir = candidate; break; }
    }
    if (!srcDir) {
      // Unresolvable imports are tolerated: dist contains try/catch guarded
      // require() calls for OPTIONAL peers (ws -> bufferutil / utf-8-validate)
      // that pnpm did not install. Ground truth for "required" is the server
      // package manifest, verified after the closure walk.
      console.warn(`[pack-runtime] WARN: ${name} not resolvable from ${fromDir} — skipped`);
      continue;
    }
    const rel = relative(nodeModulesRoot, srcDir);
    if (!rel || rel.startsWith("..")) throw new Error(`Resolved ${name} outside ${nodeModulesRoot}: ${srcDir}`);
    if (copied.has(rel)) continue;
    copied.set(rel, srcDir);
    const dest = join(stageNodeModules, rel);
    await mkdir(resolve(dest, ".."), { recursive: true });
    await cp(srcDir, dest, { recursive: true });
    const deps = await packageDeps(srcDir);
    for (const dep of deps) queue.push({ name: dep, fromDir: resolve(srcDir, "..") });
  }

  console.log(`[pack-runtime] bundled ${copied.size} package(s) into node_modules/`);

  // Hard verification: every server dependency that dist ACTUALLY imports
  // must be bundled. This intentionally does NOT include:
  //   - @lydell/node-pty-* platform forks (declared in the manifest but never
  //     imported — dist imports plain "node-pty"; the committed bundle also
  //     ships without them), and
  //   - try/catch-guarded optional peers (bufferutil / utf-8-validate), which
  //     are dist imports but not manifest dependencies.
  const ownerManifestPath = resolve(distDir, "..", "package.json");
  const ownerManifest = JSON.parse(await readFile(ownerManifestPath, "utf8"));
  const serverNonWorkspaceDeps = new Set(
    Object.entries(ownerManifest.dependencies ?? {})
      .filter(([, spec]) => typeof spec === "string" && !spec.startsWith("workspace:") && !spec.startsWith("catalog:"))
      .map(([name]) => name),
  );
  const required = [...roots].filter((name) => serverNonWorkspaceDeps.has(name));
  const missing = required.filter((name) =>
    ![...copied.keys()].some((rel) => rel === name || rel.endsWith(`node_modules/${name}`)));
  if (missing.length > 0) {
    throw new Error(`Runtime closure incomplete — missing required packages: ${missing.join(", ")}`);
  }

  await rm(outFile, { force: true });
  const tar = spawnSync("tar", ["-czf", outFile, "-C", stage, "package.json", "dist", "node_modules"], {
    stdio: "inherit",
  });
  if (tar.error || tar.status !== 0) {
    throw new Error(`tar -czf failed${tar.status ? ` (exit ${tar.status})` : ""}`);
  }
  const shaFile = resolve(outFile.replace(/\.tgz$/, ".sha256"));
  const hash = createHash("sha256").update(await readFile(outFile)).digest("hex");
  await writeFile(shaFile, `${hash}\n`);
  await rm(stage, { recursive: true, force: true });
  console.log(`[pack-runtime] wrote ${outFile}`);
  console.log(`[pack-runtime] wrote ${shaFile}`);
  console.log(`[pack-runtime] sha256: ${hash}`);
}

await main();
