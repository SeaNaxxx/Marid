export const QODEX_BUILTIN_PROVIDER_CONFIG_FILE_ENV = "QODEX_BUILTIN_PROVIDER_CONFIG_FILE";
export const QODEX_BUILTIN_PROVIDER_BUNDLED_CONFIG_FILE_ENV =
  "QODEX_BUILTIN_PROVIDER_BUNDLED_CONFIG_FILE";
export const QODEX_PERSONAL_PROVIDER_CONFIG_FILE_ENV = "QODEX_PERSONAL_PROVIDER_CONFIG_FILE";
export const PERSONAL_PROVIDER_CONFIG_FILE_NAME = "provider_config.json";

export interface NodeProviderRuntimePaths {
  readonly qodexBuiltinFilePath: string;
  readonly personalFilePath: string;
}

export function createNodeProviderRuntimePathEnv(
  paths: NodeProviderRuntimePaths,
): Record<string, string> {
  return {
    [QODEX_BUILTIN_PROVIDER_CONFIG_FILE_ENV]: paths.qodexBuiltinFilePath,
    [QODEX_PERSONAL_PROVIDER_CONFIG_FILE_ENV]: paths.personalFilePath,
  };
}

export function resolveNodeProviderRuntimePaths(
  env: Readonly<Record<string, string | undefined>>,
): NodeProviderRuntimePaths | null {
  const qodexBuiltinFilePath = env[QODEX_BUILTIN_PROVIDER_CONFIG_FILE_ENV]?.trim();
  const personalFilePath = env[QODEX_PERSONAL_PROVIDER_CONFIG_FILE_ENV]?.trim();
  if (!qodexBuiltinFilePath && !personalFilePath) return null;
  if (!qodexBuiltinFilePath || !personalFilePath) {
    throw new Error("Qodex Built-in 与 Personal Provider Config 路径必须同时提供");
  }
  return Object.freeze({ qodexBuiltinFilePath, personalFilePath });
}
