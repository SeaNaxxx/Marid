import { beforeEach, describe, expect, it, vi } from "vitest";

const mocks = vi.hoisted(() => ({
  buildPluginReferenceCatalog: vi.fn(),
  getMaridPluginsOverview: vi.fn(),
  resolveMaridPlugins: vi.fn(),
  updateMaridPluginMarketplace: vi.fn(),
}));

vi.mock("@marid/core", () => ({
  buildPluginReferenceCatalog: mocks.buildPluginReferenceCatalog,
}));

vi.mock("../src/plugins.js", () => ({
  enrichCachedClaudeMarketplaceIconsForOverview: vi.fn(),
  getMaridPluginsOverview: mocks.getMaridPluginsOverview,
  resolveMaridPlugins: mocks.resolveMaridPlugins,
  updateMaridPluginMarketplace: mocks.updateMaridPluginMarketplace,
}));

import {
  maridPluginOperationStateSchema,
  maridPluginOperationProgressNotificationSchema,
  maridPluginsResolveSuggestedReferenceParamsSchema,
  maridPluginsResolveSuggestedReferenceResultSchema,
  maridProtocolNotifications,
} from "@marid/shared";
import type { PluginReferenceCatalogEntry } from "@marid/contracts";
import { resolveSuggestedPluginReference } from "../src/marid-protocol/plugin-reference-catalog.js";
import type { MaridProtocolAgentServerContext } from "../src/marid-protocol/server-types.js";

const STABLE_ID = "document-skills@marid-plugins-official";
const WORKSPACE = { workspacePath: "/workspace", workspaceKey: "ssh://host/workspace" };
const DESKTOP_BOUNDARY = {
  operationId: "suggested-test",
  clientMode: "desktop-continuous" as const,
  deliveryKind: "desktop-continuous" as const,
};
const notify = vi.fn();

function protocolContext(): MaridProtocolAgentServerContext {
  return { notify } as unknown as MaridProtocolAgentServerContext;
}

function catalogEntry(
  overrides: Partial<PluginReferenceCatalogEntry> = {},
): PluginReferenceCatalogEntry {
  return {
    pluginId: STABLE_ID,
    name: "document-skills",
    marketplace: "marid-plugins-official",
    enabled: true,
    conflictingPluginIds: [],
    skillQualifiedNames: ["document-skills:pdf"],
    mcpServerNames: [],
    subagentNames: [],
    rootPath: "/plugins/document-skills",
    ...overrides,
  };
}

function overviewCandidate(overrides: Record<string, unknown> = {}) {
  return {
    id: STABLE_ID,
    name: "document-skills",
    marketplace: "marid-plugins-official",
    installed: false,
    listing: { displayName: "Document skills" },
    ...overrides,
  };
}

describe("plugins/resolveSuggestedReference protocol", () => {
  let entries: PluginReferenceCatalogEntry[];

  beforeEach(() => {
    entries = [];
    mocks.buildPluginReferenceCatalog.mockReset();
    mocks.buildPluginReferenceCatalog.mockImplementation(() => ({ plugins: entries }));
    mocks.resolveMaridPlugins.mockReset();
    mocks.resolveMaridPlugins.mockReturnValue({ diagnostics: [], plugins: [] });
    mocks.updateMaridPluginMarketplace.mockReset();
    mocks.updateMaridPluginMarketplace.mockResolvedValue({ diagnostics: [], marketplaces: [] });
    mocks.getMaridPluginsOverview.mockReset();
    mocks.getMaridPluginsOverview.mockReturnValue({
      availablePlugins: [],
      diagnostics: [],
      installedPlugins: [],
      marketplaces: [],
      restorableBuiltins: [],
    });
    notify.mockReset();
  });

  it("registers a strict request/result contract with operation and delivery boundaries", () => {
    expect(maridPluginOperationStateSchema.parse("cancelling")).toBe("cancelling");
    expect(maridPluginOperationStateSchema.parse("cancelled")).toBe("cancelled");
    expect(
      maridPluginOperationProgressNotificationSchema.parse({
        operationId: "suggested-1",
        state: "refreshing",
      }),
    ).toEqual({ operationId: "suggested-1", state: "refreshing" });
    expect(() =>
      maridPluginOperationProgressNotificationSchema.parse({
        operationId: "suggested-1",
        state: "refreshing",
        unexpected: true,
      }),
    ).toThrow();
    expect(
      maridPluginsResolveSuggestedReferenceParamsSchema.parse({
        workspace: WORKSPACE,
        stableId: STABLE_ID,
        operationId: "suggested-1",
        clientMode: "web-remote-replayable",
        deliveryKind: "web-remote-replayable",
      }),
    ).toEqual({
      workspace: WORKSPACE,
      stableId: STABLE_ID,
      operationId: "suggested-1",
      clientMode: "web-remote-replayable",
      deliveryKind: "web-remote-replayable",
    });
    expect(() =>
      maridPluginsResolveSuggestedReferenceParamsSchema.parse({
        workspace: WORKSPACE,
        stableId: STABLE_ID,
        operationId: "suggested-missing-client-mode",
        deliveryKind: "desktop-continuous",
      }),
    ).toThrow();
    expect(() =>
      maridPluginsResolveSuggestedReferenceParamsSchema.parse({
        workspace: WORKSPACE,
        stableId: STABLE_ID,
        clientMode: "desktop-continuous",
        deliveryKind: "desktop-continuous",
      }),
    ).toThrow();
    expect(() =>
      maridPluginsResolveSuggestedReferenceParamsSchema.parse({
        workspace: WORKSPACE,
        stableId: STABLE_ID,
        unexpected: true,
      }),
    ).toThrow();
    expect(() =>
      maridPluginsResolveSuggestedReferenceResultSchema.parse({
        stableId: STABLE_ID,
        status: "missing",
        diagnostics: [],
      }),
    ).toThrow(/trusted install identity/);
    expect(
      maridPluginsResolveSuggestedReferenceResultSchema.parse({
        stableId: STABLE_ID,
        status: "ready",
        marketplace: "marid-plugins-official",
        pluginName: "document-skills",
        sourceTrust: "official",
        icon: "https://cdn.example/plugin.svg",
        diagnostics: [],
      }).icon,
    ).toBe("https://cdn.example/plugin.svg");
  });

  it("rejects non-official stable IDs without refreshing any marketplace", async () => {
    const result = await resolveSuggestedPluginReference(protocolContext(), {
      workspace: WORKSPACE,
      stableId: "document-skills@personal",
      ...DESKTOP_BOUNDARY,
    });

    expect(result.status).toBe("unavailable");
    expect(result.diagnostics[0]?.code).toBe("plugin_suggested_reference_untrusted_source");
    expect(mocks.updateMaridPluginMarketplace).not.toHaveBeenCalled();
    expect(() => maridPluginsResolveSuggestedReferenceResultSchema.parse(result)).not.toThrow();
  });

  it.each([
    ["ready", true],
    ["disabled", false],
  ] as const)(
    "returns %s for an installed official plugin without refreshing",
    async (status, enabled) => {
      entries = [catalogEntry({ enabled })];
      mocks.getMaridPluginsOverview.mockReturnValue({
        availablePlugins: [
          overviewCandidate({
            installed: true,
            listing: { icon: "https://cdn.example/document-skills.svg" },
          }),
        ],
        diagnostics: [],
        installedPlugins: [],
        marketplaces: [],
        restorableBuiltins: [],
      });

      const result = await resolveSuggestedPluginReference(protocolContext(), {
        workspace: WORKSPACE,
        stableId: STABLE_ID,
        ...DESKTOP_BOUNDARY,
      });

      expect(result).toMatchObject({
        stableId: STABLE_ID,
        status,
        marketplace: "marid-plugins-official",
        pluginName: "document-skills",
        sourceTrust: "official",
        icon: "https://cdn.example/document-skills.svg",
      });
      expect(mocks.updateMaridPluginMarketplace).not.toHaveBeenCalled();
      expect(notify).not.toHaveBeenCalled();
    },
  );

  it("refreshes only marid-plugins-official before exposing a missing candidate", async () => {
    mocks.getMaridPluginsOverview.mockReturnValue({
      availablePlugins: [
        overviewCandidate({ listing: { icon: "https://cdn.example/document-skills.svg" } }),
      ],
      diagnostics: [],
      installedPlugins: [],
      marketplaces: [],
      restorableBuiltins: [],
    });

    const result = await resolveSuggestedPluginReference(protocolContext(), {
      workspace: WORKSPACE,
      stableId: STABLE_ID,
      operationId: "suggested-2",
      clientMode: "desktop-continuous",
      deliveryKind: "desktop-continuous",
    });

    expect(mocks.updateMaridPluginMarketplace).toHaveBeenCalledOnce();
    expect(mocks.updateMaridPluginMarketplace).toHaveBeenCalledWith(
      expect.objectContaining({
        marketplace: "marid-plugins-official",
        workingDirectory: "/workspace",
      }),
    );
    expect(notify).toHaveBeenCalledWith({
      method: "plugins/operationProgress",
      params: { operationId: "suggested-2", state: "refreshing" },
    });
    expect(result).toMatchObject({
      stableId: STABLE_ID,
      status: "missing",
      marketplace: "marid-plugins-official",
      pluginName: "document-skills",
      sourceTrust: "official",
      icon: "https://cdn.example/document-skills.svg",
    });
  });

  it("emits refreshing after the first local miss and before marketplace refresh settles", async () => {
    let settleRefresh!: (value: { diagnostics: never[]; marketplaces: never[] }) => void;
    mocks.updateMaridPluginMarketplace.mockReturnValue(
      new Promise((resolve) => {
        settleRefresh = resolve;
      }),
    );
    const pending = resolveSuggestedPluginReference(protocolContext(), {
      workspace: WORKSPACE,
      stableId: STABLE_ID,
      operationId: "suggested-progress",
      clientMode: "desktop-continuous",
      deliveryKind: "desktop-continuous",
    });

    await vi.waitFor(() => {
      expect(notify).toHaveBeenCalledWith({
        method: maridProtocolNotifications.pluginOperationProgress,
        params: { operationId: "suggested-progress", state: "refreshing" },
      });
    });
    expect(mocks.updateMaridPluginMarketplace).toHaveBeenCalledOnce();

    settleRefresh({ diagnostics: [], marketplaces: [] });
    await expect(pending).resolves.toMatchObject({ status: "unavailable" });
  });

  it("fails closed after a refresh failure even when an old snapshot still has a candidate", async () => {
    mocks.updateMaridPluginMarketplace.mockResolvedValue({
      diagnostics: [
        {
          code: "plugin_archive_fetch_failed",
          message: "offline",
          pluginId: "marid-plugins-official",
          severity: "error",
        },
      ],
      marketplaces: [],
    });
    mocks.getMaridPluginsOverview.mockReturnValue({
      availablePlugins: [overviewCandidate()],
      diagnostics: [],
      installedPlugins: [],
      marketplaces: [],
      restorableBuiltins: [],
    });

    const result = await resolveSuggestedPluginReference(protocolContext(), {
      workspace: WORKSPACE,
      stableId: STABLE_ID,
      ...DESKTOP_BOUNDARY,
    });

    expect(result.status).toBe("unavailable");
    expect(result.diagnostics[0]?.code).toBe("marketplace_refresh_failed");
    expect(mocks.getMaridPluginsOverview).not.toHaveBeenCalled();
  });

  it("fails closed and aborts the official marketplace refresh at ten seconds", async () => {
    let refreshSignal: AbortSignal | undefined;
    const refreshStarted = Promise.withResolvers<void>();
    mocks.updateMaridPluginMarketplace.mockImplementation(
      (options: { abortSignal?: AbortSignal }) => {
        refreshSignal = options.abortSignal;
        refreshStarted.resolve();
        return new Promise(() => {});
      },
    );

    vi.useFakeTimers();
    try {
      const pending = resolveSuggestedPluginReference(protocolContext(), {
        workspace: WORKSPACE,
        stableId: STABLE_ID,
        ...DESKTOP_BOUNDARY,
      });
      await refreshStarted.promise;
      expect(refreshSignal).toBeInstanceOf(AbortSignal);

      vi.advanceTimersByTime(9_999);
      await Promise.resolve();
      expect(refreshSignal?.aborted).toBe(false);

      vi.advanceTimersByTime(1);
      const result = await pending;

      expect(refreshSignal?.aborted).toBe(true);
      expect(result.status).toBe("unavailable");
      expect(result.diagnostics[0]?.code).toBe("marketplace_refresh_failed");
      expect(mocks.getMaridPluginsOverview).not.toHaveBeenCalled();
    } finally {
      vi.useRealTimers();
    }
  });

  it("reports cancellation separately from marketplace refresh failure", async () => {
    const controller = new AbortController();
    mocks.updateMaridPluginMarketplace.mockImplementation(async () => {
      controller.abort();
      throw controller.signal.reason;
    });

    const result = await resolveSuggestedPluginReference(
      protocolContext(),
      { workspace: WORKSPACE, stableId: STABLE_ID, ...DESKTOP_BOUNDARY },
      controller.signal,
    );

    expect(result.status).toBe("unavailable");
    expect(result.diagnostics[0]?.code).toBe("plugin_operation_cancelled");
  });

  it("re-resolves installed state from the refreshed catalog", async () => {
    mocks.updateMaridPluginMarketplace.mockImplementation(async () => {
      entries = [catalogEntry()];
      return { diagnostics: [], marketplaces: [] };
    });

    const result = await resolveSuggestedPluginReference(protocolContext(), {
      workspace: WORKSPACE,
      stableId: STABLE_ID,
      ...DESKTOP_BOUNDARY,
    });

    expect(mocks.resolveMaridPlugins).toHaveBeenCalledTimes(2);
    expect(result.status).toBe("ready");
  });

  it("rejects a disappeared or identity-mismatched candidate after refresh", async () => {
    mocks.getMaridPluginsOverview.mockReturnValue({
      availablePlugins: [overviewCandidate({ marketplace: "personal" })],
      diagnostics: [],
      installedPlugins: [],
      marketplaces: [],
      restorableBuiltins: [],
    });

    const result = await resolveSuggestedPluginReference(protocolContext(), {
      workspace: WORKSPACE,
      stableId: STABLE_ID,
      ...DESKTOP_BOUNDARY,
    });

    expect(result.status).toBe("unavailable");
    expect(result.diagnostics[0]?.code).toBe("plugin_suggested_reference_not_listed");
  });
});
