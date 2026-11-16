# Qodex Lite Static Distribution Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a static-source Qodex Lite artifact that users can install from an intranet file server and run as a local Web app.

**Architecture:** The build script packages the existing Vite Web dist, HTTP server dist, and `qodex.cjs` agent bundle into a tarball plus `install.sh` and `latest.json`. Runtime uses the existing plain Web same-origin `/ws` path, with optional token protection when binding to non-local hosts.

**Tech Stack:** Node.js 24, Vite Web dist, Hono HTTP server, Qodex service RPC over WebSocket, POSIX shell install script, tar/gzip artifacts.

## Global Constraints

- The intranet source is a plain static file service, not an npm registry.
- The build script must not upload or push artifacts.
- Qodex Lite must use the user's installed Node.js and must not bundle Node runtime.
- Default serve host is `127.0.0.1`; default port is an automatically selected free port.
- `--host 0.0.0.0` must default to token-protected access and no automatic browser open.
- Lite Web must use plain Web same-origin `/ws`; it must not change `/remote` relay or replayable semantics.
- `pnpm typecheck` and `pnpm lint` are required before completion.

---

### Task 1: HTTP Server Static And Token Support

**Files:**
- Modify: `packages/server/src/http.ts`
- Modify: `packages/server/src/entry-http.ts`
- Test: `packages/server/test/serverInfoHttp.test.ts`

**Interfaces:**
- Produces: `HttpServerOptions.staticRoot?: string`, `HttpServerOptions.spaFallback?: boolean`, `HttpServerOptions.host?: string`, `HttpServerOptions.authToken?: string`.
- Consumes: Existing `createHttpServer(services, port, options)` call sites remain compatible.

- [ ] Add static file serving with SPA fallback after API and WebSocket routes.
- [ ] Add token guard for `/api/*`, `/ws`, and `/ws/remote/*`.
- [ ] Read `HOST`, `QODEX_SERVER_HOST`, `QODEX_WEB_STATIC_ROOT`, and `QODEX_SERVER_AUTH_TOKEN` in `entry-http.ts`.
- [ ] Add tests for `/api/server-info` token rejection/acceptance and static `index.html` fallback.

### Task 2: Plain Web Bootstrap Workspace

**Files:**
- Modify: `packages/web/src/main.tsx`

**Interfaces:**
- Produces: plain Web bootstrap can populate `initialWorkspaceAbsPath` from `/api/server-info`.
- Consumes: Existing `ServerRemoteInfo.workspaces` response.

- [ ] Fetch `/api/server-info` in plain Web bootstrap.
- [ ] Use the first workspace path as `initialWorkspaceAbsPath`.
- [ ] Keep bootstrap tolerant when `/api/server-info` is unavailable.

### Task 3: Qodex Lite Build Script

**Files:**
- Create: `scripts/build-qodex-lite.mjs`
- Modify: `package.json`

**Interfaces:**
- Produces: `pnpm build:lite`.
- Produces: `dist/qodex-lite/install.sh`, `dist/qodex-lite/latest.json`, and `dist/qodex-lite/releases/<version>/qodex-lite-<version>.tar.gz`.

- [ ] Build or reuse existing web/server/agent outputs.
- [ ] Stage `web`, `server`, `agent/qodex.cjs`, runtime `node_modules`, and `bin/qodex-lite.mjs`.
- [ ] Patch `node-pty/prebuilds` with available `@lydell/node-pty-*` macOS/Linux prebuilds.
- [ ] Generate `install.sh`, `latest.json`, tarball, and `sha256.txt`.

### Task 4: Local Verification And Upload

**Files:**
- No source files for upload; upload remains a one-off command.

**Interfaces:**
- Consumes: local `dist/qodex-lite` output.
- Produces: remote static directory at `/Users/dev/shared/qodex/deps/qodex-lite`.

- [ ] Run targeted tests for server HTTP behavior.
- [ ] Run `pnpm build:lite`.
- [ ] Smoke run the unpacked lite server locally.
- [ ] Run `pnpm typecheck`.
- [ ] Run `pnpm lint`.
- [ ] Upload generated `dist/qodex-lite` contents to the static source over SSH/SCP without committing upload logic.
- [ ] Commit source changes with Conventional Commit format.
