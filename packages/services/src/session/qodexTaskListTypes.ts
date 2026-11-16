import type { WorkspacePurpose, QodexTaskMeta } from "@qodex/shared";

export type QodexTaskListKind = "pinned" | "archived" | "timeline" | "active";
export type QodexTaskListSortBy = "created" | "updated";

export interface QodexTaskListWorkspaceScope {
  workspacePath: string;
  workspaceIdentity?: string;
  workspacePurpose?: WorkspacePurpose;
}

export interface QodexTaskListQuery {
  kind: QodexTaskListKind | "all";
  /** host 由存储 owner 确定全部范围，不接受 UI 已打开工作区作为历史目录。 */
  workspaceScopes: QodexTaskListWorkspaceScope[] | "host";
  sortBy: QodexTaskListSortBy;
  search?: string;
  limit?: number;
  /** 只返回这些会话（例如分享管理按来源会话 ID 解析跳转目标）；空数组返回空结果。 */
  taskIds?: readonly string[];
}

export type QodexTaskListItem = QodexTaskMeta & {
  searchSnippet?: string;
  searchSnippets?: string[];
};

export interface QodexTaskListResult {
  items: QodexTaskListItem[];
  total: number;
  hasMore: boolean;
}

export interface QodexWorkspaceEventSubscriptionParams {
  workspacePath: string;
  workspaceIdentity?: string;
}
