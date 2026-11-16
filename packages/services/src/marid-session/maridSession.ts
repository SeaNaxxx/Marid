import { ServiceChannels } from "@marid/shared";
import type {
  TraceId,
  MaridAgentMcpServer,
  MaridDeliveryKind,
  MaridMessageWithParts,
  ModelSelection,
  MaridPermissionRequestParams,
  MaridProviderRuntimeHeadersRequestParams,
  MaridUserInputRequestParams,
  MaridUserInputResponse,
  MaridSessionInfo,
  MaridSessionImportHistory,
  MaridSessionEvent,
  MaridSessionMode,
  MaridSessionPersistence,
  MaridSessionStateSnapshot,
  MaridStateUpdatedNotification,
  MaridWorkspacePresentation,
} from "@marid/shared";
import { createServiceDescriptor } from "#src/descriptors.js";
import type { ProviderRuntimeHeadersUiResponse } from "#src/marid-agent/maridAgent.js";

export interface MaridSessionWorkspaceTarget {
  workspacePath: string;
  workspaceIdentity?: string;
  remoteSessionId?: string;
}

export type MaridSessionReadWorkspacePresentationParams = MaridSessionWorkspaceTarget;

export interface MaridTaskTarget extends MaridSessionWorkspaceTarget {
  sessionId: string;
}

export interface MaridSessionCreateParams extends MaridSessionWorkspaceTarget {
  /** 仅导入事务使用的预分配 ID；普通新会话继续由 Agent 分配。 */
  sessionId?: string;
  sessionTraceId?: TraceId;
  parentSessionId?: string;
  mode?: MaridSessionMode;
  model?: ModelSelection;
  persistence?: MaridSessionPersistence;
  thoughtLevel?: string;
  mcpServers?: MaridAgentMcpServer[];
  importedHistory?: MaridSessionImportHistory;
}

export interface MaridSessionResumeParams extends MaridTaskTarget {
  model?: ModelSelection;
  thoughtLevel?: string;
  mcpServers?: MaridAgentMcpServer[];
  /**
   * 默认广播 resume 得到的历史快照，并让 shadow 订阅请求初始 snapshot。
   * 续聊发送前的 runtime 预恢复会关闭它，避免旧终态快照覆盖本地已开始的新输入运行态。
   */
  broadcastSnapshot?: boolean;
}

export interface MaridSessionListParams extends MaridSessionWorkspaceTarget {
  limit?: number;
}

export interface MaridSessionReadParams extends MaridTaskTarget {
  deliveryKind?: MaridDeliveryKind;
  messageLimit?: number;
  afterSeq?: number;
}

export interface MaridSessionMessagesParams extends MaridTaskTarget {
  afterMessageId?: string;
  limit?: number;
}

export interface MaridSessionEventsParams extends MaridTaskTarget {
  afterSeq?: number;
  limit?: number;
}

export interface MaridSessionSetModelParams extends MaridTaskTarget {
  model: ModelSelection;
  expectedRevision?: number;
  persistAsWorkspaceLastUsed?: boolean;
}

export interface MaridSessionSetThoughtLevelParams extends MaridTaskTarget {
  thoughtLevel?: string;
  expectedRevision?: number;
  persistAsWorkspaceLastUsed?: boolean;
}

export interface MaridSessionSetModeParams extends MaridTaskTarget {
  mode: MaridSessionMode;
  expectedRevision?: number;
}

export interface MaridSessionRespondProviderRuntimeHeadersParams extends MaridTaskTarget {
  requestId: string;
  response: ProviderRuntimeHeadersUiResponse;
}

export interface MaridSessionSubscribeParams extends MaridTaskTarget {
  deliveryKind: MaridDeliveryKind;
  afterSeq?: number;
  includeSnapshot?: boolean;
  eventCoalescing?: {
    mode: "background-summary";
    intervalMs?: number;
  };
}

export type MaridSessionServiceEvent =
  | { type: "session.event"; event: MaridSessionEvent }
  | { type: "state.updated"; notification: MaridStateUpdatedNotification }
  | { type: "permission.request"; request: MaridPermissionRequestParams }
  | { type: "userInput.request"; request: MaridUserInputRequestParams }
  | {
      type: "userInput.response";
      requestId: string;
      response: MaridUserInputResponse;
    }
  | { type: "providerRuntimeHeaders.request"; request: MaridProviderRuntimeHeadersRequestParams }
  | { type: "snapshot"; snapshot: MaridSessionStateSnapshot };

export interface MaridSessionInitializeResult {
  available: boolean;
  workspaceKey: string;
  protocolName?: string;
  protocolVersion?: number;
  transportKind?: "stdio" | "websocket";
  reason?: string;
  reasonCode?: "provider_not_ready";
}

export interface MaridSessionWorkspaceRuntimeIdentity {
  generation: number;
  identity: string;
  processId?: number;
  workspaceKey: string;
}

export interface IMaridSessionService {
  initializeWorkspace(params: MaridSessionWorkspaceTarget): Promise<MaridSessionInitializeResult>;
  getWorkspaceRuntimeIdentity(
    params: MaridSessionWorkspaceTarget,
  ): Promise<MaridSessionWorkspaceRuntimeIdentity>;
  readWorkspacePresentation(
    params: MaridSessionReadWorkspacePresentationParams,
  ): Promise<MaridWorkspacePresentation>;
  createSession(params: MaridSessionCreateParams): Promise<MaridSessionStateSnapshot>;
  resumeSession(params: MaridSessionResumeParams): Promise<MaridSessionStateSnapshot>;
  listSessions(params: MaridSessionListParams): Promise<MaridSessionInfo[]>;
  readSession(params: MaridSessionReadParams): Promise<MaridSessionStateSnapshot>;
  readSessionMessages(params: MaridSessionMessagesParams): Promise<MaridMessageWithParts[]>;
  readSessionEvents(params: MaridSessionEventsParams): Promise<MaridSessionEvent[]>;
  promoteDeferredDraftSession(params: MaridTaskTarget): Promise<void>;
  closeSession(params: MaridTaskTarget): Promise<void>;
  closeDeferredDraftSession(params: MaridTaskTarget): Promise<boolean>;
  setModel(params: MaridSessionSetModelParams): Promise<MaridSessionStateSnapshot>;
  setThoughtLevel(params: MaridSessionSetThoughtLevelParams): Promise<MaridSessionStateSnapshot>;
  setMode(params: MaridSessionSetModeParams): Promise<MaridSessionStateSnapshot>;
  respondProviderRuntimeHeaders(
    params: MaridSessionRespondProviderRuntimeHeadersParams,
  ): Promise<void>;
  // M5 删波次 2：onDynamicSessionEvent（旧 session/subscribe 词表的 renderer 订阅面）
  // 已删除——v4 UI 走 agentService 的 conversation/sessions-index 帧通道，
  // 该方法在 renderer 侧已无任何调用者（grep packages/ui packages/desktop 为零）。
}

export const IMaridSessionService = createServiceDescriptor<IMaridSessionService>(
  ServiceChannels.MaridSession,
);
