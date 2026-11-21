import { mkdirSync, mkdtempSync, realpathSync, rmSync, symlinkSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { afterEach, beforeEach, describe, expect, it } from "vitest";
import {
  readWorkflowScriptFile,
  workflowScriptPathContainment,
} from "../src/tool/handlers/workflow-path-source.js";

/**
 * Security boundary regression tests (threat model M-08): the workflow `path`
 * readers (CreateWorkflow.path / AmendWorkflow.path / SaveWorkflow.script_path /
 * EvalWorkflowSnippet.path / FillWorkflowHole.path) must resolve model-supplied
 * paths through the same workspace containment as the filesystem tools, with a
 * named structured failure — never a thrown error, never a silent read.
 */
describe("workflow script path containment", () => {
  let root: string;
  let workspace: string;
  let outside: string;

  beforeEach(() => {
    root = realpathSync(mkdtempSync(join(tmpdir(), "qodex-workflow-path-")));
    workspace = join(root, "workspace");
    outside = join(root, "outside");
    mkdirSync(join(workspace, "scripts"), { recursive: true });
    mkdirSync(outside, { recursive: true });
    writeFileSync(join(workspace, "scripts", "main.dwf.ts"), "// inside\n");
    writeFileSync(join(outside, "secret.txt"), "outside secret\n");
  });

  afterEach(() => {
    rmSync(root, { recursive: true, force: true });
  });

  it("allows a relative path inside the workspace", () => {
    const result = workflowScriptPathContainment({
      cwd: workspace,
      inputPath: "scripts/main.dwf.ts",
      workspaceRoot: workspace,
    });
    expect(result).toEqual({ ok: true, absolute: join(workspace, "scripts", "main.dwf.ts") });
  });

  it("allows an absolute path inside the workspace", () => {
    const result = workflowScriptPathContainment({
      cwd: join(workspace, "scripts"),
      inputPath: join(workspace, "scripts", "main.dwf.ts"),
      workspaceRoot: workspace,
    });
    expect(result).toEqual({ ok: true, absolute: join(workspace, "scripts", "main.dwf.ts") });
  });

  it("rejects ../ traversal out of the workspace with the named failure", () => {
    const result = workflowScriptPathContainment({
      cwd: join(workspace, "scripts"),
      inputPath: "../../../outside/secret.txt",
      workspaceRoot: workspace,
    });
    expect(result.ok).toBe(false);
    if (!result.ok) expect(result.message).toMatch(/^workflow_script_file_outside_workspace:/);
  });

  it("rejects an absolute path outside the workspace with the named failure", () => {
    const result = workflowScriptPathContainment({
      cwd: workspace,
      inputPath: join(outside, "secret.txt"),
      workspaceRoot: workspace,
    });
    expect(result.ok).toBe(false);
    if (!result.ok) expect(result.message).toMatch(/^workflow_script_file_outside_workspace:/);
  });

  it("rejects a sibling-prefix root escape (workspace vs workspace-evil)", () => {
    const evil = join(root, "workspace-evil");
    mkdirSync(evil, { recursive: true });
    writeFileSync(join(evil, "sneaky.dwf.ts"), "// evil\n");
    const result = workflowScriptPathContainment({
      cwd: workspace,
      inputPath: join(evil, "sneaky.dwf.ts"),
      workspaceRoot: workspace,
    });
    expect(result.ok).toBe(false);
    if (!result.ok) expect(result.message).toMatch(/^workflow_script_file_outside_workspace:/);
  });

  it("rejects a symlink planted inside the workspace pointing outside", () => {
    symlinkSync(join(outside, "secret.txt"), join(workspace, "leak.dwf.ts"));
    const result = workflowScriptPathContainment({
      cwd: workspace,
      inputPath: "leak.dwf.ts",
      workspaceRoot: workspace,
    });
    expect(result.ok).toBe(false);
    if (!result.ok) expect(result.message).toMatch(/^workflow_script_file_outside_workspace:/);
  });

  it("keeps the legacy pure-resolve behavior for a bare context without a root", () => {
    // 裸 context（无 workspaceRoot）维持旧行为：不发明根，也不拦——单元测试直造的
    // resolution context 与未接线的宿主依赖这一点。
    const result = workflowScriptPathContainment({
      cwd: workspace,
      inputPath: join(outside, "secret.txt"),
    });
    expect(result).toEqual({ ok: true, absolute: join(outside, "secret.txt") });
  });

  it("readWorkflowScriptFile returns the named failure without reading the file", async () => {
    const read = await readWorkflowScriptFile({
      cwd: workspace,
      inputPath: join(outside, "secret.txt"),
      workspaceRoot: workspace,
    });
    expect(read.ok).toBe(false);
    if (!read.ok) {
      expect(read.code).toBe("outside_workspace");
      expect(read.message).toMatch(/^workflow_script_file_outside_workspace:/);
    }
  });

  it("readWorkflowScriptFile reads an inside-workspace file end to end", async () => {
    const read = await readWorkflowScriptFile({
      cwd: workspace,
      inputPath: "scripts/main.dwf.ts",
      workspaceRoot: workspace,
    });
    expect(read.ok).toBe(true);
    if (read.ok) {
      expect(read.file.source).toBe("// inside\n");
      expect(read.file.path).toBe(join(workspace, "scripts", "main.dwf.ts"));
      expect(read.file.described).toBe(join("scripts", "main.dwf.ts"));
    }
  });
});
