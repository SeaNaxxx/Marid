// Bootstrap public API surface.

export * from "./app/create-app.js";
export type {
  ListQodexSessionsOptions,
  PromptInput,
  ResolveLatestSessionOptions,
  ResumeOptions,
  RunQodexProtocolAgentOptions,
  SendInputOptions,
  SendInputResult,
  SetLocaleResult,
  SteerTurnOptions,
  SubmitPromptOptions,
  UserPromptInput,
  QodexApp,
  QodexAppOptions,
  QodexModelOption,
} from "./app/types.js";
export {
  inspectQodexCustomCommand,
  listQodexCustomCommands,
  loadQodexCustomCommand,
} from "./custom-commands.js";
export type {
  InspectQodexCustomCommandOptions,
  ListQodexCustomCommandsOptions,
  QodexCustomCommandInspection,
} from "./custom-commands.js";
export { createModelAdapter } from "./model-factory.js";
export type { CreateModelAdapterOptions } from "./model-factory.js";
export {
  createDefaultProviderEndpointRoutingPort,
  resolveProcessProviderEndpointRoutingPort,
} from "./provider-endpoint-routing.js";
export type {
  CreateDefaultProviderEndpointRoutingPortOptions,
  ResolveProcessProviderEndpointRoutingPortOptions,
} from "./provider-endpoint-routing.js";
export { startProcessProviderRegistryRuntime } from "./app/process-provider-registry-runtime.js";
export type { ProcessProviderRegistryRuntimeOptions } from "./app/process-provider-registry-runtime.js";
export {
  addQodexPluginMarketplace,
  getQodexPluginsOverview,
  installQodexMarketplacePlugin,
  listQodexPlugins,
  removeQodexPluginMarketplace,
  resolveQodexPlugins,
  setQodexPluginEnabled,
  uninstallQodexMarketplacePlugin,
  updateQodexMarketplacePlugin,
  updateQodexPluginMarketplace,
  validateQodexPluginPath,
} from "./plugins.js";
export type {
  AddQodexMarketplaceOptions,
  InstallQodexMarketplacePluginOptions,
  ListQodexPluginsOptions,
  RemoveQodexMarketplaceOptions,
  ResolveQodexPluginsOptions,
  SetQodexPluginEnabledOptions,
  SetQodexPluginEnabledResult,
  UninstallQodexMarketplacePluginOptions,
  UpdateQodexMarketplaceOptions,
  UpdateQodexMarketplacePluginOptions,
  ValidateQodexPluginPathOptions,
  QodexAvailablePluginData,
  QodexInstalledPluginData,
  QodexMarketplaceSummaryData,
  QodexMarketplaceUpdateData,
  QodexPluginInstallData,
  QodexPluginUpdateData,
  QodexPluginsOverviewData,
} from "./plugins.js";
export { runQodexProtocolAgent } from "./qodex-protocol-entrypoint.js";
// Exposed for the CLI's --output-format stream-json: it needs the same event
// shape the protocol server emits, rather than inventing a second one.
export { mapSessionEvent } from "./qodex-protocol/session-mapper.js";
export { prepareQodexTelemetryEnv, shutdownQodexTelemetry } from "./telemetry-bootstrap.js";
export type { SessionTranscriptMessage, SessionTranscriptPart } from "./session-transcript.js";
export { listQodexSessions, resolveLatestSession } from "./sessions.js";
export { inspectQodexSkill, listQodexSkills } from "./skills.js";
export type {
  InspectQodexSkillOptions,
  ListQodexSkillsOptions,
  QodexSkillInspection,
} from "./skills.js";
// Exposed for the CLI's headless slash routing: it must decide "is this a real
// custom command?" with the *same* reserved-name gate the app facade's
// customCommandPromptResolver applies, or the two disagree and a reserved name
// reaches the model as literal prompt text. See prompt-command.ts.
export { isReservedQodexSlashCommandName } from "./slash-command-surface.js";
export {
  grantWorkspaceHookTrust,
  inspectWorkspaceHookTrust,
  revokeWorkspaceHookTrustCli,
} from "./workspace-hook-trust-cli.js";
export type {
  WorkspaceHookTrustCliItem,
  WorkspaceHookTrustCliStatus,
  WorkspaceHookTrustCliTarget,
} from "./workspace-hook-trust-cli.js";

export {
  createOfficialPluginPreparation,
  type OfficialPluginPreparation,
} from "./app/bundled-plugins.js";
// 嵌入式调用复用产品请求装配，避免来源头与网络配置漂移。
export { createRuntimeAiSdkModelExecutionConfig } from "./model-config.js";
