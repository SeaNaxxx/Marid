import {
  buildRuntimeMaridEndpointUrls,
  MARID_ENV,
  type RuntimeMaridEndpointEnv,
} from "@marid/shared";

interface RendererImportMetaEnv {
  VITE_MARID_BASE_URL?: string;
  VITE_MARID_ENDPOINT_ORIGIN?: string;
}

function readRendererImportMetaEnv(): RendererImportMetaEnv {
  return ((import.meta as ImportMeta & { env?: RendererImportMetaEnv }).env ??
    {}) as RendererImportMetaEnv;
}

export function createRendererMaridEndpointEnv(
  env: RendererImportMetaEnv = readRendererImportMetaEnv(),
): RuntimeMaridEndpointEnv {
  return {
    MARID_ENV,
    // UI 侧的 marid-plan 占位 provider 以前只看 MARID_ENV，
    // 没有消费 Vite 注入的 base url，导致自定义测试域名时 renderer 和 host/service 可能不一致。
    MARID_BASE_URL: env.VITE_MARID_BASE_URL,
    MARID_ENDPOINT_ORIGIN: env.VITE_MARID_ENDPOINT_ORIGIN,
  };
}

export const RENDERER_MARID_ENDPOINT_URLS = buildRuntimeMaridEndpointUrls(
  createRendererMaridEndpointEnv(),
);
