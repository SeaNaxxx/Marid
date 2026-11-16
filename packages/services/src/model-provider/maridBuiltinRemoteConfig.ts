import { downloadMaridBuiltinRelease, type MaridBuiltinRelease } from "@marid/provider-node";
import type { ApiClient, MaridJwtTokenResolver } from "@marid/shared";

export interface FetchMaridBuiltinRemoteReleaseOptions {
  readonly apiClient: ApiClient;
  readonly endpointOrigin: string;
  readonly appVersion: string;
  readonly platform: string;
  readonly signal?: AbortSignal;
  readonly resolveMaridJwtToken?: MaridJwtTokenResolver;
}

/** Services 仅注入既有网络装配；URL、预算与 Release 校验由 provider-node 唯一实现。 */
export async function fetchMaridBuiltinRemoteRelease(
  options: FetchMaridBuiltinRemoteReleaseOptions,
): Promise<MaridBuiltinRelease | null> {
  return downloadMaridBuiltinRelease({
    endpointOrigin: options.endpointOrigin,
    appVersion: options.appVersion,
    platform: options.platform,
    signal: options.signal,
    resolveMaridJwtToken: options.resolveMaridJwtToken,
    request: (url, init) => options.apiClient.request(url, init),
  });
}
