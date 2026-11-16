import { materializeMaridBuiltinProviderConfig } from "@marid/services/node";

declare const __MARID_BUILTIN_PROVIDER_CONFIG_JSON__: string | undefined;

export interface MaterializeBundledMaridBuiltinProviderConfigOptions {
  readonly environmentConfigRoot: string;
  readonly content: string;
}

/** 返回构建时嵌入远端 Server 的 Marid Built-in Provider Config。 */
export function readBundledMaridBuiltinProviderConfig(): string {
  if (typeof __MARID_BUILTIN_PROVIDER_CONFIG_JSON__ !== "string") {
    throw new Error("当前构建未嵌入 Marid Built-in Provider Config");
  }
  return __MARID_BUILTIN_PROVIDER_CONFIG_JSON__;
}

/**
 * 将 Marid Built-in Config 原子物化到所属环境的固定资源副本。
 * 升级前退出旧进程；不保留按内容 hash 增长的历史文件。
 */
export async function materializeBundledMaridBuiltinProviderConfig(
  options: MaterializeBundledMaridBuiltinProviderConfigOptions,
): Promise<string> {
  return materializeMaridBuiltinProviderConfig(options);
}
