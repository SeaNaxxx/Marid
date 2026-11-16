import {
  createServerHostServices,
  createTopicResourcePeers,
  getAppConfigDir,
} from "@qodex/services/node";
import {
  materializeBundledQodexBuiltinProviderConfig,
  readBundledQodexBuiltinProviderConfig,
} from "./bundledQodexBuiltinProviderConfig.js";
import { createHttpServer, resolveHttpServerId } from "./http.js";

async function main(): Promise<void> {
  const qodexBuiltinProviderConfigFilePath = await materializeBundledQodexBuiltinProviderConfig({
    environmentConfigRoot: getAppConfigDir(),
    content: readBundledQodexBuiltinProviderConfig(),
  });
  const port = Number(process.env["PORT"]) || 3030;
  const host = process.env["QODEX_SERVER_HOST"]?.trim() || process.env["HOST"]?.trim() || undefined;
  const staticRoot = process.env["QODEX_WEB_STATIC_ROOT"]?.trim() || undefined;
  const authToken = process.env["QODEX_SERVER_AUTH_TOKEN"]?.trim() || undefined;
  const topicResourcePeers = createTopicResourcePeers();
  const serverId = resolveHttpServerId();
  const { services, workspaceIdentityMapper, migrationFailedPathCount } =
    await createServerHostServices({
      topicResourceRelayChannel: (request) => topicResourcePeers.getChannel(request),
      sourceServerId: serverId,
      qodexBuiltinProviderConfigFilePath,
      providerProvisioningTargetEnabled: Boolean(authToken),
    });

  createHttpServer(services, port, {
    serverId,
    workspaceIdentityMapper,
    migrationFailedPathCount,
    virtualProjects: true,
    topicResourcePeers,
    ...(host ? { host } : {}),
    ...(staticRoot ? { staticRoot, spaFallback: true } : {}),
    ...(authToken ? { authToken, authRequired: true } : {}),
  });
}

void main().catch((error: unknown) => {
  console.error("[qodex-server:http] startup failed", error);
  process.exitCode = 1;
});
