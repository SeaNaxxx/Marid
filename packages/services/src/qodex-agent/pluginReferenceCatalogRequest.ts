import {
  qodexProtocolMethods,
  qodexPluginsReferenceCatalogResultSchema,
  type QodexPluginsReferenceCatalogParams,
} from "@qodex/shared";
import type { QodexProtocolClient } from "#src/qodex-agent/qodexProtocolClient.js";

/** 旧协议严格校验响应；新展示字段走独立入口，只有 -32601 能证明旧 Agent 不支持。 */
export async function requestPluginReferenceCatalog(
  client: Pick<QodexProtocolClient, "request">,
  params: QodexPluginsReferenceCatalogParams,
) {
  try {
    return await client.request(
      qodexProtocolMethods.pluginsReferenceCatalogWithCategory,
      params,
      qodexPluginsReferenceCatalogResultSchema,
    );
  } catch (error) {
    if (!(typeof error === "object" && error !== null && "code" in error && error.code === -32601))
      throw error;
    return client.request(
      qodexProtocolMethods.pluginsReferenceCatalog,
      params,
      qodexPluginsReferenceCatalogResultSchema,
    );
  }
}
