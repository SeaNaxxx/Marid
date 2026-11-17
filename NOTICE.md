# Qodex Notice: Functional Scope, Android Security Boundaries, and Third-Party Declarations

This notice applies to the source code and build artifacts published in this
repository. Qodex is a native Android application that runs an embedded
Node.js agent engine inside its own application sandbox. The boundaries below
describe what the shipped Android app can and cannot do; they are not claims of
military-grade isolation, and users should treat agent output with the same
skepticism as any other AI-generated content.

## 1. AI output and execution risk

AI-generated code, commands, explanations, files, and suggestions can be wrong,
incomplete, insecure, or in conflict with third-party rights. Verify against
real files, command exit codes, tool results, and tests. A model claiming a
task is "done", "safe", or "authorized" is not evidence of any of those things.
Review execution targets and blast radius before important operations, and keep
recoverable backups of workspaces you care about.

Agent tools (file edits, shell commands, Git, background tasks) execute inside
the Android boundaries described in Section 2 — nothing more. Model tool
approvals are not a global permission switch for the whole app; shell
operations, plugin processes, and downloaded code each follow their own paths.

## 2. Android security boundaries

### 2.1 UID isolation

The Qodex app — and therefore the embedded Node.js engine it spawns — runs as a
single unprivileged Linux UID assigned by Android (`u0_a…`), confined by
SELinux. Concretely:

- The engine can read and write only the app's private storage
  (`/data/user/0/com.qodex.app/`), storage the user explicitly grants, and
  world-readable system data. It has **no root** and cannot elevate itself.
- Other apps' private data is invisible to the engine. Installed apps' public
  APK files under `/data/app/` are world-readable on stock Android and may be
  inspected, but their private data is not accessible.
- The engine is not a "full Linux machine": it is a userland process with the
  same kernel-enforced limits as any Android app. Claims that the agent can
  modify the system, other apps, or protected storage are false.

### 2.2 Storage access

- App-private directories (`filesDir`, `cacheDir`) are used for the runtime
  provisioning area, engine bundle, session data, and logs. No permission is
  needed for these, and uninstalling the app removes them.
- User-visible shared storage is reached through the **Storage Access
  Framework (SAF)**: the user picks a workspace directory through the system
  picker, and the engine operates only inside that grant. Qodex does not
  request broad storage permissions (`READ/WRITE_EXTERNAL_STORAGE`,
  `MANAGE_EXTERNAL_STORAGE`) as a default success path.
- Workspace files the agent creates or edits are real files in the granted
  location; deleting the app does not necessarily remove user-granted
  workspace content, and clearing app data does not restore files the agent
  already changed.

### 2.3 Shell command limits

The engine executes shell commands through `/system/bin/sh` (toybox/toolbox)
within the app UID:

- Commands run unprivileged. Anything requiring root, system write access, or
  other apps' data will fail with a real OS error — Qodex does not ship a
  privilege-escalation path.
- Executables and scripts run inside the boundaries Android enforces for the
  app (including W^X rules on newer targets for app-writable storage, which is
  why native binaries are packaged as described in AGENTS.md).
- Agent commands are subject to the app's tool-approval flow, but tool
  approval is a UX confirmation, not a security sandbox. Treat approved
  commands as running with the app's full (unprivileged) authority.

### 2.4 Shizuku (optional, only if the user authorizes it)

Some elevated operations (e.g. installing an APK via `PackageInstaller`
flows that need shell-level access, or advanced system queries) can optionally
route through **Shizuku**:

- Shizuku is never bundled, auto-enabled, or silently used. It requires the
  user to install Shizuku themselves and explicitly grant Qodex access.
- When authorized, commands inherit the ADB-shell privilege level the user
  granted — this is a real elevation of capability chosen by the user, and it
  is recorded in app logs. Without that grant, no elevated path exists.
- Qodex does not use Shizuku to bypass tool approval prompts; approval and
  privilege are independent decisions.

### 2.5 Secrets (API keys)

- Provider API keys are encrypted at rest with **Android Keystore**
  (AES-GCM, hardware-backed where the device supports it) and persisted via
  Jetpack DataStore. Plaintext keys on disk, in logs, in crash reports, or in
  terminal output are defects — report them.
- Keys are handed to the engine subprocess only through in-memory, narrowly
  scoped IPC channels for the duration needed; environment variables and
  command-line arguments are not treated as secure channels.
- Passing a key to the engine inevitably lets the engine send it to the
  configured LLM provider. Qodex cannot make a BYOK request to a provider
  without transmitting the credential to that provider.

### 2.6 Foreground service and background reality

Long agent tasks run behind a foreground service with a partial WakeLock so
the OS is less likely to kill the engine mid-task. This is not a guarantee:

- Doze, OEM battery managers, and the user's own actions can still terminate
  processes. Qodex persists session state so tasks can resume, but an
  interrupted task is not automatically "rolled back" — partial file edits and
  completed external actions remain as they were.
- Battery-optimization exemption ("Unrestricted") is requested only as an
  opt-in prompt, never assumed.

## 3. Network behavior

- The Android host and the engine communicate over the local loopback inside
  the device; this traffic does not leave the phone.
- Model requests go from the engine directly to the LLM provider endpoints the
  user configured (e.g. OpenRouter, Anthropic, Z.ai). Prompts, code context,
  diffs, and tool results required for a task are sent to whichever provider
  serves the selected model. Users working on confidential code must treat
  provider transmission as a given, not an accident.
- The app makes no telemetry or analytics calls to Qodex developers. Any
  additional endpoints come from user-configured providers or MCP/plugin
  configuration, which follow their own documented behaviors.

## 4. Local data, logs, and credentials

- Session databases, runtime logs (`boot.log` under app files), toolchain
  caches, and provider settings live in the app's private storage and are
  removed on uninstall or "clear data".
- Logs can contain prompts, code context, tool parameters, and model replies.
  Do not paste raw logs, screenshots, or session exports into public issues
  without reviewing them for credentials and private content first.
- Copying data out of the app (share sheet, file export) is a user action;
  Qodex does not silently upload workspace content anywhere.

## 5. Third-party licenses and attribution

First-party code in this repository is licensed under the root
[LICENSE](LICENSE) (Apache-2.0). That license grants nothing for third-party
software, copied code, native binaries, fonts, icons, or web assets, which
carry their own terms; see [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md).

Qodex is based on zai-org/zcode v3.18.2 (Apache-2.0). The upstream CLI engine,
protocol definitions, and shared TypeScript packages are reused under their
license; applicable notices and copyright attribution are preserved.
