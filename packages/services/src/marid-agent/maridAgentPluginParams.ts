import type {
  ModelSelection,
  MaridAgentMcpServer,
  MaridAutomationScheduleRule,
  MaridMcpListMode,
} from "@marid/shared";
import type { McpAppsAppToolCallResult, McpAppsAppToolDescriptor } from "@marid/shared/mcp-apps";

export interface MaridAgentWorkspaceTarget {
  workspacePath: string;
  workspaceIdentity?: string;
  /** 远程 workspace 的运行时会话身份；只用于隔离/路由，不能替代 workspacePath。 */
  remoteSessionId?: string;
}

export interface MaridAgentPluginMutationTarget extends MaridAgentWorkspaceTarget {
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

export interface MaridAgentPluginViewParams extends MaridAgentWorkspaceTarget {
  configScope?: "user" | "workspace";
}

export interface MaridAgentListMcpServerStatusesParams extends MaridAgentWorkspaceTarget {
  mcpServers?: MaridAgentMcpServer[];
  mode?: MaridMcpListMode;
}

export interface MaridAgentAddPluginMarketplaceParams extends MaridAgentPluginMutationTarget {
  dryRun?: boolean;
  operationId?: string;
  source: string;
}

export interface MaridAgentRemovePluginMarketplaceParams extends MaridAgentPluginMutationTarget {
  marketplace: string;
}

export interface MaridAgentUpdatePluginMarketplaceParams extends MaridAgentPluginMutationTarget {
  marketplace?: string;
  operationId?: string;
}

export interface MaridAgentInstallPluginParams extends MaridAgentPluginMutationTarget {
  dryRun?: boolean;
  marketplace: string;
  operationId?: string;
  pluginName: string;
  scope?: "user" | "workspace";
}

export interface MaridAgentCancelPluginOperationParams {
  operationId: string;
}

export interface MaridAgentUninstallPluginParams extends MaridAgentPluginMutationTarget {
  marketplace?: string;
  pluginId?: string;
  pluginName?: string;
  removeCache?: boolean;
}

export interface MaridAgentUpdatePluginParams extends MaridAgentPluginMutationTarget {
  pluginId?: string;
  marketplace?: string;
}

export interface MaridAgentRestoreBuiltinPluginParams extends MaridAgentPluginMutationTarget {
  pluginId: string;
}

export interface MaridAgentConfigurePluginParams extends MaridAgentPluginMutationTarget {
  clearOptionKeys?: string[];
  dryRun?: boolean;
  options: Record<string, unknown>;
  pluginId: string;
  scope?: "user" | "workspace";
}

export interface MaridAgentResetPluginConfigParams extends MaridAgentPluginMutationTarget {
  pluginId: string;
  scope?: "user" | "workspace";
}

export interface MaridAgentValidatePluginParams extends MaridAgentWorkspaceTarget {
  marketplace?: string;
  pluginName?: string;
  source?: string;
}

export interface MaridAgentDescribePluginParams extends MaridAgentWorkspaceTarget {
  marketplace: string;
  pluginName: string;
}

export interface MaridAgentSetPluginEnabledParams extends MaridAgentPluginMutationTarget {
  enabled: boolean;
  operationId?: string;
  pluginId: string;
  scope?: "user" | "workspace";
}

// Plugin 对话引用 catalog：
// 带 sessionId → session-owned 冻结 catalog（必须路由到持有该 session 的 workspace client）；
// 不带 → workspace 当前 catalog（新建草稿 Picker）。
export interface MaridAgentPluginReferenceCatalogParams extends MaridAgentWorkspaceTarget {
  sessionId?: string;
}

// Composer Skill catalog：与 Plugin 引用相同，以 sessionId 区分 workspace 当前目录和
// resident Session runtime 快照；不参与 Settings 管理目录。
export interface MaridAgentSkillReferenceCatalogParams extends MaridAgentWorkspaceTarget {
  sessionId?: string;
}

/** 插件 UI：UI 发起的 `ui://` 资源读取。 */
export interface MaridAgentReadMcpResourceParams extends MaridAgentWorkspaceTarget {
  instance: import("@marid/shared/mcp-apps").McpAppInstance;
  sessionId: string;
  pluginId: string;
  serverName: string;
  uri: string;
}

/** 插件 UI 页面发起的 `resources/read`；参数与 readMcpResource 同形，结果受 8 MiB 与 mimeType 白名单约束。 */
export type MaridAgentReadMcpResourceForUiParams = MaridAgentReadMcpResourceParams;

/** 插件 UI：UI 回调本插件工具；不进审批流，归属由 agent 侧 catalog fail closed。 */
export interface MaridAgentCallMcpToolForUiParams extends MaridAgentWorkspaceTarget {
  instance: import("@marid/shared/mcp-apps").McpAppInstance;
  sessionId: string;
  pluginId: string;
  serverName: string;
  toolName: string;
  arguments?: Record<string, unknown>;
  /** 宿主生成的调用 id，配合 cancelMcpToolCallForUi。 */
  callId: string;
}
/** 插件 UI 页面发起的 resources/list、resources/templates/list、subscribe、unsubscribe。 */
export interface MaridAgentListMcpResourcesForUiParams extends MaridAgentWorkspaceTarget {
  instance: import("@marid/shared/mcp-apps").McpAppInstance;
  sessionId: string;
  pluginId: string;
  serverName: string;
  cursor?: string;
}
/** App-Provided Tools：实例身份 = 会话 + 沙箱作用域 + 代际。 */
export interface MaridAgentAppToolInstanceForUiParams extends MaridAgentWorkspaceTarget {
  instance: import("@marid/shared/mcp-apps").McpAppInstance;
  sessionId: string;
  pluginId: string;
  serverName: string;
  scopeId: string;
  generation: number;
}
export interface MaridAgentRegisterAppToolsForUiParams extends MaridAgentAppToolInstanceForUiParams {
  tools: McpAppsAppToolDescriptor[];
}
export interface MaridAgentAppToolCallForUiParams extends MaridAgentAppToolInstanceForUiParams {
  callId: string;
}
export interface MaridAgentResolveAppToolCallForUiParams extends MaridAgentAppToolCallForUiParams {
  result?: McpAppsAppToolCallResult;
  error?: { message: string };
}
export interface MaridAgentMcpResourceSubscriptionForUiParams extends MaridAgentWorkspaceTarget {
  instance: import("@marid/shared/mcp-apps").McpAppInstance;
  sessionId: string;
  pluginId: string;
  serverName: string;
  scopeId: string;
  generation: number;
  uri: string;
}
export interface MaridAgentCancelMcpToolCallForUiParams extends MaridAgentWorkspaceTarget {
  instance: import("@marid/shared/mcp-apps").McpAppInstance;
  sessionId: string;
  pluginId: string;
  serverName: string;
  callId: string;
}
export interface MaridAgentResolveSuggestedPluginReferenceParams extends MaridAgentWorkspaceTarget {
  stableId: string;
  operationId: string;
  clientMode: "desktop-continuous" | "web-remote-replayable";
  deliveryKind: "desktop-continuous" | "web-remote-replayable";
}

// ---- 定时任务(automation)管理参数 ----

export interface MaridAgentCreateAutomationParams extends MaridAgentWorkspaceTarget {
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
  scheduleRule?: MaridAutomationScheduleRule;
}

export interface MaridAgentUpdateAutomationParams extends MaridAgentWorkspaceTarget {
  automationId: string;
  title?: string;
  cronExpr?: string;
  prompt?: string;
  modelSelection?: ModelSelection | null;
  mode?: string | null;
  recurring?: boolean;
  maxRuns?: number | null;
  endAt?: number | null;
  scheduleRule?: MaridAutomationScheduleRule | null;
  scheduleEditedByUser?: boolean;
}

export interface MaridAgentAutomationIdParams extends MaridAgentWorkspaceTarget {
  automationId: string;
}

export interface MaridAgentSetAutomationEnabledParams extends MaridAgentWorkspaceTarget {
  automationId: string;
  enabled: boolean;
}

export interface MaridAgentDeleteAutomationRunParams extends MaridAgentWorkspaceTarget {
  runId: string;
}
