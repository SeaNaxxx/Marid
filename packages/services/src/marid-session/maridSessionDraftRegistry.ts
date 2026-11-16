import type { MaridSessionStateSnapshot } from "@marid/shared";
import type {
  MaridSessionWorkspaceTarget,
  MaridTaskTarget,
} from "#src/marid-session/maridSession.js";

function getWorkspaceKey(target: MaridSessionWorkspaceTarget): string {
  return target.workspaceIdentity?.trim() || target.workspacePath;
}

function getSessionScopedKey(target: MaridTaskTarget): string {
  return `${getWorkspaceKey(target)}\0${target.sessionId}`;
}

export function createMaridDeferredDraftRegistry() {
  const sessionKeys = new Set<string>();

  return {
    remember(params: MaridSessionWorkspaceTarget, snapshot: MaridSessionStateSnapshot): void {
      sessionKeys.add(
        getSessionScopedKey({
          workspacePath: snapshot.session.workspace.workspacePath,
          workspaceIdentity:
            snapshot.session.workspace.workspaceIdentity ?? params.workspaceIdentity,
          sessionId: snapshot.session.sessionId,
        }),
      );
    },

    has(target: MaridTaskTarget): boolean {
      return sessionKeys.has(getSessionScopedKey(target));
    },

    forget(target: MaridTaskTarget): void {
      sessionKeys.delete(getSessionScopedKey(target));
    },
  };
}
