import { describe, expect, it } from "vitest";
import { resolveDefaultEmbeddedSearchBackend } from "../src/app/embedded-search-backend.js";

describe("resolveDefaultEmbeddedSearchBackend", () => {
  it("uses bundled bfs, ugrep, and ripgrep paths by default", () => {
    expect(
      resolveDefaultEmbeddedSearchBackend({
        env: {
          MARID_BFS_BINARY: "/opt/marid/tools/bfs/bfs",
          MARID_RG_BINARY: "/opt/marid/tools/ripgrep/rg",
          MARID_UGREP_BINARY: "/opt/marid/tools/ugrep/ugrep",
        },
      }),
    ).toEqual({
      kind: "native-binaries",
      findCommand: "/opt/marid/tools/bfs/bfs",
      grepCommand: "/opt/marid/tools/ugrep/ugrep",
      rgCommand: "/opt/marid/tools/ripgrep/rg",
    });
  });

  it("falls back to bfs, ugrep, and rg command names when bundled paths are unavailable", () => {
    expect(resolveDefaultEmbeddedSearchBackend({ env: {} })).toEqual({
      kind: "native-binaries",
      findCommand: "bfs",
      grepCommand: "ugrep",
      rgCommand: "rg",
    });
  });

  it("preserves the explicit internal CLI compatibility override", () => {
    expect(
      resolveDefaultEmbeddedSearchBackend({
        env: { MARID_EMBEDDED_SEARCH_COMMAND: "/tmp/marid-test" },
      }),
    ).toEqual({
      kind: "internal-cli",
      command: "/tmp/marid-test",
      args: ["__internal-search"],
    });
  });
});
