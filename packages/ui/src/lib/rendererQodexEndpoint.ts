import {
  buildRuntimeQodexEndpointUrls,
  QODEX_ENV,
  type RuntimeQodexEndpointEnv,
} from "@qodex/shared";

interface RendererImportMetaEnv {
  VITE_QODEX_BASE_URL?: string;
  VITE_QODEX_ENDPOINT_ORIGIN?: string;
}

function readRendererImportMetaEnv(): RendererImportMetaEnv {
  return ((import.meta as ImportMeta & { env?: RendererImportMetaEnv }).env ??
    {}) as RendererImportMetaEnv;
}

export function createRendererQodexEndpointEnv(
  env: RendererImportMetaEnv = readRendererImportMetaEnv(),
): RuntimeQodexEndpointEnv {
  return {
    QODEX_ENV,
    // UI 侧的 qodex-plan 占位 provider 以前只看 QODEX_ENV，
    // 没有消费 Vite 注入的 base url，导致自定义测试域名时 renderer 和 host/service 可能不一致。
    QODEX_BASE_URL: env.VITE_QODEX_BASE_URL,
    QODEX_ENDPOINT_ORIGIN: env.VITE_QODEX_ENDPOINT_ORIGIN,
  };
}

export const RENDERER_QODEX_ENDPOINT_URLS = buildRuntimeQodexEndpointUrls(
  createRendererQodexEndpointEnv(),
);
