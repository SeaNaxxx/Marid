import { materializeQodexBuiltinProviderConfig } from "@qodex/services/node";

declare const __QODEX_BUILTIN_PROVIDER_CONFIG_JSON__: string | undefined;

export interface MaterializeBundledQodexBuiltinProviderConfigOptions {
  readonly environmentConfigRoot: string;
  readonly content: string;
}

/** 返回构建时嵌入远端 Server 的 Qodex Built-in Provider Config。 */
export function readBundledQodexBuiltinProviderConfig(): string {
  if (typeof __QODEX_BUILTIN_PROVIDER_CONFIG_JSON__ !== "string") {
    throw new Error("当前构建未嵌入 Qodex Built-in Provider Config");
  }
  return __QODEX_BUILTIN_PROVIDER_CONFIG_JSON__;
}

/**
 * 将 Qodex Built-in Config 原子物化到所属环境的固定资源副本。
 * 升级前退出旧进程；不保留按内容 hash 增长的历史文件。
 */
export async function materializeBundledQodexBuiltinProviderConfig(
  options: MaterializeBundledQodexBuiltinProviderConfigOptions,
): Promise<string> {
  return materializeQodexBuiltinProviderConfig(options);
}
