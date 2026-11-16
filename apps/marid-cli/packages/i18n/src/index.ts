import type { UiLocale, SupportedLocale } from "@marid/contracts";
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
import type { MaridCopy } from "./types.js";

export {
  DEFAULT_LOCALE,
  SUPPORTED_LOCALES,
  detectLocale,
  isSupportedLocale,
  isUiLocale,
  resolveLocale,
};
export type { LocaleDetectionInput } from "./locale.js";
export type { CliCopy, TuiCopy, UiLocale, SupportedLocale, MaridCopy } from "./types.js";

const CATALOGS: Record<SupportedLocale, MaridCopy> = {
  "en-US": enUS,
  "zh-CN": zhCN,
};

export function getMaridCopy(locale?: UiLocale | string, detected?: string | null): MaridCopy {
  return CATALOGS[resolveLocale(locale, detected)];
}
