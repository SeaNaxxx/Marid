import type { QodexRuntimeEnv } from "./runtimeEnv.js";

export type QodexEnv = "test" | "production";
/** 安装包身份：决定应用名、app id、Electron 数据目录与更新策略；与后端环境 `QodexEnv` 是两个轴。 */
export type QodexProductFlavor = "production" | "preview";
export type ArmsRumEnv = "local" | "prod";

// 非构建环境（如 e2e 测试的 mocha）下 define 不存在，用 typeof 检查 + fallback 避免 ReferenceError
declare const __QODEX_ENV__: string;
declare const __QODEX_PRODUCT_FLAVOR__: string;
declare const __QODEX_ARMS_RUM_ENDPOINT__: string;
declare const __QODEX_TELEMETRY_REPORT_ENDPOINT__: string;
declare const __QODEX_AUTO_UPDATE_ENABLED__: boolean;

export function normalizeQodexEnv(value: string | undefined): QodexEnv {
  return value?.trim().toLowerCase() === "production" ? "production" : "test";
}

export const QODEX_ENV = normalizeQodexEnv(
  typeof __QODEX_ENV__ !== "undefined" ? __QODEX_ENV__ : undefined,
);

/**
 * 身份缺省跟随后端环境（test → preview，production → production）。
 * 桌面构建通过 `QODEX_PREVIEW_IDENTITY=1` 显式注入 preview，得到连接生产后端的 Preview 包；
 * 未注入 define 的 bundle（web、CLI、测试）沿用旧的单轴语义。
 */
export function normalizeQodexProductFlavor(
  value: string | undefined,
  qodexEnv: QodexEnv,
): QodexProductFlavor {
  const normalized = value?.trim().toLowerCase();
  if (normalized === "production" || normalized === "preview") {
    return normalized;
  }
  return qodexEnv === "production" ? "production" : "preview";
}

export const QODEX_PRODUCT_FLAVOR = normalizeQodexProductFlavor(
  typeof __QODEX_PRODUCT_FLAVOR__ !== "undefined" ? __QODEX_PRODUCT_FLAVOR__ : undefined,
  QODEX_ENV,
);
export const QODEX_APP_VERSION_ENV = "QODEX_APP_VERSION" as const;
export const QODEX_BUILD_COMMIT_ID_ENV = "QODEX_BUILD_COMMIT_ID" as const;
/**
 * Desktop 启动 Agent 子进程时写入的当前界面语言（BCP-47），供 Agent 模型请求的 X-Client-Language。
 * Agent 进程 Intl 只代表系统语言（Dock 启动无 LANG 时回退 en-US），所以由 Host 在 spawn 时显式下发；
 * 规则见 apps/qodex-cli/docs/design/v2/model/README.md。
 */
export const QODEX_CLIENT_LANGUAGE_ENV = "QODEX_CLIENT_LANGUAGE" as const;

// ── 运行时环境变量（不经过编译打包，启动时从 process.env 读取） ──
// 启用调试模式，值为 inspect-brk 的端口号，如 QODEX_DEBUG=9230
export const RUNTIME_QODEX_DEBUG =
  typeof process !== "undefined" ? process.env.QODEX_DEBUG : undefined;

// ── 构建期可选能力（见 docs/desktop/build-time-optional-capabilities.md），未注入时一律关闭 ──
/** ARMS RUM 上报地址；为空时 SDK 以 enable=false 初始化，不产生上报。 */
export const QODEX_ARMS_RUM_ENDPOINT: string =
  typeof __QODEX_ARMS_RUM_ENDPOINT__ !== "undefined" ? __QODEX_ARMS_RUM_ENDPOINT__ : "";
/** 事件上报（数仓）地址；为空时不上报。 */
export const QODEX_TELEMETRY_REPORT_ENDPOINT: string =
  typeof __QODEX_TELEMETRY_REPORT_ENDPOINT__ !== "undefined"
    ? __QODEX_TELEMETRY_REPORT_ENDPOINT__
    : "";
/** 桌面自动更新是否启用。 */
export const QODEX_AUTO_UPDATE_ENABLED: boolean =
  typeof __QODEX_AUTO_UPDATE_ENABLED__ !== "undefined" ? __QODEX_AUTO_UPDATE_ENABLED__ : false;

/** 将本地运行态与编译期 QODEX_ENV 映射为 ARMS 控制台识别的上报环境标签 */
export function mapQodexEnvToArmsRumEnv(runtimeEnv: QodexRuntimeEnv): ArmsRumEnv {
  return runtimeEnv !== "development" && QODEX_ENV === "production" ? "prod" : "local";
}
