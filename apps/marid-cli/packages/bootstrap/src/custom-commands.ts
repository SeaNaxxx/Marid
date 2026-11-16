import { resolve } from "node:path";
import { createConfigWithPluginMigrations } from "@marid/adapters/config";
import { createNodeCustomCommandAdapter } from "@marid/adapters/commands";
import type {
  CustomCommandContent,
  CustomCommandDiagnostic,
  CustomCommandLoadOutcome,
  Logger,
} from "@marid/contracts";
import { resolveMaridPlugins, type ResolveMaridPluginsOptions } from "./plugins.js";
import { collectDisabledPaths } from "./skill-command-overrides.js";

export interface ListMaridCustomCommandsOptions {
  workspaceIdentity?: string;
  remoteSessionId?: string;
  pluginPreparation?: ResolveMaridPluginsOptions["pluginPreparation"];
  pluginDiscovery?: ResolveMaridPluginsOptions["pluginDiscovery"];
  env?: NodeJS.ProcessEnv;
  homeDirectory?: string;
  logger?: Logger;
  projectConfigPath?: string;
  skipUserConfig?: boolean;
  userConfigPath?: string;
  workingDirectory?: string;
  additionalDirectories?: readonly string[];
}

export interface InspectMaridCustomCommandOptions extends ListMaridCustomCommandsOptions {
  name: string;
}

export interface MaridCustomCommandInspection {
  command: CustomCommandContent;
  diagnostics: CustomCommandDiagnostic[];
}

export async function listMaridCustomCommands(
  options: ListMaridCustomCommandsOptions = {},
): Promise<CustomCommandLoadOutcome> {
  const discovery = await createCustomCommandDiscovery(options);
  return await discovery.adapter.discoverCommands({
    workingDirectory: discovery.workingDirectory,
  });
}

export async function inspectMaridCustomCommand(
  options: InspectMaridCustomCommandOptions,
): Promise<MaridCustomCommandInspection> {
  const discovery = await createCustomCommandDiscovery(options);
  const workingDirectory = discovery.workingDirectory;
  const adapter = discovery.adapter;
  const outcome = await adapter.discoverCommands({ workingDirectory });
  const normalizedName = normalizeCommandName(options.name);
  if (!outcome.commands.some((command) => command.name === normalizedName)) {
    throw new Error(`Custom command not found: ${options.name}`);
  }

  return {
    command: await adapter.loadCommand({
      name: normalizedName,
      workingDirectory,
    }),
    diagnostics: outcome.diagnostics,
  };
}

export async function loadMaridCustomCommand(
  options: InspectMaridCustomCommandOptions,
): Promise<CustomCommandContent> {
  const discovery = await createCustomCommandDiscovery(options);
  return await discovery.adapter.loadCommand({
    name: normalizeCommandName(options.name),
    workingDirectory: discovery.workingDirectory,
  });
}

async function createCustomCommandDiscovery(options: ListMaridCustomCommandsOptions) {
  const workingDirectory = resolve(options.workingDirectory ?? process.cwd());
  const configResult = await createConfigWithPluginMigrations({
    env: options.env,
    projectConfigPath: options.projectConfigPath,
    workingDirectory,
    skipUserConfig: options.skipUserConfig,
    userConfigPath: options.userConfigPath,
  });
  const pluginOutcome = await resolveMaridPlugins({
    pluginPreparation: options.pluginPreparation,
    pluginDiscovery: options.pluginDiscovery,
    workspaceIdentity: options.workspaceIdentity,
    remoteSessionId: options.remoteSessionId,
    configResult,
    env: options.env,
    logger: options.logger,
    projectConfigPath: options.projectConfigPath,
    skipUserConfig: options.skipUserConfig,
    userConfigPath: options.userConfigPath,
    workingDirectory,
  });
  return {
    adapter: createNodeCustomCommandAdapter({
      extraResolvedRoots: pluginOutcome.commandRoots,
      disabledPaths: collectDisabledPaths(configResult.config.commandOverrides),
      homeDirectory: options.homeDirectory,
      additionalDirectories: options.additionalDirectories,
    }),
    workingDirectory,
  };
}

function normalizeCommandName(name: string): string {
  return name.trim().replace(/^\/+/, "").toLowerCase();
}
