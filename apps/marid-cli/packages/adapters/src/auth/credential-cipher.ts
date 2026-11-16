import {
  createLegacyCredentialCipher,
  type CredentialCipher,
} from "@marid/shared/credential-security";
export type MaridCredentialCipher = CredentialCipher;
export interface MaridCredentialCipherOptions {
  env?: Record<string, string | undefined>;
}
/** 仅兼容旧格式；正式凭据仓库默认使用系统安全存储。 */
export function createMaridCredentialCipher(options: MaridCredentialCipherOptions = {}) {
  return createLegacyCredentialCipher(options.env);
}
export function isEncryptedMaridCredentialValue(value: string): boolean {
  return value.startsWith("enc:");
}
