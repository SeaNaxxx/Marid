import {
  ProviderConfigService,
  type ProviderConfigLayerSnapshot,
  type ProviderConfigLayerUpdate,
} from "@qodex/provider";
import { NodeQodexBuiltinProviderConfigSource } from "./qodex-builtin-provider-config-source.js";
import {
  EndpointScopedQodexBuiltinSource,
  type EndpointScopedQodexBuiltinSourceOptions,
} from "./endpoint-scoped-qodex-builtin-source.js";
import {
  QodexBuiltinRemoteSynchronizer,
  type QodexBuiltinRemoteSynchronizerOptions,
  type QodexBuiltinRefreshResult,
} from "./qodex-builtin-remote-synchronizer.js";
import {
  NodePersonalProviderConfigRepository,
  type PersonalProviderConfigRecoveryEvent,
} from "./personal-provider-config-repository.js";

export interface NodeProviderConfigRuntimeOptions {
  readonly qodexBuiltinFilePath: string;
  readonly qodexBuiltinActiveFilePath?: string;
  readonly qodexBuiltinRemote?: Omit<QodexBuiltinRemoteSynchronizerOptions, "source">;
  readonly qodexBuiltinEnvironment?: Omit<
    EndpointScopedQodexBuiltinSourceOptions,
    "bundledFilePath"
  >;
  readonly onQodexBuiltinRefreshError?: (error: unknown) => void;
  readonly onPersonalConfigRecovery?: (event: PersonalProviderConfigRecoveryEvent) => void;
  readonly onPersonalConfigPollingError?: (error: unknown) => void;
  readonly personalFilePath: string;
  readonly personalPollingIntervalMs?: number | false;
  readonly importLegacy?: (
    qodexBuiltin: ProviderConfigLayerSnapshot,
  ) => Promise<ProviderConfigLayerUpdate | null>;
  readonly watch?: boolean;
}

/** 组装一个 Node.js 进程内共享的 Qodex Built-in/Personal Config 运行边界。 */
export class NodeProviderConfigRuntime {
  readonly configService: ProviderConfigService;
  readonly #qodexBuiltinSource:
    | NodeQodexBuiltinProviderConfigSource
    | EndpointScopedQodexBuiltinSource;
  readonly #personalRepository: NodePersonalProviderConfigRepository;
  readonly #remoteSynchronizer?: QodexBuiltinRemoteSynchronizer;
  readonly #onRemoteRefreshError?: (error: unknown) => void;
  #startPromise: Promise<void> | null = null;
  #disposed = false;
  readonly #checkListeners = new Set<() => Promise<void>>();
  #checkTimer: ReturnType<typeof setInterval> | null = null;
  #checkInFlight: Promise<void> | null = null;

  constructor(options: NodeProviderConfigRuntimeOptions) {
    this.#qodexBuiltinSource = options.qodexBuiltinEnvironment
      ? new EndpointScopedQodexBuiltinSource({
          bundledFilePath: options.qodexBuiltinFilePath,
          ...options.qodexBuiltinEnvironment,
        })
      : new NodeQodexBuiltinProviderConfigSource({
          bundledFilePath: options.qodexBuiltinFilePath,
          activeFilePath: options.qodexBuiltinActiveFilePath,
          watch: options.watch,
        });
    this.#remoteSynchronizer =
      options.qodexBuiltinRemote &&
      this.#qodexBuiltinSource instanceof NodeQodexBuiltinProviderConfigSource
        ? new QodexBuiltinRemoteSynchronizer({
            source: this.#qodexBuiltinSource,
            ...options.qodexBuiltinRemote,
          })
        : undefined;
    this.#onRemoteRefreshError = options.onQodexBuiltinRefreshError;
    this.#personalRepository = new NodePersonalProviderConfigRepository({
      filePath: options.personalFilePath,
      onRecovery: options.onPersonalConfigRecovery,
      onPollingError: options.onPersonalConfigPollingError,
      pollingIntervalMs: options.personalPollingIntervalMs,
      ...(options.importLegacy
        ? {
            importLegacy: async () => options.importLegacy!(await this.#qodexBuiltinSource.read()),
          }
        : {}),
    });
    this.configService = new ProviderConfigService({
      qodexBuiltinSource: this.#qodexBuiltinSource,
      personalRepository: this.#personalRepository,
    });
  }

  resolveQodexBuiltinActiveFilePath(): Promise<string> {
    return this.#qodexBuiltinSource instanceof NodeQodexBuiltinProviderConfigSource
      ? Promise.resolve(this.#qodexBuiltinSource.activeFilePath)
      : this.#qodexBuiltinSource.resolveActiveFilePath();
  }

  get personalRepository(): import("@qodex/provider").PersonalProviderConfigRepository {
    return this.#personalRepository;
  }

  /** Environment 同一周期检查中恢复未对齐依赖，不被下载 TTL 或失败挡住。 */
  onDidCheckQodexBuiltin(listener: () => Promise<void>): () => void {
    this.#checkListeners.add(listener);
    return () => this.#checkListeners.delete(listener);
  }

  start(): Promise<void> {
    if (this.#disposed) throw new Error("NodeProviderConfigRuntime 已 dispose");
    if (this.#startPromise) return this.#startPromise;
    const startPromise = this.configService.read().then(() => {
      if (this.#disposed) return;
      void this.#checkBackground();
      // Managed Worker 无下载配置也无恢复 owner，不建立周期任务。
      if (
        this.#remoteSynchronizer ||
        this.#qodexBuiltinSource instanceof EndpointScopedQodexBuiltinSource ||
        this.#checkListeners.size > 0
      ) {
        this.#checkTimer = setInterval(() => {
          void this.#checkBackground();
        }, 60_000);
        this.#checkTimer.unref?.();
      }
    });
    this.#startPromise = startPromise;
    void startPromise.catch(() => {
      if (this.#startPromise === startPromise) this.#startPromise = null;
    });
    return startPromise;
  }

  refreshQodexBuiltin(options?: { readonly force?: boolean }): Promise<QodexBuiltinRefreshResult> {
    if (this.#disposed) return Promise.resolve("disposed");
    if (this.#qodexBuiltinSource instanceof EndpointScopedQodexBuiltinSource) {
      return this.#qodexBuiltinSource.refresh(options);
    }
    return this.#remoteSynchronizer?.refresh(options) ?? Promise.resolve("skipped");
  }

  #checkBackground(): Promise<void> {
    if (this.#disposed) return Promise.resolve();
    if (this.#checkInFlight) return this.#checkInFlight;
    const check = Promise.allSettled([
      this.refreshQodexBuiltin(),
      ...[...this.#checkListeners].map((listener) => Promise.resolve().then(listener)),
    ])
      .then((results) => {
        if (this.#disposed) return;
        for (const result of results)
          if (result.status === "rejected") this.#onRemoteRefreshError?.(result.reason);
      })
      .finally(() => {
        if (this.#checkInFlight === check) this.#checkInFlight = null;
      });
    this.#checkInFlight = check;
    return check;
  }

  dispose(): void {
    if (this.#disposed) return;
    this.#disposed = true;
    if (this.#checkTimer) clearInterval(this.#checkTimer);
    this.#checkTimer = null;
    this.#checkListeners.clear();
    this.#remoteSynchronizer?.dispose();
    this.configService.dispose();
    this.#personalRepository.dispose();
    this.#qodexBuiltinSource.dispose();
  }
}

export function createNodeProviderConfigRuntime(
  options: NodeProviderConfigRuntimeOptions,
): NodeProviderConfigRuntime {
  return new NodeProviderConfigRuntime(options);
}
