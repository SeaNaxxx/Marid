// Android engine entry: mounts the RPC ChannelServer directly over stdin/stdout.
// No TCP port, no HTTP server, no WebSocket upgrade — the phone host speaks the
// same 13-byte SocketProtocol framing it already ports in SocketFraming.kt.
// Transport selection is driven by the launcher (entry-stdio.js); stdout is the
// frame channel and every diagnostic goes to stderr (see stdio-log-guard).
import "./stdio-log-guard.js";
import { randomUUID } from "node:crypto";
import {
  Emitter,
  LoggingChannelServer,
  ChannelServer,
  SocketProtocol,
  VSBuffer,
  type ISocket,
} from "@qodex/rpc";
import { ServiceCollection, IQodexAgentService, createQodexAgentConnectionScope, IProviderProvisioningTargetService } from "@qodex/services";
import {
  SourceServerWorkspaceIdentityChannelServer,
  createServerHostServices,
  createTopicResourcePeers,
  getAppConfigDir,
  type SourceServerWorkspaceIdentityMapper,
} from "@qodex/services/node";
import {
  materializeBundledQodexBuiltinProviderConfig,
  readBundledQodexBuiltinProviderConfig,
} from "./bundledQodexBuiltinProviderConfig.js";
import { resolveHttpServerId } from "./http.js";

const log = (...args: unknown[]) =>
  console.error(`[qodex-server:stdio] [${process.pid}]`, ...args);

/**
 * Byte-stream ISocket over the process pipes. ChunkStream inside SocketProtocol
 * reassembles partial reads, so framing is transport-agnostic — identical to
 * wrapWebSocket in http.ts but for raw pipe bytes instead of WS messages.
 */
function wrapProcessStdio(): ISocket {
  const onData = new Emitter<VSBuffer>();
  const onClose = new Emitter<void>();
  const onEnd = new Emitter<void>();
  let finished = false;

  const finish = () => {
    if (finished) return;
    finished = true;
    onClose.fire();
    onEnd.fire();
  };

  process.stdin.on("data", (chunk: Buffer) => {
    onData.fire(VSBuffer.wrap(new Uint8Array(chunk)));
  });
  process.stdin.on("end", finish);
  process.stdin.on("close", finish);
  process.stdin.on("error", (error: Error) => {
    log("stdin error", error.message);
    finish();
  });
  // Writing to a pipe whose reader died raises EPIPE asynchronously; exit hard
  // so the host never sees a half-alive engine.
  process.stdout.on("error", (error: NodeJS.ErrnoException) => {
    log("stdout error", error.code ?? error.message);
    process.exit(error.code === "EPIPE" ? 0 : 1);
  });

  return {
    onData: onData.event,
    onClose: onClose.event,
    onEnd: onEnd.event,
    write(buffer: VSBuffer) {
      if (finished) return;
      process.stdout.write(Buffer.from(buffer.buffer));
    },
    end() {
      finish();
    },
    drain() {
      return Promise.resolve();
    },
    dispose() {
      finish();
    },
  };
}

function mountStdioChannelServer(
  services: ServiceCollection,
  workspaceIdentityMapper?: SourceServerWorkspaceIdentityMapper,
): void {
  const socket = wrapProcessStdio();
  const protocol = new SocketProtocol(socket);
  const rawServer = new ChannelServer(protocol, "server");
  const loggedServer = new LoggingChannelServer(rawServer, log);
  const server = workspaceIdentityMapper
    ? new SourceServerWorkspaceIdentityChannelServer(loggedServer, workspaceIdentityMapper)
    : loggedServer;

  const agentService = services.getOptional(IQodexAgentService);
  // Parity with the /ws route: terminal-client scope, no host relay.
  const connectionScope = agentService
    ? createQodexAgentConnectionScope(agentService, {
        connectionId: `server-stdio-${randomUUID()}`,
        clientMode: "web-remote-replayable",
        role: "terminal-client",
      })
    : undefined;
  const overrides = new Map<string, unknown>();
  if (connectionScope) {
    overrides.set(IQodexAgentService.channelName, connectionScope.service);
  }
  // Cross-environment credential provisioning stays desktop-host-only, same as HTTP.
  if (services.getOptional(IProviderProvisioningTargetService)) {
    overrides.set(IProviderProvisioningTargetService.channelName, {
      apply: async () => {
        throw new Error("Provider Provisioning 仅支持受信 Desktop Host");
      },
    });
  }
  services.exposeOnChannelServer(server, overrides);

  const disposeAll = () => {
    void connectionScope?.dispose();
    rawServer.dispose();
  };
  socket.onEnd(disposeAll);
  socket.onClose(disposeAll);
}

async function main(): Promise<void> {
  const qodexBuiltinProviderConfigFilePath = await materializeBundledQodexBuiltinProviderConfig({
    environmentConfigRoot: getAppConfigDir(),
    content: readBundledQodexBuiltinProviderConfig(),
  });
  const topicResourcePeers = createTopicResourcePeers();
  const serverId = resolveHttpServerId();
  const { services, workspaceIdentityMapper, migrationFailedPathCount } =
    await createServerHostServices({
      topicResourceRelayChannel: (request) => topicResourcePeers.getChannel(request),
      sourceServerId: serverId,
      qodexBuiltinProviderConfigFilePath,
    });

  mountStdioChannelServer(services, workspaceIdentityMapper);

  if (migrationFailedPathCount > 0) {
    log(`workspace migration skipped ${migrationFailedPathCount} unreadable path(s)`);
  }
  log("RPC channel ready on stdio — waiting for host frames");
  log(`qodex builtin provider config: ${qodexBuiltinProviderConfigFilePath}`);

  // Host death closes the read pipe; exit immediately so no orphan node survives.
  const exitWhenPipeCloses = () => {
    log("stdin closed — exiting");
    process.exit(0);
  };
  process.stdin.on("end", exitWhenPipeCloses);
  process.stdin.on("close", exitWhenPipeCloses);
  process.on("SIGTERM", exitWhenPipeCloses);
  process.on("SIGINT", exitWhenPipeCloses);
}

void main().catch((error: unknown) => {
  console.error("[qodex-server:stdio] startup failed", error);
  process.exitCode = 1;
});
