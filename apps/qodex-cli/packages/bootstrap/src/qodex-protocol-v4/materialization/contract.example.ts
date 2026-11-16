import type { ConversationSnapshot } from "@qodex/shared/qodex-protocol-v4";
import { conversationWireSnapshot, measureConversationSnapshotBytes } from "./index.js";

export function measureWireExample(snapshot: ConversationSnapshot): number {
  return measureConversationSnapshotBytes(conversationWireSnapshot(snapshot), {
    topic: `conversation/${snapshot.sessionId}`,
    logEpoch: snapshot.logEpoch,
  });
}
