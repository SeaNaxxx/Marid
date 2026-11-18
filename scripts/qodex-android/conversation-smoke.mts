// Phase 2 stdio smoke test: drives the full V4 conversation flow against the
// real entry-stdio.js — createSession -> subscribeConversationV4 ->
// onDynamicConversationFrame -> sendConversationCommandV4 — byte-for-byte the
// sequence the Android host must speak. Pass = at least one conversation frame
// observed after a send (a failed turn with an explicit error frame still
// proves the pipe; total silence is the defect this test exists to catch).
import { spawn } from "node:child_process";
import { mkdtempSync, writeFileSync, mkdirSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { Emitter, VSBuffer, SocketProtocol, ChannelClient } from "../../packages/rpc/src/index.js";
import type { ISocket } from "../../packages/rpc/src/index.js";

const FRAMES_EXPECTED_WITHIN_MS = 90_000;

const secretDir = mkdtempSync(join(tmpdir(), "qodex-conv-"));
const secretFile = join(secretDir, "credential-secret");
writeFileSync(secretFile, Buffer.alloc(48).fill("q"));
// Engine session create/subscribe requires an existing workspace directory.
const workspacePath = join(secretDir, "workspace");
mkdirSync(workspacePath, { recursive: true });

const child = spawn("node", ["packages/server/dist/entry-stdio.js"], {
  stdio: ["pipe", "pipe", "pipe"],
  env: {
    ...process.env,
    QODEX_TRANSPORT: "stdio",
    QODEX_CREDENTIAL_SECRET_FILE: secretFile,
  },
});
let stderrTail = "";
child.stderr.on("data", (d: Buffer) => {
  stderrTail = (stderrTail + d.toString()).slice(-8000);
  process.stderr.write(`[engine|err] ${d}`);
});

const onData = new Emitter<VSBuffer>();
const onClose = new Emitter<void>();
const onEnd = new Emitter<void>();
child.stdout.on("data", (chunk: Buffer) => onData.fire(VSBuffer.wrap(new Uint8Array(chunk))));
child.on("exit", (code) => {
  process.stderr.write(`[smoke] engine exited (code=${code})\n`);
  onClose.fire();
  onEnd.fire();
});

const socket: ISocket = {
  onData: onData.event,
  onClose: onClose.event,
  onEnd: onEnd.event,
  write(buffer: VSBuffer) {
    child.stdin.write(Buffer.from(buffer.buffer));
  },
  end() {
    child.stdin.end();
  },
  drain() {
    return Promise.resolve();
  },
  dispose() {
    child.stdin.end();
  },
};

const client = new ChannelClient(new SocketProtocol(socket));
// Desktop ProxyChannel.toService packs method args as a single-element array
// (channel.call(propKey, [params])) and ProxyChannel.fromService applies them
// (target.apply(handler, args)) — a bare object arg would be dropped entirely
// and every params-taking method would receive undefined. The Android host
// must therefore wrap args in a one-element array exactly like this.
const agent = client.getChannel<{
  call: (command: string, arg?: unknown) => Promise<any>;
  listen: (event: string, arg?: unknown) => { (cb: (e: unknown) => void): void };
}>("qodex-agent");
const callService = (channel: string, command: string, params: Record<string, unknown>) =>
  client.getChannel<{ call: (c: string, a?: unknown) => Promise<any> }>(channel).call(command, [
    params,
  ]);

const fail = (reason: string): never => {
  console.error(`[smoke] FAIL: ${reason}`);
  console.error(`[smoke] engine stderr tail:\n${stderrTail}`);
  child.kill("SIGKILL");
  process.exit(1);
};

const hardTimeout = setTimeout(
  () => fail(`timed out before any conversation frame (${FRAMES_EXPECTED_WITHIN_MS}ms)`),
  FRAMES_EXPECTED_WITHIN_MS,
);

let framesSeen = 0;
let deltasSeen = 0;
let rowsSeen = 0;
let statesSeen = 0;

try {
  // Standalone-server entry routes session creation through the project system
  // (project.remote-project-required otherwise): register the workspace root as
  // a project, prepare its directories, then create the session inside it.
  const projectChannel = client.getChannel<{
    call: (command: string, arg?: unknown) => Promise<any>;
  }>("project");
  const project = await projectChannel.call("saveProject", [
    { name: "Qodex Workspace", roots: [workspacePath], defaultName: "Qodex Workspace" },
  ]);
  const projectId: string | undefined = project?.id;
  if (!projectId) {
    fail(`saveProject returned no id: ${JSON.stringify(project).slice(0, 600)}`);
  }
  console.log(`[smoke] saveProject OK — projectId: ${projectId} roots=${JSON.stringify(project?.roots)}`);

  const directories = await projectChannel.call("prepareSession", [{ projectId }]);
  const preparedWorkspacePath: string | undefined = directories?.workspacePath;
  console.log(
    `[smoke] prepareSession OK — workspacePath=${preparedWorkspacePath} identity=${directories?.workspaceIdentity ?? "(none)"}`,
  );

  // Provider readiness gate: session creation is refused until the
  // model-selection view carries at least one provider with one model. Ensure a
  // personal provider exists from a bundled template with its endpoint pointed
  // at a connection-refused port — the conversation transport is exercised end
  // to end while the turn itself fails with a real network error (never a fake
  // success frame).
  const providerChannel = client.getChannel<{
    call: (command: string, arg?: unknown) => Promise<any>;
  }>("provider-settings");
  const selectionBefore = await callService("model-selection", "getView", {});
  const hasReadyProvider = (selectionBefore?.providers ?? []).some(
    (p: any) => Array.isArray(p?.models) && p.models.length > 0,
  );
  if (!hasReadyProvider) {
    const created = await providerChannel.call("createPersonalProvider", [
      {
        templateId: "deepseek",
        providerName: "Smoke Unreachable Endpoint",
        locale: "en-US",
        initialConfig: {
          access: { type: "api-key", apiKey: "not-a-credential-unreachable-endpoint" },
          api: { type: "anthropic-messages", baseUrl: "http://127.0.0.1:9" },
        },
      },
    ]);
    const newProviderId = created?.providerId;
    if (!newProviderId) fail(`createPersonalProvider returned no providerId: ${JSON.stringify(created)}`);
    console.log(`[smoke] createPersonalProvider OK — providerId: ${newProviderId}`);
    const selectionAfter = await callService("model-selection", "getView", {});
    const readyAfter = (selectionAfter?.providers ?? []).some(
      (p: any) => Array.isArray(p?.models) && p.models.length > 0,
    );
    if (!readyAfter) {
      fail(
        `model-selection still not ready after provider creation: ${JSON.stringify(selectionAfter).slice(0, 800)}`,
      );
    }
    console.log("[smoke] model-selection ready (provider with models present)");
  } else {
    console.log("[smoke] model-selection already ready");
  }

  const snapshot = await callService("qodex-agent", "createSession", {
    workspacePath: preparedWorkspacePath ?? workspacePath,
    projectId,
    workspaceIdentity: directories?.workspaceIdentity,
    // session_input carries a session foreign key; without immediate persistence
    // the master row only exists after first-prompt admission, which the very
    // first sendText then violates (FOREIGN KEY constraint failed).
    persistence: "deferred",
  });
  const sessionId: string | undefined =
    snapshot?.session?.sessionId ?? snapshot?.sessionId ?? null;
  if (!sessionId) {
    fail(`createSession returned no sessionId: ${JSON.stringify(snapshot).slice(0, 600)}`);
  }
  console.log(`[smoke] createSession OK — sessionId: ${sessionId}`);

  // V4 connection handshake: hello -> clientHello, once per connection, before
  // any conversation/sessions-index subscription (fault.connection.handshakeRequired).
  const hello = await agent.call("helloConversationV4");
  console.log(
    `[smoke] hello OK — clientMode=${hello?.clientMode} protocol=${hello?.protocolVersion} wfDeltas=${hello?.capabilities?.workflowRunDeltas}`,
  );
  await agent.call("initializeConversationV4", [
    {
      kind: "clientHello",
      protocolVersion: hello?.protocolVersion ?? 3,
      clientId: "qodex-conversation-smoke",
      clientKind: "mobileApp",
      appVersion: "smoke",
      capabilities: { workspaceHookReviewUi: true },
    },
  ]);
  console.log("[smoke] clientHello OK — handshake complete");

  const sub = await callService("qodex-agent", "subscribeConversationV4", { workspacePath, sessionId });
  const subscriptionId: string | undefined = sub?.ack?.subscriptionId ?? sub?.subscriptionId;
  if (!subscriptionId) {
    fail(`subscribeConversationV4 returned no subscriptionId: ${JSON.stringify(sub).slice(0, 600)}`);
  }
  console.log(`[smoke] subscribeConversationV4 OK — subscriptionId: ${subscriptionId}`);

  const frameSubscription = agent.listen("onDynamicConversationFrame", { workspacePath })(
    (wire: any) => {
      framesSeen += 1;
      // Logical frame lives under wire.frame; wire.kind "complete" carries it
      // atomically, "fragment" would need base64 reassembly (crc32).
      const logical = wire?.kind === "complete" ? wire?.frame : undefined;
      if (!logical) {
        console.log(
          `[smoke] frame#=${framesSeen} wire kind=${wire?.kind} fragmentIndex=${wire?.fragmentIndex}/${wire?.fragmentCount}`,
        );
        return;
      }
      const payload = logical?.payload ?? {};
      if (payload.kind === "snapshot") {
        const rows = payload.snapshot?.rows;
        const windowRows = Array.isArray(rows?.window) ? rows.window : [];
        rowsSeen += windowRows.length;
        console.log(
          `[smoke] frame#=${framesSeen} kind=snapshot rows=${windowRows.length} topic=${logical?.topic} seq=${logical?.fromSeq}->${logical?.toSeq}`,
        );
        for (const row of windowRows) {
          console.log(
            `[smoke]   row rowId=${row?.rowId} kind=${row?.kind} state=${row?.state ?? "-"} text=${JSON.stringify(row?.text ?? "").slice(0, 120)}`,
          );
        }
      } else if (payload.kind === "deltas") {
        const deltas = Array.isArray(payload.deltas) ? payload.deltas : [];
        deltasSeen += deltas.length;
        console.log(
          `[smoke] frame#=${framesSeen} kind=deltas n=${deltas.length} topic=${logical?.topic} seq=${logical?.fromSeq}->${logical?.toSeq}`,
        );
        for (const delta of deltas) {
          if (delta?.op === "state.updated") statesSeen += 1;
          console.log(
            `[smoke]   delta op=${delta?.op} ${delta?.op === "row.delta" ? `path=${JSON.stringify(delta?.path)} append=${JSON.stringify(delta?.append).slice(0, 80)}` : delta?.op?.startsWith("row.") ? `kind=${delta?.row?.kind} state=${delta?.row?.state ?? "-"} text=${JSON.stringify(delta?.row?.text ?? "").slice(0, 100)}` : ""}`,
          );
        }
      } else {
        console.log(
          `[smoke] frame#=${framesSeen} payload kind=${payload?.kind} raw=${JSON.stringify(logical).slice(0, 240)}`,
        );
      }
    },
  );

  const ack = await callService("qodex-agent", "sendConversationCommandV4", {
    workspacePath,
    envelope: {
      commandId: crypto.randomUUID(),
      clientId: "qodex-conversation-smoke",
      sessionId,
      type: "sendText",
      payload: { text: "hi" },
      issuedAt: Date.now(),
    },
  });
  console.log(`[smoke] sendConversationCommandV4 ack: ${JSON.stringify(ack).slice(0, 600)}`);

  // Give the engine a window to stream frames; an explicit turn failure is a
  // PASS for transport purposes, silence is a FAIL.
  await new Promise((resolve) => setTimeout(resolve, 30_000));

  clearTimeout(hardTimeout);
  frameSubscription?.dispose();
  if (framesSeen === 0) {
    fail(`no conversation frames arrived within the window`);
  }
  console.log(
    `[smoke] PASS: conversation frames flow over stdio — frames=${framesSeen} deltas=${deltasSeen} (state.updated=${statesSeen}) rows=${rowsSeen}`,
  );
  child.kill("SIGTERM");
  process.exit(0);
} catch (error) {
  clearTimeout(hardTimeout);
  fail(`RPC step threw: ${error instanceof Error ? error.message : String(error)}`);
}
