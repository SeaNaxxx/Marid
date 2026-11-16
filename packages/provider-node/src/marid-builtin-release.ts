import { z } from "zod";
import {
  parseMaridBuiltinModelConfigRules,
  parseMaridBuiltinProviderConfigRules,
  type ModelConfigRules,
  type ProviderConfigMap,
  type ProviderTemplateMap,
} from "@marid/provider";

export const MARID_BUILTIN_RELEASE_SCHEMA_VERSION = 1 as const;
const RETIRED_ZAPI_PROVIDER_ID = "builtin:zapi";

export interface MaridBuiltinConfigContent {
  readonly providers: ProviderConfigMap;
  readonly providerTemplates: ProviderTemplateMap;
  readonly modelConfigRules: ModelConfigRules;
}

export interface MaridBuiltinRelease {
  readonly schemaVersion: typeof MARID_BUILTIN_RELEASE_SCHEMA_VERSION;
  readonly revision: number;
  readonly config: MaridBuiltinConfigContent;
}

const releaseSchema = z
  .object({
    schemaVersion: z.literal(MARID_BUILTIN_RELEASE_SCHEMA_VERSION),
    revision: z.number().int().nonnegative().max(Number.MAX_SAFE_INTEGER),
    config: z
      .object({
        providerConfigRules: z.unknown(),
        modelConfigRules: z.unknown(),
      })
      .strict(),
  })
  .strict();

export function decodeMaridBuiltinRelease(input: unknown): MaridBuiltinRelease {
  const parsed = releaseSchema.parse(input);
  const { providers, providerTemplates } = parseMaridBuiltinProviderConfigRules(
    parsed.config.providerConfigRules,
  );
  // ZAPI 已退出产品，旧 Remote Release 或 LKG 不能在 Renderer 静态入口删除后
  // 又通过目标 Host Registry 将它重新发布。拒绝整份不兼容 Release，让 Source 回落到兼容候选。
  if (providers.has(RETIRED_ZAPI_PROVIDER_ID)) {
    throw new Error(`Marid Built-in Release 包含已退出的 Provider: ${RETIRED_ZAPI_PROVIDER_ID}`);
  }
  return Object.freeze({
    schemaVersion: MARID_BUILTIN_RELEASE_SCHEMA_VERSION,
    revision: parsed.revision,
    config: Object.freeze({
      providers,
      providerTemplates,
      modelConfigRules: parseMaridBuiltinModelConfigRules(parsed.config.modelConfigRules),
    }),
  });
}

export function encodeMaridBuiltinRelease(release: MaridBuiltinRelease): object {
  return {
    schemaVersion: MARID_BUILTIN_RELEASE_SCHEMA_VERSION,
    revision: release.revision,
    config: {
      providerConfigRules: {
        templateRules: release.config.providerTemplates.toJSON(),
        providerRules: release.config.providers.toJSON(),
      },
      modelConfigRules: release.config.modelConfigRules.toMaridBuiltinJSON(),
    },
  };
}

export function serializeMaridBuiltinRelease(release: MaridBuiltinRelease): string {
  return JSON.stringify(encodeMaridBuiltinRelease(release));
}
