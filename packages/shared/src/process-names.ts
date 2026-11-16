const MARID_PROCESS_PREFIX = "marid";
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

function joinMaridProcessName(...segments: Array<string | null | undefined>): string {
  const sanitizedSegments = segments
    .map((segment) => sanitizeProcessNameSegment(segment))
    .filter((segment): segment is string => Boolean(segment));
  return [MARID_PROCESS_PREFIX, ...sanitizedSegments].join("-");
}

function pickWorkspaceTag(workspacePath: string | null | undefined): string | undefined {
  const trimmedPath = workspacePath?.trim();
  if (!trimmedPath) {
    return undefined;
  }

  const parts = trimmedPath.split(/[\\/]+/).filter(Boolean);
  return parts.at(-1) ?? trimmedPath;
}

export function formatMaridMainProcessName(): string {
  return joinMaridProcessName("main");
}

export function formatMaridGpuProcessName(): string {
  return joinMaridProcessName("gpu");
}

export function formatMaridHostProcessName(label?: string): string {
  return joinMaridProcessName("host", label);
}

export function formatMaridRendererProcessName(windowTitle?: string): string {
  const normalizedTitle = windowTitle?.trim();
  if (!normalizedTitle || normalizedTitle === "Marid") {
    return joinMaridProcessName("renderer", "main");
  }

  if (normalizedTitle === "Resource Manager") {
    return joinMaridProcessName("renderer", "resource-manager");
  }

  const remoteWindowPrefix = "Marid - ";
  if (normalizedTitle.startsWith(remoteWindowPrefix)) {
    return joinMaridProcessName(
      "renderer",
      "remote",
      normalizedTitle.slice(remoteWindowPrefix.length),
    );
  }

  return joinMaridProcessName("renderer", normalizedTitle);
}

export function formatMaridAgentProcessName(provider: string, workspacePath?: string): string {
  return joinMaridProcessName("agent", provider, pickWorkspaceTag(workspacePath));
}

export function formatMaridUtilityProcessName(name?: string, type = "utility"): string {
  return joinMaridProcessName(type, name);
}
