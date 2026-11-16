import { z } from "zod";
import type { CommandAgentSource } from "./command-types.js";
import type { MaridProvider } from "./marid-task-types-core.js";

export const MARID_AGENT_PROVIDER = "glm" satisfies MaridProvider;
export const MARID_AGENT_PROVIDER_LABEL = "Marid Agent";
export const MARID_COMMAND_AGENT_SOURCE = "maridAgent" satisfies CommandAgentSource;

/**
 * 解析持久化/relay 数据时的 provider 归一 schema：
 * 已移除的第三方 provider 历史值仅作为归一化输入保留，统一收敛为 glm；
 * 其它非法值保持拒绝，与收窄前的校验边界一致。
 */
export const legacyMaridProviderSchema = z
  .enum(["claude", "opencode", "gemini", "codex", "glm"])
  .transform((): typeof MARID_AGENT_PROVIDER => MARID_AGENT_PROVIDER);

export const MARID_ENABLED_AGENT_PROVIDERS = [
  MARID_AGENT_PROVIDER,
] as const satisfies readonly MaridProvider[];

export const MARID_COMMAND_AGENT_SOURCES = [
  MARID_COMMAND_AGENT_SOURCE,
] as const satisfies readonly CommandAgentSource[];

export function normalizeAgentProviderToMaridAgent(
  _provider?: MaridProvider | null,
): MaridProvider {
  return MARID_AGENT_PROVIDER;
}

export function normalizeAgentProvidersToMaridAgent(
  _providers?: ReadonlyArray<MaridProvider> | null,
): MaridProvider[] {
  return [...MARID_ENABLED_AGENT_PROVIDERS];
}

export function normalizeCommandAgentSourceToMaridAgent(
  _agentSource?: CommandAgentSource | null,
): CommandAgentSource {
  return MARID_COMMAND_AGENT_SOURCE;
}

export function isMaridAgentProvider(
  provider: MaridProvider | null | undefined,
): provider is typeof MARID_AGENT_PROVIDER {
  return provider === MARID_AGENT_PROVIDER;
}
