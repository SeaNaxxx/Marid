import type {
  V4SessionDeletionParams,
  V4SessionDeletionExecuteParams,
  V4SessionDeletionPreview,
  V4SessionDeletionResult,
} from "@marid/shared/marid-protocol-v4";
import type { PluginUiToolResultNotification } from "@marid/shared/mcp-apps";
import type {
  ContextComposition,
  ContextCompositionChangedEvent,
  ModelRequestMetricsFact,
} from "@marid/shared/marid-protocol-v4";
import type { MaridAgentContextCompositionParams } from "#src/marid-agent/contextCompositionClient.js";
import type {
  MaridAgentPluginMentionItemsParams,
  MaridAgentPluginMentionItemsResult,
} from "#src/marid-agent/pluginHostQueriesClient.js";
import type {
  BackgroundBashOutputResult,
  SessionDebugSnapshot,
  MaridSessionContentProfile,
} from "@marid/shared";
/* eslint-disable max-lines -- Marid agent service 接口集中声明 protocol/session/workspace 方法，拆分会增加 service descriptor 迁移成本。 */
import type { RemoteConversationTelemetryEvent } from "@marid/shared/conversation-telemetry";
import type { Event, IDisposable } from "@marid/rpc";
import { ServiceChannels } from "@marid/shared";
import type { AppUsageRange, AppUsageSnapshot, MaridTaskTokenUsageResult } from "@marid/shared";
import type { DynamicWorkflowMode } from "@marid/shared";
import type { MaridAutomation, MaridAutomationRun } from "@marid/shared";
import type {
  MaridStorageStartupState,
  MaridDeliveryKind,
  MaridAgentMcpServer,
  MaridBackgroundTurnAttribution,
  TraceId,
  MaridSessionCompactResult,
  MaridSessionGoalAction,
  MaridSessionGoalResult,
  MaridMessageWithParts,
  ModelSelection,
  MaridSessionImportHistory,
  MaridPermissionRequestParams,
  AgentLaneResourceSample,
  MaridMcpTelemetryEvent,
  MaridMcpResourceSample,
  MaridToolExecResource,
  MaridProcessChildProcess,
  MaridMcpListResult,
  WorkspaceMcpTrustDecision,
  MaridWorkspaceMcpTrustResult,
  MaridPluginsListResult,
  MaridPluginsOverviewResult,
  MaridPluginsMarketplaceMutationResult,
  MaridPluginsInstallResult,
  MaridPluginsReferenceCatalogResult,
  MaridSkillsReferenceCatalogResult,
  MaridMcpReadResourceResult,
  MaridMcpUiCallToolResult,
  MaridMcpUiCancelCallResult,
  MaridMcpUiReadResourceResult,
  MaridMcpUiListResourcesResult,
  MaridMcpUiAppToolAcceptedResult,
  MaridMcpUiRegisterAppToolsResult,
  MaridMcpUiUnregisterAppToolsResult,
  MaridMcpUiListResourceTemplatesResult,
  MaridPluginsListUiSurfacesResult,
  MaridWorkflowsDeleteResult,
  MaridWorkflowsForRunResult,
  MaridWorkflowsGetResult,
  MaridWorkflowsListResult,
  MaridWorkflowsMoveResult,
  MaridWorkflowsRunsResult,
  MaridWorkflowsSaveResult,
  MaridWorkflowsUpdateMetaResult,
  MaridPluginsUninstallResult,
  MaridPluginsRestoreBuiltinResult,
  MaridPluginsConfigureResult,
  MaridPluginsDescribeResult,
  MaridPluginsValidateResult,
  MaridPluginsSetEnabledResult,
  MaridPluginsCancelOperationResult,
  MaridPluginOperationProgressNotification,
  MaridProviderRuntimeHeadersRequestParams,
  RequestVerificationConfig,
  MaridProviderRuntimeHeadersCancelled,
  MaridProviderTestModelConnectivityParams,
  MaridProviderTestModelConnectivityResult,
  MaridUserInputRequestParams,
  MaridUserInputResponse,
  MaridSessionEvent,
  MaridSessionInfo,
  MaridSessionMode,
  MaridSessionPersistence,
  MaridSessionSendResult,
  MaridSessionRequestRuntimePreferencesParams,
  MaridSessionRuntimePreferencesResult,
  MaridSessionStateSnapshot,
  MaridSessionSubagentsResult,
  MaridStateUpdatedNotification,
  MaridTaskClientMode,
  MaridBrowserAmbientContext,
  MaridWorkspacePresentation,
  MaridWorkspaceGenerateTextResult,
  MaridWorkspaceGenerateTextParams,
  MaridWorkspaceHookTrustGrantResult,
  MaridAutomationBotDeliveryTarget,
} from "@marid/shared";
import type {
  ClientHello,
  CommandAck,
  CommandEnvelope,
  CommandKey,
  CommandsQueryResult,
  ConversationTopicWireCandidate,
  ConversationTelemetryFact,
  CuaPermissionObservation,
  ConversationRowTarget,
  HelloMessage,
  SessionsIndexTopicWireCandidate,
  SessionsIndexReconcileResult,
  V4AttachmentBeginResult,
  V4AttachmentChunkResult,
  V4AttachmentCommitResult,
  V4AttachmentPreviewSourceResult,
  V4AttachmentReadResult,
  V4ConversationAttachmentReadResult,
  V4ConversationAttachmentStatResult,
  V4ConnectionFlowState,
  V4ConversationFileChangesResult,
  V4ConversationFileRewindPreviewResult,
  V4ConversationInputPredictionResult,
  V4ConversationPlansResult,
  V4ConversationWorkflowRunEventsResult,
  V4ConversationWorkflowRunArtifactDataResult,
  V4ConversationWorkflowRunArtifactReadResult,
  V4ConversationWorkflowRunArtifactsResult,
  V4ConversationWorkflowRunNodeResultResult,
  V4ConversationWorkflowRunWorkspaceResult,
  V4ConversationWorkflowRunsResult,
  V4ConversationRowsRangeResult,
  V4ConversationResyncResult,
  V4ConversationSubscribeResult,
  V4SessionsIndexSubscribeResult,
  V4WorkspaceConfigSubscribeResult,
  WorkspaceConfigTopicWireCandidate,
} from "@marid/shared/marid-protocol-v4";
import type {
  V4SessionDataMeasureResult,
  V4SessionDataParams,
  V4SessionDataPurgeResult,
} from "@marid/shared/marid-protocol-v4";
import { createServiceDescriptor } from "../descriptors.js";

export * from "./maridAgentPluginParams.js";
export * from "./maridAgentWorkflowParams.js";
export type { MaridAgentContextCompositionParams } from "#src/marid-agent/contextCompositionClient.js";
export type {
  MaridAgentPluginMentionItemsParams,
  MaridAgentPluginMentionItemsResult,
} from "#src/marid-agent/pluginHostQueriesClient.js";
import type {
  MaridAgentAddPluginMarketplaceParams,
  MaridAgentAutomationIdParams,
  MaridAgentCancelPluginOperationParams,
  MaridAgentConfigurePluginParams,
  MaridAgentResetPluginConfigParams,
  MaridAgentCreateAutomationParams,
  MaridAgentDeleteAutomationRunParams,
  MaridAgentDescribePluginParams,
  MaridAgentInstallPluginParams,
  MaridAgentListMcpServerStatusesParams,
  MaridAgentPluginViewParams,
  MaridAgentPluginReferenceCatalogParams,
  MaridAgentSkillReferenceCatalogParams,
  MaridAgentReadMcpResourceParams,
  MaridAgentReadMcpResourceForUiParams,
  MaridAgentListMcpResourcesForUiParams,
  MaridAgentAppToolCallForUiParams,
  MaridAgentAppToolInstanceForUiParams,
  MaridAgentRegisterAppToolsForUiParams,
  MaridAgentResolveAppToolCallForUiParams,
  MaridAgentMcpResourceSubscriptionForUiParams,
  MaridAgentCallMcpToolForUiParams,
  MaridAgentCancelMcpToolCallForUiParams,
  MaridAgentResolveSuggestedPluginReferenceParams,
  MaridAgentRemovePluginMarketplaceParams,
  MaridAgentRestoreBuiltinPluginParams,
  MaridAgentSetPluginEnabledParams,
  MaridAgentSetAutomationEnabledParams,
  MaridAgentUninstallPluginParams,
  MaridAgentUpdatePluginMarketplaceParams,
  MaridAgentUpdatePluginParams,
  MaridAgentUpdateAutomationParams,
  MaridAgentValidatePluginParams,
  MaridAgentWorkspaceTarget,
} from "./maridAgentPluginParams.js";
import type {
  MaridAgentDeleteSavedWorkflowParams,
  MaridAgentFindSavedWorkflowForRunParams,
  MaridAgentGetSavedWorkflowParams,
  MaridAgentListSavedWorkflowRunsParams,
  MaridAgentListSavedWorkflowsParams,
  MaridAgentMoveSavedWorkflowParams,
  MaridAgentSaveSavedWorkflowFromRunParams,
  MaridAgentUpdateSavedWorkflowMetaParams,
} from "./maridAgentWorkflowParams.js";

export interface MaridAgentSessionTarget extends MaridAgentWorkspaceTarget {
  sessionId: string;
}

export interface MaridAgentResumeSessionParams extends MaridAgentSessionTarget {
  additionalDirectories?: string[];
  model?: ModelSelection;
  thoughtLevel?: string;
  mcpServers?: MaridAgentMcpServer[];
  // 冷恢复会重建 runtime，工具面隔离必须和 create 保持同一安全边界（CUA 只放行 marid-cua 工具、
  // 禁 Bash 等）。否则 resume 后模型可见工具面/执行权限会比创建时更宽。
  toolAllowlist?: string[];
  toolDenylist?: string[];
}

export interface MaridAgentInitializeResult {
  available: boolean;
  workspaceKey: string;
  protocolName?: string;
  protocolVersion?: number;
  transportKind?: "stdio" | "websocket";
  reason?: string;
  reasonCode?: "provider_not_ready";
}

export interface MaridAgentRunAutomationNowResult {
  status: "queued" | "duplicate";
}

export interface MaridAgentWorkspaceRuntimeIdentity {
  generation: number;
  identity: string;
  processId?: number;
  workspaceKey: string;
}

export const MARID_AGENT_RUNTIME_UNAVAILABLE_CODE = "MARID_AGENT_RUNTIME_UNAVAILABLE";

export type MaridAgentRuntimePolicy = "start-if-needed" | "existing-only";

export interface MaridAgentRuntimeLifecycleEvent extends MaridAgentWorkspaceTarget {
  workspaceKey: string;
  runtimeIdentity: MaridAgentWorkspaceRuntimeIdentity;
  state: "available" | "unavailable";
}

/**
 * runtime 进程意外退出（晚于同一进程的 unavailable，退出码已知）。只用于向用户说明原因；
 * 回合、子代理的收口由 CLI 投影在重连后给出，界面不据此改写会话状态。
 */
export interface MaridAgentRuntimeExitedEvent extends MaridAgentWorkspaceTarget {
  workspaceKey: string;
  runtimeIdentity: MaridAgentWorkspaceRuntimeIdentity;
  exitCode: number | null;
  signal: string | null;
  /** `native_out_of_memory`：Windows 0xE0000008，系统可提交内存耗尽。 */
  cause?: "native_out_of_memory";
  exitedAt: number;
}

export type MaridAgentCuaPermissionObservation = CuaPermissionObservation &
  MaridAgentWorkspaceTarget;

export interface MaridAgentCreateSessionParams extends MaridAgentWorkspaceTarget {
  projectId?: string | null;
  additionalDirectories?: string[];
  sessionId?: string;
  sessionTraceId?: TraceId;
  parentSessionId?: string;
  mode?: MaridSessionMode;
  model?: ModelSelection;
  persistence?: MaridSessionPersistence;
  thoughtLevel?: string;
  /** automation 执行会话关闭模型二次命名，保持首条用户 query 作为稳定标题。 */
  titleGenerationEnabled?: boolean;
  mcpServers?: MaridAgentMcpServer[];
  toolAllowlist?: string[];
  toolDenylist?: string[];
  importedHistory?: MaridSessionImportHistory;
}

export interface MaridAgentListSessionsParams extends MaridAgentWorkspaceTarget {
  sessionIds?: string[];
  runtimePolicy?: MaridAgentRuntimePolicy;
  limit?: number;
}

export interface MaridAgentListSessionSubagentsParams extends MaridAgentSessionTarget {
  endedCursor?: string;
  endedLimit?: number;
  /** 远程 workspace 的宿主连接身份；只用于选择现有 Host，不进入 CLI wire query。 */
  remoteSessionId?: string;
}

export interface MaridAgentAppUsageParams {
  range: AppUsageRange;
  timeZone?: string;
}

export interface MaridAgentTaskTokenUsageParams extends MaridAgentSessionTarget {}

export interface MaridAgentReadSessionParams extends MaridAgentSessionTarget {
  deliveryKind?: MaridDeliveryKind;
  messageLimit?: number;
  afterSeq?: number;
  /** 被动索引/观察者只能读取现有 runtime，禁止为了读快照拉起 session。 */
  runtimePolicy?: MaridAgentRuntimePolicy;
  /** "index" 仅供 task index：返回剥离大载荷的同结构 snapshot，禁止回写 UI 状态。 */
  contentProfile?: MaridSessionContentProfile;
}

export interface MaridAgentReadSessionMessagesParams extends MaridAgentSessionTarget {
  afterMessageId?: string;
  limit?: number;
  direction?: "forward" | "backward";
}

export interface MaridAgentReadSessionEventsParams extends MaridAgentSessionTarget {
  afterSeq?: number;
  limit?: number;
  /** App 后台状态读取只观察现有 CLI，不因 runtime 消失重启。 */
  runtimePolicy?: MaridAgentRuntimePolicy;
}

export type MaridAgentReadWorkspacePresentationParams = MaridAgentWorkspaceTarget;

export interface MaridAgentGrantWorkspaceHookTrustParams extends MaridAgentWorkspaceTarget {
  bundleDigest: string;
  hookDeclarationDigest: string;
}

export interface MaridAgentSendPromptParamsBase extends MaridAgentSessionTarget {
  modelSelection?: ModelSelection;
  modelSelectionBasis?: ModelSelection;
  modelExecution?: import("@marid/shared/marid-protocol-v4").CommandPayloadMap["sendText"]["modelExecution"];
  inputId?: string;
  queryId?: string;
  messageId?: string;
  sessionTraceId?: TraceId;
  content: string;
  attachments?: Record<string, unknown>[];
  /** provider-only 的当前 IAB 状态；UI/session persistence 仍使用 content 原文。 */
  browserAmbientContext?: MaridBrowserAmbientContext;
  clientMode?: MaridTaskClientMode;
  expectedRevision?: number;
  expectedProviderRevision?: string;
  runtimeProviderHeaders?: Record<string, string>;
  toolDenylist?: string[];
  /** Bot 来源 turn 的稳定回推地址；只在当前 turn 内供 CronCreate 读取。 */
  botDeliveryTarget?: MaridAutomationBotDeliveryTarget;
}

export type MaridAgentSendPromptParams = MaridAgentSendPromptParamsBase &
  MaridBackgroundTurnAttribution;

export interface MaridAgentCompactParams extends MaridAgentSessionTarget {
  inputId?: string;
  instructions?: string;
  expectedRevision?: number;
}

export interface MaridAgentGoalParams extends MaridAgentSessionTarget {
  inputId?: string;
  action: MaridSessionGoalAction;
  objective?: string;
  expectedRevision?: number;
}

export interface MaridAgentSetModelParams extends MaridAgentSessionTarget {
  model: ModelSelection;
  expectedRevision?: number;
  persistAsWorkspaceLastUsed?: boolean;
}

export interface MaridAgentSetThoughtLevelParams extends MaridAgentSessionTarget {
  thoughtLevel?: string;
  expectedRevision?: number;
  persistAsWorkspaceLastUsed?: boolean;
}

export interface MaridAgentSetModeParams extends MaridAgentSessionTarget {
  mode: MaridSessionMode;
  expectedRevision?: number;
}

export interface MaridAgentGenerateWorkspaceTextParams extends MaridAgentWorkspaceTarget {
  selection: MaridWorkspaceGenerateTextParams["selection"];
  prompt?: string;
  messages?: MaridWorkspaceGenerateTextParams["messages"];
  tools?: MaridWorkspaceGenerateTextParams["tools"];
  querySource: string;
  maxOutputTokens?: number;
  signal?: AbortSignal;
  /**
   * 协议层 RPC 超时。thinking 模型的长请求会超过协议 client 默认的
   * 3 分钟；调用方必须把自身 deadline 透传到这里，否则默认超时先触发、
   * 还会被 onRequestTimeout 误判 stale 杀进程。
   */
  requestTimeoutMs?: number;
}

export interface MaridAgentTestModelConnectivityParams extends MaridAgentWorkspaceTarget {
  selection: MaridProviderTestModelConnectivityParams["selection"];
  signal?: AbortSignal;
}

// 合并时曾混用 Agent 完整鉴权响应；UI 只能回传请求校验，账号鉴权必须由 Host 合成。
export type ProviderRuntimeHeadersUiResponse =
  | { headersApplied: true; runtimeProviderHeaders: Record<string, string>; errorMessage?: string }
  | { headersApplied: false; errorMessage?: string };

export interface MaridAgentRespondProviderRuntimeHeadersParams extends MaridAgentSessionTarget {
  requestId: string;
  response: ProviderRuntimeHeadersUiResponse;
}

export interface MaridAgentSessionRuntimePreferencesRequest extends MaridSessionRequestRuntimePreferencesParams {
  requestId: string;
}

export interface MaridAgentRespondSessionRuntimePreferencesParams {
  requestId: string;
  resolution:
    | { status: "resolved"; preferences: MaridSessionRuntimePreferencesResult }
    | { status: "failed"; message: string };
}

export interface MaridAgentSessionSubscribeParams extends MaridAgentSessionTarget {
  deliveryKind: MaridDeliveryKind;
  afterSeq?: number;
  includeSnapshot?: boolean;
  eventCoalescing?: {
    mode: "background-summary";
    intervalMs?: number;
  };
}

// ── v4 conversation 通道（竖切）──
// host 只做转发：subscribe/unsubscribe/command 透传给 CLI v4 gateway，
// v4/conversation/frame 通知按 workspace fan-out 给 renderer。

export interface MaridAgentConversationSubscribeParams extends MaridAgentSessionTarget {
  retryInitialization?: string;
  /** 水位不变量：仅当客户端真持有该时刻一致状态才允许带。 */
  base?: { logEpoch: string; seq: number };
  visibility?: "foreground" | "background";
}

export interface MaridAgentConversationUnsubscribeParams extends MaridAgentWorkspaceTarget {
  subscriptionId: string;
  runtimePolicy?: MaridAgentRuntimePolicy;
}

export interface MaridAgentConversationResyncParams extends MaridAgentWorkspaceTarget {
  subscriptionId: string;
  base: { logEpoch: string; seq: number } | null;
  forceSnapshot?: boolean;
  runtimePolicy?: MaridAgentRuntimePolicy;
}

/** 行分页 query（rows/range）：按游标向上取一窗历史行。 */
export interface MaridAgentConversationRowsRangeParams extends MaridAgentSessionTarget {
  /** 取 rowId < beforeRowId 的行；缺省 = 从当前尾部向前。 */
  beforeRowId?: number;
  /** 1..rowsRangeMaxLimit（200）。 */
  limit: number;
  /**
   * 手机预加载：只读热会话。工作区 CLI 未运行、执行端不支持、会话不在内存都报
   * proto.sessionNotResident，不拉起进程、不冷恢复。
   */
  residency?: "resident-only";
}

/** 当前有效分支里的终态 ExitPlanMode 目录。 */
export type MaridAgentConversationPlansParams = MaridAgentSessionTarget;

/** 手机远控输入预测（ghost hint）；线格式见 docs/web-remote-control/mobile-input-prediction.md。 */
export interface MaridAgentConversationInputPredictionParams extends MaridAgentSessionTarget {
  /** 手机当前看到的会话 revision；参与 CLI 侧缓存键，回包据此丢弃过期结果。 */
  revision: number;
  /** 当前草稿；缺省/空 = 预测下一条输入，非空 = 续写草稿。 */
  draft?: string;
  /** 手机侧请求 id，原样回声，用于丢弃过期回包。 */
  requestId?: string;
}

/** workflow run 的事件日志分页（详情页审计面）；cursor = journal sequence。 */
export interface MaridAgentConversationWorkflowRunEventsParams extends MaridAgentSessionTarget {
  runId: string;
  afterSequence?: number;
  limit?: number;
}

/** dwf run 的枚举（重启后的发现查询）。 */
export interface MaridAgentConversationWorkflowRunsParams extends MaridAgentSessionTarget {
  limit?: number;
}

// ── dwf 用户面产物──
// ⚠ 术语：artifact = 脚本经 `artifact.*` 发布给**用户**看的产出（文件 / markdown / 预置看板），
// 不是 run 的顶层返回值（引擎内部对后者的同名叫法）。

/** 产物清单；UI 冷恢复与中枢详情的 durable 读法。 */
export interface MaridAgentConversationWorkflowRunArtifactsParams extends MaridAgentSessionTarget {
  runId: string;
}

/** 预置看板的取数面；cursor = journal sequence（严格大于）。 */
export interface MaridAgentConversationWorkflowRunArtifactDataParams extends MaridAgentSessionTarget {
  runId: string;
  artifactId: string;
  afterSequence?: number;
  limit?: number;
}

/** 内容产物的字节，一次一块（≤ 512 KiB，形状逐字照 attachmentRead）。 */
export interface MaridAgentConversationWorkflowRunArtifactReadParams extends MaridAgentSessionTarget {
  runId: string;
  artifactId: string;
  version: number;
  offset: number;
  limit: number;
}

// ── dwf 工作区 transcript──
/** 轻行清单：一个 run 的 files.* / git.* / world.run 行，不带正文。 */
export interface MaridAgentConversationWorkflowRunWorkspaceParams extends MaridAgentSessionTarget {
  runId: string;
}

/** 一个工作区节点的正文，按 maxBytes 保形有界化（缺省与上限在 CLI 网关侧）。 */
export interface MaridAgentConversationWorkflowRunNodeResultParams extends MaridAgentSessionTarget {
  runId: string;
  siteId: string;
  ordinal: number;
  maxBytes?: number;
}

export interface MaridAgentBackgroundBashOutputParams extends MaridAgentSessionTarget {
  workId: string;
}

export interface MaridAgentConversationFileChangesParams extends MaridAgentSessionTarget {
  target: ConversationRowTarget;
  baseRevision: number;
  baseLogEpoch: string;
}

export interface MaridAgentConversationFileRewindPreviewParams extends MaridAgentSessionTarget {
  target: ConversationRowTarget;
  baseRevision: number;
  baseLogEpoch: string;
}

/**
 * Host 进程内的受管 worktree 服务发送命令时设置的标记。只有带此标记的命令可以携带
 * createSession.cwd / worktreeCreation 与 forkAssistant.targetCwd；Symbol 键无法经 RPC 序列化伪造。
 */
export const HOST_WORKTREE_COMMAND: unique symbol = Symbol("marid.hostWorktreeCommand");

export interface MaridAgentConversationCommandParams extends MaridAgentWorkspaceTarget {
  /** 仅 Host 消费；显式 NULL 表示全局任务，不向 CLI 发送项目概念。 */
  projectId?: string | null;
  envelope: CommandEnvelope;
  /** 只有 Host 进程内的受管 worktree 服务设置（见 HOST_WORKTREE_COMMAND）。 */
  [HOST_WORKTREE_COMMAND]?: true;
  /** 仅 host 内部用于 Browser Use runtime 边界，不进入 v4 wire envelope。 */
  clientMode?: MaridTaskClientMode;
}

export interface MaridAgentCommandsQueryParams extends MaridAgentWorkspaceTarget {
  clock?: true;
  commands: CommandKey[];
}

/** UI 不携带 connectionId；connection scope 以 trusted carrier 注入 wire identity。 */
export interface MaridAgentAttachmentBeginParams extends MaridAgentSessionTarget {
  uploadId: string;
  fileName: string;
  mime: string;
  totalBytes: number;
  totalChunks: number;
  checksum: string;
}

export interface MaridAgentAttachmentChunkParams extends MaridAgentSessionTarget {
  uploadId: string;
  chunkIndex: number;
  dataBase64: string;
}

export interface MaridAgentAttachmentTerminalParams extends MaridAgentSessionTarget {
  uploadId: string;
}

export interface MaridAgentAttachmentReadParams extends MaridAgentSessionTarget {
  ref: string;
  target?: ConversationRowTarget;
  attachmentIndex?: number;
  offset: number;
  limit: number;
}

export interface MaridAgentConversationAttachmentReadParams extends MaridAgentSessionTarget {
  ref: string;
  target: ConversationRowTarget;
  attachmentIndex: number;
  offset: number;
  limit: number;
}

export interface MaridAgentConversationAttachmentStatParams extends MaridAgentSessionTarget {
  ref: string;
  target: ConversationRowTarget;
  attachmentIndex: number;
}

export interface MaridAgentAttachmentPreviewSourceParams extends MaridAgentSessionTarget {
  ref: string;
  target?: ConversationRowTarget;
  attachmentIndex?: number;
}

/** host scope 内部 transport 控制面；connectionId 只能经 trusted carrier 注入。 */
export interface MaridAgentConnectionFlowParams extends MaridAgentWorkspaceTarget {
  state: V4ConnectionFlowState;
}

/** sessions-index：workspace 级列表订阅（无 sessionId 维度）。 */
export interface MaridAgentSessionsIndexSubscribeParams extends MaridAgentWorkspaceTarget {
  base?: { logEpoch: string; seq: number };
  visibility?: "foreground" | "background";
  /**
   * 订阅者作用域后缀：CLI 侧重订阅替换按 (connectionId, topic) 判定，
   * host 进程内多个独立消费者（renderer 侧栏 / task-index syncer）订阅同一 topic 时
   * 必须用不同 connectionId，否则互相替换对方的订阅代际。缺省共享 host 连接 id。
   */
  subscriberScope?: string;
  /**
   * task-list 等被动观察者必须使用 existing-only；runtime 不存在时返回稳定 unavailable，
   * 禁止为了建立列表订阅而启动 Agent。缺省保持显式会话入口的旧行为。
   */
  runtimePolicy?: MaridAgentRuntimePolicy;
}

/** workspace-config：workspace 级配置目录订阅（config options + slash 目录）。 */
export interface MaridAgentWorkspaceConfigSubscribeParams extends MaridAgentWorkspaceTarget {
  base?: { logEpoch: string; seq: number };
  visibility?: "foreground" | "background";
  subscriberScope?: string;
  runtimePolicy?: MaridAgentRuntimePolicy;
}

/** Host 为 App 应答方绑定的本次配置；不扩展 Agent wire 协议。 */
export type MaridAgentProviderRuntimeHeadersRequest = MaridProviderRuntimeHeadersRequestParams & {
  requestVerificationConfig?: RequestVerificationConfig;
};

export type MaridAgentServiceEvent =
  | { type: "session.event"; event: MaridSessionEvent }
  | { type: "state.updated"; notification: MaridStateUpdatedNotification }
  | { type: "permission.request"; request: MaridPermissionRequestParams }
  | { type: "userInput.request"; request: MaridUserInputRequestParams }
  | {
      type: "userInput.response";
      requestId: string;
      response: MaridUserInputResponse;
    }
  | { type: "providerRuntimeHeaders.request"; request: MaridAgentProviderRuntimeHeadersRequest }
  | { type: "snapshot"; snapshot: MaridSessionStateSnapshot };

export interface MaridAgentAppRuntimePreferences {
  askUserQuestionAutoResolutionEnabled: boolean;
  modelIoFullRetentionEnabled?: boolean;
  pluginAutoRecommendationEnabled?: boolean;
}

export interface MaridAgentLocalRuntimeChildProcesses {
  pid: number;
  provider: string;
  workspacePath: string;
  lane?: string;
  children: MaridProcessChildProcess[];
}

export interface MaridAgentStorageStartupSnapshot {
  generation: number;
  state: MaridStorageStartupState | null;
}

export interface IMaridAgentService {
  /** 控制面不需要账号或模型，且不发送普通协议请求。 */
  prepareStorage(params: MaridAgentWorkspaceTarget): Promise<void>;
  getStorageStartupState(
    params: MaridAgentWorkspaceTarget,
  ): Promise<MaridAgentStorageStartupSnapshot | null>;
  onDynamicStorageStartupState(
    params: MaridAgentWorkspaceTarget,
  ): Event<MaridAgentStorageStartupSnapshot>;
  initialize(params: MaridAgentWorkspaceTarget): Promise<MaridAgentInitializeResult>;
  /**
   * 同步 App 全局运行时偏好到所有已活动 workspace；不得为此启动空闲 Agent。
   */
  syncAppRuntimePreferences(preferences: MaridAgentAppRuntimePreferences): Promise<void>;
  /**
   * 动态工作流用户选择变化的信号（docs/dynamic-workflow/launch.md「The user's choice」）：Host 重新计算
   * 生效模式并串行重发 workspace/updateDynamicWorkflowPolicy。有本机设置权威的 Host 以设置文件为准；
   * desktop-attached remote Host 以传入值为准（缺席 = 跟随服务端）。不得为此启动空闲 Agent。
   * Host 记下选择即返回，不等 CLI 应答；返回后重读的可用性快照已是新模式。
   */
  syncDynamicWorkflowUserMode(params: { mode?: DynamicWorkflowMode }): Promise<void>;
  getWorkspaceRuntimeIdentity(
    params: MaridAgentWorkspaceTarget,
  ): Promise<MaridAgentWorkspaceRuntimeIdentity>;
  createSession(params: MaridAgentCreateSessionParams): Promise<MaridSessionStateSnapshot>;
  resumeSession(params: MaridAgentResumeSessionParams): Promise<MaridSessionStateSnapshot>;
  listSessions(params: MaridAgentListSessionsParams): Promise<MaridSessionInfo[]>;
  listSessionSubagents(
    params: MaridAgentListSessionSubagentsParams,
  ): Promise<MaridSessionSubagentsResult>;
  getAppUsageStats(params: MaridAgentAppUsageParams): Promise<AppUsageSnapshot>;
  previewSessionDeletion?(params: V4SessionDeletionParams): Promise<V4SessionDeletionPreview>;
  executeSessionDeletion?(params: V4SessionDeletionExecuteParams): Promise<V4SessionDeletionResult>;
  /** 项目数据删除前的测量：只读，用本机已有 runtime 或管理面进程，不为某个项目拉起 Agent。 */
  measureSessionData?(params: V4SessionDataParams): Promise<V4SessionDataMeasureResult>;
  /**
   * 彻底删除会话与目录记忆。workspace 已有 Agent 进程时交给它（它持有会话运行态，能判断运行中
   * 并释放已加载会话）；否则交给管理面进程。只用于本机。
   */
  purgeSessionData?(
    params: V4SessionDataParams & { workspace: MaridAgentWorkspaceTarget },
  ): Promise<V4SessionDataPurgeResult>;
  /** 该 workspace 的 CLI 是否在 runtime/capabilities 中声明 worktreeCwd（只供 Host 进程内调用）。 */
  supportsWorktreeCwd?(target: MaridAgentWorkspaceTarget): Promise<boolean>;
  /**
   * 该 workspace 的 CLI 是否在 runtime/capabilities 中声明 extensionInputSource（只供 Host 进程内调用）。
   * 代扩展发消息前必须确认；false 时调用方报 unavailable，不去掉来源后发送
   * （apps/marid-cli/docs/design/v2/conversation-input-source.md §7）。
   */
  supportsExtensionInputSource?(target: MaridAgentWorkspaceTarget): Promise<boolean>;
  /**
   * 该 workspace 的 CLI 是否在 runtime/capabilities 中声明 conversationQuoteComment。
   * renderer 据此决定是否提供选区“批注”入口；未实现（旧 Host / 远端）按不支持处理。
   */
  supportsConversationQuoteComment?(target: MaridAgentWorkspaceTarget): Promise<boolean>;
  getTaskTokenUsage(params: MaridAgentTaskTokenUsageParams): Promise<MaridTaskTokenUsageResult>;
  readSession(params: MaridAgentReadSessionParams): Promise<MaridSessionStateSnapshot>;
  readSessionMessages(
    params: MaridAgentReadSessionMessagesParams,
  ): Promise<MaridMessageWithParts[]>;
  readSessionDebug(params: MaridAgentSessionTarget): Promise<SessionDebugSnapshot>;
  readSessionEvents(params: MaridAgentReadSessionEventsParams): Promise<MaridSessionEvent[]>;
  readWorkspacePresentation(
    params: MaridAgentReadWorkspacePresentationParams,
  ): Promise<MaridWorkspacePresentation>;
  /** 无 task/session 的 Settings 预信任；Agent 会重新发现并校验 canonical snapshot。 */
  grantWorkspaceHookTrust(
    params: MaridAgentGrantWorkspaceHookTrustParams,
  ): Promise<MaridWorkspaceHookTrustGrantResult>;
  setWorkspaceMcpTrust(params: WorkspaceMcpTrustDecision): Promise<MaridWorkspaceMcpTrustResult>;
  listMcpServerStatuses(params: MaridAgentListMcpServerStatusesParams): Promise<MaridMcpListResult>;
  listPlugins(params: MaridAgentPluginViewParams): Promise<MaridPluginsListResult>;
  /**
   * Plugin 对话引用 catalog：session-scoped 只读投影。
   * 走 workspace 级 agent client（session 记录只存在于该进程），不走独立插件管理进程。
   */
  getPluginReferenceCatalog(
    params: MaridAgentPluginReferenceCatalogParams,
  ): Promise<MaridPluginsReferenceCatalogResult>;
  /** Composer Skill 引用 catalog；带 sessionId 时读取该 runtime 的冻结快照。 */
  getSkillReferenceCatalog(
    params: MaridAgentSkillReferenceCatalogParams,
  ): Promise<MaridSkillsReferenceCatalogResult>;
  /** 插件 UI：读取插件 MCP 的 `ui://` 资源；走 session 所在 workspace agent 进程。 */
  openMcpUiInstance(
    params: import("../plugin-ui-bridge/contract.js").PluginUiPrepareSandboxParams & {
      accountContext?: string;
    },
  ): Promise<import("@marid/shared/mcp-apps").McpAppInstance>;
  validateMcpUiInstance(
    params: import("../plugin-ui-bridge/contract.js").PluginUiPluginScope,
  ): Promise<void>;
  recycleMcpUiInstance(
    params: import("../plugin-ui-bridge/contract.js").PluginUiPluginScope,
  ): Promise<boolean>;
  closeMcpUiInstance(
    params: import("../plugin-ui-bridge/contract.js").PluginUiPluginScope,
  ): Promise<void>;
  readMcpResource(params: MaridAgentReadMcpResourceParams): Promise<MaridMcpReadResourceResult>;
  /** 插件 UI：UI 发起的工具调用；复用既有权限审批，不产生 transcript row。 */
  sampleMcpApp(
    params: import("../plugin-ui-bridge/samplingContract.js").PluginUiSamplingParams,
  ): Promise<import("@marid/shared/mcp-apps").McpAppsSamplingResult>;
  cancelMcpAppSampling(
    params: import("../plugin-ui-bridge/samplingContract.js").PluginUiCancelSamplingParams,
  ): Promise<{ cancelled: boolean }>;
  callMcpToolForUi(params: MaridAgentCallMcpToolForUiParams): Promise<MaridMcpUiCallToolResult>;
  /** 插件 UI 取消带 callId 的进行中 UI 工具调用；agent 侧 abort 到 MCP client。 */
  cancelMcpToolCallForUi(
    params: MaridAgentCancelMcpToolCallForUiParams,
  ): Promise<MaridMcpUiCancelCallResult>;
  /** 插件 UI 页面发起的 `resources/read`，agent 侧限同插件服务器、8 MiB、mimeType 白名单。 */
  readMcpResourceForUi(
    params: MaridAgentReadMcpResourceForUiParams,
  ): Promise<MaridMcpUiReadResourceResult>;
  /** 插件 UI 页面发起的 resources/list、resources/templates/list、subscribe、unsubscribe 代理。 */
  listMcpResourcesForUi(
    params: MaridAgentListMcpResourcesForUiParams,
  ): Promise<MaridMcpUiListResourcesResult>;
  listMcpResourceTemplatesForUi(
    params: MaridAgentListMcpResourcesForUiParams,
  ): Promise<MaridMcpUiListResourceTemplatesResult>;
  subscribeMcpResourceForUi(params: MaridAgentMcpResourceSubscriptionForUiParams): Promise<void>;
  unsubscribeMcpResourceForUi(params: MaridAgentMcpResourceSubscriptionForUiParams): Promise<void>;
  /** App-Provided Tools：页面工具登记 / 注销与模型调用的认领 / 回传（信箱投递走 v4 live 增量）。 */
  registerAppToolsForUi(
    params: MaridAgentRegisterAppToolsForUiParams,
  ): Promise<MaridMcpUiRegisterAppToolsResult>;
  unregisterAppToolsForUi(
    params: MaridAgentAppToolInstanceForUiParams,
  ): Promise<MaridMcpUiUnregisterAppToolsResult>;
  claimAppToolCallForUi(
    params: MaridAgentAppToolCallForUiParams,
  ): Promise<MaridMcpUiAppToolAcceptedResult>;
  resolveAppToolCallForUi(
    params: MaridAgentResolveAppToolCallForUiParams,
  ): Promise<MaridMcpUiAppToolAcceptedResult>;
  /** 插件 UI 工作区级面板入口（已启用插件清单 `ui.surfaces[]`）。 */
  listPluginUiSurfaces(
    params: MaridAgentWorkspaceTarget,
  ): Promise<MaridPluginsListUiSurfacesResult>;
  // 已保存工作流的 GUI 中枢：workspace 级、无会话，每次调用现扫 `<cwd>/.marid/workflows/`。
  // 全局档传 `scope: "global"`：带 workspace 就用它当载体，不带则由 services 层自选本机载体运行时。
  listSavedWorkflows(params: MaridAgentListSavedWorkflowsParams): Promise<MaridWorkflowsListResult>;
  getSavedWorkflow(params: MaridAgentGetSavedWorkflowParams): Promise<MaridWorkflowsGetResult>;
  updateSavedWorkflowMeta(
    params: MaridAgentUpdateSavedWorkflowMetaParams,
  ): Promise<MaridWorkflowsUpdateMetaResult>;
  deleteSavedWorkflow(
    params: MaridAgentDeleteSavedWorkflowParams,
  ): Promise<MaridWorkflowsDeleteResult>;
  listSavedWorkflowRuns(
    params: MaridAgentListSavedWorkflowRunsParams,
  ): Promise<MaridWorkflowsRunsResult>;
  // 在项目档 / 全局档之间移动同名文件：
  // `workspace` 是载体（移到项目传目标项目、移到全局传源项目），`to` 是落点档；不覆盖已存在的目标。
  moveSavedWorkflow(params: MaridAgentMoveSavedWorkflowParams): Promise<MaridWorkflowsMoveResult>;
  // 完成卡的「直接保存」（docs/dynamic-workflow/transcript-and-notifications.md）：按 runId 从
  // journal 取这次 run 实际跑过的脚本落盘，不经模型回合。两者都必带 workspace（journal 在那里）。
  saveSavedWorkflowFromRun(
    params: MaridAgentSaveSavedWorkflowFromRunParams,
  ): Promise<MaridWorkflowsSaveResult>;
  /** 这次 run 对应哪个已保存工作流（推导，不记忆）；顺带回 run 的实参供「再次运行」预填。 */
  findSavedWorkflowForRun(
    params: MaridAgentFindSavedWorkflowForRunParams,
  ): Promise<MaridWorkflowsForRunResult>;
  resolveSuggestedPluginReference(
    params: MaridAgentResolveSuggestedPluginReferenceParams,
  ): Promise<import("@marid/shared").MaridPluginsResolveSuggestedReferenceResult>;
  /** 推荐项 Plugin 首次本地检查缺失后的 operation-scoped 刷新进度。 */
  onDynamicPluginOperationProgress(
    operationId: string,
  ): Event<MaridPluginOperationProgressNotification>;
  getPluginsOverview(params: MaridAgentPluginViewParams): Promise<MaridPluginsOverviewResult>;
  /**
   * 资源管理器：枚举本 Host 内全部本地 Agent 进程（含 plugin / mcp-status 泳道），
   * 并向每个存活 runtime 请求 `process/childProcesses`；单个 runtime 失败只让它的 children 为空。
   */
  collectLocalRuntimeChildProcesses(
    signal?: AbortSignal,
  ): Promise<MaridAgentLocalRuntimeChildProcesses[]>;
  addPluginMarketplace(
    params: MaridAgentAddPluginMarketplaceParams,
  ): Promise<MaridPluginsMarketplaceMutationResult>;
  removePluginMarketplace(
    params: MaridAgentRemovePluginMarketplaceParams,
  ): Promise<MaridPluginsMarketplaceMutationResult>;
  updatePluginMarketplace(
    params: MaridAgentUpdatePluginMarketplaceParams,
  ): Promise<MaridPluginsMarketplaceMutationResult>;
  installPlugin(params: MaridAgentInstallPluginParams): Promise<MaridPluginsInstallResult>;
  cancelPluginOperation(
    params: MaridAgentCancelPluginOperationParams,
  ): Promise<MaridPluginsCancelOperationResult>;
  uninstallPlugin(params: MaridAgentUninstallPluginParams): Promise<MaridPluginsUninstallResult>;
  updatePlugin(params: MaridAgentUpdatePluginParams): Promise<MaridPluginsInstallResult>;
  restoreBuiltinPlugin(
    params: MaridAgentRestoreBuiltinPluginParams,
  ): Promise<MaridPluginsRestoreBuiltinResult>;
  configurePlugin(params: MaridAgentConfigurePluginParams): Promise<MaridPluginsConfigureResult>;
  resetPluginConfig(
    params: MaridAgentResetPluginConfigParams,
  ): Promise<MaridPluginsConfigureResult>;
  validatePlugin(params: MaridAgentValidatePluginParams): Promise<MaridPluginsValidateResult>;
  describePlugin(params: MaridAgentDescribePluginParams): Promise<MaridPluginsDescribeResult>;
  setPluginEnabled(params: MaridAgentSetPluginEnabledParams): Promise<MaridPluginsSetEnabledResult>;
  // ---- 定时任务(automation)管理 ----
  listAutomations(params: MaridAgentWorkspaceTarget): Promise<MaridAutomation[]>;
  listAllAutomations(): Promise<MaridAutomation[]>;
  createAutomation(params: MaridAgentCreateAutomationParams): Promise<MaridAutomation>;
  updateAutomation(params: MaridAgentUpdateAutomationParams): Promise<MaridAutomation | null>;
  deleteAutomation(params: MaridAgentAutomationIdParams): Promise<void>;
  setAutomationEnabled(params: MaridAgentSetAutomationEnabledParams): Promise<void>;
  restartAutomation(params: MaridAgentAutomationIdParams): Promise<void>;
  runAutomationNow(params: MaridAgentAutomationIdParams): Promise<MaridAgentRunAutomationNowResult>;
  listAutomationRuns(params: MaridAgentAutomationIdParams): Promise<MaridAutomationRun[]>;
  deleteAutomationRun(params: MaridAgentDeleteAutomationRunParams): Promise<void>;
  generateWorkspaceText(
    params: MaridAgentGenerateWorkspaceTextParams,
  ): Promise<MaridWorkspaceGenerateTextResult>;
  testModelConnectivity(
    params: MaridAgentTestModelConnectivityParams,
  ): Promise<MaridProviderTestModelConnectivityResult>;
  /**
   * @deprecated：send 主路径已收敛 v4 sendText 命令。仅剩两个消费点——
   * adapter 带附件输入回退（待附件命令面落地后移除）与 maridSessionService
   * pass-through；新代码禁止回用。
   */
  sendPrompt(params: MaridAgentSendPromptParams): Promise<MaridSessionSendResult>;
  compactSession(params: MaridAgentCompactParams): Promise<MaridSessionCompactResult>;
  goalSession(params: MaridAgentGoalParams): Promise<MaridSessionGoalResult>;
  closeSession(
    params: MaridAgentSessionTarget & { expectedPersistence?: "deferred" | "immediate" },
  ): Promise<boolean>;
  /**
   * 卸载 cwd 位于 directory 内的全部驻留会话（session/releaseUnderDirectory，docs/git/worktree.md §8.3）。
   * 会话选择与忙闲由 CLI 判断，任一会话忙时全部不卸载。只使用已存在的进程；没有进程时没有驻留会话。
   */
  releaseSessionsUnderDirectory?(
    params: MaridAgentWorkspaceTarget & { directory: string },
  ): Promise<{ released: string[]; busy: string[] }>;
  /**
   * 按 commandId 查询 CLI 对一条命令的裁决（commands/query）；Host 内部用于确认失败的会话命令
   * 是否实际执行（docs/git/worktree.md §3.5）。CLI 没有记录时返回 "unknown"。
   */
  queryCommandResult?(
    params: MaridAgentWorkspaceTarget & { key: CommandKey },
  ): Promise<CommandAck | "unknown">;
  setModel(params: MaridAgentSetModelParams): Promise<MaridSessionStateSnapshot>;
  setThoughtLevel(params: MaridAgentSetThoughtLevelParams): Promise<MaridSessionStateSnapshot>;
  setMode(params: MaridAgentSetModeParams): Promise<MaridSessionStateSnapshot>;
  respondProviderRuntimeHeaders(
    params: MaridAgentRespondProviderRuntimeHeadersParams,
  ): Promise<void>;
  respondSessionRuntimePreferences(
    params: MaridAgentRespondSessionRuntimePreferencesParams,
  ): Promise<void>;
  /**
   * v4 UI 的 Start Plan runtime-auth bridge 只需要本地 pending request 事件，
   * 不能为此额外打开旧 `session/subscribe`，否则会把旧 replay/continuous 读路径
   * 重新带回 v4 pane。
   */
  onDynamicProviderRuntimeHeadersRequest(
    params: MaridAgentSessionTarget,
  ): Event<MaridAgentProviderRuntimeHeadersRequest>;
  /**
   * Start Plan runtime headers 的 workspace 级订阅入口（生产应答方使用）。
   * Bug 原因（2026-08-16 web 远控卡死）：session 级入口只随桌面 pane 挂载订阅，
   * web 远控新建会话在桌面没有 pane、web 端无应答方，请求校验请求滞留 pending，
   * 任务永久「工作中」。桌面窗口用本入口为每个打开的 workspace tab 维持常驻应答方；
   * 订阅时先注册 live listener 再按 workspaceKey 前缀补投 pending，与 session 级同语义。
   */
  onDynamicWorkspaceProviderRuntimeHeadersRequest(
    params: MaridAgentWorkspaceTarget,
  ): Event<MaridAgentProviderRuntimeHeadersRequest>;
  /** 只投递本 workspace 的校验取消，不属于 conversation stream。 */
  onDynamicWorkspaceProviderRuntimeHeadersCancelled(
    params: MaridAgentWorkspaceTarget,
  ): Event<MaridProviderRuntimeHeadersCancelled>;
  onDynamicSessionRuntimePreferencesRequest(): Event<MaridAgentSessionRuntimePreferencesRequest>;
  /**
   * CLI 进程级资源样本，带 services 打的 lane 标签（CLI 自己不知道 lane）。
   * 使用 dynamic event 避免 RPC 服务在无人订阅时缓冲周期事件；
   * 该事件不属于 session/conversation continuous 或 replayable 状态。
   */
  onDynamicProcessResourceSample(): Event<AgentLaneResourceSample>;
  /** 插件启动超时的工作区提示；订阅时补投当前进程的降级事实。 */
  onDynamicPluginStartupDegraded(params: MaridAgentWorkspaceTarget): Event<void>;
  /** MCP 进程生命周期与低频内存事件，仅供可信 Host relay 上报 ARMS。 */
  /** 仅 trusted Host relay 订阅；手机统计不依赖 renderer 生命周期。 */
  onDynamicRemoteConversationTelemetry(): Event<RemoteConversationTelemetryEvent>;

  onDynamicMcpTelemetry(): Event<MaridMcpTelemetryEvent>;
  /** MCP 进程树资源事实，只供可信 Host 汇总上报。 */
  onDynamicMcpResourceSamples(): Event<MaridMcpResourceSample[]>;
  /** Bash 完成事实，仅可信 Host 资源旁路订阅。 */
  onDynamicToolExecResource(): Event<MaridToolExecResource>;
  /**
   * @deprecated 旧协议订阅面（session/subscribe + session/event + state.updated）。
   * task-index syncer 已迁 v4 sessions-index/workspace-config 帧；
   * 仅剩 maridTaskServiceAdapter.onDynamicTaskEvent（replayable 读路径）消费。
   * 写路径已收敛 v4 命令面；本订阅是读路径投影源。
   */
  onDynamicSessionEvent(params: MaridAgentSessionSubscribeParams): Event<MaridAgentServiceEvent>;
  // ── v4 conversation 通道（竖切）──
  /** RPC attachment 建立后先读取 host 可信 hello。 */
  helloConversationV4(): Promise<HelloMessage>;
  /** hello 校验后回送 clientHello；metadata 不能覆盖 connection mode/profile。 */
  initializeConversationV4(clientHello: ClientHello): Promise<void>;
  /** 仅供 trusted host relay/facade；terminal RPC caller 必须被 connection scope 拒绝。 */
  setConnectionFlowStateV4(params: MaridAgentConnectionFlowParams): Promise<void>;
  subscribeConversationV4(
    params: MaridAgentConversationSubscribeParams,
  ): Promise<V4ConversationSubscribeResult>;
  resyncConversationV4(
    params: MaridAgentConversationResyncParams,
  ): Promise<V4ConversationResyncResult>;
  unsubscribeConversationV4(params: MaridAgentConversationUnsubscribeParams): Promise<void>;
  /** rows/range 行分页 query（loadOlder 游标向上补历史）。 */
  conversationRowsRangeV4(
    params: MaridAgentConversationRowsRangeParams,
  ): Promise<V4ConversationRowsRangeResult>;
  conversationPlansV4(
    params: MaridAgentConversationPlansParams,
  ): Promise<V4ConversationPlansResult>;
  /** 输入预测：只读、无状态；Host 开关关闭 / 无运行时 / 失败一律回空结果，不抛错。 */
  conversationInputPredictionV4(
    params: MaridAgentConversationInputPredictionParams,
  ): Promise<V4ConversationInputPredictionResult>;
  /** workflow run 事件日志分页；与 plans 同族（只读、无状态、超时重发安全）。 */
  conversationWorkflowRunEventsV4(
    params: MaridAgentConversationWorkflowRunEventsParams,
  ): Promise<V4ConversationWorkflowRunEventsResult>;
  /** workflow run 枚举；journal-backed 的重启后发现面。 */
  conversationWorkflowRunsV4(
    params: MaridAgentConversationWorkflowRunsParams,
  ): Promise<V4ConversationWorkflowRunsResult>;
  /** workflow run 的用户面产物清单；与 plans 同族（只读、无状态、超时重发安全）。 */
  conversationWorkflowRunArtifactsV4(
    params: MaridAgentConversationWorkflowRunArtifactsParams,
  ): Promise<V4ConversationWorkflowRunArtifactsResult>;
  /** 预置看板的条目分页；hook 以 itemCount 变化为信号增量拉取。 */
  conversationWorkflowRunArtifactDataV4(
    params: MaridAgentConversationWorkflowRunArtifactDataParams,
  ): Promise<V4ConversationWorkflowRunArtifactDataResult>;
  /** 内容产物的字节，一次一块；授权在 CLI 侧（journal 行才是取字节的依据）。 */
  conversationWorkflowRunArtifactReadV4(
    params: MaridAgentConversationWorkflowRunArtifactReadParams,
  ): Promise<V4ConversationWorkflowRunArtifactReadResult>;
  /** dwf 工作区 transcript 的清单。 */
  conversationWorkflowRunWorkspaceV4(
    params: MaridAgentConversationWorkflowRunWorkspaceParams,
  ): Promise<V4ConversationWorkflowRunWorkspaceResult>;
  /** 一个工作区节点的有界正文。 */
  conversationWorkflowRunNodeResultV4(
    params: MaridAgentConversationWorkflowRunNodeResultParams,
  ): Promise<V4ConversationWorkflowRunNodeResultResult>;
  backgroundBashOutputV4(
    params: MaridAgentBackgroundBashOutputParams,
  ): Promise<BackgroundBashOutputResult>;
  conversationFileChangesV4(
    params: MaridAgentConversationFileChangesParams,
  ): Promise<V4ConversationFileChangesResult>;
  conversationFileRewindPreviewV4(
    params: MaridAgentConversationFileRewindPreviewParams,
  ): Promise<V4ConversationFileRewindPreviewResult>;
  sendConversationCommandV4(params: MaridAgentConversationCommandParams): Promise<CommandAck>;
  queryConversationCommandsV4(params: MaridAgentCommandsQueryParams): Promise<CommandsQueryResult>;
  attachmentBeginV4(params: MaridAgentAttachmentBeginParams): Promise<V4AttachmentBeginResult>;
  attachmentChunkV4(params: MaridAgentAttachmentChunkParams): Promise<V4AttachmentChunkResult>;
  attachmentCommitV4(params: MaridAgentAttachmentTerminalParams): Promise<V4AttachmentCommitResult>;
  attachmentAbortV4(params: MaridAgentAttachmentTerminalParams): Promise<void>;
  /** Desktop local 已发送视频 source query；远端与 Web 返回 chunked。 */
  attachmentPreviewSourceV4(
    params: MaridAgentAttachmentPreviewSourceParams,
  ): Promise<V4AttachmentPreviewSourceResult>;
  /** 已发送 image/video 只读分块查询；connection scope 注入可信 workspace 连接。 */
  attachmentReadV4(params: MaridAgentAttachmentReadParams): Promise<V4AttachmentReadResult>;
  /** Share 读取 userInput 附件，允许 text/plain 等非媒体类型。 */
  conversationAttachmentReadV4(
    params: MaridAgentConversationAttachmentReadParams,
  ): Promise<V4ConversationAttachmentReadResult>;
  /** Share 选择阶段只读 userInput 附件元数据，不读取完整内容。 */
  conversationAttachmentStatV4(
    params: MaridAgentConversationAttachmentStatParams,
  ): Promise<V4ConversationAttachmentStatResult>;
  getSessionLifecycle(
    params: MaridAgentSessionTarget,
  ): Promise<import("@marid/shared").SessionLifecycleSnapshot | null>;
  ensureSessionReady(params: MaridAgentSessionTarget): Promise<void>;
  stopSessionRuntime(params: MaridAgentSessionTarget): Promise<void>;
  restartSessionRuntime(params: MaridAgentSessionTarget): Promise<void>;
  /** 独立于可被释放的 conversation publisher 的生命周期事实。 */
  onDynamicSessionLifecycle(
    params: MaridAgentWorkspaceTarget,
  ): Event<import("@marid/shared").SessionLifecycleSnapshot>;
  /** workspace 级下行帧流（v4/conversation/frame），renderer 侧按 topic 自行路由。 */
  onDynamicConversationFrame(
    params: MaridAgentWorkspaceTarget,
  ): Event<ConversationTopicWireCandidate>;
  /** workspace 级 live telemetry 事实；connection facade 仅向可信 desktop-continuous 下游暴露。 */
  onDynamicLocalTtftFacts(
    params: MaridAgentWorkspaceTarget,
  ): Event<import("@marid/shared").LocalTtftFacts>;
  onDynamicConversationTelemetryFact(
    params: MaridAgentWorkspaceTarget,
  ): Event<ConversationTelemetryFact>;
  /**
   * context 组成（apps/marid-cli/docs/design/v2/model/context-composition.md §3.6）。
   * existing-only：工作区没有运行中的 CLI、会话不在内存、旧 CLI（method-not-found）都返回 null，不启动 CLI。
   */
  getContextComposition?(
    params: MaridAgentContextCompositionParams,
  ): Promise<ContextComposition | null>;
  // 注意：读本插件 user 层设置（含敏感值）的 readPluginUserOptions 故意不在本接口上。本服务整个
  // MaridAgent 频道经 RPC 暴露（桌面 Renderer；手机远控 allowlist 也含它），挂在这里就会被任意客户端调到。
  // Window Host 经 node.ts 的 getPluginUserOptionsReader(services) 在进程内取得（见 pluginHostQueriesClient.ts）。
  /**
   * 输入框 `@` 面板 `tool` target 的候选项（docs/plugins/extension-host.md §16.6）。existing-only：
   * 没有运行中的 CLI 抛 runtime-unavailable；会话不在 CLI 内存抛 -32004；其他失败的 `data.code` 是
   * `plugin_mention_*`。工具调用走会话的 MCP App 授权链，首次可能在会话里等待用户审批。
   * 旧 CLI（method-not-found）→ `{ supported: false }`。不设计取消，调用方只采用最新一次结果。
   */
  getPluginMentionItems?(
    params: MaridAgentPluginMentionItemsParams,
  ): Promise<MaridAgentPluginMentionItemsResult>;
  /** context 组成变更：只带修订号，live-only；收到后按需调 getContextComposition。 */
  onDynamicContextCompositionChanged?(
    target: MaridAgentWorkspaceTarget,
  ): Event<ContextCompositionChangedEvent>;
  /**
   * 模型请求指标（docs/monitoring/conversation-telemetry-v4.md「模型请求指标」）：每个完成的请求一条，
   * 带 usage 与首内容时间；按 querySource 区分主轮与旁路。旧 CLI 不发。
   */
  onDynamicModelRequestMetrics?(target: MaridAgentWorkspaceTarget): Event<ModelRequestMetricsFact>;
  /** Host 消费驻留 CLI 的闲时生命周期；不参与聊天快照恢复。 */
  onDynamicOffPeakLifecycle(
    params: MaridAgentWorkspaceTarget,
  ): Event<import("@marid/shared").OffPeakLifecycleNotification>;
  controlOffPeakExecutionV4(
    params: MaridAgentWorkspaceTarget & { control: import("@marid/shared").OffPeakControlParams },
  ): Promise<import("@marid/shared").OffPeakControlResult>;
  /** 当前窗口全部本地 live task 的 CUA 权限观察；历史、远程与 replayable 不在此事件面。 */
  onDynamicCuaPermissionObservation(): Event<MaridAgentCuaPermissionObservation>;
  /** 本地桌面窗口创建插件页面，不依赖 conversation 订阅。 */
  onDynamicPluginUiToolResult(): Event<
    PluginUiToolResultNotification & { workspacePath: string; workspaceIdentity?: string }
  >;
  /** 只读 CLI 持久事实；不恢复会话运行实例。旧远端可以不提供此能力。 */
  reconcileSessionsIndexV4?(
    params: MaridAgentWorkspaceTarget & { sessionIds: string[] },
  ): Promise<SessionsIndexReconcileResult>;
  // ── M5 sessions-index 通道（列表活性）──
  subscribeSessionsIndexV4(
    params: MaridAgentSessionsIndexSubscribeParams,
  ): Promise<V4SessionsIndexSubscribeResult>;
  resyncSessionsIndexV4(
    params: MaridAgentConversationResyncParams,
  ): Promise<V4ConversationResyncResult>;
  unsubscribeSessionsIndexV4(params: MaridAgentConversationUnsubscribeParams): Promise<void>;
  /** workspace 级 sessions-index 下行帧流（与 conversation 同一通知，按 topic 前缀分流）。 */
  onDynamicSessionsIndexFrame(
    params: MaridAgentWorkspaceTarget,
  ): Event<SessionsIndexTopicWireCandidate>;
  // ── workspace-config 通道（配置目录活性；task-index syncer 消费）──
  subscribeWorkspaceConfigV4(
    params: MaridAgentWorkspaceConfigSubscribeParams,
  ): Promise<V4WorkspaceConfigSubscribeResult>;
  resyncWorkspaceConfigV4(
    params: MaridAgentConversationResyncParams,
  ): Promise<V4ConversationResyncResult>;
  unsubscribeWorkspaceConfigV4(params: MaridAgentConversationUnsubscribeParams): Promise<void>;
  /** workspace 级 workspace-config 下行帧流（与 conversation 同一通知，按 topic 前缀分流）。 */
  onDynamicWorkspaceConfigFrame(
    params: MaridAgentWorkspaceTarget,
  ): Event<WorkspaceConfigTopicWireCandidate>;
  /**
   * （CLI 重连重订）：agent 进程换代通知（超时回收/崩溃后重新拉起）。
   * v4 订阅活在 CLI 进程内存，进程换代即失效；订阅方（task-index syncer 等）
   * 收到后必须对该 workspaceKey 重发 subscribe，否则帧流静默中断。
   */
  onAgentRuntimeRestarted(listener: (event: { workspaceKey: string }) => void): IDisposable;
  /**
   * Agent client 在 service 内完成登记后发布 available，当前 client 关闭后发布 unavailable。
   * 这是被动 observer attach/detach 的唯一生命周期信号，不表达用户使用租约。
   */
  onAgentRuntimeLifecycle?: (
    listener: (event: MaridAgentRuntimeLifecycleEvent) => void,
  ) => IDisposable;
  /** 当前 runtime 进程意外退出；界面据此提示原因并提供重新连接。 */
  onAgentRuntimeExited?: (listener: (event: MaridAgentRuntimeExitedEvent) => void) => IDisposable;
  /** 当前 desktop-local CUA turn 是否仍在执行，用于 Helper recovery 避免中途回收 Agent。 */
  hasActiveCuaOperationTurn(): boolean;
  disposeWorkspace(
    params: MaridAgentWorkspaceTarget & { releasePreparation?: boolean },
  ): Promise<void>;
  disposeAll(): void;
}

export const IMaridAgentService = createServiceDescriptor<IMaridAgentService>(
  ServiceChannels.MaridAgent,
);
