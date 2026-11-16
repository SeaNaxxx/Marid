import { existsSync, mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { join } from "node:path";
import type { MaridStdioTapDevState } from "@marid/shared";
import { getAppConfigDir } from "#src/paths.js";
import { isEffectiveDevelopmentNodeEnv } from "#src/runtime-tools/nodeEnv.js";

interface MaridStdioTapStateFile {
  enabled?: boolean;
}

export function isMaridStdioTapDevVisible(): boolean {
  return isEffectiveDevelopmentNodeEnv();
}

export function getMaridStdioTapDevDir(): string {
  return join(getAppConfigDir(), "dev");
}

export function getMaridStdioTapDevLogDir(): string {
  return join(getMaridStdioTapDevDir(), "stdio-traffic");
}

export function getMaridStdioTapDevStatePath(): string {
  return join(getMaridStdioTapDevDir(), "marid-stdio-tap.json");
}

function readStateFile(path: string): MaridStdioTapStateFile {
  if (!existsSync(path)) {
    return {};
  }

  try {
    const parsed = JSON.parse(readFileSync(path, "utf-8")) as unknown;
    return parsed && typeof parsed === "object" ? (parsed as MaridStdioTapStateFile) : {};
  } catch {
    return {};
  }
}

export function readMaridStdioTapDevState(): MaridStdioTapDevState {
  const visible = isMaridStdioTapDevVisible();
  const statePath = getMaridStdioTapDevStatePath();
  const fileState = readStateFile(statePath);
  return {
    enabled: visible && fileState.enabled === true,
    visible,
    logDir: getMaridStdioTapDevLogDir(),
    statePath,
  };
}

export function setMaridStdioTapDevEnabled(enabled: boolean): MaridStdioTapDevState {
  const visible = isMaridStdioTapDevVisible();
  const statePath = getMaridStdioTapDevStatePath();
  mkdirSync(getMaridStdioTapDevDir(), { recursive: true });
  writeFileSync(
    statePath,
    `${JSON.stringify(
      {
        // 开发态 stdio 抓包是高频原始协议帧，只能通过显式开关写旁路文件，避免误进生产日志。
        enabled: visible && enabled,
        updatedAt: new Date().toISOString(),
      },
      null,
      2,
    )}\n`,
  );
  return readMaridStdioTapDevState();
}
