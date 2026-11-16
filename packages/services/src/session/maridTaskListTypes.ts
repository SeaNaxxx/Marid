import type { WorkspacePurpose, MaridTaskMeta } from "@marid/shared";

export type MaridTaskListKind = "pinned" | "archived" | "timeline" | "active";
export type MaridTaskListSortBy = "created" | "updated";

export interface MaridTaskListWorkspaceScope {
  workspacePath: string;
  workspaceIdentity?: string;
  workspacePurpose?: WorkspacePurpose;
}

export interface MaridTaskListQuery {
  kind: MaridTaskListKind | "all";
  /** host 由存储 owner 确定全部范围，不接受 UI 已打开工作区作为历史目录。 */
  workspaceScopes: MaridTaskListWorkspaceScope[] | "host";
  sortBy: MaridTaskListSortBy;
  search?: string;
  limit?: number;
  /** 只返回这些会话（例如分享管理按来源会话 ID 解析跳转目标）；空数组返回空结果。 */
  taskIds?: readonly string[];
}

export type MaridTaskListItem = MaridTaskMeta & {
  searchSnippet?: string;
  searchSnippets?: string[];
};

export interface MaridTaskListResult {
  items: MaridTaskListItem[];
  total: number;
  hasMore: boolean;
}

export interface MaridWorkspaceEventSubscriptionParams {
  workspacePath: string;
  workspaceIdentity?: string;
}
