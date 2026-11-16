import type { BrowserClientTransport } from "@marid/core/browser-client";

export const NODE_REPL_BROWSER_BRIDGE_SYMBOL = Symbol.for("marid.node-repl.browser-control-bridge");

export interface NodeReplBrowserRuntimeBridge extends BrowserClientTransport {
  documentationRoot: string;
  assertAvailable(): void;
}

export function readNodeReplBrowserRuntimeBridge(
  globals: Record<PropertyKey, unknown>,
): NodeReplBrowserRuntimeBridge {
  const bridge = globals[NODE_REPL_BROWSER_BRIDGE_SYMBOL];
  if (!bridge || typeof bridge !== "object") {
    throw new Error(
      "Browser runtime bridge is unavailable. Use Browser from a Marid desktop or shared-host session.",
    );
  }
  return bridge as NodeReplBrowserRuntimeBridge;
}
