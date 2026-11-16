import type {
  createCodingPlanApiKeyResolver,
  createCliOAuthClient,
  BrowserOpenResult,
  CliOAuthInitData,
  CliOAuthPollData,
  CliOAuthUser,
  SharedMaridCredentialStore,
} from "@marid/adapters";
import type { EnvRecord } from "@marid/adapters/model";

export type CodingPlanProviderId = "bigmodel" | "zai";

export interface LoginMaridCliOptions {
  providerId?: CodingPlanProviderId;
  abortSignal?: AbortSignal;
  apiKeyResolver?: ReturnType<typeof createCodingPlanApiKeyResolver>;
  baseUrl?: string;
  credentialStore?: SharedMaridCredentialStore;
  env?: EnvRecord;
  httpClient?: Parameters<typeof createCliOAuthClient>[0]["httpClient"];
  noBrowser?: boolean;
  now?: () => number;
  onAuthorizeUrl?: (data: CliOAuthInitData) => void | Promise<void>;
  onBrowserOpen?: (result: BrowserOpenResult) => void | Promise<void>;
  onPollStatus?: (data: CliOAuthPollData) => void | Promise<void>;
  openBrowser?: (url: string) => Promise<BrowserOpenResult>;
  pollToken?: string;
  sleep?: (ms: number) => Promise<void>;
  timeoutMs?: number;
  personalProviderConfigPath?: string;
}

export interface LoginMaridCliResult {
  browser?: BrowserOpenResult;
  configPath: string;
  credentialsPath: string;
  model: string;
  providerId: CodingPlanProviderId;
  user: CliOAuthUser;
}

// BigModel 与 Z.ai 共用服务端轮询登录，只是 providerId 固定为 bigmodel。
export type LoginBigmodelCodingPlanOptions = Omit<LoginMaridCliOptions, "providerId">;
export type LoginBigmodelCodingPlanResult = LoginMaridCliResult & { providerId: "bigmodel" };

export interface ConfigureCodingPlanApiKeyOptions {
  apiKey: string;
  credentialStore?: SharedMaridCredentialStore;
  env?: EnvRecord;
  personalProviderConfigPath?: string;
  providerId: CodingPlanProviderId;
}

export interface ConfigureCodingPlanApiKeyResult {
  configPath: string;
  model: string;
  providerId: CodingPlanProviderId;
}

export interface LogoutMaridCliOptions {
  credentialStore?: SharedMaridCredentialStore;
  env?: EnvRecord;
}

export interface LogoutMaridCliResult {
  credentialsPath: string;
}

export class MaridCliLoginError extends Error {
  readonly code:
    | "auth_failed"
    | "auth_timeout"
    | "config_update_failed"
    | "credential_write_failed";

  constructor(
    code: MaridCliLoginError["code"],
    message: string,
    options: { cause?: unknown } = {},
  ) {
    super(message, options);
    this.name = "MaridCliLoginError";
    this.code = code;
  }
}
