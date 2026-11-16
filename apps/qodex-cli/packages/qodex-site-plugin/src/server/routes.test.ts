import { describe, expect, it } from "vitest";
import { resourcePath } from "./routes.ts";

const SITE = "1978123456789012480";
const DEP = "1978123456789012481";

describe("resourcePath", () => {
  it.each([
    ["qodex-site://usage", "/api/v1/sites/usage"],
    ["qodex-site://plans", "/api/v1/sites/plans"],
    ["qodex-site://sites", "/api/v1/sites"],
    ["qodex-site://sites?cursor=", "/api/v1/sites"],
    [
      "qodex-site://sites?cursor=a%2Fb&limit=20",
      "/api/v1/sites?cursor=a%2Fb&limit=20",
    ],
    [`qodex-site://sites/${SITE}`, `/api/v1/sites/${SITE}`],
    [
      `qodex-site://sites/${SITE}/deployments?cursor=x`,
      `/api/v1/sites/${SITE}/deployments?cursor=x`,
    ],
    [
      `qodex-site://sites/${SITE}/deployments/${DEP}`,
      `/api/v1/sites/${SITE}/deployments/${DEP}`,
    ],
    [`qodex-site://sites/${SITE}/cover`, `/api/v1/sites/${SITE}/cover`],
    [`qodex-site://sites/${SITE}/analytics`, `/api/v1/sites/${SITE}/analytics`],
    [
      `qodex-site://sites/${SITE}/analytics?range=30d`,
      `/api/v1/sites/${SITE}/analytics?range=30d`,
    ],
    [
      `qodex-site://sites/analytics/pv24h?siteIds=${SITE},${DEP}`,
      `/api/v1/sites/analytics/pv24h?siteIds=${SITE}%2C${DEP}`,
    ],
    [
      `qodex-site://sites/${SITE}/deployments/${DEP}/logs?offset=12`,
      `/api/v1/sites/${SITE}/deployments/${DEP}/logs?offset=12`,
    ],
  ])("maps %s", (uri, path) => {
    expect(resourcePath(uri)).toBe(path);
  });

  it.each([
    "qodex-site://other",
    "https://sites/1",
    "qodex-site://usage?cursor=1",
    "qodex-site://plans?limit=1",
    "qodex-site://plans/free",
    "qodex-site://sites/abc",
    "qodex-site://sites/0123",
    "qodex-site://sites/1/deployments/2/logs/extra",
    "qodex-site://sites/1/cover/2",
    "qodex-site://sites/1/cover?offset=1",
    "qodex-site://sites/1/settings",
    "qodex-site://sites/1/deployments/2/logs?offset=-1",
    "qodex-site://sites?limit=1e3",
    "qodex-site://sites?cursor=a&cursor=b",
    "qodex-site://sites/1/deployments/2?offset=1",
    "qodex-site://user@sites",
    "qodex-site://sites/..%2Fusage",
    "qodex-site://sites/1/analytics?range=1y",
    "qodex-site://sites/1/analytics?offset=1",
    "qodex-site://sites/1/analytics/pages",
    "qodex-site://sites/analytics",
    "qodex-site://sites/analytics/pv24h?siteIds=1,abc",
    `qodex-site://sites/analytics/pv24h?siteIds=${Array(21).fill("1").join(",")}`,
    "qodex-site://sites/analytics/pv24h?range=24h",
    "not a uri",
  ])("rejects %s", (uri) => {
    expect(resourcePath(uri)).toBeUndefined();
  });
});
