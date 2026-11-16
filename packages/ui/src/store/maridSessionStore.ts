/**
 * Marid session UI 状态 store
 *
 * 一个 tab 对应一个 workspace，所以聊天相关状态也必须按 workspace 分桶保存。
 * 这样切换标签页时，当前任务、输入中的草稿态和初始化状态才不会互相串台。
 */
import { create } from "zustand";
import { shouldExposeE2EStoreBridge } from "@/lib/e2eStoreBridge.js";
import { type MaridSessionStoreState } from "./maridSessionStoreTypes.js";
import { getWorkspaceState } from "./maridSessionStoreSelectors.js";
import { createNavigationSlice } from "./maridSessionStoreNavigation.js";
import { createTaskSlice } from "./maridSessionStoreTaskSlice.js";
import { createWorkspaceSlice } from "./maridSessionStoreWorkspaceSlice.js";
import { uiMemoryDiagnosticsRegistry } from "@/lib/memoryDiagnostics.js";

export const useMaridSessionStore = create<MaridSessionStoreState>()((set, get) => ({
  workspaces: {},
  ...createNavigationSlice(set, get),
  ...createWorkspaceSlice(set),
  ...createTaskSlice(set),
  getWorkspaceState: (workspacePath: string, workspaceIdentity?: string) =>
    getWorkspaceState(get(), workspacePath, workspaceIdentity),
}));

type MaridSessionStoreE2EBridge = typeof useMaridSessionStore;

declare global {
  interface Window {
    __maridSessionStoreE2E?: MaridSessionStoreE2EBridge;
  }
}

if (shouldExposeE2EStoreBridge()) {
  // E2E 诊断入口必须由 WDIO 显式打开，不能复用 MARID_ENV=test，避免产品测试环境暴露可变全局 store。
  window.__maridSessionStoreE2E = useMaridSessionStore;
}

// ────────────────────────────────────────────
// Re-exports: 保持外部 `from '@/store/maridSessionStore'` 的导入路径继续工作
// ────────────────────────────────────────────
export * from "./maridSessionStoreTypes.js";
export * from "./maridSessionStoreSelectors.js";
// Re-export navigation types used externally:
export type {
  TaskNavigationHistory,
  TaskNavEntry,
  WorkspaceNavEntry,
} from "@/lib/taskNavigationHistory.js";

// 内存诊断计数器：workspace 桶全仓无删除路径，先落日志。
uiMemoryDiagnosticsRegistry.register("sessionStore", () => ({
  workspaces: Object.keys(useMaridSessionStore.getState().workspaces).length,
}));
