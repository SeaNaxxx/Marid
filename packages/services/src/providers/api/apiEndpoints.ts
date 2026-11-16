import { buildRuntimeMaridApiUrl, resolveZaiBusinessBaseUrl } from "@marid/shared";

export const MARID_CLIENT_SCENES_URL = buildRuntimeMaridApiUrl(
  process.env,
  "/api/v1/client/scenes",
);

export const ZAI_API_HOST = resolveZaiBusinessBaseUrl(process.env);
