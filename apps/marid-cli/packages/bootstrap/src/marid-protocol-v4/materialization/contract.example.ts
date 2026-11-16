import type { ConversationSnapshot } from "@marid/shared/marid-protocol-v4";
import { conversationWireSnapshot, measureConversationSnapshotBytes } from "./index.js";

export function measureWireExample(snapshot: ConversationSnapshot): number {
  return measureConversationSnapshotBytes(conversationWireSnapshot(snapshot), {
    topic: `conversation/${snapshot.sessionId}`,
    logEpoch: snapshot.logEpoch,
  });
}
