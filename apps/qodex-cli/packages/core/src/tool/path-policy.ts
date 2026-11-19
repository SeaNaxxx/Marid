// ============================================================
// Tool Path Policy
// ============================================================

import { realpathSync } from "node:fs";
import {
  basename,
  dirname,
  isAbsolute,
  join,
  normalize,
  resolve,
  sep,
} from "node:path";
import { CoreErrorType, createCoreError } from "@qodex/contracts";

interface ToolWorkspacePathOptions {
  inputPath: string;
  workingDirectory: string;
  workspaceRoot: string;
  operation: "read" | "write" | "execute";
  /**
   * Additional directory roots the host explicitly authorized for this call
   * (e.g. engine data dirs, user-granted storage roots). Everything else
   * outside `workspaceRoot` is rejected — including `../` traversal, absolute
   * paths elsewhere, and symlinks whose real target leaves every root.
   */
  authorizedRoots?: readonly string[];
}

export function resolveWorkspacePath(options: ToolWorkspacePathOptions): string {
  const workingDirectory = normalizeAbsoluteDirectory(
    options.workingDirectory,
    "workingDirectory",
  );
  const roots = normalizeAuthorizedRoots(options.workspaceRoot, options.authorizedRoots);
  const requestedPath = options.inputPath;

  if (requestedPath.length === 0) {
    throw createCoreError(CoreErrorType.ToolExecutionFailed, "Tool path must not be empty", {
      context: {
        operation: options.operation,
      },
      recoverable: true,
    });
  }

  const resolvedPath = isAbsolute(requestedPath)
    ? normalize(requestedPath)
    : resolve(workingDirectory, requestedPath);

  // Security boundary: filesystem tools must never touch anything outside the
  // workspace (or roots the host explicitly authorized). This is enforced on
  // the REAL path — the deepest existing ancestor is resolved through symlinks
  // first, so a symlink planted inside the workspace cannot smuggle reads or
  // writes to application-private files such as the engine credential store.
  return requireWorkspaceContainment(resolvedPath, roots, options.operation);
}

export function resolveToolWorkingDirectory(
  inputCwd: string | undefined,
  options: Omit<ToolWorkspacePathOptions, "inputPath">,
): string {
  if (!inputCwd) {
    const workingDirectory = normalizeAbsoluteDirectory(
      options.workingDirectory,
      "workingDirectory",
    );
    normalizeAuthorizedRoots(options.workspaceRoot, options.authorizedRoots);
    return workingDirectory;
  }

  return resolveWorkspacePath({
    ...options,
    inputPath: inputCwd,
  });
}

/**
 * Authorized roots a tool context contributes beyond the workspace root.
 * Today: the project memory root — the engine data directory lives OUTSIDE
 * the workspace by design (cliStorageRoot/memories/...), and the
 * permission-checked memory Read/Write/Edit flows resolve paths there.
 */
export function contextAuthorizedRoots(context: { memoryRoot?: string }): string[] | undefined {
  return context.memoryRoot ? [context.memoryRoot] : undefined;
}

function normalizeAuthorizedRoots(
  workspaceRoot: string,
  authorizedRoots: readonly string[] | undefined,
): string[] {
  const primary = normalizeAbsoluteDirectory(workspaceRoot, "workspaceRoot");
  const roots = new Set<string>([realpathOrSelf(primary)]);
  for (const extra of authorizedRoots ?? []) {
    const normalized = normalizeAbsoluteDirectory(extra, "authorizedRoot");
    roots.add(realpathOrSelf(normalized));
  }
  return [...roots];
}

/**
 * Real-path the root itself when it exists (an app sandbox often exposes the
 * same tree under two prefixes, e.g. /data/user/0 vs /data/data on Android);
 * fall back to the lexical path when it does not exist yet.
 */
function realpathOrSelf(path: string): string {
  try {
    return realpathSync(path);
  } catch {
    return normalize(path);
  }
}

function requireWorkspaceContainment(
  resolvedPath: string,
  roots: readonly string[],
  operation: "read" | "write" | "execute",
): string {
  const realPath = realpathDeepestExisting(resolvedPath);
  for (const root of roots) {
    if (isSameOrInside(realPath, root)) {
      return resolvedPath;
    }
  }
  throw createCoreError(
    CoreErrorType.ToolExecutionFailed,
    `Tool ${operation} path escapes the authorized workspace: ${resolvedPath}`,
    {
      context: {
        operation,
        path: resolvedPath,
      },
      recoverable: false,
    },
  );
}

/**
 * Realpath the deepest EXISTING ancestor and re-attach the not-yet-created
 * remainder, so write targets that do not exist yet are still validated
 * against the directory their parent chain really resolves to.
 */
function realpathDeepestExisting(path: string): string {
  let cursor = normalize(path);
  const suffix: string[] = [];
  for (;;) {
    try {
      const realCursor = realpathSync(cursor);
      if (suffix.length === 0) return realCursor;
      return normalize(join(realCursor, ...suffix));
    } catch {
      const parent = dirname(cursor);
      if (parent === cursor) {
        // Reached the filesystem root without an existing ancestor; fall back
        // to the lexical path (containment then decides on normalized text).
        return normalize(path);
      }
      suffix.unshift(basename(cursor));
      cursor = parent;
    }
  }
}

function isSameOrInside(realPath: string, root: string): boolean {
  if (realPath === root) return true;
  const boundary = root.endsWith(sep) ? root : root + sep;
  return realPath.startsWith(boundary);
}

function normalizeAbsoluteDirectory(value: string, label: string): string {
  const normalized = normalize(value);
  if (!isAbsolute(normalized)) {
    throw createCoreError(
      CoreErrorType.ConfigurationError,
      `${label} must be an absolute path for tool execution`,
      {
        context: {
          [label]: value,
        },
        recoverable: false,
      },
    );
  }
  return normalized;
}
