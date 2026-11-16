import type { UiLocale, SupportedLocale } from "@qodex/contracts";
import { enUS } from "./locales/en-US.js";
import { zhCN } from "./locales/zh-CN.js";
import {
  DEFAULT_LOCALE,
  detectLocale,
  isSupportedLocale,
  isUiLocale,
  resolveLocale,
  SUPPORTED_LOCALES,
} from "./locale.js";
import type { QodexCopy } from "./types.js";

export {
  DEFAULT_LOCALE,
  SUPPORTED_LOCALES,
  detectLocale,
  isSupportedLocale,
  isUiLocale,
  resolveLocale,
};
export type { LocaleDetectionInput } from "./locale.js";
export type { CliCopy, TuiCopy, UiLocale, SupportedLocale, QodexCopy } from "./types.js";

const CATALOGS: Record<SupportedLocale, QodexCopy> = {
  "en-US": enUS,
  "zh-CN": zhCN,
};

export function getQodexCopy(locale?: UiLocale | string, detected?: string | null): QodexCopy {
  return CATALOGS[resolveLocale(locale, detected)];
}
