import type {
  MaridTaskMeta,
  MaridProvider,
  MaridTaskChangeSummary,
  EditorInfo,
  GitRepositorySummary,
  RemoteTarget,
  UserInfo,
} from "@marid/shared";

export interface WorkspaceHeaderState {
  selectedProvider: MaridProvider;
}

export type WorkspaceHeaderVariant = "task" | "draft";

export interface WorkspaceHeaderReloadSessionOptions {
  resumeTaskId?: string | null;
  provider?: MaridProvider | null;
}

export interface WorkspaceHeaderTitleSectionProps {
  variant?: WorkspaceHeaderVariant;
  readOnlyReason?: string;
  workspaceAbsPath: string;
  remoteSessionId?: string;
  workspaceIdentity?: string;
  remoteTarget?: RemoteTarget;
  localWorkspacePath?: string;
  projectName: string;
  activeTaskTitle: string;
  activeTaskChangeSummary?: MaridTaskChangeSummary | null;
  activeTaskId: string | null;
  activeTraceId: string | null;
  activeSessionId: string | null;
  activeTaskProvider: MaridProvider | null;
  resolvedActiveTaskMeta?: MaridTaskMeta | null;
  gitSummary: GitRepositorySummary;
  gitDirtyFileCount: number;
  sessionLogPath: string | null;
  nativeSessionLogProvider: MaridProvider | null;
  nativeSessionLogPath: string | null;
  nativeSessionLogExists: boolean;
  nativeSessionLogLoading: boolean;
  providerWorkspaceConfigPath: string | null;
  providerWorkspaceConfigExists: boolean;
  providerWorkspaceConfigLoading: boolean;
  onReloadSession?: (options?: WorkspaceHeaderReloadSessionOptions) => void | Promise<void>;
  reloadSessionDisabled?: boolean;
  reloadSessionPending?: boolean;
  onRefreshGit: () => void;
  workspaceHeaderState: WorkspaceHeaderState;
  isMacDesktop?: boolean;
  isMacFullscreen?: boolean;
  isWindowsDesktop?: boolean;
  isWebRemoteControl?: boolean;
  simplifyForNarrowRemote?: boolean;
  selectedEditor: EditorInfo | null;
  compact?: boolean;
}

export interface WorkspaceHeaderActionSectionProps {
  variant?: WorkspaceHeaderVariant;
  activeTaskId?: string | null;
  user?: UserInfo | null;
  readOnlyReason?: string;
  workspaceAbsPath: string;
  workspaceIdentity?: string;
  remoteSessionId?: string;
  remoteTarget?: RemoteTarget;
  isWebRemoteControl?: boolean;
  isDesktop?: boolean;
  isTerminalOpen: boolean;
  isSidePaneOpen: boolean;
  onToggleTerminal: () => void;
  onToggleSidePane: () => void;
  toggleSidePaneShortcutLabel?: string;
  onSelectedEditorChange?: (editor: EditorInfo | null) => void;
  simplifyForNarrowRemote?: boolean;
  hideHelpMenu?: boolean;
  showWindowControls?: boolean;
  useWindowsCaptionSpacing?: boolean;
}
