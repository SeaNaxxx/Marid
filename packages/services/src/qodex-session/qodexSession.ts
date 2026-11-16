import { ServiceChannels } from "@qodex/shared";
import type {
  TraceId,
  QodexAgentMcpServer,
  QodexDeliveryKind,
  QodexMessageWithParts,
  ModelSelection,
  QodexPermissionRequestParams,
  QodexProviderRuntimeHeadersRequestParams,
  QodexUserInputRequestParams,
  QodexUserInputResponse,
  QodexSessionInfo,
  QodexSessionImportHistory,
  QodexSessionEvent,
  QodexSessionMode,
  QodexSessionPersistence,
  QodexSessionStateSnapshot,
  QodexStateUpdatedNotification,
  QodexWorkspacePresentation,
} from "@qodex/shared";
import { createServiceDescriptor } from "#src/descriptors.js";
import type { ProviderRuntimeHeadersUiResponse } from "#src/qodex-agent/qodexAgent.js";

export interface QodexSessionWorkspaceTarget {
  workspacePath: string;
  workspaceIdentity?: string;
  remoteSessionId?: string;
}

export type QodexSessionReadWorkspacePresentationParams = QodexSessionWorkspaceTarget;

export interface QodexTaskTarget extends QodexSessionWorkspaceTarget {
  sessionId: string;
}

export interface QodexSessionCreateParams extends QodexSessionWorkspaceTarget {
  /** 仅导入事务使用的预分配 ID；普通新会话继续由 Agent 分配。 */
  sessionId?: string;
  sessionTraceId?: TraceId;
  parentSessionId?: string;
  mode?: QodexSessionMode;
  model?: ModelSelection;
  persistence?: QodexSessionPersistence;
  thoughtLevel?: string;
  mcpServers?: QodexAgentMcpServer[];
  importedHistory?: QodexSessionImportHistory;
}

export interface QodexSessionResumeParams extends QodexTaskTarget {
  model?: ModelSelection;
  thoughtLevel?: string;
  mcpServers?: QodexAgentMcpServer[];
  /**
   * 默认广播 resume 得到的历史快照，并让 shadow 订阅请求初始 snapshot。
   * 续聊发送前的 runtime 预恢复会关闭它，避免旧终态快照覆盖本地已开始的新输入运行态。
   */
  broadcastSnapshot?: boolean;
}

export interface QodexSessionListParams extends QodexSessionWorkspaceTarget {
  limit?: number;
}

export interface QodexSessionReadParams extends QodexTaskTarget {
  deliveryKind?: QodexDeliveryKind;
  messageLimit?: number;
  afterSeq?: number;
}

export interface QodexSessionMessagesParams extends QodexTaskTarget {
  afterMessageId?: string;
  limit?: number;
}

export interface QodexSessionEventsParams extends QodexTaskTarget {
  afterSeq?: number;
  limit?: number;
}

export interface QodexSessionSetModelParams extends QodexTaskTarget {
  model: ModelSelection;
  expectedRevision?: number;
  persistAsWorkspaceLastUsed?: boolean;
}

export interface QodexSessionSetThoughtLevelParams extends QodexTaskTarget {
  thoughtLevel?: string;
  expectedRevision?: number;
  persistAsWorkspaceLastUsed?: boolean;
}

export interface QodexSessionSetModeParams extends QodexTaskTarget {
  mode: QodexSessionMode;
  expectedRevision?: number;
}

export interface QodexSessionRespondProviderRuntimeHeadersParams extends QodexTaskTarget {
  requestId: string;
  response: ProviderRuntimeHeadersUiResponse;
}

export interface QodexSessionSubscribeParams extends QodexTaskTarget {
  deliveryKind: QodexDeliveryKind;
  afterSeq?: number;
  includeSnapshot?: boolean;
  eventCoalescing?: {
    mode: "background-summary";
    intervalMs?: number;
  };
}

export type QodexSessionServiceEvent =
  | { type: "session.event"; event: QodexSessionEvent }
  | { type: "state.updated"; notification: QodexStateUpdatedNotification }
  | { type: "permission.request"; request: QodexPermissionRequestParams }
  | { type: "userInput.request"; request: QodexUserInputRequestParams }
  | {
      type: "userInput.response";
      requestId: string;
      response: QodexUserInputResponse;
    }
  | { type: "providerRuntimeHeaders.request"; request: QodexProviderRuntimeHeadersRequestParams }
  | { type: "snapshot"; snapshot: QodexSessionStateSnapshot };

export interface QodexSessionInitializeResult {
  available: boolean;
  workspaceKey: string;
  protocolName?: string;
  protocolVersion?: number;
  transportKind?: "stdio" | "websocket";
  reason?: string;
  reasonCode?: "provider_not_ready";
}

export interface QodexSessionWorkspaceRuntimeIdentity {
  generation: number;
  identity: string;
  processId?: number;
  workspaceKey: string;
}

export interface IQodexSessionService {
  initializeWorkspace(params: QodexSessionWorkspaceTarget): Promise<QodexSessionInitializeResult>;
  getWorkspaceRuntimeIdentity(
    params: QodexSessionWorkspaceTarget,
  ): Promise<QodexSessionWorkspaceRuntimeIdentity>;
  readWorkspacePresentation(
    params: QodexSessionReadWorkspacePresentationParams,
  ): Promise<QodexWorkspacePresentation>;
  createSession(params: QodexSessionCreateParams): Promise<QodexSessionStateSnapshot>;
  resumeSession(params: QodexSessionResumeParams): Promise<QodexSessionStateSnapshot>;
  listSessions(params: QodexSessionListParams): Promise<QodexSessionInfo[]>;
  readSession(params: QodexSessionReadParams): Promise<QodexSessionStateSnapshot>;
  readSessionMessages(params: QodexSessionMessagesParams): Promise<QodexMessageWithParts[]>;
  readSessionEvents(params: QodexSessionEventsParams): Promise<QodexSessionEvent[]>;
  promoteDeferredDraftSession(params: QodexTaskTarget): Promise<void>;
  closeSession(params: QodexTaskTarget): Promise<void>;
  closeDeferredDraftSession(params: QodexTaskTarget): Promise<boolean>;
  setModel(params: QodexSessionSetModelParams): Promise<QodexSessionStateSnapshot>;
  setThoughtLevel(params: QodexSessionSetThoughtLevelParams): Promise<QodexSessionStateSnapshot>;
  setMode(params: QodexSessionSetModeParams): Promise<QodexSessionStateSnapshot>;
  respondProviderRuntimeHeaders(
    params: QodexSessionRespondProviderRuntimeHeadersParams,
  ): Promise<void>;
  // M5 删波次 2：onDynamicSessionEvent（旧 session/subscribe 词表的 renderer 订阅面）
  // 已删除——v4 UI 走 agentService 的 conversation/sessions-index 帧通道，
  // 该方法在 renderer 侧已无任何调用者（grep packages/ui packages/desktop 为零）。
}

export const IQodexSessionService = createServiceDescriptor<IQodexSessionService>(
  ServiceChannels.QodexSession,
);
