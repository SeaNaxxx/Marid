import { existsSync, mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { join } from "node:path";
import type { QodexStdioTapDevState } from "@qodex/shared";
import { getAppConfigDir } from "#src/paths.js";
import { isEffectiveDevelopmentNodeEnv } from "#src/runtime-tools/nodeEnv.js";

interface QodexStdioTapStateFile {
  enabled?: boolean;
}

export function isQodexStdioTapDevVisible(): boolean {
  return isEffectiveDevelopmentNodeEnv();
}

export function getQodexStdioTapDevDir(): string {
  return join(getAppConfigDir(), "dev");
}

export function getQodexStdioTapDevLogDir(): string {
  return join(getQodexStdioTapDevDir(), "stdio-traffic");
}

export function getQodexStdioTapDevStatePath(): string {
  return join(getQodexStdioTapDevDir(), "qodex-stdio-tap.json");
}

function readStateFile(path: string): QodexStdioTapStateFile {
  if (!existsSync(path)) {
    return {};
  }

  try {
    const parsed = JSON.parse(readFileSync(path, "utf-8")) as unknown;
    return parsed && typeof parsed === "object" ? (parsed as QodexStdioTapStateFile) : {};
  } catch {
    return {};
  }
}

export function readQodexStdioTapDevState(): QodexStdioTapDevState {
  const visible = isQodexStdioTapDevVisible();
  const statePath = getQodexStdioTapDevStatePath();
  const fileState = readStateFile(statePath);
  return {
    enabled: visible && fileState.enabled === true,
    visible,
    logDir: getQodexStdioTapDevLogDir(),
    statePath,
  };
}

export function setQodexStdioTapDevEnabled(enabled: boolean): QodexStdioTapDevState {
  const visible = isQodexStdioTapDevVisible();
  const statePath = getQodexStdioTapDevStatePath();
  mkdirSync(getQodexStdioTapDevDir(), { recursive: true });
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
  return readQodexStdioTapDevState();
}
