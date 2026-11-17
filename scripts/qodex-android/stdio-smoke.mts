// Phase 1 stdio transport smoke test: drives the real entry-stdio.js with the
// real @qodex/rpc protocol stack over raw process pipes — byte-for-byte the
// same shape the Android host speaks via SocketFraming.kt / ChannelClient.kt.
// Pass = Initialize handshake + provider-settings.getView round trip.
import { spawn } from "node:child_process";
import { mkdtempSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { Emitter, VSBuffer, SocketProtocol, ChannelClient } from "../../packages/rpc/src/index.js";
import type { ISocket } from "../../packages/rpc/src/index.js";

// Host machines have no OS secret service; the engine requires a file-based
// cipher secret. Android sets QODEX_CREDENTIAL_SECRET_FILE via BionicEnv.
const secretDir = mkdtempSync(join(tmpdir(), "qodex-smoke-"));
const secretFile = join(secretDir, "credential-secret");
writeFileSync(secretFile, Buffer.alloc(48).fill("q"));

const child = spawn("node", ["packages/server/dist/entry-stdio.js"], {
  stdio: ["pipe", "pipe", "pipe"],
  env: {
    ...process.env,
    QODEX_TRANSPORT: "stdio",
    QODEX_CREDENTIAL_SECRET_FILE: secretFile,
  },
});
child.stderr.on("data", (d: Buffer) => process.stderr.write(`[engine|err] ${d}`));

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

const protocol = new SocketProtocol(socket);
const client = new ChannelClient(protocol);

const timeout = setTimeout(() => {
  console.error("[smoke] FAIL: timed out waiting for RPC round trip");
  child.kill("SIGKILL");
  process.exit(1);
}, 60_000);

try {
  const view = (await client.getChannel("provider-settings").call("getView")) as {
    providers?: unknown[];
  };
  const providerCount = Array.isArray(view?.providers) ? view.providers.length : -1;
  console.log(`[smoke] provider-settings.getView OK — providers: ${providerCount}`);
  if (providerCount < 0) throw new Error("getView returned no providers array");
  clearTimeout(timeout);
  console.log("[smoke] PASS: Initialize handshake + full RPC round trip over stdio");
  child.kill("SIGTERM");
  process.exit(0);
} catch (error) {
  clearTimeout(timeout);
  console.error("[smoke] FAIL:", error);
  child.kill("SIGKILL");
  process.exit(1);
}
