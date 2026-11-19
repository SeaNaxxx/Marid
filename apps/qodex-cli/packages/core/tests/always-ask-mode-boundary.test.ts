import { describe, expect, it } from "vitest";
import { PermissionService } from "../src/permission/service.js";

/**
 * Audit C-03 regression: permissive execution modes must not bypass approval
 * for tools that declare alwaysAsk (e.g. the host node_repl `js` tool, which
 * runs unsandboxed with agent privileges). alwaysAsk asks survive yolo and
 * guarded pass-through by design; this pins the guarantee at the service level.
 */
describe("PermissionService alwaysAsk mode boundary", () => {
  const toolCapability = {
    toolName: "mcp__node_repl__js",
    permission: {
      permission: "mcp",
      reason: "host node_repl js executes unsandboxed",
      riskLevel: "high",
      sideEffectScope: "system",
      needsApproval: true,
      patternSources: ["toolName", "input", "network"],
      denyPriority: "beforeAsk",
      alwaysAsk: true as const,
      askOptions: { allowAlways: "session" as const },
    },
  };

  const context = {
    toolName: "mcp__node_repl__js",
    mode: "yolo" as const,
  };

  it("asks in yolo mode", () => {
    const service = new PermissionService();
    const decision = service.checkPermission(context, toolCapability);
    expect(decision.decision).toBe("ask");
    expect(decision.allowed).toBe(false);
  });

  it("asks in guarded mode", () => {
    const service = new PermissionService();
    const decision = service.checkPermission(
      { ...context, mode: "guarded" },
      toolCapability,
    );
    expect(decision.decision).toBe("ask");
    expect(decision.allowed).toBe(false);
  });

  it("still denies explicitly disallowed alwaysAsk tools (ask never beats deny)", () => {
    const service = new PermissionService({
      disallowedTools: new Set(["mcp__node_repl__js"]),
    });
    const decision = service.checkPermission(context, toolCapability);
    expect(decision.decision).toBe("deny");
  });

  it("yolo still bypasses prompts for ordinary tools (existing behavior preserved)", () => {
    const service = new PermissionService();
    const decision = service.checkPermission(
      { toolName: "mcp__some-server__widget", mode: "yolo" },
      {
        toolName: "mcp__some-server__widget",
        permission: {
          permission: "mcp",
          reason: "ordinary mcp tool",
          riskLevel: "medium",
          sideEffectScope: "network",
          needsApproval: true,
          patternSources: ["toolName", "input", "network"],
          denyPriority: "beforeAsk",
        },
      },
    );
    expect(decision.decision).toBe("allow");
    expect(decision.ruleId).toBe("mode.yolo");
  });
});
