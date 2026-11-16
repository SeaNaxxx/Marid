import {
  createServerHostServices,
  createTopicResourcePeers,
  getAppConfigDir,
} from "@marid/services/node";
import {
  materializeBundledMaridBuiltinProviderConfig,
  readBundledMaridBuiltinProviderConfig,
} from "./bundledMaridBuiltinProviderConfig.js";
import { createHttpServer, resolveHttpServerId } from "./http.js";

async function main(): Promise<void> {
  const maridBuiltinProviderConfigFilePath = await materializeBundledMaridBuiltinProviderConfig({
    environmentConfigRoot: getAppConfigDir(),
    content: readBundledMaridBuiltinProviderConfig(),
  });
  const port = Number(process.env["PORT"]) || 3030;
  const host = process.env["MARID_SERVER_HOST"]?.trim() || process.env["HOST"]?.trim() || undefined;
  const staticRoot = process.env["MARID_WEB_STATIC_ROOT"]?.trim() || undefined;
  const authToken = process.env["MARID_SERVER_AUTH_TOKEN"]?.trim() || undefined;
  const topicResourcePeers = createTopicResourcePeers();
  const serverId = resolveHttpServerId();
  const { services, workspaceIdentityMapper, migrationFailedPathCount } =
    await createServerHostServices({
      topicResourceRelayChannel: (request) => topicResourcePeers.getChannel(request),
      sourceServerId: serverId,
      maridBuiltinProviderConfigFilePath,
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
  console.error("[marid-server:http] startup failed", error);
  process.exitCode = 1;
});
