import { recordArmsCustomEventForE2E } from "@marid/ui";
import {
  DesktopCommandIds,
  buildLocalMediaPreviewUrl,
  resolveLocaleFromLanguageTag,
  unwrapRemoteWorkspaceBindResult,
  type IPlatformService,
} from "@marid/shared";

import { desktopBrowserPlatformBridge } from "./desktopBrowserPlatformBridge.js";
import { registerAppControlNavigationHandler } from "#src/app-control-navigation/renderer.js";
import { createDesktopPluginSandboxPlatform } from "./plugin-sandbox/desktopPluginSandboxPlatform.js";

export function createDesktopPlatform(options: {
  isLocalDevelopmentRuntime: boolean;
}): IPlatformService {
  return {
    onAppControlNavigation: (handler) =>
      registerAppControlNavigationHandler(
        {
          subscribe: (listener) => window.marid.onAppControlNavigationMessage(listener),
          register: (generation) =>
            window.marid.sendAppControlNavigation({ type: "register", generation }),
          unregister: (generation) =>
            window.marid.sendAppControlNavigation({ type: "unregister", generation }),
          reply: (message) => window.marid.sendAppControlNavigation({ type: "result", ...message }),
        },
        handler,
        crypto.randomUUID(),
      ),
    canSelectFilePath: true,
    canOpenLocalFiles: true,
    pluginSandbox: createDesktopPluginSandboxPlatform(),
    createLocalMediaPreviewUrl: buildLocalMediaPreviewUrl,
    isLocalDevelopmentRuntime: options.isLocalDevelopmentRuntime,
    selectDirectory: () => window.marid.selectDirectory(),
    selectDirectories: () => window.marid.selectDirectories(),
    selectFile: () => window.marid.selectFile(),
    selectFiles: () => window.marid.selectFiles?.() ?? Promise.resolve([]),
    createTempTextAttachment: (payload) => window.marid.createTempTextAttachment(payload),
    materializeWorkflowArtifactFile: (payload) =>
      window.marid.materializeWorkflowArtifactFile(payload),
    onRemoteConnectionLog: (handler) => window.marid.onRemoteConnectionLog(handler),
    onRemoteSessionClosed: (handler) => window.marid.onRemoteSessionClosed(handler),
    // Source 自动重连会替换 logical session；统一由 preload bridge 转发换代事件。
    onRemoteSessionReconnected: (handler) =>
      window.marid.onRemoteSessionReconnected?.(handler) ?? (() => {}),
    activateOrSetWorkspace: (path) =>
      window.marid.activateOrSetWorkspace?.(path) ?? Promise.resolve({ activated: false }),
    connectRemote: (remoteOptions, requestId, context) =>
      window.marid.connectRemote(remoteOptions, requestId, context),
    requestSourceSettingsAttachment: (sourceServerId) =>
      window.marid.requestSourceSettingsAttachment?.(sourceServerId) ??
      Promise.reject(new Error("Source settings attachment bridge unavailable")),
    cancelPendingRemoteConnection: (requestId) =>
      window.marid.cancelPendingRemoteConnection?.(requestId) ?? Promise.resolve(),
    bindRemoteWorkspaceSessionContext: async (context) => {
      if (!window.marid.bindRemoteWorkspaceSessionContext) {
        throw new Error("Desktop bindRemoteWorkspaceSessionContext bridge unavailable");
      }
      // preload 返回结构化结果，在页面一侧还原错误才能保留 code/retryable。
      return unwrapRemoteWorkspaceBindResult(
        await window.marid.bindRemoteWorkspaceSessionContext(context),
      );
    },
    startWebRemoteControl: (context) => window.marid.startWebRemoteControl(context),
    stopWebRemoteControl: () => window.marid.stopWebRemoteControl(),
    getWebRemoteControlStatus: () => window.marid.getWebRemoteControlStatus(),
    onWebRemoteControlStatusChanged: (handler) =>
      window.marid.onWebRemoteControlStatusChanged?.(handler) ?? (() => {}),
    disposeRemoteSession: (sessionId) => window.marid.disposeRemoteSession(sessionId),
    isDockerAvailable: () => window.marid.isDockerAvailable(),
    listWSLDistros: () => window.marid.listWSLDistros(),
    listDockerContainers: () => window.marid.listDockerContainers(),
    listSSHConfigAliases: () => window.marid.listSSHConfigAliases(),
    loadMcpFromUserDirectory: (payload) => window.marid.loadMcpFromUserDirectory(payload),
    saveMcpToUserDirectory: (payload) => window.marid.saveMcpToUserDirectory(payload),
    migrateLegacyCommonMcp: (payload) => window.marid.migrateLegacyCommonMcp(payload),
    openExternal: (url) => window.marid.openExternal(url),
    openFeedback: () => window.marid.executeDesktopCommand(DesktopCommandIds.OpenFeedback),
    openCommunity: () => window.marid.executeDesktopCommand(DesktopCommandIds.OpenCommunity),
    canOpenCommunity: (locale) => window.marid.canOpenCommunity(locale),
    openInFileManager: (path) => window.marid.openInFileManager(path),
    openExternalFile: (path) => window.marid.openExternalFile(path),
    openCuaPermissionOnboarding: window.marid.openCuaPermissionOnboarding
      ? (permissionOptions) =>
          window.marid.openCuaPermissionOnboarding?.(permissionOptions) ??
          Promise.resolve({ success: false, error: "not_supported" })
      : undefined,
    prepareCuaHelperPermissionDrag: window.marid.prepareCuaHelperPermissionDrag
      ? () =>
          window.marid.prepareCuaHelperPermissionDrag?.() ??
          Promise.resolve({ success: false, error: "not_supported" })
      : undefined,
    startCuaHelperPermissionDrag: window.marid.startCuaHelperPermissionDrag
      ? () => window.marid.startCuaHelperPermissionDrag?.()
      : undefined,
    notifyWebRemoteControlAuthStateChanged: (change) =>
      window.marid.notifyWebRemoteControlAuthStateChanged(change),
    registerOAuthState: (payload) => window.marid.registerOAuthState(payload),
    onOAuthCallback: (callback) => window.marid.onOAuthCallback(callback),
    onPaymentCallback: (callback) => window.marid.onPaymentCallback(callback),
    onShareImport: (callback) => window.marid.onShareImport?.(callback) ?? (() => {}),
    notifyRendererReady: () => window.marid.notifyRendererReady(),
    syncTelemetryContext: (context) => window.marid.syncTelemetryContext(context),
    reportTelemetryEvent: (payload) => window.marid.reportTelemetryEvent(payload),
    reportArmsCustomEvent: (payload) => {
      recordArmsCustomEventForE2E(payload);
      return window.marid.reportArmsCustomEvent(payload);
    },
    getRendererActionTraceConfig: window.marid.getRendererActionTraceConfig
      ? () => window.marid.getRendererActionTraceConfig!()
      : undefined,
    onRendererActionTraceConfigChanged: window.marid.onRendererActionTraceConfigChanged
      ? (callback) => window.marid.onRendererActionTraceConfigChanged!(callback)
      : undefined,
    reportLocalTtftBatch: (batch) => window.marid.reportLocalTtftBatch(batch),
    reportRendererActionTraceBatch: window.marid.reportRendererActionTraceBatch
      ? (batch) => window.marid.reportRendererActionTraceBatch!(batch)
      : undefined,
    reportRendererHeapSample: window.marid.reportRendererHeapSample
      ? (sample) => window.marid.reportRendererHeapSample!(sample)
      : undefined,
    showTaskNotification: (payload) => window.marid.showTaskNotification(payload),
    // 扩展的系统通知（§18.1 H8）：只有桌面实现；Web 与手机远控没有这两个方法，Renderer 按 unsupported 处理。
    showExtensionNotification: (payload) => window.marid.showExtensionNotification(payload),
    onExtensionNotificationClick: (handler) => window.marid.onExtensionNotificationClick(handler),
    syncWindowTabs: (paths) => window.marid.syncWindowTabs(paths),
    syncWebRemoteControlWorkspaces: (workspaces) =>
      window.marid.syncWebRemoteControlWorkspaces?.(workspaces),
    onWebRemoteControlReconnectWorkspace: (handler) =>
      window.marid.onWebRemoteControlReconnectWorkspace?.(handler) ?? (() => {}),
    onWebRemoteControlRendererAction: (handler) =>
      window.marid.onWebRemoteControlRendererAction?.(handler) ?? (() => {}),
    syncWindowUnreadCount: (count) => window.marid.syncWindowUnreadCount(count),
    syncActiveTaskSession: (sessionId) => window.marid.syncActiveTaskSession(sessionId),
    syncAppSettings: (patch) => window.marid.syncAppSettings?.(patch),
    // 桥缺失（旧 preload 升级窗口）时按 supported 兜底，与 Windows 现状一致，设置开关可用。
    getCloseToTrayCapability: () =>
      window.marid.getCloseToTrayCapability?.() ??
      Promise.resolve({ supported: true, gnomeLikeWithoutTray: false }),
    setShortcutRecordingActive: (active) => window.marid.setShortcutRecordingActive?.(active),
    onFocusTab: (handler) => window.marid.onFocusTab(handler),
    onNewTab: (handler) => window.marid.onNewTab(handler),
    onCloseActiveContextRequest: (handler) =>
      window.marid.onCloseActiveContextRequest?.(handler) ?? (() => {}),
    onOpenBrowserUrl: (handler) => window.marid.onOpenBrowserUrl?.(handler) ?? (() => {}),
    onEmbeddedBrowserPermissionPrompt: (handler) =>
      window.marid.onEmbeddedBrowserPermissionPrompt?.(handler) ?? (() => {}),
    resolveEmbeddedBrowserPermissionPrompt: (request) =>
      window.marid.resolveEmbeddedBrowserPermissionPrompt?.(request),
    getEmbeddedBrowserSitePermissions: () =>
      window.marid.getEmbeddedBrowserSitePermissions?.() ?? Promise.resolve({}),
    setEmbeddedBrowserSitePermission: (request) =>
      window.marid.setEmbeddedBrowserSitePermission?.(request) ?? Promise.resolve({}),
    clearEmbeddedBrowserSitePermissions: () =>
      window.marid.clearEmbeddedBrowserSitePermissions?.() ?? Promise.resolve({}),
    resetEmbeddedBrowserSitePermission: (request) =>
      window.marid.resetEmbeddedBrowserSitePermission?.(request) ?? Promise.resolve({}),
    onBrowserViewScreenshotSurfacePrepare: (handler) =>
      window.marid.onBrowserViewScreenshotSurfacePrepare?.(handler) ?? (() => {}),
    onBrowserViewScreenshotSurfaceRelease: (handler) =>
      window.marid.onBrowserViewScreenshotSurfaceRelease?.(handler) ?? (() => {}),
    browserViewScreenshotSurfaceReady: (payload) =>
      window.marid.browserViewScreenshotSurfaceReady?.(payload),
    ...desktopBrowserPlatformBridge,
    onNewTask: (handler) => window.marid.onNewTask(handler),
    onOpenWorkspace: (handler) => {
      // 开发态或升级后的旧窗口可能仍运行未暴露 onOpenWorkspace 的 preload，
      // renderer 直接调用会在启动时崩溃。这里和 activateOrSetWorkspace 一样做兼容兜底，
      // 缺少该 bridge 时只禁用原生菜单回调，不影响应用继续打开。
      return window.marid.onOpenWorkspace?.(handler) ?? (() => {});
    },
    onOpenWorkspacePath: (handler) => window.marid.onOpenWorkspacePath?.(handler) ?? (() => {}),
    onOpenFeedbackDialog: (handler) => window.marid.onOpenFeedbackDialog?.(handler) ?? (() => {}),
    onOpenTicketsPanel: (handler) => window.marid.onOpenTicketsPanel?.(handler) ?? (() => {}),
    onWindowFullscreenChanged: (handler) => window.marid.onWindowFullscreenChanged(handler),
    getDesktopWindowChromeState: window.marid.getDesktopWindowChromeState
      ? () => window.marid.getDesktopWindowChromeState!()
      : undefined,
    onDesktopWindowChromeStateChanged: window.marid.onDesktopWindowChromeStateChanged
      ? (handler) => window.marid.onDesktopWindowChromeStateChanged!(handler)
      : undefined,
    getWindowControlsOverlayMetrics: () => window.marid.getWindowControlsOverlayMetrics?.() ?? null,
    onWindowControlsOverlayChanged: (handler) =>
      window.marid.onWindowControlsOverlayChanged?.(handler) ?? (() => {}),
    getDesktopZoomLevel: () =>
      window.marid.getDesktopZoomLevel?.() ?? Promise.resolve({ zoomLevel: 0 }),
    onDesktopZoomLevelChanged: (handler) =>
      window.marid.onDesktopZoomLevelChanged?.(handler) ?? (() => {}),
    onTaskNotificationClick: (handler) => window.marid.onTaskNotificationClick(handler),
    exportLogs: () => window.marid.exportLogs(),
    captureWindowScreenshot: () =>
      window.marid.captureWindowScreenshot?.() ?? Promise.resolve(null),
    onUpdateReady: (callback) => window.marid.onUpdateReady(callback),
    onUpdateCheckResult: (callback) => window.marid.onUpdateCheckResult(callback),
    onUpdateStateChanged: (callback) => window.marid.onUpdateStateChanged?.(callback) ?? (() => {}),
    getUpdateState: () =>
      window.marid.getUpdateState?.() ?? Promise.resolve({ kind: "idle", enabled: true }),
    downloadUpdate: () => window.marid.downloadUpdate?.() ?? Promise.resolve(),
    cancelUpdateDownload: () => window.marid.cancelUpdateDownload?.() ?? Promise.resolve(),
    openUpdateStatusWindow: () => window.marid.openUpdateStatusWindow?.() ?? Promise.resolve(),
    getAutoUpdatePreferences: () =>
      window.marid.getAutoUpdatePreferences?.() ??
      Promise.resolve({ autoDownloadAndInstallUpdates: false }),
    setAutoDownloadAndInstallUpdates: (enabled) =>
      window.marid.setAutoDownloadAndInstallUpdates?.(enabled) ?? Promise.resolve(),
    getDesktopSessionActivity: () =>
      window.marid.getDesktopSessionActivity?.() ??
      Promise.resolve({ runningAgentSessionCount: 0 }),
    getMaridStdioTapDevState: () =>
      window.marid.getMaridStdioTapDevState?.() ??
      Promise.resolve({ enabled: false, visible: false, logDir: "", statePath: "" }),
    onSettingsChanged: (callback) => window.marid.onSettingsChanged?.(callback) ?? (() => {}),
    onApplicationLocaleChanged: (callback) =>
      window.marid.onApplicationLocaleChanged?.(callback) ?? (() => {}),
    onPostUpdateReleaseNotes: (callback) => window.marid.onPostUpdateReleaseNotes(callback),
    acknowledgePostUpdateReleaseNotes: (version) =>
      window.marid.acknowledgePostUpdateReleaseNotes(version),
    skipUpdateVersion: (version) => window.marid.skipUpdateVersion?.(version) ?? Promise.resolve(),
    quitAndInstallUpdate: () => window.marid.quitAndInstallUpdate(),
    getInstalledEditors: (filePath) => window.marid.getInstalledEditors(filePath),
    getFileApplications: (target) => window.marid.getFileApplications(target),
    openFileWithApplication: (applicationId, target) =>
      window.marid.openFileWithApplication(applicationId, target),
    getApplicationIcon: (bundleId) =>
      window.marid.getApplicationIcon?.(bundleId) ?? Promise.resolve(null),
    openInEditor: (editorId, path, editorOptions) =>
      window.marid.openInEditor(editorId, path, editorOptions),
    executeDesktopCommand: (command) => window.marid.executeDesktopCommand(command),
    setApplicationLocale: (locale) => window.marid.setApplicationLocale(locale),
    getSystemLocale: () =>
      window.marid.getSystemLocale?.() ??
      Promise.resolve(resolveLocaleFromLanguageTag(navigator.language)),
    setTitleBarTheme: (theme) => window.marid.setTitleBarTheme(theme),
    getDeviceId: () =>
      (window as Window & { __MARID_DEVICE_ID__?: string }).__MARID_DEVICE_ID__ ?? "",
  };
}
