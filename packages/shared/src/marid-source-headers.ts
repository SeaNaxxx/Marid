import { DEFAULT_MARID_ENDPOINT_ORIGIN } from "./maridEndpoint.js";

export const MARID_SOURCE_HEADERS = {
  "User-Agent": "Marid/unknown",
  "HTTP-Referer": DEFAULT_MARID_ENDPOINT_ORIGIN,
  "X-Title": "Z Code@electron",
} as const;

export interface BuildMaridSourceHeadersFromContextOptions {
  appVersion?: string;
  arch?: string;
  clientLanguage?: string;
  clientTimezone?: string;
  deviceMid?: string;
  endpointOrigin?: string;
  osVersion?: string;
  platform?: string;
  releaseChannel?: string;
  sourceTitle?: string;
}

export function normalizeMaridSourceHeaderValue(value: string | undefined): string | undefined {
  const trimmed = value?.trim();
  if (!trimmed || !/^[\x20-\x7e]+$/.test(trimmed)) {
    return undefined;
  }
  return trimmed;
}

export function buildMaridSourceHeadersFromContext(
  options: BuildMaridSourceHeadersFromContextOptions = {},
): Record<string, string> {
  const appVersion = normalizeMaridSourceHeaderValue(options.appVersion);
  const arch = normalizeMaridSourceHeaderValue(options.arch);
  const clientLanguage = normalizeMaridSourceHeaderValue(options.clientLanguage) ?? "unknown";
  const clientTimezone = normalizeMaridSourceHeaderValue(options.clientTimezone) ?? "unknown";
  const deviceMid = normalizeMaridSourceHeaderValue(options.deviceMid);
  const endpointOrigin =
    normalizeMaridSourceHeaderValue(options.endpointOrigin) ?? DEFAULT_MARID_ENDPOINT_ORIGIN;
  const osVersion = normalizeMaridSourceHeaderValue(options.osVersion);
  const platform = normalizeMaridSourceHeaderValue(options.platform);
  const releaseChannel = normalizeMaridSourceHeaderValue(options.releaseChannel);
  const sourceTitle = normalizeMaridSourceHeaderValue(options.sourceTitle) ?? "electron";

  return {
    ...MARID_SOURCE_HEADERS,
    "HTTP-Referer": endpointOrigin,
    "User-Agent": `Marid/${appVersion ?? "unknown"}`,
    ...(appVersion ? { "X-Marid-App-Version": appVersion } : {}),
    "X-Title": `Z Code@${sourceTitle}`,
    ...(platform && arch ? { "X-Platform": `${platform}-${arch}` } : {}),
    ...(releaseChannel ? { "X-Release-Channel": releaseChannel } : {}),
    "X-Client-Language": clientLanguage,
    "X-Client-Timezone": clientTimezone,
    ...(platform ? { "X-Os-Category": normalizeOsCategory(platform) } : {}),
    ...(osVersion ? { "X-Os-Version": osVersion } : {}),
    ...(deviceMid ? { "X-Device-Mid": deviceMid } : {}),
  };
}

function normalizeOsCategory(platform: string): string {
  switch (platform) {
    case "darwin":
      return "macos";
    case "win32":
      return "windows";
    default:
      return "linux";
  }
}
