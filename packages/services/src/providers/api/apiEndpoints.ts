import { buildRuntimeQodexApiUrl, resolveZaiBusinessBaseUrl } from "@qodex/shared";

export const QODEX_CLIENT_SCENES_URL = buildRuntimeQodexApiUrl(
  process.env,
  "/api/v1/client/scenes",
);

export const ZAI_API_HOST = resolveZaiBusinessBaseUrl(process.env);
