import { recordArmsCustomEventForE2E } from "@qodex/ui";
import {
  DesktopCommandIds,
  buildLocalMediaPreviewUrl,
  resolveLocaleFromLanguageTag,
  unwrapRemoteWorkspaceBindResult,
  type IPlatformService,
} from "@qodex/shared";

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
          subscribe: (listener) => window.qodex.onAppControlNavigationMessage(listener),
          register: (generation) =>
            window.qodex.sendAppControlNavigation({ type: "register", generation }),
          unregister: (generation) =>
            window.qodex.sendAppControlNavigation({ type: "unregister", generation }),
          reply: (message) => window.qodex.sendAppControlNavigation({ type: "result", ...message }),
        },
        handler,
        crypto.randomUUID(),
      ),
    canSelectFilePath: true,
    canOpenLocalFiles: true,
    pluginSandbox: createDesktopPluginSandboxPlatform(),
    createLocalMediaPreviewUrl: buildLocalMediaPreviewUrl,
    isLocalDevelopmentRuntime: options.isLocalDevelopmentRuntime,
    selectDirectory: () => window.qodex.selectDirectory(),
    selectDirectories: () => window.qodex.selectDirectories(),
    selectFile: () => window.qodex.selectFile(),
    selectFiles: () => window.qodex.selectFiles?.() ?? Promise.resolve([]),
    createTempTextAttachment: (payload) => window.qodex.createTempTextAttachment(payload),
    materializeWorkflowArtifactFile: (payload) =>
      window.qodex.materializeWorkflowArtifactFile(payload),
    onRemoteConnectionLog: (handler) => window.qodex.onRemoteConnectionLog(handler),
    onRemoteSessionClosed: (handler) => window.qodex.onRemoteSessionClosed(handler),
    // Source 自动重连会替换 logical session；统一由 preload bridge 转发换代事件。
    onRemoteSessionReconnected: (handler) =>
      window.qodex.onRemoteSessionReconnected?.(handler) ?? (() => {}),
    activateOrSetWorkspace: (path) =>
      window.qodex.activateOrSetWorkspace?.(path) ?? Promise.resolve({ activated: false }),
    connectRemote: (remoteOptions, requestId, context) =>
      window.qodex.connectRemote(remoteOptions, requestId, context),
    requestSourceSettingsAttachment: (sourceServerId) =>
      window.qodex.requestSourceSettingsAttachment?.(sourceServerId) ??
      Promise.reject(new Error("Source settings attachment bridge unavailable")),
    cancelPendingRemoteConnection: (requestId) =>
      window.qodex.cancelPendingRemoteConnection?.(requestId) ?? Promise.resolve(),
    bindRemoteWorkspaceSessionContext: async (context) => {
      if (!window.qodex.bindRemoteWorkspaceSessionContext) {
        throw new Error("Desktop bindRemoteWorkspaceSessionContext bridge unavailable");
      }
      // preload 返回结构化结果，在页面一侧还原错误才能保留 code/retryable。
      return unwrapRemoteWorkspaceBindResult(
        await window.qodex.bindRemoteWorkspaceSessionContext(context),
      );
    },
    startWebRemoteControl: (context) => window.qodex.startWebRemoteControl(context),
    stopWebRemoteControl: () => window.qodex.stopWebRemoteControl(),
    getWebRemoteControlStatus: () => window.qodex.getWebRemoteControlStatus(),
    onWebRemoteControlStatusChanged: (handler) =>
      window.qodex.onWebRemoteControlStatusChanged?.(handler) ?? (() => {}),
    disposeRemoteSession: (sessionId) => window.qodex.disposeRemoteSession(sessionId),
    isDockerAvailable: () => window.qodex.isDockerAvailable(),
    listWSLDistros: () => window.qodex.listWSLDistros(),
    listDockerContainers: () => window.qodex.listDockerContainers(),
    listSSHConfigAliases: () => window.qodex.listSSHConfigAliases(),
    loadMcpFromUserDirectory: (payload) => window.qodex.loadMcpFromUserDirectory(payload),
    saveMcpToUserDirectory: (payload) => window.qodex.saveMcpToUserDirectory(payload),
    migrateLegacyCommonMcp: (payload) => window.qodex.migrateLegacyCommonMcp(payload),
    openExternal: (url) => window.qodex.openExternal(url),
    openFeedback: () => window.qodex.executeDesktopCommand(DesktopCommandIds.OpenFeedback),
    openCommunity: () => window.qodex.executeDesktopCommand(DesktopCommandIds.OpenCommunity),
    canOpenCommunity: (locale) => window.qodex.canOpenCommunity(locale),
    openInFileManager: (path) => window.qodex.openInFileManager(path),
    openExternalFile: (path) => window.qodex.openExternalFile(path),
    openCuaPermissionOnboarding: window.qodex.openCuaPermissionOnboarding
      ? (permissionOptions) =>
          window.qodex.openCuaPermissionOnboarding?.(permissionOptions) ??
          Promise.resolve({ success: false, error: "not_supported" })
      : undefined,
    prepareCuaHelperPermissionDrag: window.qodex.prepareCuaHelperPermissionDrag
      ? () =>
          window.qodex.prepareCuaHelperPermissionDrag?.() ??
          Promise.resolve({ success: false, error: "not_supported" })
      : undefined,
    startCuaHelperPermissionDrag: window.qodex.startCuaHelperPermissionDrag
      ? () => window.qodex.startCuaHelperPermissionDrag?.()
      : undefined,
    notifyWebRemoteControlAuthStateChanged: (change) =>
      window.qodex.notifyWebRemoteControlAuthStateChanged(change),
    registerOAuthState: (payload) => window.qodex.registerOAuthState(payload),
    onOAuthCallback: (callback) => window.qodex.onOAuthCallback(callback),
    onPaymentCallback: (callback) => window.qodex.onPaymentCallback(callback),
    onShareImport: (callback) => window.qodex.onShareImport?.(callback) ?? (() => {}),
    notifyRendererReady: () => window.qodex.notifyRendererReady(),
    syncTelemetryContext: (context) => window.qodex.syncTelemetryContext(context),
    reportTelemetryEvent: (payload) => window.qodex.reportTelemetryEvent(payload),
    reportArmsCustomEvent: (payload) => {
      recordArmsCustomEventForE2E(payload);
      return window.qodex.reportArmsCustomEvent(payload);
    },
    getRendererActionTraceConfig: window.qodex.getRendererActionTraceConfig
      ? () => window.qodex.getRendererActionTraceConfig!()
      : undefined,
    onRendererActionTraceConfigChanged: window.qodex.onRendererActionTraceConfigChanged
      ? (callback) => window.qodex.onRendererActionTraceConfigChanged!(callback)
      : undefined,
    reportLocalTtftBatch: (batch) => window.qodex.reportLocalTtftBatch(batch),
    reportRendererActionTraceBatch: window.qodex.reportRendererActionTraceBatch
      ? (batch) => window.qodex.reportRendererActionTraceBatch!(batch)
      : undefined,
    reportRendererHeapSample: window.qodex.reportRendererHeapSample
      ? (sample) => window.qodex.reportRendererHeapSample!(sample)
      : undefined,
    showTaskNotification: (payload) => window.qodex.showTaskNotification(payload),
    // 扩展的系统通知（§18.1 H8）：只有桌面实现；Web 与手机远控没有这两个方法，Renderer 按 unsupported 处理。
    showExtensionNotification: (payload) => window.qodex.showExtensionNotification(payload),
    onExtensionNotificationClick: (handler) => window.qodex.onExtensionNotificationClick(handler),
    syncWindowTabs: (paths) => window.qodex.syncWindowTabs(paths),
    syncWebRemoteControlWorkspaces: (workspaces) =>
      window.qodex.syncWebRemoteControlWorkspaces?.(workspaces),
    onWebRemoteControlReconnectWorkspace: (handler) =>
      window.qodex.onWebRemoteControlReconnectWorkspace?.(handler) ?? (() => {}),
    onWebRemoteControlRendererAction: (handler) =>
      window.qodex.onWebRemoteControlRendererAction?.(handler) ?? (() => {}),
    syncWindowUnreadCount: (count) => window.qodex.syncWindowUnreadCount(count),
    syncActiveTaskSession: (sessionId) => window.qodex.syncActiveTaskSession(sessionId),
    syncAppSettings: (patch) => window.qodex.syncAppSettings?.(patch),
    // 桥缺失（旧 preload 升级窗口）时按 supported 兜底，与 Windows 现状一致，设置开关可用。
    getCloseToTrayCapability: () =>
      window.qodex.getCloseToTrayCapability?.() ??
      Promise.resolve({ supported: true, gnomeLikeWithoutTray: false }),
    setShortcutRecordingActive: (active) => window.qodex.setShortcutRecordingActive?.(active),
    onFocusTab: (handler) => window.qodex.onFocusTab(handler),
    onNewTab: (handler) => window.qodex.onNewTab(handler),
    onCloseActiveContextRequest: (handler) =>
      window.qodex.onCloseActiveContextRequest?.(handler) ?? (() => {}),
    onOpenBrowserUrl: (handler) => window.qodex.onOpenBrowserUrl?.(handler) ?? (() => {}),
    onEmbeddedBrowserPermissionPrompt: (handler) =>
      window.qodex.onEmbeddedBrowserPermissionPrompt?.(handler) ?? (() => {}),
    resolveEmbeddedBrowserPermissionPrompt: (request) =>
      window.qodex.resolveEmbeddedBrowserPermissionPrompt?.(request),
    getEmbeddedBrowserSitePermissions: () =>
      window.qodex.getEmbeddedBrowserSitePermissions?.() ?? Promise.resolve({}),
    setEmbeddedBrowserSitePermission: (request) =>
      window.qodex.setEmbeddedBrowserSitePermission?.(request) ?? Promise.resolve({}),
    clearEmbeddedBrowserSitePermissions: () =>
      window.qodex.clearEmbeddedBrowserSitePermissions?.() ?? Promise.resolve({}),
    resetEmbeddedBrowserSitePermission: (request) =>
      window.qodex.resetEmbeddedBrowserSitePermission?.(request) ?? Promise.resolve({}),
    onBrowserViewScreenshotSurfacePrepare: (handler) =>
      window.qodex.onBrowserViewScreenshotSurfacePrepare?.(handler) ?? (() => {}),
    onBrowserViewScreenshotSurfaceRelease: (handler) =>
      window.qodex.onBrowserViewScreenshotSurfaceRelease?.(handler) ?? (() => {}),
    browserViewScreenshotSurfaceReady: (payload) =>
      window.qodex.browserViewScreenshotSurfaceReady?.(payload),
    ...desktopBrowserPlatformBridge,
    onNewTask: (handler) => window.qodex.onNewTask(handler),
    onOpenWorkspace: (handler) => {
      // 开发态或升级后的旧窗口可能仍运行未暴露 onOpenWorkspace 的 preload，
      // renderer 直接调用会在启动时崩溃。这里和 activateOrSetWorkspace 一样做兼容兜底，
      // 缺少该 bridge 时只禁用原生菜单回调，不影响应用继续打开。
      return window.qodex.onOpenWorkspace?.(handler) ?? (() => {});
    },
    onOpenWorkspacePath: (handler) => window.qodex.onOpenWorkspacePath?.(handler) ?? (() => {}),
    onOpenFeedbackDialog: (handler) => window.qodex.onOpenFeedbackDialog?.(handler) ?? (() => {}),
    onOpenTicketsPanel: (handler) => window.qodex.onOpenTicketsPanel?.(handler) ?? (() => {}),
    onWindowFullscreenChanged: (handler) => window.qodex.onWindowFullscreenChanged(handler),
    getDesktopWindowChromeState: window.qodex.getDesktopWindowChromeState
      ? () => window.qodex.getDesktopWindowChromeState!()
      : undefined,
    onDesktopWindowChromeStateChanged: window.qodex.onDesktopWindowChromeStateChanged
      ? (handler) => window.qodex.onDesktopWindowChromeStateChanged!(handler)
      : undefined,
    getWindowControlsOverlayMetrics: () => window.qodex.getWindowControlsOverlayMetrics?.() ?? null,
    onWindowControlsOverlayChanged: (handler) =>
      window.qodex.onWindowControlsOverlayChanged?.(handler) ?? (() => {}),
    getDesktopZoomLevel: () =>
      window.qodex.getDesktopZoomLevel?.() ?? Promise.resolve({ zoomLevel: 0 }),
    onDesktopZoomLevelChanged: (handler) =>
      window.qodex.onDesktopZoomLevelChanged?.(handler) ?? (() => {}),
    onTaskNotificationClick: (handler) => window.qodex.onTaskNotificationClick(handler),
    exportLogs: () => window.qodex.exportLogs(),
    captureWindowScreenshot: () =>
      window.qodex.captureWindowScreenshot?.() ?? Promise.resolve(null),
    onUpdateReady: (callback) => window.qodex.onUpdateReady(callback),
    onUpdateCheckResult: (callback) => window.qodex.onUpdateCheckResult(callback),
    onUpdateStateChanged: (callback) => window.qodex.onUpdateStateChanged?.(callback) ?? (() => {}),
    getUpdateState: () =>
      window.qodex.getUpdateState?.() ?? Promise.resolve({ kind: "idle", enabled: true }),
    downloadUpdate: () => window.qodex.downloadUpdate?.() ?? Promise.resolve(),
    cancelUpdateDownload: () => window.qodex.cancelUpdateDownload?.() ?? Promise.resolve(),
    openUpdateStatusWindow: () => window.qodex.openUpdateStatusWindow?.() ?? Promise.resolve(),
    getAutoUpdatePreferences: () =>
      window.qodex.getAutoUpdatePreferences?.() ??
      Promise.resolve({ autoDownloadAndInstallUpdates: false }),
    setAutoDownloadAndInstallUpdates: (enabled) =>
      window.qodex.setAutoDownloadAndInstallUpdates?.(enabled) ?? Promise.resolve(),
    getDesktopSessionActivity: () =>
      window.qodex.getDesktopSessionActivity?.() ??
      Promise.resolve({ runningAgentSessionCount: 0 }),
    getQodexStdioTapDevState: () =>
      window.qodex.getQodexStdioTapDevState?.() ??
      Promise.resolve({ enabled: false, visible: false, logDir: "", statePath: "" }),
    onSettingsChanged: (callback) => window.qodex.onSettingsChanged?.(callback) ?? (() => {}),
    onApplicationLocaleChanged: (callback) =>
      window.qodex.onApplicationLocaleChanged?.(callback) ?? (() => {}),
    onPostUpdateReleaseNotes: (callback) => window.qodex.onPostUpdateReleaseNotes(callback),
    acknowledgePostUpdateReleaseNotes: (version) =>
      window.qodex.acknowledgePostUpdateReleaseNotes(version),
    skipUpdateVersion: (version) => window.qodex.skipUpdateVersion?.(version) ?? Promise.resolve(),
    quitAndInstallUpdate: () => window.qodex.quitAndInstallUpdate(),
    getInstalledEditors: (filePath) => window.qodex.getInstalledEditors(filePath),
    getFileApplications: (target) => window.qodex.getFileApplications(target),
    openFileWithApplication: (applicationId, target) =>
      window.qodex.openFileWithApplication(applicationId, target),
    getApplicationIcon: (bundleId) =>
      window.qodex.getApplicationIcon?.(bundleId) ?? Promise.resolve(null),
    openInEditor: (editorId, path, editorOptions) =>
      window.qodex.openInEditor(editorId, path, editorOptions),
    executeDesktopCommand: (command) => window.qodex.executeDesktopCommand(command),
    setApplicationLocale: (locale) => window.qodex.setApplicationLocale(locale),
    getSystemLocale: () =>
      window.qodex.getSystemLocale?.() ??
      Promise.resolve(resolveLocaleFromLanguageTag(navigator.language)),
    setTitleBarTheme: (theme) => window.qodex.setTitleBarTheme(theme),
    getDeviceId: () =>
      (window as Window & { __QODEX_DEVICE_ID__?: string }).__QODEX_DEVICE_ID__ ?? "",
  };
}
