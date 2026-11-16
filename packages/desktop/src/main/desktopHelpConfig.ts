import { net } from "electron";
import {
  buildHelpAppConfigUrl,
  buildQodexSourceHeadersFromContext,
  createHelpAppConfigReader,
  resolveOptionalQodexAuthorization,
  type QodexJwtTokenResolver,
  QODEX_ENV,
} from "@qodex/shared";

export function createDesktopHelpConfigReader(options: {
  resolveEndpointOrigin: () => Promise<string>;
  appVersion: string;
  deviceMid: string;
  /** 当前界面语言；以前不传，X-Client-Language 固定为 unknown。 */
  getClientLanguage: () => string;
  resolveQodexJwtToken?: QodexJwtTokenResolver;
}) {
  const read = createHelpAppConfigReader({
    fetchImpl: (input, init) => net.fetch(input instanceof URL ? input.href : input, init),
  });
  return async () => {
    const endpointOrigin = await options.resolveEndpointOrigin();
    const authorization = await resolveOptionalQodexAuthorization(options.resolveQodexJwtToken);
    return read(
      buildHelpAppConfigUrl(
        endpointOrigin,
        options.appVersion,
        `${process.platform}-${process.arch}`,
      ),
      {
        ...(authorization ? { Authorization: authorization } : {}),
        ...buildQodexSourceHeadersFromContext({
          endpointOrigin,
          appVersion: options.appVersion,
          clientLanguage: options.getClientLanguage(),
          deviceMid: options.deviceMid,
          platform: process.platform,
          arch: process.arch,
          releaseChannel: QODEX_ENV,
          sourceTitle: "electron",
        }),
      },
    );
  };
}
