import { describe, expect, it } from "vitest";
import { resolveDefaultEmbeddedSearchBackend } from "../src/app/embedded-search-backend.js";

describe("resolveDefaultEmbeddedSearchBackend", () => {
  it("uses bundled bfs, ugrep, and ripgrep paths by default", () => {
    expect(
      resolveDefaultEmbeddedSearchBackend({
        env: {
          QODEX_BFS_BINARY: "/opt/qodex/tools/bfs/bfs",
          QODEX_RG_BINARY: "/opt/qodex/tools/ripgrep/rg",
          QODEX_UGREP_BINARY: "/opt/qodex/tools/ugrep/ugrep",
        },
      }),
    ).toEqual({
      kind: "native-binaries",
      findCommand: "/opt/qodex/tools/bfs/bfs",
      grepCommand: "/opt/qodex/tools/ugrep/ugrep",
      rgCommand: "/opt/qodex/tools/ripgrep/rg",
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
        env: { QODEX_EMBEDDED_SEARCH_COMMAND: "/tmp/qodex-test" },
      }),
    ).toEqual({
      kind: "internal-cli",
      command: "/tmp/qodex-test",
      args: ["__internal-search"],
    });
  });
});
