import {
  MARID_AGENT_PROVIDER,
  MARID_AGENT_PROVIDER_LABEL,
  type MaridProvider,
} from "@marid/shared";

export const MARID_AGENT_PROVIDERS: MaridProvider[] = [MARID_AGENT_PROVIDER];

export const MARID_AGENT_PROVIDER_LABELS: Record<MaridProvider, string> = {
  glm: MARID_AGENT_PROVIDER_LABEL,
};

export function supportsMaridSessionFork(provider: MaridProvider): boolean {
  void provider;
  // 功能开关：fork 入口恢复对所有 Marid provider 开放；实际可操作性仍由 task 状态、
  // assistant 目标和运行中保护 guard 控制，避免把 active/failed 分支暴露为可 fork。
  return true;
}

export function shouldHideMaridSessionForkAction(provider: MaridProvider): boolean {
  return !supportsMaridSessionFork(provider);
}
