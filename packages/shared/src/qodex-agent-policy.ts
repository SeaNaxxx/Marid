import { z } from "zod";
import type { CommandAgentSource } from "./command-types.js";
import type { QodexProvider } from "./qodex-task-types-core.js";

export const QODEX_AGENT_PROVIDER = "glm" satisfies QodexProvider;
export const QODEX_AGENT_PROVIDER_LABEL = "Qodex Agent";
export const QODEX_COMMAND_AGENT_SOURCE = "qodexAgent" satisfies CommandAgentSource;

/**
 * 解析持久化/relay 数据时的 provider 归一 schema：
 * 已移除的第三方 provider 历史值仅作为归一化输入保留，统一收敛为 glm；
 * 其它非法值保持拒绝，与收窄前的校验边界一致。
 */
export const legacyQodexProviderSchema = z
  .enum(["claude", "opencode", "gemini", "codex", "glm"])
  .transform((): typeof QODEX_AGENT_PROVIDER => QODEX_AGENT_PROVIDER);

export const QODEX_ENABLED_AGENT_PROVIDERS = [
  QODEX_AGENT_PROVIDER,
] as const satisfies readonly QodexProvider[];

export const QODEX_COMMAND_AGENT_SOURCES = [
  QODEX_COMMAND_AGENT_SOURCE,
] as const satisfies readonly CommandAgentSource[];

export function normalizeAgentProviderToQodexAgent(
  _provider?: QodexProvider | null,
): QodexProvider {
  return QODEX_AGENT_PROVIDER;
}

export function normalizeAgentProvidersToQodexAgent(
  _providers?: ReadonlyArray<QodexProvider> | null,
): QodexProvider[] {
  return [...QODEX_ENABLED_AGENT_PROVIDERS];
}

export function normalizeCommandAgentSourceToQodexAgent(
  _agentSource?: CommandAgentSource | null,
): CommandAgentSource {
  return QODEX_COMMAND_AGENT_SOURCE;
}

export function isQodexAgentProvider(
  provider: QodexProvider | null | undefined,
): provider is typeof QODEX_AGENT_PROVIDER {
  return provider === QODEX_AGENT_PROVIDER;
}
