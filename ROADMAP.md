# Qodex Architecture & Engineering Roadmap

Qodex is an autonomous, on-device AI coding agent designed natively for Android (ARM64). It pairs a native Jetpack Compose interface with an in-process Bionic Node.js engine and high-performance Rust native accelerators via JNI, completely eliminating the need for web views or desktop shims.

---

## 1. Architectural Baseline

| Layer | Technology | Responsibility |
| :--- | :--- | :--- |
| **User Interface** | Jetpack Compose (Material 3) | Native Android UX, session navigation, settings hub, diff viewer, terminal sheet, back gesture handling, and system life cycle. |
| **Agent Engine** | Node.js (Bionic / ARM64) | Agentic orchestration loop, tool resolution, conversation state machines, and LLM communication. |
| **IPC Bridge** | Bi-directional stdio streams | Framed RPC messaging over standard process pipes (`stdin`/`stdout`), free of open TCP ports or unauthenticated localhost websockets. |
| **Native Accelerator** | Rust (`crates/qodex-native` JNI) | In-process workspace traversal, ripgrep-grade `.gitignore` walking, and pattern matching compiled to a `cdylib` (`libqodex_native.so`) that strictly honors Android W^X. |

---

## 2. Roadmap Phases

### Phase 1: Core Runtime, Cloud Providers & Native UI (Current)

- [x] **In-Process Stdio RPC**: Migrate engine transport from local HTTP/WebSocket to private stdio stream framing.
- [x] **Transactional Single-Flight Provisioning**: Ensure runtime extraction checks SHA markers and only extracts when missing or invalid.
- [x] **Runtime Staging Fix**: Correct Bionic extraction destination paths so `usr/bin/node` and `usr/bin/bash` reliably resolve without nested prefix duplication.
- [x] **Dynamic Model Provider Hub**: Full UI and RPC wiring for cloud providers (OpenRouter, DeepSeek, OpenAI, Anthropic, Z.ai, and custom OpenAI-compatible endpoints) via user-provided API keys.
- [x] **Clean Native UI (No AI Slop)**: Pure Jetpack Compose Material 3 dark/light interface with monospace code blocks, drawer navigation, and task history.
- [ ] **Unified Zstandard Runtime Archive**: Transition from 48 loose `.deb` files in assets to a single pre-baked `runtime.tar.zst` decompressed via bundled `libzstd-jni` in one linear pass, reducing cold boot from ~60s to <4s.

### Phase 2: Native Acceleration, File Ops & System Bridges

- [x] **Rust Native Scaffold**: Ingest `crates/qodex-native` compiling with `ignore` and `regex` crates for native file walking.
- [ ] **JNI Workspace Engine**: Route workspace search tools from the TypeScript engine directly to `QodexNative.walkFiles()` and pattern searching, outperforming JavaScript I/O loops.
- [ ] **Storage Access Framework (SAF)**: Provide full folder-level workspace mounts outside the internal sandbox without requiring root.
- [ ] **Interactive Diff & Git Log**: Dedicated Compose diff viewer with side-by-side and unified syntax-highlighted diffs.
- [ ] **Optional Shizuku (`rish`) Bridge**: Elevate system commands and package inspection for authorized power users without altering base app security.

### Phase 3: Offline AI Pocket & Extensibility Ecosystem

- [ ] **Offline AI Pocket (Local LLM Engine)**:
  - Integrate an on-device quantized model runtime (e.g., llama.cpp / ExecuTorch with Qualcomm NPU / Vulkan acceleration).
  - Target lightweight pocket models (1.5B–3B parameters) for offline code analysis, quick tool routing, and commit message drafting.
  - Implement seamless fallback: complex reasoning delegates to cloud providers (OpenRouter), while offline/instant actions run in the Pocket.
- [ ] **Model Context Protocol (MCP) Client**: Full client support for external MCP servers over stdio and SSE.
- [ ] **Agent Skills & Custom Hooks**: Port ZCode desktop skills, browser automation (via Android Accessibility/WebView), and REPL plugins to the mobile engine.

---

## 3. Security & Platform Compatibility

1. **Target SDK & Executable Rules (W^X)**:
   - Native code is distributed as shared libraries (`.so`) inside `jniLibs/arm64-v8a/` and mapped by the Android dynamic linker.
   - Node.js runtime binaries execute within the app's private internal storage (`filesDir`), isolated from other apps.
2. **Secret Management**:
   - Provider API keys and session tokens are encrypted using Android Keystore / EncryptedSharedPreferences.
3. **16KB Page Alignment**:
   - All native shared libraries (`libqodex_native.so`, `libzstd-jni.so`, and Bionic binaries) comply with Android 15+ 16KB memory page alignment mandates.
