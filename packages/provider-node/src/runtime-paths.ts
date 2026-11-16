export const MARID_BUILTIN_PROVIDER_CONFIG_FILE_ENV = "MARID_BUILTIN_PROVIDER_CONFIG_FILE";
export const MARID_BUILTIN_PROVIDER_BUNDLED_CONFIG_FILE_ENV =
  "MARID_BUILTIN_PROVIDER_BUNDLED_CONFIG_FILE";
export const MARID_PERSONAL_PROVIDER_CONFIG_FILE_ENV = "MARID_PERSONAL_PROVIDER_CONFIG_FILE";
export const PERSONAL_PROVIDER_CONFIG_FILE_NAME = "provider_config.json";

export interface NodeProviderRuntimePaths {
  readonly maridBuiltinFilePath: string;
  readonly personalFilePath: string;
}

export function createNodeProviderRuntimePathEnv(
  paths: NodeProviderRuntimePaths,
): Record<string, string> {
  return {
    [MARID_BUILTIN_PROVIDER_CONFIG_FILE_ENV]: paths.maridBuiltinFilePath,
    [MARID_PERSONAL_PROVIDER_CONFIG_FILE_ENV]: paths.personalFilePath,
  };
}

export function resolveNodeProviderRuntimePaths(
  env: Readonly<Record<string, string | undefined>>,
): NodeProviderRuntimePaths | null {
  const maridBuiltinFilePath = env[MARID_BUILTIN_PROVIDER_CONFIG_FILE_ENV]?.trim();
  const personalFilePath = env[MARID_PERSONAL_PROVIDER_CONFIG_FILE_ENV]?.trim();
  if (!maridBuiltinFilePath && !personalFilePath) return null;
  if (!maridBuiltinFilePath || !personalFilePath) {
    throw new Error("Marid Built-in 与 Personal Provider Config 路径必须同时提供");
  }
  return Object.freeze({ maridBuiltinFilePath, personalFilePath });
}
