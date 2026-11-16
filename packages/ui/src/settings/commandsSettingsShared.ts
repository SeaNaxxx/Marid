import {
  MARID_AGENT_PROVIDER,
  MARID_COMMAND_AGENT_SOURCES,
  type MaridProvider,
  type CommandAgentSource,
} from "@marid/shared";

export const COMMAND_AGENT_SOURCES = [
  ...MARID_COMMAND_AGENT_SOURCES,
] as const satisfies readonly CommandAgentSource[];

export const COMMAND_AGENT_SOURCE_META: Record<
  CommandAgentSource,
  {
    labelId: string;
    provider: MaridProvider;
    pathHint: string;
  }
> = {
  maridAgent: {
    labelId: "settings.commands.source.maridAgent",
    pathHint: "~/.marid/commands",
    provider: MARID_AGENT_PROVIDER,
  },
};
