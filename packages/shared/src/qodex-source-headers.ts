import { DEFAULT_QODEX_ENDPOINT_ORIGIN } from "./qodexEndpoint.js";

export const QODEX_SOURCE_HEADERS = {
  "User-Agent": "Qodex/unknown",
  "HTTP-Referer": DEFAULT_QODEX_ENDPOINT_ORIGIN,
  "X-Title": "Z Code@electron",
} as const;

export interface BuildQodexSourceHeadersFromContextOptions {
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

export function normalizeQodexSourceHeaderValue(value: string | undefined): string | undefined {
  const trimmed = value?.trim();
  if (!trimmed || !/^[\x20-\x7e]+$/.test(trimmed)) {
    return undefined;
  }
  return trimmed;
}

export function buildQodexSourceHeadersFromContext(
  options: BuildQodexSourceHeadersFromContextOptions = {},
): Record<string, string> {
  const appVersion = normalizeQodexSourceHeaderValue(options.appVersion);
  const arch = normalizeQodexSourceHeaderValue(options.arch);
  const clientLanguage = normalizeQodexSourceHeaderValue(options.clientLanguage) ?? "unknown";
  const clientTimezone = normalizeQodexSourceHeaderValue(options.clientTimezone) ?? "unknown";
  const deviceMid = normalizeQodexSourceHeaderValue(options.deviceMid);
  const endpointOrigin =
    normalizeQodexSourceHeaderValue(options.endpointOrigin) ?? DEFAULT_QODEX_ENDPOINT_ORIGIN;
  const osVersion = normalizeQodexSourceHeaderValue(options.osVersion);
  const platform = normalizeQodexSourceHeaderValue(options.platform);
  const releaseChannel = normalizeQodexSourceHeaderValue(options.releaseChannel);
  const sourceTitle = normalizeQodexSourceHeaderValue(options.sourceTitle) ?? "electron";

  return {
    ...QODEX_SOURCE_HEADERS,
    "HTTP-Referer": endpointOrigin,
    "User-Agent": `Qodex/${appVersion ?? "unknown"}`,
    ...(appVersion ? { "X-Qodex-App-Version": appVersion } : {}),
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
