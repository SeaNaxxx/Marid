# Android Agent Threat Model & Security Baseline

Repository: `SeaNaxxx/Qodex`
Scope: Android host (`android/`), local Bionic Node engine (`apps/qodex-cli` + `packages/*`), host↔engine transport (`stdio`), native module (`crates/qodex-native`).

This document is the security baseline the audit requested: every finding has an identifier, affected files, severity, remediation commit and regression test. It is a living document — new security-relevant changes must add or update a row.

## 1. Trust model

| Boundary | Trust level | Notes |
| --- | --- | --- |
| Android host (Kotlin) | Trusted. Owns the engine process, secrets, UI. | Spawns `libnode.so` with ProcessBuilder; communicates over stdio pipes only. |
| Node engine (TypeScript) | Semi-trusted. Executes the agent loop; reads provider credentials via env. | A defect here must not become a device-wide compromise: it runs as the app UID inside the app sandbox. |
| Model output / tool inputs | UNTRUSTED. Prompt injection is assumed: any instruction the model emits can be attacker-controlled. | This is why filesystem containment, permission asks and execution budgets exist below the model. |
| Workspace content | UNTRUSTED. Repositories, downloads, generated files. | Never treated as configuration of the agent itself. |

Non-negotiable transport rules (AGENTS.md): **no WebView UI, no localhost TCP/WS servers, no alternate host↔engine channel.** Stdio pipes only; closing the app closes the pipes, which is the engine shutdown signal.

## 2. Findings ledger

| ID | Severity | Area | Files | Status / commit | Regression test |
| --- | --- | --- | --- | --- | --- |
| C-01 | Critical | Workspace isolation was not enforced — `resolveWorkspacePath` normalized but never rejected escapes; absolute paths and `../` traversal reached read/write/execute handlers | `apps/qodex-cli/packages/core/src/tool/path-policy.ts` and every tool handler | **FIXED** `security:` — symlink-aware containment against `workspaceRoot` + optional host-authorized roots; enforced before every filesystem access | `apps/qodex-cli/packages/core/tests/path-policy.test.ts` (19 cases: traversal, absolute, sibling-prefix, symlink file+dir escape, not-yet-created targets, authorized roots, fs-root edge) |
| C-02 | Critical | Master credential secret lived in app-data files with no backup exclusion — Android Auto Backup could carry both ciphertext and wrapping key | `android/.../Provisioner.kt`, `BionicEnv.kt`, `AndroidManifest.xml` | **PARTIALLY FIXED** `security:` — `allowBackup=false` + `dataExtractionRules`/`fullBackupContent` excluding everything (cloud backup + device transfer); FileLogger moved off external storage to `noBackupFilesDir`. **Keystore-wrapped master key still OPEN** — see §4 roadmap | Manifest + rules XML; backup exclusion is platform behavior (no JVM test) |
| C-03 | Critical | Permission bypass: `yolo`/`guarded` short-circuited approval for every tool, including the host `node_repl` `js` tool that runs unsandboxed and documents that it skips Bash approval | `apps/qodex-cli/packages/core/src/permission/service.ts`, `mcp/index.ts` | **FIXED** `security:` — `js` tool now declares `alwaysAsk: true` + `askOptions.allowAlways: "session"` (the engine's designed "ask survives every permissive mode" mechanism); ordinary MCP tools unchanged | `tests/always-ask-mode-boundary.test.ts` (asks in yolo+guarded, deny still beats ask, yolo passthrough preserved for ordinary tools), `tests/mcp-tool-bridge.test.ts` |
| M-01 | Medium | Logging: diagnostics written to external storage; no redaction boundary for logcat/boot.log | `android/.../utils/FileLogger.kt`, `runtime/RuntimeBus.kt` | **FIXED** `security:` — `noBackupFilesDir` + central `LogRedactor` applied before logcat AND file (Authorization headers, provider key shapes, query-string secrets, JSON credential fields, `QODEX_CREDENTIAL_SECRET*` values) | `android/app/src/test/.../LogRedactorTest.kt` |
| M-02 | Medium | RPC framing: unsigned 32-bit length converted to Int without validation (negative-length desync), unbounded reassembly buffer | `android/.../rpc/SocketFraming.kt` | **FIXED** `security:` — 64 MiB `MAX_FRAME_BYTES` ceiling enforced in `acceptChunk` (pre-allocation) and at header inspection via typed `FrameTooLargeException`; negative `read()` rejected; outbound `frameRegular` guarded | `android/app/src/test/.../FrameLimitTest.kt` (7 cases incl. 0xFFFFFFFF header, ceiling-exact frame, buffer-bound) |
| M-03 | Medium | Android RPC deserializer rebuilds generic values; no channel-schema validation at the boundary | `android/.../rpc/*` | **OPEN** — deliberate read-side defensiveness (`asStringOrNull` family); per-channel schema codegen is a tracked follow-up, not silently shippable | — |
| M-04 | High (accepted tradeoff) | `targetSdk = 28` keeps W^X-exempt execution of embedded Bionic binaries | `android/app/build.gradle.kts` | **ACCEPTED, DOCUMENTED** — changing it breaks the embedded runtime architecture (AGENTS.md §3.1); requires the jniLibs-packaged runtime design first. Do not bump silently | — |
| M-05 | Medium | `.gitignore` allowed future `.env.*` secrets to be committed | `.gitignore` | **FIXED** `security:` — `.env`/`.env.*` ignored; only `*.example` templates negated. Pre-tracked non-secret env files (.env.development/.env.production: URLs + public client ID) remain tracked pending an example-file rename | `git check-ignore` |
| M-06 | Medium | Approval UI integration unproven end-to-end on device | `QodexAppViewModel.kt`, conversation UI | **OPEN** — permission events reach the projection as tool rows; a dedicated ApprovalCard (Task 2.2 of the UX plan) is still pending device verification | — |
| M-07 | Medium | Bash/hardlink residual: the filesystem tools resolve every path through `resolveWorkspacePath` (symlink-aware), but a `Bash`-created hardlink inside the workspace to a file outside it yields byte-level access through a fully contained path — realpath cannot see through hardlinks | `apps/qodex-cli/packages/core/src/tool/handlers/bash.ts` (interpreter surface) | **OPEN** — engine design task: interpreter sandbox/proxy or deny-by-default for link syscalls; not fixable at the path-resolution layer | — |
| M-08 | Medium | Workflow `path` readers bypassed workspace containment at resolveInput: `CreateWorkflow.path`, `AmendWorkflow.path`, `SaveWorkflow.script_path`, `EvalWorkflowSnippet.path`, `FillWorkflowHole.path` resolved with plain resolve/normalize, so an absolute path or `../` traversal could read any file on the device into the approval window / run / transcript | `apps/qodex-cli/packages/core/src/tool/handlers/workflow-path-source.ts` + the five resolvers | **FIXED** `security:` — `ToolInputResolutionContext.workspaceRoot` (populated from executor `getWorkspaceRoot`) and a shared `workflowScriptPathContainment` gate before every model-supplied read; rejection is the named structured failure `workflow_script_file_outside_workspace`; engine-trusted predecessor-script reads and bare contexts (no root) keep legacy behavior | `apps/qodex-cli/packages/core/tests/workflow-path-containment.test.ts` |
| E-01 | Medium | Agent execution budgets (iterations / wall clock / spend) not centrally capped | `agent-runtime.ts`, `turn-machine.ts` | **OPEN, INVESTIGATED** — engine has per-tool timeouts + permission timeouts + model timeouts; a global turn/iteration ceiling needs an engine-side decision and is tracked in §4 | — |

## 3. Control inventory (what protects what)

| Control | Implementation | Enforced at |
| --- | --- | --- |
| Filesystem containment | `resolveWorkspacePath` realpath containment (+ `authorizedRoots`) | Every tool handler before fs access |
| Unsandboxed-JS approval | `alwaysAsk` permission spec on `mcp__node_repl__js` | Permission service, all modes |
| Dangerous Bash approval | guarded-mode `userApprovalRule` + `prepareApproval` gate (pre-existing) | Bash executor |
| Credential secrecy in transit | stdio pipes only; `QODEX_TRANSPORT=stdio`; no TCP/WS listener | Manifest + `network_security_config.xml` |
| Credential confidentiality at rest | AES-256-GCM cipher keyed by `credential-secret` (48 random bytes, mode 600, app-private) + `allowBackup=false` | Provisioner + manifest |
| Secret-free diagnostics | `LogRedactor` at FileLogger + RuntimeBus sinks | Both log writers |
| Frame DoS resistance | `MAX_FRAME_BYTES` + typed rejection before allocation | `ChunkStream` |
| Backup exclusion | `allowBackup=false`, `dataExtractionRules`, `fullBackupContent`, `noBackupFilesDir` | Manifest + platform |

## 4. Open roadmap (do not claim these as done)

1. **Keystore-wrapped master secret (C-02 remainder):** wrap the runtime `credential-secret` with an Android Keystore AES key so a restored data directory alone cannot decrypt credentials; keep the existing file-based cipher as the engine contract (`QODEX_CREDENTIAL_SECRET_FILE`) and wrap/unwrap in the host. Requires safe migration of already-encrypted credential stores.
2. **ApprovalCard (M-06):** native Compose approval surface fed by `permission.requested` stream events, wired to `qodex-agent.resolvePermission`; deny-by-default on timeout/disconnect.
3. **Execution budgets (E-01):** global iteration/turn ceiling, wall-clock deadline and cumulative spend cap, enforced in the turn machine; expired turns end with an explicit user-visible limit event.
4. **Channel-schema validation (M-03):** generate/validate per-channel method + payload schemas at the Android RPC boundary.
5. **Modern targetSdk (M-04):** migrate runtime packaging so executables live in `jniLibs` (W^X-safe) before raising targetSdk beyond 28.
6. **Historical secret scan + `pnpm audit`:** run both in CI; neither has been executed yet (audit limitation).

## 5. Verification requirements (CI gates)

| Test | Expected |
| --- | --- |
| `../../` traversal | Rejected (`path-policy.test.ts`) |
| Absolute path outside workspace | Rejected unless under `authorizedRoots` |
| Symlink escaping workspace (file or parent dir) | Rejected |
| Not-yet-created write target inside workspace | Allowed, validated via deepest-existing ancestor |
| `js` tool call in yolo/guarded | Asks (`always-ask-mode-boundary.test.ts`) |
| API key in Logcat/boot.log | Redacted (`LogRedactorTest.kt`) |
| 4 GiB / over-ceiling RPC frame | Rejected before allocation (`FrameLimitTest.kt`) |
| Backup inspection | No credential material (allowBackup=false; no JVM test — device/ADB verification pending) |
