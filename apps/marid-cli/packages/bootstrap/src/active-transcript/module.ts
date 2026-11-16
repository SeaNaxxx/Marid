export const activeTranscriptModule = {
  id: "cli-active-transcript",
  requires: ["shared", "marid-cli"],
  provides: ["cli-active-transcript"],
  publicEntrypoints: ["index.ts", "contract.ts"],
} as const;
