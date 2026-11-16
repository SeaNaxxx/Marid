import type { Event, IDisposable } from "@marid/rpc";
import type { MaridProtocolMessage } from "@marid/shared";

export type MaridProtocolTransportKind = "stdio" | "websocket" | "memory";

export interface MaridProtocolTransportClosedEvent {
  code?: number | null;
  signal?: NodeJS.Signals | null;
  reason?: string;
}

export interface MaridProtocolTransport extends IDisposable {
  readonly kind: MaridProtocolTransportKind;
  readonly onMessage: Event<MaridProtocolMessage>;
  readonly onClose: Event<MaridProtocolTransportClosedEvent>;
  send(message: MaridProtocolMessage): Promise<void>;
  disposeAndWait?(options?: { deadlineAtMs?: number }): Promise<void>;
}
