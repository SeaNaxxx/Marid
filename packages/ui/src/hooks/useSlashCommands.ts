/**
 * Qodex Agent Slash Commands 便捷 hook
 *
 * 返回当前 workspace 下 Agent 广播的可用 slash commands 列表。
 */
import { useQodexSessionStore, selectWorkspaceQodexState } from "../store/qodexSessionStore.js";

export function useSlashCommands(workspacePath: string, workspaceIdentity?: string) {
  return useQodexSessionStore(
    (state) => selectWorkspaceQodexState(state, workspacePath, workspaceIdentity).slashCommands,
  );
}
