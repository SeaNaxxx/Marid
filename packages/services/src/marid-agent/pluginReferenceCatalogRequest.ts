import {
  maridProtocolMethods,
  maridPluginsReferenceCatalogResultSchema,
  type MaridPluginsReferenceCatalogParams,
} from "@marid/shared";
import type { MaridProtocolClient } from "#src/marid-agent/maridProtocolClient.js";

/** 旧协议严格校验响应；新展示字段走独立入口，只有 -32601 能证明旧 Agent 不支持。 */
export async function requestPluginReferenceCatalog(
  client: Pick<MaridProtocolClient, "request">,
  params: MaridPluginsReferenceCatalogParams,
) {
  try {
    return await client.request(
      maridProtocolMethods.pluginsReferenceCatalogWithCategory,
      params,
      maridPluginsReferenceCatalogResultSchema,
    );
  } catch (error) {
    if (!(typeof error === "object" && error !== null && "code" in error && error.code === -32601))
      throw error;
    return client.request(
      maridProtocolMethods.pluginsReferenceCatalog,
      params,
      maridPluginsReferenceCatalogResultSchema,
    );
  }
}
