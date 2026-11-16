import {
  QODEX_AGENT_PROVIDER,
  QODEX_AGENT_PROVIDER_LABEL,
  type QodexProvider,
} from "@qodex/shared";

export const QODEX_AGENT_PROVIDERS: QodexProvider[] = [QODEX_AGENT_PROVIDER];

export const QODEX_AGENT_PROVIDER_LABELS: Record<QodexProvider, string> = {
  glm: QODEX_AGENT_PROVIDER_LABEL,
};

export function supportsQodexSessionFork(provider: QodexProvider): boolean {
  void provider;
  // 功能开关：fork 入口恢复对所有 Qodex provider 开放；实际可操作性仍由 task 状态、
  // assistant 目标和运行中保护 guard 控制，避免把 active/failed 分支暴露为可 fork。
  return true;
}

export function shouldHideQodexSessionForkAction(provider: QodexProvider): boolean {
  return !supportsQodexSessionFork(provider);
}
