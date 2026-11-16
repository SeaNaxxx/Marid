# OLD-ZCODE-RESEARCH — Phase A reference research (SeaNaxxx/Zcode-Android)

Status: research output for the Marid fresh-build decision (2026-11-13).
Method: read-only clone of `SeaNaxxx/Zcode-Android` (HEAD `62359c5` at research
time). No Kotlin was copied; concepts, failure history, and evidence were
extracted so Marid's fresh code starts from the proven lessons instead of
rediscovering them on a device.

> Scope note: the task brief named paths that do not exist in the old repo
> (`local/PRootEnv.kt`, `shizuku/ShizukuExecutor.kt`, `assets/runtime/`
> committed to git). Section 1 records what actually exists; Sections 2–5
> answer the research questions against the real code.

## 1. Repository map (what actually exists)

| Research question | Assumed file | Reality |
|---|---|---|
| Bionic runtime | `local/BionicEnv.kt`, `Provisioner.kt`, `TarExtractor.kt` | All present (`app/local/`), plus `RuntimeState.kt` (holds `RuntimePaths` + state machine), `ZcodeServer.kt`, `LocalServerService.kt` |
| PRoot | `local/PRootEnv.kt` | **Does not exist.** proot invocation is inline in `ZcodeServer.start()` (legacy branch); rootfs provisioning is the non-bionic branch of `Provisioner.provision()` |
| Terminal / PTY | assets runtime binaries | `node-pty` ships inside the ZCode dist but **never worked on Android** — `PTY-PROBE-VERDICT: NEGATIVE` (§4) |
| Shizuku | `shizuku/ShizukuExecutor.kt` | **Never implemented.** Appears only in docs as explicitly out of scope (§5) |
| Embedded runtime | `assets/runtime/bionic/` | Built by CI (`scripts/embedded/build-bionic-bundle.py` + `android-verify.yml`), embedded into the APK at build time; nothing is committed to git |

App shell facts: `minSdk 24`, `targetSdk 28` (deliberate), `compileSdk 34`,
`commons-compress:1.27.1`, Compose UI.

## 2. Bionic runtime (the proven path)

### 2.1 Where the Node binary comes from (build time)

`scripts/embedded/build-bionic-bundle.py` builds the bundle from the **Termux
APT repository** (`packages.termux.dev`, stable/main, pinned index):

- Seeds: `nodejs-lts`, `git`, `bash`, `curl`, `ca-certificates`,
  `termux-exec`, `libandroid-support`, plus explicitly seeded `libmount`,
  `libblkid` (a Depends-closure gap found by the first bundle audit).
- Final manifest: 53 pinned packages — nodejs-lts 24.22.2, git 2.56.0,
  bash 5.3.20, openssl 3.6.5, curl 8.23.0, libicu 78.3, libsqlite 3.54.1, …
- Every `.deb` is SHA256-verified against the index stanza **before**
  extraction (guarantee G4). Man/doc/locale (+npm) are pruned. Absolute
  Termux symlinks (`/data/data/com.termux/...`) are rewritten to relative.
- Output: `prefix.tgz` (tar root `usr/`) + `prefix.sha256` +
  `prefix.critical.txt` (linker-critical sonames) + `manifest.json` +
  `cacert.pem` (Mozilla bundle, 118 certs).

Embedded node binary (audit-verified): ELF 64-bit **AArch64**, ET_DYN,
interpreter **`/system/bin/linker64`**, "for Android 24, built by NDK r30",
v24.22.2. NEEDED libs: libz / libcares / libsqlite3 / libcrypto.so.3 /
libssl.so.3 / libicui18n|uc.so.78 (materialized inside the prefix) +
libc/libm/libdl (Android system allowlist). The CI also builds the same
closure for x86_64 — one userspace, two ABIs.

### 2.2 Bundle guarantees (device-failure driven)

- **G1** — every linker-critical soname (DT_NEEDED of every staged ELF) is a
  REAL FILE in `usr/lib`, byte-copied from its target before tarring. Linker
  closure never depends on device symlink support.
- **G2** — the delivered tar contains ZERO hardlink entries (deb-materialized
  duplicates become relative symlinks); a tar-level audit fails the build
  otherwise. Devices refuse `link()` (AccessDeniedException, 135x observed).
- **G3** — `usr/etc/tls/openssl.cnf` and `usr/etc/tls/cert.pem` are present,
  because the Termux-built libcrypto compiles `OPENSSLDIR` in as
  `/data/data/com.termux/files/usr/etc/tls` (proven via `strings`), which is
  unreadable for another app and FATAL at node startup.
- **G4** — per-deb SHA256 verification at bundle build.

### 2.3 Where it lives at runtime + layout

Old repo layout (all under app-private storage):

```
<filesDir>/runtime/
  usr/bin/{node,bash,git,...}     Termux prefix (extracted from prefix.tgz)
  usr/lib/                        shared-lib closure (sonames = real files)
  usr/etc/tls/{openssl.cnf,cert.pem,cacert-zcode.pem}
  usr/lib/ossl-modules/
  dist/zcode/                     official ZCode distribution
  bionic/                         offline agent kit scripts
  home/                           HOME (tmp/, .gitconfig)
  zcode-data/                     ZCODE_DATA_BASE_DIR
  provision-complete.json         marker
```

Marid Phase A will use `<filesDir>/bionic/` with the same internal shape
(usr/...) and a Marid-owned marker.

### 2.4 Env contract (`BionicEnv.kt` — contracts to inherit, not code to copy)

Pure-JVM, unit-testable contract applied to every spawned process:

| Variable | Value / purpose |
|---|---|
| `PREFIX` | `<runtime>/usr` |
| `HOME` | `<runtime>/home` |
| `TMPDIR` | `<runtime>/home/tmp` |
| `PATH` | `<usr>/bin:/system/bin:/system/xbin` |
| `LD_LIBRARY_PATH` | `<usr>/lib` |
| `SHELL` | `<usr>/bin/bash` |
| `LANG` | `C.UTF-8`; `NODE_OPTIONS=""` (immune to ambient host values) |
| `OPENSSL_CONF` | always a readable file INSIDE the runtime (fallback written if bundled one missing — an unreadable target is fatal, exit 13) |
| `OPENSSL_MODULES` | `<usr>/lib/ossl-modules` (overrides MODULESDIR compiled into libcrypto) |
| `SSL_CERT_FILE` / `SSL_CERT_DIR` / `GIT_SSL_CAINFO` | bundled real CA files/dirs |
| `NODE_EXTRA_CA_CERTS` | app-shipped Mozilla bundle (appends roots; never weakens) |
| `GIT_PAGER`/`PAGER`/`GIT_CONFIG_NOSYSTEM`/`GIT_TEMPLATE_DIR`/`GIT_EXEC_PATH` | neutralize Termux-compiled-in git paths |
| `ZCODE_DATA_BASE_DIR` | redirect agent data store into app-private storage |

Owner rules attached to the contract: every variable points INSIDE the app
runtime (never at Termux); TLS verification is NEVER disabled (no
`NODE_TLS_REJECT_*`, no empty CA stores); variables are set only when their
target actually exists (or is materialized) in the bundle.

### 2.5 Extraction lessons (device failures 1+2, Vivo V2035)

- **Failure 1 (2026-10-27):** `CANNOT LINK EXECUTABLE .../usr/bin/node:
  library "libz.so.1" not found`. Termux ships linker-critical sonames as
  SYMLINKS; the old audit matched NEEDED names against directory ENTRIES, so
  a symlink name passed. Fix → G1 (real files).
- **Failure 2 (2026-10-28):** two independent defects:
  1. Extraction used `java.nio.file.Path.of(...)` — absent before API 34 —
     and the `NoSuchMethodError` was SWALLOWED and reported as a "device
     refusal", masking the real defect for two device builds. Lesson:
     an app-API bug must FAIL LOUDLY, never masquerade as device behavior
     (`isAppApiBug` classification).
  2. `Files.createLink` genuinely refused on-device (AccessDeniedException
     x135) → hardlink tar entries forced ~199MB of budget-capped copies and
     left helper utilities absent. Fix → G2 (zero hardlinks in the tar).
  Plus the OpenSSL one: node exit 13, "OpenSSL configuration error" —
  compiled-in Termux OPENSSLDIR → EACCES. Fix → G3 + env wiring (§2.4).
- `TarExtractor` design points worth replicating as CONTRACTS: pure JVM (no
  android.* imports, injectable LinkOps for JVM tests), symlink → hardlink →
  budgeted-copy fallback chain (200 MB budget), both tar-member and
  parent-relative link-name resolution confined under the destination
  (path-traversal guard), tar mode bits applied via `File.setReadable/
  setWritable/setExecutable`, `usr/bin/*` explicitly chmod +x after
  extraction.

### 2.6 Provisioning gate + failure taxonomy

Provisioning runs the embedded node ON THE DEVICE as a gate:

`node -e "console.log(process.version + ' ' + process.platform + ' ' + process.arch)"`

expected `v24.22.2 linux arm64`. Failures are classified by output signature:
`CANNOT LINK` → runtime file missing (bundle/extraction defect); "OpenSSL
configuration error" → app-side env defect; any `com.termux` in output →
env leaked a Termux path. Nothing is faked; every state is real and carries
its stage.

Self-healing pattern: assets (CA bundle, kit scripts, rg kit) are re-checked
per start by comparing asset SHA against the staged copy, so a device that
provisioned with an older APK refreshes on the next start without a full
re-provision.

Residual risk [documented, not fixed]: ~19 baked `/data/data/com.termux`
strings in the node binary (13 in libcrypto) — inert because the app uid
differs and G3 redirects every resolution; noted because Phase B logs must
not misread these as leaks.

## 3. PRoot (legacy mode — Marid will NOT use it)

- Invocation: `<nativeLibDir>/libproot.so -r <rootfs> -0 -w /root -b /dev
  -b /proc -b /sys -b <workspace>:/workspace [-b /sdcard:/sdcard]
  /opt/node/bin/node /opt/zcode/bin/zcode.mjs --web --no-open
  --host 127.0.0.1 --port 3030 --workspace /workspace`.
- proot + libtalloc ship as **jniLibs** (`libproot.so`) so Android's W^X
  policy keeps them executable — a trick worth remembering for any future
  external binary.
- Rootfs: Ubuntu base 24.04.3 arm64 (downloaded, SHA256SUMS-verified) +
  Node.js 24.14.0 **glibc** linux-arm64 (nodejs.org, SHASUMS256-verified).
- Env: `LD_LIBRARY_PATH=<nativeLibDir>`, `PROOT_NO_SECCOMP=1`, `HOME=/root`,
  `PATH=/opt/node/bin:...`.
- Constraints observed: /sdcard bind retry loop, 120 s health deadline on
  `http://127.0.0.1:3030`, requires network on first run (fails the
  airplane-mode provisioning gate that bionic mode passes).
- Verdict: superseded by the embedded bionic path everywhere; Marid skips it
  entirely.

## 4. Terminal / PTY

- `node-pty 1.1.0` prebuilds in the dist cover win32/darwin/linux-x64/
  linux-arm64 — ALL glibc (`ld-linux-aarch64.so.1`, `libutil.so.1`,
  `libpthread.so.0`). **No android/bionic prebuild exists anywhere.**
- On-device probe verdict (CI run 42103274954): **`PTY-PROBE-VERDICT:
  NEGATIVE`** — `Cannot find module './prebuilds/android-x64/pty.node'`;
  the probe requires a real `pty.spawn()`, not merely a module load.
- Consequence in the old repo: the terminal UI was honestly GATED and never
  built; both workflow and code treat PTY as unavailable.
- The sanctioned pattern for delivering glibc-unfriendly native code on
  bionic is the **musl static-loader kit** (ripgrep from Alpine 3.20:
  `rg` + `ld-musl-aarch64.so.1` + pcre2 + libgcc, launched via
  `lib/ld-musl --library-path lib lib/rg`). This is the template for any
  future node-pty fix; an on-device bionic rebuild against the shipped
  node headers (`usr/include/node/*`, node-gyp-build present) is plausible
  but unproven.
- Marid verdict: no terminal UI in any phase until a bionic-compatible PTY
  is proven by a positive on-device probe. Never fake it.

## 5. Shizuku

- **Never implemented.** The only mention is a scope line in
  `docs/architecture/ARCHITECTURE.md`: "ADB/Shizuku/REA stay out of scope
  until later phases."
- What the app used instead of privileged shell: app-private storage only,
  a foreground service owning the runtime lifecycle, and same-uid leftover
  cleanup via `pidof node` + `android.os.Process.killProcess` (Android uid
  isolation keeps this scoped to the app's own processes).
- Marid verdict: not needed for Phase A (a plain `ProcessBuilder` spawn of
  an app-private binary needs no privileges). Revisit only if a future
  phase genuinely requires shell-UID operations.

## 6. App-shell platform lessons that carry into Marid

1. **targetSdk 28 is deliberate** (Android 10+ W^X policy blocks exec() of
   binaries in app-writable storage; pre-Q target keeps it permitted — the
   Termux/proot-distro ecosystem precedent). The old repo documents a
   re-raising strategy (native library-dir launch) as a prerequisite for any
   higher targetSdk. **Marid's current scaffold (targetSdk 34, minSdk 26)
   MUST change to targetSdk 28, minSdk 24 in Phase A.**
2. **Dark mode on pre-Q targets:** apps targeting <29 do not receive the
   system night mode in their `Configuration`; read `UiModeManager` and
   qualify the base context (relevant for later UI phases).
3. **Runtime lifecycle ownership:** a foreground service (`dataSync`) owns
   spawn/stop; the UI never spawns Node; `START_REDELIVER_INTENT` +
   adopt-if-healthy probe (never spawn a duplicate node on a served port);
   explicit Stop must win (no auto-restart after a deliberate stop); leave
   leftovers to `pidof` cleanup. Phase B/C pattern — Phase A stays
   service-less for the one-shot probe.
4. **Loopback-only server contract** (`127.0.0.1:3030`) and SSRF guards for
   any server-side URL fetch (https-only, host allowlist, per-hop
   re-validation, DNS pinning) — Phase B/C relevance.
5. **Verification model:** an x86_64 emulator proves APP LOGIC only — never
   ARM64 hardware behavior; CI API level must not exceed the acceptance
   device's (a real past failure: CI had API 34, device 33, masking
   `Path.of`). Device rows stay NOT TESTED until executed on the phone;
   protocol gates T0–T5 (T1 = provisioning must succeed in airplane mode).
6. **Evidence culture:** persist runtime stdout to a rotated `server.log`,
   write `diag-report.json`, mirror evidence lines to logcat, print marker
   lines (`...-OK/FAIL`) greppable by CI. "Nothing is faked" is enforced by
   making every claim a disk/log effect.

## 7. What Marid does differently (fresh-code deltas)

- Fresh Kotlin throughout — the old files are reference reading, not copied
  sources. Phase A scope: `runtime/BionicRuntime.kt` (paths, env contract,
  spawn + probe, failure taxonomy), a fresh pure-JVM tar extractor sized to
  Phase A needs (same guarantees), minimal MainActivity (Start Node +
  output), `runtime/NodeBinarySource.kt` (binary provenance + verification).
- Layout `<filesDir>/bionic/`, Marid-branded data-dir variable and probe
  string ("Marid Node OK ..."), Marid logcat tag.
- Inherited as CONTRACTS (re-implemented, not pasted): the env contract
  (§2.4), bundle guarantees G1–G4 when reusing the proven prefix bundle,
  fail-loud app-API-bug classification, TLS-verification-never-disabled,
  targetSdk 28 / minSdk 24, on-device node probe as the phase gate.
- Node binary source decision: evaluated in the Phase A design proposal
  (unofficial-builds vs nodejs-mobile vs source build vs the proven 0.6.0
  bundle closure).

## 8. Evidence basis

- Clone: `SeaNaxxx/Zcode-Android` @ `62359c5` (2026-11-13, read-only).
- Code read: `BionicEnv.kt` (164 L), `Provisioner.kt` (780 L),
  `TarExtractor.kt` (288 L), `RuntimeState.kt` (117 L), `ZcodeServer.kt`
  (541 L), `LocalServerService.kt` (263 L), `MainActivity.kt` (367 L),
  `app/build.gradle.kts`, `scripts/embedded/build-bionic-bundle.py`.
- Docs read: `ENGINEERING-NOTES.md` §3–§5, `ANDROID-RUNTIME-COMPATIBILITY-AUDIT.md`
  (release 0.6.0 binary-level audit), `DEVICE-VALIDATION.md`,
  `architecture/ARCHITECTURE.md`.
- Releases: 0.7.0 / 0.6.0 / 0.5.0 arm64 debug APKs (181.5 / 175.9 / 192.2 MB);
  0.6.0 is the audit-verified release.
