import {
  createLegacyCredentialCipher,
  type CredentialCipher,
} from "@qodex/shared/credential-security";
export type QodexCredentialCipher = CredentialCipher;
export interface QodexCredentialCipherOptions {
  env?: Record<string, string | undefined>;
}
/** 仅兼容旧格式；正式凭据仓库默认使用系统安全存储。 */
export function createQodexCredentialCipher(options: QodexCredentialCipherOptions = {}) {
  return createLegacyCredentialCipher(options.env);
}
export function isEncryptedQodexCredentialValue(value: string): boolean {
  return value.startsWith("enc:");
}
