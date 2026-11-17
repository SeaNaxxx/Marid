# Runtime Transport & 16KB Roadmap (Phase 1 done, Phase 3 spike pending)

This document records what is implemented, what is deliberately NOT changed in
this cycle, and the verification roadmap for the remaining runtime decisions
(16KB page size, `targetSdk` elevation, hermetic Node binary).

## 1. Implemented: stdio RPC transport (Phase 1) — DONE, CI-verified

The Android host and the Node engine now communicate exclusively over the
engine process's stdio pipes:

- **Engine:** `packages/server/src/entry-stdio.ts` mounts the RPC
  `ChannelServer` directly on `process.stdin`/`process.stdout`
  (`SocketProtocol` 13-byte framing reassembles partial pipe reads). All
  diagnostics go to stderr (`stdio-log-guard.ts`); stdout carries frames only.
  stdin close / SIGTERM / EPIPE → engine exits (no orphans).
- **Host:** `EngineProcess.kt` spawns `entry-stdio.js` with
  `redirectErrorStream(false)`, pipes stdout/stderr exactly as the directive
  requires, and drops every TCP-port artifact (random port picking, HTTP
  health check, adopt-survivor logic, port marker file).
- **Client:** `QodexRpcClient.kt` replaced OkHttp WebSocket with direct pipe
  I/O into the existing `SocketFraming.kt` / `ChunkStream` / `ChannelClient`
  stack. CONNECTED fires only when the engine's Initialize handshake arrives.
  One `ChannelClient` per engine process (the server sends Initialize once at
  process start; transport loss = full engine respawn via `restartEngine`).
- **Security posture:** there is no listener at all — no TCP port for other
  apps to snoop, no UDS file races, no `LocalSocket` API-level concerns, and
  the localhost cleartext exemption was removed from
  `network_security_config.xml`.
- **Verification:** `scripts/qodex-android/stdio-smoke.mts` drives the real
  `entry-stdio.js` with the real `@qodex/rpc` client over raw pipes and
  completes `provider-settings.getView`; it runs as a CI gate in
  `.github/workflows/build.yml`. On-device verification (boot → handshake →
  chat turn) remains a Phase 0 device task — CI proves protocol correctness
  on the host, not device behavior.

### Why stdio over UDS (audit conclusions)

- Abstract-namespace UDS bypasses filesystem permissions; filesystem UDS
  brings the 108-byte `sun_path` limit, stale-socket `EADDRINUSE`, and the
  API 33+ `UnixDomainSocketAddress` restriction vs `minSdk 24`.
- Inherited pipes from `ProcessBuilder.start()` have none of those problems
  and are fully isolated to the parent/child pair.

## 2. Deliberately unchanged this cycle (Phase 3 non-goals)

- `targetSdk` stays **28** with the documented W^X rationale in
  `android/app/build.gradle.kts`. Raising it before a proven `jniLibs`-based
  runtime would break `exec()` of the runtime-extracted Bionic node.
- `termux-debs` offline provisioning (`Provisioner.kt`, `DebExtractor.kt`)
  stays the shipping runtime path.
- OkHttp dependency removed; if a future remote-workspace feature needs an
  HTTP client, reintroduce it deliberately.

## 3. 16KB page-size spike (Phase 3 — verification only, pending device work)

Known facts from the audit:

1. Termux-built binaries in the deb closure are 4KB-aligned; devices running
   16KB kernels (REDMAGIC 10 / Snapdragon 8 Elite / Dimensity 9400 class)
   may fail to load or crash on them.
2. `BionicEnv.kt` wires >20 shared libraries (`libcrypto`, `libssl`,
   `libc++`, `libuv`, `libcares`, ...) via `LD_LIBRARY_PATH`; moving `node`
   alone into `jniLibs` would break `dlopen` for this closure.

Spike checklist (device/emulator, Android 15+ 16KB system image):

- [ ] Boot the current provisioning pipeline on a 16KB page-size emulator;
      record exactly which ELFs refuse to load (`dlopen` / `execve` logs).
- [ ] Read ELF LOAD alignment for every runtime binary
      (`llvm-readelf -l`, expect failures at 0x1000).
- [ ] Evaluate upstream Termux packages rebuilt for 16KB, versus building a
      hermetic Node.

## 4. Hermetic static Node roadmap (enables targetSdk 35+)

End state: one statically linked, 16KB-aligned `libnode.so` shipped in
`android/app/src/main/jniLibs/arm64-v8a/` executing from
`applicationInfo.nativeLibraryDir` (`useLegacyPackaging = true`), eliminating
the deb-extraction runtime and unlocking `targetSdk` 35/36 with Play policy
compliance.

Steps, in order, each gated on device evidence:

1. Build Node 24 with NDK r28 for `aarch64-unknown-linux-android`
   (static OpenSSL, no external `.so` dependencies), or evaluate a
   Bionic-compatible prebuilt; document build config + binary size.
2. Validate `execve` from `nativeLibraryDir` on target API levels
   (permissions, SELinux, extraction behavior).
3. Verify engine boot through the stdio transport with the packaged binary
   (same smoke protocol as CI, run on device).
4. Only then: raise `targetSdk`, remove the deb pipeline, and re-run the
   full Phase 0 battery (FGS survival, Doze, memory, recovery).

## 5. Phase 2 native module status

`crates/qodex-native` is scaffolded (JNI `cdylib`, `ignore` + `regex`
crates, 16KB rustflags, opt-in `buildQodexNative` Gradle task). It is NOT
wired into the agent search path yet — wiring is gated on device benchmarks
(see `crates/qodex-native/README.md`).
