# qodex-native (Rust, Phase 2 baseline)

JNI `cdylib` loaded in-process by the Android host — **not** a standalone CLI
binary. This is the deliberate architecture choice from the Qodex directive:
shared libraries are mapped by the Android linker and are unaffected by W^X
`execve()` restrictions on app-writable storage, and they work on every
Android version without process-spawn overhead.

## Build (on a host with the Android NDK, not in Gradle CI)

```bash
rustup target add aarch64-linux-android
cargo install cargo-ndk
# NDK r27+ required for 16KB page-size defaults; r28 preferred.
export ANDROID_NDK_HOME=/path/to/android-ndk-r28
cargo ndk -t arm64-v8a -o ../../android/app/src/main/jniLibs build --release
```

`cargo-ndk` drops `libqodex_native.so` into
`android/app/src/main/jniLibs/arm64-v8a/`; Gradle packs it automatically.

## 16KB page alignment (Android 15+ kernels)

`.cargo/config.toml` enforces `-Wl,-z,max-page-size=16384` for the
`aarch64-linux-android` target. Verify after building:

```bash
llvm-readelf -l android/app/src/main/jniLibs/arm64-v8a/libqodex_native.so | grep -A1 LOAD
# LOAD segment align must be >= 0x4000
```

## Gradle integration (opt-in)

`android/app/build.gradle.kts` registers `buildQodexNative`, which runs only
when `-Pqodex.native=true` is passed AND `cargo` + `cargo-ndk` are on PATH.
CI builds stay deterministic (no Rust toolchain needed) until the native
module is production-ready.

```bash
./gradlew :app:assembleDebug -Pqodex.native=true
```

## Runtime contract (Kotlin side)

`com.qodex.app.native.QodexNative` loads lazily and degrades gracefully:
`available == false` and `*Safe(...) == null` whenever the `.so` is absent, so
callers always fall back to the Node engine path. No UI or agent flow depends
on this library yet — wiring `searchFilesSafe` into workspace search is a
Phase 2 follow-up gated on device benchmarks (compare against `rg` and Node
`fs` scans before replacing anything).

## Scope guard

Per the architecture directive: agent orchestration, prompt logic, and UI stay
in TypeScript and Kotlin/Compose. This crate is a leaf accelerator only
(workspace walk + regex scan). No IPC channels, no LLM calls, no state.
