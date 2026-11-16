import type { QodexSessionStateSnapshot } from "@qodex/shared";
import { createServiceLogger } from "#src/logger/serviceLogger.js";
import { repairImportedClaudeSessionSnapshot } from "#src/session/claude-native/importedClaudeHistoryRepair.js";
import type { IQodexAgentService } from "#src/qodex-agent/qodexAgent.js";
import type {
  QodexSessionReadParams,
  QodexSessionResumeParams,
} from "#src/qodex-session/qodexSession.js";

const logger = createServiceLogger("qodex-session-service");

export async function repairEmptyImportedClaudeSessionSnapshot(params: {
  agentService: IQodexAgentService;
  snapshot: QodexSessionStateSnapshot;
  target: QodexSessionResumeParams | QodexSessionReadParams;
}): Promise<QodexSessionStateSnapshot> {
  const repaired = await repairImportedClaudeSessionSnapshot({
    snapshot: params.snapshot,
    target: {
      workspacePath: params.target.workspacePath,
      workspaceIdentity: params.target.workspaceIdentity,
      taskId: params.target.sessionId,
      ...("mcpServers" in params.target && params.target.mcpServers
        ? { mcpServers: params.target.mcpServers }
        : {}),
    },
    createSession: (input) => params.agentService.createSession(input),
    onRepair: (history) => {
      logger.warn(
        undefined,
        `[qodex-session-service] Claude 导入 session 历史异常，按 ${history.source} 回填 taskId=${params.target.sessionId}`,
      );
    },
  });
  return repaired ?? params.snapshot;
}
