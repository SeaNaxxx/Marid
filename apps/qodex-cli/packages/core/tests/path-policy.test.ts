import { mkdtempSync, mkdirSync, realpathSync, rmSync, symlinkSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join, sep } from "node:path";
import { afterEach, beforeEach, describe, expect, it } from "vitest";
import { resolveToolWorkingDirectory, resolveWorkspacePath } from "../src/tool/path-policy.js";

/**
 * Security boundary regression tests (audit C-01): filesystem tools must be
 * confined to the workspace root, with symlink-aware containment, explicit
 * authorization for external roots, and deterministic rejection of escapes.
 */
describe("resolveWorkspacePath workspace containment", () => {
  let root: string;
  let workspace: string;

  beforeEach(() => {
    root = realpathSync(mkdtempSync(join(tmpdir(), "qodex-path-policy-")));
    workspace = join(root, "workspace");
    mkdirSync(join(workspace, "src"), { recursive: true });
    writeFileSync(join(workspace, "src", "index.ts"), "export {};\n");
  });

  afterEach(() => {
    rmSync(root, { recursive: true, force: true });
  });

  it("allows relative paths inside the workspace", () => {
    const resolved = resolveWorkspacePath({
      inputPath: "src/index.ts",
      workingDirectory: workspace,
      workspaceRoot: workspace,
      operation: "read",
    });
    expect(resolved).toBe(join(workspace, "src", "index.ts"));
  });

  it("allows absolute paths inside the workspace", () => {
    const resolved = resolveWorkspacePath({
      inputPath: join(workspace, "src", "index.ts"),
      workingDirectory: workspace,
      workspaceRoot: workspace,
      operation: "read",
    });
    expect(resolved).toBe(join(workspace, "src", "index.ts"));
  });

  it("rejects ../ traversal out of the workspace", () => {
    expect(() =>
      resolveWorkspacePath({
        inputPath: "../outside.txt",
        workingDirectory: workspace,
        workspaceRoot: workspace,
        operation: "read",
      }),
    ).toThrow(/escapes the authorized workspace/);
  });

  it("rejects deep ../ traversal that lands back inside then escapes", () => {
    expect(() =>
      resolveWorkspacePath({
        inputPath: "src/../../outside.txt",
        workingDirectory: workspace,
        workspaceRoot: workspace,
        operation: "write",
      }),
    ).toThrow(/escapes the authorized workspace/);
  });

  it("rejects absolute paths outside the workspace", () => {
    expect(() =>
      resolveWorkspacePath({
        inputPath: join(root, "sibling", "secret.txt"),
        workingDirectory: workspace,
        workspaceRoot: workspace,
        operation: "read",
      }),
    ).toThrow(/escapes the authorized workspace/);
  });

  it("rejects sibling-directory paths that merely share a string prefix", () => {
    const sibling = workspace + "-sibling";
    mkdirSync(sibling, { recursive: true });
    expect(() =>
      resolveWorkspacePath({
        inputPath: sibling,
        workingDirectory: workspace,
        workspaceRoot: workspace,
        operation: "read",
      }),
    ).toThrow(/escapes the authorized workspace/);
  });

  it("allows a write target that does not exist yet inside the workspace", () => {
    const resolved = resolveWorkspacePath({
      inputPath: "src/generated/new-file.ts",
      workingDirectory: workspace,
      workspaceRoot: workspace,
      operation: "write",
    });
    expect(resolved).toBe(join(workspace, "src", "generated", "new-file.ts"));
  });

  it("rejects a symlink inside the workspace that targets an outside file", () => {
    const outside = join(root, "outside.txt");
    writeFileSync(outside, "secret\n");
    symlinkSync(outside, join(workspace, "escape-link"));
    expect(() =>
      resolveWorkspacePath({
        inputPath: "escape-link",
        workingDirectory: workspace,
        workspaceRoot: workspace,
        operation: "read",
      }),
    ).toThrow(/escapes the authorized workspace/);
  });

  it("rejects a write through a symlinked parent directory that escapes", () => {
    const outsideDir = join(root, "outside-dir");
    mkdirSync(outsideDir, { recursive: true });
    symlinkSync(outsideDir, join(workspace, "dir-link"));
    expect(() =>
      resolveWorkspacePath({
        inputPath: "dir-link/evil.txt",
        workingDirectory: workspace,
        workspaceRoot: workspace,
        operation: "write",
      }),
    ).toThrow(/escapes the authorized workspace/);
  });

  it("allows a symlink inside the workspace that stays inside", () => {
    symlinkSync(join(workspace, "src"), join(workspace, "src-alias"));
    const resolved = resolveWorkspacePath({
      inputPath: "src-alias/index.ts",
      workingDirectory: workspace,
      workspaceRoot: workspace,
      operation: "read",
    });
    expect(resolved).toBe(join(workspace, "src-alias", "index.ts"));
  });

  it("allows paths under explicitly authorized external roots", () => {
    const outside = join(root, "authorized");
    mkdirSync(outside, { recursive: true });
    const resolved = resolveWorkspacePath({
      inputPath: join(outside, "data.json"),
      workingDirectory: workspace,
      workspaceRoot: workspace,
      operation: "read",
      authorizedRoots: [outside],
    });
    expect(resolved).toBe(join(outside, "data.json"));
  });

  it("keeps rejecting outside paths that are not in the authorized roots", () => {
    const authorized = join(root, "authorized");
    mkdirSync(authorized, { recursive: true });
    expect(() =>
      resolveWorkspacePath({
        inputPath: join(root, "unauthorized", "x.txt"),
        workingDirectory: workspace,
        workspaceRoot: workspace,
        operation: "read",
        authorizedRoots: [authorized],
      }),
    ).toThrow(/escapes the authorized workspace/);
  });

  it("rejects an empty path", () => {
    expect(() =>
      resolveWorkspacePath({
        inputPath: "",
        workingDirectory: workspace,
        workspaceRoot: workspace,
        operation: "read",
      }),
    ).toThrow(/must not be empty/);
  });

  it("rejects a working directory outside the workspace", () => {
    const outside = join(root, "elsewhere");
    mkdirSync(outside, { recursive: true });
    expect(() =>
      resolveWorkspacePath({
        inputPath: "file.txt",
        workingDirectory: outside,
        workspaceRoot: workspace,
        operation: "read",
      }),
    ).toThrow(/escapes the authorized workspace/);
  });

  it("rejects a relative working directory", () => {
    expect(() =>
      resolveWorkspacePath({
        inputPath: "file.txt",
        workingDirectory: "relative/cwd",
        workspaceRoot: workspace,
        operation: "read",
      }),
    ).toThrow(/must be an absolute path/);
  });
});

describe("resolveToolWorkingDirectory containment", () => {
  let root: string;
  let workspace: string;

  beforeEach(() => {
    root = realpathSync(mkdtempSync(join(tmpdir(), "qodex-cwd-policy-")));
    workspace = join(root, "workspace");
    mkdirSync(workspace, { recursive: true });
  });

  afterEach(() => {
    rmSync(root, { recursive: true, force: true });
  });

  it("returns the working directory when no cwd is supplied", () => {
    const cwd = resolveToolWorkingDirectory(undefined, {
      workingDirectory: workspace,
      workspaceRoot: workspace,
      operation: "execute",
    });
    expect(cwd).toBe(workspace);
  });

  it("rejects a cwd that escapes the workspace", () => {
    expect(() =>
      resolveToolWorkingDirectory("..", {
        workingDirectory: workspace,
        workspaceRoot: workspace,
        operation: "execute",
      }),
    ).toThrow(/escapes the authorized workspace/);
  });

  it("accepts a cwd inside the workspace", () => {
    const nested = join(workspace, "sub");
    mkdirSync(nested, { recursive: true });
    const cwd = resolveToolWorkingDirectory("sub", {
      workingDirectory: workspace,
      workspaceRoot: workspace,
      operation: "execute",
    });
    expect(cwd).toBe(nested);
  });

  it("keeps the workspace boundary at the filesystem root edge", () => {
    // A root of "/" contains every path — the boundary check must not throw.
    const resolved = resolveWorkspacePath({
      inputPath: join(sep, "etc", "hostname"),
      workingDirectory: workspace,
      workspaceRoot: sep,
      operation: "read",
    });
    expect(resolved).toBe(join(sep, "etc", "hostname"));
  });
});
