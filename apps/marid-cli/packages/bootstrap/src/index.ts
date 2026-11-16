// Bootstrap public API surface.

export * from "./app/create-app.js";
export type {
  ListMaridSessionsOptions,
  PromptInput,
  ResolveLatestSessionOptions,
  ResumeOptions,
  RunMaridProtocolAgentOptions,
  SendInputOptions,
  SendInputResult,
  SetLocaleResult,
  SteerTurnOptions,
  SubmitPromptOptions,
  UserPromptInput,
  MaridApp,
  MaridAppOptions,
  MaridModelOption,
} from "./app/types.js";
export {
  inspectMaridCustomCommand,
  listMaridCustomCommands,
  loadMaridCustomCommand,
} from "./custom-commands.js";
export type {
  InspectMaridCustomCommandOptions,
  ListMaridCustomCommandsOptions,
  MaridCustomCommandInspection,
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
  addMaridPluginMarketplace,
  getMaridPluginsOverview,
  installMaridMarketplacePlugin,
  listMaridPlugins,
  removeMaridPluginMarketplace,
  resolveMaridPlugins,
  setMaridPluginEnabled,
  uninstallMaridMarketplacePlugin,
  updateMaridMarketplacePlugin,
  updateMaridPluginMarketplace,
  validateMaridPluginPath,
} from "./plugins.js";
export type {
  AddMaridMarketplaceOptions,
  InstallMaridMarketplacePluginOptions,
  ListMaridPluginsOptions,
  RemoveMaridMarketplaceOptions,
  ResolveMaridPluginsOptions,
  SetMaridPluginEnabledOptions,
  SetMaridPluginEnabledResult,
  UninstallMaridMarketplacePluginOptions,
  UpdateMaridMarketplaceOptions,
  UpdateMaridMarketplacePluginOptions,
  ValidateMaridPluginPathOptions,
  MaridAvailablePluginData,
  MaridInstalledPluginData,
  MaridMarketplaceSummaryData,
  MaridMarketplaceUpdateData,
  MaridPluginInstallData,
  MaridPluginUpdateData,
  MaridPluginsOverviewData,
} from "./plugins.js";
export { runMaridProtocolAgent } from "./marid-protocol-entrypoint.js";
// Exposed for the CLI's --output-format stream-json: it needs the same event
// shape the protocol server emits, rather than inventing a second one.
export { mapSessionEvent } from "./marid-protocol/session-mapper.js";
export { prepareMaridTelemetryEnv, shutdownMaridTelemetry } from "./telemetry-bootstrap.js";
export type { SessionTranscriptMessage, SessionTranscriptPart } from "./session-transcript.js";
export { listMaridSessions, resolveLatestSession } from "./sessions.js";
export { inspectMaridSkill, listMaridSkills } from "./skills.js";
export type {
  InspectMaridSkillOptions,
  ListMaridSkillsOptions,
  MaridSkillInspection,
} from "./skills.js";
// Exposed for the CLI's headless slash routing: it must decide "is this a real
// custom command?" with the *same* reserved-name gate the app facade's
// customCommandPromptResolver applies, or the two disagree and a reserved name
// reaches the model as literal prompt text. See prompt-command.ts.
export { isReservedMaridSlashCommandName } from "./slash-command-surface.js";
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
