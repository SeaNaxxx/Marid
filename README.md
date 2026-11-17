# Qodex

Native Android coding agent running an embedded Node.js Bionic ARM64 runtime locally.

[![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/platform-Android%20ARM64-green.svg)](https://developer.android.com)
[![Engine](https://img.shields.io/badge/node-v24.14.0%20(Bionic)-orange.svg)](https://nodejs.org)
[![UI](https://img.shields.io/badge/UI-Jetpack%20Compose-purple.svg)](https://developer.android.com/jetpack/compose)

---

## Overview

Qodex is an on-device AI coding agent designed for Android ARM64 devices. It separates the native mobile interface from the agent execution engine:

- **Host Interface (`android/`):** Native Jetpack Compose UI providing chat, diff viewing, workspace browsing, and settings. Handles Android lifecycle, background services, and system integrations.
- **Engine Subprocess (`apps/qodex-cli/`):** Runs on a standalone Node.js 24 Bionic ARM64 binary within the application sandbox. Executes the agent loop, tool dispatch, Git commands, and file edits based on `zai-org/zcode`.
- **Native Tools (`android/app/src/main/jniLibs/`):** Standalone ARM64 utilities (`ripgrep`, `fd-find`) compiled for Android Bionic and 16KB page alignment.

Communication between the Android host and the Node engine runs exclusively over the engine process's stdio pipes using the upstream `SocketProtocol` binary framing (13-byte headers reassembled by `ChunkStream`). The application opens no network listener — no localhost HTTP/WebSocket servers, no Unix domain sockets — and embeds no WebViews for its primary interface.

---

## Architecture

```
┌────────────────────────────────────────────────────────┐
│             Android Host (Kotlin / Compose)            │
│  - Jetpack Compose UI (Chat, Diff, Workspace Tree)     │
│  - Keystore AES-GCM + DataStore (API Key Management)   │
│  - Foreground Service (specialUse) + Partial WakeLock  │
│  - System APIs (SAF, PackageInstaller, Shizuku IPC)   │
└──────────────────────────┬─────────────────────────────┘
                           │ stdio binary-frame RPC
                           ▼
┌────────────────────────────────────────────────────────┐
│             Engine Subprocess (Node.js 24)             │
│  - Runtime: Node.js 24.14.0 (Bionic ARM64)             │
│  - CLI Engine: apps/qodex-cli + packages/rpc           │
│  - Task execution loop, tool dispatch, LLM streaming   │
│  - Workspace file read/write, patch application, Git   │
└──────────────────────────┬─────────────────────────────┘
                           │ ProcessBuilder / exec
                           ▼
┌────────────────────────────────────────────────────────┐
│             Native Binaries (ARM64 Bionic)             │
│  - libripgrep.so (`rg`) for codebase regex search      │
│  - libfd.so (`fd`) for fast path traversal             │
│  - libgit.so / Git CLI                                 │
└────────────────────────────────────────────────────────┘
```

---

## Android Platform Specifications

1. **W^X Execution (targetSdk >= 29):**
   Android SELinux disallows `exec()` calls on writable app data paths (`filesDir`, `cacheDir`). All native ELF binaries (`node`, `git`, `rg`, `fd`) are packaged in `jniLibs/arm64-v8a/` with `lib` prefixes and extracted to `applicationInfo.nativeLibraryDir` using `android.packaging.jniLibs.useLegacyPackaging = true`. Workspace scripts (`.js`, `.ts`, `.py`) are read as data by `libnode.so` and executed in-process without triggering native execution restrictions.

2. **16KB Page Alignment (Android 15 / 16):**
   All native binaries and shared objects are built using NDK r28+ with `-Wl,-z,max-page-size=16384` to ensure compatibility with 16KB kernel page sizes.

3. **Background Lifecycle:**
   Long-running tasks run inside an Android `ForegroundService` typed as `specialUse` with `FOREGROUND_SERVICE_SPECIAL_USE` declarations and a partial `WakeLock` to prevent OEM process suspension during code generation.

4. **Credential Security:**
   Provider API keys are encrypted at rest via hardware-backed Android Keystore (AES-GCM) and Jetpack DataStore. Keys are injected into the engine subprocess in-memory via environment variables or stdin during session initialization.

5. **Storage and Package Management:**
   - Workspace access uses the Storage Access Framework (SAF) for user-selected directories.
   - APK builds trigger installation prompts via Android `PackageInstaller`.
   - Elevated shell operations can optionally route through Shizuku when authorized by the user.

---

## Repository Layout

```
Qodex/
├── android/                   # Native Android application
│   ├── app/
│   │   ├── src/main/java/     # Jetpack Compose UI, ViewModels, Services, IPC
│   │   ├── src/main/jniLibs/  # Native ARM64 binaries (libnode.so, libripgrep.so, libgit.so)
│   │   └── build.gradle.kts   # Packaging configuration
│   └── build.gradle.kts
├── apps/
│   └── qodex-cli/             # Node.js agent engine (derived from ZCode CLI runtime)
│       ├── packages/cli/      # Entry point and runtime handlers
│       └── packages/core/     # Agent loop, tool dispatch, and permissions
├── packages/                  # Reused TypeScript modules
│   ├── rpc/                   # JSON-RPC protocol schemas and types
│   ├── client/                # RPC client definitions
│   ├── provider/              # Model provider adapters
│   └── shared/                # Common types and utilities
├── AGENTS.md                  # Guidelines and constraints for AI coding agents
└── README.md                  # Project technical documentation
```

---

## Prerequisites and Building

### Prerequisites
- **Android Studio:** Ladybug (2024.2.1+) or newer
- **Android NDK:** r28+
- **Node.js:** v24.14.0+ and `pnpm` v9+
- **Device:** Android 14+ ARM64 device (tested on Snapdragon 8 Gen 2 / Gen 3 / 8 Elite)

### Building the Project

1. **Build the CLI runtime bundle:**
   ```bash
   pnpm install
   pnpm --filter @qodex/cli build
   ```

2. **Assemble the Android application:**
   ```bash
   cd android
   ./gradlew assembleDebug
   ```

---

## License and Attribution

- Qodex is licensed under the [Apache License, Version 2.0](LICENSE).
- Core CLI and protocol components are derived from [`zai-org/zcode`](https://github.com/zai-org/zcode) under Apache-2.0.
- Third-party component notices are documented in [NOTICE.md](NOTICE.md) and [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md).
