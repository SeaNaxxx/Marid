import type { QodexProvider } from "@qodex/shared";

export function resolveWorkspaceHeaderProvider(
  activeTaskProvider: QodexProvider | null,
  selectedProvider: QodexProvider,
): QodexProvider {
  // 当前 task 打开时，Header 应展示 task 自己的 provider；
  // 否则在“仅切换新建任务 provider”后会误显示成 workspace 级选择，和当前会话上下文不一致。
  return activeTaskProvider ?? selectedProvider;
}
