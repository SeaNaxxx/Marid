import { loadBuiltinProviderConfig } from "../../../../../scripts/builtin-provider-config.mjs";

export const SEA_QODEX_BUILTIN_PROVIDER_CONFIG_ASSET_KEY = "qodex-provider/qodex-builtin.json";

export const collectSeaProviderConfigAssets = async ({ root, env = process.env }) => {
  const { sourcePath } = await loadBuiltinProviderConfig({ root, env });
  return {
    [SEA_QODEX_BUILTIN_PROVIDER_CONFIG_ASSET_KEY]: sourcePath,
  };
};
