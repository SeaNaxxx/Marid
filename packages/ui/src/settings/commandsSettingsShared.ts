import {
  QODEX_AGENT_PROVIDER,
  QODEX_COMMAND_AGENT_SOURCES,
  type QodexProvider,
  type CommandAgentSource,
} from "@qodex/shared";

export const COMMAND_AGENT_SOURCES = [
  ...QODEX_COMMAND_AGENT_SOURCES,
] as const satisfies readonly CommandAgentSource[];

export const COMMAND_AGENT_SOURCE_META: Record<
  CommandAgentSource,
  {
    labelId: string;
    provider: QodexProvider;
    pathHint: string;
  }
> = {
  qodexAgent: {
    labelId: "settings.commands.source.qodexAgent",
    pathHint: "~/.qodex/commands",
    provider: QODEX_AGENT_PROVIDER,
  },
};
