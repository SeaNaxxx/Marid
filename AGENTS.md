# AGENTS.md — Qodex Project Rules & Architecture Directive

> **Target Audience:** Coding Agents (GLM, Claude, Qwen, Codex) and Human Contributors.  
> **Repository:** Qodex (Native Android Autonomous Coding Agent)  
> **Base Upstream:** `zai-org/zcode` (Apache-2.0)  

---

## 1. Core Identity & Architectural North Star

Qodex is an autonomous, on-device software engineering agent built natively for **Android ARM64** (Android 14+ / Android 16).

### The "Brave vs. Chromium" Model
- **Upstream Engine (`apps/qodex-cli`):** We reuse the agent execution loop, prompt compositions, tool calling, provider adapters, and protocol types from `zai-org/zcode`.
- **Native Host Shell (`android/`):** We discard all Desktop/Electron/Web frontend code (`packages/desktop`, `packages/web`, `packages/ui` React code) and replace it entirely with a **100% native Android Jetpack Compose UI**.
- **Absolute Non-Negotiables:**
  - **NO WebViews** for the primary application interface.
  - **NO localhost HTTP/WebSocket servers** (e.g. `127.0.0.1:3030`). The `--web --port 3030` flags in upstream ZCode are for desktop browser debugging only.
  - **NO Electron or Desktop container emulation.**
  - **Transport:** Communication between Kotlin (Host) and Node.js (Engine) runs strictly over **Standard I/O (stdio NDJSON)** or local abstract Unix Domain Sockets (UDS).

---

## 2. Tri-Language Responsibility Matrix

```
┌────────────────────────────────────────────────────────┐
│             Kotlin (Android Host & UI)                 │
│  - Jetpack Compose (Theme, Chat, Diff, Tree, Terminal) │
│  - Android Keystore AES-GCM + DataStore (Secrets)      │
│  - Foreground Service (specialUse) + WakeLock          │
│  - System APIs (SAF, PackageInstaller, Shizuku Bridge) │
└──────────────────────────┬─────────────────────────────┘
                           │ stdio NDJSON RPC
                           ▼
┌────────────────────────────────────────────────────────┐
│         JavaScript / TypeScript (Agent Brain)          │
│  - Subprocess: Node.js 24.14.0 (Bionic ARM64)          │
│  - Upstream Runtime: apps/qodex-cli + packages/rpc     │
│  - Agent Loop, Tool Dispatcher, LLM Streaming          │
│  - Git Operations, File Read/Write, AST Analysis       │
└──────────────────────────┬─────────────────────────────┘
                           │ Child Process Exec
                           ▼
┌────────────────────────────────────────────────────────┐
│             Rust (Targeted Native Muscle)              │
│  - Standalone Bionic Binaries (16KB page-aligned)       │
│  - libripgrep.so (`rg`) & libfd.so (`fd`) in jniLibs   │
│  - Targeted DEX/AXML/APK high-speed parsers (NAPI-RS)   │
└────────────────────────────────────────────────────────┘
```

### A. Kotlin (Host, UI & Android Guardian)
- **UI & State:** 100% Jetpack Compose. Follow native Android guidelines, smooth touch targets, edge-to-edge layout, and adaptive dark/light themes.
- **Background Survival:** Long agent runs must be bound to an Android `ForegroundService` with `specialUse` type and a partial `WakeLock`. Never rely on default background process priority.
- **Hardware Security:** BYOK API Keys (OpenRouter, Anthropic, Gemini, OpenAI) must be encrypted via **Android Keystore (AES-GCM)** and saved in Jetpack DataStore (`androidx.security.crypto` is deprecated). Keys are transmitted to the Node engine strictly in-memory over IPC/env vars; never written as plain text to disk.
- **Android OS Integrations:**
  - Storage Access Framework (SAF) for user-granted directories.
  - `PackageInstaller` for user-confirmed APK installations.
  - Android Intents and Sharesheet for sharing diffs and APKs.
  - Optional **Shizuku** bridge for executing elevated shell commands without root.

### B. JavaScript & TypeScript (The Agent Brain & Tool Orchestrator)
- **Runtime Environment:** Standalone Node.js 24.14.0 Bionic ARM64 binary executing inside the application sandbox UID (`/data/user/0/com.qodex.app/`).
- **Core Loop:** LLM prompt formatting, streaming token processing, tool selection, error retries, and context window pruning.
- **Workspace Execution:** Creating directories, modifying source code, computing git diffs, running Node scripts.
- **RPC Client & Protocol:** Reuse message definitions from `packages/rpc`. Do not reinvent message structures; wrap them in line-delimited JSON (`NDJSON`) over stdio.

### C. Rust (Specialized High-Performance Tools)
- **Strict Constraint:** Do **NOT** rewrite agent orchestration, networking, or Compose UI in Rust.
- **Allowed Scope:**
  - Pre-compiled, standalone ARM64 Bionic CLI utilities placed into `jniLibs`: `ripgrep` (`rg`) for rapid codebase search, and `fd-find` for file discovery.
  - Future/Optional: Fast binary inspection of Android APKs/DEX files via `napi-rs` or CLI if Node.js hits memory/CPU ceilings.

---

## 3. Verified Execution Realities (Platform Constraints)

### 1. W^X Policy (targetSdk >= 29)
On Android 10+ with `targetSdk >= 29`, SELinux enforces W^X (Write XOR Execute) on `app_data_file`.
- **Rule:** Never attempt `exec()` or `chmod +x` on files placed in `context.filesDir` or `context.cacheDir`.
- **Packaging:** All native ELF executables (`node`, `git`, `rg`, `fd`) must be packaged in `android/app/src/main/jniLibs/arm64-v8a/` with a `lib` prefix (e.g., `libnode.so`, `libgit.so`, `librg.so`).
- **Gradle Setting:** Set `android.packaging.jniLibs.useLegacyPackaging = true` in `build.gradle.kts` so libraries are extracted as physical files to `context.applicationInfo.nativeLibraryDir` (which has execute permissions).
- **Dynamic Scripting Clarification:** User scripts, `.js`, `.ts`, and `.py` files written by the agent into the workspace live as standard data. `libnode.so` executes them by reading their text into memory. This is standard file reading, **NOT** `execve()`, and is completely unaffected by W^X.

### 2. 16KB Page Alignment (Android 15 / 16)
Modern Android devices (including REDMAGIC 10 Air / Snapdragon 8 Elite) run with 16KB kernel page sizes.
- Every native shared object or executable (`libnode.so`, `librg.so`, `libgit.so`) must be built using NDK r28+ or `cargo-ndk` with 16KB ELF alignment (`-Wl,-z,max-page-size=16384`).
- Verification: `readelf -l <binary> | grep -A 1 LOAD` must show alignment `>= 0x4000`.

### 3. Foreground Service & Battery Optimization
- On Android 14–16, `dataSync` is capped at 6 hours and deprecated.
- Declare `android:foregroundServiceType="specialUse"` with `FOREGROUND_SERVICE_SPECIAL_USE` permission.
- Always prompt the user to grant "Unrestricted" battery optimization for seamless long-running background tasks.

### 4. IPC Architecture (Stdio NDJSON)
- Avoid local HTTP/WebSocket sockets to prevent Android 16 Local Network Permission (LNP) dialogs and attack surface from other apps.
- Kotlin spawns `libnode.so` with ProcessBuilder and communicates via `stdin`/`stdout`.
- Stdio messages are encoded as Newline-Delimited JSON (NDJSON):
  ```json
  {"jsonrpc":"2.0","method":"session/stream","params":{"token":"..."}}
  ```
- If the Android UI process terminates, the OS automatically closes the stdio pipes, cleanly signaling the Node process to shut down and preventing orphaned zombie processes.

### 5. Memory & Process Quotas
- Android limits background child processes (32 per UID on Android 12+). Reuse persistent shell workers instead of spawning hundreds of short-lived processes.
- Set Node heap constraint: `--max-old-space-size=512` (or `768` on devices with >= 12GB RAM).
- Implement persistent session checkpoints so if the OS kills the process under low memory, Qodex can resume seamlessly.

---

## 4. Coding Agent Guardrails & Anti-AI Slop Rules

1. **Mobile-First UX / No AI Slop:**
   - No robotic marketing headers like "Supercharge your coding!!". Use clear, functional action language: "Apply Diff", "Run Test", "Save Settings".
   - Avoid generic solid-black rectangular buttons with ALL-CAPS text. Use rounded Jetpack Compose Material 3 buttons with semantic colors.
   - Respect mobile touch targets (minimum 48dp) and keyboard insets (`WindowInsets.ime`).
2. **Atomic Changes & Grounded Investigation:**
   - Before modifying code, inspect relevant files and state owners.
   - Do not hallucinate or assume dependencies exist in upstream ZCode; check `package.json` and imports.
   - Make small, testable commits. Never refactor working logic unless required by the task.
3. **Respect Upstream Licenses:**
   - Preserve Apache-2.0 copyright and notices when reusing packages from `zai-org/zcode`.
