import type { IPlatformService } from "@marid/shared";

type DesktopBrowserPlatformBridge = Pick<
  IPlatformService,
  | "onBrowserViewDownloadStarted"
  | "onBrowserViewReady"
  | "onBrowserViewOperation"
  | "onBrowserViewViewportChanged"
  | "onBrowserViewVisibility"
  | "onBrowserViewCloseTab"
  | "onBrowserViewSuspend"
  | "onBrowserViewRestore"
  | "browserViewLoadUrl"
  | "browserViewAttachGuest"
  | "browserViewDetachGuest"
  | "browserViewCloseTab"
  | "browserViewReportResidency"
  | "browserViewPromoteDraftTabs"
  | "browserViewSuspendReady"
  | "browserViewEnsureResident"
  | "browserViewRestoreTabs"
  | "browserViewUpdateViewport"
  | "importChromeBrowserData"
  | "clearEmbeddedBrowserData"
  | "getPathForFile"
  | "saveFile"
  | "printPageToPdf"
  | "microphone"
  | "captureBrowserGuestPage"
>;

// Rebase 集成：browser bridge 若继续内联在 renderer 入口，会让入口越过 max-lines 门禁。
// 独立对象只做 preload 委托与旧 bridge 兼容兜底，不持有 Browser 业务状态。
export const desktopBrowserPlatformBridge = {
  getPathForFile: (file) => window.marid.getPathForFile?.(file) ?? null,
  saveFile: (payload) =>
    window.marid.saveFile?.(payload) ?? Promise.resolve({ success: false, error: "not_supported" }),
  // 条件定义而非兜底返回失败：UI 靠方法是否存在做能力检测，旧 preload 下必须保持 undefined
  printPageToPdf: window.marid.printPageToPdf ? () => window.marid.printPageToPdf!() : undefined,
  // 与 printPageToPdf 一样按 preload 是否提供决定能力是否存在，UI 据此决定是否显示语音输入。
  microphone:
    window.marid.ensureMicrophoneAccess &&
    window.marid.resetMicrophoneAccess &&
    window.marid.openMicrophoneSettings
      ? {
          ensureAccess: () => window.marid.ensureMicrophoneAccess!(),
          resetAccess: () => window.marid.resetMicrophoneAccess!(),
          openSettings: (target) => window.marid.openMicrophoneSettings!(target),
        }
      : undefined,
  onBrowserViewDownloadStarted: (handler) =>
    window.marid.onBrowserViewDownloadStarted?.(handler) ?? (() => {}),
  captureBrowserGuestPage: window.marid.captureBrowserGuestPage
    ? (payload) => window.marid.captureBrowserGuestPage!(payload)
    : undefined,
  onBrowserViewReady: (handler) => window.marid.onBrowserViewReady?.(handler) ?? (() => {}),
  onBrowserViewOperation: (handler) => window.marid.onBrowserViewOperation?.(handler) ?? (() => {}),
  onBrowserViewViewportChanged: (handler) =>
    window.marid.onBrowserViewViewportChanged?.(handler) ?? (() => {}),
  onBrowserViewVisibility: (handler) =>
    window.marid.onBrowserViewVisibility?.(handler) ?? (() => {}),
  onBrowserViewCloseTab: (handler) => window.marid.onBrowserViewCloseTab?.(handler) ?? (() => {}),
  onBrowserViewSuspend: (handler) => window.marid.onBrowserViewSuspend?.(handler) ?? (() => {}),
  onBrowserViewRestore: (handler) => window.marid.onBrowserViewRestore?.(handler) ?? (() => {}),
  browserViewLoadUrl: (payload) => window.marid.browserViewLoadUrl!(payload),
  browserViewAttachGuest: (payload) =>
    window.marid.browserViewAttachGuest?.(payload) ??
    Promise.resolve({ ok: false, reason: "not-found", recoveryRequested: false }),
  browserViewDetachGuest: (payload) =>
    window.marid.browserViewDetachGuest?.(payload) ?? Promise.resolve(false),
  browserViewCloseTab: (payload) =>
    window.marid.browserViewCloseTab?.(payload) ?? Promise.resolve(),
  browserViewReportResidency: (payload) =>
    window.marid.browserViewReportResidency?.(payload) ?? Promise.resolve(),
  browserViewPromoteDraftTabs: (payload) =>
    window.marid.browserViewPromoteDraftTabs?.(payload) ?? Promise.resolve([]),
  browserViewSuspendReady: (payload) =>
    window.marid.browserViewSuspendReady?.(payload) ?? Promise.resolve(),
  browserViewEnsureResident: (payload) =>
    window.marid.browserViewEnsureResident?.(payload) ?? Promise.resolve(),
  browserViewRestoreTabs: (payload) =>
    window.marid.browserViewRestoreTabs?.(payload) ?? Promise.resolve([]),
  browserViewUpdateViewport: (payload) =>
    window.marid.browserViewUpdateViewport?.(payload) ?? Promise.resolve(),
  importChromeBrowserData: (options) =>
    window.marid.importChromeBrowserData?.(options) ??
    Promise.resolve({
      success: false,
      cookies: { imported: 0, skipped: 0, failed: 0 },
      localStorage: {
        originsImported: 0,
        entriesImported: 0,
        originsSkipped: 0,
        originsFailed: 0,
      },
      error: "unsupported",
    }),
  clearEmbeddedBrowserData: (mode) =>
    window.marid.clearEmbeddedBrowserData?.(mode) ??
    Promise.resolve({ success: false, error: "unsupported" }),
} satisfies DesktopBrowserPlatformBridge;
