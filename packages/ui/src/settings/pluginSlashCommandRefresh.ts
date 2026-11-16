import type { MaridCommand, MaridSlashCommand } from "@marid/shared";

function normalizeSlashCommandName(name: string): string {
  return name.trim().replace(/^\/+/, "");
}

function slashCommandKey(name: string): string {
  return normalizeSlashCommandName(name).toLowerCase();
}

function commandToSlashCommand(command: MaridCommand): MaridSlashCommand | null {
  if (!command.enabled) {
    return null;
  }

  const name = normalizeSlashCommandName(command.name);
  if (!name) {
    return null;
  }

  return {
    name,
    description: command.description ?? "",
    inputHint: command.argumentHint ? `/${name} ${command.argumentHint}` : `/${name}`,
    source: "custom",
  };
}

export function mergeSlashCommandsAfterCommandRefresh(
  currentSlashCommands: readonly MaridSlashCommand[],
  refreshedCommands: readonly MaridCommand[],
): MaridSlashCommand[] {
  const preservedCommands = currentSlashCommands.filter((command) => command.source !== "custom");
  const preservedNames = new Set(
    preservedCommands.map((command) => slashCommandKey(command.name)).filter(Boolean),
  );
  const customCommands: MaridSlashCommand[] = [];
  const seenCustomNames = new Set<string>();

  for (const command of refreshedCommands) {
    const slashCommand = commandToSlashCommand(command);
    if (!slashCommand) {
      continue;
    }

    const key = slashCommandKey(slashCommand.name);
    if (!key || preservedNames.has(key) || seenCustomNames.has(key)) {
      continue;
    }

    seenCustomNames.add(key);
    customCommands.push(slashCommand);
  }

  return [...preservedCommands, ...customCommands];
}
