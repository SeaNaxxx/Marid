import { downloadQodexBuiltinRelease, type QodexBuiltinRelease } from "@qodex/provider-node";
import type { ApiClient, QodexJwtTokenResolver } from "@qodex/shared";

export interface FetchQodexBuiltinRemoteReleaseOptions {
  readonly apiClient: ApiClient;
  readonly endpointOrigin: string;
  readonly appVersion: string;
  readonly platform: string;
  readonly signal?: AbortSignal;
  readonly resolveQodexJwtToken?: QodexJwtTokenResolver;
}

/** Services 仅注入既有网络装配；URL、预算与 Release 校验由 provider-node 唯一实现。 */
export async function fetchQodexBuiltinRemoteRelease(
  options: FetchQodexBuiltinRemoteReleaseOptions,
): Promise<QodexBuiltinRelease | null> {
  return downloadQodexBuiltinRelease({
    endpointOrigin: options.endpointOrigin,
    appVersion: options.appVersion,
    platform: options.platform,
    signal: options.signal,
    resolveQodexJwtToken: options.resolveQodexJwtToken,
    request: (url, init) => options.apiClient.request(url, init),
  });
}
