import type { QodexSessionStateSnapshot } from "@qodex/shared";
import type {
  QodexSessionWorkspaceTarget,
  QodexTaskTarget,
} from "#src/qodex-session/qodexSession.js";

function getWorkspaceKey(target: QodexSessionWorkspaceTarget): string {
  return target.workspaceIdentity?.trim() || target.workspacePath;
}

function getSessionScopedKey(target: QodexTaskTarget): string {
  return `${getWorkspaceKey(target)}\0${target.sessionId}`;
}

export function createQodexDeferredDraftRegistry() {
  const sessionKeys = new Set<string>();

  return {
    remember(params: QodexSessionWorkspaceTarget, snapshot: QodexSessionStateSnapshot): void {
      sessionKeys.add(
        getSessionScopedKey({
          workspacePath: snapshot.session.workspace.workspacePath,
          workspaceIdentity:
            snapshot.session.workspace.workspaceIdentity ?? params.workspaceIdentity,
          sessionId: snapshot.session.sessionId,
        }),
      );
    },

    has(target: QodexTaskTarget): boolean {
      return sessionKeys.has(getSessionScopedKey(target));
    },

    forget(target: QodexTaskTarget): void {
      sessionKeys.delete(getSessionScopedKey(target));
    },
  };
}
