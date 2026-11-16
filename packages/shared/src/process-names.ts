const QODEX_PROCESS_PREFIX = "qodex";
const MAX_PROCESS_NAME_SEGMENT_LENGTH = 24;

function sanitizeProcessNameSegment(value: string | null | undefined): string | null {
  if (!value) {
    return null;
  }

  const normalized = value
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "");
  if (!normalized) {
    return null;
  }

  return normalized.slice(0, MAX_PROCESS_NAME_SEGMENT_LENGTH);
}

function joinQodexProcessName(...segments: Array<string | null | undefined>): string {
  const sanitizedSegments = segments
    .map((segment) => sanitizeProcessNameSegment(segment))
    .filter((segment): segment is string => Boolean(segment));
  return [QODEX_PROCESS_PREFIX, ...sanitizedSegments].join("-");
}

function pickWorkspaceTag(workspacePath: string | null | undefined): string | undefined {
  const trimmedPath = workspacePath?.trim();
  if (!trimmedPath) {
    return undefined;
  }

  const parts = trimmedPath.split(/[\\/]+/).filter(Boolean);
  return parts.at(-1) ?? trimmedPath;
}

export function formatQodexMainProcessName(): string {
  return joinQodexProcessName("main");
}

export function formatQodexGpuProcessName(): string {
  return joinQodexProcessName("gpu");
}

export function formatQodexHostProcessName(label?: string): string {
  return joinQodexProcessName("host", label);
}

export function formatQodexRendererProcessName(windowTitle?: string): string {
  const normalizedTitle = windowTitle?.trim();
  if (!normalizedTitle || normalizedTitle === "Qodex") {
    return joinQodexProcessName("renderer", "main");
  }

  if (normalizedTitle === "Resource Manager") {
    return joinQodexProcessName("renderer", "resource-manager");
  }

  const remoteWindowPrefix = "Qodex - ";
  if (normalizedTitle.startsWith(remoteWindowPrefix)) {
    return joinQodexProcessName(
      "renderer",
      "remote",
      normalizedTitle.slice(remoteWindowPrefix.length),
    );
  }

  return joinQodexProcessName("renderer", normalizedTitle);
}

export function formatQodexAgentProcessName(provider: string, workspacePath?: string): string {
  return joinQodexProcessName("agent", provider, pickWorkspaceTag(workspacePath));
}

export function formatQodexUtilityProcessName(name?: string, type = "utility"): string {
  return joinQodexProcessName(type, name);
}
