import type { TuiReadClipboardImage, TuiWriteClipboardText } from "@qodex/tui";
import type { UiLocale } from "@qodex/i18n";
import type { Logger } from "@qodex/contracts";
import type {
  createManagedCdpBrowserRuntime,
  ManagedCdpBrowserRuntimeOptions,
} from "@qodex/adapters/browser";
import type {
  createDefaultProviderEndpointRoutingPort,
  createModelAdapter,
  createQodexApp,
  CreateDefaultProviderEndpointRoutingPortOptions,
  CreateModelAdapterOptions,
  configureCodingPlanApiKey,
  ConfigureCodingPlanApiKeyOptions,
  inspectQodexSkill,
  inspectWorkspaceHookTrust,
  grantWorkspaceHookTrust,
  revokeWorkspaceHookTrustCli,
  inspectQodexCustomCommand,
  InspectQodexCustomCommandOptions,
  InspectQodexSkillOptions,
  listQodexCustomCommands,
  ListQodexCustomCommandsOptions,
  loadQodexCustomCommand,
  listQodexSessions,
  listQodexSkills,
  ListQodexSessionsOptions,
  ListQodexSkillsOptions,
  resolveLatestSession,
  ResolveLatestSessionOptions,
  RunQodexProtocolAgentOptions,
  prepareQodexTelemetryEnv,
  startProcessProviderRegistryRuntime,
  shutdownQodexTelemetry,
  QodexAppOptions,
} from "@qodex/bootstrap";
import type { CliEnv, DotenvLoadResult, LoadCliDotenvOptions } from "./env.js";
import type { PluginsCommandOverrides } from "./plugins-command.js";
import type { CliShutdownProcess } from "./shutdown.js";
import type { resolveWorkspaceGitBranch } from "./tui-workspace-git.js";

export type BootstrapModule = typeof import("@qodex/bootstrap");

export interface RunDependencies extends PluginsCommandOverrides {
  migrateStorage?: () => Promise<import("@qodex/shared/sqlite-migrations").MigrationResult>;
  protocolLifecycle?: RunQodexProtocolAgentOptions["lifecycle"];
  protocolInput?: NodeJS.ReadableStream;
  createManagedCdpBrowserRuntime?: (
    options?: ManagedCdpBrowserRuntimeOptions,
  ) => ReturnType<typeof createManagedCdpBrowserRuntime>;
  createModelAdapter?: (
    options?: CreateModelAdapterOptions,
  ) => ReturnType<typeof createModelAdapter>;
  createProviderEndpointRoutingPort?: (
    options: CreateDefaultProviderEndpointRoutingPortOptions,
  ) => ReturnType<typeof createDefaultProviderEndpointRoutingPort>;
  createQodexApp?: (
    options?: QodexAppOptions,
  ) => Awaited<ReturnType<typeof createQodexApp>> | ReturnType<typeof createQodexApp>;
  /**
   * Session-event shaper for --output-format stream-json. Defaults to the
   * bootstrap module's, which is also what the protocol server uses; injectable
   * so a caller that supplies its own `createQodexApp` (tests, embedders) can
   * still stream, since the bootstrap module is not loaded on that path.
   */
  mapSessionEvent?: BootstrapModule["mapSessionEvent"];
  cwd?: () => string;
  env?: CliEnv;
  inspectSkill?: (options: InspectQodexSkillOptions) => ReturnType<typeof inspectQodexSkill>;
  inspectWorkspaceHookTrust?: typeof inspectWorkspaceHookTrust;
  grantWorkspaceHookTrust?: typeof grantWorkspaceHookTrust;
  revokeWorkspaceHookTrustCli?: typeof revokeWorkspaceHookTrustCli;
  inspectCustomCommand?: (
    options: InspectQodexCustomCommandOptions,
  ) => ReturnType<typeof inspectQodexCustomCommand>;
  configureCodingPlanApiKey?: (
    options: ConfigureCodingPlanApiKeyOptions,
  ) => ReturnType<typeof configureCodingPlanApiKey>;
  loadDotenv?: (options?: LoadCliDotenvOptions) => DotenvLoadResult;
  prepareQodexTelemetryEnv?: typeof prepareQodexTelemetryEnv;
  projectConfigPath?: string;
  listSessions?: (options: ListQodexSessionsOptions) => ReturnType<typeof listQodexSessions>;
  listCustomCommands?: (
    options: ListQodexCustomCommandsOptions,
  ) => ReturnType<typeof listQodexCustomCommands>;
  loadCustomCommand?: (
    options: InspectQodexCustomCommandOptions,
  ) => ReturnType<typeof loadQodexCustomCommand>;
  // headless slash 路由要和 app facade 的保留名 gate 用同一个判据；默认取 bootstrap 的，
  // 注入点只为让单测不必拉起整个 bootstrap 模块。见 prompt-command.ts。
  isReservedSlashCommandName?: BootstrapModule["isReservedQodexSlashCommandName"];
  listSkills?: (options: ListQodexSkillsOptions) => ReturnType<typeof listQodexSkills>;
  logger?: Logger;
  readClipboardImage?: TuiReadClipboardImage;
  writeClipboardText?: TuiWriteClipboardText;
  resolveLatestSession?: (
    options: ResolveLatestSessionOptions,
  ) => ReturnType<typeof resolveLatestSession>;
  resolveWorkspaceGitBranch?: typeof resolveWorkspaceGitBranch;
  runQodexProtocolAgent?: (options?: RunQodexProtocolAgentOptions) => Promise<void>;
  runTui?: typeof import("@qodex/tui").runTui;
  skipUserConfig?: boolean;
  userConfigPath?: string;
  exitProcess?: (code: number) => void;
  shutdownCleanupTimeoutMs?: number;
  shutdownProcess?: CliShutdownProcess;
  startProcessProviderRegistryRuntime?: typeof startProcessProviderRegistryRuntime;
  shutdownQodexTelemetry?: typeof shutdownQodexTelemetry;
}

export type CliPermissionMode = "build" | "plan" | "edit" | "yolo" | "guarded";
export type CliRuntimeMode = CliPermissionMode | "auto";

export interface CliModeState {
  current?: CliRuntimeMode;
  override?: CliPermissionMode;
}

export interface CliTargetRequest {
  objective: string;
  replaceExisting: boolean;
}

export type ModeCapableApp = Awaited<ReturnType<typeof createQodexApp>> & {
  getMode?: () => CliRuntimeMode;
  setLocale?: (locale: UiLocale) => Promise<{ locale: "en-US" | "zh-CN" }>;
  setMode?: (mode: CliRuntimeMode) => Promise<{ mode: CliRuntimeMode }>;
};

export interface CliResumeRequest {
  continueSession: boolean;
  resumeSessionId?: string;
}
