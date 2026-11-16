import type { Event, IDisposable } from "@qodex/rpc";
import type { QodexProtocolMessage } from "@qodex/shared";

export type QodexProtocolTransportKind = "stdio" | "websocket" | "memory";

export interface QodexProtocolTransportClosedEvent {
  code?: number | null;
  signal?: NodeJS.Signals | null;
  reason?: string;
}

export interface QodexProtocolTransport extends IDisposable {
  readonly kind: QodexProtocolTransportKind;
  readonly onMessage: Event<QodexProtocolMessage>;
  readonly onClose: Event<QodexProtocolTransportClosedEvent>;
  send(message: QodexProtocolMessage): Promise<void>;
  disposeAndWait?(options?: { deadlineAtMs?: number }): Promise<void>;
}
