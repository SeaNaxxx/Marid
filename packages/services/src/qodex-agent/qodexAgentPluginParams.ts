import type {
  ModelSelection,
  QodexAgentMcpServer,
  QodexAutomationScheduleRule,
  QodexMcpListMode,
} from "@qodex/shared";
import type { McpAppsAppToolCallResult, McpAppsAppToolDescriptor } from "@qodex/shared/mcp-apps";

export interface QodexAgentWorkspaceTarget {
  workspacePath: string;
  workspaceIdentity?: string;
  /** 远程 workspace 的运行时会话身份；只用于隔离/路由，不能替代 workspacePath。 */
  remoteSessionId?: string;
}

export interface QodexAgentPluginMutationTarget extends QodexAgentWorkspaceTarget {
  /** 仅 Host 观测使用；不会传给 CLI。 */
  telemetry?: {
    entry?:
      | "store_list"
      | "store_detail"
      | "side_pane_market"
      | "recommended_prompt"
      | "recommended_ui"
      | "component_settings";
    client_kind?: "desktop" | "mobile";
    trigger?: "user" | "automatic" | "dependency" | "restore" | "sync";
  };
}

export interface QodexAgentPluginViewParams extends QodexAgentWorkspaceTarget {
  configScope?: "user" | "workspace";
}

export interface QodexAgentListMcpServerStatusesParams extends QodexAgentWorkspaceTarget {
  mcpServers?: QodexAgentMcpServer[];
  mode?: QodexMcpListMode;
}

export interface QodexAgentAddPluginMarketplaceParams extends QodexAgentPluginMutationTarget {
  dryRun?: boolean;
  operationId?: string;
  source: string;
}

export interface QodexAgentRemovePluginMarketplaceParams extends QodexAgentPluginMutationTarget {
  marketplace: string;
}

export interface QodexAgentUpdatePluginMarketplaceParams extends QodexAgentPluginMutationTarget {
  marketplace?: string;
  operationId?: string;
}

export interface QodexAgentInstallPluginParams extends QodexAgentPluginMutationTarget {
  dryRun?: boolean;
  marketplace: string;
  operationId?: string;
  pluginName: string;
  scope?: "user" | "workspace";
}

export interface QodexAgentCancelPluginOperationParams {
  operationId: string;
}

export interface QodexAgentUninstallPluginParams extends QodexAgentPluginMutationTarget {
  marketplace?: string;
  pluginId?: string;
  pluginName?: string;
  removeCache?: boolean;
}

export interface QodexAgentUpdatePluginParams extends QodexAgentPluginMutationTarget {
  pluginId?: string;
  marketplace?: string;
}

export interface QodexAgentRestoreBuiltinPluginParams extends QodexAgentPluginMutationTarget {
  pluginId: string;
}

export interface QodexAgentConfigurePluginParams extends QodexAgentPluginMutationTarget {
  clearOptionKeys?: string[];
  dryRun?: boolean;
  options: Record<string, unknown>;
  pluginId: string;
  scope?: "user" | "workspace";
}

export interface QodexAgentResetPluginConfigParams extends QodexAgentPluginMutationTarget {
  pluginId: string;
  scope?: "user" | "workspace";
}

export interface QodexAgentValidatePluginParams extends QodexAgentWorkspaceTarget {
  marketplace?: string;
  pluginName?: string;
  source?: string;
}

export interface QodexAgentDescribePluginParams extends QodexAgentWorkspaceTarget {
  marketplace: string;
  pluginName: string;
}

export interface QodexAgentSetPluginEnabledParams extends QodexAgentPluginMutationTarget {
  enabled: boolean;
  operationId?: string;
  pluginId: string;
  scope?: "user" | "workspace";
}

// Plugin 对话引用 catalog：
// 带 sessionId → session-owned 冻结 catalog（必须路由到持有该 session 的 workspace client）；
// 不带 → workspace 当前 catalog（新建草稿 Picker）。
export interface QodexAgentPluginReferenceCatalogParams extends QodexAgentWorkspaceTarget {
  sessionId?: string;
}

// Composer Skill catalog：与 Plugin 引用相同，以 sessionId 区分 workspace 当前目录和
// resident Session runtime 快照；不参与 Settings 管理目录。
export interface QodexAgentSkillReferenceCatalogParams extends QodexAgentWorkspaceTarget {
  sessionId?: string;
}

/** 插件 UI：UI 发起的 `ui://` 资源读取。 */
export interface QodexAgentReadMcpResourceParams extends QodexAgentWorkspaceTarget {
  instance: import("@qodex/shared/mcp-apps").McpAppInstance;
  sessionId: string;
  pluginId: string;
  serverName: string;
  uri: string;
}

/** 插件 UI 页面发起的 `resources/read`；参数与 readMcpResource 同形，结果受 8 MiB 与 mimeType 白名单约束。 */
export type QodexAgentReadMcpResourceForUiParams = QodexAgentReadMcpResourceParams;

/** 插件 UI：UI 回调本插件工具；不进审批流，归属由 agent 侧 catalog fail closed。 */
export interface QodexAgentCallMcpToolForUiParams extends QodexAgentWorkspaceTarget {
  instance: import("@qodex/shared/mcp-apps").McpAppInstance;
  sessionId: string;
  pluginId: string;
  serverName: string;
  toolName: string;
  arguments?: Record<string, unknown>;
  /** 宿主生成的调用 id，配合 cancelMcpToolCallForUi。 */
  callId: string;
}
/** 插件 UI 页面发起的 resources/list、resources/templates/list、subscribe、unsubscribe。 */
export interface QodexAgentListMcpResourcesForUiParams extends QodexAgentWorkspaceTarget {
  instance: import("@qodex/shared/mcp-apps").McpAppInstance;
  sessionId: string;
  pluginId: string;
  serverName: string;
  cursor?: string;
}
/** App-Provided Tools：实例身份 = 会话 + 沙箱作用域 + 代际。 */
export interface QodexAgentAppToolInstanceForUiParams extends QodexAgentWorkspaceTarget {
  instance: import("@qodex/shared/mcp-apps").McpAppInstance;
  sessionId: string;
  pluginId: string;
  serverName: string;
  scopeId: string;
  generation: number;
}
export interface QodexAgentRegisterAppToolsForUiParams extends QodexAgentAppToolInstanceForUiParams {
  tools: McpAppsAppToolDescriptor[];
}
export interface QodexAgentAppToolCallForUiParams extends QodexAgentAppToolInstanceForUiParams {
  callId: string;
}
export interface QodexAgentResolveAppToolCallForUiParams extends QodexAgentAppToolCallForUiParams {
  result?: McpAppsAppToolCallResult;
  error?: { message: string };
}
export interface QodexAgentMcpResourceSubscriptionForUiParams extends QodexAgentWorkspaceTarget {
  instance: import("@qodex/shared/mcp-apps").McpAppInstance;
  sessionId: string;
  pluginId: string;
  serverName: string;
  scopeId: string;
  generation: number;
  uri: string;
}
export interface QodexAgentCancelMcpToolCallForUiParams extends QodexAgentWorkspaceTarget {
  instance: import("@qodex/shared/mcp-apps").McpAppInstance;
  sessionId: string;
  pluginId: string;
  serverName: string;
  callId: string;
}
export interface QodexAgentResolveSuggestedPluginReferenceParams extends QodexAgentWorkspaceTarget {
  stableId: string;
  operationId: string;
  clientMode: "desktop-continuous" | "web-remote-replayable";
  deliveryKind: "desktop-continuous" | "web-remote-replayable";
}

// ---- 定时任务(automation)管理参数 ----

export interface QodexAgentCreateAutomationParams extends QodexAgentWorkspaceTarget {
  projectId?: string | null;
  title: string;
  cronExpr: string;
  relativeDelayMinutes?: number;
  prompt: string;
  modelSelection?: ModelSelection;
  mode?: string;
  recurring?: boolean;
  maxRuns?: number;
  endAt?: number;
  scheduleRule?: QodexAutomationScheduleRule;
}

export interface QodexAgentUpdateAutomationParams extends QodexAgentWorkspaceTarget {
  automationId: string;
  title?: string;
  cronExpr?: string;
  prompt?: string;
  modelSelection?: ModelSelection | null;
  mode?: string | null;
  recurring?: boolean;
  maxRuns?: number | null;
  endAt?: number | null;
  scheduleRule?: QodexAutomationScheduleRule | null;
  scheduleEditedByUser?: boolean;
}

export interface QodexAgentAutomationIdParams extends QodexAgentWorkspaceTarget {
  automationId: string;
}

export interface QodexAgentSetAutomationEnabledParams extends QodexAgentWorkspaceTarget {
  automationId: string;
  enabled: boolean;
}

export interface QodexAgentDeleteAutomationRunParams extends QodexAgentWorkspaceTarget {
  runId: string;
}
